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

package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static org.apache.logging.log4j.junit.LoggerContextResolver.getParameterLoggerContext;

class AppenderResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return Appender.class.isAssignableFrom(parameterContext.getParameter().getType()) && parameterContext
                .isAnnotated(Named.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        final LoggerContext loggerContext = getParameterLoggerContext(parameterContext, extensionContext);
        if (loggerContext == null) {
            throw new ParameterResolutionException("No LoggerContext defined");
        }
        final String name = parameterContext.findAnnotation(Named.class)
                .map(Named::value)
                .map(s -> s.isEmpty() ? parameterContext.getParameter().getName() : s)
                .orElseThrow(() -> new ParameterResolutionException("No @Named present after checking earlier"));
        final Appender appender = loggerContext.getConfiguration().getAppender(name);
        if (appender == null) {
            throw new ParameterResolutionException("No appender named " + name);
        }
        return appender;
    }
}
