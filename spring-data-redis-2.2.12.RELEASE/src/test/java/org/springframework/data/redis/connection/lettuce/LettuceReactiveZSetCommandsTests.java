/*
 * Copyright 2016-2020 the original author or authors.
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
import static org.junit.Assume.*;
import static org.springframework.data.domain.Range.Bound.*;

import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.DefaultTuple;
import org.springframework.data.redis.core.ScanOptions;

/**
 * Integration tests for {@link LettuceReactiveZSetCommands}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Michele Mancioppi
 */
public class LettuceReactiveZSetCommandsTests extends LettuceReactiveCommandsTestsBase {

	private static final Range<Long> ONE_TO_TWO = Range.closed(1L, 2L);

	private static final Range<Double> TWO_TO_THREE_ALL_INCLUSIVE = Range.closed(2D, 3D);
	private static final Range<Double> TWO_INCLUSIVE_TO_THREE_EXCLUSIVE = Range.rightOpen(2D, 3D);
	private static final Range<Double> TWO_EXCLUSIVE_TO_THREE_INCLUSIVE = Range.leftOpen(2D, 3D);

	@Test // DATAREDIS-525
	public void zAddShouldAddValuesWithScores() {
		assertThat(connection.zSetCommands().zAdd(KEY_1_BBUFFER, 3.5D, VALUE_1_BBUFFER).block()).isEqualTo(1L);
	}

	@Test // DATAREDIS-525
	public void zRemShouldRemoveValuesFromSet() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRem(KEY_1_BBUFFER, Arrays.asList(VALUE_1_BBUFFER, VALUE_3_BBUFFER)).block())
				.isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zIncrByShouldInreaseAndReturnScore() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);

		assertThat(connection.zSetCommands().zIncrBy(KEY_1_BBUFFER, 3.5D, VALUE_1_BBUFFER).block()).isEqualTo(4.5D);
	}

	@Test // DATAREDIS-525
	public void zRankShouldReturnIndexCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRank(KEY_1_BBUFFER, VALUE_3_BBUFFER).block()).isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zRevRankShouldReturnIndexCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRevRank(KEY_1_BBUFFER, VALUE_3_BBUFFER).block()).isEqualTo(0L);
	}

	@Test // DATAREDIS-525
	public void zRangeShouldReturnValuesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRange(KEY_1_BBUFFER, ONE_TO_TWO)) //
				.expectNext(VALUE_2_BBUFFER, VALUE_3_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRangeWithScoreShouldReturnTuplesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRangeWithScores(KEY_1_BBUFFER, ONE_TO_TWO)) //
				.expectNext(new DefaultTuple(VALUE_2_BBUFFER.array(), 2D), new DefaultTuple(VALUE_3_BBUFFER.array(), 3D)) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRevRangeShouldReturnValuesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRevRange(KEY_1_BBUFFER, ONE_TO_TWO)) //
				.expectNext(VALUE_2_BBUFFER, VALUE_1_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRevRangeWithScoreShouldReturnTuplesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRevRangeWithScores(KEY_1_BBUFFER, ONE_TO_TWO)) //
				.expectNext(new DefaultTuple(VALUE_2_BBUFFER.array(), 2D), new DefaultTuple(VALUE_1_BBUFFER.array(), 1D)) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRangeByScoreShouldReturnValuesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRangeByScore(KEY_1_BBUFFER, Range.closed(2D, 3D))) //
				.expectNext(VALUE_2_BBUFFER, VALUE_3_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-852
	public void zRangeByScoreShouldReturnValuesCorrectlyWithMinUnbounded() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier
				.create(connection.zSetCommands().zRangeByScore(KEY_1_BBUFFER,
						Range.of(Range.Bound.unbounded(), Range.Bound.inclusive(3D)))) //
				.expectNext(VALUE_1_BBUFFER, VALUE_2_BBUFFER, VALUE_3_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-852
	public void zRangeByScoreShouldReturnValuesCorrectlyWithMaxUnbounded() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier
				.create(connection.zSetCommands().zRangeByScore(KEY_1_BBUFFER,
						Range.of(Range.Bound.inclusive(0D), Range.Bound.unbounded()))) //
				.expectNext(VALUE_1_BBUFFER, VALUE_2_BBUFFER, VALUE_3_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRangeByScoreShouldReturnValuesCorrectlyWithMinExclusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRangeByScore(KEY_1_BBUFFER, TWO_EXCLUSIVE_TO_THREE_INCLUSIVE)) //
				.expectNext(VALUE_3_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRangeByScoreShouldReturnValuesCorrectlyWithMaxExclusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRangeByScore(KEY_1_BBUFFER, TWO_INCLUSIVE_TO_THREE_EXCLUSIVE)) //
				.expectNext(VALUE_2_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRangeByScoreWithScoreShouldReturnTuplesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRangeByScoreWithScores(KEY_1_BBUFFER, TWO_TO_THREE_ALL_INCLUSIVE)) //
				.expectNext(new DefaultTuple(VALUE_2_BBUFFER.array(), 2D), new DefaultTuple(VALUE_3_BBUFFER.array(), 3D)) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRangeByScoreWithScoreShouldReturnTuplesCorrectlyWithMinExclusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier
				.create(connection.zSetCommands().zRangeByScoreWithScores(KEY_1_BBUFFER, TWO_EXCLUSIVE_TO_THREE_INCLUSIVE)) //
				.expectNext(new DefaultTuple(VALUE_3_BBUFFER.array(), 3D)) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRangeByScoreWithScoreShouldReturnTuplesCorrectlyWithMaxExclusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier
				.create(connection.zSetCommands().zRangeByScoreWithScores(KEY_1_BBUFFER, TWO_INCLUSIVE_TO_THREE_EXCLUSIVE)) //
				.expectNext(new DefaultTuple(VALUE_2_BBUFFER.array(), 2D)) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRevRangeByScoreShouldReturnValuesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRevRangeByScore(KEY_1_BBUFFER, TWO_TO_THREE_ALL_INCLUSIVE)) //
				.expectNext(VALUE_3_BBUFFER, VALUE_2_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRevRangeByScoreShouldReturnValuesCorrectlyWithMinExclusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRevRangeByScore(KEY_1_BBUFFER, TWO_EXCLUSIVE_TO_THREE_INCLUSIVE)) //
				.expectNext(VALUE_3_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRevRangeByScoreShouldReturnValuesCorrectlyWithMaxExclusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRevRangeByScore(KEY_1_BBUFFER, TWO_INCLUSIVE_TO_THREE_EXCLUSIVE)) //
				.expectNext(VALUE_2_BBUFFER) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRevRangeByScoreWithScoreShouldReturnTuplesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zRevRangeByScoreWithScores(KEY_1_BBUFFER, TWO_TO_THREE_ALL_INCLUSIVE)) //
				.expectNext(new DefaultTuple(VALUE_3_BBUFFER.array(), 3D), new DefaultTuple(VALUE_2_BBUFFER.array(), 2D)) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRevRangeByScoreWithScoreShouldReturnTuplesCorrectlyWithMinExclusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier
				.create(connection.zSetCommands().zRevRangeByScoreWithScores(KEY_1_BBUFFER, TWO_EXCLUSIVE_TO_THREE_INCLUSIVE)) //
				.expectNext(new DefaultTuple(VALUE_3_BBUFFER.array(), 3D)) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zRevRangeByScoreWithScoreShouldReturnTuplesCorrectlyWithMaxExclusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier
				.create(connection.zSetCommands().zRevRangeByScoreWithScores(KEY_1_BBUFFER, TWO_INCLUSIVE_TO_THREE_EXCLUSIVE)) //
				.expectNext(new DefaultTuple(VALUE_2_BBUFFER.array(), 2D)) //
				.verifyComplete();
	}

	@Test // DATAREDIS-743
	public void zScanShouldIterateOverSortedSet() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		StepVerifier.create(connection.zSetCommands().zScan(KEY_1_BBUFFER, ScanOptions.scanOptions().count(1).build())) //
				.expectNextCount(3).verifyComplete();

		StepVerifier
				.create(connection.zSetCommands().zScan(KEY_1_BBUFFER, ScanOptions.scanOptions().match(VALUE_2).build())) //
				.expectNext(new DefaultTuple(VALUE_2_BBUFFER.array(), 2D)) //
				.verifyComplete();
	}

	@Test // DATAREDIS-525
	public void zCountShouldCountValuesInRange() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zCount(KEY_1_BBUFFER, TWO_TO_THREE_ALL_INCLUSIVE).block()).isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zCountShouldCountValuesInRangeWithMinExlusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zCount(KEY_1_BBUFFER, TWO_EXCLUSIVE_TO_THREE_INCLUSIVE).block()).isEqualTo(1L);
	}

	@Test // DATAREDIS-525
	public void zCountShouldCountValuesInRangeWithMaxExlusion() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zCount(KEY_1_BBUFFER, TWO_INCLUSIVE_TO_THREE_EXCLUSIVE).block()).isEqualTo(1L);
	}

	@Test // DATAREDIS-525
	public void zCountShouldCountValuesInRangeWithNegativeInfinity() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zCount(KEY_1_BBUFFER, Range.leftUnbounded(inclusive(2D))).block())
				.isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zCountShouldCountValuesInRangeWithPositiveInfinity() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zCount(KEY_1_BBUFFER, Range.rightUnbounded(inclusive(2D))).block())
				.isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zCardShouldReturnSizeCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zCard(KEY_1_BBUFFER).block()).isEqualTo(3L);
	}

	@Test // DATAREDIS-525
	public void zScoreShouldReturnScoreCorrectly() {

		nativeCommands.zadd(KEY_1, 2D, VALUE_2);

		assertThat(connection.zSetCommands().zScore(KEY_1_BBUFFER, VALUE_2_BBUFFER).block()).isEqualTo(2D);
	}

	@Test // DATAREDIS-525
	public void zRemRangeByRankShouldRemoveValuesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRemRangeByRank(KEY_1_BBUFFER, ONE_TO_TWO).block()).isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zRemRangeByScoreShouldRemoveValuesCorrectly() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRemRangeByScore(KEY_1_BBUFFER, Range.closed(1D, 2D)).block()).isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zRemRangeByScoreShouldRemoveValuesCorrectlyWithNegativeInfinity() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRemRangeByScore(KEY_1_BBUFFER, Range.leftUnbounded(inclusive(2D))).block())
				.isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zRemRangeByScoreShouldRemoveValuesCorrectlyWithPositiveInfinity() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRemRangeByScore(KEY_1_BBUFFER, Range.rightUnbounded(inclusive(2D))).block())
				.isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zRemRangeByScoreShouldRemoveValuesCorrectlyWithExcludingMinRange() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRemRangeByScore(KEY_1_BBUFFER, TWO_EXCLUSIVE_TO_THREE_INCLUSIVE).block())
				.isEqualTo(1L);
	}

	@Test // DATAREDIS-525
	public void zRemRangeByScoreShouldRemoveValuesCorrectlyWithExcludingMaxRange() {

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_1, 3D, VALUE_3);

		assertThat(connection.zSetCommands().zRemRangeByScore(KEY_1_BBUFFER, TWO_INCLUSIVE_TO_THREE_EXCLUSIVE).block())
				.isEqualTo(1L);
	}

	@Test // DATAREDIS-525
	public void zUnionStoreShouldWorkCorrectly() {

		assumeTrue(connectionProvider instanceof StandaloneConnectionProvider);

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_2, 1D, VALUE_1);
		nativeCommands.zadd(KEY_2, 2D, VALUE_2);
		nativeCommands.zadd(KEY_2, 3D, VALUE_3);

		assertThat(connection.zSetCommands()
				.zUnionStore(KEY_3_BBUFFER, Arrays.asList(KEY_1_BBUFFER, KEY_2_BBUFFER), Arrays.asList(2D, 3D)).block())
						.isEqualTo(3L);
	}

	@Test // DATAREDIS-525
	public void zInterStoreShouldWorkCorrectly() {

		assumeTrue(connectionProvider instanceof StandaloneConnectionProvider);

		nativeCommands.zadd(KEY_1, 1D, VALUE_1);
		nativeCommands.zadd(KEY_1, 2D, VALUE_2);
		nativeCommands.zadd(KEY_2, 1D, VALUE_1);
		nativeCommands.zadd(KEY_2, 2D, VALUE_2);
		nativeCommands.zadd(KEY_2, 3D, VALUE_3);

		assertThat(connection.zSetCommands()
				.zInterStore(KEY_3_BBUFFER, Arrays.asList(KEY_1_BBUFFER, KEY_2_BBUFFER), Arrays.asList(2D, 3D)).block())
						.isEqualTo(2L);
	}

	@Test // DATAREDIS-525
	public void zRangeByLex() {

		nativeCommands.zadd(KEY_1, 0D, "a");
		nativeCommands.zadd(KEY_1, 0D, "b");
		nativeCommands.zadd(KEY_1, 0D, "c");
		nativeCommands.zadd(KEY_1, 0D, "d");
		nativeCommands.zadd(KEY_1, 0D, "e");
		nativeCommands.zadd(KEY_1, 0D, "f");
		nativeCommands.zadd(KEY_1, 0D, "g");

		Range<String> emptyToC = Range.closed("", "c");

		assertThat(connection.zSetCommands().zRangeByLex(KEY_1_BBUFFER, emptyToC).collectList().block()).containsExactly(
				ByteBuffer.wrap("a".getBytes()), ByteBuffer.wrap("b".getBytes()), ByteBuffer.wrap("c".getBytes()));

		assertThat(connection.zSetCommands().zRangeByLex(KEY_1_BBUFFER, Range.rightOpen("", "c")).collectList().block())
				.containsExactly(ByteBuffer.wrap("a".getBytes()), ByteBuffer.wrap("b".getBytes()));

		assertThat(connection.zSetCommands().zRangeByLex(KEY_1_BBUFFER, Range.rightOpen("aaa", "g")).collectList().block())
				.containsExactly(ByteBuffer.wrap("b".getBytes()), ByteBuffer.wrap("c".getBytes()),
						ByteBuffer.wrap("d".getBytes()), ByteBuffer.wrap("e".getBytes()), ByteBuffer.wrap("f".getBytes()));
	}

	@Test // DATAREDIS-525
	public void zRevRangeByLex() {

		nativeCommands.zadd(KEY_1, 0D, "a");
		nativeCommands.zadd(KEY_1, 0D, "b");
		nativeCommands.zadd(KEY_1, 0D, "c");
		nativeCommands.zadd(KEY_1, 0D, "d");
		nativeCommands.zadd(KEY_1, 0D, "e");
		nativeCommands.zadd(KEY_1, 0D, "f");
		nativeCommands.zadd(KEY_1, 0D, "g");

		assertThat(connection.zSetCommands().zRevRangeByLex(KEY_1_BBUFFER, Range.closed("", "c")).collectList().block())
				.containsExactly(ByteBuffer.wrap("c".getBytes()), ByteBuffer.wrap("b".getBytes()),
						ByteBuffer.wrap("a".getBytes()));

		assertThat(connection.zSetCommands().zRevRangeByLex(KEY_1_BBUFFER, Range.rightOpen("", "c")).collectList().block())
				.containsExactly(ByteBuffer.wrap("b".getBytes()), ByteBuffer.wrap("a".getBytes()));

		assertThat(
				connection.zSetCommands().zRevRangeByLex(KEY_1_BBUFFER, Range.rightOpen("aaa", "g")).collectList().block())
						.containsExactly(ByteBuffer.wrap("f".getBytes()), ByteBuffer.wrap("e".getBytes()),
								ByteBuffer.wrap("d".getBytes()), ByteBuffer.wrap("c".getBytes()), ByteBuffer.wrap("b".getBytes()));
	}

}
