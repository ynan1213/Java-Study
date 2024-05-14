/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.layout.template.json.util;

import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class QueueingRecycler<V> implements Recycler<V> {

    private final Supplier<V> supplier;

    private final Consumer<V> cleaner;

    private final Queue<V> queue;

    public QueueingRecycler(
            final Supplier<V> supplier,
            final Consumer<V> cleaner,
            final Queue<V> queue) {
        this.supplier = supplier;
        this.cleaner = cleaner;
        this.queue = queue;
    }

    // Visible for tests.
    Queue<V> getQueue() {
        return queue;
    }

    @Override
    public V acquire() {
        final V value = queue.poll();
        if (value == null) {
            return supplier.get();
        } else {
            cleaner.accept(value);
            return value;
        }
    }

    @Override
    public void release(final V value) {
        queue.offer(value);
    }

}
