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
package org.apache.logging.log4j.util;

import java.util.Collection;
import java.util.Map;

/**
 * PropertySource implementation that uses environment variables as a source.
 * All environment variables must begin with {@code LOG4J_} so as not to
 * conflict with other variables. Normalized environment variables follow a
 * scheme like this: {@code log4j2.fooBarProperty} would normalize to
 * {@code LOG4J_FOO_BAR_PROPERTY}.
 *
 * @since 2.10.0
 */
public class EnvironmentPropertySource implements PropertySource {

	private static final String PREFIX = "LOG4J_";
	private static final int DEFAULT_PRIORITY = 100;

	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}

	private void logException(SecurityException e) {
		// There is no status logger yet.
		LowLevelLogUtil.logException(
				"The system environment variables are not available to Log4j due to security restrictions: " + e, e);
	}

	@Override
	public void forEach(final BiConsumer<String, String> action) {
		final Map<String, String> getenv;
		try {
			getenv = System.getenv();
		} catch (final SecurityException e) {
			logException(e);
			return;
		}
		for (final Map.Entry<String, String> entry : getenv.entrySet()) {
			final String key = entry.getKey();
			if (key.startsWith(PREFIX)) {
				action.accept(key.substring(PREFIX.length()), entry.getValue());
			}
		}
	}

	@Override
	public CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
		final StringBuilder sb = new StringBuilder("LOG4J");
		boolean empty = true;
		for (final CharSequence token : tokens) {
			empty = false;
			sb.append('_');
			for (int i = 0; i < token.length(); i++) {
				sb.append(Character.toUpperCase(token.charAt(i)));
			}
		}
		return empty ? null : sb.toString();
	}

	@Override
	public Collection<String> getPropertyNames() {
		try {
			return System.getenv().keySet();
		} catch (final SecurityException e) {
			logException(e);
			return PropertySource.super.getPropertyNames();
		}
	}

	@Override
	public String getProperty(String key) {
		try {
			return System.getenv(key);
		} catch (final SecurityException e) {
			logException(e);
			return PropertySource.super.getProperty(key);
		}
	}

	@Override
	public boolean containsProperty(String key) {
		try {
			return System.getenv().containsKey(key);
		} catch (final SecurityException e) {
			logException(e);
			return PropertySource.super.containsProperty(key);
		}
	}

}
