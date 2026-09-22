/*
 * Copyright © 2026 Deutsche Telekom AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amdocs.zusammen.commons.db.impl.cassandra;

import com.amdocs.zusammen.commons.configuration.ConfigurationManager;
import com.amdocs.zusammen.commons.configuration.ConfigurationManagerFactory;
import com.amdocs.zusammen.commons.configuration.datatypes.PluginInfo;
import com.amdocs.zusammen.commons.configuration.impl.ConfigurationManagerFactoryImpl;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stands in for the merged {@code zusammen.json} without putting one on the test classpath -
 * {@code ConfigurationManagerImpl} merges every copy it finds, so a fixture file here would leak
 * into other modules' tests. A stub factory also allows non-String values (a JSON list, a JSON
 * boolean), which a system property can never carry.
 */
final class ConfigurationFixture {

    static final String NODES = "cassandra.nodes";
    static final String KEYSPACE = "cassandra.keyspace";
    static final String USER = "cassandra.user";
    static final String PASSWORD = "cassandra.password";
    static final String AUTHENTICATE = "cassandra.authenticate";
    static final String SSL = "cassandra.ssl";
    static final String TRUST_STORE = "cassandra.truststore";
    static final String TRUST_STORE_PASSWORD = "cassandra.truststore.password";
    static final String DATA_CENTER = "cassandra.datacenter";
    static final String CONSISTENCY_LEVEL = "cassandra.consistency.level";
    static final String PORT = "cassandra.port";
    static final String RECONNECT_DELAY = "cassandra.reconnection.delay";

    private static final String[] ALL_PROPERTIES = {
            NODES, KEYSPACE, USER, PASSWORD, AUTHENTICATE, SSL, TRUST_STORE, TRUST_STORE_PASSWORD,
            DATA_CENTER, CONSISTENCY_LEVEL, PORT, RECONNECT_DELAY};

    private static final Map<String, Object> CONFIGURED = new HashMap<>();

    private ConfigurationFixture() {
    }

    static void install() {
        // AbstractComponentFactory's static initialiser reloads the registry from every
        // factoryConfiguration.json on the classpath, which would undo the registration below. A
        // class literal does not trigger it, so force initialisation before registering.
        forceInitialisation(ConfigurationManagerFactory.class);
        CONFIGURED.clear();
        StubConfigurationManagerFactory.manager = new MapConfigurationManager();
        AbstractFactoryBase.registerFactory(
                ConfigurationManagerFactory.class, StubConfigurationManagerFactory.class);
    }

    static void restore() {
        AbstractFactoryBase.registerFactory(
                ConfigurationManagerFactory.class, ConfigurationManagerFactoryImpl.class);
        StubConfigurationManagerFactory.manager = null;
        CONFIGURED.clear();
        for (String property : ALL_PROPERTIES) {
            System.clearProperty(property);
        }
    }

    static void configure(String property, Object value) {
        CONFIGURED.put(property, value);
    }

    static void forceInitialisation(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class StubConfigurationManagerFactory extends ConfigurationManagerFactory {

        static ConfigurationManager manager;

        @Override
        public ConfigurationManager createInterface() {
            return manager;
        }
    }

    private static class MapConfigurationManager implements ConfigurationManager {

        @Override
        public PluginInfo getPluginInfo(String pluginType) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> getProperty(String propertyName) {
            return Optional.ofNullable((T) CONFIGURED.get(propertyName));
        }
    }
}
