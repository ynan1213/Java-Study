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

package org.apache.shardingsphere.sharding.rewrite.context;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.route.SQLRouteResult;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.sharding.rewrite.parameter.ShardingParameterRewriterBuilder;
import org.apache.shardingsphere.sharding.rewrite.token.pojo.impl.ShardingTokenGenerateBuilder;
import org.apache.shardingsphere.underlying.rewrite.context.SQLRewriteContext;
import org.apache.shardingsphere.underlying.rewrite.context.SQLRewriteContextDecorator;
import org.apache.shardingsphere.underlying.rewrite.parameter.rewriter.ParameterRewriter;

import java.util.Collection;

/**
 * SQL rewrite context decorator for sharding.
 * 
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class ShardingSQLRewriteContextDecorator implements SQLRewriteContextDecorator {
    
    private final ShardingRule shardingRule;
    
    private final SQLRouteResult sqlRouteResult;
    
    @Override
    public void decorate(final SQLRewriteContext sqlRewriteContext) {
        ShardingParameterRewriterBuilder shardingParameterRewriterBuilder = new ShardingParameterRewriterBuilder(shardingRule, sqlRouteResult);
        Collection<ParameterRewriter> parameterRewriters = shardingParameterRewriterBuilder.getParameterRewriters(sqlRewriteContext.getRelationMetas());
        /**
         * 改写预处理参数，对于分片的场景有两个实现类，一个是分页、一个是需要补列
         * 分页的例子：select * from t_order limit ?,? [1,2]  -> select * from t_order ?,? [0,3]
         * 详情见 https://shardingsphere.apache.org/document/3.0.0.M4/cn/reference/sharding/rewrite/
         */
        for (ParameterRewriter each : parameterRewriters) {
            if (!sqlRewriteContext.getParameters().isEmpty() && each.isNeedRewrite(sqlRewriteContext.getSqlStatementContext())) {
                each.rewrite(sqlRewriteContext.getParameterBuilder(), sqlRewriteContext.getSqlStatementContext(), sqlRewriteContext.getParameters());
            }
        }
        sqlRewriteContext.addSQLTokenGenerators(new ShardingTokenGenerateBuilder(shardingRule, sqlRouteResult).getSQLTokenGenerators());
    }
}
