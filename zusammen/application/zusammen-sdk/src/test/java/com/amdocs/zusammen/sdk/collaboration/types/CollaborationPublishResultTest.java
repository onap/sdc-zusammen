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

package com.amdocs.zusammen.sdk.collaboration.types;

import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CollaborationPublishResultTest {

  @Test
  public void testChangeDefaultsToUnset() {
    CollaborationPublishResult result = new CollaborationPublishResult();

    Assert.assertNull(result.getChange());
  }

  @Test
  public void testSetChange() {
    CollaborationPublishResult result = new CollaborationPublishResult();
    CollaborationMergeChange change = new CollaborationMergeChange();
    ItemVersionChange versionChange = new ItemVersionChange();
    versionChange.setAction(Action.UPDATE);
    change.setChangedVersion(versionChange);

    result.setChange(change);

    Assert.assertSame(result.getChange(), change);
    Assert.assertEquals(result.getChange().getChangedVersion().getAction(), Action.UPDATE);
  }
}
