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

import org.testng.Assert;
import org.testng.annotations.Test;

public class CollaborationMergeResultTest {

  @Test
  public void testResultDefaultsToUnset() {
    CollaborationMergeResult result = new CollaborationMergeResult();

    Assert.assertNull(result.getChange());
    Assert.assertNull(result.getConflict());
  }

  @Test
  public void testSetChangeAndConflict() {
    CollaborationMergeResult result = new CollaborationMergeResult();
    CollaborationMergeChange change = new CollaborationMergeChange();
    CollaborationMergeConflict conflict = new CollaborationMergeConflict();

    result.setChange(change);
    result.setConflict(conflict);

    Assert.assertSame(result.getChange(), change);
    Assert.assertSame(result.getConflict(), conflict);
  }
}
