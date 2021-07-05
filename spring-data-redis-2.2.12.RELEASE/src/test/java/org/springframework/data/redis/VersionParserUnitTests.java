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
package org.springframework.data.redis;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * @author Christoph Strobl
 */
public class VersionParserUnitTests {

	@Test
	public void shouldParseNullToUnknown() {
		assertThat(VersionParser.parseVersion(null)).isEqualTo(Version.UNKNOWN);
	}

	@Test
	public void shouldParseEmptyVersionStringToUnknown() {
		assertThat(VersionParser.parseVersion("")).isEqualTo(Version.UNKNOWN);
	}

	@Test
	public void shouldParseInvalidVersionStringToUnknown() {
		assertThat(VersionParser.parseVersion("ThisIsNoValidVersion")).isEqualTo(Version.UNKNOWN);
	}

	@Test
	public void shouldParseMajorMinorWithoutPatchCorrectly() {
		assertThat(VersionParser.parseVersion("1.2")).isEqualTo(new Version(1, 2, 0));
	}

	@Test
	public void shouldParseMajorMinorPatchCorrectly() {
		assertThat(VersionParser.parseVersion("1.2.3")).isEqualTo(new Version(1, 2, 3));
	}

	@Test
	public void shouldParseMajorWithoutMinorPatchCorrectly() {
		assertThat(VersionParser.parseVersion("1.2.3.a")).isEqualTo(new Version(1, 2, 3));
	}
}
