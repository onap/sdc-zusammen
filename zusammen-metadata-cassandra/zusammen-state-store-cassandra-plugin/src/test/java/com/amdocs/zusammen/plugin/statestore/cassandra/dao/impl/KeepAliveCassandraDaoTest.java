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
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.plugin.statestore.cassandra.TestUtils;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.MappingManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class KeepAliveCassandraDaoTest {
  private static final String TENANT = "test";
  private static final String USER = "KeepAliveCassandraDaoTest_user";
  private static final SessionContext context =
      TestUtils.createSessionContext(new UserInfo(USER), TENANT);

  private AutoCloseable mocks;

  @Mock
  private CassandraConnector connectorMock;
  @Mock
  private MappingManager mappingManagerMock;
  @Mock
  private KeepAliveCassandraDao.KeepAliveAccessor accessorMock;
  @Mock
  private ResultSet resultSetMock;
  @Mock
  private ColumnDefinitions columnDefinitionsMock;

  private KeepAliveCassandraDao dao;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    CassandraSeam.install(connectorMock);
    when(connectorMock.getMappingManager(any())).thenReturn(mappingManagerMock);
    when(mappingManagerMock.createAccessor(KeepAliveCassandraDao.KeepAliveAccessor.class))
        .thenReturn(accessorMock);
    when(accessorMock.check()).thenReturn(resultSetMock);
    when(resultSetMock.getColumnDefinitions()).thenReturn(columnDefinitionsMock);
    dao = new KeepAliveCassandraDao();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    CassandraSeam.restore();
    mocks.close();
  }

  @Test
  public void testGetWhenElementTableCarriesItemIdColumn() {
    when(columnDefinitionsMock.contains("item_id")).thenReturn(true);

    Assert.assertTrue(dao.get(context));
  }

  @Test
  public void testGetWhenElementTableHasNoItemIdColumn() {
    when(columnDefinitionsMock.contains("item_id")).thenReturn(false);

    Assert.assertFalse(dao.get(context));
  }
}
