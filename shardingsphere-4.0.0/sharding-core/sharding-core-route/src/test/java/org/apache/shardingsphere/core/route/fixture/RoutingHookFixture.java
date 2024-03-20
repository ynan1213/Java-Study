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

package org.apache.shardingsphere.core.route.fixture;

import org.apache.shardingsphere.core.metadata.table.TableMetas;
import org.apache.shardingsphere.core.route.SQLRouteResult;
import org.apache.shardingsphere.core.route.hook.RoutingHook;

import lombok.Getter;

@Getter
public final class RoutingHookFixture implements RoutingHook {
    
    private String sql;
    
    private SQLRouteResult sqlRouteResult;
    
    private TableMetas tableMetas;
    
    private Exception cause;
    
    @Override
    public void start(final String sql) {
        this.sql = sql;
    }
    
    @Override
    public void finishSuccess(final SQLRouteResult sqlRouteResult, final TableMetas tableMetas) {
        this.sqlRouteResult = sqlRouteResult;
        this.tableMetas = tableMetas;
    }
    
    @Override
    public void finishFailure(final Exception cause) {
        this.cause = cause;
    }
    
}
