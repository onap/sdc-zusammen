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

import com.amdocs.zusammen.commons.db.api.cassandra.types.CassandraContext;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.Session;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CassandraConnectorImplTest {

    private CassandraConnectorImpl connector;
    private Cluster primedCluster;
    private Cluster cluster;

    @BeforeMethod
    public void setUp() {
        ConfigurationFixture.install();
        ConnectionFactoryStatics.prime();
        primedCluster = ConnectionFactoryStatics.getCluster();
        ConnectionFactoryStatics.mappingManagers().clear();
        cluster = mock(Cluster.class, RETURNS_DEEP_STUBS);
        Session session = mock(Session.class, RETURNS_DEEP_STUBS);
        when(cluster.getConfiguration().getProtocolOptions().getProtocolVersion())
                .thenReturn(ProtocolVersion.V4);
        when(cluster.connect(anyString())).thenReturn(session);
        when(session.getCluster()).thenReturn(cluster);
        connector = new CassandraConnectorImpl();
    }

    @AfterMethod
    public void tearDown() {
        ConnectionFactoryStatics.setCluster(primedCluster);
        ConnectionFactoryStatics.mappingManagers().clear();
        ConfigurationFixture.restore();
    }

    @Test
    public void testGetMappingManagerUsesTenantKeyspaceFromContext() {
        ConnectionFactoryStatics.setCluster(cluster);
        CassandraContext context = new CassandraContext();
        context.setTenant("acme");

        Assert.assertNotNull(connector.getMappingManager(context));
        verify(cluster).connect(ConnectionFactoryStatics.PRIMED_KEYSPACE + "_acme");
    }

    @Test
    public void testGetMappingManagerUsesDefaultKeyspaceWhenContextHasNoTenant() {
        ConnectionFactoryStatics.setCluster(cluster);

        Assert.assertNotNull(connector.getMappingManager(new CassandraContext()));
        verify(cluster).connect(ConnectionFactoryStatics.PRIMED_KEYSPACE);
    }

    @Test
    public void testGetConfigurationReturnsClusterConfiguration() {
        ConnectionFactoryStatics.setCluster(cluster);

        Assert.assertSame(connector.getConfiguration(), cluster.getConfiguration());
    }
}
