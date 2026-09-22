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
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.plugin.statestore.cassandra.TestUtils;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.mapping.MappingManager;
import com.google.gson.reflect.TypeToken;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VersionCassandraDaoTest {
  private static final String TENANT = "test";
  private static final String USER = "VersionCassandraDaoTest_user";
  private static final SessionContext context =
      TestUtils.createSessionContext(new UserInfo(USER), TENANT);
  private static final String SPACE = "VersionCassandraDaoTest_space";
  private static final String ITEM_ID = "item-1";
  private static final String VERSION_ID = "version-1";
  private static final String BASE_VERSION_ID = "version-0";

  private AutoCloseable mocks;

  @Mock
  private CassandraConnector connectorMock;
  @Mock
  private MappingManager mappingManagerMock;
  @Mock
  private VersionCassandraDao.VersionAccessor accessorMock;
  @Mock
  private ResultSet resultSetMock;
  @Captor
  private ArgumentCaptor<String> infoJsonCaptor;
  @Captor
  private ArgumentCaptor<String> relationsJsonCaptor;

  private VersionCassandraDao dao;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    CassandraSeam.install(connectorMock);
    when(connectorMock.getMappingManager(any())).thenReturn(mappingManagerMock);
    when(mappingManagerMock.createAccessor(VersionCassandraDao.VersionAccessor.class))
        .thenReturn(accessorMock);
    dao = new VersionCassandraDao();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    CassandraSeam.restore();
    mocks.close();
  }

  @Test
  public void testCreateWritesCreationTimeAsBothTimestamps() {
    ItemVersionData data = createVersionData("v1");
    Date creationTime = new Date(1_000L);

    dao.create(context, SPACE, new Id(ITEM_ID), new Id(BASE_VERSION_ID), new Id(VERSION_ID), data,
        creationTime);

    verify(accessorMock).create(eq(SPACE), eq(ITEM_ID), eq(VERSION_ID), eq(BASE_VERSION_ID),
        eq(creationTime), eq(creationTime), infoJsonCaptor.capture(),
        relationsJsonCaptor.capture());
    assertInfoJson(infoJsonCaptor.getValue(), data.getInfo());
    assertRelationsJson(relationsJsonCaptor.getValue(), data.getRelations());
  }

  @Test
  public void testCreateWithoutBaseVersionWritesNullBaseVersionId() {
    ItemVersionData data = createVersionData("v1");
    Date creationTime = new Date(1_000L);

    dao.create(context, SPACE, new Id(ITEM_ID), null, new Id(VERSION_ID), data, creationTime);

    verify(accessorMock).create(eq(SPACE), eq(ITEM_ID), eq(VERSION_ID), isNull(),
        eq(creationTime), eq(creationTime), any(), any());
  }

  @Test
  public void testUpdate() {
    ItemVersionData data = createVersionData("v1 updated");
    Date modificationTime = new Date(2_000L);

    dao.update(context, SPACE, new Id(ITEM_ID), new Id(VERSION_ID), data, modificationTime);

    verify(accessorMock).update(infoJsonCaptor.capture(), relationsJsonCaptor.capture(),
        eq(modificationTime), eq(SPACE), eq(ITEM_ID), eq(VERSION_ID));
    assertInfoJson(infoJsonCaptor.getValue(), data.getInfo());
    assertRelationsJson(relationsJsonCaptor.getValue(), data.getRelations());
  }

  @Test
  public void testUpdateItemVersionModificationTime() {
    Date modificationTime = new Date(3_000L);

    dao.updateItemVersionModificationTime(context, SPACE, new Id(ITEM_ID), new Id(VERSION_ID),
        modificationTime);

    verify(accessorMock).updateModificationTime(modificationTime, SPACE, ITEM_ID, VERSION_ID);
  }

  @Test
  public void testDelete() {
    dao.delete(context, SPACE, new Id(ITEM_ID), new Id(VERSION_ID));

    verify(accessorMock).delete(SPACE, ITEM_ID, VERSION_ID);
  }

  @Test
  public void testGetMapsEveryColumn() {
    ItemVersionData data = createVersionData("v1");
    Date creationTime = new Date(4_000L);
    Date modificationTime = new Date(5_000L);
    Row row = versionRow(VERSION_ID, BASE_VERSION_ID, data, creationTime, modificationTime);
    when(accessorMock.get(SPACE, ITEM_ID, VERSION_ID)).thenReturn(resultSetMock);
    when(resultSetMock.one()).thenReturn(row);

    Optional<ItemVersion> version =
        dao.get(context, SPACE, new Id(ITEM_ID), new Id(VERSION_ID));

    Assert.assertTrue(version.isPresent());
    Assert.assertEquals(version.get().getId(), new Id(VERSION_ID));
    Assert.assertEquals(version.get().getBaseId(), new Id(BASE_VERSION_ID));
    Assert.assertEquals(version.get().getCreationTime(), creationTime);
    Assert.assertEquals(version.get().getModificationTime(), modificationTime);
    Assert.assertEquals(version.get().getData().getInfo().getName(), data.getInfo().getName());
    Assert.assertEquals(version.get().getData().getInfo().getProperties(),
        data.getInfo().getProperties());
    assertRelationTypes(version.get().getData().getRelations(), data.getRelations());
  }

  @Test
  public void testGetWhenItemVersionDoesNotExist() {
    when(accessorMock.get(SPACE, ITEM_ID, VERSION_ID)).thenReturn(resultSetMock);
    when(resultSetMock.one()).thenReturn(null);

    Assert.assertEquals(dao.get(context, SPACE, new Id(ITEM_ID), new Id(VERSION_ID)),
        Optional.empty());
  }

  @Test
  public void testListMapsEveryRow() {
    ItemVersionData data1 = createVersionData("v1");
    ItemVersionData data2 = createVersionData("v2");
    List<Row> rows = Arrays.asList(
        versionRow("version-1", null, data1, new Date(10L), new Date(11L)),
        versionRow("version-2", "version-1", data2, new Date(20L), new Date(21L)));
    when(accessorMock.list(SPACE, ITEM_ID)).thenReturn(resultSetMock);
    when(resultSetMock.all()).thenReturn(rows);

    List<ItemVersion> versions =
        new ArrayList<>(dao.list(context, SPACE, new Id(ITEM_ID)));

    Assert.assertEquals(versions.size(), 2);
    Assert.assertEquals(versions.get(0).getId(), new Id("version-1"));
    Assert.assertEquals(versions.get(0).getData().getInfo().getName(), data1.getInfo().getName());
    Assert.assertEquals(versions.get(0).getCreationTime(), new Date(10L));
    Assert.assertEquals(versions.get(0).getModificationTime(), new Date(11L));
    Assert.assertEquals(versions.get(1).getId(), new Id("version-2"));
    Assert.assertEquals(versions.get(1).getBaseId(), new Id("version-1"));
    Assert.assertEquals(versions.get(1).getData().getInfo().getName(), data2.getInfo().getName());
  }

  @Test
  public void testListWhenItemHasNoVersions() {
    when(accessorMock.list(SPACE, ITEM_ID)).thenReturn(resultSetMock);
    when(resultSetMock.all()).thenReturn(Collections.<Row>emptyList());

    Assert.assertTrue(dao.list(context, SPACE, new Id(ITEM_ID)).isEmpty());
  }

  @Test
  public void testListWhenResultSetYieldsNullRowList() {
    when(accessorMock.list(SPACE, ITEM_ID)).thenReturn(resultSetMock);
    when(resultSetMock.all()).thenReturn(null);

    Assert.assertTrue(dao.list(context, SPACE, new Id(ITEM_ID)).isEmpty());
  }

  private Row versionRow(String versionId, String baseVersionId, ItemVersionData data,
                         Date creationTime, Date modificationTime) {
    Row row = mock(Row.class);
    when(row.getString("version_id")).thenReturn(versionId);
    when(row.getString("base_version_id")).thenReturn(baseVersionId);
    when(row.getString("info")).thenReturn(JsonUtil.object2Json(data.getInfo()));
    when(row.getString("relations")).thenReturn(JsonUtil.object2Json(data.getRelations()));
    when(row.getTimestamp("creation_time")).thenReturn(creationTime);
    when(row.getTimestamp("modification_time")).thenReturn(modificationTime);
    return row;
  }

  private ItemVersionData createVersionData(String name) {
    ItemVersionData data = new ItemVersionData();
    data.setInfo(TestUtils.createInfo(name));
    data.setRelations(Arrays.asList(createRelation(name + "_r1"), createRelation(name + "_r2")));
    return data;
  }

  private Relation createRelation(String type) {
    Relation relation = new Relation();
    relation.setType(type);
    return relation;
  }

  private void assertInfoJson(String json, Info expected) {
    Info actual = JsonUtil.json2Object(json, Info.class);
    Assert.assertEquals(actual.getName(), expected.getName());
    Assert.assertEquals(actual.getProperties(), expected.getProperties());
  }

  private void assertRelationsJson(String json, Collection<Relation> expected) {
    Collection<Relation> actual =
        JsonUtil.json2Object(json, new TypeToken<ArrayList<Relation>>() {
        }.getType());
    assertRelationTypes(actual, expected);
  }

  private void assertRelationTypes(Collection<Relation> actual, Collection<Relation> expected) {
    Assert.assertEquals(relationTypes(actual), relationTypes(expected));
  }

  private List<String> relationTypes(Collection<Relation> relations) {
    List<String> types = new ArrayList<>();
    for (Relation relation : relations) {
      types.add(relation.getType());
    }
    return types;
  }
}
