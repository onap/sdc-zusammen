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

import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;

public class ItemVersionConflictTest {

  @Test
  public void testConflictStartsEmpty() {
    ItemVersionConflict conflict = new ItemVersionConflict();

    Assert.assertNull(conflict.getVersionDataConflict());
    Assert.assertTrue(conflict.getElementConflictInfos().isEmpty());
  }

  @Test
  public void testSetVersionDataConflict() {
    ItemVersionConflict conflict = new ItemVersionConflict();
    ItemVersionDataConflict dataConflict = new ItemVersionDataConflict();
    ItemVersionData localData = new ItemVersionData();
    dataConflict.setLocalData(localData);

    conflict.setVersionDataConflict(dataConflict);

    Assert.assertSame(conflict.getVersionDataConflict().getLocalData(), localData);
    Assert.assertNull(conflict.getVersionDataConflict().getRemoteData());
  }

  @Test
  public void testAddElementConflictInfoAppends() {
    ItemVersionConflict conflict = new ItemVersionConflict();
    ElementConflictInfo first = new ElementConflictInfo();
    ElementConflictInfo second = new ElementConflictInfo();

    conflict.addElementConflictInfo(first);
    conflict.addElementConflictInfo(second);

    Assert.assertEquals(conflict.getElementConflictInfos().size(), 2);
    Assert.assertTrue(conflict.getElementConflictInfos().contains(first));
    Assert.assertTrue(conflict.getElementConflictInfos().contains(second));
  }

  @Test
  public void testSetElementConflictInfosReplacesTheCollection() {
    ItemVersionConflict conflict = new ItemVersionConflict();
    conflict.addElementConflictInfo(new ElementConflictInfo());
    Collection<ElementConflictInfo> replacement = new ArrayList<>();

    conflict.setElementConflictInfos(replacement);

    Assert.assertSame(conflict.getElementConflictInfos(), replacement);
    Assert.assertTrue(conflict.getElementConflictInfos().isEmpty());
  }

  @Test
  public void testAddElementConflictInfoWritesIntoTheCollectionThatWasSet() {
    ItemVersionConflict conflict = new ItemVersionConflict();
    Collection<ElementConflictInfo> elementConflictInfos = new ArrayList<>();
    conflict.setElementConflictInfos(elementConflictInfos);

    conflict.addElementConflictInfo(new ElementConflictInfo());

    Assert.assertEquals(elementConflictInfos.size(), 1);
  }
}
