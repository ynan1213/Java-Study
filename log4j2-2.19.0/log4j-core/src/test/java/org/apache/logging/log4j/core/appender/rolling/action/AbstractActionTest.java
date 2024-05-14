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
package org.apache.logging.log4j.core.appender.rolling.action;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.junit.StatusLoggerLevel;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@StatusLoggerLevel("WARN")
public class AbstractActionTest {

    // Test for LOG4J2-2658
    @Test
    public void testExceptionsAreLoggedToStatusLogger() {
        StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.clear();
        new TestAction().run();
        List<StatusData> statusDataList = statusLogger.getStatusData();
        assertThat(statusDataList, hasSize(1));
        StatusData statusData = statusDataList.get(0);
        assertEquals(Level.WARN, statusData.getLevel());
        String formattedMessage = statusData.getFormattedStatus();
        assertThat(formattedMessage, containsString("Exception reported by action 'class org.apache."
                + "logging.log4j.core.appender.rolling.action.AbstractActionTest$TestAction' java.io.IOException: "
                + "failed" + System.lineSeparator()
                + "\tat org.apache.logging.log4j.core.appender.rolling.action.AbstractActionTest"
                + "$TestAction.execute(AbstractActionTest.java:"));
    }

    @Test
    public void testRuntimeExceptionsAreLoggedToStatusLogger() {
        StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.clear();
        new AbstractAction() {
            @Override
            public boolean execute() {
                throw new IllegalStateException();
            }
        }.run();
        List<StatusData> statusDataList = statusLogger.getStatusData();
        assertThat(statusDataList, hasSize(1));
        StatusData statusData = statusDataList.get(0);
        assertEquals(Level.WARN, statusData.getLevel());
        String formattedMessage = statusData.getFormattedStatus();
        assertThat(formattedMessage, containsString("Exception reported by action"));
    }

    @Test
    public void testErrorsAreLoggedToStatusLogger() {
        StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.clear();
        new AbstractAction() {
            @Override
            public boolean execute() {
                throw new AssertionError();
            }
        }.run();
        List<StatusData> statusDataList = statusLogger.getStatusData();
        assertThat(statusDataList, hasSize(1));
        StatusData statusData = statusDataList.get(0);
        assertEquals(Level.WARN, statusData.getLevel());
        String formattedMessage = statusData.getFormattedStatus();
        assertThat(formattedMessage, containsString("Exception reported by action"));
    }

    private static final class TestAction extends AbstractAction {
        @Override
        public boolean execute() throws IOException {
            throw new IOException("failed");
        }
    }
}
