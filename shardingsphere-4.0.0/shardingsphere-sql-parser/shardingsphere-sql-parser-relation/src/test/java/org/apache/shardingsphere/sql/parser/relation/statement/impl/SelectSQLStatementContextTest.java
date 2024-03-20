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

package org.apache.shardingsphere.sql.parser.relation.statement.impl;

import com.google.common.collect.Lists;
import org.apache.shardingsphere.sql.parser.core.constant.OrderDirection;
import org.apache.shardingsphere.sql.parser.relation.segment.select.groupby.GroupByContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.orderby.OrderByContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.orderby.OrderByItem;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.Projection;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.ProjectionsContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.impl.AggregationProjection;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.impl.ColumnProjection;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.SelectItemsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.GroupBySegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.OrderBySegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.ColumnOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.OrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.TableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class SelectSQLStatementContextTest {
    
    private static final String INDEX_ORDER_BY = "IndexOrderBy";
    
    private static final String COLUMN_ORDER_BY_WITH_OWNER = "ColumnOrderByWithOwner";
    
    private static final String COLUMN_ORDER_BY_WITH_ALIAS = "ColumnOrderByWithAlias";
    
    private static final String COLUMN_ORDER_BY_WITHOUT_OWNER_ALIAS = "ColumnOrderByWithoutOwnerAlias";
    
    @Test
    public void assertSetIndexForItemsByIndexOrderBy() {
        SelectSQLStatementContext selectSQLStatementContext = new SelectSQLStatementContext(
                new SelectStatement(), new GroupByContext(Collections.<OrderByItem>emptyList(), 0), createOrderBy(INDEX_ORDER_BY), createProjectionsContext(), null);
        selectSQLStatementContext.setIndexes(Collections.<String, Integer>emptyMap());
        assertThat(selectSQLStatementContext.getOrderByContext().getItems().iterator().next().getIndex(), is(4));
    }
    
    @Test
    public void assertSetIndexForItemsByColumnOrderByWithOwner() {
        SelectSQLStatementContext selectSQLStatementContext = new SelectSQLStatementContext(
                new SelectStatement(), new GroupByContext(Collections.<OrderByItem>emptyList(), 0), createOrderBy(COLUMN_ORDER_BY_WITH_OWNER), createProjectionsContext(), null);
        selectSQLStatementContext.setIndexes(Collections.<String, Integer>emptyMap());
        assertThat(selectSQLStatementContext.getOrderByContext().getItems().iterator().next().getIndex(), is(1));
    }
    
    @Test
    public void assertSetIndexForItemsByColumnOrderByWithAlias() {
        SelectSQLStatementContext selectSQLStatementContext = new SelectSQLStatementContext(
                new SelectStatement(), new GroupByContext(Collections.<OrderByItem>emptyList(), 0), createOrderBy(COLUMN_ORDER_BY_WITH_ALIAS), createProjectionsContext(), null);
        Map<String, Integer> columnLabelIndexMap = new HashMap<>();
        columnLabelIndexMap.put("n", 2);
        selectSQLStatementContext.setIndexes(columnLabelIndexMap);
        assertThat(selectSQLStatementContext.getOrderByContext().getItems().iterator().next().getIndex(), is(2));
    }
    
    @Test
    public void assertSetIndexForItemsByColumnOrderByWithoutAlias() {
        SelectSQLStatementContext selectSQLStatementContext = new SelectSQLStatementContext(
                new SelectStatement(), new GroupByContext(Collections.<OrderByItem>emptyList(), 0), createOrderBy(COLUMN_ORDER_BY_WITHOUT_OWNER_ALIAS), createProjectionsContext(), null);
        Map<String, Integer> columnLabelIndexMap = new HashMap<>();
        columnLabelIndexMap.put("id", 3);
        selectSQLStatementContext.setIndexes(columnLabelIndexMap);
        assertThat(selectSQLStatementContext.getOrderByContext().getItems().iterator().next().getIndex(), is(3));
    }
    
    @Test
    public void assertIsSameGroupByAndOrderByItems() {
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.setSelectItems(new SelectItemsSegment(0, 0, false));
        selectStatement.setGroupBy(new GroupBySegment(0, 0, Collections.<OrderByItemSegment>singletonList(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.DESC))));
        selectStatement.setOrderBy(new OrderBySegment(0, 0, Collections.<OrderByItemSegment>singletonList(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.DESC))));
        SelectSQLStatementContext selectSQLStatementContext = new SelectSQLStatementContext(null, "", Collections.emptyList(), selectStatement);
        assertTrue(selectSQLStatementContext.isSameGroupByAndOrderByItems());
    }
    
    @Test
    public void assertIsNotSameGroupByAndOrderByItemsWhenEmptyGroupBy() {
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.setSelectItems(new SelectItemsSegment(0, 0, false));
        SelectSQLStatementContext selectSQLStatementContext = new SelectSQLStatementContext(null, "", Collections.emptyList(), selectStatement);
        assertFalse(selectSQLStatementContext.isSameGroupByAndOrderByItems());
    }
    
    @Test
    public void assertIsNotSameGroupByAndOrderByItemsWhenDifferentGroupByAndOrderBy() {
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.setSelectItems(new SelectItemsSegment(0, 0, false));
        selectStatement.setGroupBy(new GroupBySegment(0, 0, Collections.<OrderByItemSegment>singletonList(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.ASC, OrderDirection.DESC))));
        selectStatement.setOrderBy(new OrderBySegment(0, 0, Collections.<OrderByItemSegment>singletonList(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.DESC))));
        SelectSQLStatementContext selectSQLStatementContext = new SelectSQLStatementContext(null, "", Collections.emptyList(), selectStatement);
        assertFalse(selectSQLStatementContext.isSameGroupByAndOrderByItems());
    }
    
    @Test
    public void assertSetIndexWhenAggregationProjectionsPresent() {
        ProjectionsContext projectionsContext = mock(ProjectionsContext.class);
        AggregationProjection aggregationProjection = mock(AggregationProjection.class);
        when(projectionsContext.getAggregationProjections()).thenReturn(Collections.singletonList(aggregationProjection));
        when(aggregationProjection.getDerivedAggregationProjections()).thenReturn(Collections.singletonList(aggregationProjection));
        when(aggregationProjection.getColumnLabel()).thenReturn("id");
        SelectSQLStatementContext selectSQLStatementContext = new SelectSQLStatementContext(
                new SelectStatement(), new GroupByContext(Collections.<OrderByItem>emptyList(), 0), createOrderBy(COLUMN_ORDER_BY_WITHOUT_OWNER_ALIAS), projectionsContext, null);
        Map<String, Integer> columnLabelIndexMap = new HashMap<>();
        columnLabelIndexMap.put("id", 3);
        selectSQLStatementContext.setIndexes(columnLabelIndexMap);
        assertThat(selectSQLStatementContext.getOrderByContext().getItems().iterator().next().getIndex(), is(3));
    }
    
    private OrderByContext createOrderBy(final String type) {
        OrderByItemSegment orderByItemSegment = createOrderByItemSegment(type);
        OrderByItem orderByItem = new OrderByItem(orderByItemSegment);
        return new OrderByContext(Lists.newArrayList(orderByItem), true);
    }
    
    private OrderByItemSegment createOrderByItemSegment(final String type) {
        switch (type) {
            case INDEX_ORDER_BY:
                return new IndexOrderByItemSegment(0, 0, 4, OrderDirection.ASC, OrderDirection.ASC);
            case COLUMN_ORDER_BY_WITH_OWNER:
                ColumnSegment columnSegment = new ColumnSegment(0, 0, "name");
                columnSegment.setOwner(new TableSegment(0, 0, "table"));
                return new ColumnOrderByItemSegment(0, 0, columnSegment, OrderDirection.ASC, OrderDirection.ASC);
            case COLUMN_ORDER_BY_WITH_ALIAS:
                return new ColumnOrderByItemSegment(0, 0, new ColumnSegment(0, 0, "n"), OrderDirection.ASC, OrderDirection.ASC);
            default:
                return new ColumnOrderByItemSegment(0, 0, new ColumnSegment(0, 0, "id"), OrderDirection.ASC, OrderDirection.ASC);
        }
    }
    
    private ProjectionsContext createProjectionsContext() {
        return new ProjectionsContext(
                0, 0, true, Arrays.asList(getColumnSelectItemWithoutOwner(), getColumnSelectItemWithoutOwner(true), getColumnSelectItemWithoutOwner(false)), Collections.<String>emptyList());
    }
    
    private Projection getColumnSelectItemWithoutOwner() {
        return new ColumnProjection("table", "name", null);
    }
    
    private Projection getColumnSelectItemWithoutOwner(final boolean hasAlias) {
        return new ColumnProjection(null, hasAlias ? "name" : "id", hasAlias ? "n" : null);
    }
}
