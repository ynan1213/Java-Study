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
package org.apache.logging.log4j.core.config;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("functional")
@LoggerContextSource("log4j-set-level.xml")
public class ConfiguratorSetLevelTest {

    private final ListAppender app1;
    private final LoggerContext loggerContext;
    private org.apache.logging.log4j.Logger logger1;

    public ConfiguratorSetLevelTest(final LoggerContext context, @Named("LIST1") final ListAppender first) {
        this.loggerContext = context;
        logger1 = context.getLogger("org.apache.logging");
        app1 = first.clear();
    }

    @Test
    public void testSetLevel() {
        Logger logger = loggerContext.getLogger(ConfiguratorSetLevelTest.class);
        Configurator.setLevel(logger, Level.DEBUG);
        LoggerConfig loggerConfig = ((AbstractConfiguration)loggerContext.getConfiguration())
                .getLogger(ConfiguratorSetLevelTest.class.getName());
        assertNotNull(loggerConfig);
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        assertEquals(0, loggerConfig.getAppenderRefs().size());
        logger.trace("Test trace message");
        logger.debug("Test debug message");
        assertEquals(1, app1.getEvents().size());
    }
}
