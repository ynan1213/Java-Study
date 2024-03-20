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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.apache.shardingsphere.core.metadata.datasource.DataSourceMetas;
import org.apache.shardingsphere.core.route.type.RoutingResult;
import org.apache.shardingsphere.core.rule.MasterSlaveRule;
import org.apache.shardingsphere.core.rule.ShardingDataSourceNames;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class MasterInstanceBroadcastRoutingEngineTest {
    
    private static final String DATASOURCE_NAME = "ds";
    
    @Mock
    private ShardingRule shardingRule;
    
    @Mock
    private ShardingDataSourceNames shardingDataSourceNames;
    
    @Mock
    private MasterSlaveRule masterSlaveRule;
    
    @Mock
    private DataSourceMetas dataSourceMetas;
    
    private MasterInstanceBroadcastRoutingEngine masterInstanceBroadcastRoutingEngine;
    
    @Before
    public void setUp() {
        when(shardingRule.getShardingDataSourceNames()).thenReturn(shardingDataSourceNames);
        when(shardingRule.findMasterSlaveRule(DATASOURCE_NAME)).thenReturn(Optional.of(masterSlaveRule));
        when(masterSlaveRule.getMasterDataSourceName()).thenReturn(DATASOURCE_NAME);
        when(dataSourceMetas.getAllInstanceDataSourceNames()).thenReturn(Lists.newArrayList(DATASOURCE_NAME));
        when(shardingDataSourceNames.getDataSourceNames()).thenReturn(Lists.newArrayList(DATASOURCE_NAME));
        masterInstanceBroadcastRoutingEngine = new MasterInstanceBroadcastRoutingEngine(shardingRule, dataSourceMetas);
    }
    
    @Test
    public void assertRoute() {
        RoutingResult actual = masterInstanceBroadcastRoutingEngine.route();
        assertThat(actual.getRoutingUnits().size(), is(1));
        assertThat(actual.getRoutingUnits().iterator().next().getDataSourceName(), is(DATASOURCE_NAME));
    }
}
