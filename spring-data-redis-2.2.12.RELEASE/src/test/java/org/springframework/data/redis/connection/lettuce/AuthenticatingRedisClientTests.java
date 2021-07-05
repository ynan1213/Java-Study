/*
 * Copyright 2013-2020 the original author or authors.
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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test of {@link AuthenticatingRedisClient}. Enable requirepass and comment out the @Ignore to run.
 *
 * @author Jennifer Hickey
 * @author Thomas Darimont
 * @author Christoph Strobl
 */
@Ignore("Redis must have requirepass set to run this test")
public class AuthenticatingRedisClientTests {

	private RedisClient client;

	@Before
	public void setUp() {
		client = new AuthenticatingRedisClient("localhost", "foo");
	}

	@After
	public void tearDown() {
		if (client != null) {
			client.shutdown();
		}
	}

	@Test
	public void connect() {
		StatefulRedisConnection<String, String> conn = client.connect();
		conn.sync().ping();
		conn.close();
	}

	@Test(expected = RedisException.class)
	public void connectWithInvalidPassword() {

		if (client != null) {
			client.shutdown();
		}

		RedisClient badClient = new AuthenticatingRedisClient("localhost", "notthepassword");
		badClient.connect();
	}

	@Test
	public void codecConnect() {
		StatefulRedisConnection<byte[], byte[]> conn = client.connect(LettuceConnection.CODEC);
		conn.sync().ping();
		conn.close();
	}

	@Test
	public void connectAsync() {
		StatefulRedisConnection<String, String> conn = client.connect();
		conn.sync().ping();
		conn.close();
	}

	@Test
	public void codecConnectAsync() {
		StatefulRedisConnection<byte[], byte[]> conn = client.connect(LettuceConnection.CODEC);
		conn.sync().ping();
		conn.close();
	}

	@Test
	public void connectPubSub() {
		StatefulRedisPubSubConnection<String, String> conn = client.connectPubSub();
		conn.sync().ping();
		conn.close();
	}

	@Test
	public void codecConnectPubSub() {
		StatefulRedisPubSubConnection<byte[], byte[]> conn = client.connectPubSub(LettuceConnection.CODEC);
		conn.sync().ping();
		conn.close();
	}

}
