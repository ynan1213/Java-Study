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
package org.apache.logging.log4j.kubernetes;

import java.time.Duration;

import org.apache.logging.log4j.util.PropertiesUtil;

import io.fabric8.kubernetes.client.Config;

/**
 * Obtains properties used to configure the Kubernetes client.
 */
public class KubernetesClientProperties {

    private static final String[] PREFIXES = {"log4j2.kubernetes.client.", "spring.cloud.kubernetes.client."};
    private static final String API_VERSION = "apiVersion";
    private static final String CA_CERT_FILE = "caCertFile";
    private static final String CA_CERT_DATA = "caCertData";
    private static final String CLIENT_CERT_FILE = "clientCertFile";
    private static final String CLIENT_CERT_DATA = "clientCertData";
    private static final String CLIENT_KEY_FILE = "clientKeyFile";
    private static final String CLIENT_KEY_DATA = "cientKeyData";
    private static final String CLIENT_KEY_ALGO = "clientKeyAlgo";
    private static final String CLIENT_KEY_PASSPHRASE = "clientKeyPassphrase";
    private static final String CONNECTION_TIMEOUT = "connectionTimeout";
    private static final String HTTP_PROXY = "httpProxy";
    private static final String HTTPS_PROXY = "httpsProxy";
    private static final String LOGGING_INTERVAL = "loggingInterval";
    private static final String MASTER_URL = "masterUrl";
    private static final String NAMESPACE = "namespace";
    private static final String NO_PROXY = "noProxy";
    private static final String PASSWORD = "password";
    private static final String PROXY_USERNAME = "proxyUsername";
    private static final String PROXY_PASSWORD = "proxyPassword";
    private static final String REQUEST_TIMEOUT = "requestTimeout";
    private static final String ROLLING_TIMEOUT = "rollingTimeout";
    private static final String TRUST_CERTS = "trustCerts";
    private static final String USERNAME = "username";
    private static final String WATCH_RECONNECT_INTERVAL = "watchReconnectInterval";
    private static final String WATCH_RECONNECT_LIMIT = "watchReconnectLimit";

    private PropertiesUtil props = PropertiesUtil.getProperties();
    private final Config base;

    public KubernetesClientProperties(Config base) {
        this.base = base;
    }


    public String getApiVersion() {
        return props.getStringProperty(PREFIXES, API_VERSION, base::getApiVersion);
    }
    public String getCaCertFile() {
        return props.getStringProperty(PREFIXES, CA_CERT_FILE, base::getCaCertFile);
    }

    public String getCaCertData() {
        return props.getStringProperty(PREFIXES, CA_CERT_DATA, base::getCaCertData);
    }

    public String getClientCertFile() {
        return props.getStringProperty(PREFIXES, CLIENT_CERT_FILE, base::getClientCertFile);
    }

    public String getClientCertData() {
        return props.getStringProperty(PREFIXES, CLIENT_CERT_DATA, base::getClientCertData);
    }

    public String getClientKeyFile() {
        return props.getStringProperty(PREFIXES, CLIENT_KEY_FILE, base::getClientKeyFile);
    }

    public String getClientKeyData() {
        return props.getStringProperty(PREFIXES, CLIENT_KEY_DATA, base::getClientKeyData);
    }

    public String getClientKeyAlgo() {
        return props.getStringProperty(PREFIXES, CLIENT_KEY_ALGO, base::getClientKeyAlgo);
    }

    public String getClientKeyPassphrase() {
        return props.getStringProperty(PREFIXES, CLIENT_KEY_PASSPHRASE, base::getClientKeyPassphrase);
    }

    public int getConnectionTimeout() {
        Duration timeout = props.getDurationProperty(PREFIXES, CONNECTION_TIMEOUT, null);
        if (timeout != null) {
            return (int) timeout.toMillis();
        }
        return base.getConnectionTimeout();
    }

    public String getHttpProxy() {
        return props.getStringProperty(PREFIXES, HTTP_PROXY, base::getHttpProxy);
    }

    public String getHttpsProxy() {
        return props.getStringProperty(PREFIXES, HTTPS_PROXY, base::getHttpsProxy);
    }

    public int getLoggingInterval() {
        Duration interval = props.getDurationProperty(PREFIXES, LOGGING_INTERVAL, null);
        if (interval != null) {
            return (int) interval.toMillis();
        }
        return base.getLoggingInterval();
    }

    public String getMasterUrl() {
        return props.getStringProperty(PREFIXES, MASTER_URL, base::getMasterUrl);
    }

    public String getNamespace() {
        return props.getStringProperty(PREFIXES, NAMESPACE, base::getNamespace);
    }

    public String[] getNoProxy() {
        String result = props.getStringProperty(PREFIXES, NO_PROXY, null);
        if (result != null) {
            return result.replace("\\s", "").split(",");
        }
        return base.getNoProxy();
    }

    public String getPassword() {
        return props.getStringProperty(PREFIXES, PASSWORD, base::getPassword);
    }

    public String getProxyUsername() {
        return props.getStringProperty(PREFIXES, PROXY_USERNAME, base::getProxyUsername);
    }

    public String getProxyPassword() {
        return props.getStringProperty(PREFIXES, PROXY_PASSWORD, base::getProxyPassword);
    }

    public int getRequestTimeout() {
        Duration interval = props.getDurationProperty(PREFIXES, REQUEST_TIMEOUT, null);
        if (interval != null) {
            return (int) interval.toMillis();
        }
        return base.getRequestTimeout();
    }

    public long getRollingTimeout() {
        Duration interval = props.getDurationProperty(PREFIXES, ROLLING_TIMEOUT, null);
        if (interval != null) {
            return interval.toMillis();
        }
        return base.getRollingTimeout();
    }

    public Boolean isTrustCerts() {
        return props.getBooleanProperty(PREFIXES, TRUST_CERTS, base::isTrustCerts);
    }

    public String getUsername() {
        return props.getStringProperty(PREFIXES, USERNAME, base::getUsername);
    }

    public int getWatchReconnectInterval() {
        Duration interval = props.getDurationProperty(PREFIXES, WATCH_RECONNECT_INTERVAL, null);
        if (interval != null) {
            return (int) interval.toMillis();
        }
        return base.getWatchReconnectInterval();
    }

    public int getWatchReconnectLimit() {
        Duration interval = props.getDurationProperty(PREFIXES, WATCH_RECONNECT_LIMIT, null);
        if (interval != null) {
            return (int) interval.toMillis();
        }
        return base.getWatchReconnectLimit();
    }
}
