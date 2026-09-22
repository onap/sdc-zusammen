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
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.plugin.statestore.cassandra.TestUtils;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CassandraElementRepositoryTest {
  private static final String TENANT = "test";
  private static final String USER = "CassandraElementRepositoryTest_user";
  private static final SessionContext context =
      TestUtils.createSessionContext(new UserInfo(USER), TENANT);
  private static final String SPACE = "CassandraElementRepositoryTest_space";
  private static final String ITEM_ID = "item-1";
  private static final String VERSION_ID = "version-1";
  private static final String ELEMENT_ID = "element-1";
  private static final String PARENT_ID = "element-0";

  private final ElementEntityContext elementContext =
      new ElementEntityContext(SPACE, new Id(ITEM_ID), new Id(VERSION_ID));

  private AutoCloseable mocks;

  @Mock
  private CassandraConnector connectorMock;
  @Mock
  private MappingManager mappingManagerMock;
  @Mock
  private CassandraElementRepository.ElementAccessor elementAccessorMock;
  @Mock
  private CassandraElementRepository.ElementNamespaceAccessor namespaceAccessorMock;
  @Mock
  private CassandraElementRepository.VersionElementsAccessor versionElementsAccessorMock;
  @Captor
  private ArgumentCaptor<String> infoJsonCaptor;
  @Captor
  private ArgumentCaptor<String> relationsJsonCaptor;
  @Captor
  private ArgumentCaptor<Set<String>> idSetCaptor;

  private CassandraElementRepository repository;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    CassandraSeam.install(connectorMock);
    when(connectorMock.getMappingManager(any())).thenReturn(mappingManagerMock);
    when(mappingManagerMock.createAccessor(CassandraElementRepository.ElementAccessor.class))
        .thenReturn(elementAccessorMock);
    when(mappingManagerMock
        .createAccessor(CassandraElementRepository.ElementNamespaceAccessor.class))
        .thenReturn(namespaceAccessorMock);
    when(mappingManagerMock
        .createAccessor(CassandraElementRepository.VersionElementsAccessor.class))
        .thenReturn(versionElementsAccessorMock);
    repository = new CassandraElementRepository();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    CassandraSeam.restore();
    mocks.close();
  }

  @Test
  public void testGetMapsEveryColumn() {
    Info info = TestUtils.createInfo("element1");
    List<Relation> relations = Arrays.asList(createRelation("r1"), createRelation("r2"));
    Set<String> subElementIds = new HashSet<>(Arrays.asList("sub-1", "sub-2"));
    stubElementRow(ELEMENT_ID,
        elementRow(PARENT_ID, "item-1/element-0", info, relations, subElementIds));

    Optional<ElementEntity> element =
        repository.get(context, elementContext, new ElementEntity(new Id(ELEMENT_ID)));

    Assert.assertTrue(element.isPresent());
    Assert.assertEquals(element.get().getId(), new Id(ELEMENT_ID));
    Assert.assertEquals(element.get().getParentId(), new Id(PARENT_ID));
    Assert.assertEquals(element.get().getNamespace().getValue(), "item-1/element-0");
    Assert.assertEquals(element.get().getInfo().getName(), info.getName());
    Assert.assertEquals(element.get().getInfo().getProperties(), info.getProperties());
    Assert.assertEquals(relationTypes(element.get().getRelations()),
        Arrays.asList("r1", "r2"));
    Assert.assertEquals(element.get().getSubElementIds(),
        new HashSet<>(Arrays.asList(new Id("sub-1"), new Id("sub-2"))));
  }

  @Test
  public void testGetPassesTheFullPartitionKeyToTheAccessor() {
    stubElementRow(ELEMENT_ID, elementRow(PARENT_ID, "", TestUtils.createInfo("e"),
        Collections.<Relation>emptyList(), Collections.<String>emptySet()));

    repository.get(context, elementContext, new ElementEntity(new Id(ELEMENT_ID)));

    verify(elementAccessorMock).get(SPACE, ITEM_ID, VERSION_ID, ELEMENT_ID);
  }

  @Test
  public void testGetWhenElementDoesNotExist() {
    stubElementRow(ELEMENT_ID, null);

    Assert.assertEquals(
        repository.get(context, elementContext, new ElementEntity(new Id(ELEMENT_ID))),
        Optional.empty());
  }

  @Test
  public void testGetWhenNamespaceInfoAndRelationsColumnsAreNull() {
    stubElementRow(ELEMENT_ID,
        elementRow(PARENT_ID, null, null, null, Collections.<String>emptySet()));

    Optional<ElementEntity> element =
        repository.get(context, elementContext, new ElementEntity(new Id(ELEMENT_ID)));

    Assert.assertTrue(element.isPresent());
    Assert.assertEquals(element.get().getNamespace(), Namespace.ROOT_NAMESPACE);
    Assert.assertNull(element.get().getInfo());
    Assert.assertNull(element.get().getRelations());
  }

  @Test
  public void testGetNamespace() {
    Row row = mock(Row.class);
    when(row.getString("namespace")).thenReturn("item-1/element-0");
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.one()).thenReturn(row);
    when(namespaceAccessorMock.get(ITEM_ID, ELEMENT_ID)).thenReturn(resultSet);

    Optional<Namespace> namespace =
        repository.getNamespace(context, elementContext, new ElementEntity(new Id(ELEMENT_ID)));

    Assert.assertTrue(namespace.isPresent());
    Assert.assertEquals(namespace.get().getValue(), "item-1/element-0");
  }

  @Test
  public void testGetNamespaceWhenColumnIsNull() {
    Row row = mock(Row.class);
    when(row.getString("namespace")).thenReturn(null);
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.one()).thenReturn(row);
    when(namespaceAccessorMock.get(ITEM_ID, ELEMENT_ID)).thenReturn(resultSet);

    Optional<Namespace> namespace =
        repository.getNamespace(context, elementContext, new ElementEntity(new Id(ELEMENT_ID)));

    Assert.assertEquals(namespace.get(), Namespace.ROOT_NAMESPACE);
  }

  @Test
  public void testGetNamespaceWhenElementDoesNotExist() {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.one()).thenReturn(null);
    when(namespaceAccessorMock.get(ITEM_ID, ELEMENT_ID)).thenReturn(resultSet);

    Assert.assertEquals(
        repository.getNamespace(context, elementContext, new ElementEntity(new Id(ELEMENT_ID))),
        Optional.empty());
  }

  @Test
  public void testCreateWritesTheElementRow() {
    ElementEntity element = createElement(ELEMENT_ID, PARENT_ID, "created element", "sub-1");

    repository.create(context, elementContext, element);

    verify(elementAccessorMock).create(eq(SPACE), eq(ITEM_ID), eq(VERSION_ID), eq(ELEMENT_ID),
        eq(PARENT_ID), eq("item-1/element-0"), infoJsonCaptor.capture(),
        relationsJsonCaptor.capture(), idSetCaptor.capture());
    assertInfoJson(infoJsonCaptor.getValue(), element.getInfo());
    Assert.assertEquals(relationTypes(parseRelations(relationsJsonCaptor.getValue())),
        Arrays.asList("r1", "r2"));
    Assert.assertEquals(idSetCaptor.getValue(), Collections.singleton("sub-1"));
  }

  @Test
  public void testCreateWritesTheElementNamespaceRow() {
    ElementEntity element = createElement(ELEMENT_ID, PARENT_ID, "created element");

    repository.create(context, elementContext, element);

    verify(namespaceAccessorMock).create(ITEM_ID, ELEMENT_ID, "item-1/element-0");
  }

  @Test
  public void testCreateAddsTheElementToTheVersionElementIndex() {
    ElementEntity element = createElement(ELEMENT_ID, PARENT_ID, "created element");

    repository.create(context, elementContext, element);

    verify(versionElementsAccessorMock)
        .addElements(Collections.singleton(ELEMENT_ID), SPACE, ITEM_ID, VERSION_ID);
  }

  @Test
  public void testCreateAddsTheElementToItsParentsSubElements() {
    ElementEntity element = createElement(ELEMENT_ID, PARENT_ID, "created element");

    repository.create(context, elementContext, element);

    verify(elementAccessorMock)
        .addSubElements(Collections.singleton(ELEMENT_ID), SPACE, ITEM_ID, VERSION_ID, PARENT_ID);
  }

  @Test
  public void testUpdateWritesOnlyInfoAndRelations() {
    ElementEntity element = createElement(ELEMENT_ID, PARENT_ID, "updated element", "sub-1");

    repository.update(context, elementContext, element);

    verify(elementAccessorMock).update(infoJsonCaptor.capture(), relationsJsonCaptor.capture(),
        eq(SPACE), eq(ITEM_ID), eq(VERSION_ID), eq(ELEMENT_ID));
    assertInfoJson(infoJsonCaptor.getValue(), element.getInfo());
    Assert.assertEquals(relationTypes(parseRelations(relationsJsonCaptor.getValue())),
        Arrays.asList("r1", "r2"));
    verify(namespaceAccessorMock, never()).create(any(), any(), any());
    verify(elementAccessorMock, never()).addSubElements(any(), any(), any(), any(), any());
  }

  @Test
  public void testDeleteRemovesTheRowAndBothIndexEntries() {
    ElementEntity element = createElement(ELEMENT_ID, PARENT_ID, "deleted element");

    repository.delete(context, elementContext, element);

    verify(elementAccessorMock)
        .removeSubElements(Collections.singleton(ELEMENT_ID), SPACE, ITEM_ID, VERSION_ID,
            PARENT_ID);
    verify(elementAccessorMock).delete(SPACE, ITEM_ID, VERSION_ID, ELEMENT_ID);
    verify(versionElementsAccessorMock)
        .removeElements(Collections.singleton(ELEMENT_ID), SPACE, ITEM_ID, VERSION_ID);
  }

  @Test
  public void testDeleteWithoutParentIdSkipsTheParentUpdate() {
    ElementEntity element = createElement(ELEMENT_ID, null, "deleted element");

    repository.delete(context, elementContext, element);

    verify(elementAccessorMock, never()).removeSubElements(any(), any(), any(), any(), any());
    verify(elementAccessorMock).delete(SPACE, ITEM_ID, VERSION_ID, ELEMENT_ID);
    verify(versionElementsAccessorMock)
        .removeElements(Collections.singleton(ELEMENT_ID), SPACE, ITEM_ID, VERSION_ID);
  }

  @Test
  public void testListReturnsEveryElementOfTheVersion() {
    stubVersionElementIds(new HashSet<>(Arrays.asList("element-1", "element-2")));
    stubElementRow("element-1", elementRow(PARENT_ID, "ns-1", TestUtils.createInfo("e1"),
        Arrays.asList(createRelation("r1")), Collections.<String>emptySet()));
    stubElementRow("element-2", elementRow(PARENT_ID, "ns-2", TestUtils.createInfo("e2"),
        Arrays.asList(createRelation("r1")), Collections.<String>emptySet()));

    Collection<ElementEntity> elements = repository.list(context, elementContext);

    Assert.assertEquals(elements.size(), 2);
    Map<String, ElementEntity> byId = new HashMap<>();
    for (ElementEntity element : elements) {
      byId.put(element.getId().getValue(), element);
    }
    Assert.assertEquals(byId.get("element-1").getNamespace().getValue(), "ns-1");
    Assert.assertEquals(byId.get("element-1").getInfo().getName(), "e1");
    Assert.assertEquals(byId.get("element-2").getNamespace().getValue(), "ns-2");
    Assert.assertEquals(byId.get("element-2").getInfo().getName(), "e2");
  }

  @Test
  public void testListWhenTheVersionHasNoElementIndexRow() {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.one()).thenReturn(null);
    when(versionElementsAccessorMock.get(SPACE, ITEM_ID, VERSION_ID)).thenReturn(resultSet);

    Assert.assertTrue(repository.list(context, elementContext).isEmpty());
    verify(elementAccessorMock, never()).get(any(), any(), any(), any());
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testListFailsWhenAnIndexedElementIsMissing() {
    stubVersionElementIds(Collections.singleton("element-1"));
    stubElementRow("element-1", null);

    repository.list(context, elementContext);
  }

  private void stubVersionElementIds(Set<String> elementIds) {
    Row row = mock(Row.class);
    when(row.getSet("element_ids", String.class)).thenReturn(elementIds);
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.one()).thenReturn(row);
    when(versionElementsAccessorMock.get(SPACE, ITEM_ID, VERSION_ID)).thenReturn(resultSet);
  }

  private void stubElementRow(String elementId, Row row) {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.one()).thenReturn(row);
    when(elementAccessorMock.get(SPACE, ITEM_ID, VERSION_ID, elementId)).thenReturn(resultSet);
  }

  private Row elementRow(String parentId, String namespace, Info info,
                         Collection<Relation> relations, Set<String> subElementIds) {
    Row row = mock(Row.class);
    when(row.getString("parent_id")).thenReturn(parentId);
    when(row.getString("namespace")).thenReturn(namespace);
    when(row.getString("info")).thenReturn(info == null ? null : JsonUtil.object2Json(info));
    when(row.getString("relations"))
        .thenReturn(relations == null ? null : JsonUtil.object2Json(relations));
    when(row.getSet("sub_element_ids", String.class)).thenReturn(subElementIds);
    return row;
  }

  private ElementEntity createElement(String elementId, String parentId, String name,
                                      String... subElementIds) {
    ElementEntity element = new ElementEntity(new Id(elementId));
    element.setParentId(parentId == null ? null : new Id(parentId));
    Namespace namespace = new Namespace();
    namespace.setValue("item-1/element-0");
    element.setNamespace(namespace);
    element.setInfo(TestUtils.createInfo(name));
    element.setRelations(Arrays.asList(createRelation("r1"), createRelation("r2")));
    Set<Id> subIds = new HashSet<>();
    for (String subElementId : subElementIds) {
      subIds.add(new Id(subElementId));
    }
    element.setSubElementIds(subIds);
    return element;
  }

  private Relation createRelation(String type) {
    Relation relation = new Relation();
    relation.setType(type);
    return relation;
  }

  private Collection<Relation> parseRelations(String json) {
    return JsonUtil.json2Object(json, new TypeToken<ArrayList<Relation>>() {
    }.getType());
  }

  private List<String> relationTypes(Collection<Relation> relations) {
    List<String> types = new ArrayList<>();
    for (Relation relation : relations) {
      types.add(relation.getType());
    }
    return types;
  }

  private void assertInfoJson(String json, Info expected) {
    Info actual = JsonUtil.json2Object(json, Info.class);
    Assert.assertEquals(actual.getName(), expected.getName());
    Assert.assertEquals(actual.getProperties(), expected.getProperties());
  }
}
