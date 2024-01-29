/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.druid.bvt.proxy.filter.encoding;

import org.junit.Assert;
import junit.framework.TestCase;

import com.alibaba.druid.filter.encoding.CharsetParameter;

/**
 * @author gang.su
 */
public class CharsetParameterTest extends TestCase {
    public void testQ() {
        CharsetParameter c = new CharsetParameter();
        c.setClientEncoding("1");
        c.setServerEncoding("2");
        Assert.assertEquals("1", c.getClientEncoding());
        Assert.assertEquals("2", c.getServerEncoding());

    }
}
