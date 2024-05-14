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
package org.apache.logging.log4j.core.appender.db.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDriver;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;

/**
 * A {@link ConnectionSource} that uses a JDBC connection string, a user name, and a password to call
 * {@link DriverManager#getConnection(String, String, String)}. The connections are served from an
 * <a href="http://commons.apache.org/proper/commons-dbcp/">Apache Commons DBCP</a> pooling driver.
 */
@Plugin(name = "PoolingDriver", category = Core.CATEGORY_NAME, elementType = "connectionSource", printObject = true)
public final class PoolingDriverConnectionSource extends AbstractDriverManagerConnectionSource {

    /**
     * Builds PoolingDriverConnectionSource instances.
     *
     * @param <B>
     *            This builder type or a subclass.
     */
    public static class Builder<B extends Builder<B>> extends AbstractDriverManagerConnectionSource.Builder<B>
    implements org.apache.logging.log4j.core.util.Builder<PoolingDriverConnectionSource> {

        public static final String DEFAULT_POOL_NAME = "example";

        @PluginElement("PoolableConnectionFactoryConfig")
        private PoolableConnectionFactoryConfig poolableConnectionFactoryConfig;

        @PluginBuilderAttribute
        private String poolName = DEFAULT_POOL_NAME;

        @Override
		public PoolingDriverConnectionSource build() {
			try {
				return new PoolingDriverConnectionSource(getDriverClassName(), getConnectionString(), getUserName(),
						getPassword(), getProperties(), poolName, poolableConnectionFactoryConfig);
			} catch (final SQLException e) {
				getLogger().error("Exception constructing {} to '{}' with {}", PoolingDriverConnectionSource.class,
						getConnectionString(), this, e);
				return null;
			}
		}

        public B setPoolableConnectionFactoryConfig(final PoolableConnectionFactoryConfig poolableConnectionFactoryConfig) {
            this.poolableConnectionFactoryConfig = poolableConnectionFactoryConfig;
            return asBuilder();
        }

		public B setPoolName(final String poolName) {
            this.poolName = poolName;
            return asBuilder();
        }

        @Override
		public String toString() {
			return "Builder [poolName=" + poolName + ", connectionString=" + connectionString + ", driverClassName="
					+ driverClassName + ", properties=" + Arrays.toString(properties) + ", userName="
					+ Arrays.toString(userName) + "]";
		}
    }

    public static final String URL_PREFIX = "jdbc:apache:commons:dbcp:";

    // This method is not named newBuilder() to make the compiler happy.
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newPoolingDriverConnectionSourceBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final String poolingDriverClassName = "org.apache.commons.dbcp2.PoolingDriver";

    private final String poolName;

    /**
     * @deprecated Use {@link #newPoolingDriverConnectionSourceBuilder()}.
     */
    @Deprecated
    public PoolingDriverConnectionSource(final String driverClassName, final String connectionString,
            final char[] userName, final char[] password, final Property[] properties, final String poolName)
            throws SQLException {
        super(driverClassName, connectionString, URL_PREFIX + poolName, userName, password, properties);
        this.poolName = poolName;
        setupDriver(connectionString, null);
    }

    private PoolingDriverConnectionSource(final String driverClassName, final String connectionString,
            final char[] userName, final char[] password, final Property[] properties, final String poolName,
            final PoolableConnectionFactoryConfig poolableConnectionFactoryConfig)
            throws SQLException {
        super(driverClassName, connectionString, URL_PREFIX + poolName, userName, password, properties);
        this.poolName = poolName;
        setupDriver(connectionString, poolableConnectionFactoryConfig);
    }

    @Override
    public String getActualConnectionString() {
        // TODO Auto-generated method stub
        return super.getActualConnectionString();
    }

    private PoolingDriver getPoolingDriver() throws SQLException {
        final PoolingDriver driver = (PoolingDriver) DriverManager.getDriver(URL_PREFIX);
        if (driver == null) {
            getLogger().error("No JDBC driver for '{}'", URL_PREFIX);
        }
        return driver;
    }

    private void setupDriver(final String connectionString,
            final PoolableConnectionFactoryConfig poolableConnectionFactoryConfig) throws SQLException {
        //
        // First, we'll create a ConnectionFactory that the
        // pool will use to create Connections.
        // We'll use the DriverManagerConnectionFactory,
        // using the connect string passed in the command line
        // arguments.
        //
    	final Property[] properties = getProperties();
    	final char[] userName = getUserName();
    	final char[] password = getPassword();
    	final ConnectionFactory connectionFactory;
        if (properties != null && properties.length > 0) {
            if (userName != null || password != null) {
                throw new SQLException("Either set the userName and password, or set the Properties, but not both.");
            }
            connectionFactory = new DriverManagerConnectionFactory(connectionString, toProperties(properties));
        } else {
            connectionFactory = new DriverManagerConnectionFactory(connectionString, toString(userName), toString(password));
        }

        //
        // Next, we'll create the PoolableConnectionFactory, which wraps
        // the "real" Connections created by the ConnectionFactory with
        // the classes that implement the pooling functionality.
        //
        final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,
                null);
        if (poolableConnectionFactoryConfig != null) {
            poolableConnectionFactoryConfig.init(poolableConnectionFactory);
        }

        //
        // Now we'll need a ObjectPool that serves as the
        // actual pool of connections.
        //
        // We'll use a GenericObjectPool instance, although
        // any ObjectPool implementation will suffice.
        //
        @SuppressWarnings("resource")
        // This GenericObjectPool will be closed on shutdown
        final ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);

        // Set the factory's pool property to the owning pool
        poolableConnectionFactory.setPool(connectionPool);

        loadDriver(poolingDriverClassName);
        final PoolingDriver driver = getPoolingDriver();
        if (driver != null) {
            getLogger().debug("Registering DBCP pool '{}' with pooling driver {}: {}", poolName, driver, connectionPool);
            driver.registerPool(poolName, connectionPool);
        }
        //
        // Now we can just use the connect string "jdbc:apache:commons:dbcp:example"
        // to access our pool of Connections.
        //
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        try {
            final PoolingDriver driver = getPoolingDriver();
            if (driver != null) {
                getLogger().debug("Driver {} closing DBCP pool '{}'", driver, poolName);
                driver.closePool(poolName);
            }
            return true;
        } catch (final Exception e) {
            getLogger().error("Exception stopping connection source for '{}' → '{}'", getConnectionString(),
                    getActualConnectionString(), e);
            return false;
        }
    }
}
