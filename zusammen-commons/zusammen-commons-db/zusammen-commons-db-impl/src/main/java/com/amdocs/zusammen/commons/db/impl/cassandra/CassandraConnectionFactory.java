/*
 * Copyright © 2016-2017 European Support Limited
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

import com.amdocs.zusammen.commons.log.ZusammenLogger;
import com.amdocs.zusammen.commons.log.ZusammenLoggerFactory;
import com.datastax.driver.core.*;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.policies.*;
import com.datastax.driver.mapping.MappingManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

class CassandraConnectionFactory {

    private static final ZusammenLogger LOGGER = ZusammenLoggerFactory.getLogger(CassandraConnectionFactory.class.getName());
    private static final long SHUTDOWN_TIMEOUT = 10;
    private static final String DEFAULT_KEYSPACE = CassandraConfig.getKeyspace();
    private static final String TENANT_KEYSPACE_PREFIX = DEFAULT_KEYSPACE + "_";

    private static Cluster cluster = initCluster();
    private static Map<String, MappingManager> mappingManagerByKeyspace = new HashMap<>();

    static Configuration getConfiguration() {
        return cluster.getConfiguration();
    }

    static Session getSession(String tenant) {
        return getMappingManager(tenant).getSession();
    }

    static MappingManager getMappingManager(String tenant) {
        String keyspace = getKeyspace(tenant);
        return mappingManagerByKeyspace.computeIfAbsent(keyspace, k -> new MappingManager(initSession(keyspace)));
    }

    private static String getKeyspace(String tenant) {
        return tenant == null ? DEFAULT_KEYSPACE : TENANT_KEYSPACE_PREFIX + tenant;
    }

    private static Session initSession(String keyspace) {
        return cluster.connect(keyspace);
    }

    private static Cluster initCluster() {
        String[] nodes = CassandraConfig.getNodes();

        if (nodes.length == 0) {
            throw new IllegalStateException("no nodes specified");
        }
        Builder builder = Cluster.builder();
        builder.withoutJMXReporting();

        CassandraConfig.getReconnectionDelay().ifPresent(delay -> builder
                .withReconnectionPolicy(new ConstantReconnectionPolicy(delay))
                .withRetryPolicy(DefaultRetryPolicy.INSTANCE));

        builder.addContactPoints(nodes);

        CassandraConfig.getPort().ifPresent(builder::withPort);

        if (CassandraConfig.isSsl()) {
            getSSLOptions().ifPresent(builder::withSSL);
        }

        if (CassandraConfig.isAuthenticate()) {
            builder.withCredentials(CassandraConfig.getUser(), CassandraConfig.getPassword());
        }

        Optional<String> dataCenter = CassandraConfig.getDataCenter();
        if (dataCenter.isPresent()) {
            LoadBalancingPolicy tokenAwarePolicy =
                    new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(dataCenter.get()).build());
            builder.withLoadBalancingPolicy(tokenAwarePolicy);
        }
        if (nodes.length > 1) {
            CassandraConfig.getConsistencyLevel().ifPresent(
                    s -> builder.withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.valueOf(s))));
        }

        LOGGER.debug("Creating Cassandra cluster to hosts:{} port:{} with reconnect timeout:{} SSL is: {}",
                nodes, CassandraConfig.getPort().isPresent() ? CassandraConfig.getPort().get() : "default",
                CassandraConfig.getReconnectionDelay().isPresent() ? CassandraConfig.getReconnectionDelay().get() :
                        "default", CassandraConfig.isSsl());

        return builder.build();
    }

    void shutdown() {
        if (cluster != null) {
            try {
                // Close asynchronously, then get with our timeout value
                // get will throw exception if timeout exceeded, or close fails
                cluster.closeAsync().get(SHUTDOWN_TIMEOUT, SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Unable to shutdown cluster", e);
            }
        }
    }

    private static Optional<SSLOptions> getSSLOptions() {
        Optional<String> truststore = CassandraConfig.getTrustStore();
        Optional<String> truststorePassword = CassandraConfig.getTrustStorePassword();

        if (truststore.isPresent() && truststorePassword.isPresent()) {
            SSLContext context = getSSLContext(truststore.get(), truststorePassword.get());

            String[] css = new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA"};
            return Optional.of(
                    RemoteEndpointAwareJdkSSLOptions.builder().withSSLContext(context).withCipherSuites(css).build());
        }
        return Optional.empty();
    }

    private static SSLContext getSSLContext(String truststorePath, String truststorePassword) {
        SSLContext ctx = null;
        try (FileInputStream tsf = new FileInputStream(truststorePath)) {
            ctx = SSLContext.getInstance("SSL");

            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(tsf, truststorePassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
        } catch (Exception e) {
            LOGGER.error("Error while getting SSL context", e);
        }
        return ctx;
    }


}
