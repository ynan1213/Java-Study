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

package org.apache.shardingsphere.core.route.router.sharding.validator.impl;

import org.apache.shardingsphere.core.exception.ShardingException;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.assignment.AssignmentSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.assignment.SetAssignmentsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.PredicateSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateCompareRightValue;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.TableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.UpdateStatement;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class ShardingUpdateStatementValidatorTest {
    
    @Mock
    private ShardingRule shardingRule;
    
    @Test
    public void assertValidateUpdateWithoutShardingKey() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(false);
        new ShardingUpdateStatementValidator().validate(shardingRule, createUpdateStatement(), Collections.emptyList());
    }
    
    @Test(expected = ShardingException.class)
    public void assertValidateUpdateWithShardingKey() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(true);
        new ShardingUpdateStatementValidator().validate(shardingRule, createUpdateStatement(), Collections.emptyList());
    }

    @Test
    public void assertValidateUpdateWithoutShardingKeyAndParameters() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(false);
        List<Object> parameters = Arrays.<Object>asList(1, 1);
        new ShardingUpdateStatementValidator().validate(shardingRule, createUpdateStatement(), parameters);
    }

    @Test
    public void assertValidateUpdateWithShardingKeyAndShardingParameterEquals() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(true);
        List<Object> parameters = Arrays.<Object>asList(1, 1);
        new ShardingUpdateStatementValidator().validate(shardingRule, createUpdateStatementAndParameters(1), parameters);
    }

    @Test(expected = ShardingException.class)
    public void assertValidateUpdateWithShardingKeyAndShardingParameterNotEquals() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(true);
        List<Object> parameters = Arrays.<Object>asList(1, 1);
        new ShardingUpdateStatementValidator().validate(shardingRule, createUpdateStatementAndParameters(2), parameters);
    }

    private UpdateStatement createUpdateStatement() {
        UpdateStatement result = new UpdateStatement();
        result.getAllSQLSegments().add(new TableSegment(0, 0, "user"));
        result.setSetAssignment(new SetAssignmentsSegment(0, 0, Collections.singletonList(new AssignmentSegment(0, 0, new ColumnSegment(0, 0, "id"), new LiteralExpressionSegment(0, 0, "")))));
        return result;
    }

    private UpdateStatement createUpdateStatementAndParameters(final Object shardingColumnParameter) {
        UpdateStatement result = new UpdateStatement();
        result.getAllSQLSegments().add(new TableSegment(0, 0, "user"));
        Collection<AssignmentSegment> assignments = Collections.singletonList(new AssignmentSegment(0, 0, new ColumnSegment(0, 0, "id"), new LiteralExpressionSegment(0, 0, shardingColumnParameter)));
        SetAssignmentsSegment setAssignmentsSegment = new SetAssignmentsSegment(0, 0, assignments);
        result.setSetAssignment(setAssignmentsSegment);
        WhereSegment where = new WhereSegment(0, 0, 1);
        where.setParameterStartIndex(0);
        AndPredicate andPre = new AndPredicate();
        andPre.getPredicates().add(new PredicateSegment(0, 1, new ColumnSegment(0, 0, "id"), new PredicateCompareRightValue("=", new ParameterMarkerExpressionSegment(0, 0, 0))));
        where.getAndPredicates().add(andPre);
        result.setWhere(where);
        return result;
    }
}
