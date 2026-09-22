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

package com.amdocs.zusammen.sdk.state.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.Info;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

public class StateElementTest {

  private static final Id ITEM_ID = new Id("item-1");
  private static final Id VERSION_ID = new Id("version-2");
  private static final Id ELEMENT_ID = new Id("element-3");

  @Test
  public void testConstructorPopulatesIdentity() {
    Namespace namespace = new Namespace(new Namespace(), new Id("parent-4"));
    StateElement element = new StateElement(ITEM_ID, VERSION_ID, namespace, ELEMENT_ID);

    Assert.assertEquals(element.getItemId(), ITEM_ID);
    Assert.assertEquals(element.getVersionId(), VERSION_ID);
    Assert.assertEquals(element.getNamespace(), namespace);
    Assert.assertEquals(element.getId(), ELEMENT_ID);
  }

  @Test
  public void testSpaceDefaultsToPrivate() {
    StateElement element = newElement();

    Assert.assertEquals(element.getSpace(), Space.PRIVATE);
  }

  @Test
  public void testSetSpace() {
    StateElement element = newElement();

    element.setSpace(Space.PUBLIC);

    Assert.assertEquals(element.getSpace(), Space.PUBLIC);
  }

  @Test
  public void testInheritedDescriptorPropertiesAreSettable() {
    StateElement element = newElement();
    Info info = new Info();
    info.setDescription("state element");
    Set<Id> subElements = new HashSet<>();
    subElements.add(new Id("sub-5"));

    element.setParentId(new Id("parent-6"));
    element.setInfo(info);
    element.setSubElements(subElements);

    Assert.assertEquals(element.getParentId().getValue(), "parent-6");
    Assert.assertEquals(element.getInfo().getDescription(), "state element");
    Assert.assertSame(element.getSubElements(), subElements);
  }

  private StateElement newElement() {
    return new StateElement(ITEM_ID, VERSION_ID, new Namespace(), ELEMENT_ID);
  }
}
