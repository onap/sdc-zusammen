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

package com.amdocs.zusammen.adaptor.inbound.api.types.item;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class ElementInfoTest {

  @Test
  public void testElementInfoStartsEmpty() {
    ElementInfo elementInfo = new ElementInfo();

    Assert.assertNull(elementInfo.getId());
    Assert.assertNull(elementInfo.getInfo());
    Assert.assertNull(elementInfo.getRelations());
    Assert.assertTrue(elementInfo.getSubElements().isEmpty());
  }

  @Test
  public void testSetIdAndInfo() {
    ElementInfo elementInfo = new ElementInfo();
    Id id = new Id("element-1");
    Info info = new Info();
    info.setDescription("element description");

    elementInfo.setId(id);
    elementInfo.setInfo(info);

    Assert.assertEquals(elementInfo.getId(), id);
    Assert.assertEquals(elementInfo.getInfo().getDescription(), "element description");
  }

  @Test
  public void testSetRelationsKeepsCallerCollection() {
    ElementInfo elementInfo = new ElementInfo();
    Relation relation = new Relation();
    relation.setType("contains");
    Collection<Relation> relations = Arrays.asList(relation);

    elementInfo.setRelations(relations);

    Assert.assertSame(elementInfo.getRelations(), relations);
    Assert.assertEquals(elementInfo.getRelations().iterator().next().getType(), "contains");
  }

  @Test
  public void testSetSubElementsKeepsCallerCollection() {
    ElementInfo elementInfo = new ElementInfo();
    Collection<ElementInfo> subElements = new ArrayList<>();
    ElementInfo subElement = new ElementInfo();
    subElement.setId(new Id("sub-1"));
    subElements.add(subElement);

    elementInfo.setSubElements(subElements);

    Assert.assertSame(elementInfo.getSubElements(), subElements);
    Assert.assertEquals(elementInfo.getSubElements().iterator().next().getId().getValue(), "sub-1");
  }

  @Test
  public void testSetEmptySubElements() {
    ElementInfo elementInfo = new ElementInfo();

    elementInfo.setSubElements(Collections.<ElementInfo>emptyList());

    Assert.assertTrue(elementInfo.getSubElements().isEmpty());
  }
}
