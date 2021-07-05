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
package org.springframework.data.redis.connection;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.data.redis.SpinBarrier.*;
import static org.springframework.data.redis.connection.BitFieldSubCommands.*;
import static org.springframework.data.redis.connection.BitFieldSubCommands.BitFieldIncrBy.Overflow.*;
import static org.springframework.data.redis.connection.BitFieldSubCommands.BitFieldType.*;
import static org.springframework.data.redis.connection.ClusterTestVariables.*;
import static org.springframework.data.redis.connection.RedisGeoCommands.DistanceUnit.*;
import static org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs.*;
import static org.springframework.data.redis.core.ScanOptions.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range.Bound;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.ConnectionFactoryTracker;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.RedisTestProfileValueSource;
import org.springframework.data.redis.RedisVersionUtils;
import org.springframework.data.redis.TestCondition;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisListCommands.Position;
import org.springframework.data.redis.connection.RedisStringCommands.BitOperation;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.connection.RedisZSetCommands.Aggregate;
import org.springframework.data.redis.connection.RedisZSetCommands.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands.Range;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.connection.SortParameters.Order;
import org.springframework.data.redis.connection.StringRedisConnection.StringTuple;
import org.springframework.data.redis.connection.ValueEncoding.RedisValueEncoding;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.test.util.HexStringUtils;
import org.springframework.data.redis.test.util.RedisClientRule;
import org.springframework.data.redis.test.util.RedisDriver;
import org.springframework.data.redis.test.util.WithRedisDriver;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;

/**
 * Base test class for AbstractConnection integration tests
 *
 * @author Costin Leau
 * @author Jennifer Hickey
 * @author Christoph Strobl
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Dejan Jankov
 * @author Andrey Shlykov
 */
@ProfileValueSourceConfiguration(RedisTestProfileValueSource.class)
public abstract class AbstractConnectionIntegrationTests {

	private static final Point POINT_ARIGENTO = new Point(13.583333, 37.316667);
	private static final Point POINT_CATANIA = new Point(15.087269, 37.502669);
	private static final Point POINT_PALERMO = new Point(13.361389, 38.115556);

	private static final GeoLocation<String> ARIGENTO = new GeoLocation<>("arigento", POINT_ARIGENTO);
	private static final GeoLocation<String> CATANIA = new GeoLocation<>("catania", POINT_CATANIA);
	private static final GeoLocation<String> PALERMO = new GeoLocation<>("palermo", POINT_PALERMO);

	protected StringRedisConnection connection;
	protected RedisSerializer<Object> serializer = RedisSerializer.java();
	protected RedisSerializer<String> stringSerializer = RedisSerializer.string();

	private static final byte[] EMPTY_ARRAY = new byte[0];

	protected List<Object> actual = new ArrayList<>();

	@Autowired protected RedisConnectionFactory connectionFactory;

	protected RedisConnection byteConnection;

	public @Rule RedisClientRule clientRule = new RedisClientRule() {
		public RedisConnectionFactory getConnectionFactory() {
			return connectionFactory;
		}
	};

	@Before
	public void setUp() {

		ConnectionFactoryTracker.add(connectionFactory);

		byteConnection = connectionFactory.getConnection();
		connection = new DefaultStringRedisConnection(byteConnection);
		((DefaultStringRedisConnection) connection).setDeserializePipelineAndTxResults(true);
		initConnection();
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
		connection = null;
	}

	@AfterClass
	public static void cleanUp() {
		ConnectionFactoryTracker.cleanUp();
	}

	@Test
	public void testSelect() {
		// Make sure this doesn't throw Exception
		connection.select(1);
	}

	@Test
	@IfProfileValue(name = "runLongTests", value = "true")
	public void testExpire() throws Exception {

		actual.add(connection.set("exp", "true"));
		actual.add(connection.expire("exp", 1));

		verifyResults(Arrays.asList(true, true));
		assertThat(waitFor(new KeyExpired("exp"), 3000l)).isTrue();
	}

	@Test
	@IfProfileValue(name = "runLongTests", value = "true")
	public void testExpireAt() throws Exception {

		actual.add(connection.set("exp2", "true"));
		actual.add(connection.expireAt("exp2", System.currentTimeMillis() / 1000 + 1));
		verifyResults(Arrays.asList(true, true));
		assertThat(waitFor(new KeyExpired("exp2"), 3000l)).isTrue();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testPExpire() {

		actual.add(connection.set("exp", "true"));
		actual.add(connection.pExpire("exp", 100));
		verifyResults(Arrays.asList(true, true));
		assertThat(waitFor(new KeyExpired("exp"), 1000l)).isTrue();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testPExpireKeyNotExists() {
		actual.add(connection.pExpire("nonexistent", 100));
		verifyResults(Arrays.asList(new Object[] { false }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testPExpireAt() {

		actual.add(connection.set("exp2", "true"));
		actual.add(connection.pExpireAt("exp2", System.currentTimeMillis() + 200));
		verifyResults(Arrays.asList(true, true));
		assertThat(waitFor(new KeyExpired("exp2"), 1000l)).isTrue();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testPExpireAtKeyNotExists() {
		actual.add(connection.pExpireAt("nonexistent", System.currentTimeMillis() + 200));
		verifyResults(Arrays.asList(new Object[] { false }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testScriptLoadEvalSha() {
		getResults();
		String sha1 = connection.scriptLoad("return KEYS[1]");
		initConnection();
		actual.add(connection.evalSha(sha1, ReturnType.VALUE, 2, "key1", "key2"));
		assertThat(new String((byte[]) getResults().get(0))).isEqualTo("key1");
	}

	@SuppressWarnings("unchecked")
	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalShaArrayStrings() {
		getResults();
		String sha1 = connection.scriptLoad("return {KEYS[1],ARGV[1]}");
		initConnection();
		actual.add(connection.evalSha(sha1, ReturnType.MULTI, 1, "key1", "arg1"));
		List<Object> results = getResults();
		List<byte[]> scriptResults = (List<byte[]>) results.get(0);
		assertThat(Arrays.asList(new Object[] { new String(scriptResults.get(0)), new String(scriptResults.get(1)) }))
				.isEqualTo(Arrays.asList(new Object[] { "key1", "arg1" }));
	}

	@SuppressWarnings("unchecked")
	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalShaArrayBytes() {
		getResults();
		byte[] sha1 = connection.scriptLoad("return {KEYS[1],ARGV[1]}").getBytes();
		initConnection();
		actual.add(byteConnection.evalSha(sha1, ReturnType.MULTI, 1, "key1".getBytes(), "arg1".getBytes()));
		List<Object> results = getResults();
		List<byte[]> scriptResults = (List<byte[]>) results.get(0);
		assertThat(Arrays.asList(new Object[] { new String(scriptResults.get(0)), new String(scriptResults.get(1)) }))
				.isEqualTo(Arrays.asList(new Object[] { "key1", "arg1" }));
	}

	@Test(expected = RedisSystemException.class)
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalShaArrayError() {
		connection.evalSha("notasha", ReturnType.MULTI, 1, "key1", "arg1");
		getResults();
	}

	@Test(expected = RedisSystemException.class)
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalShaNotFound() {
		connection.evalSha("somefakesha", ReturnType.VALUE, 2, "key1", "key2");
		getResults();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnString() {
		actual.add(connection.eval("return KEYS[1]", ReturnType.VALUE, 1, "foo"));
		byte[] result = (byte[]) getResults().get(0);
		assertThat(new String(result)).isEqualTo("foo");
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnNumber() {
		actual.add(connection.eval("return 10", ReturnType.INTEGER, 0));
		verifyResults(Arrays.asList(new Object[] { 10l }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnSingleOK() {
		actual.add(connection.eval("return redis.call('set','abc','ghk')", ReturnType.STATUS, 0));
		assertThat(getResults()).isEqualTo(Arrays.asList(new Object[] { "OK" }));
	}

	@Test(expected = RedisSystemException.class)
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnSingleError() {
		connection.eval("return redis.call('expire','foo')", ReturnType.BOOLEAN, 0);
		getResults();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnFalse() {
		actual.add(connection.eval("return false", ReturnType.BOOLEAN, 0));
		verifyResults(Arrays.asList(new Object[] { false }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnTrue() {
		actual.add(connection.eval("return true", ReturnType.BOOLEAN, 0));
		verifyResults(Arrays.asList(new Object[] { true }));
	}

	@SuppressWarnings("unchecked")
	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnArrayStrings() {
		actual.add(connection.eval("return {KEYS[1],ARGV[1]}", ReturnType.MULTI, 1, "foo", "bar"));
		List<byte[]> result = (List<byte[]>) getResults().get(0);
		assertThat(Arrays.asList(new Object[] { new String(result.get(0)), new String(result.get(1)) }))
				.isEqualTo(Arrays.asList(new Object[] { "foo", "bar" }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnArrayNumbers() {
		actual.add(connection.eval("return {1,2}", ReturnType.MULTI, 1, "foo", "bar"));
		verifyResults(Arrays.asList(new Object[] { Arrays.asList(new Object[] { 1l, 2l }) }));
	}

	@Test(expected = RedisSystemException.class)
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalArrayScriptError() {
		// Syntax error
		connection.eval("return {1,2", ReturnType.MULTI, 1, "foo", "bar");
		getResults();
	}

	@SuppressWarnings("unchecked")
	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnArrayOKs() {
		actual.add(connection.eval("return { redis.call('set','abc','ghk'),  redis.call('set','abc','lfdf')}",
				ReturnType.MULTI, 0));
		List<byte[]> result = (List<byte[]>) getResults().get(0);
		assertThat(Arrays.asList(new Object[] { new String(result.get(0)), new String(result.get(1)) }))
				.isEqualTo(Arrays.asList(new Object[] { "OK", "OK" }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnArrayFalses() {
		actual.add(connection.eval("return { false, false}", ReturnType.MULTI, 0));
		verifyResults(Arrays.asList(new Object[] { Arrays.asList(new Object[] { null, null }) }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testEvalReturnArrayTrues() {
		actual.add(connection.eval("return { true, true}", ReturnType.MULTI, 0));
		verifyResults(Arrays.asList(new Object[] { Arrays.asList(new Object[] { 1l, 1l }) }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testScriptExists() {
		getResults();
		String sha1 = connection.scriptLoad("return 'foo'");
		initConnection();
		actual.add(connection.scriptExists(sha1, "98777234"));
		verifyResults(Arrays.asList(new Object[] { Arrays.asList(new Object[] { true, false }) }));
	}

	@Test
	@IfProfileValue(name = "runLongTests", value = "true")
	public void testScriptKill() throws Exception {
		getResults();
		assumeTrue(RedisVersionUtils.atLeast("2.6", byteConnection));
		initConnection();
		final AtomicBoolean scriptDead = new AtomicBoolean(false);
		final CountDownLatch sync = new CountDownLatch(1);
		Thread th = new Thread(() -> {
			DefaultStringRedisConnection conn2 = new DefaultStringRedisConnection(connectionFactory.getConnection());
			try {
				sync.countDown();
				conn2.eval("local time=1 while time < 10000000000 do time=time+1 end", ReturnType.BOOLEAN, 0);
			} catch (DataAccessException e) {
				scriptDead.set(true);
			}
			conn2.close();
		});
		th.start();
		sync.await(2, TimeUnit.SECONDS);
		Thread.sleep(200);
		connection.scriptKill();
		getResults();
		assertThat(waitFor(() -> scriptDead.get(), 3000l)).isTrue();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testScriptFlush() {
		getResults();
		String sha1 = connection.scriptLoad("return KEYS[1]");
		connection.scriptFlush();
		initConnection();
		actual.add(connection.scriptExists(sha1));
		verifyResults(Arrays.asList(new Object[] { Arrays.asList(new Object[] { false }) }));
	}

	@Test
	@IfProfileValue(name = "runLongTests", value = "true")
	public void testPersist() throws Exception {

		actual.add(connection.set("exp3", "true"));
		actual.add(connection.expire("exp3", 30));
		actual.add(connection.persist("exp3"));
		actual.add(connection.ttl("exp3"));
		verifyResults(Arrays.asList(true, true, true, -1L));
	}

	@Test
	@IfProfileValue(name = "runLongTests", value = "true")
	public void testSetEx() throws Exception {

		actual.add(connection.setEx("expy", 1l, "yep"));
		actual.add(connection.get("expy"));

		verifyResults(Arrays.asList(true, "yep"));
		assertThat(waitFor(new KeyExpired("expy"), 2500l)).isTrue();
	}

	@Test // DATAREDIS-271
	@IfProfileValue(name = "runLongTests", value = "true")
	public void testPsetEx() throws Exception {

		actual.add(connection.pSetEx("expy", 500L, "yep"));
		actual.add(connection.get("expy"));

		verifyResults(Arrays.asList(true, "yep"));
		assertThat(waitFor(new KeyExpired("expy"), 2500L)).isTrue();
	}

	@Test
	@IfProfileValue(name = "runLongTests", value = "true")
	public void testBRPopTimeout() throws Exception {
		actual.add(connection.bRPop(1, "alist"));
		verifyResults(Arrays.asList(new Object[] { null }));
	}

	@Test
	@IfProfileValue(name = "runLongTests", value = "true")
	public void testBLPopTimeout() throws Exception {
		actual.add(connection.bLPop(1, "alist"));
		verifyResults(Arrays.asList(new Object[] { null }));
	}

	@Test
	@IfProfileValue(name = "runLongTests", value = "true")
	public void testBRPopLPushTimeout() throws Exception {
		actual.add(connection.bRPopLPush(1, "alist", "foo"));
		verifyResults(Arrays.asList(new Object[] { null }));
	}

	@Test
	public void testSetAndGet() {

		String key = "foo";
		String value = "blabla";

		actual.add(connection.set(key.getBytes(), value.getBytes()));
		actual.add(connection.get(key));
		verifyResults(Arrays.asList(true, value));
	}

	@Test
	public void testPingPong() throws Exception {
		actual.add(connection.ping());
		verifyResults(new ArrayList<>(Collections.singletonList("PONG")));
	}

	@Test
	public void testBitSet() throws Exception {
		String key = "bitset-test";
		actual.add(connection.setBit(key, 0, true));
		actual.add(connection.setBit(key, 0, false));
		actual.add(connection.setBit(key, 1, true));
		actual.add(connection.getBit(key, 0));
		actual.add(connection.getBit(key, 1));
		verifyResults(Arrays.asList(new Object[] { false, true, false, false, true }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testBitCount() {
		String key = "bitset-test";
		actual.add(connection.setBit(key, 0, false));
		actual.add(connection.setBit(key, 1, true));
		actual.add(connection.setBit(key, 2, true));
		actual.add(connection.bitCount(key));
		verifyResults(Arrays.asList(new Object[] { false, false, false, 2L }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testBitCountInterval() {

		actual.add(connection.set("mykey", "foobar"));
		actual.add(connection.bitCount("mykey", 1, 1));
		verifyResults(Arrays.asList(Boolean.TRUE, 6L));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testBitCountNonExistentKey() {
		actual.add(connection.bitCount("mykey"));
		verifyResults(new ArrayList<>(Collections.singletonList(0l)));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testBitOpAnd() {

		actual.add(connection.set("key1", "foo"));
		actual.add(connection.set("key2", "bar"));
		actual.add(connection.bitOp(BitOperation.AND, "key3", "key1", "key2"));
		actual.add(connection.get("key3"));
		verifyResults(Arrays.asList(Boolean.TRUE, Boolean.TRUE, 3L, "bab"));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testBitOpOr() {

		actual.add(connection.set("key1", "foo"));
		actual.add(connection.set("key2", "ugh"));
		actual.add(connection.bitOp(BitOperation.OR, "key3", "key1", "key2"));
		actual.add(connection.get("key3"));
		verifyResults(Arrays.asList(Boolean.TRUE, Boolean.TRUE, 3l, "woo"));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testBitOpXOr() {

		actual.add(connection.set("key1", "abcd"));
		actual.add(connection.set("key2", "efgh"));
		actual.add(connection.bitOp(BitOperation.XOR, "key3", "key1", "key2"));
		verifyResults(Arrays.asList(Boolean.TRUE, Boolean.TRUE, 4L));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testBitOpNot() {

		actual.add(connection.set("key1", "abcd"));
		actual.add(connection.bitOp(BitOperation.NOT, "key3", "key1"));
		verifyResults(Arrays.asList(Boolean.TRUE, 4L));
	}

	@Test(expected = UnsupportedOperationException.class)
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testBitOpNotMultipleSources() {

		actual.add(connection.set("key1", "abcd"));
		actual.add(connection.set("key2", "efgh"));
		actual.add(connection.bitOp(BitOperation.NOT, "key3", "key1", "key2"));
		getResults();
	}

	@Test
	public void testInfo() throws Exception {

		actual.add(connection.info());
		List<Object> results = getResults();
		Properties info = (Properties) results.get(0);
		assertThat(info.size() >= 5).as("at least 5 settings should be present").isTrue();
		String version = info.getProperty("redis_version");
		assertThat(version).isNotNull();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testInfoBySection() throws Exception {
		actual.add(connection.info("server"));
		List<Object> results = getResults();
		Properties info = (Properties) results.get(0);
		assertThat(info.size() >= 5).as("at least 5 settings should be present").isTrue();
		String version = info.getProperty("redis_version");
		assertThat(version).isNotNull();
	}

	@Test
	@Ignore("DATAREDIS-525")
	public void testNullKey() throws Exception {
		try {
			connection.decr((String) null);
			fail("Decrement should fail with null key");
		} catch (Exception ex) {
			// expected
		}
	}

	@Test
	@Ignore("DATAREDIS-525")
	public void testNullValue() throws Exception {

		byte[] key = UUID.randomUUID().toString().getBytes();
		connection.append(key, EMPTY_ARRAY);
		try {
			connection.append(key, null);
			fail("Append should fail with null value");
		} catch (DataAccessException ex) {
			// expected
		}
	}

	@Test
	@Ignore("DATAREDIS-525")
	public void testHashNullKey() throws Exception {

		byte[] key = UUID.randomUUID().toString().getBytes();
		try {
			connection.hExists(key, null);
			fail("hExists should fail with null key");
		} catch (DataAccessException ex) {
			// expected
		}
	}

	@Test
	@Ignore("DATAREDIS-525")
	public void testHashNullValue() throws Exception {
		byte[] key = UUID.randomUUID().toString().getBytes();
		byte[] field = "random".getBytes();

		connection.hSet(key, field, EMPTY_ARRAY);
		try {
			connection.hSet(key, field, null);
			fail("hSet should fail with null value");
		} catch (DataAccessException ex) {
			// expected
		}
	}

	@Test
	public void testNullSerialization() throws Exception {
		String[] keys = new String[] { "~", "[" };
		actual.add(connection.mGet(keys));
		verifyResults(Arrays.asList(new Object[] { Arrays.asList(new String[] { null, null }) }));
		StringRedisTemplate stringTemplate = new StringRedisTemplate(connectionFactory);
		List<String> multiGet = stringTemplate.opsForValue().multiGet(Arrays.asList(keys));
		assertThat(multiGet).isEqualTo(Arrays.asList(new String[] { null, null }));
	}

	@Test
	public void testAppend() {
		actual.add(connection.set("a", "b"));
		actual.add(connection.append("a", "c"));
		actual.add(connection.get("a"));
		verifyResults(Arrays.asList(new Object[] { Boolean.TRUE, 2l, "bc" }));
	}

	@Test
	public void testPubSubWithNamedChannels() throws Exception {
		final String expectedChannel = "channel1";
		final String expectedMessage = "msg";
		final BlockingDeque<Message> messages = new LinkedBlockingDeque<>();

		MessageListener listener = (message, pattern) -> {
			messages.add(message);
		};

		Thread th = new Thread(() -> {
			// sync to let the registration happen
			waitFor(connection::isSubscribed, 2000);
			try {
				Thread.sleep(500);
			} catch (InterruptedException o_O) {}

			// open a new connection
			RedisConnection connection2 = connectionFactory.getConnection();
			connection2.publish(expectedChannel.getBytes(), expectedMessage.getBytes());
			connection2.close();
			// In some clients, unsubscribe happens async of message
			// receipt, so not all
			// messages may be received if unsubscribing now.
			// Connection.close in teardown
			// will take care of unsubscribing.
			if (!(ConnectionUtils.isAsync(connectionFactory))) {
				connection.getSubscription().unsubscribe();
			}
		});

		th.start();
		connection.subscribe(listener, expectedChannel.getBytes());
		// Not all providers block on subscribe, give some time for messages to
		// be received
		Message message = messages.poll(5, TimeUnit.SECONDS);
		assertThat(message).isNotNull();
		assertThat(new String(message.getBody())).isEqualTo(expectedMessage);
		assertThat(new String(message.getChannel())).isEqualTo(expectedChannel);
	}

	@Test
	public void testPubSubWithPatterns() throws Exception {
		final String expectedPattern = "channel*";
		final String expectedMessage = "msg";
		final BlockingDeque<Message> messages = new LinkedBlockingDeque<>();

		final MessageListener listener = (message, pattern) -> {
			assertThat(new String(pattern)).isEqualTo(expectedPattern);
			messages.add(message);
		};

		Thread th = new Thread(() -> {
			// sync to let the registration happen
			waitFor(connection::isSubscribed, 2000);

			try {
				Thread.sleep(500);
			} catch (InterruptedException o_O) {}

			// open a new connection
			RedisConnection connection2 = connectionFactory.getConnection();
			connection2.publish("channel1".getBytes(), expectedMessage.getBytes());
			connection2.publish("channel2".getBytes(), expectedMessage.getBytes());
			connection2.close();
			// In some clients, unsubscribe happens async of message
			// receipt, so not all
			// messages may be received if unsubscribing now.
			// Connection.close in teardown
			// will take care of unsubscribing.
			if (!(ConnectionUtils.isAsync(connectionFactory))) {
				connection.getSubscription().pUnsubscribe(expectedPattern.getBytes());
			}
		});

		th.start();
		connection.pSubscribe(listener, expectedPattern);
		// Not all providers block on subscribe (Lettuce does not), give some
		// time for messages to be received
		Message message = messages.poll(5, TimeUnit.SECONDS);
		assertThat(message).isNotNull();
		assertThat(new String(message.getBody())).isEqualTo(expectedMessage);
		message = messages.poll(5, TimeUnit.SECONDS);
		assertThat(message).isNotNull();
		assertThat(new String(message.getBody())).isEqualTo(expectedMessage);
	}

	@Test(expected = DataAccessException.class)
	public void exceptionExecuteNative() throws Exception {
		connection.execute("set", "foo");
		connection.execute("ZadD", getClass() + "#foo\t0.90\titem");
		getResults();
	}

	@Test
	public void testExecute() {

		actual.add(connection.set("foo", "bar"));
		actual.add(connection.execute("GET", "foo"));

		assertThat(stringSerializer.deserialize((byte[]) getResults().get(1))).isEqualTo("bar");
	}

	@Test
	public void testExecuteNoArgs() {

		actual.add(connection.execute("PING"));
		List<Object> results = getResults();
		assertThat(stringSerializer.deserialize((byte[]) results.get(0))).isEqualTo("PONG");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMultiExec() throws Exception {

		connection.multi();
		connection.set("key", "value");
		connection.get("key");
		actual.add(connection.exec());
		List<Object> results = getResults();
		List<Object> execResults = (List<Object>) results.get(0);
		assertThat(execResults).isEqualTo(Arrays.asList(true, "value"));
		assertThat(connection.get("key")).isEqualTo("value");
	}

	@Test
	public void testMultiAlreadyInTx() throws Exception {
		connection.multi();
		// Ensure it's OK to call multi twice
		testMultiExec();
	}

	@Test(expected = RedisSystemException.class)
	public void testExecWithoutMulti() {
		connection.exec();
		getResults();
	}

	@Test(expected = RedisSystemException.class)
	public void testErrorInTx() {
		connection.multi();
		connection.set("foo", "bar");
		// Try to do a list op on a value
		connection.lPop("foo");
		connection.exec();
		getResults();
	}

	@Test
	public void testMultiDiscard() throws Exception {
		DefaultStringRedisConnection conn2 = new DefaultStringRedisConnection(connectionFactory.getConnection());
		conn2.set("testitnow", "willdo");
		connection.multi();
		connection.set("testitnow2", "notok");
		connection.discard();
		actual.add(connection.get("testitnow"));
		List<Object> results = getResults();
		assertThat(results).isEqualTo(Arrays.asList(new String[] { "willdo" }));
		initConnection();
		// Ensure we can run a new tx after discarding previous one
		testMultiExec();
	}

	@Test
	public void testWatch() throws Exception {

		actual.add(connection.set("testitnow", "willdo"));

		connection.watch("testitnow".getBytes());
		// Give some time for watch to be asynch executed in extending tests
		Thread.sleep(500);
		DefaultStringRedisConnection conn2 = new DefaultStringRedisConnection(connectionFactory.getConnection());
		conn2.set("testitnow", "something");
		conn2.close();
		connection.multi();
		connection.set("testitnow", "somethingelse");
		actual.add(connection.exec());
		actual.add(connection.get("testitnow"));

		verifyResults(Arrays.asList(new Object[] { true, null, "something" }));
	}

	@Test
	public void testUnwatch() throws Exception {

		actual.add(connection.set("testitnow", "willdo"));

		connection.watch("testitnow".getBytes());
		connection.unwatch();

		connection.multi();

		// Give some time for unwatch to be asynch executed
		Thread.sleep(100);
		DefaultStringRedisConnection conn2 = new DefaultStringRedisConnection(connectionFactory.getConnection());
		conn2.set("testitnow", "something");

		connection.set("testitnow", "somethingelse");
		connection.get("testitnow");
		actual.add(connection.exec());

		verifyResults(Arrays.asList(true, Arrays.asList(true, "somethingelse")));
	}

	@Test
	public void testSort() {
		actual.add(connection.rPush("sortlist", "foo"));
		actual.add(connection.rPush("sortlist", "bar"));
		actual.add(connection.rPush("sortlist", "baz"));
		actual.add(connection.sort("sortlist", new DefaultSortParameters(null, Order.ASC, true)));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, 3l, Arrays.asList(new String[] { "bar", "baz", "foo" }) }));
	}

	@Test
	public void testSortStore() {
		actual.add(connection.rPush("sortlist", "foo"));
		actual.add(connection.rPush("sortlist", "bar"));
		actual.add(connection.rPush("sortlist", "baz"));
		actual.add(connection.sort("sortlist", new DefaultSortParameters(null, Order.ASC, true), "newlist"));
		actual.add(connection.lRange("newlist", 0, 9));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, 3l, 3l, Arrays.asList(new String[] { "bar", "baz", "foo" }) }));
	}

	@Test
	public void testSortNullParams() {
		actual.add(connection.rPush("sortlist", "5"));
		actual.add(connection.rPush("sortlist", "2"));
		actual.add(connection.rPush("sortlist", "3"));
		actual.add(connection.sort("sortlist", null));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, 3l, Arrays.asList(new String[] { "2", "3", "5" }) }));
	}

	@Test
	public void testSortStoreNullParams() {
		actual.add(connection.rPush("sortlist", "9"));
		actual.add(connection.rPush("sortlist", "3"));
		actual.add(connection.rPush("sortlist", "5"));
		actual.add(connection.sort("sortlist", null, "newlist"));
		actual.add(connection.lRange("newlist", 0, 9));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, 3l, 3l, Arrays.asList(new String[] { "3", "5", "9" }) }));
	}

	@Test
	public void testDbSize() {

		actual.add(connection.set("dbparam", "foo"));
		actual.add(connection.dbSize());
		assertThat((Long) getResults().get(1) > 0).isTrue();
	}

	@Test
	public void testFlushDb() {
		connection.flushDb();
		actual.add(connection.dbSize());
		verifyResults(Arrays.asList(new Object[] { 0l }));
	}

	@SuppressWarnings("unchecked")
	@Test // DATAREDIS-661
	public void testGetConfig() {
		actual.add(connection.getConfig("*"));
		Properties config = (Properties) getResults().get(0);
		assertThat(!config.isEmpty()).isTrue();
	}

	@Test
	public void testEcho() {
		actual.add(connection.echo("Hello World"));
		verifyResults(Arrays.asList(new Object[] { "Hello World" }));
	}

	@Test
	public void testExists() {

		actual.add(connection.set("existent", "true"));
		actual.add(connection.exists("existent"));
		actual.add(connection.exists("nonexistent"));
		verifyResults(Arrays.asList(true, true, false));
	}

	@Test // DATAREDIS-529
	public void testExistsWithMultipleKeys() {

		actual.add(connection.set("exist-1", "true"));
		actual.add(connection.set("exist-2", "true"));
		actual.add(connection.set("exist-3", "true"));

		actual.add(connection.exists("exist-1", "exist-2", "exist-3", "nonexistent"));

		verifyResults(Arrays.asList(new Object[] { true, true, true, 3L }));
	}

	@Test // DATAREDIS-529
	public void testExistsWithMultipleKeysNoneExists() {

		actual.add(connection.exists("no-exist-1", "no-exist-2"));

		verifyResults(Arrays.asList(new Object[] { 0L }));
	}

	@Test // DATAREDIS-529
	public void testExistsSameKeyMultipleTimes() {

		actual.add(connection.set("existent", "true"));

		actual.add(connection.exists("existent", "existent"));

		verifyResults(Arrays.asList(true, 2L));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeys() throws Exception {

		actual.add(connection.set("keytest", "true"));
		actual.add(connection.keys("key*"));
		assertThat(((Collection<String>) getResults().get(1)).contains("keytest")).isTrue();
	}

	@Test
	public void testRandomKey() {

		actual.add(connection.set("some", "thing"));
		actual.add(connection.randomKey());
		List<Object> results = getResults();
		assertThat(results.get(1)).isNotNull();
	}

	@Test
	public void testRename() {

		actual.add(connection.set("renametest", "testit"));
		connection.rename("renametest", "newrenametest");
		actual.add(connection.get("newrenametest"));
		actual.add(connection.exists("renametest"));
		verifyResults(Arrays.asList(true, "testit", false));
	}

	@Test
	public void testRenameNx() {

		actual.add(connection.set("nxtest", "testit"));
		actual.add(connection.renameNX("nxtest", "newnxtest"));
		actual.add(connection.get("newnxtest"));
		actual.add(connection.exists("nxtest"));
		verifyResults(Arrays.asList(true, true, "testit", false));
	}

	@Test
	public void testTtl() {
		actual.add(connection.set("whatup", "yo"));
		actual.add(connection.ttl("whatup"));
		verifyResults(Arrays.asList(true, -1L));
	}

	@Test // DATAREDIS-526
	public void testTtlWithTimeUnit() {

		actual.add(connection.set("whatup", "yo"));
		actual.add(connection.expire("whatup", 10));
		actual.add(connection.ttl("whatup", TimeUnit.MILLISECONDS));

		List<Object> results = getResults();

		assertThat((Long) results.get(2) > TimeUnit.SECONDS.toMillis(5)).isTrue();
		assertThat((Long) results.get(2) <= TimeUnit.SECONDS.toMillis(10)).isTrue();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testPTtlNoExpire() {

		actual.add(connection.set("whatup", "yo"));
		actual.add(connection.pTtl("whatup"));
		verifyResults(Arrays.asList(true, -1L));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testPTtl() {

		actual.add(connection.set("whatup", "yo"));
		actual.add(connection.pExpire("whatup", TimeUnit.SECONDS.toMillis(10)));
		actual.add(connection.pTtl("whatup"));

		List<Object> results = getResults();

		assertThat((Long) results.get(2) > -1).isTrue();
	}

	@Test // DATAREDIS-526
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testPTtlWithTimeUnit() {

		actual.add(connection.set("whatup", "yo"));
		actual.add(connection.pExpire("whatup", TimeUnit.MINUTES.toMillis(10)));
		actual.add(connection.pTtl("whatup", TimeUnit.SECONDS));

		List<Object> results = getResults();

		assertThat((Long) results.get(2) > TimeUnit.MINUTES.toSeconds(9)).isTrue();
		assertThat((Long) results.get(2) <= TimeUnit.MINUTES.toSeconds(10)).isTrue();
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testDumpAndRestore() {

		connection.set("testing", "12");
		actual.add(connection.dump("testing".getBytes()));
		List<Object> results = getResults();
		initConnection();

		actual.add(connection.del("testing"));
		actual.add((connection.get("testing")));
		connection.restore("testing".getBytes(), 0, (byte[]) results.get(results.size() - 1));
		actual.add(connection.get("testing"));

		verifyResults(Arrays.asList(new Object[] { 1L, null, "12" }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testDumpNonExistentKey() {
		actual.add(connection.dump("fakey".getBytes()));
		verifyResults(Arrays.asList(new Object[] { null }));
	}

	@Test(expected = RedisSystemException.class)
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testRestoreBadData() {
		// Use something other than dump-specific serialization
		connection.restore("testing".getBytes(), 0, "foo".getBytes());
		getResults();
	}

	@Test(expected = RedisSystemException.class)
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testRestoreExistingKey() {

		actual.add(connection.set("testing", "12"));
		actual.add(connection.dump("testing".getBytes()));
		List<Object> results = getResults();
		initConnection();
		connection.restore("testing".getBytes(), 0, (byte[]) results.get(1));
		getResults();
	}

	@Test // DATAREDIS-696
	@IfProfileValue(name = "redisVersion", value = "3.0+")
	public void testRestoreExistingKeyWithReplaceOption() {

		actual.add(connection.set("testing", "12"));
		actual.add(connection.dump("testing".getBytes()));
		actual.add(connection.set("testing", "21"));
		connection.restore("testing".getBytes(), 0, (byte[]) getResults().get(1), true);

		initConnection();
		actual.add(connection.get("testing"));
		verifyResults(Arrays.asList(new Object[] { "12" }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testRestoreTtl() {

		actual.add(connection.set("testing", "12"));
		actual.add(connection.dump("testing".getBytes()));

		List<Object> results = getResults();
		initConnection();
		actual.add(connection.del("testing"));
		actual.add(connection.get("testing"));
		connection.restore("testing".getBytes(), 100l, (byte[]) results.get(1));
		verifyResults(Arrays.asList(1l, null));
		assertThat(waitFor(new KeyExpired("testing"), 400l)).isTrue();
	}

	@Test
	public void testDel() {

		actual.add(connection.set("testing", "123"));
		actual.add(connection.del("testing"));
		actual.add(connection.exists("testing"));
		verifyResults(Arrays.asList(true, 1L, false));
	}

	@Test // DATAREDIS-693
	@IfProfileValue(name = "redisVersion", value = "4.0+")
	public void unlinkReturnsNrOfKeysRemoved() {

		actual.add(connection.set("unlink.this", "Can't track this!"));

		actual.add(connection.unlink("unlink.this", "unlink.that"));

		verifyResults(Arrays.asList(new Object[] { true, 1L }));
	}

	@Test // DATAREDIS-693
	@IfProfileValue(name = "redisVersion", value = "4.0+")
	public void testUnlinkBatch() {

		actual.add(connection.set("testing", "123"));
		actual.add(connection.set("foo", "bar"));
		actual.add(connection.unlink("testing", "foo"));
		actual.add(connection.exists("testing"));

		verifyResults(Arrays.asList(true, true, 2L, false));
	}

	@Test // DATAREDIS-693
	@IfProfileValue(name = "redisVersion", value = "4.0+")
	public void unlinkReturnsZeroIfNoKeysRemoved() {

		actual.add(connection.unlink("unlink.this"));

		verifyResults(Arrays.asList(new Object[] { 0L }));
	}

	@Test
	public void testType() {

		actual.add(connection.set("something", "yo"));
		actual.add(connection.type("something"));
		verifyResults(Arrays.asList(true, DataType.STRING));
	}

	@Test
	public void testGetSet() {
		actual.add(connection.set("testGS", "1"));
		actual.add(connection.getSet("testGS", "2"));
		actual.add(connection.get("testGS"));
		verifyResults(Arrays.asList(true, "1", "2"));
	}

	@Test
	public void testMSet() {
		Map<String, String> vals = new HashMap<>();
		vals.put("color", "orange");
		vals.put("size", "1");

		actual.add(connection.mSetString(vals));
		actual.add(connection.mGet("color", "size"));
		verifyResults(Arrays.asList(true, Arrays.asList(new String[] { "orange", "1" })));
	}

	@Test
	public void testMSetNx() {
		Map<String, String> vals = new HashMap<>();
		vals.put("height", "5");
		vals.put("width", "1");
		actual.add(connection.mSetNXString(vals));
		actual.add(connection.mGet("height", "width"));
		verifyResults(Arrays.asList(new Object[] { true, Arrays.asList(new String[] { "5", "1" }) }));
	}

	@Test
	public void testMSetNxFailure() {
		actual.add(connection.set("height", "2"));
		Map<String, String> vals = new HashMap<>();
		vals.put("height", "5");
		vals.put("width", "1");
		actual.add(connection.mSetNXString(vals));
		actual.add(connection.mGet("height", "width"));
		verifyResults(Arrays.asList(true, false, Arrays.asList(new String[] { "2", null })));
	}

	@Test
	public void testSetNx() {
		actual.add(connection.setNX("notaround", "54"));
		actual.add(connection.get("notaround"));
		actual.add(connection.setNX("notaround", "55"));
		actual.add(connection.get("notaround"));
		verifyResults(Arrays.asList(new Object[] { true, "54", false, "54" }));
	}

	@Test
	public void testGetRangeSetRange() {

		actual.add(connection.set("rangekey", "supercalifrag"));
		actual.add(connection.getRange("rangekey", 0l, 2l));
		connection.setRange("rangekey", "ck", 2);
		actual.add(connection.get("rangekey"));
		verifyResults(Arrays.asList(true, "sup", "suckrcalifrag"));
	}

	@Test
	public void testDecrByIncrBy() {

		actual.add(connection.set("tdb", "4"));
		actual.add(connection.decrBy("tdb", 3l));
		actual.add(connection.incrBy("tdb", 7l));
		verifyResults(Arrays.asList(Boolean.TRUE, 1L, 8L));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testIncrByDouble() {

		actual.add(connection.set("tdb", "4.5"));
		actual.add(connection.incrBy("tdb", 7.2));
		actual.add(connection.get("tdb"));

		verifyResults(Arrays.asList(Boolean.TRUE, 11.7d, "11.7"));
	}

	@Test
	public void testIncrDecrByLong() {

		String key = "test.count";
		long largeNumber = 0x123456789L; // > 32bits
		actual.add(connection.set(key, "0"));
		actual.add(connection.incrBy(key, largeNumber));
		actual.add(connection.decrBy(key, largeNumber));
		actual.add(connection.decrBy(key, 2 * largeNumber));
		verifyResults(Arrays.asList(Boolean.TRUE, largeNumber, 0l, -2 * largeNumber));
	}

	@Test
	public void testHashIncrDecrByLong() {

		String key = "test.hcount";
		String hkey = "hashkey";

		long largeNumber = 0x123456789L; // > 32bits
		actual.add(connection.hSet(key, hkey, "0"));
		actual.add(connection.hIncrBy(key, hkey, largeNumber));
		// assertEquals(largeNumber, Long.valueOf(connection.hGet(key, hkey)).longValue());
		actual.add(connection.hIncrBy(key, hkey, -2 * largeNumber));
		// assertEquals(-largeNumber, Long.valueOf(connection.hGet(key, hkey)).longValue());
		verifyResults(Arrays.asList(new Object[] { true, largeNumber, -largeNumber }));
	}

	@Test
	public void testIncDecr() {

		actual.add(connection.set("incrtest", "0"));
		actual.add(connection.incr("incrtest"));
		actual.add(connection.get("incrtest"));
		actual.add(connection.decr("incrtest"));
		actual.add(connection.get("incrtest"));
		verifyResults(Arrays.asList(Boolean.TRUE, 1L, "1", 0L, "0"));
	}

	@Test
	public void testStrLen() {

		actual.add(connection.set("strlentest", "cat"));
		actual.add(connection.strLen("strlentest"));
		verifyResults(Arrays.asList(Boolean.TRUE, 3L));
	}

	// List operations

	@Test
	public void testBLPop() {
		DefaultStringRedisConnection conn2 = new DefaultStringRedisConnection(connectionFactory.getConnection());
		conn2.lPush("poplist", "foo");
		conn2.lPush("poplist", "bar");
		actual.add(connection.bLPop(100, "poplist", "otherlist"));
		verifyResults(Arrays.asList(new Object[] { Arrays.asList(new String[] { "poplist", "bar" }) }));
	}

	@Test
	public void testBRPop() {
		DefaultStringRedisConnection conn2 = new DefaultStringRedisConnection(connectionFactory.getConnection());
		conn2.rPush("rpoplist", "bar");
		conn2.rPush("rpoplist", "foo");
		actual.add(connection.bRPop(1, "rpoplist"));
		verifyResults(Arrays.asList(new Object[] { Arrays.asList(new String[] { "rpoplist", "foo" }) }));
	}

	@Test
	public void testLInsert() {
		actual.add(connection.rPush("MyList", "hello"));
		actual.add(connection.rPush("MyList", "world"));
		actual.add(connection.lInsert("MyList", Position.AFTER, "hello", "big"));
		actual.add(connection.lRange("MyList", 0, -1));
		actual.add(connection.lInsert("MyList", Position.BEFORE, "big", "very"));
		actual.add(connection.lRange("MyList", 0, -1));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, 3l, Arrays.asList(new String[] { "hello", "big", "world" }), 4l,
				Arrays.asList(new String[] { "hello", "very", "big", "world" }) }));
	}

	@Test
	public void testLPop() {
		actual.add(connection.rPush("PopList", "hello"));
		actual.add(connection.rPush("PopList", "world"));
		actual.add(connection.lPop("PopList"));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, "hello" }));
	}

	@Test
	public void testLRem() {
		actual.add(connection.rPush("PopList", "hello"));
		actual.add(connection.rPush("PopList", "big"));
		actual.add(connection.rPush("PopList", "world"));
		actual.add(connection.rPush("PopList", "hello"));
		actual.add(connection.lRem("PopList", 2, "hello"));
		actual.add(connection.lRange("PopList", 0, -1));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, 3l, 4l, 2l, Arrays.asList(new String[] { "big", "world" }) }));
	}

	@Test
	public void testLLen() {
		actual.add(connection.rPush("PopList", "hello"));
		actual.add(connection.rPush("PopList", "big"));
		actual.add(connection.rPush("PopList", "world"));
		actual.add(connection.rPush("PopList", "hello"));
		actual.add(connection.lLen("PopList"));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, 3l, 4l, 4l }));
	}

	@Test
	public void testLSet() {
		actual.add(connection.rPush("PopList", "hello"));
		actual.add(connection.rPush("PopList", "big"));
		actual.add(connection.rPush("PopList", "world"));
		connection.lSet("PopList", 1, "cruel");
		actual.add(connection.lRange("PopList", 0, -1));
		verifyResults(
				Arrays.asList(new Object[] { 1l, 2l, 3l, Arrays.asList(new String[] { "hello", "cruel", "world" }) }));
	}

	@Test
	public void testLTrim() {
		actual.add(connection.rPush("PopList", "hello"));
		actual.add(connection.rPush("PopList", "big"));
		actual.add(connection.rPush("PopList", "world"));
		connection.lTrim("PopList", 1, -1);
		actual.add(connection.lRange("PopList", 0, -1));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, 3l, Arrays.asList(new String[] { "big", "world" }) }));
	}

	@Test
	public void testRPop() {
		actual.add(connection.rPush("PopList", "hello"));
		actual.add(connection.rPush("PopList", "world"));
		actual.add(connection.rPop("PopList"));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, "world" }));
	}

	@Test
	public void testRPopLPush() {
		actual.add(connection.rPush("PopList", "hello"));
		actual.add(connection.rPush("PopList", "world"));
		actual.add(connection.rPush("pop2", "hey"));
		actual.add(connection.rPopLPush("PopList", "pop2"));
		actual.add(connection.lRange("PopList", 0, -1));
		actual.add(connection.lRange("pop2", 0, -1));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, 1l, "world", Arrays.asList(new String[] { "hello" }),
				Arrays.asList(new String[] { "world", "hey" }) }));

	}

	@Test
	public void testBRPopLPush() {
		DefaultStringRedisConnection conn2 = new DefaultStringRedisConnection(connectionFactory.getConnection());
		conn2.rPush("PopList", "hello");
		conn2.rPush("PopList", "world");
		conn2.rPush("pop2", "hey");
		actual.add(connection.bRPopLPush(1, "PopList", "pop2"));
		List<Object> results = getResults();
		assertThat(results).isEqualTo(Arrays.asList(new String[] { "world" }));
		assertThat(connection.lRange("PopList", 0, -1)).isEqualTo(Arrays.asList(new String[] { "hello" }));
		assertThat(connection.lRange("pop2", 0, -1)).isEqualTo(Arrays.asList(new String[] { "world", "hey" }));
	}

	@Test
	public void testLPushX() {
		actual.add(connection.rPush("mylist", "hi"));
		actual.add(connection.lPushX("mylist", "foo"));
		actual.add(connection.lRange("mylist", 0, -1));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, Arrays.asList(new String[] { "foo", "hi" }) }));
	}

	@Test
	public void testRPushMultiple() {
		actual.add(connection.rPush("mylist", "hi", "foo"));
		actual.add(connection.lRange("mylist", 0, -1));
		verifyResults(Arrays.asList(new Object[] { 2l, Arrays.asList(new String[] { "hi", "foo" }) }));
	}

	@Test
	public void testRPushX() {
		actual.add(connection.rPush("mylist", "hi"));
		actual.add(connection.rPushX("mylist", "foo"));
		actual.add(connection.lRange("mylist", 0, -1));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, Arrays.asList(new String[] { "hi", "foo" }) }));
	}

	@Test
	public void testLIndex() {
		actual.add(connection.lPush("testylist", "foo"));
		actual.add(connection.lIndex("testylist", 0));
		verifyResults(Arrays.asList(new Object[] { 1l, "foo" }));
	}

	@Test
	public void testLPush() throws Exception {
		actual.add(connection.lPush("testlist", "bar"));
		actual.add(connection.lPush("testlist", "baz"));
		actual.add(connection.lRange("testlist", 0, -1));
		verifyResults(Arrays.asList(new Object[] { 1l, 2l, Arrays.asList(new String[] { "baz", "bar" }) }));
	}

	@Test
	public void testLPushMultiple() {
		actual.add(connection.lPush("testlist", "bar", "baz"));
		actual.add(connection.lRange("testlist", 0, -1));
		verifyResults(Arrays.asList(new Object[] { 2l, Arrays.asList(new String[] { "baz", "bar" }) }));
	}

	// Set operations

	@Test
	public void testSAdd() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sMembers("myset"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })) }));
	}

	@Test
	public void testSAddMultiple() {
		actual.add(connection.sAdd("myset", "foo", "bar"));
		actual.add(connection.sAdd("myset", "baz"));
		actual.add(connection.sMembers("myset"));
		verifyResults(
				Arrays.asList(new Object[] { 2l, 1l, new HashSet<>(Arrays.asList(new String[] { "foo", "bar", "baz" })) }));
	}

	@Test
	public void testSCard() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sCard("myset"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, 2l }));
	}

	@Test
	public void testSDiff() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("otherset", "bar"));
		actual.add(connection.sDiff("myset", "otherset"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, 1l, new HashSet<>(Collections.singletonList("foo")) }));
	}

	@Test
	public void testSDiffStore() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("otherset", "bar"));
		actual.add(connection.sDiffStore("thirdset", "myset", "otherset"));
		actual.add(connection.sMembers("thirdset"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, 1l, 1l, new HashSet<>(Collections.singletonList("foo")) }));
	}

	@Test
	public void testSInter() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("otherset", "bar"));
		actual.add(connection.sInter("myset", "otherset"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, 1l, new HashSet<>(Collections.singletonList("bar")) }));
	}

	@Test
	public void testSInterStore() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("otherset", "bar"));
		actual.add(connection.sInterStore("thirdset", "myset", "otherset"));
		actual.add(connection.sMembers("thirdset"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, 1l, 1l, new HashSet<>(Collections.singletonList("bar")) }));
	}

	@Test
	public void testSIsMember() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sIsMember("myset", "foo"));
		actual.add(connection.sIsMember("myset", "baz"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, true, false }));
	}

	@Test
	public void testSMove() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("otherset", "bar"));
		actual.add(connection.sMove("myset", "otherset", "foo"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, 1l, true }));
	}

	@Test
	public void testSPop() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sPop("myset"));
		assertThat(new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })).contains((String) getResults().get(2)))
				.isTrue();
	}

	@Test // DATAREDIS-688
	public void testSPopWithCount() {

		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("myset", "baz"));
		actual.add(connection.sPop("myset", 2));

		assertThat((Collection<Object>) getResults().get(3)).hasSize(2);
	}

	@Test
	public void testSRandMember() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sRandMember("myset"));
		assertThat(new HashSet<>(Arrays.asList(new String[] { "foo", "bar" })).contains(getResults().get(2))).isTrue();
	}

	@Test
	public void testSRandMemberKeyNotExists() {
		actual.add(connection.sRandMember("notexist"));
		assertThat(getResults().get(0)).isNull();
	}

	@SuppressWarnings("rawtypes")
	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testSRandMemberCount() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("myset", "baz"));
		actual.add(connection.sRandMember("myset", 2));
		assertThat(((Collection) getResults().get(3)).size() == 2).isTrue();
	}

	@SuppressWarnings("rawtypes")
	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testSRandMemberCountNegative() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sRandMember("myset", -2));
		assertThat(getResults().get(1)).isEqualTo(Arrays.asList(new String[] { "foo", "foo" }));
	}

	@SuppressWarnings("rawtypes")
	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testSRandMemberCountKeyNotExists() {
		actual.add(connection.sRandMember("notexist", 2));
		assertThat(((Collection) getResults().get(0)).isEmpty()).isTrue();
	}

	@Test
	public void testSRem() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sRem("myset", "foo"));
		actual.add(connection.sRem("myset", "baz"));
		actual.add(connection.sMembers("myset"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, 1l, 0l, new HashSet<>(Collections.singletonList("bar")) }));
	}

	@Test
	public void testSRemMultiple() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("myset", "baz"));
		actual.add(connection.sRem("myset", "foo", "nope", "baz"));
		actual.add(connection.sMembers("myset"));
		verifyResults(Arrays.asList(new Object[] { 1l, 1l, 1l, 2l, new HashSet<>(Collections.singletonList("bar")) }));
	}

	@Test
	public void testSUnion() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("otherset", "bar"));
		actual.add(connection.sAdd("otherset", "baz"));
		actual.add(connection.sUnion("myset", "otherset"));
		verifyResults(Arrays
				.asList(new Object[] { 1l, 1l, 1l, 1l, new HashSet<>(Arrays.asList(new String[] { "foo", "bar", "baz" })) }));
	}

	@Test
	public void testSUnionStore() {
		actual.add(connection.sAdd("myset", "foo"));
		actual.add(connection.sAdd("myset", "bar"));
		actual.add(connection.sAdd("otherset", "bar"));
		actual.add(connection.sAdd("otherset", "baz"));
		actual.add(connection.sUnionStore("thirdset", "myset", "otherset"));
		actual.add(connection.sMembers("thirdset"));
		verifyResults(Arrays.asList(
				new Object[] { 1l, 1l, 1l, 1l, 3l, new HashSet<>(Arrays.asList(new String[] { "foo", "bar", "baz" })) }));
	}

	// ZSet

	@Test
	public void testZAddAndZRange() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRange("myset", 0, -1));
		verifyResults(Arrays
				.asList(new Object[] { true, true, new LinkedHashSet<>(Arrays.asList(new String[] { "James", "Bob" })) }));
	}

	@Test
	public void testZAddMultiple() {
		Set<StringTuple> strTuples = new HashSet<>();
		strTuples.add(new DefaultStringTuple("Bob".getBytes(), "Bob", 2.0));
		strTuples.add(new DefaultStringTuple("James".getBytes(), "James", 1.0));
		Set<Tuple> tuples = new HashSet<>();
		tuples.add(new DefaultTuple("Joe".getBytes(), 2.5));
		actual.add(connection.zAdd("myset", strTuples));
		actual.add(connection.zAdd("myset".getBytes(), tuples));
		actual.add(connection.zRange("myset", 0, -1));
		verifyResults(Arrays
				.asList(new Object[] { 2l, 1l, new LinkedHashSet<>(Arrays.asList(new String[] { "James", "Bob", "Joe" })) }));
	}

	@Test
	public void testZCard() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zCard("myset"));
		verifyResults(Arrays.asList(new Object[] { true, true, 2l }));
	}

	@Test
	public void testZCount() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 4, "Joe"));
		actual.add(connection.zCount("myset", 1, 2));
		verifyResults(Arrays.asList(new Object[] { true, true, true, 2l }));
	}

	@Test
	public void testZIncrBy() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 4, "Joe"));
		actual.add(connection.zIncrBy("myset", 2, "Joe"));
		actual.add(connection.zRangeByScore("myset", 6, 6));
		verifyResults(
				Arrays.asList(new Object[] { true, true, true, 6d, new LinkedHashSet<>(Collections.singletonList("Joe")) }));
	}

	@Test
	public void testZInterStore() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 4, "Joe"));
		actual.add(connection.zAdd("otherset", 1, "Bob"));
		actual.add(connection.zAdd("otherset", 4, "James"));
		actual.add(connection.zInterStore("thirdset", "myset", "otherset"));
		actual.add(connection.zRange("thirdset", 0, -1));
		verifyResults(Arrays.asList(new Object[] { true, true, true, true, true, 2l,
				new LinkedHashSet<>(Arrays.asList(new String[] { "Bob", "James" })) }));
	}

	@Test
	public void testZInterStoreAggWeights() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 4, "Joe"));
		actual.add(connection.zAdd("otherset", 1, "Bob"));
		actual.add(connection.zAdd("otherset", 4, "James"));
		actual.add(connection.zInterStore("thirdset", Aggregate.MAX, new int[] { 2, 3 }, "myset", "otherset"));

		actual.add(connection.zRangeWithScores("thirdset", 0, -1));
		verifyResults(Arrays.asList(new Object[] { true, true, true, true, true, 2l,
				new LinkedHashSet<>(Arrays.asList(new StringTuple[] { new DefaultStringTuple("Bob".getBytes(), "Bob", 4d),
						new DefaultStringTuple("James".getBytes(), "James", 12d) })) }));
	}

	@Test
	public void testZRangeWithScores() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRangeWithScores("myset", 0, -1));
		verifyResults(Arrays.asList(new Object[] { true, true,
				new LinkedHashSet<>(Arrays.asList(new StringTuple[] { new DefaultStringTuple("James".getBytes(), "James", 1d),
						new DefaultStringTuple("Bob".getBytes(), "Bob", 2d) })) }));
	}

	@Test
	public void testZRangeByScore() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRangeByScore("myset", 1, 1));
		verifyResults(
				Arrays.asList(new Object[] { true, true, new LinkedHashSet<>(Arrays.asList(new String[] { "James" })) }));
	}

	@Test
	public void testZRangeByScoreOffsetCount() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRangeByScore("myset", 1d, 3d, 1, -1));
		verifyResults(
				Arrays.asList(new Object[] { true, true, new LinkedHashSet<>(Arrays.asList(new String[] { "Bob" })) }));
	}

	@Test
	public void testZRangeByScoreWithScores() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRangeByScoreWithScores("myset", 2d, 5d));
		verifyResults(Arrays.asList(new Object[] { true, true, new LinkedHashSet<>(
				Arrays.asList(new StringTuple[] { new DefaultStringTuple("Bob".getBytes(), "Bob", 2d) })) }));
	}

	@Test
	public void testZRangeByScoreWithScoresOffsetCount() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRangeByScoreWithScores("myset", 1d, 5d, 0, 1));
		verifyResults(Arrays.asList(new Object[] { true, true, new LinkedHashSet<>(
				Arrays.asList(new StringTuple[] { new DefaultStringTuple("James".getBytes(), "James", 1d) })) }));
	}

	@Test
	public void testZRevRange() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRevRange("myset", 0, -1));
		verifyResults(Arrays
				.asList(new Object[] { true, true, new LinkedHashSet<>(Arrays.asList(new String[] { "Bob", "James" })) }));
	}

	@Test
	public void testZRevRangeWithScores() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRevRangeWithScores("myset", 0, -1));
		verifyResults(Arrays.asList(new Object[] { true, true,
				new LinkedHashSet<>(Arrays.asList(new StringTuple[] { new DefaultStringTuple("Bob".getBytes(), "Bob", 2d),
						new DefaultStringTuple("James".getBytes(), "James", 1d) })) }));
	}

	@Test
	public void testZRevRangeByScoreOffsetCount() {
		actual.add(connection.zAdd("myset".getBytes(), 2, "Bob".getBytes()));
		actual.add(connection.zAdd("myset".getBytes(), 1, "James".getBytes()));
		actual.add(connection.zRevRangeByScore("myset", 0d, 3d, 0, 5));
		verifyResults(Arrays
				.asList(new Object[] { true, true, new LinkedHashSet<>(Arrays.asList(new String[] { "Bob", "James" })) }));
	}

	@Test
	public void testZRevRangeByScore() {
		actual.add(connection.zAdd("myset".getBytes(), 2, "Bob".getBytes()));
		actual.add(connection.zAdd("myset".getBytes(), 1, "James".getBytes()));
		actual.add(connection.zRevRangeByScore("myset", 0d, 3d));
		verifyResults(Arrays
				.asList(new Object[] { true, true, new LinkedHashSet<>(Arrays.asList(new String[] { "Bob", "James" })) }));
	}

	@Test
	public void testZRevRangeByScoreWithScoresOffsetCount() {
		actual.add(connection.zAdd("myset".getBytes(), 2, "Bob".getBytes()));
		actual.add(connection.zAdd("myset".getBytes(), 1, "James".getBytes()));
		actual.add(connection.zRevRangeByScoreWithScores("myset", 0d, 3d, 0, 1));
		verifyResults(Arrays.asList(new Object[] { true, true, new LinkedHashSet<>(
				Arrays.asList(new StringTuple[] { new DefaultStringTuple("Bob".getBytes(), "Bob", 2d) })) }));
	}

	@Test
	public void testZRevRangeByScoreWithScores() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 3, "Joe"));
		actual.add(connection.zRevRangeByScoreWithScores("myset", 0d, 2d));
		verifyResults(Arrays.asList(new Object[] { true, true, true,
				new LinkedHashSet<>(Arrays.asList(new StringTuple[] { new DefaultStringTuple("Bob".getBytes(), "Bob", 2d),
						new DefaultStringTuple("James".getBytes(), "James", 1d) })) }));
	}

	@Test
	public void testZRank() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRank("myset", "James"));
		actual.add(connection.zRank("myset", "Bob"));
		verifyResults(Arrays.asList(new Object[] { true, true, 0l, 1l }));
	}

	@Test
	public void testZRem() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRem("myset", "James"));
		actual.add(connection.zRange("myset", 0l, -1l));
		verifyResults(
				Arrays.asList(new Object[] { true, true, 1l, new LinkedHashSet<>(Arrays.asList(new String[] { "Bob" })) }));
	}

	@Test
	public void testZRemMultiple() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 0.5, "Joe"));
		actual.add(connection.zAdd("myset", 2.5, "Jen"));
		actual.add(connection.zRem("myset", "James", "Jen"));
		actual.add(connection.zRange("myset", 0l, -1l));
		verifyResults(Arrays.asList(new Object[] { true, true, true, true, 2l,
				new LinkedHashSet<>(Arrays.asList(new String[] { "Joe", "Bob" })) }));
	}

	@Test
	public void testZRemRangeByRank() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRemRange("myset", 0l, 3l));
		actual.add(connection.zRange("myset", 0l, -1l));
		verifyResults(Arrays.asList(new Object[] { true, true, 2l, new LinkedHashSet<String>(0) }));
	}

	@Test
	public void testZRemRangeByScore() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zRemRangeByScore("myset", 0d, 1d));
		actual.add(connection.zRange("myset", 0l, -1l));
		verifyResults(
				Arrays.asList(new Object[] { true, true, 1l, new LinkedHashSet<>(Arrays.asList(new String[] { "Bob" })) }));
	}

	@Test
	public void testZRevRank() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 3, "Joe"));
		actual.add(connection.zRevRank("myset", "Joe"));
		verifyResults(Arrays.asList(new Object[] { true, true, true, 0l }));
	}

	@Test
	public void testZScore() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 3, "Joe"));
		actual.add(connection.zScore("myset", "Joe"));
		verifyResults(Arrays.asList(new Object[] { true, true, true, 3d }));
	}

	@Test
	public void testZUnionStore() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 5, "Joe"));
		actual.add(connection.zAdd("otherset", 1, "Bob"));
		actual.add(connection.zAdd("otherset", 4, "James"));
		actual.add(connection.zUnionStore("thirdset", "myset", "otherset"));
		actual.add(connection.zRange("thirdset", 0, -1));
		verifyResults(Arrays.asList(new Object[] { true, true, true, true, true, 3l,
				new LinkedHashSet<>(Arrays.asList(new String[] { "Bob", "James", "Joe" })) }));
	}

	@Test
	public void testZUnionStoreAggWeights() {
		actual.add(connection.zAdd("myset", 2, "Bob"));
		actual.add(connection.zAdd("myset", 1, "James"));
		actual.add(connection.zAdd("myset", 4, "Joe"));
		actual.add(connection.zAdd("otherset", 1, "Bob"));
		actual.add(connection.zAdd("otherset", 4, "James"));
		actual.add(connection.zUnionStore("thirdset", Aggregate.MAX, new int[] { 2, 3 }, "myset", "otherset"));
		actual.add(connection.zRangeWithScores("thirdset", 0, -1));
		verifyResults(Arrays.asList(new Object[] { true, true, true, true, true, 3l,
				new LinkedHashSet<>(Arrays.asList(new StringTuple[] { new DefaultStringTuple("Bob".getBytes(), "Bob", 4d),
						new DefaultStringTuple("Joe".getBytes(), "Joe", 8d),
						new DefaultStringTuple("James".getBytes(), "James", 12d) })) }));
	}

	// Hash Ops

	@Test
	public void testHSetGet() throws Exception {
		String hash = getClass() + ":hashtest";
		String key1 = UUID.randomUUID().toString();
		String key2 = UUID.randomUUID().toString();
		String value1 = "foo";
		String value2 = "bar";
		actual.add(connection.hSet(hash, key1, value1));
		actual.add(connection.hSet(hash, key2, value2));
		actual.add(connection.hGet(hash, key1));
		actual.add(connection.hGetAll(hash));
		Map<String, String> expected = new HashMap<>();
		expected.put(key1, value1);
		expected.put(key2, value2);
		verifyResults(Arrays.asList(new Object[] { true, true, value1, expected }));
	}

	@Test
	public void testHSetNX() throws Exception {
		actual.add(connection.hSetNX("myhash", "key1", "foo"));
		actual.add(connection.hSetNX("myhash", "key1", "bar"));
		actual.add(connection.hGet("myhash", "key1"));
		verifyResults(Arrays.asList(new Object[] { true, false, "foo" }));
	}

	@Test
	public void testHDel() throws Exception {
		actual.add(connection.hSet("test", "key", "val"));
		actual.add(connection.hDel("test", "key"));
		actual.add(connection.hDel("test", "foo"));
		actual.add(connection.hExists("test", "key"));
		verifyResults(Arrays.asList(new Object[] { true, 1l, 0l, false }));
	}

	@Test
	public void testHDelMultiple() {
		actual.add(connection.hSet("test", "key", "val"));
		actual.add(connection.hSet("test", "foo", "bar"));
		actual.add(connection.hDel("test", "key", "foo"));
		actual.add(connection.hExists("test", "key"));
		actual.add(connection.hExists("test", "foo"));
		verifyResults(Arrays.asList(new Object[] { true, true, 2l, false, false }));
	}

	@Test
	public void testHIncrBy() {
		actual.add(connection.hSet("test", "key", "2"));
		actual.add(connection.hIncrBy("test", "key", 3l));
		actual.add(connection.hGet("test", "key"));
		verifyResults(Arrays.asList(new Object[] { true, 5l, "5" }));
	}

	@Test
	@IfProfileValue(name = "redisVersion", value = "2.6+")
	public void testHIncrByDouble() {
		actual.add(connection.hSet("test", "key", "2.9"));
		actual.add(connection.hIncrBy("test", "key", 3.5));
		actual.add(connection.hGet("test", "key"));
		verifyResults(Arrays.asList(new Object[] { true, 6.4d, "6.4" }));
	}

	@Test
	public void testHKeys() {
		actual.add(connection.hSet("test", "key", "2"));
		actual.add(connection.hSet("test", "key2", "2"));
		actual.add(connection.hKeys("test"));
		verifyResults(
				Arrays.asList(new Object[] { true, true, new LinkedHashSet<>(Arrays.asList(new String[] { "key", "key2" })) }));
	}

	@Test
	public void testHLen() {
		actual.add(connection.hSet("test", "key", "2"));
		actual.add(connection.hSet("test", "key2", "2"));
		actual.add(connection.hLen("test"));
		verifyResults(Arrays.asList(new Object[] { true, true, 2l }));
	}

	@Test
	public void testHMGetSet() {
		Map<String, String> tuples = new HashMap<>();
		tuples.put("key", "foo");
		tuples.put("key2", "bar");
		connection.hMSet("test", tuples);
		actual.add(connection.hMGet("test", "key", "key2"));
		verifyResults(Arrays.asList(new Object[] { Arrays.asList(new String[] { "foo", "bar" }) }));
	}

	@Test
	public void testHVals() {
		actual.add(connection.hSet("test", "key", "foo"));
		actual.add(connection.hSet("test", "key2", "bar"));
		actual.add(connection.hVals("test"));
		verifyResults(Arrays.asList(new Object[] { true, true, Arrays.asList(new String[] { "foo", "bar" }) }));
	}

	@Test
	public void testMove() {

		actual.add(connection.set("foo", "bar"));
		actual.add(connection.move("foo", 1));

		verifyResults(Arrays.asList(true, true));
		connection.select(1);
		try {
			assertThat(connection.get("foo")).isEqualTo("bar");
		} finally {
			if (connection.exists("foo")) {
				connection.del("foo");
			}
		}
	}

	@Test
	public void testLastSave() {
		actual.add(connection.lastSave());
		List<Object> results = getResults();
		assertThat(results.get(0)).isNotNull();
	}

	@Test // DATAREDIS-206, DATAREDIS-513
	public void testGetTimeShouldRequestServerTime() {

		actual.add(connection.time());

		List<Object> results = getResults();
		assertThat(results).isNotEmpty();
		assertThat(results.get(0)).isNotNull();
		assertThat((Long) results.get(0) > 0).isTrue();
	}

	@Test // DATAREDIS-269
	public void clientSetNameWorksCorrectly() {
		connection.setClientName("foo".getBytes());
	}

	@Test // DATAREDIS-268
	public void testListClientsContainsAtLeastOneElement() {

		actual.add(connection.getClientList());

		List<Object> results = getResults();
		assertThat(results.get(0)).isNotNull();

		List<?> firstEntry = (List<?>) results.get(0);
		assertThat(firstEntry.size()).isNotEqualTo(0);
		assertThat(firstEntry.get(0)).isInstanceOf(RedisClientInfo.class);

		RedisClientInfo info = (RedisClientInfo) firstEntry.get(0);
		assertThat(info.getDatabaseId()).isNotNull();
	}

	@Test // DATAREDIS-290
	@IfProfileValue(name = "redisVersion", value = "2.8+")
	public void scanShouldReadEntireValueRange() {

		if (!ConnectionUtils.isJedis(connectionFactory) && !ConnectionUtils.isLettuce(connectionFactory)) {
			throw new AssumptionViolatedException("SCAN is only available for jedis and lettuce");
		}

		if (connection.isPipelined() || connection.isQueueing()) {
			throw new AssumptionViolatedException("SCAN is only available in non pipeline | queue mode.");
		}

		connection.set("spring", "data");

		int itemCount = 22;
		for (int i = 0; i < itemCount; i++) {
			connection.set(("key_" + i), ("foo_" + i));
		}

		Cursor<byte[]> cursor = connection.scan(scanOptions().count(20).match("ke*").build());

		int i = 0;
		while (cursor.hasNext()) {
			byte[] value = cursor.next();
			assertThat(new String(value)).doesNotContain("spring");
			i++;
		}

		assertThat(i).isEqualTo(itemCount);
	}

	@Test // DATAREDIS-417
	@IfProfileValue(name = "redisVersion", value = "2.8+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void scanShouldReadEntireValueRangeWhenIdividualScanIterationsReturnEmptyCollection() {

		connection.execute("DEBUG", "POPULATE".getBytes(), "100".getBytes());

		Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match("key*9").count(10).build());

		int i = 0;
		while (cursor.hasNext()) {
			assertThat(new String(cursor.next())).contains("key:");
			i++;
		}

		assertThat(i).isEqualTo(10);
	}

	@Test // DATAREDIS-306
	@IfProfileValue(name = "redisVersion", value = "2.8+")
	public void zScanShouldReadEntireValueRange() {

		if (!ConnectionUtils.isJedis(connectionFactory) && !ConnectionUtils.isLettuce(connectionFactory)) {
			throw new AssumptionViolatedException("ZSCAN is only available for jedis and lettuce");
		}

		if (connection.isPipelined() || connection.isQueueing()) {
			throw new AssumptionViolatedException("ZSCAN is only available in non pipeline | queue mode.");
		}

		connection.zAdd("myset", 2, "Bob");
		connection.zAdd("myset", 1, "James");
		connection.zAdd("myset", 4, "Joe");

		Cursor<StringTuple> tuples = connection.zScan("myset", ScanOptions.NONE);

		int count = 0;
		while (tuples.hasNext()) {

			StringTuple tuple = tuples.next();

			assertThat(tuple.getValueAsString()).isIn("Bob", "James", "Joe");
			assertThat(tuple.getScore()).isIn(1D, 2D, 4D);

			count++;
		}

		assertThat(count).isEqualTo(3);
	}

	@Test // DATAREDIS-304
	@IfProfileValue(name = "redisVersion", value = "2.8+")
	public void sScanShouldReadEntireValueRange() {

		if (!ConnectionUtils.isJedis(connectionFactory) && !ConnectionUtils.isLettuce(connectionFactory)) {
			throw new AssumptionViolatedException("SCAN is only available for jedis and lettuce");
		}

		if (connection.isPipelined() || connection.isQueueing()) {
			throw new AssumptionViolatedException("SCAN is only available in non pipeline | queue mode.");
		}

		connection.sAdd("sscankey", "bar");
		connection.sAdd("sscankey", "foo-1", "foo-2", "foo-3", "foo-4", "foo-5", "foo-6");

		Cursor<String> cursor = connection.sScan("sscankey", scanOptions().count(2).match("fo*").build());

		int i = 0;
		while (cursor.hasNext()) {
			assertThat(cursor.next()).doesNotContain("bar");
			i++;
		}

		assertThat(i).isEqualTo(6);
	}

	@Test // DATAREDIS-305
	@IfProfileValue(name = "redisVersion", value = "2.8+")
	public void hScanShouldReadEntireValueRange() {

		if (!ConnectionUtils.isJedis(connectionFactory) && !ConnectionUtils.isLettuce(connectionFactory)) {
			throw new AssumptionViolatedException("HSCAN is only available for jedis and lettuce");
		}

		if (connection.isPipelined() || connection.isQueueing()) {
			throw new AssumptionViolatedException("HSCAN is only available in non pipeline | queue mode.");
		}

		connection.hSet("hscankey", "bar", "foobar");

		connection.hSet("hscankey", "foo-1", "v-1");
		connection.hSet("hscankey", "foo-2", "v-2");
		connection.hSet("hscankey", "foo-3", "v-3");

		Cursor<Map.Entry<String, String>> cursor = connection.hScan("hscankey",
				scanOptions().count(2).match("fo*").build());

		int i = 0;
		while (cursor.hasNext()) {

			String key = cursor.next().getKey();

			assertThat(key).doesNotContain("bar");
			assertThat(key).contains("foo");

			i++;
		}

		assertThat(i).isEqualTo(3);
	}

	@Test // DATAREDIS-308
	@IfProfileValue(name = "redisVersion", value = "2.8.9+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void pfAddShouldAddToNonExistingKeyCorrectly() {

		actual.add(connection.pfAdd("hll", "a", "b", "c"));

		List<Object> results = getResults();
		assertThat(results.get(0)).isEqualTo(1L);
	}

	@Test // DATAREDIS-308
	@IfProfileValue(name = "redisVersion", value = "2.8.9+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void pfAddShouldReturnZeroWhenValueAlreadyExists() {

		actual.add(connection.pfAdd("hll", "a", "b", "c"));
		actual.add(connection.pfAdd("hll2", "c", "d", "e"));
		actual.add(connection.pfAdd("hll2", "e"));

		List<Object> results = getResults();
		assertThat(results.get(0)).isEqualTo(1L);
		assertThat(results.get(1)).isEqualTo(1L);
		assertThat(results.get(2)).isEqualTo(0L);
	}

	@Test // DATAREDIS-308
	@IfProfileValue(name = "redisVersion", value = "2.8.9+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void pfCountShouldReturnCorrectly() {

		actual.add(connection.pfAdd("hll", "a", "b", "c"));
		actual.add(connection.pfCount("hll"));

		List<Object> results = getResults();
		assertThat(results.get(0)).isEqualTo(1L);
		assertThat(results.get(1)).isEqualTo(3L);
	}

	@Test // DATAREDIS-308
	@IfProfileValue(name = "redisVersion", value = "2.8.9+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void pfCountWithMultipleKeysShouldReturnCorrectly() {

		actual.add(connection.pfAdd("hll", "a", "b", "c"));
		actual.add(connection.pfAdd("hll2", "d", "e", "f"));
		actual.add(connection.pfCount("hll", "hll2"));

		List<Object> results = getResults();
		assertThat(results.get(0)).isEqualTo(1L);
		assertThat(results.get(1)).isEqualTo(1L);
		assertThat(results.get(2)).isEqualTo(6L);
	}

	@Test // DATAREDIS-308
	@IfProfileValue(name = "redisVersion", value = "2.8.9+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void pfCountWithNullKeysShouldThrowIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> actual.add(connection.pfCount((String[]) null)));
	}

	@SuppressWarnings("unchecked")
	@Test // DATAREDIS-378, DATAREDIS-1222
	@IfProfileValue(name = "redisVersion", value = "2.9.0+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void zRangeByLexTest() {

		actual.add(connection.zAdd("myzset", 0, "a"));
		actual.add(connection.zAdd("myzset", 0, "b"));
		actual.add(connection.zAdd("myzset", 0, "c"));
		actual.add(connection.zAdd("myzset", 0, "d"));
		actual.add(connection.zAdd("myzset", 0, "e"));
		actual.add(connection.zAdd("myzset", 0, "f"));
		actual.add(connection.zAdd("myzset", 0, "g"));

		actual.add(connection.zRangeByLex("myzset", Range.range().lte("c")));
		actual.add(connection.zRangeByLex("myzset", Range.range().lt("c")));
		actual.add(connection.zRangeByLex("myzset", Range.range().gte("aaa").lt("g")));
		actual.add(connection.zRangeByLex("myzset", Range.range().gte("e")));

		actual.add(connection.zRangeByLex("myzset", Range.range().lte("c"), Limit.unlimited()));
		actual.add(connection.zRangeByLex("myzset", Range.range().lte("c"), Limit.limit().count(1)));
		actual.add(connection.zRangeByLex("myzset", Range.range().lte("c"), Limit.limit().count(1).offset(1)));

		List<Object> results = getResults();

		Set<String> values = (Set<String>) results.get(7);

		assertThat(values).contains("a", "b", "c");
		assertThat(values).doesNotContain("d", "e", "f", "g");

		values = (Set<String>) results.get(8);
		assertThat(values).contains("a", "b");
		assertThat(values).doesNotContain("c");

		values = (Set<String>) results.get(9);
		assertThat(values).contains("b", "c", "d", "e", "f");
		assertThat(values).doesNotContain("a", "g");

		values = (Set<String>) results.get(10);
		assertThat(values).contains("e", "f", "g");
		assertThat(values).doesNotContain("a", "b", "c", "d");

		values = (Set<String>) results.get(11);
		assertThat(values).contains("a", "b", "c");
		assertThat(values).doesNotContain("d", "e", "f", "g");

		values = (Set<String>) results.get(12);
		assertThat(values).contains("a");
		assertThat(values).doesNotContain("b", "c", "d", "e", "f", "g");

		values = (Set<String>) results.get(13);
		assertThat(values).contains("b");
		assertThat(values).doesNotContain("a", "c", "d", "e", "f", "g");
	}

	@Test(expected = IllegalArgumentException.class) // DATAREDIS-316, DATAREDIS-692
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithExpirationAndNullOpionShouldThrowException() {

		String key = "exp-" + UUID.randomUUID();
		connection.set(key, "foo", Expiration.milliseconds(500), null);
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithExpirationAndUpsertOpionShouldSetTtlWhenKeyDoesNotExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "foo", Expiration.milliseconds(500), SetOption.upsert()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(2)).doubleValue()).isCloseTo(500d, Offset.offset(499d));
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithExpirationAndUpsertOpionShouldSetTtlWhenKeyDoesExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "spring"));
		actual.add(connection.set(key, "data", Expiration.milliseconds(500), SetOption.upsert()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(2)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(3)).doubleValue()).isCloseTo(500d, Offset.offset(499d));
		assertThat((result.get(4))).isEqualTo("data");
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithExpirationAndAbsentOptionShouldSetTtlWhenKeyDoesExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "spring"));
		actual.add(connection.set(key, "data", Expiration.milliseconds(500), SetOption.ifAbsent()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.FALSE);
		assertThat(result.get(2)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(3)).doubleValue()).isCloseTo(-1, Offset.offset(0d));
		assertThat((result.get(4))).isEqualTo("spring");
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithExpirationAndAbsentOptionShouldSetTtlWhenKeyDoesNotExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "data", Expiration.milliseconds(500), SetOption.ifAbsent()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(2)).doubleValue()).isCloseTo(500d, Offset.offset(499d));
		assertThat((result.get(3))).isEqualTo("data");
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithExpirationAndPresentOptionShouldSetTtlWhenKeyDoesExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "spring"));
		actual.add(connection.set(key, "data", Expiration.milliseconds(500), SetOption.ifPresent()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(2)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(3)).doubleValue()).isCloseTo(500, Offset.offset(499d));
		assertThat((result.get(4))).isEqualTo("data");
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithExpirationAndPresentOptionShouldSetTtlWhenKeyDoesNotExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "data", Expiration.milliseconds(500), SetOption.ifPresent()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.FALSE);
		assertThat(result.get(1)).isEqualTo(Boolean.FALSE);
		assertThat(((Long) result.get(2)).doubleValue()).isCloseTo(-2, Offset.offset(0d));
	}

	@Test(expected = IllegalArgumentException.class) // DATAREDIS-316, DATAREDIS-692
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithNullExpirationAndUpsertOpionShouldThrowException() {

		String key = "exp-" + UUID.randomUUID();
		connection.set(key, "foo", null, SetOption.upsert());
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithoutExpirationAndUpsertOpionShouldSetTtlWhenKeyDoesNotExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "foo", Expiration.persistent(), SetOption.upsert()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(2)).doubleValue()).isCloseTo(-1, Offset.offset(0d));
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithoutExpirationAndUpsertOpionShouldSetTtlWhenKeyDoesExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "spring"));
		actual.add(connection.set(key, "data", Expiration.persistent(), SetOption.upsert()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();

		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(2)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(3)).doubleValue()).isCloseTo(-1, Offset.offset(0d));
		assertThat((result.get(4))).isEqualTo("data");
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithoutExpirationAndAbsentOptionShouldSetTtlWhenKeyDoesExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "spring"));
		actual.add(connection.set(key, "data", Expiration.persistent(), SetOption.ifAbsent()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.FALSE);
		assertThat(result.get(2)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(3)).doubleValue()).isCloseTo(-1d, Offset.offset(0d));
		assertThat((result.get(4))).isEqualTo("spring");
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithoutExpirationAndAbsentOptionShouldSetTtlWhenKeyDoesNotExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "data", Expiration.persistent(), SetOption.ifAbsent()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(2)).doubleValue()).isCloseTo(-1, Offset.offset(0d));
		assertThat((result.get(3))).isEqualTo("data");
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithoutExpirationAndPresentOptionShouldSetTtlWhenKeyDoesExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "spring"));
		actual.add(connection.set(key, "data", Expiration.persistent(), SetOption.ifPresent()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
		assertThat(result.get(2)).isEqualTo(Boolean.TRUE);
		assertThat(((Long) result.get(3)).doubleValue()).isCloseTo(-1, Offset.offset(0d));
		assertThat((result.get(4))).isEqualTo("data");
	}

	@Test // DATAREDIS-316
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void setWithoutExpirationAndPresentOptionShouldSetTtlWhenKeyDoesNotExist() {

		String key = "exp-" + UUID.randomUUID();
		actual.add(connection.set(key, "data", Expiration.persistent(), SetOption.ifPresent()));

		actual.add(connection.exists(key));
		actual.add(connection.pTtl(key));
		actual.add(connection.get(key));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(Boolean.FALSE);
		assertThat(result.get(1)).isEqualTo(Boolean.FALSE);
		assertThat(((Long) result.get(2)).doubleValue()).isCloseTo(-2, Offset.offset(0d));
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoAddSingleGeoLocation() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, PALERMO));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(1L);
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoAddMultipleGeoLocations() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(PALERMO, ARIGENTO, CATANIA, PALERMO)));

		List<Object> result = getResults();
		assertThat(result.get(0)).isEqualTo(3L);
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoDist() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(PALERMO, CATANIA)));
		actual.add(connection.geoDist(key, PALERMO.getName(), CATANIA.getName()));

		List<Object> result = getResults();
		assertThat(((Distance) result.get(1)).getValue()).isCloseTo(166274.15156960033D, Offset.offset(0.005));
		assertThat(((Distance) result.get(1)).getUnit()).isEqualTo("m");
	}

	@Test // DATAREDIS-1214
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoDistNotExisting() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(PALERMO, CATANIA)));
		actual.add(connection.geoDist(key, "Spring", "Data"));

		List<Object> result = getResults();
		assertThat(result.get(1)).isNull();
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoDistWithMetric() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(PALERMO, CATANIA)));
		actual.add(connection.geoDist(key, PALERMO.getName(), CATANIA.getName(), Metrics.KILOMETERS));

		List<Object> result = getResults();
		assertThat(((Distance) result.get(1)).getValue()).isCloseTo(166.27415156960033D, Offset.offset(0.005));
		assertThat(((Distance) result.get(1)).getUnit()).isEqualTo("km");
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS })
	public void geoHash() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(PALERMO, CATANIA)));
		actual.add(connection.geoHash(key, PALERMO.getName(), CATANIA.getName()));

		List<Object> result = getResults();
		assertThat(((List<String>) result.get(1)).get(0)).isEqualTo("sqc8b49rny0");
		assertThat(((List<String>) result.get(1)).get(1)).isEqualTo("sqdtr74hyu0");
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS })
	public void geoHashNonExisting() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(PALERMO, CATANIA)));
		actual.add(connection.geoHash(key, PALERMO.getName(), ARIGENTO.getName(), CATANIA.getName()));

		List<Object> result = getResults();
		assertThat(((List<String>) result.get(1)).get(0)).isEqualTo("sqc8b49rny0");
		assertThat(((List<String>) result.get(1)).get(1)).isNull();
		assertThat(((List<String>) result.get(1)).get(2)).isEqualTo("sqdtr74hyu0");
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoPosition() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(PALERMO, CATANIA)));

		actual.add(connection.geoPos(key, PALERMO.getName(), CATANIA.getName()));

		List<Object> result = getResults();
		assertThat(((List<Point>) result.get(1)).get(0).getX()).isCloseTo(POINT_PALERMO.getX(), Offset.offset(0.005));
		assertThat(((List<Point>) result.get(1)).get(0).getY()).isCloseTo(POINT_PALERMO.getY(), Offset.offset(0.005));

		assertThat(((List<Point>) result.get(1)).get(1).getX()).isCloseTo(POINT_CATANIA.getX(), Offset.offset(0.005));
		assertThat(((List<Point>) result.get(1)).get(1).getY()).isCloseTo(POINT_CATANIA.getY(), Offset.offset(0.005));
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoPositionNonExisting() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(PALERMO, CATANIA)));

		actual.add(connection.geoPos(key, PALERMO.getName(), ARIGENTO.getName(), CATANIA.getName()));

		List<Object> result = getResults();
		assertThat(((List<Point>) result.get(1)).get(0).getX()).isCloseTo(POINT_PALERMO.getX(), Offset.offset(0.005));
		assertThat(((List<Point>) result.get(1)).get(0).getY()).isCloseTo(POINT_PALERMO.getY(), Offset.offset(0.005));

		assertThat(((List<Point>) result.get(1)).get(1)).isNull();

		assertThat(((List<Point>) result.get(1)).get(2).getX()).isCloseTo(POINT_CATANIA.getX(), Offset.offset(0.005));
		assertThat(((List<Point>) result.get(1)).get(2).getY()).isCloseTo(POINT_CATANIA.getY(), Offset.offset(0.005));
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoRadiusShouldReturnMembersCorrectly() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(ARIGENTO, CATANIA, PALERMO)));

		actual.add(connection.geoRadius(key, new Circle(new Point(15D, 37D), new Distance(200D, KILOMETERS))));
		actual.add(connection.geoRadius(key, new Circle(new Point(15D, 37D), new Distance(150D, KILOMETERS))));

		List<Object> results = getResults();
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent()).hasSize(3);
		assertThat(((GeoResults<GeoLocation<String>>) results.get(2)).getContent()).hasSize(2);
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoRadiusShouldReturnDistanceCorrectly() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(ARIGENTO, CATANIA, PALERMO)));

		actual.add(connection.geoRadius(key, new Circle(new Point(15D, 37D), new Distance(200D, KILOMETERS)),
				newGeoRadiusArgs().includeDistance()));

		List<Object> results = getResults();
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent()).hasSize(3);
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent().get(0).getDistance().getValue())
				.isCloseTo(130.423D, Offset.offset(0.005));
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent().get(0).getDistance().getUnit())
				.isEqualTo("km");
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoRadiusShouldApplyLimit() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(ARIGENTO, CATANIA, PALERMO)));

		actual.add(connection.geoRadius(key, new Circle(new Point(15D, 37D), new Distance(200D, KILOMETERS)),
				newGeoRadiusArgs().limit(2)));

		List<Object> results = getResults();
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent()).hasSize(2);
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoRadiusByMemberShouldReturnMembersCorrectly() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(ARIGENTO, CATANIA, PALERMO)));

		actual.add(connection.geoRadiusByMember(key, PALERMO.getName(), new Distance(100, KILOMETERS),
				newGeoRadiusArgs().sortAscending()));

		List<Object> results = getResults();
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent().get(0).getContent().getName())
				.isEqualTo(PALERMO.getName());
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent().get(1).getContent().getName())
				.isEqualTo(ARIGENTO.getName());
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoRadiusByMemberShouldReturnDistanceCorrectly() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(ARIGENTO, CATANIA, PALERMO)));

		actual.add(connection.geoRadiusByMember(key, PALERMO.getName(), new Distance(100, KILOMETERS),
				newGeoRadiusArgs().includeDistance()));

		List<Object> results = getResults();
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent()).hasSize(2);
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent().get(0).getDistance().getValue())
				.isCloseTo(90.978D, Offset.offset(0.005));
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent().get(0).getDistance().getUnit())
				.isEqualTo("km");
	}

	@Test // DATAREDIS-438
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void geoRadiusByMemberShouldApplyLimit() {

		String key = "geo-" + UUID.randomUUID();
		actual.add(connection.geoAdd(key, Arrays.asList(ARIGENTO, CATANIA, PALERMO)));

		actual.add(connection.geoRadiusByMember(key, PALERMO.getName(), new Distance(200, KILOMETERS),
				newGeoRadiusArgs().limit(2)));

		List<Object> results = getResults();
		assertThat(((GeoResults<GeoLocation<String>>) results.get(1)).getContent()).hasSize(2);
	}

	@Test // DATAREDIS-698
	public void hStrLenReturnsFieldLength() {

		actual.add(connection.hSet("hash-hstrlen", "key-1", "value-1"));
		actual.add(connection.hSet("hash-hstrlen", "key-2", "value-2"));
		actual.add(connection.hStrLen("hash-hstrlen", "key-2"));

		verifyResults(Arrays.asList(new Object[] { Boolean.TRUE, Boolean.TRUE, Long.valueOf("value-2".length()) }));
	}

	@Test // DATAREDIS-698
	public void hStrLenReturnsZeroWhenFieldDoesNotExist() {

		actual.add(connection.hSet("hash-hstrlen", "key-1", "value-1"));
		actual.add(connection.hStrLen("hash-hstrlen", "key-2"));

		verifyResults(Arrays.asList(new Object[] { Boolean.TRUE, 0L }));
	}

	@Test // DATAREDIS-698
	public void hStrLenReturnsZeroWhenKeyDoesNotExist() {

		actual.add(connection.hStrLen("hash-no-exist", "key-2"));

		verifyResults(Arrays.asList(new Object[] { 0L }));
	}

	@Test // DATAREDIS-694
	public void touchReturnsNrOfKeysTouched() {

		actual.add(connection.set("touch.this", "Can't touch this! - oh-oh oh oh oh-oh-oh"));
		actual.add(connection.touch("touch.this", "touch.that"));

		verifyResults(Arrays.asList(new Object[] { Boolean.TRUE, 1L }));
	}

	@Test // DATAREDIS-694
	public void touchReturnsZeroIfNoKeysTouched() {

		actual.add(connection.touch("touch.this", "touch.that"));

		verifyResults(Arrays.asList(new Object[] { 0L }));
	}

	@Test // DATAREDIS-697
	@IfProfileValue(name = "redisVersion", value = "2.8.7+")
	public void bitPosShouldReturnPositionCorrectly() {

		actual.add(connection.set("bitpos-1".getBytes(), HexStringUtils.hexToBytes("fff000")));
		actual.add(connection.bitPos("bitpos-1", false));

		verifyResults(Arrays.asList(new Object[] { true, 12L }));
	}

	@Test // DATAREDIS-697
	@IfProfileValue(name = "redisVersion", value = "2.8.7+")
	public void bitPosShouldReturnPositionInRangeCorrectly() {

		actual.add(connection.set("bitpos-1".getBytes(), HexStringUtils.hexToBytes("fff0f0")));
		actual.add(connection.bitPos("bitpos-1", true,
				org.springframework.data.domain.Range.of(Bound.inclusive(2L), Bound.unbounded())));

		verifyResults(Arrays.asList(new Object[] { true, 16L }));
	}

	@Test // DATAREDIS-716
	public void encodingReturnsCorrectly() {

		actual.add(connection.set("encode.this", "1000"));

		actual.add(connection.encodingOf("encode.this"));

		verifyResults(Arrays.asList(new Object[] { true, RedisValueEncoding.INT }));
	}

	@Test // DATAREDIS-716
	public void encodingReturnsVacantWhenKeyDoesNotExist() {

		actual.add(connection.encodingOf("encode.this"));

		verifyResults(Arrays.asList(new Object[] { RedisValueEncoding.VACANT }));
	}

	@Test // DATAREDIS-716
	public void idletimeReturnsCorrectly() {

		actual.add(connection.set("idle.this", "1000"));
		actual.add(connection.get("idle.this"));

		actual.add(connection.idletime("idle.this"));

		verifyResults(Arrays.asList(new Object[] { true, "1000", Duration.ofSeconds(0) }));
	}

	@Test // DATAREDIS-716
	public void idldetimeReturnsNullWhenKeyDoesNotExist() {

		actual.add(connection.idletime("idle.this"));

		verifyResults(Arrays.asList(new Object[] { null }));
	}

	@Test // DATAREDIS-716
	public void refcountReturnsCorrectly() {

		actual.add(connection.lPush("refcount.this", "1000"));

		actual.add(connection.refcount("refcount.this"));

		verifyResults(Arrays.asList(new Object[] { 1L, 1L }));
	}

	@Test // DATAREDIS-716
	public void refcountReturnsNullWhenKeyDoesNotExist() {

		actual.add(connection.refcount("refcount.this"));

		verifyResults(Arrays.asList(new Object[] { null }));
	}

	@Test // DATAREDIS-562
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void bitFieldSetShouldWorkCorrectly() {

		actual.add(connection.bitfield(KEY_1, create().set(INT_8).valueAt(BitFieldSubCommands.Offset.offset(0L)).to(10L)));
		actual.add(connection.bitfield(KEY_1, create().set(INT_8).valueAt(BitFieldSubCommands.Offset.offset(0L)).to(20L)));

		List<Object> results = getResults();
		assertThat((List<Long>) results.get(0)).containsExactly(0L);
		assertThat((List<Long>) results.get(1)).containsExactly(10L);
	}

	@Test // DATAREDIS-562
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void bitFieldGetShouldWorkCorrectly() {

		actual.add(connection.bitfield(KEY_1, create().get(INT_8).valueAt(BitFieldSubCommands.Offset.offset(0L))));

		List<Object> results = getResults();
		assertThat((List<Long>) results.get(0)).containsExactly(0L);
	}

	@Test // DATAREDIS-562
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void bitFieldIncrByShouldWorkCorrectly() {

		actual
				.add(connection.bitfield(KEY_1, create().incr(INT_8).valueAt(BitFieldSubCommands.Offset.offset(100L)).by(1L)));

		List<Object> results = getResults();
		assertThat((List<Long>) results.get(0)).containsExactly(1L);
	}

	@Test // DATAREDIS-562
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void bitFieldIncrByWithOverflowShouldWorkCorrectly() {

		actual.add(connection.bitfield(KEY_1,
				create().incr(unsigned(2)).valueAt(BitFieldSubCommands.Offset.offset(102L)).overflow(FAIL).by(1L)));
		actual.add(connection.bitfield(KEY_1,
				create().incr(unsigned(2)).valueAt(BitFieldSubCommands.Offset.offset(102L)).overflow(FAIL).by(1L)));
		actual.add(connection.bitfield(KEY_1,
				create().incr(unsigned(2)).valueAt(BitFieldSubCommands.Offset.offset(102L)).overflow(FAIL).by(1L)));
		actual.add(connection.bitfield(KEY_1,
				create().incr(unsigned(2)).valueAt(BitFieldSubCommands.Offset.offset(102L)).overflow(FAIL).by(1L)));

		List<Object> results = getResults();
		assertThat((List<Long>) results.get(0)).containsExactly(1L);
		assertThat((List<Long>) results.get(1)).containsExactly(2L);
		assertThat((List<Long>) results.get(2)).containsExactly(3L);
		assertThat(results.get(3)).isNotNull();
	}

	@Test // DATAREDIS-562
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void bitfieldShouldAllowMultipleSubcommands() {

		actual.add(connection.bitfield(KEY_1,
				create().incr(signed(5)).valueAt(BitFieldSubCommands.Offset.offset(100L)).by(1L).get(unsigned(4)).valueAt(0L)));

		assertThat((List<Long>) getResults().get(0)).containsExactly(1L, 0L);
	}

	@Test // DATAREDIS-562
	@IfProfileValue(name = "redisVersion", value = "3.2+")
	@WithRedisDriver({ RedisDriver.JEDIS, RedisDriver.LETTUCE })
	public void bitfieldShouldWorkUsingNonZeroBasedOffset() {

		actual.add(connection.bitfield(KEY_1,
				create().set(INT_8).valueAt(BitFieldSubCommands.Offset.offset(0L).multipliedByTypeLength()).to(100L).set(INT_8)
						.valueAt(BitFieldSubCommands.Offset.offset(1L).multipliedByTypeLength()).to(200L)));
		actual.add(connection.bitfield(KEY_1,
				create().get(INT_8).valueAt(BitFieldSubCommands.Offset.offset(0L).multipliedByTypeLength()).get(INT_8)
						.valueAt(BitFieldSubCommands.Offset.offset(1L).multipliedByTypeLength())));

		List<Object> results = getResults();
		assertThat((List<Long>) results.get(0)).containsExactly(0L, 0L);
		assertThat((List<Long>) results.get(1)).containsExactly(100L, -56L);
	}

	@Test // DATAREDIS-864
	@IfProfileValue(name = "redisVersion", value = "5.0")
	@WithRedisDriver({ RedisDriver.LETTUCE })
	public void xAddShouldCreateStream() {

		actual.add(connection.xAdd(KEY_1, Collections.singletonMap(KEY_2, VALUE_2)));
		actual.add(connection.type(KEY_1));

		List<Object> results = getResults();
		assertThat(results).hasSize(2);
		assertThat(((RecordId) results.get(0)).getValue()).contains("-");
		assertThat(results.get(1)).isEqualTo(DataType.STREAM);
	}

	@Test // DATAREDIS-864
	@IfProfileValue(name = "redisVersion", value = "5.0")
	@WithRedisDriver({ RedisDriver.LETTUCE })
	public void xReadShouldReadMessage() {

		actual.add(connection.xAdd(KEY_1, Collections.singletonMap(KEY_2, VALUE_2)));
		actual.add(connection.xReadAsString(StreamOffset.create(KEY_1, ReadOffset.from("0"))));

		List<Object> results = getResults();

		List<MapRecord<String, String, String>> messages = (List) results.get(1);

		assertThat(messages.get(0).getStream()).isEqualTo(KEY_1);
		assertThat(messages.get(0).getValue()).isEqualTo(Collections.singletonMap(KEY_2, VALUE_2));
	}

	@Test // DATAREDIS-864
	@IfProfileValue(name = "redisVersion", value = "5.0")
	@WithRedisDriver({ RedisDriver.LETTUCE })
	public void xReadGroupShouldReadMessage() {

		actual.add(connection.xAdd(KEY_1, Collections.singletonMap(KEY_2, VALUE_2)));
		actual.add(connection.xGroupCreate(KEY_1, ReadOffset.from("0"), "my-group"));
		actual.add(connection.xReadGroupAsString(Consumer.from("my-group", "my-consumer"),
				StreamOffset.create(KEY_1, ReadOffset.lastConsumed())));
		actual.add(connection.xReadGroupAsString(Consumer.from("my-group", "my-consumer"),
				StreamOffset.create(KEY_1, ReadOffset.lastConsumed())));

		List<Object> results = getResults();

		List<MapRecord<String, String, String>> messages = (List) results.get(2);

		assertThat(messages.get(0).getStream()).isEqualTo(KEY_1);
		assertThat(messages.get(0).getValue()).isEqualTo(Collections.singletonMap(KEY_2, VALUE_2));

		assertThat((List<MapRecord>) results.get(3)).isEmpty();
	}

	@Test // DATAREDIS-864
	@IfProfileValue(name = "redisVersion", value = "5.0")
	@WithRedisDriver({ RedisDriver.LETTUCE })
	public void xRangeShouldReportMessages() {

		actual.add(connection.xAdd(KEY_1, Collections.singletonMap(KEY_2, VALUE_2)));
		actual.add(connection.xAdd(KEY_1, Collections.singletonMap(KEY_3, VALUE_3)));
		actual.add(connection.xRange(KEY_1, org.springframework.data.domain.Range.unbounded()));

		List<Object> results = getResults();
		assertThat(results).hasSize(3);

		List<MapRecord<String, String, String>> messages = (List) results.get(2);

		assertThat(messages.get(0).getStream()).isEqualTo(KEY_1);
		assertThat(messages.get(0).getValue()).isEqualTo(Collections.singletonMap(KEY_2, VALUE_2));

		assertThat(messages.get(1).getStream()).isEqualTo(KEY_1);
		assertThat(messages.get(1).getValue()).isEqualTo(Collections.singletonMap(KEY_3, VALUE_3));
	}

	@Test // DATAREDIS-864
	@IfProfileValue(name = "redisVersion", value = "5.0")
	@WithRedisDriver({ RedisDriver.LETTUCE })
	public void xRevRangeShouldReportMessages() {

		actual.add(connection.xAdd(KEY_1, Collections.singletonMap(KEY_2, VALUE_2)));
		actual.add(connection.xAdd(KEY_1, Collections.singletonMap(KEY_3, VALUE_3)));
		actual.add(connection.xRevRange(KEY_1, org.springframework.data.domain.Range.unbounded()));

		List<Object> results = getResults();
		assertThat(results).hasSize(3);
		assertThat(((RecordId) results.get(0)).getValue()).contains("-");

		List<MapRecord<String, String, String>> messages = (List) results.get(2);

		assertThat(messages.get(0).getStream()).isEqualTo(KEY_1);
		assertThat(messages.get(0).getValue()).isEqualTo(Collections.singletonMap(KEY_3, VALUE_3));

		assertThat(messages.get(1).getStream()).isEqualTo(KEY_1);
		assertThat(messages.get(1).getValue()).isEqualTo(Collections.singletonMap(KEY_2, VALUE_2));
	}

	@Test // DATAREDIS-1207
	@IfProfileValue(name = "redisVersion", value = "5.0")
	@WithRedisDriver({ RedisDriver.LETTUCE })
	public void xRevRangeShouldWorkWithBoundedRange() {

		actual.add(connection.xAdd(KEY_1, Collections.singletonMap(KEY_2, VALUE_2)));
		actual.add(connection.xAdd(KEY_1, Collections.singletonMap(KEY_3, VALUE_3)));
		actual.add(connection.xRevRange(KEY_1, org.springframework.data.domain.Range.closed("0-0", "+")));

		List<Object> results = getResults();
		assertThat(results).hasSize(3);

		List<MapRecord<String, String, String>> messages = (List) results.get(2);
		assertThat(messages).hasSize(2);

		assertThat(messages.get(0).getStream()).isEqualTo(KEY_1);
		assertThat(messages.get(0).getValue()).isEqualTo(Collections.singletonMap(KEY_3, VALUE_3));

		assertThat(messages.get(1).getStream()).isEqualTo(KEY_1);
		assertThat(messages.get(1).getValue()).isEqualTo(Collections.singletonMap(KEY_2, VALUE_2));
	}

	protected void verifyResults(List<Object> expected) {
		assertThat(getResults()).isEqualTo(expected);
	}

	protected List<Object> getResults() {
		return actual;
	}

	protected void initConnection() {
		actual = new ArrayList<>();
	}

	protected class KeyExpired implements TestCondition {
		private String key;

		public KeyExpired(String key) {
			this.key = key;
		}

		public boolean passes() {
			return (!connection.exists(key));
		}
	}

}
