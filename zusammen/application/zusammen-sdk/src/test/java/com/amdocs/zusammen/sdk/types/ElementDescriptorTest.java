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

package com.amdocs.zusammen.sdk.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ElementDescriptorTest {

  private static final Id ITEM_ID = new Id("item-1");
  private static final Id VERSION_ID = new Id("version-2");
  private static final Id ELEMENT_ID = new Id("element-3");
  private static final Namespace NAMESPACE = new Namespace(new Namespace(), new Id("parent-4"));

  @Test
  public void testConstructorPopulatesIdentity() {
    ElementDescriptor descriptor =
        new ElementDescriptor(ITEM_ID, VERSION_ID, NAMESPACE, ELEMENT_ID);

    Assert.assertEquals(descriptor.getItemId(), ITEM_ID);
    Assert.assertEquals(descriptor.getVersionId(), VERSION_ID);
    Assert.assertEquals(descriptor.getNamespace(), NAMESPACE);
    Assert.assertEquals(descriptor.getId(), ELEMENT_ID);
  }

  @Test
  public void testConstructorAcceptsNullIdentity() {
    ElementDescriptor descriptor = new ElementDescriptor(null, null, null, null);

    Assert.assertNull(descriptor.getItemId());
    Assert.assertNull(descriptor.getVersionId());
    Assert.assertNull(descriptor.getNamespace());
    Assert.assertNull(descriptor.getId());
  }

  @Test
  public void testOptionalPropertiesDefaultToUnset() {
    ElementDescriptor descriptor =
        new ElementDescriptor(ITEM_ID, VERSION_ID, NAMESPACE, ELEMENT_ID);

    Assert.assertNull(descriptor.getParentId());
    Assert.assertNull(descriptor.getInfo());
    Assert.assertNull(descriptor.getRelations());
    Assert.assertTrue(descriptor.getSubElements().isEmpty());
  }

  @Test
  public void testSetParentId() {
    ElementDescriptor descriptor =
        new ElementDescriptor(ITEM_ID, VERSION_ID, NAMESPACE, ELEMENT_ID);
    Id parentId = new Id("parent-5");

    descriptor.setParentId(parentId);

    Assert.assertEquals(descriptor.getParentId(), parentId);
  }

  @Test
  public void testSetInfo() {
    ElementDescriptor descriptor =
        new ElementDescriptor(ITEM_ID, VERSION_ID, NAMESPACE, ELEMENT_ID);
    Info info = new Info();
    info.setName("element name");

    descriptor.setInfo(info);

    Assert.assertEquals(descriptor.getInfo().getName(), "element name");
  }

  @Test
  public void testSetRelationsKeepsCallerCollection() {
    ElementDescriptor descriptor =
        new ElementDescriptor(ITEM_ID, VERSION_ID, NAMESPACE, ELEMENT_ID);
    Relation relation = new Relation();
    relation.setType("depends-on");
    Collection<Relation> relations = Arrays.asList(relation);

    descriptor.setRelations(relations);

    Assert.assertSame(descriptor.getRelations(), relations);
    Assert.assertEquals(descriptor.getRelations().iterator().next().getType(), "depends-on");
  }

  @Test
  public void testSetSubElementsKeepsCallerCollection() {
    ElementDescriptor descriptor =
        new ElementDescriptor(ITEM_ID, VERSION_ID, NAMESPACE, ELEMENT_ID);
    Set<Id> subElements = new HashSet<>();
    subElements.add(new Id("sub-6"));

    descriptor.setSubElements(subElements);
    subElements.add(new Id("sub-7"));

    Assert.assertSame(descriptor.getSubElements(), subElements);
    Assert.assertEquals(descriptor.getSubElements().size(), 2);
  }

  @Test
  public void testSetEmptySubElements() {
    ElementDescriptor descriptor =
        new ElementDescriptor(ITEM_ID, VERSION_ID, NAMESPACE, ELEMENT_ID);

    descriptor.setSubElements(Collections.<Id>emptySet());

    Assert.assertTrue(descriptor.getSubElements().isEmpty());
  }
}
