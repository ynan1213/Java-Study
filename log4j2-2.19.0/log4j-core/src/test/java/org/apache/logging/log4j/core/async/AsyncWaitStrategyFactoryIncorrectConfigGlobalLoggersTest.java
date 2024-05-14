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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Category(AsyncLoggers.class)
public class AsyncWaitStrategyFactoryIncorrectConfigGlobalLoggersTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                AsyncLoggerContextSelector.class.getName());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "AsyncWaitStrategyIncorrectFactoryConfigGlobalLoggerTest.xml");
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
    }


    @Test
    public void testIncorrectConfigWaitStrategyFactory() throws Exception {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        assertThat("context is AsyncLoggerContext", context instanceof AsyncLoggerContext);

        AsyncWaitStrategyFactory asyncWaitStrategyFactory = context.getConfiguration().getAsyncWaitStrategyFactory();
        assertNull(asyncWaitStrategyFactory);

        AsyncLogger logger = (AsyncLogger) context.getRootLogger();
        AsyncLoggerDisruptor delegate = logger.getAsyncLoggerDisruptor();
        assertEquals(TimeoutBlockingWaitStrategy.class, delegate.getWaitStrategy().getClass());
        assertThat("waitstrategy is TimeoutBlockingWaitStrategy", delegate.getWaitStrategy() instanceof TimeoutBlockingWaitStrategy);
    }
}
