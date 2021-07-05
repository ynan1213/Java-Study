/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.data.redis.stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.NestedMultiOutput;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.redis.ConnectionFactoryTracker;
import org.springframework.data.redis.RedisVersionUtils;
import org.springframework.data.redis.SettingsUtils;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceTestClientResources;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.springframework.util.NumberUtils;

/**
 * Integration tests for {@link StreamMessageListenerContainer}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
public class StreamMessageListenerContainerIntegrationTests {

	private static final RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration(
			SettingsUtils.getHost(), SettingsUtils.getPort());

	private static RedisConnectionFactory connectionFactory;

	private StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
	private StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> containerOptions = StreamMessageListenerContainerOptions
			.builder().pollTimeout(Duration.ofMillis(100)).build();

	@BeforeClass
	public static void beforeClass() {

		LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder() //
				.shutdownTimeout(Duration.ZERO) //
				.clientResources(LettuceTestClientResources.getSharedClientResources()) //
				.build();

		LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(standaloneConfiguration,
				clientConfiguration);
		lettuceConnectionFactory.afterPropertiesSet();

		ConnectionFactoryTracker.add(lettuceConnectionFactory);

		connectionFactory = lettuceConnectionFactory;

		assumeTrue(RedisVersionUtils.atLeast("5.0", connectionFactory.getConnection()));
	}

	@AfterClass
	public static void tearDown() {
		ConnectionFactoryTracker.cleanUp();
	}

	@Before
	public void before() {

		RedisConnection connection = connectionFactory.getConnection();
		connection.flushDb();
		connection.close();
	}

	@Test // DATAREDIS-864
	public void shouldReceiveMapMessages() throws InterruptedException {

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);
		BlockingQueue<MapRecord<String, String, String>> queue = new LinkedBlockingQueue<>();

		container.start();
		Subscription subscription = container.receive(StreamOffset.create("my-stream", ReadOffset.from("0-0")), queue::add);

		subscription.await(Duration.ofSeconds(2));

		redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value1"));
		redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value2"));
		redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value3"));

		assertThat(queue.poll(1, TimeUnit.SECONDS)).isNotNull();
		assertThat(queue.poll(1, TimeUnit.SECONDS)).isNotNull();
		assertThat(queue.poll(1, TimeUnit.SECONDS)).isNotNull();

		cancelAwait(subscription);

		assertThat(subscription.isActive()).isFalse();
	}

	@Test // DATAREDIS-864
	public void shouldReceiveSimpleObjectHashRecords() throws InterruptedException {

		StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> containerOptions = StreamMessageListenerContainerOptions
				.builder().pollTimeout(Duration.ofMillis(100)).targetType(String.class).build();

		StreamMessageListenerContainer<String, ObjectRecord<String, String>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);
		BlockingQueue<ObjectRecord<String, String>> queue = new LinkedBlockingQueue<>();

		container.start();
		Subscription subscription = container.receive(StreamOffset.create("my-stream", ReadOffset.from("0-0")), queue::add);

		subscription.await(Duration.ofSeconds(2));

		redisTemplate.opsForStream().add(ObjectRecord.create("my-stream", "value1"));

		assertThat(queue.poll(1, TimeUnit.SECONDS)).isNotNull().extracting(Record::getValue).isEqualTo("value1");

		cancelAwait(subscription);

		assertThat(subscription.isActive()).isFalse();
	}

	@Test // DATAREDIS-864
	public void shouldReceiveObjectHashRecords() throws InterruptedException {

		StreamMessageListenerContainerOptions<String, ObjectRecord<String, LoginEvent>> containerOptions = StreamMessageListenerContainerOptions
				.builder().pollTimeout(Duration.ofMillis(100)).targetType(LoginEvent.class).build();

		StreamMessageListenerContainer<String, ObjectRecord<String, LoginEvent>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);
		BlockingQueue<ObjectRecord<String, LoginEvent>> queue = new LinkedBlockingQueue<>();

		container.start();
		Subscription subscription = container.receive(StreamOffset.create("my-stream", ReadOffset.from("0-0")), queue::add);

		subscription.await(Duration.ofSeconds(2));

		redisTemplate.opsForStream().add(ObjectRecord.create("my-stream", new LoginEvent("Walter", "White")));

		assertThat(queue.poll(1, TimeUnit.SECONDS)).isNotNull().extracting(Record::getValue)
				.isEqualTo(new LoginEvent("Walter", "White"));

		cancelAwait(subscription);

		assertThat(subscription.isActive()).isFalse();
	}

	@Test // DATAREDIS-864, DATAREDIS-1079
	public void shouldReceiveMessagesInConsumerGroup() throws InterruptedException {

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);
		BlockingQueue<MapRecord<String, String, String>> queue = new LinkedBlockingQueue<>();
		RecordId messageId = redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value1"));
		redisTemplate.opsForStream().createGroup("my-stream", ReadOffset.from(messageId), "my-group");

		container.start();
		Subscription subscription = container.receive(Consumer.from("my-group", "my-consumer"),
				StreamOffset.create("my-stream", ReadOffset.lastConsumed()), queue::add);

		subscription.await(Duration.ofSeconds(2));

		redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value2"));

		MapRecord<String, String, String> message = queue.poll(1, TimeUnit.SECONDS);
		assertThat(message).isNotNull();
		assertThat(message.getValue()).containsEntry("key", "value2");

		assertThat(getNumberOfPending("my-stream", "my-group")).isOne();

		cancelAwait(subscription);
	}

	@Test // DATAREDIS-1079
	public void shouldReceiveAndAckMessagesInConsumerGroup() throws InterruptedException {

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);
		BlockingQueue<MapRecord<String, String, String>> queue = new LinkedBlockingQueue<>();
		RecordId messageId = redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value1"));
		redisTemplate.opsForStream().createGroup("my-stream", ReadOffset.from(messageId), "my-group");

		container.start();
		Subscription subscription = container.receiveAutoAck(Consumer.from("my-group", "my-consumer"),
				StreamOffset.create("my-stream", ReadOffset.lastConsumed()), queue::add);

		subscription.await(Duration.ofSeconds(2));

		redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value2"));

		MapRecord<String, String, String> message = queue.poll(1, TimeUnit.SECONDS);
		assertThat(message).isNotNull();
		assertThat(message.getValue()).containsEntry("key", "value2");

		assertThat(getNumberOfPending("my-stream", "my-group")).isZero();

		cancelAwait(subscription);
	}

	@Test // DATAREDIS-864
	public void shouldUseCustomErrorHandler() throws InterruptedException {

		BlockingQueue<Throwable> failures = new LinkedBlockingQueue<>();

		StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> containerOptions = StreamMessageListenerContainerOptions
				.builder().errorHandler(failures::add).pollTimeout(Duration.ofMillis(100)).build();
		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);

		container.start();
		Subscription subscription = container.receive(Consumer.from("my-group", "my-consumer"),
				StreamOffset.create("my-stream", ReadOffset.lastConsumed()), it -> {});

		subscription.await(Duration.ofSeconds(2));

		Throwable error = failures.poll(1, TimeUnit.SECONDS);
		assertThat(failures).isEmpty();
		assertThat(error).isNotNull();

		cancelAwait(subscription);
	}

	@Test // DATAREDIS-864
	public void errorShouldStopListening() throws InterruptedException {

		BlockingQueue<Throwable> failures = new LinkedBlockingQueue<>();

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);

		StreamReadRequest<String> readRequest = StreamReadRequest
				.builder(StreamOffset.create("my-stream", ReadOffset.lastConsumed())).errorHandler(failures::add)
				.consumer(Consumer.from("my-group", "my-consumer")).build();

		RecordId messageId = redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value1"));
		redisTemplate.opsForStream().createGroup("my-stream", ReadOffset.from(messageId), "my-group");

		container.start();
		Subscription subscription = container.register(readRequest, it -> {});

		subscription.await(Duration.ofSeconds(1));

		redisTemplate.delete("my-stream");

		subscription.await(Duration.ofSeconds(1));

		assertThat(failures.poll(1, TimeUnit.SECONDS)).isNotNull();
		assertThat(subscription.isActive()).isFalse();

		cancelAwait(subscription);
	}

	@Test // DATAREDIS-864
	public void customizedCancelPredicateShouldNotStopListening() throws InterruptedException {

		BlockingQueue<Throwable> failures = new LinkedBlockingQueue<>();

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);

		StreamReadRequest<String> readRequest = StreamReadRequest
				.builder(StreamOffset.create("my-stream", ReadOffset.lastConsumed())) //
				.errorHandler(failures::add) // //
				.cancelOnError(t -> false) //
				.consumer(Consumer.from("my-group", "my-consumer")) //
				.build();

		RecordId messageId = redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value1"));
		redisTemplate.opsForStream().createGroup("my-stream", ReadOffset.from(messageId), "my-group");

		container.start();
		Subscription subscription = container.register(readRequest, it -> {});

		subscription.await(Duration.ofSeconds(2));

		redisTemplate.delete("my-stream");

		assertThat(failures.poll(1, TimeUnit.SECONDS)).isNotNull();
		assertThat(failures.poll(1, TimeUnit.SECONDS)).isNotNull();
		assertThat(subscription.isActive()).isTrue();

		cancelAwait(subscription);
	}

	@Test // DATAREDIS-864
	public void cancelledStreamShouldNotReceiveMessages() throws InterruptedException {

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);
		BlockingQueue<MapRecord<String, String, String>> queue = new LinkedBlockingQueue<>();

		container.start();
		Subscription subscription = container.receive(StreamOffset.create("my-stream", ReadOffset.from("0-0")), queue::add);

		subscription.await(Duration.ofSeconds(2));
		cancelAwait(subscription);

		redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value4"));

		assertThat(queue.poll(200, TimeUnit.MILLISECONDS)).isNull();
	}

	@Test // DATAREDIS-864
	public void containerRestartShouldRestartSubscription() throws InterruptedException {

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer
				.create(connectionFactory, containerOptions);
		BlockingQueue<MapRecord<String, String, String>> queue = new LinkedBlockingQueue<>();

		container.start();
		Subscription subscription = container.receive(StreamOffset.create("my-stream", ReadOffset.from("0-0")), queue::add);

		subscription.await(Duration.ofSeconds(2));

		container.stop();

		while (subscription.isActive()) {
			Thread.sleep(10);
		}

		container.start();

		subscription.await(Duration.ofSeconds(2));

		redisTemplate.opsForStream().add("my-stream", Collections.singletonMap("key", "value1"));

		assertThat(queue.poll(1, TimeUnit.SECONDS)).isNotNull();

		cancelAwait(subscription);
	}

	private static void cancelAwait(Subscription subscription) throws InterruptedException {

		subscription.cancel();

		while (subscription.isActive()) {
			Thread.sleep(10);
		}
	}

	private int getNumberOfPending(String stream, String group) {

		String value = ((List) ((LettuceConnection) connectionFactory.getConnection()).execute("XPENDING",
				new NestedMultiOutput<>(StringCodec.UTF8), new byte[][] { stream.getBytes(), group.getBytes() })).get(0)
						.toString();
		return NumberUtils.parseNumber(value, Integer.class);
	}

	@Data
	@AllArgsConstructor
	static class LoginEvent {
		String firstname, lastname;
	}
}
