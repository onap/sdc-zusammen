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

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CollaborationElementConflictTest {

  @Test
  public void testElementsDefaultToUnset() {
    CollaborationElementConflict conflict = new CollaborationElementConflict();

    Assert.assertNull(conflict.getLocalElement());
    Assert.assertNull(conflict.getRemoteElement());
  }

  @Test
  public void testLocalAndRemoteElementsAreKeptApart() {
    CollaborationElementConflict conflict = new CollaborationElementConflict();

    conflict.setLocalElement(element("local"));
    conflict.setRemoteElement(element("remote"));

    Assert.assertEquals(conflict.getLocalElement().getId().getValue(), "local");
    Assert.assertEquals(conflict.getRemoteElement().getId().getValue(), "remote");
  }

  private CollaborationElement element(String elementId) {
    return new CollaborationElement(new Id("item-1"), new Id("version-2"), new Namespace(),
        new Id(elementId));
  }
}
