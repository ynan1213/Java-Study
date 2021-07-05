/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.redis.connection.jedis;

import static org.assertj.core.api.Assertions.*;

import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.AbstractConnectionIntegrationTests;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.RedisServer;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.test.util.MinimumRedisVersionRule;
import org.springframework.data.redis.test.util.RedisSentinelRule;
import org.springframework.data.redis.test.util.RelaxedJUnit4ClassRunner;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Christoph Strobl
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@RunWith(RelaxedJUnit4ClassRunner.class)
@ContextConfiguration("JedisConnectionIntegrationTests-context.xml")
public class JedisSentinelIntegrationTests extends AbstractConnectionIntegrationTests {

	private static final String MASTER_NAME = "mymaster";
	private static final RedisServer SENTINEL_0 = new RedisServer("127.0.0.1", 26379);
	private static final RedisServer SENTINEL_1 = new RedisServer("127.0.0.1", 26380);

	private static final RedisServer SLAVE_0 = new RedisServer("127.0.0.1", 6380);
	private static final RedisServer SLAVE_1 = new RedisServer("127.0.0.1", 6381);

	private static final RedisSentinelConfiguration SENTINEL_CONFIG = new RedisSentinelConfiguration() //
			.master(MASTER_NAME).sentinel(SENTINEL_0).sentinel(SENTINEL_1);

	public static @ClassRule RedisSentinelRule sentinelRule = RedisSentinelRule.forConfig(SENTINEL_CONFIG).oneActive();
	public @Rule MinimumRedisVersionRule minimumVersionRule = new MinimumRedisVersionRule();

	@Before
	public void setUp() {
		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(SENTINEL_CONFIG);
		jedisConnectionFactory.setClientName("jedis-client");
		jedisConnectionFactory.afterPropertiesSet();
		connectionFactory = jedisConnectionFactory;
		super.setUp();
	}

	@After
	public void tearDown() {
		super.tearDown();
		((JedisConnectionFactory) connectionFactory).destroy();
	}

	@Test
	@Ignore
	public void testScriptKill() throws Exception {
		super.testScriptKill();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnSingleError() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> connection.eval("return redis.call('expire','foo')", ReturnType.BOOLEAN, 0));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalArrayScriptError() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> super.testEvalArrayScriptError());
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalShaNotFound() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> connection.evalSha("somefakesha", ReturnType.VALUE, 2, "key1", "key2"));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalShaArrayError() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() -> super.testEvalShaArrayError());
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testRestoreBadData() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() -> super.testRestoreBadData());
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testRestoreExistingKey() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> super.testRestoreExistingKey());
	}

	@Test
	public void testExecWithoutMulti() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() -> super.testExecWithoutMulti());
	}

	@Test
	public void testErrorInTx() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() -> super.testErrorInTx());
	}

	@Test // DATAREDIS-330
	public void shouldReadMastersCorrectly() {

		List<RedisServer> servers = (List<RedisServer>) connectionFactory.getSentinelConnection().masters();
		assertThat(servers.size()).isEqualTo(1);
		assertThat(servers.get(0).getName()).isEqualTo(MASTER_NAME);
	}

	@Test // DATAREDIS-330
	public void shouldReadSlavesOfMastersCorrectly() {

		RedisSentinelConnection sentinelConnection = connectionFactory.getSentinelConnection();

		List<RedisServer> servers = (List<RedisServer>) sentinelConnection.masters();
		assertThat(servers.size()).isEqualTo(1);

		Collection<RedisServer> slaves = sentinelConnection.slaves(servers.get(0));
		assertThat(slaves.size()).isEqualTo(2);
		assertThat(slaves).contains(SLAVE_0, SLAVE_1);
	}

	@Test // DATAREDIS-552
	public void shouldSetClientName() {

		RedisSentinelConnection sentinelConnection = connectionFactory.getSentinelConnection();
		Jedis jedis = (Jedis) ReflectionTestUtils.getField(sentinelConnection, "jedis");

		assertThat(jedis.clientGetname()).isEqualTo("jedis-client");
	}

}
