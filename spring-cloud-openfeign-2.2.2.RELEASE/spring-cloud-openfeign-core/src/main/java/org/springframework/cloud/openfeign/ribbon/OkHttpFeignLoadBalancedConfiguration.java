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

package org.springframework.cloud.openfeign.ribbon;

import feign.Client;
import feign.okhttp.OkHttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.clientconfig.OkHttpFeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
/**
 * <dependency>
 *     <groupId>io.github.openfeign</groupId>
 *     <artifactId>feign-okhttp</artifactId>
 * </dependency>
 * 需要主动引入该依赖才会生效
 */
@ConditionalOnClass(OkHttpClient.class)
@ConditionalOnProperty("feign.okhttp.enabled")
@Import(OkHttpFeignConfiguration.class)
class OkHttpFeignLoadBalancedConfiguration {

	@Bean
	@ConditionalOnMissingBean(Client.class)
	public Client feignClient(CachingSpringLoadBalancerFactory cachingFactory, SpringClientFactory clientFactory, okhttp3.OkHttpClient okHttpClient) {
		OkHttpClient delegate = new OkHttpClient(okHttpClient);
		return new LoadBalancerFeignClient(delegate, cachingFactory, clientFactory);
	}

}
