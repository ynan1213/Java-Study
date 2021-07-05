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
package org.springframework.data.redis.connection.lettuce;

import static org.assertj.core.api.Assertions.*;

import io.lettuce.core.ReadFrom;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.data.redis.ConnectionFactoryTracker;
import org.springframework.data.redis.connection.AbstractConnectionIntegrationTests;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.RedisServer;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.test.util.MinimumRedisVersionRule;
import org.springframework.data.redis.test.util.RedisSentinelRule;

/**
 * Integration tests for Lettuce and Redis Sentinel interaction.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@RunWith(Parameterized.class)
public class LettuceSentinelIntegrationTests extends AbstractConnectionIntegrationTests {

	private static final String MASTER_NAME = "mymaster";
	private static final RedisServer SENTINEL_0 = new RedisServer("127.0.0.1", 26379);
	private static final RedisServer SENTINEL_1 = new RedisServer("127.0.0.1", 26380);

	private static final RedisServer SLAVE_0 = new RedisServer("127.0.0.1", 6380);
	private static final RedisServer SLAVE_1 = new RedisServer("127.0.0.1", 6381);

	private static final RedisSentinelConfiguration SENTINEL_CONFIG;
	static {

		SENTINEL_CONFIG = new RedisSentinelConfiguration() //
				.master(MASTER_NAME).sentinel(SENTINEL_0).sentinel(SENTINEL_1);

		SENTINEL_CONFIG.setDatabase(5);
	}

	public static @ClassRule RedisSentinelRule sentinelRule = RedisSentinelRule.forConfig(SENTINEL_CONFIG).oneActive();
	public @Rule MinimumRedisVersionRule minimumVersionRule = new MinimumRedisVersionRule();

	public LettuceSentinelIntegrationTests(LettuceConnectionFactory connectionFactory, String displayName) {

		this.connectionFactory = connectionFactory;
		ConnectionFactoryTracker.add(connectionFactory);
	}

	@Parameters(name = "{1}")
	public static List<Object[]> parameters() {

		List<Object[]> parameters = new ArrayList<>();

		LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(SENTINEL_CONFIG);
		lettuceConnectionFactory.setClientResources(LettuceTestClientResources.getSharedClientResources());
		lettuceConnectionFactory.setShareNativeConnection(false);
		lettuceConnectionFactory.setShutdownTimeout(0);
		lettuceConnectionFactory.afterPropertiesSet();

		LettuceConnectionFactory pooledConnectionFactory = new LettuceConnectionFactory(SENTINEL_CONFIG,
				LettucePoolingClientConfiguration.builder()
						.clientResources(LettuceTestClientResources.getSharedClientResources()).build());
		pooledConnectionFactory.setShareNativeConnection(false);
		pooledConnectionFactory.afterPropertiesSet();

		parameters.add(new Object[] { lettuceConnectionFactory, "Sentinel" });
		parameters.add(new Object[] { pooledConnectionFactory, "Sentinel/Pooled" });

		return parameters;
	}

	@After
	public void tearDown() {

		try {

			// since we use more than one db we're required to flush them all
			connection.flushAll();
		} catch (Exception e) {
			// Connection may be closed in certain cases, like after pub/sub
			// tests
		}
		connection.close();
	}

	@AfterClass
	public static void afterClass() {
		ConnectionFactoryTracker.cleanUp();
	}

	@Test // DATAREDIS-348
	public void shouldReadMastersCorrectly() {

		List<RedisServer> servers = (List<RedisServer>) connectionFactory.getSentinelConnection().masters();
		assertThat(servers.size()).isEqualTo(1);
		assertThat(servers.get(0).getName()).isEqualTo(MASTER_NAME);
	}

	@Test // DATAREDIS-842, DATAREDIS-973
	public void shouldUseSpecifiedDatabase() {

		RedisConnection connection = connectionFactory.getConnection();

		connection.flushAll();
		connection.set("foo".getBytes(), "bar".getBytes());
		connection.close();

		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
		connectionFactory.setClientResources(LettuceTestClientResources.getSharedClientResources());
		connectionFactory.setShutdownTimeout(0);
		connectionFactory.setShareNativeConnection(false);
		connectionFactory.setDatabase(5);
		connectionFactory.afterPropertiesSet();

		RedisConnection directConnection = connectionFactory.getConnection();
		assertThat(directConnection.exists("foo".getBytes())).isTrue();
		directConnection.select(0);

		assertThat(directConnection.exists("foo".getBytes())).isFalse();
		directConnection.close();
		connectionFactory.destroy();
	}

	@Test // DATAREDIS-973
	public void reactiveShouldUseSpecifiedDatabase() {

		RedisConnection connection = connectionFactory.getConnection();

		connection.flushAll();
		connection.set("foo".getBytes(), "bar".getBytes());
		connection.close();

		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
		connectionFactory.setClientResources(LettuceTestClientResources.getSharedClientResources());
		connectionFactory.setShutdownTimeout(0);
		connectionFactory.setShareNativeConnection(false);
		connectionFactory.setDatabase(5);
		connectionFactory.afterPropertiesSet();

		LettuceReactiveRedisConnection reactiveConnection = connectionFactory.getReactiveConnection();

		reactiveConnection.keyCommands().exists(ByteBuffer.wrap("foo".getBytes())) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		reactiveConnection.close();
		connectionFactory.destroy();
	}

	@Test // DATAREDIS-348
	public void shouldReadSlavesOfMastersCorrectly() {

		RedisSentinelConnection sentinelConnection = connectionFactory.getSentinelConnection();

		List<RedisServer> servers = (List<RedisServer>) sentinelConnection.masters();
		assertThat(servers.size()).isEqualTo(1);

		Collection<RedisServer> slaves = sentinelConnection.slaves(servers.get(0));
		assertThat(slaves.size()).isEqualTo(2);
		assertThat(slaves).contains(SLAVE_0, SLAVE_1);
	}

	@Test // DATAREDIS-462
	public void factoryWorksWithoutClientResources() {

		LettuceConnectionFactory factory = new LettuceConnectionFactory(SENTINEL_CONFIG);
		factory.setShutdownTimeout(0);
		factory.afterPropertiesSet();

		ConnectionFactoryTracker.add(factory);

		StringRedisConnection connection = new DefaultStringRedisConnection(factory.getConnection());

		try {
			assertThat(connection.ping()).isEqualTo("PONG");
		} finally {
			connection.close();
		}
	}

	@Test // DATAREDIS-576
	public void connectionAppliesClientName() {

		LettuceClientConfiguration clientName = LettuceClientConfiguration.builder()
				.clientResources(LettuceTestClientResources.getSharedClientResources()).clientName("clientName").build();

		LettuceConnectionFactory factory = new LettuceConnectionFactory(SENTINEL_CONFIG, clientName);
		factory.afterPropertiesSet();

		ConnectionFactoryTracker.add(factory);

		StringRedisConnection connection = new DefaultStringRedisConnection(factory.getConnection());

		try {
			assertThat(connection.getClientName()).isEqualTo("clientName");
		} finally {
			connection.close();
		}
	}

	@Test // DATAREDIS-580
	public void factoryWithReadFromMasterSettings() {

		LettuceConnectionFactory factory = new LettuceConnectionFactory(SENTINEL_CONFIG,
				LettuceTestClientConfiguration.builder().readFrom(ReadFrom.MASTER).build());
		factory.afterPropertiesSet();

		ConnectionFactoryTracker.add(factory);

		StringRedisConnection connection = new DefaultStringRedisConnection(factory.getConnection());

		try {
			assertThat(connection.ping()).isEqualTo("PONG");
			assertThat(connection.info().getProperty("role")).isEqualTo("master");
		} finally {
			connection.close();
		}
	}

	@Test // DATAREDIS-580
	public void factoryWithReadFromSlaveSettings() {

		LettuceConnectionFactory factory = new LettuceConnectionFactory(SENTINEL_CONFIG,
				LettuceTestClientConfiguration.builder().readFrom(ReadFrom.SLAVE).build());
		factory.afterPropertiesSet();

		ConnectionFactoryTracker.add(factory);

		StringRedisConnection connection = new DefaultStringRedisConnection(factory.getConnection());

		try {
			assertThat(connection.ping()).isEqualTo("PONG");
			assertThat(connection.info().getProperty("role")).isEqualTo("slave");
		} finally {
			connection.close();
		}
	}

	@Test // DATAREDIS-580
	public void factoryUsesMasterSlaveConnections() {

		LettuceClientConfiguration configuration = LettuceTestClientConfiguration.builder().readFrom(ReadFrom.SLAVE)
				.build();

		LettuceConnectionFactory factory = new LettuceConnectionFactory(SENTINEL_CONFIG, configuration);
		factory.afterPropertiesSet();

		RedisConnection connection = factory.getConnection();

		try {
			assertThat(connection.ping()).isEqualTo("PONG");
			assertThat(connection.info().getProperty("role")).isEqualTo("slave");
		} finally {
			connection.close();
		}

		factory.destroy();
	}
}
