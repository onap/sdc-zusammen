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

package com.amdocs.zusammen.plugin.statestore.cassandra.dao.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ElementEntityTest {

  @Test
  public void testRelationsAndSubElementIdsDefaultToEmpty() {
    ElementEntity element = new ElementEntity(new Id("element"));

    Assert.assertTrue(element.getRelations().isEmpty());
    Assert.assertTrue(element.getSubElementIds().isEmpty());
    Assert.assertNull(element.getParentId());
    Assert.assertNull(element.getNamespace());
    Assert.assertNull(element.getInfo());
  }

  @Test
  public void testEverySetterIsReadBackBySetGetter() {
    ElementEntity element = new ElementEntity(new Id("original"));
    Namespace namespace = new Namespace(Namespace.ROOT_NAMESPACE, new Id("parent"));
    Info info = new Info();
    info.setName("element name");
    Collection<Relation> relations = Arrays.asList(new Relation(), new Relation());
    Set<Id> subElementIds = new HashSet<>(Arrays.asList(new Id("sub1"), new Id("sub2")));

    element.setId(new Id("replaced"));
    element.setParentId(new Id("parent"));
    element.setNamespace(namespace);
    element.setInfo(info);
    element.setRelations(relations);
    element.setSubElementIds(subElementIds);

    Assert.assertEquals(element.getId(), new Id("replaced"));
    Assert.assertEquals(element.getParentId(), new Id("parent"));
    Assert.assertEquals(element.getNamespace(), namespace);
    Assert.assertSame(element.getInfo(), info);
    Assert.assertSame(element.getRelations(), relations);
    Assert.assertEquals(element.getSubElementIds(), subElementIds);
  }

  @Test
  public void testEqualsComparesTheIdOnly() {
    ElementEntity element = new ElementEntity(new Id("element"));
    element.setParentId(new Id("parent"));
    element.setInfo(new Info());
    ElementEntity sameIdOtherState = new ElementEntity(new Id("element"));

    Assert.assertEquals(element, sameIdOtherState);
    Assert.assertEquals(element.hashCode(), sameIdOtherState.hashCode());
  }

  @Test
  public void testEqualsOfDifferentIds() {
    Assert.assertNotEquals(new ElementEntity(new Id("element1")),
        new ElementEntity(new Id("element2")));
  }

  @Test
  public void testEqualsOfTheSameInstance() {
    ElementEntity element = new ElementEntity(new Id("element"));

    Assert.assertEquals(element, element);
  }

  @Test
  public void testEqualsOfNullAndOfAnotherType() {
    ElementEntity element = new ElementEntity(new Id("element"));

    Assert.assertFalse(element.equals(null));
    Assert.assertFalse(element.equals("element"));
  }

  @Test
  public void testHashCodeIsTheIdHashCode() {
    Assert.assertEquals(new ElementEntity(new Id("element")).hashCode(),
        new Id("element").hashCode());
  }
}
