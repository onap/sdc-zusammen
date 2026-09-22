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

import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

public class CoreMergeResultTest {

  @Test
  public void testResultDefaultsToUnset() {
    CoreMergeResult result = new CoreMergeResult();

    Assert.assertNull(result.getChange());
    Assert.assertNull(result.getConflict());
  }

  @Test
  public void testSetChange() {
    CoreMergeResult result = new CoreMergeResult();
    CoreMergeChange change = new CoreMergeChange();

    result.setChange(change);

    Assert.assertSame(result.getChange(), change);
  }

  @Test
  public void testIsCompletedWhenThereIsNoConflict() {
    Assert.assertTrue(new CoreMergeResult().isCompleted());
  }

  @Test
  public void testIsCompletedWhenTheConflictIsEmpty() {
    CoreMergeResult result = new CoreMergeResult();

    result.setConflict(new CoreMergeConflict());

    Assert.assertTrue(result.isCompleted());
  }

  @Test
  public void testIsNotCompletedWhenTheConflictHasElementConflicts() {
    CoreMergeResult result = new CoreMergeResult();
    CoreMergeConflict conflict = new CoreMergeConflict();
    conflict.setElementConflicts(Arrays.asList(new CoreElementConflict()));

    result.setConflict(conflict);

    Assert.assertSame(result.getConflict(), conflict);
    Assert.assertFalse(result.isCompleted());
  }

  @Test
  public void testIsNotCompletedWhenTheConflictHasVersionData() {
    CoreMergeResult result = new CoreMergeResult();
    CoreMergeConflict conflict = new CoreMergeConflict();
    conflict.setVersionDataConflict(new ItemVersionDataConflict());

    result.setConflict(conflict);

    Assert.assertFalse(result.isCompleted());
  }
}
