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

package org.apache.shardingsphere.core.route.type.broadcast;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.route.type.RoutingEngine;
import org.apache.shardingsphere.core.route.type.RoutingResult;
import org.apache.shardingsphere.core.route.type.RoutingUnit;
import org.apache.shardingsphere.core.rule.ShardingRule;

/**
 * Broadcast routing engine for databases.
 * 
 * @author zhangliang
 * @author maxiaoguang
 */
@RequiredArgsConstructor
public final class DatabaseBroadcastRoutingEngine implements RoutingEngine {
    
    private final ShardingRule shardingRule;

    /**
     * 非常直接，即基于每个 DataSourceName 构建一个 RoutingUnit
     */
    @Override
    public RoutingResult route() {
        RoutingResult result = new RoutingResult();
        for (String each : shardingRule.getShardingDataSourceNames().getDataSourceNames()) {
            result.getRoutingUnits().add(new RoutingUnit(each));
        }
        return result;
    }
}
