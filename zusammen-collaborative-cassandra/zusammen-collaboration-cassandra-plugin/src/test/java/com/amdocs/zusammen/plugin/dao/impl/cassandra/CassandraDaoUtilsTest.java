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

import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.VersionDaoImpl.VersionAccessor;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.extras.codecs.enums.EnumNameCodec;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CassandraDaoUtilsTest {

    private static final String TENANT = "CassandraDaoUtilsTest_tenant";

    @BeforeMethod
    public void setUp() {
        CassandraAccessorSeam.install();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        CassandraAccessorSeam.uninstall();
    }

    @Test
    public void testGetAccessorReturnsAccessorForRequestedType() {
        VersionAccessor accessor = Mockito.mock(VersionAccessor.class);
        CassandraAccessorSeam.registerAccessor(VersionAccessor.class, accessor);

        VersionAccessor retrieved =
                CassandraDaoUtils.getAccessor(sessionContext(TENANT), VersionAccessor.class);

        Assert.assertSame(retrieved, accessor);
    }

    @Test
    public void testGetAccessorPassesSessionTenantAsCassandraContextTenant() {
        CassandraAccessorSeam.registerAccessor(VersionAccessor.class, Mockito.mock(VersionAccessor.class));

        CassandraDaoUtils.getAccessor(sessionContext(TENANT), VersionAccessor.class);

        Assert.assertEquals(CassandraAccessorSeam.lastCassandraContext().getTenant(), TENANT);
    }

    @Test
    public void testGetAccessorPassesNullTenantWhenSessionHasNone() {
        CassandraAccessorSeam.registerAccessor(VersionAccessor.class, Mockito.mock(VersionAccessor.class));

        CassandraDaoUtils.getAccessor(sessionContext(null), VersionAccessor.class);

        Assert.assertNull(CassandraAccessorSeam.lastCassandraContext().getTenant());
    }

    @Test
    public void testRegisterCodecsAddsCodecToClusterCodecRegistry() {
        EnumNameCodec<Space> codec = new EnumNameCodec<>(Space.class);

        CassandraDaoUtils.registerCodecs(codec);

        TypeCodec<Space> resolved =
                CassandraAccessorSeam.codecRegistry().codecFor(DataType.varchar(), Space.class);
        Assert.assertSame(resolved, codec);
    }

    private static SessionContext sessionContext(String tenant) {
        SessionContext context = new SessionContext();
        context.setUser(new UserInfo("CassandraDaoUtilsTest_user"));
        context.setTenant(tenant);
        return context;
    }
}
