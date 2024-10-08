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

package org.apache.shardingsphere.traffic.algorithm.context;

import org.apache.shardingsphere.infra.executor.sql.context.ExecutionUnit;
import org.apache.shardingsphere.infra.executor.sql.context.SQLUnit;
import org.apache.shardingsphere.traffic.context.TrafficContext;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class TrafficContextTest {
    
    @Test
    public void assertIsMatchTrafficWhenExistExecutionUnit() {
        TrafficContext trafficContext = new TrafficContext();
        trafficContext.getExecutionUnits().add(new ExecutionUnit("127.0.0.1@3307", new SQLUnit("SELECT * FROM t_order", Collections.emptyList())));
        assertTrue(trafficContext.isMatchTraffic());
    }
    
    @Test
    public void assertIsMatchTrafficWhenNotExistExecutionUnit() {
        TrafficContext trafficContext = new TrafficContext();
        assertFalse(trafficContext.isMatchTraffic());
    }
}
