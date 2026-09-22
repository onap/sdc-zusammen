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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class CoreElementMergeResultTest {

  @Test
  public void testResultDefaultsToUnset() {
    CoreElementMergeResult result = new CoreElementMergeResult();

    Assert.assertNull(result.getConflicts());
    Assert.assertNull(result.getChangedElements());
  }

  @Test
  public void testIsSuccessWhenThereAreNoConflicts() {
    Assert.assertTrue(new CoreElementMergeResult().isSuccess());
  }

  @Test
  public void testIsSuccessWhenTheConflictCollectionIsEmpty() {
    CoreElementMergeResult result = new CoreElementMergeResult();

    result.setConflicts(Collections.<CoreElementConflict>emptyList());

    Assert.assertTrue(result.isSuccess());
  }

  @Test
  public void testIsNotSuccessWhenAnElementConflicts() {
    CoreElementMergeResult result = new CoreElementMergeResult();
    Collection<CoreElementConflict> conflicts = Arrays.asList(new CoreElementConflict());

    result.setConflicts(conflicts);

    Assert.assertSame(result.getConflicts(), conflicts);
    Assert.assertFalse(result.isSuccess());
  }

  @Test
  public void testSetChangedElementsKeepsCallerCollection() {
    CoreElementMergeResult result = new CoreElementMergeResult();
    CoreElement changedElement = new CoreElement();
    changedElement.setId(new Id("element-1"));
    Collection<CoreElement> changedElements = Arrays.asList(changedElement);

    result.setChangedElements(changedElements);

    Assert.assertSame(result.getChangedElements(), changedElements);
    Assert.assertEquals(result.getChangedElements().iterator().next().getId().getValue(),
        "element-1");
  }
}
