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

package com.amdocs.zusammen.plugin.statestore.cassandra;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.sdk.state.types.StateElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class StateStoreUtilTest {
  private static final String TENANT = "test";
  private static final String USER = "StateStoreUtilTest_user";
  private static final SessionContext context =
      TestUtils.createSessionContext(new UserInfo(USER), TENANT);

  @Test
  public void testGetSpaceNameOfPublicSpace() {
    Assert.assertEquals(StateStoreUtil.getSpaceName(context, Space.PUBLIC),
        StateStoreConstants.PUBLIC_SPACE);
  }

  @Test
  public void testGetSpaceNameOfPrivateSpaceIsTheUserName() {
    Assert.assertEquals(StateStoreUtil.getSpaceName(context, Space.PRIVATE), USER);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetSpaceNameOfBothSpacesIsRejected() {
    StateStoreUtil.getSpaceName(context, Space.BOTH);
  }

  @Test
  public void testGetPrivateSpaceNameIsTheUserName() {
    Assert.assertEquals(StateStoreUtil.getPrivateSpaceName(context), USER);
  }

  @Test
  public void testGetElementEntityCopiesEveryField() {
    Id parentId = new Id("parent");
    Namespace namespace = new Namespace(Namespace.ROOT_NAMESPACE, parentId);
    StateElement element =
        new StateElement(new Id("item"), new Id("version"), namespace, new Id("element"));
    element.setParentId(parentId);
    Info info = TestUtils.createInfo("element");
    element.setInfo(info);
    Collection<Relation> relations = Arrays.asList(createRelation("r1"), createRelation("r2"));
    element.setRelations(relations);

    ElementEntity elementEntity = StateStoreUtil.getElementEntity(element);

    Assert.assertEquals(elementEntity.getId(), new Id("element"));
    Assert.assertEquals(elementEntity.getParentId(), parentId);
    Assert.assertEquals(elementEntity.getNamespace(), namespace);
    Assert.assertSame(elementEntity.getInfo(), info);
    Assert.assertSame(elementEntity.getRelations(), relations);
  }

  @Test
  public void testGetElementEntityOfRootElementUsesTheRootParentId() {
    StateElement element = new StateElement(new Id("item"), new Id("version"),
        Namespace.ROOT_NAMESPACE, new Id("element"));

    ElementEntity elementEntity = StateStoreUtil.getElementEntity(element);

    Assert.assertEquals(elementEntity.getParentId(), StateStoreConstants.ROOT_ELEMENTS_PARENT_ID);
  }

  @Test
  public void testGetStateElementCopiesEveryField() {
    ElementEntityContext elementEntityContext =
        new ElementEntityContext(USER, new Id("item"), new Id("version"));
    ElementEntity elementEntity = new ElementEntity(new Id("element"));
    elementEntity.setParentId(new Id("parent"));
    Namespace namespace = new Namespace(Namespace.ROOT_NAMESPACE, new Id("parent"));
    elementEntity.setNamespace(namespace);
    Info info = TestUtils.createInfo("element");
    elementEntity.setInfo(info);
    Collection<Relation> relations = Arrays.asList(createRelation("r1"), createRelation("r2"));
    elementEntity.setRelations(relations);
    Set<Id> subElementIds = new HashSet<>(Arrays.asList(new Id("sub1"), new Id("sub2")));
    elementEntity.setSubElementIds(subElementIds);

    StateElement element = StateStoreUtil.getStateElement(elementEntityContext, elementEntity);

    Assert.assertEquals(element.getItemId(), new Id("item"));
    Assert.assertEquals(element.getVersionId(), new Id("version"));
    Assert.assertEquals(element.getId(), new Id("element"));
    Assert.assertEquals(element.getParentId(), new Id("parent"));
    Assert.assertEquals(element.getNamespace(), namespace);
    Assert.assertSame(element.getInfo(), info);
    Assert.assertSame(element.getRelations(), relations);
    Assert.assertEquals(element.getSubElements(), subElementIds);
  }

  @Test
  public void testGetStateElementOfRootElementHasNoParentId() {
    ElementEntityContext elementEntityContext =
        new ElementEntityContext(USER, new Id("item"), new Id("version"));
    ElementEntity elementEntity = new ElementEntity(new Id("element"));
    elementEntity.setParentId(StateStoreConstants.ROOT_ELEMENTS_PARENT_ID);
    elementEntity.setNamespace(Namespace.ROOT_NAMESPACE);

    StateElement element = StateStoreUtil.getStateElement(elementEntityContext, elementEntity);

    Assert.assertNull(element.getParentId());
  }

  private Relation createRelation(String type) {
    Relation relation = new Relation();
    relation.setType(type);
    return relation;
  }
}
