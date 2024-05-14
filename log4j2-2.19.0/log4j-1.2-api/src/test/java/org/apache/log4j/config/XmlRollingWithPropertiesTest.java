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
package org.apache.log4j.config;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.test.SystemPropertyTestRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * Test configuration from Properties.
 */
public class XmlRollingWithPropertiesTest {

    private static final String TEST_DIR = "target/" + XmlRollingWithPropertiesTest.class.getSimpleName();

    @ClassRule
    public static TestRule SP_RULE = RuleChain.emptyRuleChain()
    //@formatter:off
        .around(SystemPropertyTestRule.create("test.directory", TEST_DIR))
        .around(SystemPropertyTestRule.create("log4j.configuration", "target/test-classes/log4j1-rolling-properties.xml"));
    //@formatter:on

    @Test
    public void testProperties() throws Exception {
        // ${test.directory}/logs/etl.log
        final Path path = Paths.get(TEST_DIR, "logs/etl.log");
        Files.deleteIfExists(path);
        final Logger logger = LogManager.getLogger("test");
        logger.debug("This is a test of the root logger");
        assertTrue("Log file was not created " + path, Files.exists(path));
        assertTrue("Log file is empty " + path, Files.size(path) > 0);
    }

}
