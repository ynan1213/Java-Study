/*
 * Copyright 2015-2020 the original author or authors.
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
package org.springframework.data.redis.connection;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class RedisClusterConfigurationUnitTests {

	static final String HOST_AND_PORT_1 = "127.0.0.1:123";
	static final String HOST_AND_PORT_2 = "localhost:456";
	static final String HOST_AND_PORT_3 = "localhost:789";
	static final String HOST_AND_NO_PORT = "localhost";

	@Test // DATAREDIS-315
	public void shouldCreateRedisClusterConfigurationCorrectly() {

		RedisClusterConfiguration config = new RedisClusterConfiguration(Collections.singleton(HOST_AND_PORT_1));

		assertThat(config.getClusterNodes().size()).isEqualTo(1);
		assertThat(config.getClusterNodes()).contains(new RedisNode("127.0.0.1", 123));
		assertThat(config.getMaxRedirects()).isNull();
	}

	@Test // DATAREDIS-315
	public void shouldCreateRedisClusterConfigurationCorrectlyGivenMultipleHostAndPortStrings() {

		RedisClusterConfiguration config = new RedisClusterConfiguration(
				new HashSet<>(Arrays.asList(HOST_AND_PORT_1,
				HOST_AND_PORT_2, HOST_AND_PORT_3)));

		assertThat(config.getClusterNodes().size()).isEqualTo(3);
		assertThat(config.getClusterNodes()).contains(new RedisNode("127.0.0.1", 123), new RedisNode("localhost", 456),
				new RedisNode("localhost", 789));
	}

	@Test // DATAREDIS-315
	public void shouldThrowExecptionOnInvalidHostAndPortString() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new RedisClusterConfiguration(Collections.singleton(HOST_AND_NO_PORT)));
	}

	@Test // DATAREDIS-315
	public void shouldThrowExceptionWhenListOfHostAndPortIsNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new RedisClusterConfiguration(Collections.<String> singleton(null)));
	}

	@Test // DATAREDIS-315
	public void shouldNotFailWhenListOfHostAndPortIsEmpty() {

		RedisClusterConfiguration config = new RedisClusterConfiguration(Collections.<String> emptySet());

		assertThat(config.getClusterNodes().size()).isEqualTo(0);
	}

	@Test // DATAREDIS-315
	public void shouldThrowExceptionGivenNullPropertySource() {
		assertThatIllegalArgumentException().isThrownBy(() -> new RedisClusterConfiguration((PropertySource<?>) null));
	}

	@Test // DATAREDIS-315
	public void shouldNotFailWhenGivenPropertySourceNotContainingRelevantProperties() {

		RedisClusterConfiguration config = new RedisClusterConfiguration(new MockPropertySource());

		assertThat(config.getMaxRedirects()).isNull();
		assertThat(config.getClusterNodes().size()).isEqualTo(0);
	}

	@Test // DATAREDIS-315
	public void shouldBeCreatedCorrecltyGivenValidPropertySourceWithSingleHostPort() {

		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.redis.cluster.nodes", HOST_AND_PORT_1);
		propertySource.setProperty("spring.redis.cluster.max-redirects", "5");

		RedisClusterConfiguration config = new RedisClusterConfiguration(propertySource);

		assertThat(config.getMaxRedirects()).isEqualTo(5);
		assertThat(config.getClusterNodes()).contains(new RedisNode("127.0.0.1", 123));
	}

	@Test // DATAREDIS-315
	public void shouldBeCreatedCorrecltyGivenValidPropertySourceWithMultipleHostPort() {

		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.redis.cluster.nodes",
				StringUtils.collectionToCommaDelimitedString(Arrays.asList(HOST_AND_PORT_1, HOST_AND_PORT_2, HOST_AND_PORT_3)));
		propertySource.setProperty("spring.redis.cluster.max-redirects", "5");

		RedisClusterConfiguration config = new RedisClusterConfiguration(propertySource);

		assertThat(config.getMaxRedirects()).isEqualTo(5);
		assertThat(config.getClusterNodes()).contains(new RedisNode("127.0.0.1", 123), new RedisNode("localhost", 456),
				new RedisNode("localhost", 789));
	}

}
