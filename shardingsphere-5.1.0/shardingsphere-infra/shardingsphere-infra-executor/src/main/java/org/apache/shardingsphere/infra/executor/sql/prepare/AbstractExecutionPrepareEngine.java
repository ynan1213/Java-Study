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

package org.apache.shardingsphere.infra.executor.sql.prepare;

import com.google.common.collect.Lists;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroup;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroupContext;
import org.apache.shardingsphere.infra.executor.sql.context.ExecutionUnit;
import org.apache.shardingsphere.infra.executor.sql.context.SQLUnit;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.ConnectionMode;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;
import org.apache.shardingsphere.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.spi.ordered.OrderedSPIRegistry;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Abstract execution prepare engine.
 * 
 * @param <T> type of input value
 */
public abstract class AbstractExecutionPrepareEngine<T> implements ExecutionPrepareEngine<T> {
    
    static {
        ShardingSphereServiceLoader.register(ExecutionPrepareDecorator.class);
    }
    
    private final int maxConnectionsSizePerQuery;
    
    @SuppressWarnings("rawtypes")
    private final Map<ShardingSphereRule, ExecutionPrepareDecorator> decorators;
    
    protected AbstractExecutionPrepareEngine(final int maxConnectionsSizePerQuery, final Collection<ShardingSphereRule> rules) {
        this.maxConnectionsSizePerQuery = maxConnectionsSizePerQuery;
        decorators = OrderedSPIRegistry.getRegisteredServices(ExecutionPrepareDecorator.class, rules);
    }
    
    @Override
    public final ExecutionGroupContext<T> prepare(final RouteContext routeContext, final Collection<ExecutionUnit> executionUnits) throws SQLException {
        Collection<ExecutionGroup<T>> result = new LinkedList<>();
        for (Entry<String, List<SQLUnit>> entry : aggregateSQLUnitGroups(executionUnits).entrySet()) {
            String dataSourceName = entry.getKey();
            List<SQLUnit> sqlUnits = entry.getValue();
            List<List<SQLUnit>> sqlUnitGroups = group(sqlUnits);
            /*
             * 用户通过配置 maxConnectionSizePerQuery 参数，可以指定每条语句在同一个数据源上最大允许的连接数。
             * 通过计算公式，当每个数据库连接需执行的 SQL 数量小于等于 1 时，说明当前可以满足每条真实执行的 SQL 都分配一个独立的数据库连接，
             * 此时会选择内存限制模式，同一个数据源允许创建多个数据库连接进行并行执行。反之则会选择连接限制模式，同一个数据源只允许创建一个数
             * 据库连接进行执行，然后将结果集加载进内存结果集，再提供给归并引擎使用。
             */
            ConnectionMode connectionMode = maxConnectionsSizePerQuery < sqlUnits.size() ? ConnectionMode.CONNECTION_STRICTLY : ConnectionMode.MEMORY_STRICTLY;
            result.addAll(group(dataSourceName, sqlUnitGroups, connectionMode));
        }
        return decorate(routeContext, result);
    }
    
    private List<List<SQLUnit>> group(final List<SQLUnit> sqlUnits) {
        int desiredPartitionSize = Math.max(0 == sqlUnits.size() % maxConnectionsSizePerQuery ? sqlUnits.size() / maxConnectionsSizePerQuery : sqlUnits.size() / maxConnectionsSizePerQuery + 1, 1);
        return Lists.partition(sqlUnits, desiredPartitionSize);
    }
    
    protected abstract List<ExecutionGroup<T>> group(String dataSourceName, List<List<SQLUnit>> sqlUnitGroups, ConnectionMode connectionMode) throws SQLException;
    
    private Map<String, List<SQLUnit>> aggregateSQLUnitGroups(final Collection<ExecutionUnit> executionUnits) {
        Map<String, List<SQLUnit>> result = new LinkedHashMap<>(executionUnits.size(), 1);
        for (ExecutionUnit each : executionUnits) {
            if (!result.containsKey(each.getDataSourceName())) {
                result.put(each.getDataSourceName(), new LinkedList<>());
            }
            result.get(each.getDataSourceName()).add(each.getSqlUnit());
        }
        return result;
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ExecutionGroupContext<T> decorate(final RouteContext routeContext, final Collection<ExecutionGroup<T>> executionGroups) {
        Collection<ExecutionGroup<T>> result = executionGroups;
        for (Entry<ShardingSphereRule, ExecutionPrepareDecorator> each : decorators.entrySet()) {
            result = each.getValue().decorate(routeContext, each.getKey(), result);
        }
        return new ExecutionGroupContext(result);
    }
}
