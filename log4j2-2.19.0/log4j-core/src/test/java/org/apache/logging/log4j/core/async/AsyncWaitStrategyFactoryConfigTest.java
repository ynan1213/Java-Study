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

import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Category(AsyncLoggers.class)
public class AsyncWaitStrategyFactoryConfigTest {

    @Test
    @LoggerContextSource("AsyncWaitStrategyFactoryConfigTest.xml")
    public void testConfigWaitStrategyFactory(final LoggerContext context) throws Exception {
        AsyncWaitStrategyFactory asyncWaitStrategyFactory = context.getConfiguration().getAsyncWaitStrategyFactory();
        assertEquals(YieldingWaitStrategyFactory.class, asyncWaitStrategyFactory.getClass());
        assertThat("factory is YieldingWaitStrategyFactory", asyncWaitStrategyFactory instanceof YieldingWaitStrategyFactory);
    }

    @Test
    @LoggerContextSource("AsyncWaitStrategyFactoryConfigTest.xml")
    public void testWaitStrategy(final LoggerContext context) throws Exception {

        org.apache.logging.log4j.Logger logger = context.getRootLogger();

        AsyncLoggerConfig loggerConfig = (AsyncLoggerConfig) ((org.apache.logging.log4j.core.Logger) logger).get();
        AsyncLoggerConfigDisruptor delegate = (AsyncLoggerConfigDisruptor) loggerConfig.getAsyncLoggerConfigDelegate();
        assertEquals(YieldingWaitStrategy.class, delegate.getWaitStrategy().getClass());
        assertThat("waitstrategy is YieldingWaitStrategy", delegate.getWaitStrategy() instanceof com.lmax.disruptor.YieldingWaitStrategy);
    }

    @Test
    @LoggerContextSource("AsyncWaitStrategyIncorrectFactoryConfigTest.xml")
    public void testIncorrectConfigWaitStrategyFactory(final LoggerContext context) throws Exception {
        AsyncWaitStrategyFactory asyncWaitStrategyFactory = context.getConfiguration().getAsyncWaitStrategyFactory();
        assertNull(asyncWaitStrategyFactory);
    }

    @Test
    @LoggerContextSource("AsyncWaitStrategyIncorrectFactoryConfigTest.xml")
    public void testIncorrectWaitStrategyFallsBackToDefault(
            @Named("WaitStrategyAppenderList") final ListAppender list1,
            final LoggerContext context) throws Exception {
        org.apache.logging.log4j.Logger logger = context.getRootLogger();

        AsyncLoggerConfig loggerConfig = (AsyncLoggerConfig) ((org.apache.logging.log4j.core.Logger) logger).get();
        AsyncLoggerConfigDisruptor delegate = (AsyncLoggerConfigDisruptor) loggerConfig.getAsyncLoggerConfigDelegate();
        assertEquals(TimeoutBlockingWaitStrategy.class, delegate.getWaitStrategy().getClass());
        assertThat("waitstrategy is TimeoutBlockingWaitStrategy", delegate.getWaitStrategy() instanceof TimeoutBlockingWaitStrategy);
    }

    public static class YieldingWaitStrategyFactory implements AsyncWaitStrategyFactory {
        @Override
        public WaitStrategy createWaitStrategy() {
            return new YieldingWaitStrategy();
        }
    }
}
