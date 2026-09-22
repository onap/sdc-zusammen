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

package com.amdocs.zusammen.core.api.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class CoreElementInfoTest {

  @Test
  public void testElementInfoStartsEmpty() {
    CoreElementInfo elementInfo = new CoreElementInfo();

    Assert.assertNull(elementInfo.getId());
    Assert.assertNull(elementInfo.getParentId());
    Assert.assertNull(elementInfo.getNamespace());
    Assert.assertNull(elementInfo.getInfo());
    Assert.assertTrue(elementInfo.getRelations().isEmpty());
    Assert.assertTrue(elementInfo.getSubElements().isEmpty());
  }

  @Test
  public void testSetIdentityAndInfo() {
    CoreElementInfo elementInfo = new CoreElementInfo();
    Id id = new Id("element-1");
    Id parentId = new Id("parent-2");
    Namespace namespace = new Namespace(new Namespace(), parentId);
    Info info = new Info();
    info.setDescription("core element info");

    elementInfo.setId(id);
    elementInfo.setParentId(parentId);
    elementInfo.setNamespace(namespace);
    elementInfo.setInfo(info);

    Assert.assertEquals(elementInfo.getId(), id);
    Assert.assertEquals(elementInfo.getParentId(), parentId);
    Assert.assertEquals(elementInfo.getNamespace(), namespace);
    Assert.assertEquals(elementInfo.getInfo().getDescription(), "core element info");
  }

  @Test
  public void testSetRelationsKeepsCallerCollection() {
    CoreElementInfo elementInfo = new CoreElementInfo();
    Relation relation = new Relation();
    relation.setType("contains");
    Collection<Relation> relations = Arrays.asList(relation);

    elementInfo.setRelations(relations);

    Assert.assertSame(elementInfo.getRelations(), relations);
  }

  @Test
  public void testSetSubElementsKeepsCallerCollection() {
    CoreElementInfo elementInfo = new CoreElementInfo();
    Collection<CoreElementInfo> subElements = new ArrayList<>();
    CoreElementInfo subElement = new CoreElementInfo();
    subElement.setId(new Id("sub-3"));
    subElements.add(subElement);

    elementInfo.setSubElements(subElements);

    Assert.assertSame(elementInfo.getSubElements(), subElements);
    Assert.assertEquals(elementInfo.getSubElements().iterator().next().getId().getValue(), "sub-3");
  }
}
