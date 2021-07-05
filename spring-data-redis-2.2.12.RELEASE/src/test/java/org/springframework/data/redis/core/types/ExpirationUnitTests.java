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

package org.springframework.data.redis.core.types;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class ExpirationUnitTests {

	@Test // DATAREDIS-316
	public void fromDefault() throws Exception {

		Expiration expiration = Expiration.from(5, null);

		assertThat(expiration.getExpirationTime()).isEqualTo(5L);
		assertThat(expiration.getTimeUnit()).isEqualTo(TimeUnit.SECONDS);
	}

	@Test // DATAREDIS-316
	public void fromNanos() throws Exception {

		Expiration expiration = Expiration.from(5L * 1000 * 1000, TimeUnit.NANOSECONDS);

		assertThat(expiration.getExpirationTime()).isEqualTo(5L);
		assertThat(expiration.getTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
	}

	@Test // DATAREDIS-316
	public void fromMinutes() throws Exception {

		Expiration expiration = Expiration.from(5, TimeUnit.MINUTES);

		assertThat(expiration.getExpirationTime()).isEqualTo(5L * 60);
		assertThat(expiration.getTimeUnit()).isEqualTo(TimeUnit.SECONDS);
	}
}
