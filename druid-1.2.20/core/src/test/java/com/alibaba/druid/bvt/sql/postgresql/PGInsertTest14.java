/*
 * Copyright 1999-2017 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.bvt.sql.postgresql;

import com.alibaba.druid.sql.PGTest;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.postgresql.parser.PGSQLStatementParser;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGSchemaStatVisitor;

import java.util.List;

public class PGInsertTest14 extends PGTest {
    public void test_0() throws Exception {
        String sql = "insert into wf_task_geom\n" +
                "    (task_id,geom_in_gds,polygon,lseg)\n" +
                "     values                     \n" +
                "    (?,ST_GeomFromText(?,4326),?,?)";

        PGSQLStatementParser parser = new PGSQLStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        SQLStatement stmt = statementList.get(0);
//        print(statementList);

        assertEquals(1, statementList.size());

        PGSchemaStatVisitor visitor = new PGSchemaStatVisitor();
        stmt.accept(visitor);

        System.out.println("Tables : " + visitor.getTables());
        System.out.println("fields : " + visitor.getColumns());
//        System.out.println("coditions : " + visitor.getConditions());

        assertTrue(visitor.containsTable("wf_task_geom"));
        assertEquals(4, visitor.getColumns().size());
//
//        assertTrue(visitor.getColumns().contains(new TableStat.Column("distributors", "did")));
//        assertTrue(visitor.getColumns().contains(new TableStat.Column("distributors", "dname")));

        assertEquals("INSERT INTO wf_task_geom (task_id, geom_in_gds, polygon, lseg)\n" +
                "VALUES (?, ST_GeomFromText(?, 4326), ?, ?)", stmt.toString());
    }

}
