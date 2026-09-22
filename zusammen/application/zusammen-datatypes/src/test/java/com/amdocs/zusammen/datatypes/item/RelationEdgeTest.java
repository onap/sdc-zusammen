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

package com.amdocs.zusammen.datatypes.item;

import com.amdocs.zusammen.datatypes.Id;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RelationEdgeTest {

  @Test
  public void testFreshEdgeHasNoIdsSet() {
    RelationEdge edge = new RelationEdge();
    Assert.assertNull(edge.getItemId());
    Assert.assertNull(edge.getVersionId());
    Assert.assertNull(edge.getElementId());
  }

  @Test
  public void testRoundTripOfAllFields() {
    RelationEdge edge = new RelationEdge();
    Id itemId = new Id("item-1");
    Id versionId = new Id("version-2");
    Id elementId = new Id("element-3");

    edge.setItemId(itemId);
    edge.setVersionId(versionId);
    edge.setElementId(elementId);

    Assert.assertSame(edge.getItemId(), itemId);
    Assert.assertSame(edge.getVersionId(), versionId);
    Assert.assertSame(edge.getElementId(), elementId);
  }
}
