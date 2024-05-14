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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@LoggerContextSource("log4j-lookup.xml")
public class LookupTest {

    @Test
    public void testHostname(@Named final ConsoleAppender console) {
        final Layout<?> layout = console.getLayout();
        assertNotNull(layout, "No Layout");
        assertTrue(layout instanceof PatternLayout, "Layout is not a PatternLayout");
        final String pattern = ((PatternLayout) layout).getConversionPattern();
        assertNotNull(pattern, "No conversion pattern");
        assertTrue(pattern.contains("org.junit,org.apache.maven,org.eclipse,sun.reflect,java.lang.reflect"),
                "No filters");
    }
}
