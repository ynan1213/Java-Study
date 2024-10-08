/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.bootstrap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.cloud.bootstrap.encrypt.EnvironmentDecryptApplicationInitializer;
import org.springframework.cloud.bootstrap.support.OriginTrackedCompositePropertySource;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A listener that prepares a SpringApplication (e.g. populating its Environment) by
 * delegating to {@link ApplicationContextInitializer} beans in a separate bootstrap
 * context. The bootstrap context is a SpringApplication created from sources defined in
 * spring.factories as {@link BootstrapConfiguration}, and initialized with external
 * config taken from "bootstrap.properties" (or yml), instead of the normal
 * "application.properties".
 *
 * @author Dave Syer
 *
 * BootstrapApplicationListener 在 ConfigFileApplicationListener前面！！！
 *
 * 1. SpringCloud Context模块的功能  ：主要是构建了一个Bootstrap容器，并让其成为原有的springboot程序构建的容器的父容器。
 * 2. Bootstrap容器的作用：是为了预先完成一些bean的实例化工作，可以把Bootstrap容器看作是先头部队。
 * 3. Bootstrap容器的构建：是利用了Springboot的事件机制，当 springboot 的初始化 Environment  准备好之后会发布一个事件，
 *    这个事件的监听器将负责完成Bootstrap容器的创建。构建时是使用 SpringApplicationBuilder 类来完成的。
 * 4. 如何让Bootstrap容器与应用Context 建立父子关系 ：由于Bootstrap容器与应用Context都是关联着同一个SpringApplication实例，
 * 	  Bootstrap容器自己完成初始化器的调用之后，会动态添加了一个初始化器 AncestorInitializer，相当于给应用Context 埋了个雷 ，
 *    这个初始化器在应用容器进行初始化器调用执行时，完成父子关系的设置。
 */
public class BootstrapApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	/**
	 * Property source name for bootstrap.
	 */
	public static final String BOOTSTRAP_PROPERTY_SOURCE_NAME = "bootstrap";

	/**
	 * The default order for this listener.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

	/**
	 * The name of the default properties.
	 */
	public static final String DEFAULT_PROPERTIES = "springCloudDefaultProperties";

	private int order = DEFAULT_ORDER;

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();

		// 可以在application.properties中配置spring.cloud.bootstrap.enabled=false来禁用spring cloud的bootstrap容器
		// 其实是不行的，因为此时的子容器并未加载配置文件，所以该配置只能配置在系统环境变量中
		if (!environment.getProperty("spring.cloud.bootstrap.enabled", Boolean.class, true)) {
			return;
		}

		// 由于Bootstrap容器在创建时还是会再次调用上面步骤的代码，还会再次触发BootstrapApplicationListener类这个方法，
		// 所以此处作个判断，如果当前是Bootstrap容器，则直接返回
		if (environment.getPropertySources().contains(BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
			return;
		}

		ConfigurableApplicationContext context = null;

		// 获取配置文件的名字，默认为bootstrap.properties或.yml ,并且这个名字可以通过 spring.cloud.bootstrap.name在环境中配置
		// 同样的，该配置只能配置在系统环境变量中才会生效
		String configName = environment.resolvePlaceholders("${spring.cloud.bootstrap.name:bootstrap}");

		// 从spring.factories文件中读取初始化器的配置类实例，如果这个实例类是ParentContextApplicationContextInitializer类型
		// 则直接从该类中获取父容器，默认情况下没有提供这样的类，下面这段代码会跳过
		for (ApplicationContextInitializer<?> initializer : event.getSpringApplication().getInitializers()) {
			if (initializer instanceof ParentContextApplicationContextInitializer) {
				context = findBootstrapContext((ParentContextApplicationContextInitializer) initializer, configName);
			}
		}
		if (context == null) {
			// 一般没有父上下文初始化，所以要重新创建一个上下文，为的就是来加载一些配置文件
			context = bootstrapServiceContext(environment, event.getSpringApplication(), configName);
			event.getSpringApplication().addListeners(new CloseContextOnFailureApplicationListener(context));
		}

		apply(context, event.getSpringApplication(), environment);
	}

	private ConfigurableApplicationContext findBootstrapContext(ParentContextApplicationContextInitializer initializer, String configName) {
		Field field = ReflectionUtils.findField(ParentContextApplicationContextInitializer.class, "parent");
		ReflectionUtils.makeAccessible(field);
		ConfigurableApplicationContext parent = safeCast(ConfigurableApplicationContext.class, ReflectionUtils.getField(field, initializer));
		if (parent != null && !configName.equals(parent.getId())) {
			parent = safeCast(ConfigurableApplicationContext.class, parent.getParent());
		}
		return parent;
	}

	private <T> T safeCast(Class<T> type, Object object) {
		try {
			return type.cast(object);
		} catch (ClassCastException e) {
			return null;
		}
	}

	private ConfigurableApplicationContext bootstrapServiceContext(
			ConfigurableEnvironment environment, final SpringApplication application, String configName) {
		// 创建一个新的环境
		StandardEnvironment bootstrapEnvironment = new StandardEnvironment();
		MutablePropertySources bootstrapProperties = bootstrapEnvironment.getPropertySources();
		for (PropertySource<?> source : bootstrapProperties) {
			// 将里面的属性源删除干净，创建的时候默认会添加两个系统环境变量：systemProperties、systemEnvironment
			bootstrapProperties.remove(source.getName());
		}
		String configLocation = environment.resolvePlaceholders("${spring.cloud.bootstrap.location:}");
		String configAdditionalLocation = environment.resolvePlaceholders("${spring.cloud.bootstrap.additional-location:}");
		Map<String, Object> bootstrapMap = new HashMap<>();

		// spring.config.name 可以设置配置文件名称，不配置的话是 application.yml 和 application.properties
		// 这里设置为 bootstrap，即默认情况下 springcloud 的父容器默认的配置文件为 bootstrap.yml 和 bootstrap.properties
		bootstrapMap.put("spring.config.name", configName);

		// if an app (or test) uses spring.main.web-application-type=reactive, bootstrap will fail
		// force the environment to use none, because if though it is set below in the builder
		// the environment overrides it
		bootstrapMap.put("spring.main.web-application-type", "none");
		if (StringUtils.hasText(configLocation)) {
			bootstrapMap.put("spring.config.location", configLocation);
		}
		if (StringUtils.hasText(configAdditionalLocation)) {
			bootstrapMap.put("spring.config.additional-location", configAdditionalLocation);
		}
		bootstrapProperties.addFirst(new MapPropertySource(BOOTSTRAP_PROPERTY_SOURCE_NAME, bootstrapMap));
		for (PropertySource<?> source : environment.getPropertySources()) {
			// StubPropertySource 没有内容仅仅起到占位的作用
			if (source instanceof StubPropertySource) {
				continue;
			}
			// 将原先environment中的属性添加进来
			bootstrapProperties.addLast(source);
		}

		// TODO: is it possible or sensible to share a ResourceLoader?  共享一个ResourceLoader是可能的还是明智的
		SpringApplicationBuilder builder = new SpringApplicationBuilder()
				.profiles(environment.getActiveProfiles())
				.bannerMode(Mode.OFF)
				.environment(bootstrapEnvironment)
				.registerShutdownHook(false)
				.logStartupInfo(false)
				.web(WebApplicationType.NONE);
		final SpringApplication builderApplication = builder.application();
		if (builderApplication.getMainApplicationClass() == null) {
			// gh_425:
			// SpringApplication cannot deduce the MainApplicationClass here if it is booted from SpringBootServletInitializer due to the
			// absense of the "main" method in stackTraces. But luckily this method's second parameter "application" here
			// carries the real MainApplicationClass which has been explicitly set by SpringBootServletInitializer itself already.
			// 什么时候 mainApplicationClass 为null呢？
			// mainApplicationClass是遍历stackTraces寻找main方法所在的类，如果listener是使用异步线程池的方式，是获取不到的
			builder.main(application.getMainApplicationClass());
		}
		if (environment.getPropertySources().contains("refreshArgs")) {
			// If we are doing a context refresh, really we only want to refresh the
			// Environment, and there are some toxic listeners (like the
			// LoggingApplicationListener) that affect global static state, so we need a
			// way to switch those off.
			builderApplication.setListeners(filterListeners(builderApplication.getListeners()));
		}
		builder.sources(BootstrapImportSelectorConfiguration.class);

		// 构建出BootstrapContext（父容器），这里返回的 ApplicationContext 类型是 AnnotationConfigApplicationContext
		final ConfigurableApplicationContext context = builder.run();

		// gh-214 using spring.application.name=bootstrap to set the context id via
		// `ContextIdApplicationContextInitializer` prevents apps from getting the actual spring.application.name during the bootstrap phase.
		context.setId("bootstrap");

		// Make the bootstrap context a parent of the app context
		// 设置 BootstrapContext 成为应用Context的父容器
		addAncestorInitializer(application, context);

		// It only has properties in it now that we don't want in the parent so remove
		// it (and it will be added back later)
		bootstrapProperties.remove(BOOTSTRAP_PROPERTY_SOURCE_NAME);
		mergeDefaultProperties(environment.getPropertySources(), bootstrapProperties);
		return context;
	}

	private Collection<? extends ApplicationListener<?>> filterListeners(Set<ApplicationListener<?>> listeners) {
		Set<ApplicationListener<?>> result = new LinkedHashSet<>();
		for (ApplicationListener<?> listener : listeners) {
			if (!(listener instanceof LoggingApplicationListener) && !(listener instanceof LoggingSystemShutdownListener)) {
				result.add(listener);
			}
		}
		return result;
	}

	private void mergeDefaultProperties(MutablePropertySources environment, MutablePropertySources bootstrap) {
		String name = DEFAULT_PROPERTIES;
		if (bootstrap.contains(name)) {
			PropertySource<?> source = bootstrap.get(name);
			if (!environment.contains(name)) {
				environment.addLast(source);
			} else {
				PropertySource<?> target = environment.get(name);
				if (target instanceof MapPropertySource && target != source && source instanceof MapPropertySource) {
					Map<String, Object> targetMap = ((MapPropertySource) target).getSource();
					Map<String, Object> map = ((MapPropertySource) source).getSource();
					for (String key : map.keySet()) {
						if (!target.containsProperty(key)) {
							targetMap.put(key, map.get(key));
						}
					}
				}
			}
		}
		mergeAdditionalPropertySources(environment, bootstrap);
	}

	private void mergeAdditionalPropertySources(MutablePropertySources environment, MutablePropertySources bootstrap) {
		PropertySource<?> defaultProperties = environment.get(DEFAULT_PROPERTIES);
		ExtendedDefaultPropertySource result = defaultProperties instanceof ExtendedDefaultPropertySource
				? (ExtendedDefaultPropertySource) defaultProperties
				: new ExtendedDefaultPropertySource(DEFAULT_PROPERTIES, defaultProperties);
		for (PropertySource<?> source : bootstrap) {
			if (!environment.contains(source.getName())) {
				result.add(source);
			}
		}
		for (String name : result.getPropertySourceNames()) {
			bootstrap.remove(name);
		}
		addOrReplace(environment, result);
		addOrReplace(bootstrap, result);
	}

	private void addOrReplace(MutablePropertySources environment, PropertySource<?> result) {
		if (environment.contains(result.getName())) {
			environment.replace(result.getName(), result);
		} else {
			environment.addLast(result);
		}
	}

	private void addAncestorInitializer(SpringApplication application, ConfigurableApplicationContext context) {
		boolean installed = false;
		// 从spring.factories文件中获取初始化器的配置类且类型为AncestorInitializer
		for (ApplicationContextInitializer<?> initializer : application.getInitializers()) {
			if (initializer instanceof AncestorInitializer) {
				installed = true;
				// New parent
				((AncestorInitializer) initializer).setParent(context);
			}
		}
		// 默认情况下是没有配置AncestorInitializer这样的类，此处是则执行由BootstrapListener提供的内部类
		if (!installed) {
			// 将BootstrapContext作为父容器传到AncestorInitializer实例中，并将其放入SpringApplication实例的初始器列表中
			application.addInitializers(new AncestorInitializer(context));
		}

	}

	@SuppressWarnings("unchecked")
	private void apply(ConfigurableApplicationContext context, SpringApplication application, ConfigurableEnvironment environment) {
		if (application.getAllSources().contains(BootstrapMarkerConfiguration.class)) {
			return;
		}
		application.addPrimarySources(Arrays.asList(BootstrapMarkerConfiguration.class));
		@SuppressWarnings("rawtypes")
		Set target = new LinkedHashSet<>(application.getInitializers());
		target.addAll(getOrderedBeansOfType(context, ApplicationContextInitializer.class));
		application.setInitializers(target);
		addBootstrapDecryptInitializer(application);
	}

	@SuppressWarnings("unchecked")
	private void addBootstrapDecryptInitializer(SpringApplication application) {
		DelegatingEnvironmentDecryptApplicationInitializer decrypter = null;
		Set<ApplicationContextInitializer<?>> initializers = new LinkedHashSet<>();
		for (ApplicationContextInitializer<?> ini : application.getInitializers()) {
			if (ini instanceof EnvironmentDecryptApplicationInitializer) {
				@SuppressWarnings("rawtypes")
				ApplicationContextInitializer del = ini;
				decrypter = new DelegatingEnvironmentDecryptApplicationInitializer(del);
				initializers.add(ini);
				initializers.add(decrypter);
			} else if (ini instanceof DelegatingEnvironmentDecryptApplicationInitializer) {
				// do nothing
			} else {
				initializers.add(ini);
			}
		}
		ArrayList<ApplicationContextInitializer<?>> target = new ArrayList<ApplicationContextInitializer<?>>(initializers);
		application.setInitializers(target);
	}

	private <T> List<T> getOrderedBeansOfType(ListableBeanFactory context, Class<T> type) {
		List<T> result = new ArrayList<T>();
		for (String name : context.getBeanNamesForType(type)) {
			result.add(context.getBean(name, type));
		}
		AnnotationAwareOrderComparator.sort(result);
		return result;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	private static class BootstrapMarkerConfiguration {

	}

	private static class AncestorInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

		private ConfigurableApplicationContext parent;

		AncestorInitializer(ConfigurableApplicationContext parent) {
			this.parent = parent;
		}

		public void setParent(ConfigurableApplicationContext parent) {
			this.parent = parent;
		}

		@Override
		public int getOrder() {
			// Need to run not too late (so not unordered), so that, for instance, the
			// ContextIdApplicationContextInitializer runs later and picks up the merged
			// Environment. Also needs to be quite early so that other initializers can
			// pick up the parent (especially the Environment).
			return Ordered.HIGHEST_PRECEDENCE + 5;
		}

		// 初始化器AncestorInitializer被触发，是由应用Context的处理触发的
		// 入参 context 就是应用context
		@Override
		public void initialize(ConfigurableApplicationContext context) {
			// 如果父容器不为空，设置为父容器的父容器
			while (context.getParent() != null && context.getParent() != context) {
				context = (ConfigurableApplicationContext) context.getParent();
			}
			reorderSources(context.getEnvironment());
			// 为应用容器设置父容器
			new ParentContextApplicationContextInitializer(this.parent).initialize(context);
		}

		private void reorderSources(ConfigurableEnvironment environment) {
			PropertySource<?> removed = environment.getPropertySources().remove(DEFAULT_PROPERTIES);
			if (removed instanceof ExtendedDefaultPropertySource) {
				ExtendedDefaultPropertySource defaultProperties = (ExtendedDefaultPropertySource) removed;
				environment.getPropertySources().addLast(new MapPropertySource(DEFAULT_PROPERTIES, defaultProperties.getSource()));
				for (PropertySource<?> source : defaultProperties.getPropertySources().getPropertySources()) {
					if (!environment.getPropertySources().contains(source.getName())) {
						environment.getPropertySources().addBefore(DEFAULT_PROPERTIES, source);
					}
				}
			}
		}

	}

	/**
	 * A special initializer designed to run before the property source bootstrap and
	 * decrypt any properties needed there (e.g. URL of config server).
	 */
	@Order(Ordered.HIGHEST_PRECEDENCE + 9)
	private static class DelegatingEnvironmentDecryptApplicationInitializer implements
			ApplicationContextInitializer<ConfigurableApplicationContext> {

		private ApplicationContextInitializer<ConfigurableApplicationContext> delegate;

		DelegatingEnvironmentDecryptApplicationInitializer(ApplicationContextInitializer<ConfigurableApplicationContext> delegate) {
			this.delegate = delegate;
		}

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			this.delegate.initialize(applicationContext);
		}

	}

	private static class ExtendedDefaultPropertySource extends SystemEnvironmentPropertySource implements OriginLookup<String> {

		private final OriginTrackedCompositePropertySource sources;

		private final List<String> names = new ArrayList<>();

		ExtendedDefaultPropertySource(String name, PropertySource<?> propertySource) {
			super(name, findMap(propertySource));
			this.sources = new OriginTrackedCompositePropertySource(name);
		}

		@SuppressWarnings("unchecked")
		private static Map<String, Object> findMap(PropertySource<?> propertySource) {
			if (propertySource instanceof MapPropertySource) {
				return (Map<String, Object>) propertySource.getSource();
			}
			return new LinkedHashMap<String, Object>();
		}

		public CompositePropertySource getPropertySources() {
			return this.sources;
		}

		public List<String> getPropertySourceNames() {
			return this.names;
		}

		public void add(PropertySource<?> source) {
			// Only add map property sources added by boot, see gh-476
			if (source instanceof OriginTrackedMapPropertySource && !this.names.contains(source.getName())) {
				this.sources.addPropertySource(source);
				this.names.add(source.getName());
			}
		}

		@Override
		public Object getProperty(String name) {
			if (this.sources.containsProperty(name)) {
				return this.sources.getProperty(name);
			}
			return super.getProperty(name);
		}

		@Override
		public boolean containsProperty(String name) {
			if (this.sources.containsProperty(name)) {
				return true;
			}
			return super.containsProperty(name);
		}

		@Override
		public String[] getPropertyNames() {
			List<String> names = new ArrayList<>();
			names.addAll(Arrays.asList(this.sources.getPropertyNames()));
			names.addAll(Arrays.asList(super.getPropertyNames()));
			return names.toArray(new String[0]);
		}

		@Override
		public Origin getOrigin(String name) {
			return this.sources.getOrigin(name);
		}

	}

	private static class CloseContextOnFailureApplicationListener implements SmartApplicationListener {

		private final ConfigurableApplicationContext context;

		CloseContextOnFailureApplicationListener(ConfigurableApplicationContext context) {
			this.context = context;
		}

		@Override
		public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
			return ApplicationFailedEvent.class.isAssignableFrom(eventType);
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ApplicationFailedEvent) {
				this.context.close();
			}

		}

	}

}
