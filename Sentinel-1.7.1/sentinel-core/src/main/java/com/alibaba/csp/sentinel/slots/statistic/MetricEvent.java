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
package com.alibaba.csp.sentinel.slots.statistic;

/**
 * @author Eric Zhao
 */
public enum MetricEvent {

    /**
     * Normal pass.
     * 通过指标
     */
    PASS,
    /**
     * Normal block.
     * 阻塞指标
     */
    BLOCK,
    /**
     * 异常指标
     */
    EXCEPTION,
    /**
     * 成功指标
     */
    SUCCESS,
    /**
     * 平均响应时间指标
     */
    RT,

    /**
     * Passed in future quota (pre-occupied, since 1.5.0).
     * 没有明白
     */
    OCCUPIED_PASS
}
