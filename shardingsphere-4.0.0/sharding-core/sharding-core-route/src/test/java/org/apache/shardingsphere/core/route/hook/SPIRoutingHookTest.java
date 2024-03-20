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

package org.apache.shardingsphere.core.route.hook;

import lombok.SneakyThrows;
import org.apache.shardingsphere.core.metadata.table.TableMetas;
import org.apache.shardingsphere.core.route.SQLRouteResult;
import org.apache.shardingsphere.core.route.fixture.RoutingHookFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Collection;
import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public final class SPIRoutingHookTest {
    
    private final SPIRoutingHook spiRoutingHook = new SPIRoutingHook();
    
    @Mock
    private SQLRouteResult sqlRouteResult;
    
    @Mock
    private TableMetas tableMetas;
    
    @Mock
    private Exception exception;
    
    @Test
    public void assertStart() {
        String sql = "SELECT * FROM table";
        spiRoutingHook.start(sql);
        RoutingHook routingHook = getFixtureHook();
        assertThat(routingHook, instanceOf(RoutingHookFixture.class));
        assertThat(((RoutingHookFixture) routingHook).getSql(), is(sql));
    }
    
    @Test
    public void assertFinishSuccess() {
        spiRoutingHook.finishSuccess(sqlRouteResult, tableMetas);
        RoutingHook routingHook = getFixtureHook();
        assertThat(routingHook, instanceOf(RoutingHookFixture.class));
        assertThat(((RoutingHookFixture) routingHook).getSqlRouteResult(), is(sqlRouteResult));
        assertThat(((RoutingHookFixture) routingHook).getTableMetas(), is(tableMetas));
    }
    
    @Test
    public void assertFinishFailure() {
        spiRoutingHook.finishFailure(exception);
        RoutingHook routingHook = getFixtureHook();
        assertThat(routingHook, instanceOf(RoutingHookFixture.class));
        assertThat(((RoutingHookFixture) routingHook).getCause(), is(exception));
    }
    
    @SuppressWarnings("unchecked")
    @SneakyThrows
    private RoutingHook getFixtureHook() {
        Field routingHooksField = SPIRoutingHook.class.getDeclaredField("routingHooks");
        routingHooksField.setAccessible(true);
        return ((Collection<RoutingHook>) routingHooksField.get(spiRoutingHook)).iterator().next();
    }
}
