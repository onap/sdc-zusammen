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
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;

public class CoreMergeChangeTest {

  @Test
  public void testChangeDefaultsToUnset() {
    CoreMergeChange change = new CoreMergeChange();

    Assert.assertNull(change.getChangedVersion());
    Assert.assertNull(change.getChangedElements());
  }

  @Test
  public void testSetChangedVersion() {
    CoreMergeChange change = new CoreMergeChange();
    ItemVersionChange versionChange = new ItemVersionChange();
    versionChange.setAction(Action.CREATE);

    change.setChangedVersion(versionChange);

    Assert.assertEquals(change.getChangedVersion().getAction(), Action.CREATE);
  }

  @Test
  public void testSetChangedElementsKeepsCallerCollection() {
    CoreMergeChange change = new CoreMergeChange();
    CoreElement changedElement = new CoreElement();
    changedElement.setId(new Id("element-1"));
    Collection<CoreElement> changedElements = Arrays.asList(changedElement);

    change.setChangedElements(changedElements);

    Assert.assertSame(change.getChangedElements(), changedElements);
    Assert.assertEquals(change.getChangedElements().iterator().next().getId().getValue(),
        "element-1");
  }
}
