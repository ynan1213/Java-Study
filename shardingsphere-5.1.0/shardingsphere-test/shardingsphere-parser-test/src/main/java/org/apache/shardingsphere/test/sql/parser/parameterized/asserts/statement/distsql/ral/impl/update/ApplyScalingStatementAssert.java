/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.test.sql.parser.parameterized.asserts.statement.distsql.ral.impl.update;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.scaling.distsql.statement.ApplyScalingStatement;
import org.apache.shardingsphere.test.sql.parser.parameterized.asserts.SQLCaseAssertContext;
import org.apache.shardingsphere.test.sql.parser.parameterized.jaxb.cases.domain.statement.distsql.ral.scaling.ApplyScalingStatementTestCase;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Apply scaling statement assert.
 */
public final class ApplyScalingStatementAssert {
    
    /**
     * Assert apply scaling statement is correct with expected parser result.
     *
     * @param assertContext assert context
     * @param actual actual apply scaling statement
     * @param expected expected apply scaling statement test case
     */
    public static void assertIs(final SQLCaseAssertContext assertContext, final ApplyScalingStatement actual, final ApplyScalingStatementTestCase expected) {
        if (null == expected) {
            assertNull(assertContext.getText("Actual statement should not exist."), actual);
        } else {
            assertNotNull(assertContext.getText("Actual statement should exist."), actual);
            assertJobIds(assertContext, actual.getJobId(), expected.getJobIds());
        }
    }
    
    private static void assertJobIds(final SQLCaseAssertContext assertContext, final String actual, final List<String> expected) {
        if (CollectionUtils.isEmpty(expected)) {
            assertNull(assertContext.getText("Actual job id should not exist."), actual);
        } else {
            assertNotNull(assertContext.getText("Actual job id should exist."), actual);
            assertThat(assertContext.getText("Job id assertion error"), actual, is(expected.iterator().next()));
        }
    }
}
