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

public class CoreElementConflictTest {

  @Test
  public void testElementsDefaultToUnset() {
    CoreElementConflict conflict = new CoreElementConflict();

    Assert.assertNull(conflict.getLocalElement());
    Assert.assertNull(conflict.getRemoteElement());
  }

  @Test
  public void testLocalAndRemoteElementsAreKeptApart() {
    CoreElementConflict conflict = new CoreElementConflict();

    conflict.setLocalElement(element("local"));
    conflict.setRemoteElement(element("remote"));

    Assert.assertEquals(conflict.getLocalElement().getId().getValue(), "local");
    Assert.assertEquals(conflict.getRemoteElement().getId().getValue(), "remote");
  }

  private CoreElement element(String id) {
    CoreElement element = new CoreElement();
    element.setId(new Id(id));
    return element;
  }
}
