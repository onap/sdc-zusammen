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
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Item;
import com.amdocs.zusammen.plugin.statestore.cassandra.TestUtils;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.mapping.MappingManager;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemCassandraDaoTest {
  private static final String TENANT = "test";
  private static final String USER = "ItemCassandraDaoTest_user";
  private static final SessionContext context =
      TestUtils.createSessionContext(new UserInfo(USER), TENANT);
  private static final String ITEM_ID = "item-1";

  private AutoCloseable mocks;

  @Mock
  private CassandraConnector connectorMock;
  @Mock
  private MappingManager mappingManagerMock;
  @Mock
  private ItemCassandraDao.ItemAccessor accessorMock;
  @Mock
  private ResultSet resultSetMock;
  @Captor
  private ArgumentCaptor<String> jsonCaptor;

  private ItemCassandraDao dao;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    CassandraSeam.install(connectorMock);
    when(connectorMock.getMappingManager(any())).thenReturn(mappingManagerMock);
    when(mappingManagerMock.createAccessor(ItemCassandraDao.ItemAccessor.class))
        .thenReturn(accessorMock);
    dao = new ItemCassandraDao();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    CassandraSeam.restore();
    mocks.close();
  }

  @Test
  public void testCreateWritesCreationTimeAsBothTimestamps() {
    Info info = TestUtils.createInfo("created item");
    Date creationTime = new Date(1_000L);

    dao.create(context, new Id(ITEM_ID), info, creationTime);

    verify(accessorMock)
        .create(eq(ITEM_ID), jsonCaptor.capture(), eq(creationTime), eq(creationTime));
    assertInfoJson(jsonCaptor.getValue(), info);
  }

  @Test
  public void testUpdateWritesOnlyModificationTime() {
    Info info = TestUtils.createInfo("updated item");
    Date modificationTime = new Date(2_000L);

    dao.update(context, new Id(ITEM_ID), info, modificationTime);

    verify(accessorMock).update(eq(ITEM_ID), jsonCaptor.capture(), eq(modificationTime));
    assertInfoJson(jsonCaptor.getValue(), info);
  }

  @Test
  public void testUpdateItemModificationTimeDoesNotTouchInfo() {
    Date modificationTime = new Date(3_000L);

    dao.updateItemModificationTime(context, new Id(ITEM_ID), modificationTime);

    verify(accessorMock).updateModificationTime(ITEM_ID, modificationTime);
    verify(accessorMock, never()).update(any(), any(), any());
  }

  @Test
  public void testDelete() {
    dao.delete(context, new Id(ITEM_ID));

    verify(accessorMock).delete(ITEM_ID);
  }

  @Test
  public void testGetMapsEveryColumn() {
    Info info = TestUtils.createInfo("retrieved item");
    Date creationTime = new Date(4_000L);
    Date modificationTime = new Date(5_000L);
    Row row = itemRow(ITEM_ID, info, creationTime, modificationTime);
    when(accessorMock.get(ITEM_ID)).thenReturn(resultSetMock);
    when(resultSetMock.one()).thenReturn(row);

    Optional<Item> item = dao.get(context, new Id(ITEM_ID));

    Assert.assertTrue(item.isPresent());
    Assert.assertEquals(item.get().getId(), new Id(ITEM_ID));
    Assert.assertEquals(item.get().getInfo().getName(), info.getName());
    Assert.assertEquals(item.get().getInfo().getProperties(), info.getProperties());
    Assert.assertEquals(item.get().getCreationTime(), creationTime);
    Assert.assertEquals(item.get().getModificationTime(), modificationTime);
  }

  @Test
  public void testGetWhenItemDoesNotExist() {
    when(accessorMock.get(ITEM_ID)).thenReturn(resultSetMock);
    when(resultSetMock.one()).thenReturn(null);

    Assert.assertEquals(dao.get(context, new Id(ITEM_ID)), Optional.empty());
  }

  @Test
  public void testListMapsEveryRow() {
    Info info1 = TestUtils.createInfo("item1");
    Info info2 = TestUtils.createInfo("item2");
    List<Row> rows = Arrays.asList(
        itemRow("item-1", info1, new Date(10L), new Date(11L)),
        itemRow("item-2", info2, new Date(20L), new Date(21L)));
    when(accessorMock.list()).thenReturn(resultSetMock);
    when(resultSetMock.all()).thenReturn(rows);

    List<Item> items = dao.list(context);

    Assert.assertEquals(items.size(), 2);
    Assert.assertEquals(items.get(0).getId(), new Id("item-1"));
    Assert.assertEquals(items.get(0).getInfo().getName(), info1.getName());
    Assert.assertEquals(items.get(0).getCreationTime(), new Date(10L));
    Assert.assertEquals(items.get(0).getModificationTime(), new Date(11L));
    Assert.assertEquals(items.get(1).getId(), new Id("item-2"));
    Assert.assertEquals(items.get(1).getInfo().getName(), info2.getName());
    Assert.assertEquals(items.get(1).getCreationTime(), new Date(20L));
    Assert.assertEquals(items.get(1).getModificationTime(), new Date(21L));
  }

  @Test
  public void testListWhenNoItemsExist() {
    when(accessorMock.list()).thenReturn(resultSetMock);
    when(resultSetMock.all()).thenReturn(Collections.<Row>emptyList());

    Assert.assertTrue(dao.list(context).isEmpty());
  }

  @Test
  public void testListWhenResultSetYieldsNullRowList() {
    when(accessorMock.list()).thenReturn(resultSetMock);
    when(resultSetMock.all()).thenReturn(null);

    Assert.assertTrue(dao.list(context).isEmpty());
  }

  private Row itemRow(String itemId, Info info, Date creationTime, Date modificationTime) {
    Row row = mock(Row.class);
    when(row.getString("item_id")).thenReturn(itemId);
    when(row.getString("item_info")).thenReturn(JsonUtil.object2Json(info));
    when(row.getTimestamp("creation_time")).thenReturn(creationTime);
    when(row.getTimestamp("modification_time")).thenReturn(modificationTime);
    return row;
  }

  private void assertInfoJson(String json, Info expected) {
    Info actual = JsonUtil.json2Object(json, Info.class);
    Assert.assertEquals(actual.getName(), expected.getName());
    Assert.assertEquals(actual.getProperties(), expected.getProperties());
  }
}
