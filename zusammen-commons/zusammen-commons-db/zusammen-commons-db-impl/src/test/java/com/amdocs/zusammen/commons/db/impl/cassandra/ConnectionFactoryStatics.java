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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.EndPoint;
import com.datastax.driver.mapping.MappingManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reaches the static state of {@link CassandraConnectionFactory}. The class resolves the keyspace
 * and builds the cluster in its static initialiser, so the first test to touch it fixes both for
 * the rest of the JVM; every test class in this module must therefore prime it through
 * {@link #prime()} rather than rely on what another class left behind.
 */
final class ConnectionFactoryStatics {

    static final String PRIMED_NODE = "127.0.0.1";
    static final String PRIMED_KEYSPACE = "zusammen_primed";

    private static boolean primed;

    private ConnectionFactoryStatics() {
    }

    static synchronized void prime() {
        if (primed) {
            return;
        }
        System.setProperty(ConfigurationFixture.NODES, PRIMED_NODE);
        System.setProperty(ConfigurationFixture.KEYSPACE, PRIMED_KEYSPACE);
        // isSsl and isAuthenticate throw when their property is absent, so the cluster cannot be
        // built without them.
        System.setProperty(ConfigurationFixture.SSL, "false");
        System.setProperty(ConfigurationFixture.AUTHENTICATE, "false");
        try {
            ConfigurationFixture.forceInitialisation(CassandraConnectionFactory.class);
        } finally {
            System.clearProperty(ConfigurationFixture.NODES);
            System.clearProperty(ConfigurationFixture.KEYSPACE);
            System.clearProperty(ConfigurationFixture.SSL);
            System.clearProperty(ConfigurationFixture.AUTHENTICATE);
            primed = true;
        }
    }

    static Cluster initCluster() {
        prime();
        try {
            Method initCluster = CassandraConnectionFactory.class.getDeclaredMethod("initCluster");
            initCluster.setAccessible(true);
            return (Cluster) initCluster.invoke(null);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static Cluster getCluster() {
        return (Cluster) read("cluster");
    }

    static void setCluster(Cluster cluster) {
        write("cluster", cluster);
    }

    @SuppressWarnings("unchecked")
    static Map<String, MappingManager> mappingManagers() {
        return (Map<String, MappingManager>) read("mappingManagerByKeyspace");
    }

    @SuppressWarnings("unchecked")
    static List<InetSocketAddress> contactPointsOf(Cluster cluster) {
        try {
            Field manager = Cluster.class.getDeclaredField("manager");
            manager.setAccessible(true);
            Object clusterManager = manager.get(cluster);
            Field contactPoints = clusterManager.getClass().getDeclaredField("contactPoints");
            contactPoints.setAccessible(true);
            List<InetSocketAddress> addresses = new ArrayList<>();
            for (EndPoint contactPoint : (List<EndPoint>) contactPoints.get(clusterManager)) {
                addresses.add(contactPoint.resolve());
            }
            return addresses;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object read(String name) {
        prime();
        try {
            return field(name).get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(String name, Object value) {
        prime();
        try {
            field(name).set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = CassandraConnectionFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
