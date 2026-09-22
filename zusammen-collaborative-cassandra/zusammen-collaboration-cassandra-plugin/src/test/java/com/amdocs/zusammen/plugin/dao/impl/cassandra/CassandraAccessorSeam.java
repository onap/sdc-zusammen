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

package com.amdocs.zusammen.plugin.dao.impl.cassandra;

import static org.mockito.Mockito.when;

import com.amdocs.zusammen.commons.db.api.cassandra.CassandraConnector;
import com.amdocs.zusammen.commons.db.api.cassandra.CassandraConnectorFactory;
import com.amdocs.zusammen.commons.db.api.cassandra.types.CassandraContext;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.Configuration;
import com.datastax.driver.mapping.MappingManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;

/**
 * Replaces the Cassandra connector behind {@link CassandraDaoUtils} so the DAO implementations can
 * be exercised without a cluster: every {@code @Accessor} interface they ask for is served from the
 * stubs registered here, and the {@link CassandraContext} they derive from the session context is
 * recorded for assertion.
 */
class CassandraAccessorSeam {

    private static final String PRODUCTION_CONNECTOR_FACTORY =
            "com.amdocs.zusammen.commons.db.impl.cassandra.CassandraConnectorFactoryImpl";

    private static final Map<Class<?>, Object> ACCESSORS = new HashMap<>();
    private static final List<CassandraContext> CASSANDRA_CONTEXTS = new ArrayList<>();
    private static final CodecRegistry CODEC_REGISTRY = new CodecRegistry();

    private static MappingManager mappingManager;
    private static Configuration configuration;

    private CassandraAccessorSeam() {
    }

    static void install() {
        ACCESSORS.clear();
        CASSANDRA_CONTEXTS.clear();

        configuration = Mockito.mock(Configuration.class);
        when(configuration.getCodecRegistry()).thenReturn(CODEC_REGISTRY);

        mappingManager = Mockito.mock(MappingManager.class);
        when(mappingManager.createAccessor(Mockito.any())).thenAnswer(invocation -> {
            Class<?> accessorType = (Class<?>) invocation.getArguments()[0];
            Object accessor = ACCESSORS.get(accessorType);
            if (accessor == null) {
                throw new AssertionError("no accessor stub registered for " + accessorType.getName());
            }
            return accessor;
        });

        // AbstractComponentFactory's static initialiser reloads every factoryConfiguration.json into
        // the registry, which would undo the stub registered below - so let it run first.
        CassandraConnectorFactory.getInstance();
        AbstractFactoryBase.registerFactory(CassandraConnectorFactory.class, StubFactory.class);
    }

    /**
     * registerFactory has no counterpart that removes a registry entry, so the production mapping has
     * to be put back by name - the implementation is a runtime-scoped dependency and cannot be named
     * as a class literal here.
     */
    @SuppressWarnings("unchecked")
    static void uninstall() throws ClassNotFoundException {
        ACCESSORS.clear();
        CASSANDRA_CONTEXTS.clear();
        mappingManager = null;
        configuration = null;
        AbstractFactoryBase.registerFactory(CassandraConnectorFactory.class,
                (Class<? extends CassandraConnectorFactory>) Class.forName(PRODUCTION_CONNECTOR_FACTORY));
    }

    static <T> void registerAccessor(Class<T> accessorType, T accessor) {
        ACCESSORS.put(accessorType, accessor);
    }

    static CassandraContext lastCassandraContext() {
        if (CASSANDRA_CONTEXTS.isEmpty()) {
            throw new AssertionError("no accessor was requested");
        }
        return CASSANDRA_CONTEXTS.get(CASSANDRA_CONTEXTS.size() - 1);
    }

    static CodecRegistry codecRegistry() {
        return CODEC_REGISTRY;
    }

    public static class StubFactory extends CassandraConnectorFactory {

        @Override
        public CassandraConnector createInterface() {
            return new StubConnector();
        }
    }

    private static class StubConnector implements CassandraConnector {

        @Override
        public MappingManager getMappingManager(CassandraContext context) {
            CASSANDRA_CONTEXTS.add(context);
            return mappingManager;
        }

        @Override
        public Configuration getConfiguration() {
            return configuration;
        }
    }
}
