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

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.Authenticator;
import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.JdkSSLOptions;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.RemoteEndpointAwareJdkSSLOptions;
import com.datastax.driver.core.SSLOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.Policies;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.datastax.driver.mapping.MappingManager;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CassandraConnectionFactoryTest {

    private static final String CACERTS_PASSWORD = "changeit";

    private Cluster primedCluster;

    @BeforeMethod
    public void setUp() {
        ConfigurationFixture.install();
        ConnectionFactoryStatics.prime();
        primedCluster = ConnectionFactoryStatics.getCluster();
        ConnectionFactoryStatics.mappingManagers().clear();
    }

    @AfterMethod
    public void tearDown() {
        ConnectionFactoryStatics.setCluster(primedCluster);
        ConnectionFactoryStatics.mappingManagers().clear();
        ConfigurationFixture.restore();
    }

    @Test
    public void testInitClusterUsesConfiguredNodesAsContactPoints() {
        configureNodes("127.0.0.1", "127.0.0.2");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        Assert.assertEquals(hostAddressesOf(cluster), Arrays.asList("127.0.0.1", "127.0.0.2"));
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "no nodes specified")
    public void testInitClusterFailsWhenNodeListIsEmpty() {
        ConfigurationFixture.configure(ConfigurationFixture.NODES, Collections.emptyList());

        ConnectionFactoryStatics.initCluster();
    }

    @Test
    public void testInitClusterUsesDefaultPortWhenPortNotConfigured() {
        configureNodes("127.0.0.1");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        Assert.assertEquals(cluster.getConfiguration().getProtocolOptions().getPort(),
                ProtocolOptions.DEFAULT_PORT);
    }

    @Test
    public void testInitClusterUsesConfiguredPort() {
        configureNodes("127.0.0.1");
        ConfigurationFixture.configure(ConfigurationFixture.PORT, "19042");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        Assert.assertEquals(cluster.getConfiguration().getProtocolOptions().getPort(), 19042);
        Assert.assertEquals(
                ConnectionFactoryStatics.contactPointsOf(cluster).get(0).getPort(), 19042);
    }

    @Test
    public void testInitClusterDisablesJmxReporting() {
        configureNodes("127.0.0.1");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        Assert.assertFalse(cluster.getConfiguration().getMetricsOptions().isJMXReportingEnabled());
    }

    @Test
    public void testInitClusterAppliesConfiguredReconnectionDelay() {
        configureNodes("127.0.0.1");
        ConfigurationFixture.configure(ConfigurationFixture.RECONNECT_DELAY, "7000");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        Policies policies = cluster.getConfiguration().getPolicies();
        Assert.assertTrue(policies.getReconnectionPolicy() instanceof ConstantReconnectionPolicy);
        Assert.assertEquals(
                ((ConstantReconnectionPolicy) policies.getReconnectionPolicy()).getConstantDelayMs(),
                7000L);
        Assert.assertSame(policies.getRetryPolicy(), DefaultRetryPolicy.INSTANCE);
    }

    @Test
    public void testInitClusterKeepsDriverReconnectionPolicyWhenDelayNotConfigured() {
        configureNodes("127.0.0.1");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        Assert.assertSame(cluster.getConfiguration().getPolicies().getReconnectionPolicy(),
                Policies.defaultReconnectionPolicy());
    }

    @Test
    public void testInitClusterPassesCredentialsWhenAuthenticationEnabled() {
        configureNodes("127.0.0.1");
        ConfigurationFixture.configure(ConfigurationFixture.AUTHENTICATE, "true");
        ConfigurationFixture.configure(ConfigurationFixture.USER, "zusammen_user");
        ConfigurationFixture.configure(ConfigurationFixture.PASSWORD, "s3cret");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        AuthProvider authProvider = cluster.getConfiguration().getProtocolOptions().getAuthProvider();
        Assert.assertTrue(authProvider instanceof PlainTextAuthProvider);
        Assert.assertEquals(plainTextCredentials(authProvider),
                Arrays.asList("zusammen_user", "s3cret"));
    }

    @Test
    public void testInitClusterPassesNoCredentialsWhenAuthenticationDisabled() {
        configureNodes("127.0.0.1");
        ConfigurationFixture.configure(ConfigurationFixture.AUTHENTICATE, "false");
        ConfigurationFixture.configure(ConfigurationFixture.USER, "zusammen_user");
        ConfigurationFixture.configure(ConfigurationFixture.PASSWORD, "s3cret");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        Assert.assertSame(cluster.getConfiguration().getProtocolOptions().getAuthProvider(),
                AuthProvider.NONE);
    }

    @Test
    public void testInitClusterMakesLoadBalancingLocalToConfiguredDataCenter() {
        configureNodes("127.0.0.1");
        ConfigurationFixture.configure(ConfigurationFixture.DATA_CENTER, "dc1");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        LoadBalancingPolicy policy = cluster.getConfiguration().getPolicies().getLoadBalancingPolicy();
        Assert.assertTrue(policy instanceof TokenAwarePolicy);
        LoadBalancingPolicy childPolicy = ((TokenAwarePolicy) policy).getChildPolicy();
        Assert.assertTrue(childPolicy instanceof DCAwareRoundRobinPolicy);
        Assert.assertEquals(localDcOf((DCAwareRoundRobinPolicy) childPolicy), "dc1");
    }

    @Test
    public void testInitClusterAppliesConsistencyLevelWhenSeveralNodesAreConfigured() {
        configureNodes("127.0.0.1", "127.0.0.2");
        ConfigurationFixture.configure(ConfigurationFixture.CONSISTENCY_LEVEL, "QUORUM");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        Assert.assertEquals(cluster.getConfiguration().getQueryOptions().getConsistencyLevel(),
                ConsistencyLevel.QUORUM);
    }

    @Test
    public void testInitClusterBuildsFromASingleNodeWithAConfiguredConsistencyLevel() {
        configureNodes("127.0.0.1");
        ConfigurationFixture.configure(ConfigurationFixture.CONSISTENCY_LEVEL, "QUORUM");

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        Assert.assertEquals(ConnectionFactoryStatics.contactPointsOf(cluster).size(), 1);
        Assert.assertEquals(ConnectionFactoryStatics.contactPointsOf(cluster).get(0).getAddress()
                .getHostAddress(), "127.0.0.1");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInitClusterRejectsUnknownConsistencyLevel() {
        configureNodes("127.0.0.1", "127.0.0.2");
        ConfigurationFixture.configure(ConfigurationFixture.CONSISTENCY_LEVEL, "EVENTUALLY");

        ConnectionFactoryStatics.initCluster();
    }

    @Test
    public void testInitClusterEnablesSslFromConfiguredTrustStore() throws Exception {
        Path trustStore = readableJdkTrustStore();
        configureNodes("127.0.0.1");
        ConfigurationFixture.configure(ConfigurationFixture.SSL, "true");
        ConfigurationFixture.configure(ConfigurationFixture.TRUST_STORE, trustStore.toString());
        ConfigurationFixture.configure(ConfigurationFixture.TRUST_STORE_PASSWORD, CACERTS_PASSWORD);

        Cluster cluster = ConnectionFactoryStatics.initCluster();

        SSLOptions sslOptions = cluster.getConfiguration().getProtocolOptions().getSSLOptions();
        Assert.assertTrue(sslOptions instanceof RemoteEndpointAwareJdkSSLOptions);
        Assert.assertNotSame(sslContextOf(sslOptions), SSLContext.getDefault());
    }

    @Test
    public void testGetConfigurationReturnsConfigurationOfBuiltCluster() {
        Assert.assertSame(CassandraConnectionFactory.getConfiguration(),
                ConnectionFactoryStatics.getCluster().getConfiguration());
    }

    @Test
    public void testGetMappingManagerConnectsToDefaultKeyspaceWithoutTenant() {
        Cluster cluster = clusterConnectingTo(mock(Session.class, RETURNS_DEEP_STUBS));
        ConnectionFactoryStatics.setCluster(cluster);

        CassandraConnectionFactory.getMappingManager(null);

        verify(cluster).connect(ConnectionFactoryStatics.PRIMED_KEYSPACE);
    }

    @Test
    public void testGetMappingManagerConnectsToTenantKeyspace() {
        Cluster cluster = clusterConnectingTo(mock(Session.class, RETURNS_DEEP_STUBS));
        ConnectionFactoryStatics.setCluster(cluster);

        CassandraConnectionFactory.getMappingManager("acme");

        verify(cluster).connect(ConnectionFactoryStatics.PRIMED_KEYSPACE + "_acme");
    }

    @Test
    public void testGetMappingManagerIsCachedPerKeyspace() {
        Cluster cluster = clusterConnectingTo(mock(Session.class, RETURNS_DEEP_STUBS));
        ConnectionFactoryStatics.setCluster(cluster);

        MappingManager first = CassandraConnectionFactory.getMappingManager("acme");
        MappingManager second = CassandraConnectionFactory.getMappingManager("acme");

        Assert.assertSame(second, first);
        verify(cluster, times(1)).connect(anyString());
    }

    @Test
    public void testGetMappingManagerUsesOneManagerPerTenant() {
        Cluster cluster = clusterConnectingTo(mock(Session.class, RETURNS_DEEP_STUBS));
        ConnectionFactoryStatics.setCluster(cluster);

        MappingManager acme = CassandraConnectionFactory.getMappingManager("acme");
        MappingManager other = CassandraConnectionFactory.getMappingManager("other");

        Assert.assertNotSame(other, acme);
    }

    @Test
    public void testGetSessionReturnsSessionOfKeyspace() {
        Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        ConnectionFactoryStatics.setCluster(clusterConnectingTo(session));

        Assert.assertSame(CassandraConnectionFactory.getSession("acme"), session);
    }

    @Test
    public void testShutdownClosesCluster() throws Exception {
        Cluster cluster = mock(Cluster.class);
        CloseFuture closeFuture = mock(CloseFuture.class);
        when(cluster.closeAsync()).thenReturn(closeFuture);
        ConnectionFactoryStatics.setCluster(cluster);

        new CassandraConnectionFactory().shutdown();

        verify(closeFuture).get(10L, TimeUnit.SECONDS);
    }

    @Test(expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "Unable to shutdown cluster")
    public void testShutdownFailsWhenClusterDoesNotCloseInTime() throws Exception {
        Cluster cluster = mock(Cluster.class);
        CloseFuture closeFuture = mock(CloseFuture.class);
        when(closeFuture.get(10L, TimeUnit.SECONDS)).thenThrow(new TimeoutException());
        when(cluster.closeAsync()).thenReturn(closeFuture);
        ConnectionFactoryStatics.setCluster(cluster);

        new CassandraConnectionFactory().shutdown();
    }

    @Test
    public void testShutdownDoesNothingWhenNoClusterWasBuilt() {
        ConnectionFactoryStatics.setCluster(null);

        new CassandraConnectionFactory().shutdown();

        Assert.assertNull(ConnectionFactoryStatics.getCluster());
    }

    private static void configureNodes(String... nodes) {
        ConfigurationFixture.configure(ConfigurationFixture.NODES, Arrays.asList(nodes));
        ConfigurationFixture.configure(ConfigurationFixture.SSL, "false");
        ConfigurationFixture.configure(ConfigurationFixture.AUTHENTICATE, "false");
    }

    private static Cluster clusterConnectingTo(Session session) {
        Cluster cluster = mock(Cluster.class, RETURNS_DEEP_STUBS);
        when(cluster.getConfiguration().getProtocolOptions().getProtocolVersion())
                .thenReturn(ProtocolVersion.V4);
        when(cluster.connect(anyString())).thenReturn(session);
        when(session.getCluster()).thenReturn(cluster);
        return cluster;
    }

    private static List<String> hostAddressesOf(Cluster cluster) {
        List<String> addresses = new ArrayList<>();
        for (InetSocketAddress contactPoint : ConnectionFactoryStatics.contactPointsOf(cluster)) {
            addresses.add(contactPoint.getAddress().getHostAddress());
        }
        return addresses;
    }

    private static List<String> plainTextCredentials(AuthProvider authProvider) {
        Authenticator authenticator = authProvider.newAuthenticator(
                new InetSocketAddress("127.0.0.1", ProtocolOptions.DEFAULT_PORT),
                "org.apache.cassandra.auth.PasswordAuthenticator");
        // SASL PLAIN initial response: a zero byte, the user name, a zero byte, the password.
        String[] parts = new String(authenticator.initialResponse(), StandardCharsets.UTF_8)
                .split("\u0000");
        return Arrays.asList(parts[1], parts[2]);
    }

    private static String localDcOf(DCAwareRoundRobinPolicy policy) {
        return (String) readField(DCAwareRoundRobinPolicy.class, "localDc", policy);
    }

    private static SSLContext sslContextOf(SSLOptions sslOptions) {
        return (SSLContext) readField(JdkSSLOptions.class, "context", sslOptions);
    }

    private static Object readField(Class<?> owner, String name, Object target) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path readableJdkTrustStore() throws IOException, GeneralSecurityException {
        Path cacerts = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        if (!Files.isReadable(cacerts)) {
            throw new SkipException("no readable JDK trust store at " + cacerts);
        }
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream in = Files.newInputStream(cacerts)) {
            keyStore.load(in, CACERTS_PASSWORD.toCharArray());
        } catch (IOException e) {
            throw new SkipException("JDK trust store " + cacerts + " is not a JKS store", e);
        }
        if (keyStore.size() == 0) {
            throw new SkipException("JDK trust store " + cacerts + " holds no trust anchors");
        }
        return cacerts;
    }
}
