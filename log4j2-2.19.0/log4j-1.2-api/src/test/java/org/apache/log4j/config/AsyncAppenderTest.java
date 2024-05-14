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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.Test;

/**
 * Test configuration from XML.
 */
public class AsyncAppenderTest {

    @Test
    public void testAsyncXml() throws Exception {
        try (final LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/log4j1-async.xml")) {
            final Logger logger = LogManager.getLogger("test");
            logger.debug("This is a test of the root logger");
            Thread.sleep(50);
            final Configuration configuration = loggerContext.getConfiguration();
            final Map<String, Appender> appenders = configuration.getAppenders();
            ListAppender messageAppender = null;
            for (final Map.Entry<String, Appender> entry : appenders.entrySet()) {
                if (entry.getKey().equals("list")) {
                    messageAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
                }
            }
            assertNotNull("No Message Appender", messageAppender);
            final List<String> messages = messageAppender.getMessages();
            assertTrue("No messages", messages != null && messages.size() > 0);
        }
    }

    @Test
    public void testAsyncProperties() throws Exception {
        try (final LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/log4j1-async.properties")) {
            final Logger logger = LogManager.getLogger("test");
            logger.debug("This is a test of the root logger");
            Thread.sleep(50);
            final Configuration configuration = loggerContext.getConfiguration();
            final Map<String, Appender> appenders = configuration.getAppenders();
            ListAppender messageAppender = null;
            for (final Map.Entry<String, Appender> entry : appenders.entrySet()) {
                if (entry.getKey().equals("list")) {
                    messageAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
                }
            }
            assertNotNull("No Message Appender", messageAppender);
            final List<String> messages = messageAppender.getMessages();
            assertTrue("No messages", messages != null && messages.size() > 0);
        }
    }
}
