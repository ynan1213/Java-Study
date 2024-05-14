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

package org.apache.logging.log4j.jpl;

import java.lang.System.Logger;
import java.lang.System.LoggerFinder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * {@link Logger} registry implementation using just log4j-api.
 *
 * @since 2.14
 */
public class Log4jSystemLoggerAdapter extends AbstractLoggerAdapter<Logger> {

    @Override
    protected Logger newLogger(String name, LoggerContext context) {
        return new Log4jSystemLogger(context.getLogger(name));
    }

    @Override
    protected LoggerContext getContext() {
        return getContext(LogManager.getFactory().isClassLoaderDependent()
                ? StackLocatorUtil.getCallerClass(LoggerFinder.class)
                : null);
    }
}
