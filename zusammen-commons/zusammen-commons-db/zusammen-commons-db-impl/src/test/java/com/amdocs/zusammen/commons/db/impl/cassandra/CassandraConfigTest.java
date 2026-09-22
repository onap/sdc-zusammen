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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class CassandraConfigTest {

    @BeforeMethod
    public void setUp() {
        ConfigurationFixture.install();
    }

    @AfterMethod
    public void tearDown() {
        ConfigurationFixture.restore();
    }

    @Test
    public void testGetNodesFromCommaSeparatedString() {
        ConfigurationFixture.configure(ConfigurationFixture.NODES, "10.0.0.1,10.0.0.2,10.0.0.3");

        Assert.assertEquals(CassandraConfig.getNodes(),
                new String[] {"10.0.0.1", "10.0.0.2", "10.0.0.3"});
    }

    @Test
    public void testGetNodesFromSingleNode() {
        ConfigurationFixture.configure(ConfigurationFixture.NODES, "cassandra.onap");

        Assert.assertEquals(CassandraConfig.getNodes(), new String[] {"cassandra.onap"});
    }

    @Test
    public void testGetNodesFromList() {
        ConfigurationFixture.configure(ConfigurationFixture.NODES,
                Arrays.asList("10.0.0.1", "10.0.0.2"));

        Assert.assertEquals(CassandraConfig.getNodes(), new String[] {"10.0.0.1", "10.0.0.2"});
    }

    @Test
    public void testGetNodesFromEmptyList() {
        ConfigurationFixture.configure(ConfigurationFixture.NODES, Collections.emptyList());

        Assert.assertEquals(CassandraConfig.getNodes(), new String[0]);
    }

    @Test
    public void testGetNodesPrefersSystemProperty() {
        ConfigurationFixture.configure(ConfigurationFixture.NODES,
                Collections.singletonList("from.json"));
        System.setProperty(ConfigurationFixture.NODES, "from.system,also.from.system");

        Assert.assertEquals(CassandraConfig.getNodes(),
                new String[] {"from.system", "also.from.system"});
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetNodesFailsWhenAbsent() {
        CassandraConfig.getNodes();
    }

    @Test
    public void testGetKeyspace() {
        ConfigurationFixture.configure(ConfigurationFixture.KEYSPACE, "zusammen");

        Assert.assertEquals(CassandraConfig.getKeyspace(), "zusammen");
    }

    @Test
    public void testGetKeyspacePrefersSystemProperty() {
        ConfigurationFixture.configure(ConfigurationFixture.KEYSPACE, "from_json");
        System.setProperty(ConfigurationFixture.KEYSPACE, "from_system");

        Assert.assertEquals(CassandraConfig.getKeyspace(), "from_system");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetKeyspaceFailsWhenAbsent() {
        CassandraConfig.getKeyspace();
    }

    @Test
    public void testGetUser() {
        ConfigurationFixture.configure(ConfigurationFixture.USER, "zusammen_user");

        Assert.assertEquals(CassandraConfig.getUser(), "zusammen_user");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetUserFailsWhenAbsent() {
        CassandraConfig.getUser();
    }

    @Test
    public void testGetPassword() {
        ConfigurationFixture.configure(ConfigurationFixture.PASSWORD, "s3cret");

        Assert.assertEquals(CassandraConfig.getPassword(), "s3cret");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetPasswordFailsWhenAbsent() {
        CassandraConfig.getPassword();
    }

    @Test
    public void testIsAuthenticateFromStringTrue() {
        ConfigurationFixture.configure(ConfigurationFixture.AUTHENTICATE, "true");

        Assert.assertTrue(CassandraConfig.isAuthenticate());
    }

    @Test
    public void testIsAuthenticateFromStringFalse() {
        ConfigurationFixture.configure(ConfigurationFixture.AUTHENTICATE, "false");

        Assert.assertFalse(CassandraConfig.isAuthenticate());
    }

    @Test
    public void testIsAuthenticateFromJsonBoolean() {
        ConfigurationFixture.configure(ConfigurationFixture.AUTHENTICATE, Boolean.TRUE);

        Assert.assertTrue(CassandraConfig.isAuthenticate());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testIsAuthenticateFailsWhenAbsent() {
        CassandraConfig.isAuthenticate();
    }

    @Test
    public void testIsSslFromStringTrue() {
        ConfigurationFixture.configure(ConfigurationFixture.SSL, "true");

        Assert.assertTrue(CassandraConfig.isSsl());
    }

    @Test
    public void testIsSslFromJsonBooleanFalse() {
        ConfigurationFixture.configure(ConfigurationFixture.SSL, Boolean.FALSE);

        Assert.assertFalse(CassandraConfig.isSsl());
    }

    @Test
    public void testIsSslPrefersSystemProperty() {
        ConfigurationFixture.configure(ConfigurationFixture.SSL, Boolean.TRUE);
        System.setProperty(ConfigurationFixture.SSL, "false");

        Assert.assertFalse(CassandraConfig.isSsl());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testIsSslFailsWhenAbsent() {
        CassandraConfig.isSsl();
    }

    @Test
    public void testGetPort() {
        ConfigurationFixture.configure(ConfigurationFixture.PORT, "19042");

        Assert.assertEquals(CassandraConfig.getPort(), Optional.of(19042));
    }

    @Test
    public void testGetPortPrefersSystemProperty() {
        ConfigurationFixture.configure(ConfigurationFixture.PORT, "19042");
        System.setProperty(ConfigurationFixture.PORT, "29042");

        Assert.assertEquals(CassandraConfig.getPort(), Optional.of(29042));
    }

    @Test
    public void testGetPortIsEmptyWhenAbsent() {
        Assert.assertEquals(CassandraConfig.getPort(), Optional.empty());
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testGetPortRejectsNonNumericValue() {
        ConfigurationFixture.configure(ConfigurationFixture.PORT, "9042a");

        CassandraConfig.getPort();
    }

    @Test
    public void testGetReconnectionDelay() {
        ConfigurationFixture.configure(ConfigurationFixture.RECONNECT_DELAY, "5000");

        Assert.assertEquals(CassandraConfig.getReconnectionDelay(), Optional.of(5000L));
    }

    @Test
    public void testGetReconnectionDelayIsEmptyWhenAbsent() {
        Assert.assertEquals(CassandraConfig.getReconnectionDelay(), Optional.empty());
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testGetReconnectionDelayRejectsNonNumericValue() {
        ConfigurationFixture.configure(ConfigurationFixture.RECONNECT_DELAY, "5 seconds");

        CassandraConfig.getReconnectionDelay();
    }

    @Test
    public void testGetTrustStore() {
        ConfigurationFixture.configure(ConfigurationFixture.TRUST_STORE, "/etc/zusammen/truststore");

        Assert.assertEquals(CassandraConfig.getTrustStore(),
                Optional.of("/etc/zusammen/truststore"));
    }

    @Test
    public void testGetTrustStoreIsEmptyWhenAbsent() {
        Assert.assertEquals(CassandraConfig.getTrustStore(), Optional.empty());
    }

    @Test
    public void testGetTrustStorePassword() {
        ConfigurationFixture.configure(ConfigurationFixture.TRUST_STORE_PASSWORD, "changeit");

        Assert.assertEquals(CassandraConfig.getTrustStorePassword(), Optional.of("changeit"));
    }

    @Test
    public void testGetTrustStorePasswordIsEmptyWhenAbsent() {
        Assert.assertEquals(CassandraConfig.getTrustStorePassword(), Optional.empty());
    }

    @Test
    public void testGetDataCenter() {
        ConfigurationFixture.configure(ConfigurationFixture.DATA_CENTER, "dc1");

        Assert.assertEquals(CassandraConfig.getDataCenter(), Optional.of("dc1"));
    }

    @Test
    public void testGetDataCenterIsEmptyWhenAbsent() {
        Assert.assertEquals(CassandraConfig.getDataCenter(), Optional.empty());
    }

    @Test
    public void testGetConsistencyLevel() {
        ConfigurationFixture.configure(ConfigurationFixture.CONSISTENCY_LEVEL, "LOCAL_QUORUM");

        Assert.assertEquals(CassandraConfig.getConsistencyLevel(), Optional.of("LOCAL_QUORUM"));
    }

    @Test
    public void testGetConsistencyLevelIsEmptyWhenAbsent() {
        Assert.assertEquals(CassandraConfig.getConsistencyLevel(), Optional.empty());
    }
}
