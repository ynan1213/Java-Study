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
package org.apache.log4j;

import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Used to test Log4j 1 support. All we are looking for here is that this code compiles.
 */
public class CustomAppenderSkeleton extends AppenderSkeleton {

    @Override
    protected void append(final LoggingEvent event) {
        // NOOP @Override
    }

    @Override
    public void close() {
        // NOOP @Override
    }

    @SuppressWarnings({"cast", "unused"})
    public void compilerAccessToWriterAppenderSkeletonVariables() {
        if (closed) {
            // Yep, it compiles.
            final boolean compileMe = closed;
        }
        if (errorHandler instanceof ErrorHandler) {
            // Yep, it compiles.
            final ErrorHandler other = errorHandler;
        }
        if (headFilter instanceof Filter) {
            // Yep, it compiles.
            final Filter other = headFilter;
        }
        if (layout instanceof Layout) {
            // Yep, it compiles.
            final Layout other = layout;
        }
        if (name instanceof String) {
            // Yep, it compiles.
            final String other = name;
        }
        if (tailFilter instanceof Filter) {
            // Yep, it compiles.
            final Filter other = tailFilter;
        }
        if (threshold instanceof Priority) {
            // Yep, it compiles.
            final Priority other = threshold;
        }
    }

    @Override
    public boolean requiresLayout() {
        // NOOP @Override
        return false;
    }
}
