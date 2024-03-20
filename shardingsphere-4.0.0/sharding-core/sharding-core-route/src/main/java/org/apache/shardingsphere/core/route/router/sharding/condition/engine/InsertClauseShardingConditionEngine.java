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

package org.apache.shardingsphere.core.route.router.sharding.condition.engine;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.sql.parser.relation.segment.insert.InsertValueContext;
import org.apache.shardingsphere.sql.parser.relation.statement.impl.InsertSQLStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.SimpleExpressionSegment;
import org.apache.shardingsphere.core.route.router.sharding.condition.ExpressionConditionUtils;
import org.apache.shardingsphere.core.route.router.sharding.condition.ShardingCondition;
import org.apache.shardingsphere.core.route.router.sharding.keygen.GeneratedKey;
import org.apache.shardingsphere.core.route.spi.SPITimeService;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.core.strategy.route.value.ListRouteValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Sharding condition engine for insert clause.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class InsertClauseShardingConditionEngine {
    
    private final ShardingRule shardingRule;
    
    /**
     * Create sharding conditions.
     * 
     * @param insertSQLStatementContext insert SQL statement context
     * @param generatedKey generated key
     * @param parameters SQL parameters
     * @return sharding conditions
     */
    public List<ShardingCondition> createShardingConditions(final InsertSQLStatementContext insertSQLStatementContext, final GeneratedKey generatedKey, final List<Object> parameters) {
        List<ShardingCondition> result = new LinkedList<>();
        String tableName = insertSQLStatementContext.getTablesContext().getSingleTableName();
        Collection<String> columnNames = getColumnNames(insertSQLStatementContext, generatedKey);
        for (InsertValueContext each : insertSQLStatementContext.getInsertValueContexts()) {
            result.add(createShardingCondition(tableName, columnNames.iterator(), each, parameters));
        }
        if (null != generatedKey && generatedKey.isGenerated() && shardingRule.isShardingColumn(generatedKey.getColumnName(), tableName)) {
            appendGeneratedKeyCondition(generatedKey, tableName, result);
        }
        return result;
    }
    
    private Collection<String> getColumnNames(final InsertSQLStatementContext insertSQLStatementContext, final GeneratedKey generatedKey) {
        if (null == generatedKey || !generatedKey.isGenerated()) {
            return insertSQLStatementContext.getColumnNames();
        }
        Collection<String> result = new LinkedList<>(insertSQLStatementContext.getColumnNames());
        result.remove(generatedKey.getColumnName());
        return result;
    }
    
    private ShardingCondition createShardingCondition(final String tableName, final Iterator<String> columnNames, final InsertValueContext insertValueContext, final List<Object> parameters) {
        ShardingCondition result = new ShardingCondition();
        SPITimeService timeService = new SPITimeService();
        for (ExpressionSegment each : insertValueContext.getValueExpressions()) {
            String columnName = columnNames.next();
            if (shardingRule.isShardingColumn(columnName, tableName)) {
                if (each instanceof SimpleExpressionSegment) {
                    result.getRouteValues().add(new ListRouteValue<>(columnName, tableName, Collections.singletonList(getRouteValue((SimpleExpressionSegment) each, parameters))));
                } else if (ExpressionConditionUtils.isNowExpression(each)) {
                    result.getRouteValues().add(new ListRouteValue<>(columnName, tableName, Collections.singletonList(timeService.getTime())));
                }
            }
        }
        return result;
    }
    
    private Comparable<?> getRouteValue(final SimpleExpressionSegment expressionSegment, final List<Object> parameters) {
        Object result;
        if (expressionSegment instanceof ParameterMarkerExpressionSegment) {
            result = parameters.get(((ParameterMarkerExpressionSegment) expressionSegment).getParameterMarkerIndex());
        } else {
            result = ((LiteralExpressionSegment) expressionSegment).getLiterals();
        }
        Preconditions.checkArgument(result instanceof Comparable, "Sharding value must implements Comparable.");
        return (Comparable) result;
    }
    
    private void appendGeneratedKeyCondition(final GeneratedKey generatedKey, final String tableName, final List<ShardingCondition> shardingConditions) {
        Iterator<Comparable<?>> generatedValues = generatedKey.getGeneratedValues().iterator();
        for (ShardingCondition each : shardingConditions) {
            each.getRouteValues().add(new ListRouteValue<>(generatedKey.getColumnName(), tableName, Collections.<Comparable<?>>singletonList(generatedValues.next())));
        }
    }
}
