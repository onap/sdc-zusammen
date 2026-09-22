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

import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;

public class CollaborationMergeConflictTest {

  @Test
  public void testConflictStartsEmpty() {
    CollaborationMergeConflict conflict = new CollaborationMergeConflict();

    Assert.assertNull(conflict.getVersionDataConflict());
    Assert.assertTrue(conflict.getElementConflicts().isEmpty());
  }

  @Test
  public void testSetVersionDataConflict() {
    CollaborationMergeConflict conflict = new CollaborationMergeConflict();
    ItemVersionDataConflict dataConflict = new ItemVersionDataConflict();
    ItemVersionData remoteData = new ItemVersionData();
    dataConflict.setRemoteData(remoteData);

    conflict.setVersionDataConflict(dataConflict);

    Assert.assertSame(conflict.getVersionDataConflict().getRemoteData(), remoteData);
    Assert.assertNull(conflict.getVersionDataConflict().getLocalData());
  }

  @Test
  public void testAddElementConflictAppends() {
    CollaborationMergeConflict conflict = new CollaborationMergeConflict();
    CollaborationElementConflict first = new CollaborationElementConflict();
    CollaborationElementConflict second = new CollaborationElementConflict();

    conflict.addElementConflict(first);
    conflict.addElementConflict(second);

    Assert.assertEquals(conflict.getElementConflicts().size(), 2);
    Assert.assertTrue(conflict.getElementConflicts().contains(first));
    Assert.assertTrue(conflict.getElementConflicts().contains(second));
  }

  @Test
  public void testSetElementConflictsReplacesTheCollection() {
    CollaborationMergeConflict conflict = new CollaborationMergeConflict();
    conflict.addElementConflict(new CollaborationElementConflict());
    Collection<CollaborationElementConflict> replacement = new ArrayList<>();

    conflict.setElementConflicts(replacement);

    Assert.assertSame(conflict.getElementConflicts(), replacement);
    Assert.assertTrue(conflict.getElementConflicts().isEmpty());
  }
}
