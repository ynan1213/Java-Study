/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.openfeign;

import java.util.Map;
import java.util.Objects;

import feign.Client;
import feign.Contract;
import feign.ExceptionPropagationPolicy;
import feign.Feign;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.Target.HardCodedTarget;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 * @author Venil Noronha
 * @author Eko Kurniawan Khannedy
 * @author Gregor Zurowski
 */
class FeignClientFactoryBean implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

	/***********************************
	 * WARNING! Nothing in this class should be @Autowired. It causes NPEs because of some lifecycle race condition.
	 ***********************************/

	private Class<?> type;

	private String name;

	private String url;

	private String contextId;

	private String path;

	private boolean decode404;

	private ApplicationContext applicationContext;

	private Class<?> fallback = void.class;

	private Class<?> fallbackFactory = void.class;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.contextId, "Context id must be set");
		Assert.hasText(this.name, "Name must be set");
	}

	protected Feign.Builder feign(FeignContext context) {
		// 每个client容器都有一个FeignLoggerFactory对象
		// 因为每个client的 FeignClientsConfiguration 都会注入一个，注意：如果父容器有了该对象，子容器是不会创建了的，因为@ConditionalOnMissingBean注解同样会生效
		FeignLoggerFactory loggerFactory = get(context, FeignLoggerFactory.class);

		// 默认情况下父容器和子容器都不会有logger对象，DefaultFeignLoggerFactory在这里创建一个slf4j的Logger对象
		Logger logger = loggerFactory.create(this.type);

		// 每个client容器都有一个Feign.Builder对象，同样Encoder、Decoder、Contract对象也是每个client容器都有一个默认的
		// @formatter:off
		Feign.Builder builder = get(context, Feign.Builder.class)
				.logger(logger)
				.encoder(get(context, Encoder.class))
				.decoder(get(context, Decoder.class))
				.contract(get(context, Contract.class));
		// @formatter:on
		// 注意里面的配置顺序
		configureFeign(context, builder);
		return builder;
	}

	/**
	 *  通过这里可以看出，有三个地方可以配置Feign
	 *  	①：java config，也就是 @EnableFeignClients(defaultConfiguration) 和 @FeignClient(configuration)，
	 *  		注意：这里的两个类不要注入相同的bean，否则会抛异常；
	 *  	②：application.properties(.yml)配置文件全局默认配置，配置属性feign.client.default-config指定默认值(defult)
	 *		③：application.properties(.yml)配置文件局部配置，指定@FeignClient#name局部配置
	 *
	 *	优先级如何？根据 feign.client.default-to-properties(默认为true)
	 * 		如果为true：③ > ② > ①
	 * 		如果为false：① > ③ > ②
	 */
	protected void configureFeign(FeignContext context, Feign.Builder builder) {
		FeignClientProperties properties = this.applicationContext.getBean(FeignClientProperties.class);
		if (properties != null) {
			if (properties.isDefaultToProperties()) {
				configureUsingConfiguration(context, builder);
				configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
				configureUsingProperties(properties.getConfig().get(this.contextId), builder);
			}
			else {
				configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
				configureUsingProperties(properties.getConfig().get(this.contextId), builder);
				configureUsingConfiguration(context, builder);
			}
		}
		else {
			configureUsingConfiguration(context, builder);
		}
	}

	protected void configureUsingConfiguration(FeignContext context, Feign.Builder builder) {
		Logger.Level level = getOptional(context, Logger.Level.class);
		if (level != null) {
			builder.logLevel(level);
		}

		// 每个client默认都会注入一个：NEVER_RETRY
		Retryer retryer = getOptional(context, Retryer.class);
		if (retryer != null) {
			builder.retryer(retryer);
		}
		ErrorDecoder errorDecoder = getOptional(context, ErrorDecoder.class);
		if (errorDecoder != null) {
			builder.errorDecoder(errorDecoder);
		}
		// 如果没有自定义，则注入的是父容器中 FeignRibbonClientAutoConfiguration 配置类注入的全局 Options
		// 默认：connectTimeout=10，readTimeout=60
		Request.Options options = getOptional(context, Request.Options.class);
		if (options != null) {
			builder.options(options);
		}
		Map<String, RequestInterceptor> requestInterceptors = context.getInstances(this.contextId, RequestInterceptor.class);
		if (requestInterceptors != null) {
			builder.requestInterceptors(requestInterceptors.values());
		}
		QueryMapEncoder queryMapEncoder = getOptional(context, QueryMapEncoder.class);
		if (queryMapEncoder != null) {
			builder.queryMapEncoder(queryMapEncoder);
		}
		if (this.decode404) {
			builder.decode404();
		}
		ExceptionPropagationPolicy exceptionPropagationPolicy = getOptional(context, ExceptionPropagationPolicy.class);
		if (exceptionPropagationPolicy != null) {
			builder.exceptionPropagationPolicy(exceptionPropagationPolicy);
		}
	}

	protected void configureUsingProperties(FeignClientProperties.FeignClientConfiguration config, Feign.Builder builder) {
		if (config == null) {
			return;
		}

		if (config.getLoggerLevel() != null) {
			builder.logLevel(config.getLoggerLevel());
		}

		if (config.getConnectTimeout() != null && config.getReadTimeout() != null) {
			builder.options(new Request.Options(config.getConnectTimeout(), config.getReadTimeout()));
		}

		if (config.getRetryer() != null) {
			Retryer retryer = getOrInstantiate(config.getRetryer());
			builder.retryer(retryer);
		}

		if (config.getErrorDecoder() != null) {
			ErrorDecoder errorDecoder = getOrInstantiate(config.getErrorDecoder());
			builder.errorDecoder(errorDecoder);
		}

		if (config.getRequestInterceptors() != null && !config.getRequestInterceptors().isEmpty()) {
			// this will add request interceptor to builder, not replace existing
			for (Class<RequestInterceptor> bean : config.getRequestInterceptors()) {
				RequestInterceptor interceptor = getOrInstantiate(bean);
				builder.requestInterceptor(interceptor);
			}
		}

		if (config.getDecode404() != null) {
			if (config.getDecode404()) {
				builder.decode404();
			}
		}

		if (Objects.nonNull(config.getEncoder())) {
			builder.encoder(getOrInstantiate(config.getEncoder()));
		}

		if (Objects.nonNull(config.getDecoder())) {
			builder.decoder(getOrInstantiate(config.getDecoder()));
		}

		if (Objects.nonNull(config.getContract())) {
			builder.contract(getOrInstantiate(config.getContract()));
		}

		if (Objects.nonNull(config.getExceptionPropagationPolicy())) {
			builder.exceptionPropagationPolicy(config.getExceptionPropagationPolicy());
		}
	}

	private <T> T getOrInstantiate(Class<T> tClass) {
		try {
			return this.applicationContext.getBean(tClass);
		}
		catch (NoSuchBeanDefinitionException e) {
			return BeanUtils.instantiateClass(tClass);
		}
	}

	protected <T> T get(FeignContext context, Class<T> type) {
		T instance = context.getInstance(this.contextId, type);
		if (instance == null) {
			throw new IllegalStateException("No bean found of type " + type + " for " + this.contextId);
		}
		return instance;
	}

	protected <T> T getOptional(FeignContext context, Class<T> type) {
		return context.getInstance(this.contextId, type);
	}

	protected <T> T loadBalance(Feign.Builder builder, FeignContext context, HardCodedTarget<T> target) {
		/**
		 * spring.factories 注入了 FeignRibbonClientAutoConfiguration 类，在该类上 @Import 三个client实现
		 * 根据配置属性的不同会有不同的生效，默认是 DefaultFeignLoadBalancedConfiguration
		 * 因为该对象是在父context中，所以每个@FeignClient共享同一个client
		 */
		Client client = getOptional(context, Client.class);

		/**
		 * 这个是ribbon和SpringCloud loadBalancer整合之后默认的client，
		 */
		LoadBalancerClient loadBalancerClient = getOptional(context, LoadBalancerClient.class);

		if (client != null) {
			builder.client(client);
			// Targeter 在父容器中，由 FeignAutoConfiguration 类注入
			Targeter targeter = get(context, Targeter.class);
			return targeter.target(this, builder, context, target);
		}

		throw new IllegalStateException("No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-netflix-ribbon?");
	}

	@Override
	public Object getObject() throws Exception {
		return getTarget();
	}

	/**
	 * @param <T> the target type of the Feign client
	 * @return a {@link Feign} client created with the specified data and the context
	 * information
	 */
	<T> T getTarget() {
		// 如果有多个，这里会报错
		FeignContext context = this.applicationContext.getBean(FeignContext.class);
		Feign.Builder builder = feign(context);

		// 如果未配置url，则进入if，生成有负载均衡功能的代理类
		if (!StringUtils.hasText(this.url)) {
			if (!this.name.startsWith("http")) {
				this.url = "http://" + this.name;
			}
			else {
				this.url = this.name;
			}
			// 拼接上 path，path起通用前缀的作用
			this.url += cleanPath();
			return (T) loadBalance(builder, context, new HardCodedTarget<>(this.type, this.name, this.url));
		}

		// 如果 url 不以 http:// 开头，则拼接上
		if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
			this.url = "http://" + this.url;
		}
		// 拼接上 path，path起通用前缀的作用
		String url = this.url + cleanPath();
		/**
		 * spring.factories 注入了 FeignRibbonClientAutoConfiguration 类，在该类上 @Import 三个client实现
		 * 根据配置属性的不同会有不同的生效，默认是 DefaultFeignLoadBalancedConfiguration
		 * 因为该对象是在父context中，所以每个@FeignClient共享同一个client
		 */
		Client client = getOptional(context, Client.class);

		// 如果指定了url，则不走注册中心的负载均衡
		if (client != null) {
			if (client instanceof LoadBalancerFeignClient) {
				// not load balancing because we have a url, but ribbon is on the classpath, so unwrap
				client = ((LoadBalancerFeignClient) client).getDelegate();
			}
			if (client instanceof FeignBlockingLoadBalancerClient) {
				// not load balancing because we have a url,
				// but Spring Cloud LoadBalancer is on the classpath, so unwrap
				client = ((FeignBlockingLoadBalancerClient) client).getDelegate();
			}
			builder.client(client);
		}
		// Targeter 在父容器中，由 FeignAutoConfiguration 类注入
		// 由于spring-cloud-openfeign-core模块默认已经引入了Hystrix 的依赖，所以这里默认是HystrixTargeter；
		Targeter targeter = get(context, Targeter.class);
		return (T) targeter.target(this, builder, context, new HardCodedTarget<>(this.type, this.name, url));
	}

	private String cleanPath() {
		String path = this.path.trim();
		if (StringUtils.hasLength(path)) {
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
		}
		return path;
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public Class<?> getType() {
		return this.type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContextId() {
		return this.contextId;
	}

	public void setContextId(String contextId) {
		this.contextId = contextId;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isDecode404() {
		return this.decode404;
	}

	public void setDecode404(boolean decode404) {
		this.decode404 = decode404;
	}

	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.applicationContext = context;
	}

	public Class<?> getFallback() {
		return this.fallback;
	}

	public void setFallback(Class<?> fallback) {
		this.fallback = fallback;
	}

	public Class<?> getFallbackFactory() {
		return this.fallbackFactory;
	}

	public void setFallbackFactory(Class<?> fallbackFactory) {
		this.fallbackFactory = fallbackFactory;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FeignClientFactoryBean that = (FeignClientFactoryBean) o;
		return Objects.equals(this.applicationContext, that.applicationContext)
				&& this.decode404 == that.decode404
				&& Objects.equals(this.fallback, that.fallback)
				&& Objects.equals(this.fallbackFactory, that.fallbackFactory)
				&& Objects.equals(this.name, that.name)
				&& Objects.equals(this.path, that.path)
				&& Objects.equals(this.type, that.type)
				&& Objects.equals(this.url, that.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.applicationContext, this.decode404, this.fallback, this.fallbackFactory, this.name, this.path, this.type, this.url);
	}

	@Override
	public String toString() {
		return new StringBuilder("FeignClientFactoryBean{").append("type=")
				.append(this.type).append(", ").append("name='").append(this.name)
				.append("', ").append("url='").append(this.url).append("', ")
				.append("path='").append(this.path).append("', ").append("decode404=")
				.append(this.decode404).append(", ").append("applicationContext=")
				.append(this.applicationContext).append(", ").append("fallback=")
				.append(this.fallback).append(", ").append("fallbackFactory=")
				.append(this.fallbackFactory).append("}").toString();
	}

}
