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

package org.apache.shardingsphere.core.route.type.complex;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.exception.ShardingException;
import org.apache.shardingsphere.sql.parser.relation.statement.SQLStatementContext;
import org.apache.shardingsphere.core.route.router.sharding.condition.ShardingConditions;
import org.apache.shardingsphere.core.route.type.RoutingEngine;
import org.apache.shardingsphere.core.route.type.RoutingResult;
import org.apache.shardingsphere.core.route.type.standard.StandardRoutingEngine;
import org.apache.shardingsphere.core.rule.BindingTableRule;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.core.rule.TableRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Complex routing engine.
 * 
 * @author gaohongtao
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class ComplexRoutingEngine implements RoutingEngine {
    
    private final ShardingRule shardingRule;
    
    private final Collection<String> logicTables;
    
    private final SQLStatementContext sqlStatementContext;
    
    private final ShardingConditions shardingConditions;
    
    @Override
    public RoutingResult route() {
        Collection<RoutingResult> result = new ArrayList<>(logicTables.size());
        Collection<String> bindingTableNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String each : logicTables) {
            Optional<TableRule> tableRule = shardingRule.findTableRule(each);
            if (tableRule.isPresent()) {
                if (!bindingTableNames.contains(each)) {
                    result.add(new StandardRoutingEngine(shardingRule, tableRule.get().getLogicTable(), sqlStatementContext, shardingConditions).route());
                }
                Optional<BindingTableRule> bindingTableRule = shardingRule.findBindingTableRule(each);
                if (bindingTableRule.isPresent()) {
                    bindingTableNames.addAll(Lists.transform(bindingTableRule.get().getTableRules(), new Function<TableRule, String>() {
                        
                        @Override
                        public String apply(final TableRule input) {
                            return input.getLogicTable();
                        }
                    }));
                }
            }
        }
        if (result.isEmpty()) {
            throw new ShardingException("Cannot find table rule and default data source with logic tables: '%s'", logicTables);
        }
        if (1 == result.size()) {
            return result.iterator().next();
        }
        // Cartesian 笛卡尔
        return new CartesianRoutingEngine(result).route();
    }
}
