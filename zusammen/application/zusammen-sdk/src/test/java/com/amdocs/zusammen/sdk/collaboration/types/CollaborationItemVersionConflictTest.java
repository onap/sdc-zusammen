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
import com.amdocs.zusammen.sdk.types.ElementConflictDescriptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class CollaborationItemVersionConflictTest {

  @Test
  public void testConflictStartsEmpty() {
    CollaborationItemVersionConflict conflict = new CollaborationItemVersionConflict();

    Assert.assertNull(conflict.getVersionDataConflict());
    Assert.assertTrue(conflict.getElementConflictDescriptors().isEmpty());
  }

  @Test
  public void testSetVersionDataConflict() {
    CollaborationItemVersionConflict conflict = new CollaborationItemVersionConflict();
    ItemVersionDataConflict dataConflict = new ItemVersionDataConflict();
    ItemVersionData localData = new ItemVersionData();
    dataConflict.setLocalData(localData);

    conflict.setVersionDataConflict(dataConflict);

    Assert.assertSame(conflict.getVersionDataConflict().getLocalData(), localData);
    Assert.assertNull(conflict.getVersionDataConflict().getRemoteData());
  }

  @Test
  public void testAddElementConflictDescriptorAppends() {
    CollaborationItemVersionConflict conflict = new CollaborationItemVersionConflict();
    ElementConflictDescriptor first = new ElementConflictDescriptor();
    ElementConflictDescriptor second = new ElementConflictDescriptor();

    conflict.addElementConflictDescriptor(first);
    conflict.addElementConflictDescriptor(second);

    Assert.assertEquals(conflict.getElementConflictDescriptors().size(), 2);
    Assert.assertTrue(conflict.getElementConflictDescriptors().contains(first));
    Assert.assertTrue(conflict.getElementConflictDescriptors().contains(second));
  }

  @Test
  public void testSetElementConflictDescriptorsReplacesTheCollection() {
    CollaborationItemVersionConflict conflict = new CollaborationItemVersionConflict();
    conflict.addElementConflictDescriptor(new ElementConflictDescriptor());
    Collection<ElementConflictDescriptor> replacement = new ArrayList<>();

    conflict.setElementConflictDescriptors(replacement);

    Assert.assertSame(conflict.getElementConflictDescriptors(), replacement);
    Assert.assertTrue(conflict.getElementConflictDescriptors().isEmpty());
  }

  @Test
  public void testAddElementConflictDescriptorWritesIntoTheCollectionThatWasSet() {
    CollaborationItemVersionConflict conflict = new CollaborationItemVersionConflict();
    Collection<ElementConflictDescriptor> descriptors = new ArrayList<>();
    conflict.setElementConflictDescriptors(descriptors);

    conflict.addElementConflictDescriptor(new ElementConflictDescriptor());

    Assert.assertEquals(descriptors.size(), 1);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testAddElementConflictDescriptorFailsOnAnImmutableCollection() {
    CollaborationItemVersionConflict conflict = new CollaborationItemVersionConflict();
    conflict.setElementConflictDescriptors(
        Collections.<ElementConflictDescriptor>emptyList());

    conflict.addElementConflictDescriptor(new ElementConflictDescriptor());
  }
}
