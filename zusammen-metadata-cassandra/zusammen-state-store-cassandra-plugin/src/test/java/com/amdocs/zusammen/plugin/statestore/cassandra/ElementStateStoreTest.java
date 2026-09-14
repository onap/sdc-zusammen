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

package com.amdocs.zusammen.plugin.statestore.cassandra;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ElementRepository;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.sdk.state.types.StateElement;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElementStateStoreTest {
  private static final String TENANT = "test";

  private static final String USER = "ElementStateStoreTest_user";
  private static final SessionContext context =
      TestUtils.createSessionContext(new UserInfo(USER), TENANT);
  private static final ElementContext elementContext =
      TestUtils.createElementContext(new Id(), new Id());

  @Spy
  private ElementStateStore elementStateStore;
  @Mock
  private ElementRepository elementRepositoryMock;
  @Captor
  private ArgumentCaptor<ElementEntity> elementEntityCaptor;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(elementStateStore.getElementRepository(anyObject())).thenReturn(elementRepositoryMock);
  }

  @Test
  public void testListRootElements() throws Exception {
    testListElements(null, StateStoreConstants.ROOT_ELEMENTS_PARENT_ID);
  }

  @Test
  public void testListElements() throws Exception {
    Id elementId = new Id();
    testListElements(elementId, elementId);
  }

  @Test
  public void testIsElementExist() throws Exception {
    ElementEntity retrievedElement = getRetrievedElement(new Id(), new Id(), "elm1");

    doReturn(Optional.of(retrievedElement))
        .when(elementRepositoryMock).get(anyObject(), anyObject(), anyObject());


    boolean elementExist =
        elementStateStore.isElementExist(context, elementContext, retrievedElement.getId());

    Assert.assertTrue(elementExist);
  }

  @Test
  public void testIsElementExistWhenNot() throws Exception {
    doReturn(Optional.empty())
        .when(elementRepositoryMock).get(anyObject(), anyObject(), anyObject());

    ElementContext elementContext = TestUtils.createElementContext(new Id(), new Id());
    boolean elementExist = elementStateStore.isElementExist(context, elementContext, new Id());

    Assert.assertFalse(elementExist);
  }

  @Test
  public void testGetElementNamespace() throws Exception {
    Namespace retrievedNamespace = new Namespace(Namespace.ROOT_NAMESPACE, new Id());
    doReturn(Optional.of(retrievedNamespace))
        .when(elementRepositoryMock).getNamespace(anyObject(), anyObject(), anyObject());

    Namespace namespace =
        elementStateStore.getElementNamespace(context, elementContext.getItemId(), new Id());

    Assert.assertEquals(namespace, retrievedNamespace);
  }

  @Test
  public void testGetNonExistingElementNamespace() throws Exception {
    doReturn(Optional.empty())
        .when(elementRepositoryMock).getNamespace(anyObject(), anyObject(), anyObject());

    Namespace namespace =
        elementStateStore.getElementNamespace(context, elementContext.getItemId(), new Id());

    Assert.assertNull(namespace);
  }

  @Test
  public void testGetElement() throws Exception {
    ElementEntity retrievedElement = getRetrievedElement(new Id(), new Id(), "elm1");

    doReturn(Optional.of(retrievedElement))
        .when(elementRepositoryMock).get(anyObject(), anyObject(), anyObject());

    ElementContext elementContext = TestUtils.createElementContext(new Id(), new Id());
    StateElement element =
        elementStateStore.getElement(context, elementContext, retrievedElement.getId());

    Assert.assertNotNull(element);
    assertElementEquals(retrievedElement, element);
  }

  @Test
  public void testGetNonExistingElement() throws Exception {
    doReturn(Optional.empty())
        .when(elementRepositoryMock).get(anyObject(), anyObject(), anyObject());

    ElementContext elementContext = TestUtils.createElementContext(new Id(), new Id());
    StateElement element = elementStateStore.getElement(context, elementContext, new Id());

    Assert.assertNull(element);
  }

  @Test
  public void testCreateElement() throws Exception {
    Id parentId = new Id();
    StateElement element =
        new StateElement(new Id(), new Id(), new Namespace(Namespace.ROOT_NAMESPACE, parentId),
            new Id());
    element.setParentId(parentId);
    element.setInfo(TestUtils.createInfo("elm1"));
    element.setRelations(Arrays.asList(createRelation("r1"), createRelation("r2")));

    elementStateStore.createElement(context, element);

    verify(elementRepositoryMock).create(anyObject(), anyObject(), elementEntityCaptor.capture());
    assertElementEquals(elementEntityCaptor.getValue(), element);
  }

  @Test
  public void testUpdateElement() throws Exception {
    Id parentId = new Id();
    ElementEntity retrievedElement = getRetrievedElement(new Id(), parentId, "elm1");
    doReturn(Optional.of(retrievedElement))
        .when(elementRepositoryMock).get(anyObject(), anyObject(), anyObject());

    StateElement element =
        new StateElement(new Id(), new Id(), new Namespace(Namespace.ROOT_NAMESPACE, parentId),
            retrievedElement.getId());
    element.setParentId(parentId);
    element.setInfo(TestUtils.createInfo("elm1 updated"));
    element.setRelations(Arrays.asList(createRelation("r1"), createRelation("r2")));

    elementStateStore.updateElement(context, element);

    verify(elementRepositoryMock).update(anyObject(), anyObject(), elementEntityCaptor.capture());
    assertElementEquals(elementEntityCaptor.getValue(), element);
  }

  @Test
  public void testDeleteElement() throws Exception {
    ElementEntity retrievedElement =
        getRetrievedElement(new Id(), new Id(), "elm1", new Id(), new Id());
    StateElement element =
        new StateElement(new Id(), new Id(), Namespace.ROOT_NAMESPACE, retrievedElement.getId());
    element.setParentId(retrievedElement.getParentId());
    ElementEntityContext elementEntityContext =
        new ElementEntityContext(USER, element.getItemId(), element.getVersionId());
    doReturn(Optional.of(retrievedElement))
        .when(elementRepositoryMock)
        .get(eq(context), eq(elementEntityContext), eq(retrievedElement));

    retrievedElement.getSubElementIds().stream()
        .map(ElementEntity::new)
        .forEach(reqForRetrieveSubElement -> {
          ElementEntity retrievedSubElement = new ElementEntity(reqForRetrieveSubElement.getId());
          retrievedSubElement.setParentId(retrievedElement.getId());
          doReturn(Optional.of(retrievedSubElement))
              .when(elementRepositoryMock)
              .get(eq(context), eq(elementEntityContext), eq(reqForRetrieveSubElement));
        });

    elementStateStore.deleteElement(context, element);

    verify(elementRepositoryMock, times(3)).delete(anyObject(), anyObject(), anyObject());
    verify(elementRepositoryMock)
        .delete(eq(context), eq(elementEntityContext), eq(retrievedElement));
    retrievedElement.getSubElementIds().stream()
        .map(ElementEntity::new)
        .forEach(subElement -> verify(elementRepositoryMock)
            .delete(eq(context), eq(elementEntityContext), eq(subElement)));
  }

  private void testListElements(Id requestedElementId, Id stateStoreElementId) {
    ElementEntity elm1 =
        getRetrievedElement(new Id(), stateStoreElementId, "elm1");
    ElementEntity elm2 =
        getRetrievedElement(new Id(), stateStoreElementId, "elm2");
    ElementEntity parentElm =
        getRetrievedElement(stateStoreElementId, null, null, elm1.getId(),
            elm2.getId());
    doReturn(Optional.of(parentElm)).when(elementRepositoryMock)
        .get(eq(context), eq(new ElementEntityContext(USER, elementContext)), eq(parentElm));
    doReturn(Optional.of(elm1)).when(elementRepositoryMock)
        .get(eq(context), eq(new ElementEntityContext(USER, elementContext)), eq(elm1));
    doReturn(Optional.of(elm2)).when(elementRepositoryMock)
        .get(eq(context), eq(new ElementEntityContext(USER, elementContext)), eq(elm2));

    Collection<StateElement> elements =
        elementStateStore.listElements(context, elementContext, requestedElementId);

    Assert.assertEquals(elements.size(), 2);

    elm1.setParentId(requestedElementId);
    elm2.setParentId(requestedElementId);

    int foundElementCounter = 0;
    for (StateElement element : elements) {
      if (element.getId().equals(elm1.getId())) {
        assertElementEquals(elm1, element);
        foundElementCounter++;
      } else if (element.getId().equals(elm2.getId())) {
        assertElementEquals(elm2, element);
        foundElementCounter++;
      }
    }
    Assert.assertEquals(foundElementCounter, 2);
  }

  private void assertElementEquals(ElementEntity expected, StateElement actual) {
    Assert.assertEquals(actual.getId(), expected.getId());
    Assert.assertEquals(actual.getParentId(), expected.getParentId());
    Assert.assertEquals(actual.getNamespace(), expected.getNamespace());
    Assert.assertEquals(actual.getInfo(), expected.getInfo());
    Assert.assertEquals(actual.getRelations(), expected.getRelations());
    Assert
        .assertEquals(actual.getSubElements().size(), expected.getSubElementIds().size());
    actual.getSubElements()
        .forEach(subElement -> expected.getSubElementIds().contains(subElement));
  }

  private ElementEntity getRetrievedElement(Id id, Id parentId, String name, Id... subIds) {
    ElementEntity elementEntity = new ElementEntity(id);
    elementEntity.setParentId(parentId);
    elementEntity.setNamespace(parentId == null ? new Namespace() : new Namespace(new Namespace()
        , parentId));
    elementEntity.setInfo(TestUtils.createInfo(name));
    elementEntity.setRelations(Arrays.asList(createRelation("r1"), createRelation("r2")));
    if (subIds != null) {
      elementEntity.setSubElementIds(new HashSet<>());
      for (Id subId : subIds) {
        elementEntity.getSubElementIds().add(subId);
      }
    }
    return elementEntity;
  }

  private Relation createRelation(String relationName) {
    Relation relation = new Relation();
    relation.setInfo(TestUtils.createInfo(relationName));
    return relation;
  }

}