/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import java.util.regex.Matcher;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test getEligibleFiles method.
 */
public class RolloverFilePatternTest {

    @Test
    public void testFilePatternWithoutPadding() throws Exception {
      final Matcher matcher = AbstractRolloverStrategy.PATTERN_COUNTER.matcher("target/logs/test-%i.log.gz");
      assertTrue(matcher.matches());
      assertNull(matcher.group("ZEROPAD"));
      assertNull(matcher.group("PADDING"));
    }

    @Test
    public void testFilePatternWithSpacePadding() throws Exception {
      final Matcher matcher = AbstractRolloverStrategy.PATTERN_COUNTER.matcher("target/logs/test-%3i.log.gz");
      assertTrue(matcher.matches());
      assertNull(matcher.group("ZEROPAD"));
      assertEquals("3", matcher.group("PADDING"));
    }

    @Test
    public void testFilePatternWithZeroPadding() throws Exception {
      final Matcher matcher = AbstractRolloverStrategy.PATTERN_COUNTER.matcher("target/logs/test-%03i.log.gz");
      assertTrue(matcher.matches());
      assertEquals("0", matcher.group("ZEROPAD"));
      assertEquals("3", matcher.group("PADDING"));
    }

    @Test
    public void testFilePatternUnmatched() throws Exception {
      final Matcher matcher = AbstractRolloverStrategy.PATTERN_COUNTER.matcher("target/logs/test-%n.log.gz");
      assertFalse(matcher.matches());
    }
}
