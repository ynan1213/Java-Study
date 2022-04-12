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
package com.alibaba.csp.sentinel.slots.statistic.metric;

import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.node.metric.MetricNode;
import com.alibaba.csp.sentinel.slots.statistic.MetricEvent;
import com.alibaba.csp.sentinel.slots.statistic.base.LeapArray;
import com.alibaba.csp.sentinel.slots.statistic.base.WindowWrap;
import com.alibaba.csp.sentinel.slots.statistic.data.MetricBucket;
import com.alibaba.csp.sentinel.slots.statistic.metric.occupy.OccupiableBucketLeapArray;
import com.alibaba.csp.sentinel.util.function.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * The basic metric class in Sentinel using a {@link BucketLeapArray} internal.
 *
 * @author jialiang.linjl
 * @author Eric Zhao
 */
public class ArrayMetric implements Metric {

    // 用来存储各个窗口的数据，MetricBucket：指标桶
    private final LeapArray<MetricBucket> data;

    // sampleCount默认为2，intervalInMs默认为1000（单位ms），表示1s内有两个时间窗口
    public ArrayMetric(int sampleCount, int intervalInMs) {
        this.data = new OccupiableBucketLeapArray(sampleCount, intervalInMs);
    }

    /**
     * For unit test.
     */
    public ArrayMetric(LeapArray<MetricBucket> array) {
        this.data = array;
    }

    /**
     * @param sampleCount 在一个采集间隔中抽样的个数，默认为 2。
     * @param intervalInMs 表示一个采集的时间间隔，例如1秒，1分钟。
     * @param enableOccupy 是否允许抢占，即当前时间戳已经达到限制后，是否可以占用下一个时间窗口的容量，
     */
    public ArrayMetric(int sampleCount, int intervalInMs, boolean enableOccupy) {
        if (enableOccupy) {
            // 秒级使用这个
            this.data = new OccupiableBucketLeapArray(sampleCount, intervalInMs);
        } else {
            // 分钟级使用这个
            this.data = new BucketLeapArray(sampleCount, intervalInMs);
        }
    }

    @Override
    public long success() {
        data.currentWindow();
        long success = 0;

        List<MetricBucket> list = data.values();
        for (MetricBucket window : list) {
            success += window.success();
        }
        return success;
    }

    @Override
    public long maxSuccess() {
        data.currentWindow();
        long success = 0;

        List<MetricBucket> list = data.values();
        for (MetricBucket window : list) {
            if (window.success() > success) {
                success = window.success();
            }
        }
        return Math.max(success, 1);
    }

    @Override
    public long exception() {
        data.currentWindow();
        long exception = 0;
        List<MetricBucket> list = data.values();
        for (MetricBucket window : list) {
            exception += window.exception();
        }
        return exception;
    }

    @Override
    public long block() {
        data.currentWindow();
        long block = 0;
        List<MetricBucket> list = data.values();
        for (MetricBucket window : list) {
            block += window.block();
        }
        return block;
    }

    @Override
    public long pass() {
        // 为什么要调用一下这个方法？ 用于初始化吗？
        data.currentWindow();
        long pass = 0;
        // 返回当前所有的时间窗格内对象，内部会过滤掉过期的
        List<MetricBucket> list = data.values();

        for (MetricBucket window : list) {
            pass += window.pass();
        }
        return pass;
    }

    @Override
    public long occupiedPass() {
        data.currentWindow();
        long pass = 0;
        List<MetricBucket> list = data.values();
        for (MetricBucket window : list) {
            pass += window.occupiedPass();
        }
        return pass;
    }

    @Override
    public long rt() {
        data.currentWindow();
        long rt = 0;
        List<MetricBucket> list = data.values();
        for (MetricBucket window : list) {
            rt += window.rt();
        }
        return rt;
    }

    @Override
    public long minRt() {
        data.currentWindow();
        long rt = SentinelConfig.statisticMaxRt();
        List<MetricBucket> list = data.values();
        for (MetricBucket window : list) {
            if (window.minRt() < rt) {
                rt = window.minRt();
            }
        }

        return Math.max(1, rt);
    }

    @Override
    public List<MetricNode> details() {
        List<MetricNode> details = new ArrayList<>();
        data.currentWindow();
        List<WindowWrap<MetricBucket>> list = data.list();
        for (WindowWrap<MetricBucket> window : list) {
            if (window == null) {
                continue;
            }
            details.add(fromBucket(window));
        }
        return details;
    }

    @Override
    public List<MetricNode> detailsOnCondition(Predicate<Long> timePredicate) {
        List<MetricNode> details = new ArrayList<>();
        data.currentWindow();
        List<WindowWrap<MetricBucket>> list = data.list();
        for (WindowWrap<MetricBucket> window : list) {
            if (window == null) {
                continue;
            }
            if (timePredicate != null && !timePredicate.test(window.windowStart())) {
                continue;
            }

            details.add(fromBucket(window));
        }

        return details;
    }

    private MetricNode fromBucket(WindowWrap<MetricBucket> wrap) {
        MetricNode node = new MetricNode();
        node.setBlockQps(wrap.value().block());
        node.setExceptionQps(wrap.value().exception());
        node.setPassQps(wrap.value().pass());
        long successQps = wrap.value().success();
        node.setSuccessQps(successQps);
        if (successQps != 0) {
            node.setRt(wrap.value().rt() / successQps);
        } else {
            node.setRt(wrap.value().rt());
        }
        node.setTimestamp(wrap.windowStart());
        // 没有明白这个的意思
        node.setOccupiedPassQps(wrap.value().occupiedPass());
        return node;
    }

    @Override
    public MetricBucket[] windows() {
        data.currentWindow();
        return data.values().toArray(new MetricBucket[0]);
    }

    @Override
    public void addException(int count) {
        WindowWrap<MetricBucket> wrap = data.currentWindow();
        wrap.value().addException(count);
    }

    @Override
    public void addBlock(int count) {
        WindowWrap<MetricBucket> wrap = data.currentWindow();
        wrap.value().addBlock(count);
    }

    @Override
    public void addWaiting(long time, int acquireCount) {
        data.addWaiting(time, acquireCount);
    }

    @Override
    public void addOccupiedPass(int acquireCount) {
        WindowWrap<MetricBucket> wrap = data.currentWindow();
        wrap.value().addOccupiedPass(acquireCount);
    }

    @Override
    public void addSuccess(int count) {
        WindowWrap<MetricBucket> wrap = data.currentWindow();
        wrap.value().addSuccess(count);
    }

    @Override
    public void addPass(int count) {
        // 获取当前时间对应的时间窗格内存储的对象
        WindowWrap<MetricBucket> wrap = data.currentWindow();
        // MetricBucket该对象内部会存储多个指标，这里是对 PASS 指标 +count 个
        wrap.value().addPass(count);
    }

    @Override
    public void addRT(long rt) {
        WindowWrap<MetricBucket> wrap = data.currentWindow();
        wrap.value().addRT(rt);
    }

    @Override
    public void debug() {
        data.debug(System.currentTimeMillis());
    }

    @Override
    public long previousWindowBlock() {
        data.currentWindow();
        WindowWrap<MetricBucket> wrap = data.getPreviousWindow();
        if (wrap == null) {
            return 0;
        }
        return wrap.value().block();
    }

    @Override
    public long previousWindowPass() {
        data.currentWindow();
        WindowWrap<MetricBucket> wrap = data.getPreviousWindow();
        if (wrap == null) {
            return 0;
        }
        return wrap.value().pass();
    }

    public void add(MetricEvent event, long count) {
        data.currentWindow().value().add(event, count);
    }

    public long getCurrentCount(MetricEvent event) {
        return data.currentWindow().value().get(event);
    }

    /**
     * Get total sum for provided event in {@code intervalInSec}.
     *
     * @param event event to calculate
     * @return total sum for event
     */
    public long getSum(MetricEvent event) {
        data.currentWindow();
        long sum = 0;

        List<MetricBucket> buckets = data.values();
        for (MetricBucket bucket : buckets) {
            sum += bucket.get(event);
        }
        return sum;
    }

    /**
     * Get average count for provided event per second.
     *
     * @param event event to calculate
     * @return average count per second for event
     */
    public double getAvg(MetricEvent event) {
        return getSum(event) / data.getIntervalInSecond();
    }

    @Override
    public long getWindowPass(long timeMillis) {
        MetricBucket bucket = data.getWindowValue(timeMillis);
        if (bucket == null) {
            return 0L;
        }
        return bucket.pass();
    }

    @Override
    public long waiting() {
        return data.currentWaiting();
    }

    @Override
    public double getWindowIntervalInSec() {
        return data.getIntervalInSecond();
    }

    @Override
    public int getSampleCount() {
        return data.getSampleCount();
    }
}
