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
package org.apache.logging.slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.NOPLoggingEventBuilder;

/**
 * SLF4J logger implementation that uses Log4j.
 */
public class Log4jLogger implements LocationAwareLogger, Serializable {

    public static final String FQCN = Log4jLogger.class.getName();

    private static final long serialVersionUID = 7869000638091304316L;
    private transient ExtendedLogger logger;
    private final String name;
    private transient Log4jMarkerFactory markerFactory;

    public Log4jLogger(final Log4jMarkerFactory markerFactory, final ExtendedLogger logger, final String name) {
        this.markerFactory = markerFactory;
        this.logger = logger;
        this.name = name;
    }

    @Override
    public void trace(final String format) {
        logger.logIfEnabled(FQCN, Level.TRACE, null, format);
    }

    @Override
    public void trace(final String format, final Object o) {
        logger.logIfEnabled(FQCN, Level.TRACE, null, format, o);
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
        logger.logIfEnabled(FQCN, Level.TRACE, null, format, arg1, arg2);
    }

    @Override
    public void trace(final String format, final Object... args) {
        logger.logIfEnabled(FQCN, Level.TRACE, null, format, args);
    }

    @Override
    public void trace(final String format, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.TRACE, null, format, t);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isEnabled(Level.TRACE, null, null);
    }

    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return logger.isEnabled(Level.TRACE, markerFactory.getLog4jMarker(marker), null);
    }

    @Override
    public void trace(final Marker marker, final String s) {
        logger.logIfEnabled(FQCN, Level.TRACE, markerFactory.getLog4jMarker(marker), s);
    }

    @Override
    public void trace(final Marker marker, final String s, final Object o) {
        logger.logIfEnabled(FQCN, Level.TRACE, markerFactory.getLog4jMarker(marker), s, o);
    }

    @Override
    public void trace(final Marker marker, final String s, final Object o, final Object o1) {
        logger.logIfEnabled(FQCN, Level.TRACE, markerFactory.getLog4jMarker(marker), s, o, o1);
    }

    @Override
    public void trace(final Marker marker, final String s, final Object... objects) {
        logger.logIfEnabled(FQCN, Level.TRACE, markerFactory.getLog4jMarker(marker), s, objects);
    }

    @Override
    public void trace(final Marker marker, final String s, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.TRACE, markerFactory.getLog4jMarker(marker), s, throwable);
    }

    @Override
    public void debug(final String format) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, format);
    }

    @Override
    public void debug(final String format, final Object o) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, format, o);
    }

    @Override
    public void debug(final String format, final Object arg1, final Object arg2) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, format, arg1, arg2);
    }

    @Override
    public void debug(final String format, final Object... args) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, format, args);
    }

    @Override
    public void debug(final String format, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, format, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isEnabled(Level.DEBUG, null, null);
    }

    @Override
    public boolean isDebugEnabled(final Marker marker) {
        return logger.isEnabled(Level.DEBUG, markerFactory.getLog4jMarker(marker), null);
    }

    @Override
    public void debug(final Marker marker, final String s) {
        logger.logIfEnabled(FQCN, Level.DEBUG, markerFactory.getLog4jMarker(marker), s);
    }

    @Override
    public void debug(final Marker marker, final String s, final Object o) {
        logger.logIfEnabled(FQCN, Level.DEBUG, markerFactory.getLog4jMarker(marker), s, o);
    }

    @Override
    public void debug(final Marker marker, final String s, final Object o, final Object o1) {
        logger.logIfEnabled(FQCN, Level.DEBUG, markerFactory.getLog4jMarker(marker), s, o, o1);
    }

    @Override
    public void debug(final Marker marker, final String s, final Object... objects) {
        logger.logIfEnabled(FQCN, Level.DEBUG, markerFactory.getLog4jMarker(marker), s, objects);
    }

    @Override
    public void debug(final Marker marker, final String s, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.DEBUG, markerFactory.getLog4jMarker(marker), s, throwable);
    }

    @Override
    public void info(final String format) {
        logger.logIfEnabled(FQCN, Level.INFO, null, format);
    }

    @Override
    public void info(final String format, final Object o) {
        logger.logIfEnabled(FQCN, Level.INFO, null, format, o);
    }

    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        logger.logIfEnabled(FQCN, Level.INFO, null, format, arg1, arg2);
    }

    @Override
    public void info(final String format, final Object... args) {
        logger.logIfEnabled(FQCN, Level.INFO, null, format, args);
    }

    @Override
    public void info(final String format, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.INFO, null, format, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isEnabled(Level.INFO, null, null);
    }

    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return logger.isEnabled(Level.INFO, markerFactory.getLog4jMarker(marker), null);
    }

    @Override
    public void info(final Marker marker, final String s) {
        logger.logIfEnabled(FQCN, Level.INFO, markerFactory.getLog4jMarker(marker), s);
    }

    @Override
    public void info(final Marker marker, final String s, final Object o) {
        logger.logIfEnabled(FQCN, Level.INFO, markerFactory.getLog4jMarker(marker), s, o);
    }

    @Override
    public void info(final Marker marker, final String s, final Object o, final Object o1) {
        logger.logIfEnabled(FQCN, Level.INFO, markerFactory.getLog4jMarker(marker), s, o, o1);
    }

    @Override
    public void info(final Marker marker, final String s, final Object... objects) {
        logger.logIfEnabled(FQCN, Level.INFO, markerFactory.getLog4jMarker(marker), s, objects);
    }

    @Override
    public void info(final Marker marker, final String s, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.INFO, markerFactory.getLog4jMarker(marker), s, throwable);
    }

    @Override
    public void warn(final String format) {
        logger.logIfEnabled(FQCN, Level.WARN, null, format);
    }

    @Override
    public void warn(final String format, final Object o) {
        logger.logIfEnabled(FQCN, Level.WARN, null, format, o);
    }

    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        logger.logIfEnabled(FQCN, Level.WARN, null, format, arg1, arg2);
    }

    @Override
    public void warn(final String format, final Object... args) {
        logger.logIfEnabled(FQCN, Level.WARN, null, format, args);
    }

    @Override
    public void warn(final String format, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.WARN, null, format, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isEnabled(Level.WARN, null, null);
    }

    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return logger.isEnabled(Level.WARN, markerFactory.getLog4jMarker(marker), null);
    }

    @Override
    public void warn(final Marker marker, final String s) {
        logger.logIfEnabled(FQCN, Level.WARN, markerFactory.getLog4jMarker(marker), s);
    }

    @Override
    public void warn(final Marker marker, final String s, final Object o) {
        logger.logIfEnabled(FQCN, Level.WARN, markerFactory.getLog4jMarker(marker), s, o);
    }

    @Override
    public void warn(final Marker marker, final String s, final Object o, final Object o1) {
        logger.logIfEnabled(FQCN, Level.WARN, markerFactory.getLog4jMarker(marker), s, o, o1);
    }

    @Override
    public void warn(final Marker marker, final String s, final Object... objects) {
        logger.logIfEnabled(FQCN, Level.WARN, markerFactory.getLog4jMarker(marker), s, objects);
    }

    @Override
    public void warn(final Marker marker, final String s, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.WARN, markerFactory.getLog4jMarker(marker), s, throwable);
    }

    @Override
    public void error(final String format) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, format);
    }

    @Override
    public void error(final String format, final Object o) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, format, o);
    }

    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, format, arg1, arg2);
    }

    @Override
    public void error(final String format, final Object... args) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, format, args);
    }

    @Override
    public void error(final String format, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, format, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isEnabled(Level.ERROR, null, null);
    }

    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return logger.isEnabled(Level.ERROR, markerFactory.getLog4jMarker(marker), null);
    }

    @Override
    public void error(final Marker marker, final String s) {
        logger.logIfEnabled(FQCN, Level.ERROR, markerFactory.getLog4jMarker(marker), s);
    }

    @Override
    public void error(final Marker marker, final String s, final Object o) {
        logger.logIfEnabled(FQCN, Level.ERROR, markerFactory.getLog4jMarker(marker), s, o);
    }

    @Override
    public void error(final Marker marker, final String s, final Object o, final Object o1) {
        logger.logIfEnabled(FQCN, Level.ERROR, markerFactory.getLog4jMarker(marker), s, o, o1);
    }

    @Override
    public void error(final Marker marker, final String s, final Object... objects) {
        logger.logIfEnabled(FQCN, Level.ERROR, markerFactory.getLog4jMarker(marker), s, objects);
    }

    @Override
    public void error(final Marker marker, final String s, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.ERROR, markerFactory.getLog4jMarker(marker), s, throwable);
    }

    @Override
    public void log(final Marker marker, final String fqcn, final int level, final String message, final Object[] params, final Throwable throwable) {
        final Level log4jLevel = getLevel(level);
        final org.apache.logging.log4j.Marker log4jMarker = markerFactory.getLog4jMarker(marker);

        if (!logger.isEnabled(log4jLevel, log4jMarker, message, params)) {
            return;
        }
        final Message msg;
        final Throwable actualThrowable;
        if (params == null) {
            msg = new SimpleMessage(message);
            actualThrowable = throwable;
        } else {
            msg = new ParameterizedMessage(message, params, throwable);
            actualThrowable = throwable != null ? throwable : msg.getThrowable();
        }
        logger.logMessage(fqcn, log4jLevel, log4jMarker, msg, actualThrowable);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Always treat de-serialization as a full-blown constructor, by validating the final state of
     * the de-serialized object.
     */
    private void readObject(final ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
        // always perform the default de-serialization first
        aInputStream.defaultReadObject();
        logger = LogManager.getContext().getLogger(name);
        markerFactory = ((Log4jLoggerFactory) org.slf4j.LoggerFactory.getILoggerFactory()).getMarkerFactory();
    }

    /**
     * This is the default implementation of writeObject. Customise if necessary.
     */
    private void writeObject(final ObjectOutputStream aOutputStream) throws IOException {
        // perform the default serialization for all non-transient, non-static fields
        aOutputStream.defaultWriteObject();
    }

    private static Level getLevel(final int i) {
        switch (i) {
        case TRACE_INT:
            return Level.TRACE;
        case DEBUG_INT:
            return Level.DEBUG;
        case INFO_INT:
            return Level.INFO;
        case WARN_INT:
            return Level.WARN;
        case ERROR_INT:
            return Level.ERROR;
        }
        return Level.ERROR;
    }

    @Override
    public LoggingEventBuilder makeLoggingEventBuilder(org.slf4j.event.Level level) {
        final Level log4jLevel = getLevel(level.toInt());
        if (logger.isEnabled(log4jLevel)) {
            return new Log4jEventBuilder(markerFactory, logger.atLevel(log4jLevel));
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atTrace() {
        if (logger.isTraceEnabled()) {
            return new Log4jEventBuilder(markerFactory, logger.atTrace());
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atDebug() {
        if (logger.isDebugEnabled()) {
            return new Log4jEventBuilder(markerFactory, logger.atDebug());
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atInfo() {
        if (logger.isInfoEnabled()) {
            return new Log4jEventBuilder(markerFactory, logger.atInfo());
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atWarn() {
        if (logger.isWarnEnabled()) {
            return new Log4jEventBuilder(markerFactory, logger.atWarn());
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public LoggingEventBuilder atError() {
        if (logger.isErrorEnabled()) {
            return new Log4jEventBuilder(markerFactory, logger.atError());
        }
        return NOPLoggingEventBuilder.singleton();
    }

    @Override
    public boolean isEnabledForLevel(org.slf4j.event.Level level) {
        return logger.isEnabled(getLevel(level.toInt()));
    }

    
}
