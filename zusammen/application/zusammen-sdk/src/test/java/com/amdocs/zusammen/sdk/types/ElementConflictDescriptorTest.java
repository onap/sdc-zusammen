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

package com.amdocs.zusammen.sdk.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ElementConflictDescriptorTest {

  @Test
  public void testDescriptorsDefaultToUnset() {
    ElementConflictDescriptor conflict = new ElementConflictDescriptor();

    Assert.assertNull(conflict.getLocalElementDescriptor());
    Assert.assertNull(conflict.getRemoteElementDescriptor());
  }

  @Test
  public void testLocalAndRemoteDescriptorsAreKeptApart() {
    ElementConflictDescriptor conflict = new ElementConflictDescriptor();
    ElementDescriptor local = descriptor("local");
    ElementDescriptor remote = descriptor("remote");

    conflict.setLocalElementDescriptor(local);
    conflict.setRemoteElementDescriptor(remote);

    Assert.assertEquals(conflict.getLocalElementDescriptor().getId().getValue(), "local");
    Assert.assertEquals(conflict.getRemoteElementDescriptor().getId().getValue(), "remote");
  }

  private ElementDescriptor descriptor(String elementId) {
    return new ElementDescriptor(new Id("item-1"), new Id("version-2"), new Namespace(),
        new Id(elementId));
  }
}
