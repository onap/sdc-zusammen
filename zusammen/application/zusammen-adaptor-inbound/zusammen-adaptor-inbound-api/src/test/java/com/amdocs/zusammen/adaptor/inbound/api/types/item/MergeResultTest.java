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

import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

public class MergeResultTest {

  @Test
  public void testResultDefaultsToUnset() {
    MergeResult result = new MergeResult();

    Assert.assertNull(result.getChange());
    Assert.assertNull(result.getConflict());
  }

  @Test
  public void testSetChange() {
    MergeResult result = new MergeResult();
    MergeChange change = new MergeChange();

    result.setChange(change);

    Assert.assertSame(result.getChange(), change);
  }

  @Test
  public void testIsSuccessWhenThereIsNoConflict() {
    Assert.assertTrue(new MergeResult().isSuccess());
  }

  @Test
  public void testIsSuccessWhenTheConflictIsEmpty() {
    MergeResult result = new MergeResult();

    result.setConflict(new MergeConflict());

    Assert.assertTrue(result.isSuccess());
  }

  @Test
  public void testIsNotSuccessWhenTheConflictHasElementConflicts() {
    MergeResult result = new MergeResult();
    MergeConflict conflict = new MergeConflict();
    conflict.setElementConflicts(Arrays.asList(new ElementConflict()));

    result.setConflict(conflict);

    Assert.assertSame(result.getConflict(), conflict);
    Assert.assertFalse(result.isSuccess());
  }

  @Test
  public void testIsNotSuccessWhenTheConflictHasVersionData() {
    MergeResult result = new MergeResult();
    MergeConflict conflict = new MergeConflict();
    conflict.setVersionDataConflict(new ItemVersionDataConflict());

    result.setConflict(conflict);

    Assert.assertFalse(result.isSuccess());
  }
}
