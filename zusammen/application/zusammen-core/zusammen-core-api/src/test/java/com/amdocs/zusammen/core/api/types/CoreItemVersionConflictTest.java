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

import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;

public class CoreItemVersionConflictTest {

  @Test
  public void testConflictStartsEmpty() {
    CoreItemVersionConflict conflict = new CoreItemVersionConflict();

    Assert.assertNull(conflict.getVersionDataConflict());
    Assert.assertTrue(conflict.getElementConflictInfos().isEmpty());
  }

  @Test
  public void testSetVersionDataConflict() {
    CoreItemVersionConflict conflict = new CoreItemVersionConflict();
    ItemVersionDataConflict dataConflict = new ItemVersionDataConflict();
    ItemVersionData remoteData = new ItemVersionData();
    dataConflict.setRemoteData(remoteData);

    conflict.setVersionDataConflict(dataConflict);

    Assert.assertSame(conflict.getVersionDataConflict().getRemoteData(), remoteData);
    Assert.assertNull(conflict.getVersionDataConflict().getLocalData());
  }

  @Test
  public void testAddElementConflictAppends() {
    CoreItemVersionConflict conflict = new CoreItemVersionConflict();
    CoreElementConflictInfo first = new CoreElementConflictInfo();
    CoreElementConflictInfo second = new CoreElementConflictInfo();

    conflict.addElementConflict(first);
    conflict.addElementConflict(second);

    Assert.assertEquals(conflict.getElementConflictInfos().size(), 2);
    Assert.assertTrue(conflict.getElementConflictInfos().contains(first));
    Assert.assertTrue(conflict.getElementConflictInfos().contains(second));
  }

  @Test
  public void testSetElementConflictInfosReplacesTheCollection() {
    CoreItemVersionConflict conflict = new CoreItemVersionConflict();
    conflict.addElementConflict(new CoreElementConflictInfo());
    Collection<CoreElementConflictInfo> replacement = new ArrayList<>();

    conflict.setElementConflictInfos(replacement);

    Assert.assertSame(conflict.getElementConflictInfos(), replacement);
    Assert.assertTrue(conflict.getElementConflictInfos().isEmpty());
  }

  @Test
  public void testAddElementConflictWritesIntoTheCollectionThatWasSet() {
    CoreItemVersionConflict conflict = new CoreItemVersionConflict();
    Collection<CoreElementConflictInfo> elementConflictInfos = new ArrayList<>();
    conflict.setElementConflictInfos(elementConflictInfos);

    conflict.addElementConflict(new CoreElementConflictInfo());

    Assert.assertEquals(elementConflictInfos.size(), 1);
  }
}
