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

package com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl;

import com.amdocs.zusammen.commons.db.api.cassandra.CassandraConnector;
import com.amdocs.zusammen.commons.db.api.cassandra.types.CassandraContext;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.plugin.statestore.cassandra.TestUtils;
import com.datastax.driver.mapping.MappingManager;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CassandraDaoUtilsTest {
  private static final String TENANT = "CassandraDaoUtilsTest_tenant";
  private static final String USER = "CassandraDaoUtilsTest_user";

  private AutoCloseable mocks;

  @Mock
  private CassandraConnector connectorMock;
  @Mock
  private MappingManager mappingManagerMock;
  @Mock
  private ItemCassandraDao.ItemAccessor itemAccessorMock;
  @Mock
  private KeepAliveCassandraDao.KeepAliveAccessor keepAliveAccessorMock;
  @Captor
  private ArgumentCaptor<CassandraContext> cassandraContextCaptor;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    CassandraSeam.install(connectorMock);
    when(connectorMock.getMappingManager(any())).thenReturn(mappingManagerMock);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    CassandraSeam.restore();
    mocks.close();
  }

  @Test
  public void testGetAccessorDerivesCassandraContextFromSessionTenant() {
    when(mappingManagerMock.createAccessor(ItemCassandraDao.ItemAccessor.class))
        .thenReturn(itemAccessorMock);
    SessionContext context = TestUtils.createSessionContext(new UserInfo(USER), TENANT);

    ItemCassandraDao.ItemAccessor accessor =
        CassandraDaoUtils.getAccessor(context, ItemCassandraDao.ItemAccessor.class);

    Assert.assertSame(accessor, itemAccessorMock);
    verify(connectorMock).getMappingManager(cassandraContextCaptor.capture());
    Assert.assertEquals(cassandraContextCaptor.getValue().getTenant(), TENANT);
  }

  @Test
  public void testGetAccessorReturnsTheAccessorForTheRequestedInterface() {
    when(mappingManagerMock.createAccessor(ItemCassandraDao.ItemAccessor.class))
        .thenReturn(itemAccessorMock);
    when(mappingManagerMock.createAccessor(KeepAliveCassandraDao.KeepAliveAccessor.class))
        .thenReturn(keepAliveAccessorMock);
    SessionContext context = TestUtils.createSessionContext(new UserInfo(USER), TENANT);

    Assert.assertSame(
        CassandraDaoUtils.getAccessor(context, KeepAliveCassandraDao.KeepAliveAccessor.class),
        keepAliveAccessorMock);
    Assert.assertSame(
        CassandraDaoUtils.getAccessor(context, ItemCassandraDao.ItemAccessor.class),
        itemAccessorMock);
  }
}
