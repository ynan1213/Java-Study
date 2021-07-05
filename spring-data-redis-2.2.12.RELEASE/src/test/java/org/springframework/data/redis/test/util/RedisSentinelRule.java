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
package org.springframework.data.redis.test.util;

import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;

/**
 * @author Christoph Strobl
 */
public class RedisSentinelRule implements TestRule {

	public enum SentinelsAvailable {
		ALL_ACTIVE, ONE_ACTIVE, NONE_ACTIVE
	}

	private static final RedisSentinelConfiguration DEFAULT_SENTINEL_CONFIG = new RedisSentinelConfiguration()
			.master("mymaster").sentinel("127.0.0.1", 26379).sentinel("127.0.0.1", 26380).sentinel("127.0.0.1", 26381);

	private RedisSentinelConfiguration sentinelConfig;
	private SentinelsAvailable requiredSentinels;

	private Map<Object, Boolean> cache = new HashMap<>();

	protected RedisSentinelRule(RedisSentinelConfiguration config) {
		this.sentinelConfig = config;
	}

	/**
	 * Create new {@link RedisSentinelRule} for given {@link RedisSentinelConfiguration}.
	 *
	 * @param config
	 * @return
	 */
	public static RedisSentinelRule forConfig(RedisSentinelConfiguration config) {
		return new RedisSentinelRule(config != null ? config : DEFAULT_SENTINEL_CONFIG);
	}

	/**
	 * Create new {@link RedisSentinelRule} using default configuration.
	 *
	 * @return
	 */
	public static RedisSentinelRule withDefaultConfig() {
		return new RedisSentinelRule(DEFAULT_SENTINEL_CONFIG);
	}

	public RedisSentinelRule sentinelsDisabled() {

		this.requiredSentinels = SentinelsAvailable.NONE_ACTIVE;
		return this;
	}

	/**
	 * Verifies all {@literal Sentinel} nodes are available.
	 *
	 * @return
	 */
	public RedisSentinelRule allActive() {

		this.requiredSentinels = SentinelsAvailable.ALL_ACTIVE;
		return this;
	}

	/**
	 * Verifies at least one {@literal Sentinel} node is available.
	 *
	 * @return
	 */
	public RedisSentinelRule oneActive() {

		this.requiredSentinels = SentinelsAvailable.ONE_ACTIVE;
		return this;
	}

	/**
	 * Will only check {@link RedisSentinelConfiguration} configuration in case {@link RequiresRedisSentinel} is detected
	 * on test method.
	 *
	 * @return
	 */
	public RedisSentinelRule dynamicModeSelection() {
		this.requiredSentinels = null;
		return this;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {

		return new Statement() {

			@Override
			public void evaluate() throws Throwable {

				if (description.isTest()) {
					RequiresRedisSentinel sentinels = description.getAnnotation(RequiresRedisSentinel.class);
					if (RedisSentinelRule.this.requiredSentinels != null || sentinels != null) {
						verify(sentinels != null ? sentinels.value() : RedisSentinelRule.this.requiredSentinels);
					}

				} else {
					verify(RedisSentinelRule.this.requiredSentinels);
				}

				base.evaluate();
			}
		};
	}

	private void verify(SentinelsAvailable verificationMode) {

		int failed = 0;
		for (RedisNode node : sentinelConfig.getSentinels()) {

			if (cache.isEmpty() || !cache.containsKey(node.asString())) {
				cache.put(node.asString(), isAvailable(node));
			}

			if (!cache.get(node.asString())) {
				failed++;
			}
		}

		if (failed > 0) {
			if (SentinelsAvailable.ALL_ACTIVE.equals(verificationMode)) {
				throw new AssumptionViolatedException(String.format(
						"Expected all Redis Sentinels to respone but %s of %s did not responde", failed, sentinelConfig
								.getSentinels().size()));
			}

			if (SentinelsAvailable.ONE_ACTIVE.equals(verificationMode) && sentinelConfig.getSentinels().size() - 1 < failed) {
				throw new AssumptionViolatedException(
						"Expected at least one sentinel to respond but it seems all are offline - Game Over!");
			}
		}

		if (SentinelsAvailable.NONE_ACTIVE.equals(verificationMode) && failed != sentinelConfig.getSentinels().size()) {
			throw new AssumptionViolatedException(String.format(
					"Expected to have no sentinels online but found that %s are still alive.", (sentinelConfig.getSentinels()
							.size() - failed)));
		}
	}

	private boolean isAvailable(RedisNode node) {

		Jedis jedis = null;
		try {

			jedis = new Jedis(node.getHost(), node.getPort());
			jedis.ping();

			return true;
		} catch (Exception e) {
			return false;
		} finally {

			if (jedis != null) {
				try {

					jedis.disconnect();
					if (jedis.getClient().getSocket().isConnected()) {
						jedis.getClient().getSocket().close();
						Thread.sleep(5);
					}

					jedis.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
