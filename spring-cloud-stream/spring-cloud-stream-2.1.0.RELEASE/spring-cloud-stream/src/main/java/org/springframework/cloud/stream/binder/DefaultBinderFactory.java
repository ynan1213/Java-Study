/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.binder;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.config.SpelExpressionConverterConfiguration;
import org.springframework.cloud.stream.reflection.GenericsUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


/**
 * Default {@link BinderFactory} implementation.
 *
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Soby Chacko
 */
public class DefaultBinderFactory implements BinderFactory, DisposableBean, ApplicationContextAware {

	private final Map<String, BinderConfiguration> binderConfigurations;

	private final Map<String, Entry<Binder<?, ?, ?>, ConfigurableApplicationContext>> binderInstanceCache = new HashMap<>();

	private volatile ConfigurableApplicationContext context;

	private final Map<String, String> defaultBinderForBindingTargetType = new HashMap<>();

	private Collection<Listener> listeners;

	private volatile String defaultBinder;

	private final BinderTypeRegistry binderTypeRegistry;

	public DefaultBinderFactory(Map<String, BinderConfiguration> binderConfigurations, BinderTypeRegistry binderTypeRegistry) {
		this.binderConfigurations = new HashMap<>(binderConfigurations);
		this.binderTypeRegistry = binderTypeRegistry;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext);
		this.context = (ConfigurableApplicationContext) applicationContext;
	}

	public void setDefaultBinder(String defaultBinder) {
		this.defaultBinder = defaultBinder;
	}

	public void setListeners(Collection<Listener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void destroy() {
		this.binderInstanceCache.values().stream().map(Entry::getValue).forEach(ConfigurableApplicationContext::close);
		this.defaultBinderForBindingTargetType.clear();
	}

	/**
	 *
	 * @param name 就是spring.cloud.stream.bindings.xxxx.binder配置的值，如果未配置，这里就为null
	 * @param bindingTargetType 一般就是@Input或者@Output方法的返回值，一般为MessageChannel类型
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public synchronized <T> Binder<T, ?, ?> getBinder(String name, Class<? extends T> bindingTargetType) {
		// 如果未指定binder，则获取全局默认binder，全局默认binder也有可能为空
		String binderName = StringUtils.hasText(name) ? name : this.defaultBinder;
		// 从容器中获取 Binder 类型的 bean，也就是说容器中Binder类型的bean优先级高
		Map<String, Binder> binders = this.context == null ? Collections.emptyMap() : this.context.getBeansOfType(Binder.class);
		Binder<T, ConsumerProperties, ProducerProperties> binder;
		if (StringUtils.hasText(binderName) && binders.containsKey(binderName)) {
			binder = (Binder<T, ConsumerProperties, ProducerProperties>) this.context.getBean(binderName);
		}
		else if (binders.size() == 1) {
			binder = binders.values().iterator().next();
		}
		else  if (binders.size() > 1) {
			throw new IllegalStateException("Multiple binders are available, however neither default nor " + "per-destination binder name is provided. Available binders are " + binders.keySet());
		}
		else {
			/*
			 * This is the fall back to the old bootstrap that relies on spring.binders.
			 */
			binder = this.doGetBinder(binderName, bindingTargetType);
		}
		return binder;
	}


	private <T> Binder<T, ConsumerProperties, ProducerProperties> doGetBinder(String name, Class<? extends T> bindingTargetType) {
		String configurationName;
		// Fall back to a default if no argument is provided
		// 什么情况下name为空？未配置spring.cloud.stream.bindings.xxx.binder 并且未指定全局默认binder（spring.cloud.stream.defaultBinder）
		if (StringUtils.isEmpty(name)) {
			Assert.notEmpty(this.binderConfigurations, "A default binder has been requested, but there is no binder available");
			if (!StringUtils.hasText(this.defaultBinder)) {
				Set<String> defaultCandidateConfigurations = new HashSet<>();
				for (Map.Entry<String, BinderConfiguration> binderConfigurationEntry : this.binderConfigurations.entrySet()) {
					if (binderConfigurationEntry.getValue().isDefaultCandidate()) {
						defaultCandidateConfigurations.add(binderConfigurationEntry.getKey());
					}
				}
				if (defaultCandidateConfigurations.size() == 1) {
					configurationName = defaultCandidateConfigurations.iterator().next();
					this.defaultBinderForBindingTargetType.put(bindingTargetType.getName(), configurationName);
				}
				else {
					List<String> candidatesForBindableType = new ArrayList<>();
					for (String defaultCandidateConfiguration : defaultCandidateConfigurations) {
						// rabbit环境这里就会返回 RabbitMessageChannelBinder
						Binder<Object, ?, ?> binderInstance = getBinderInstance(defaultCandidateConfiguration);
						// 返回实现的Binder接口的第一个泛型？？？
						Class<?> binderType = GenericsUtils.getParameterType(binderInstance.getClass(), Binder.class, 0);
						if (binderType.isAssignableFrom(bindingTargetType)) {
							candidatesForBindableType.add(defaultCandidateConfiguration);
						}
					}
					if (candidatesForBindableType.size() == 1) {
						configurationName = candidatesForBindableType.iterator().next();
						this.defaultBinderForBindingTargetType.put(bindingTargetType.getName(), configurationName);
					}
					else {
						String countMsg = (candidatesForBindableType.size() == 0) ? "are no binders" : "is more than one binder";
						throw new IllegalStateException(
								"A default binder has been requested, but there " + countMsg
								+ " available for '" + bindingTargetType.getName() + "' : "
								+ StringUtils.collectionToCommaDelimitedString(candidatesForBindableType)
								+ ", and no default binder has been set.");
					}
				}
			}
			else {
				configurationName = this.defaultBinder;
			}
		}
		else {
			configurationName = name;
		}
		Binder<T, ConsumerProperties, ProducerProperties> binderInstance = getBinderInstance(configurationName);
		Assert.state(verifyBinderTypeMatchesTarget(binderInstance, bindingTargetType), "The binder '" + configurationName + "' cannot bind a " + bindingTargetType.getName());
		return binderInstance;
	}

	/**
	 * Return true if the binder is a {@link PollableConsumerBinder} and the target type
	 * is a {@link PollableSource} and their generic types match (e.g. MessageHandler), OR
	 * if it's a {@link Binder} and the target matches the binder's generic type.
	 * @param binderInstance the binder.
	 * @param bindingTargetType the binding target type.
	 * @return true if the conditions match.
	 */
	private <T> boolean verifyBinderTypeMatchesTarget(Binder<T, ?, ?> binderInstance,
			Class<? extends T> bindingTargetType) {
		return (binderInstance instanceof PollableConsumerBinder
				&& GenericsUtils.checkCompatiblePollableBinder(binderInstance, bindingTargetType))
			|| GenericsUtils.getParameterType(binderInstance.getClass(), Binder.class, 0)
					.isAssignableFrom(bindingTargetType);
	}

	@SuppressWarnings("unchecked")
	private <T> Binder<T, ConsumerProperties, ProducerProperties> getBinderInstance(String configurationName) {
		if (!this.binderInstanceCache.containsKey(configurationName)) {
			BinderConfiguration binderConfiguration = this.binderConfigurations.get(configurationName);
			Assert.state(binderConfiguration != null, "Unknown binder configuration: " + configurationName);
			BinderType binderType = this.binderTypeRegistry.get(binderConfiguration.getBinderType());
			Assert.notNull(binderType, "Binder type " + binderConfiguration.getBinderType() + " is not defined");

			Map<String, String> binderProperties = new HashMap<>();
			this.flatten(null, binderConfiguration.getProperties(), binderProperties);

			// Convert all properties to arguments, so that they receive maximum
			// precedence
			ArrayList<String> args = new ArrayList<>();
			for (Map.Entry<String, String> property : binderProperties.entrySet()) {
				args.add(String.format("--%s=%s", property.getKey(), property.getValue()));
			}
			// Initialize the domain with a unique name based on the bootstrapping context
			// setting
			ConfigurableEnvironment environment = this.context != null ? this.context.getEnvironment() : null;
			String defaultDomain = environment != null ? environment.getProperty("spring.jmx.default-domain") : "";
			args.add("--spring.jmx.default-domain=" + defaultDomain + "binder." + configurationName);
			// Initializing SpringApplicationBuilder with SpelExpressionConverterConfiguration due to the fact that
			// infrastructure related configuration is not propagated in a multi binder scenario.
			// See this GH issue for more details: https://github.com/spring-cloud/spring-cloud-stream/issues/1412
			// and the associated PR: https://github.com/spring-cloud/spring-cloud-stream/pull/1413
            // 重新构造一个容器的意义是什么？
			// 个人猜测：项目配置了多个binder，因为每个binder都有自己的初始化参数，因为spring rabbit客户端的原因，会互相影响吧  ~~
			SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(SpelExpressionConverterConfiguration.class)
					.sources(binderType.getConfigurationClasses())
					.bannerMode(Mode.OFF)
					.logStartupInfo(false)
					.web(WebApplicationType.NONE);
			// If the environment is not customized and a main context is available, we will set the latter as parent.
			// This ensures that the defaults and user-defined customizations (e.g. custom connection factory beans)
			// are propagated to the binder context. If the environment is customized, then the binder context should
			// not inherit any beans from the parent
			// 也就是说如果不设置 environment ，就会设置父容器，进而读取到父容器的配置
			boolean useApplicationContextAsParent = binderProperties.isEmpty() && this.context != null;
			if (useApplicationContextAsParent) {
				springApplicationBuilder.parent(this.context);
			}
			// If the current application context is not set as parent and the environment is set,
			// provide the current context as an additional bean in the BeanFactory.
			if (environment != null && !useApplicationContextAsParent){
				springApplicationBuilder.initializers(new InitializerWithOuterContext(this.context));
			}

			if (environment != null && (useApplicationContextAsParent || binderConfiguration.isInheritEnvironment())) {
				StandardEnvironment binderEnvironment = new StandardEnvironment();
				binderEnvironment.merge(environment);
				springApplicationBuilder.environment(binderEnvironment);
			}
			/**
			 * rabbit会自动读取
			 * spring.rabbitmq.addresses=47.100.24.106
			 * spring.rabbitmq.port=5672
			 * spring.rabbitmq.virtual-host=my_vhost
			 * spring.rabbitmq.username=admin
			 * spring.rabbitmq.password=12345
			 */
			ConfigurableApplicationContext binderProducingContext = springApplicationBuilder.run(args.toArray(new String[args.size()]));

			Binder<T, ?, ?> binder = binderProducingContext.getBean(Binder.class);
			/*
			 * This will ensure that application defined errorChannel and other beans are accessible within binder's context
			 * (see https://github.com/spring-cloud/spring-cloud-stream/issues/1384)
			 */
			if (this.context != null && binder instanceof ApplicationContextAware) {
				((ApplicationContextAware)binder).setApplicationContext(this.context);
			}
			if (!CollectionUtils.isEmpty(this.listeners)) {
				for (Listener binderFactoryListener : listeners) {
					binderFactoryListener.afterBinderContextInitialized(configurationName, binderProducingContext);
				}
			}
			this.binderInstanceCache.put(configurationName, new SimpleImmutableEntry<>(binder, binderProducingContext));
		}
		return (Binder<T, ConsumerProperties, ProducerProperties>) this.binderInstanceCache.get(configurationName).getKey();
	}

	/**
	 * Ensures that nested properties are flattened (i.e., foo.bar=baz instead of foo={bar=baz})
	 */
	@SuppressWarnings("unchecked")
	private void flatten(String propertyName, Object value, Map<String, String> flattenedProperties) {
		if (value instanceof Map) {
			((Map<Object, Object>) value)
				.forEach((k, v) -> flatten((propertyName != null ? propertyName + "." : "") + k, v, flattenedProperties));
		}
		else {
			flattenedProperties.put(propertyName, value.toString());
		}
	}

	/**
	 * A listener that can be registered with the {@link DefaultBinderFactory} that allows
	 * the registration of additional configuration.
	 *
	 * @author Ilayaperumal Gopinathan
	 */
	public interface Listener {

		/**
		 * Applying additional capabilities to the binder after the binder context has
		 * been initialized.
		 *
		 * @param configurationName the binder configuration name
		 * @param binderContext the application context of the binder
		 */
		void afterBinderContextInitialized(String configurationName,
				ConfigurableApplicationContext binderContext);
	}

	private static class InitializerWithOuterContext implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		private final ApplicationContext context;

		InitializerWithOuterContext(ApplicationContext context) {
			this.context = context;
		}

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getBeanFactory().registerSingleton("outerContext", context);
		}
	}

}
