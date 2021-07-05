/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.redis.listener;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.runners.model.Statement;

import org.springframework.data.redis.ObjectFactory;
import org.springframework.data.redis.Person;
import org.springframework.data.redis.PersonObjectFactory;
import org.springframework.data.redis.RawObjectFactory;
import org.springframework.data.redis.SettingsUtils;
import org.springframework.data.redis.StringObjectFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceTestClientResources;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.test.util.RedisClusterRule;

/**
 * @author Costin Leau
 * @author Jennifer Hickey
 * @author Mark Paluch
 */
public class PubSubTestParams {

	public static Collection<Object[]> testParams() {
		// create Jedis Factory
		ObjectFactory<String> stringFactory = new StringObjectFactory();
		ObjectFactory<Person> personFactory = new PersonObjectFactory();
		ObjectFactory<byte[]> rawFactory = new RawObjectFactory();

		JedisConnectionFactory jedisConnFactory = new JedisConnectionFactory();
		jedisConnFactory.setUsePool(true);
		jedisConnFactory.setPort(SettingsUtils.getPort());
		jedisConnFactory.setHostName(SettingsUtils.getHost());
		jedisConnFactory.setDatabase(2);

		jedisConnFactory.afterPropertiesSet();

		RedisTemplate<String, String> stringTemplate = new StringRedisTemplate(jedisConnFactory);
		RedisTemplate<String, Person> personTemplate = new RedisTemplate<>();
		personTemplate.setConnectionFactory(jedisConnFactory);
		personTemplate.afterPropertiesSet();
		RedisTemplate<byte[], byte[]> rawTemplate = new RedisTemplate<>();
		rawTemplate.setEnableDefaultSerializer(false);
		rawTemplate.setConnectionFactory(jedisConnFactory);
		rawTemplate.afterPropertiesSet();

		// add Lettuce
		LettuceConnectionFactory lettuceConnFactory = new LettuceConnectionFactory();
		lettuceConnFactory.setClientResources(LettuceTestClientResources.getSharedClientResources());
		lettuceConnFactory.setPort(SettingsUtils.getPort());
		lettuceConnFactory.setHostName(SettingsUtils.getHost());
		lettuceConnFactory.afterPropertiesSet();

		RedisTemplate<String, String> stringTemplateLtc = new StringRedisTemplate(lettuceConnFactory);
		RedisTemplate<String, Person> personTemplateLtc = new RedisTemplate<>();
		personTemplateLtc.setConnectionFactory(lettuceConnFactory);
		personTemplateLtc.afterPropertiesSet();
		RedisTemplate<byte[], byte[]> rawTemplateLtc = new RedisTemplate<>();
		rawTemplateLtc.setEnableDefaultSerializer(false);
		rawTemplateLtc.setConnectionFactory(lettuceConnFactory);
		rawTemplateLtc.afterPropertiesSet();

		Collection<Object[]> parameters = new ArrayList<>();
		parameters.add(new Object[] { stringFactory, stringTemplate });
		parameters.add(new Object[] { personFactory, personTemplate });
		parameters.add(new Object[] { stringFactory, stringTemplateLtc });
		parameters.add(new Object[] { personFactory, personTemplateLtc });
		parameters.add(new Object[] { rawFactory, rawTemplateLtc });

		if (clusterAvailable()) {

			RedisClusterConfiguration configuration = new RedisClusterConfiguration().clusterNode("127.0.0.1", 7379);

			// add Jedis
			JedisConnectionFactory jedisClusterFactory = new JedisConnectionFactory(configuration);
			jedisClusterFactory.afterPropertiesSet();

			RedisTemplate<String, String> jedisClusterStringTemplate = new StringRedisTemplate(jedisClusterFactory);

			// add Lettuce
			LettuceConnectionFactory lettuceClusterFactory = new LettuceConnectionFactory(configuration);
			lettuceClusterFactory.setClientResources(LettuceTestClientResources.getSharedClientResources());
			lettuceClusterFactory.afterPropertiesSet();

			RedisTemplate<String, String> lettuceClusterStringTemplate = new StringRedisTemplate(lettuceClusterFactory);

			parameters.add(new Object[] { stringFactory, jedisClusterStringTemplate });
			parameters.add(new Object[] { stringFactory, lettuceClusterStringTemplate });
		}

		return parameters;
	}

	private static boolean clusterAvailable() {

		try {
			new RedisClusterRule().apply(new Statement() {
				@Override
				public void evaluate() {

				}
			}, null).evaluate();
		} catch (Throwable throwable) {
			return false;
		}
		return true;
	}
}
