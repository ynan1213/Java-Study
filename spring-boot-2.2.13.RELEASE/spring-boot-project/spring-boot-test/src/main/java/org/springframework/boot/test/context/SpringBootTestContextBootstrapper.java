/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link TestContextBootstrapper} for Spring Boot. Provides support for
 * {@link SpringBootTest @SpringBootTest} and may also be used directly or subclassed.
 * Provides the following features over and above {@link DefaultTestContextBootstrapper}:
 * <ul>
 * <li>Uses {@link SpringBootContextLoader} as the
 * {@link #getDefaultContextLoaderClass(Class) default context loader}.</li>
 * <li>Automatically searches for a
 * {@link SpringBootConfiguration @SpringBootConfiguration} when required.</li>
 * <li>Allows custom {@link Environment} {@link #getProperties(Class)} to be defined.</li>
 * <li>Provides support for different {@link WebEnvironment webEnvironment} modes.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Madhura Bhave
 * @author Lorenzo Dee
 * @since 1.4.0
 * @see SpringBootTest
 * @see TestConfiguration
 */
public class SpringBootTestContextBootstrapper extends DefaultTestContextBootstrapper {

	private static final String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };

	private static final String REACTIVE_WEB_ENVIRONMENT_CLASS = "org.springframework."
			+ "web.reactive.DispatcherHandler";

	private static final String MVC_WEB_ENVIRONMENT_CLASS = "org.springframework.web.servlet.DispatcherServlet";

	private static final String JERSEY_WEB_ENVIRONMENT_CLASS = "org.glassfish.jersey.server.ResourceConfig";

	private static final String ACTIVATE_SERVLET_LISTENER = "org.springframework.test."
			+ "context.web.ServletTestExecutionListener.activateListener";

	private static final Log logger = LogFactory.getLog(SpringBootTestContextBootstrapper.class);

	@Override
	public TestContext buildTestContext() {
		TestContext context = super.buildTestContext();
		verifyConfiguration(context.getTestClass());
		WebEnvironment webEnvironment = getWebEnvironment(context.getTestClass());
		if (webEnvironment == WebEnvironment.MOCK && deduceWebApplicationType() == WebApplicationType.SERVLET) {
			context.setAttribute(ACTIVATE_SERVLET_LISTENER, true);
		}
		else if (webEnvironment != null && webEnvironment.isEmbedded()) {
			context.setAttribute(ACTIVATE_SERVLET_LISTENER, false);
		}
		return context;
	}

	@Override
	protected Set<Class<? extends TestExecutionListener>> getDefaultTestExecutionListenerClasses() {
		Set<Class<? extends TestExecutionListener>> listeners = super.getDefaultTestExecutionListenerClasses();
		List<DefaultTestExecutionListenersPostProcessor> postProcessors = SpringFactoriesLoader
				.loadFactories(DefaultTestExecutionListenersPostProcessor.class, getClass().getClassLoader());
		for (DefaultTestExecutionListenersPostProcessor postProcessor : postProcessors) {
			listeners = postProcessor.postProcessDefaultTestExecutionListeners(listeners);
		}
		return listeners;
	}

	@Override
	protected ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList) {
		// 获取 @SpringBootTest 注解的 classes 属性值
		Class<?>[] classes = getClasses(testClass);
		if (!ObjectUtils.isEmpty(classes)) {
			// 如果 classes属性不为空，设置进去，相当于 @ContextConfiguration(classes = RootConfig.class)
			for (ContextConfigurationAttributes configAttributes : configAttributesList) {
				addConfigAttributesClasses(configAttributes, classes);
			}
		}
		// 默认提供了 SpringBootContextLoader
		return super.resolveContextLoader(testClass, configAttributesList);
	}

	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
		return SpringBootContextLoader.class;
	}

	private void addConfigAttributesClasses(ContextConfigurationAttributes configAttributes, Class<?>[] classes) {
		List<Class<?>> combined = new ArrayList<>(Arrays.asList(classes));
		if (configAttributes.getClasses() != null) {
			combined.addAll(Arrays.asList(configAttributes.getClasses()));
		}
		configAttributes.setClasses(ClassUtils.toClassArray(combined));
	}

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		// 获取 @SpringBootTest 注解的 classes 属性值，如果为空，则读取测试类上有@SpringBootConfiguration注解，把测试类当做配置类
		Class<?>[] classes = getOrFindConfigurationClasses(mergedConfig);
		// 读取 @SpringBootTest 注解的 properties 属性值
		List<String> propertySourceProperties = getAndProcessPropertySourceProperties(mergedConfig);
		mergedConfig = createModifiedConfig(mergedConfig, classes, StringUtils.toStringArray(propertySourceProperties));
		WebEnvironment webEnvironment = getWebEnvironment(mergedConfig.getTestClass());
		if (webEnvironment != null && isWebEnvironmentSupported(mergedConfig)) {
			WebApplicationType webApplicationType = getWebApplicationType(mergedConfig);
			if (webApplicationType == WebApplicationType.SERVLET
					&& (webEnvironment.isEmbedded() || webEnvironment == WebEnvironment.MOCK)) {
				mergedConfig = new WebMergedContextConfiguration(mergedConfig, determineResourceBasePath(mergedConfig));
			}
			else if (webApplicationType == WebApplicationType.REACTIVE
					&& (webEnvironment.isEmbedded() || webEnvironment == WebEnvironment.MOCK)) {
				return new ReactiveWebMergedContextConfiguration(mergedConfig);
			}
		}
		return mergedConfig;
	}

	private WebApplicationType getWebApplicationType(MergedContextConfiguration configuration) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(
				TestPropertySourceUtils.convertInlinedPropertiesToMap(configuration.getPropertySourceProperties()));
		Binder binder = new Binder(source);
		return binder.bind("spring.main.web-application-type", Bindable.of(WebApplicationType.class))
				.orElseGet(this::deduceWebApplicationType);
	}

	private WebApplicationType deduceWebApplicationType() {
		if (ClassUtils.isPresent(REACTIVE_WEB_ENVIRONMENT_CLASS, null)
				&& !ClassUtils.isPresent(MVC_WEB_ENVIRONMENT_CLASS, null)
				&& !ClassUtils.isPresent(JERSEY_WEB_ENVIRONMENT_CLASS, null)) {
			return WebApplicationType.REACTIVE;
		}
		for (String className : WEB_ENVIRONMENT_CLASSES) {
			if (!ClassUtils.isPresent(className, null)) {
				return WebApplicationType.NONE;
			}
		}
		return WebApplicationType.SERVLET;
	}

	/**
	 * Determines the resource base path for web applications using the value of
	 * {@link WebAppConfiguration @WebAppConfiguration}, if any, on the test class of the
	 * given {@code configuration}. Defaults to {@code src/main/webapp} in its absence.
	 * @param configuration the configuration to examine
	 * @return the resource base path
	 * @since 2.1.6
	 */
	protected String determineResourceBasePath(MergedContextConfiguration configuration) {
		return MergedAnnotations.from(configuration.getTestClass(), SearchStrategy.TYPE_HIERARCHY)
				.get(WebAppConfiguration.class).getValue(MergedAnnotation.VALUE, String.class)
				.orElse("src/main/webapp");
	}

	private boolean isWebEnvironmentSupported(MergedContextConfiguration mergedConfig) {
		Class<?> testClass = mergedConfig.getTestClass();
		ContextHierarchy hierarchy = AnnotationUtils.getAnnotation(testClass, ContextHierarchy.class);
		if (hierarchy == null || hierarchy.value().length == 0) {
			return true;
		}
		ContextConfiguration[] configurations = hierarchy.value();
		return isFromConfiguration(mergedConfig, configurations[configurations.length - 1]);
	}

	private boolean isFromConfiguration(MergedContextConfiguration candidateConfig,
			ContextConfiguration configuration) {
		ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(candidateConfig.getTestClass(),
				configuration);
		Set<Class<?>> configurationClasses = new HashSet<>(Arrays.asList(attributes.getClasses()));
		for (Class<?> candidate : candidateConfig.getClasses()) {
			if (configurationClasses.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	protected Class<?>[] getOrFindConfigurationClasses(MergedContextConfiguration mergedConfig) {
		Class<?>[] classes = mergedConfig.getClasses();
		if (containsNonTestComponent(classes) || mergedConfig.hasLocations()) {
			return classes;
		}
		/**
		 * 遍历测试类的包以及子包，寻找带有@SpringBootConfiguration注解的类
		 * 1.没有找到，下面会报错。
		 * 2.如果找到多个，内部也会报错。
		 * 这就是为什么直接使用在测试类上使用@SpringBootTest不指定classes属性，也能读取的原因。
		 * 但是如果在/src/test下和启动类相同包路径也创建一个启动类，是会报错的，因为找到了两个。
		 * 但是如果/src/test下的启动类全限定类名和启动类一样，就没问题，因为类加载器只会读取到一个，并且/src/test下的优先级高
		 */
		Class<?> found = new AnnotatedClassFinder(SpringBootConfiguration.class)
				.findFromClass(mergedConfig.getTestClass());
		Assert.state(found != null, "Unable to find a @SpringBootConfiguration, you need to use "
				+ "@ContextConfiguration or @SpringBootTest(classes=...) with your test");
		logger.info("Found @SpringBootConfiguration " + found.getName() + " for test " + mergedConfig.getTestClass());
		return merge(found, classes);
	}

	private boolean containsNonTestComponent(Class<?>[] classes) {
		for (Class<?> candidate : classes) {
			if (!MergedAnnotations.from(candidate, SearchStrategy.INHERITED_ANNOTATIONS)
					.isPresent(TestConfiguration.class)) {
				return true;
			}
		}
		return false;
	}

	private Class<?>[] merge(Class<?> head, Class<?>[] existing) {
		Class<?>[] result = new Class<?>[existing.length + 1];
		result[0] = head;
		System.arraycopy(existing, 0, result, 1, existing.length);
		return result;
	}

	private List<String> getAndProcessPropertySourceProperties(MergedContextConfiguration mergedConfig) {
		List<String> propertySourceProperties = new ArrayList<>(
				Arrays.asList(mergedConfig.getPropertySourceProperties()));
		String differentiator = getDifferentiatorPropertySourceProperty();
		if (differentiator != null) {
			propertySourceProperties.add(differentiator);
		}
		processPropertySourceProperties(mergedConfig, propertySourceProperties);
		return propertySourceProperties;
	}

	/**
	 * Return a "differentiator" property to ensure that there is something to
	 * differentiate regular tests and bootstrapped tests. Without this property a cached
	 * context could be returned that wasn't created by this bootstrapper. By default uses
	 * the bootstrapper class as a property.
	 * @return the differentiator or {@code null}
	 */
	protected String getDifferentiatorPropertySourceProperty() {
		return getClass().getName() + "=true";
	}

	/**
	 * Post process the property source properties, adding or removing elements as
	 * required.
	 * @param mergedConfig the merged context configuration
	 * @param propertySourceProperties the property source properties to process
	 */
	protected void processPropertySourceProperties(MergedContextConfiguration mergedConfig,
			List<String> propertySourceProperties) {
		Class<?> testClass = mergedConfig.getTestClass();
		String[] properties = getProperties(testClass);
		if (!ObjectUtils.isEmpty(properties)) {
			// Added first so that inlined properties from @TestPropertySource take
			// precedence
			propertySourceProperties.addAll(0, Arrays.asList(properties));
		}
		if (getWebEnvironment(testClass) == WebEnvironment.RANDOM_PORT) {
			propertySourceProperties.add("server.port=0");
		}
	}

	/**
	 * Return the {@link WebEnvironment} type for this test or null if undefined.
	 * @param testClass the source test class
	 * @return the {@link WebEnvironment} or {@code null}
	 */
	protected WebEnvironment getWebEnvironment(Class<?> testClass) {
		SpringBootTest annotation = getAnnotation(testClass);
		return (annotation != null) ? annotation.webEnvironment() : null;
	}

	protected Class<?>[] getClasses(Class<?> testClass) {
		SpringBootTest annotation = getAnnotation(testClass);
		return (annotation != null) ? annotation.classes() : null;
	}

	protected String[] getProperties(Class<?> testClass) {
		SpringBootTest annotation = getAnnotation(testClass);
		return (annotation != null) ? annotation.properties() : null;
	}

	protected SpringBootTest getAnnotation(Class<?> testClass) {
		return MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS).get(SpringBootTest.class)
				.synthesize(MergedAnnotation::isPresent).orElse(null);
	}

	protected void verifyConfiguration(Class<?> testClass) {
		SpringBootTest springBootTest = getAnnotation(testClass);
		if (springBootTest != null && isListeningOnPort(springBootTest.webEnvironment()) && MergedAnnotations
				.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS).isPresent(WebAppConfiguration.class)) {
			throw new IllegalStateException("@WebAppConfiguration should only be used "
					+ "with @SpringBootTest when @SpringBootTest is configured with a "
					+ "mock web environment. Please remove @WebAppConfiguration or reconfigure @SpringBootTest.");
		}
	}

	private boolean isListeningOnPort(WebEnvironment webEnvironment) {
		return webEnvironment == WebEnvironment.DEFINED_PORT || webEnvironment == WebEnvironment.RANDOM_PORT;
	}

	/**
	 * Create a new {@link MergedContextConfiguration} with different classes.
	 * @param mergedConfig the source config
	 * @param classes the replacement classes
	 * @return a new {@link MergedContextConfiguration}
	 */
	protected final MergedContextConfiguration createModifiedConfig(MergedContextConfiguration mergedConfig,
			Class<?>[] classes) {
		return createModifiedConfig(mergedConfig, classes, mergedConfig.getPropertySourceProperties());
	}

	/**
	 * Create a new {@link MergedContextConfiguration} with different classes and
	 * properties.
	 * @param mergedConfig the source config
	 * @param classes the replacement classes
	 * @param propertySourceProperties the replacement properties
	 * @return a new {@link MergedContextConfiguration}
	 */
	protected final MergedContextConfiguration createModifiedConfig(MergedContextConfiguration mergedConfig,
			Class<?>[] classes, String[] propertySourceProperties) {
		Set<ContextCustomizer> contextCustomizers = new LinkedHashSet<>(mergedConfig.getContextCustomizers());
		contextCustomizers.add(new SpringBootTestArgs(mergedConfig.getTestClass()));
		contextCustomizers.add(new SpringBootTestWebEnvironment(mergedConfig.getTestClass()));
		return new MergedContextConfiguration(mergedConfig.getTestClass(), mergedConfig.getLocations(), classes,
				mergedConfig.getContextInitializerClasses(), mergedConfig.getActiveProfiles(),
				mergedConfig.getPropertySourceLocations(), propertySourceProperties, contextCustomizers,
				mergedConfig.getContextLoader(), getCacheAwareContextLoaderDelegate(), mergedConfig.getParent());
	}

}
