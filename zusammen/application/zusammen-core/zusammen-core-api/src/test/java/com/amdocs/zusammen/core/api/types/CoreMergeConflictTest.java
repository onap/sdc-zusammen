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
import java.util.Collection;
import java.util.Collections;

public class CoreMergeConflictTest {

  @Test
  public void testConflictDefaultsToUnset() {
    CoreMergeConflict conflict = new CoreMergeConflict();

    Assert.assertNull(conflict.getVersionDataConflict());
    Assert.assertNull(conflict.getElementConflicts());
  }

  @Test
  public void testIsSuccessWhenNothingConflicts() {
    Assert.assertTrue(new CoreMergeConflict().isSuccess());
  }

  @Test
  public void testIsSuccessWhenElementConflictsAreEmpty() {
    CoreMergeConflict conflict = new CoreMergeConflict();

    conflict.setElementConflicts(Collections.<CoreElementConflict>emptyList());

    Assert.assertTrue(conflict.isSuccess());
  }

  @Test
  public void testIsNotSuccessWhenAnElementConflicts() {
    CoreMergeConflict conflict = new CoreMergeConflict();
    Collection<CoreElementConflict> elementConflicts = Arrays.asList(new CoreElementConflict());

    conflict.setElementConflicts(elementConflicts);

    Assert.assertSame(conflict.getElementConflicts(), elementConflicts);
    Assert.assertFalse(conflict.isSuccess());
  }

  @Test
  public void testIsNotSuccessWhenTheVersionDataConflicts() {
    CoreMergeConflict conflict = new CoreMergeConflict();
    ItemVersionDataConflict dataConflict = new ItemVersionDataConflict();

    conflict.setVersionDataConflict(dataConflict);

    Assert.assertSame(conflict.getVersionDataConflict(), dataConflict);
    Assert.assertFalse(conflict.isSuccess());
  }
}
