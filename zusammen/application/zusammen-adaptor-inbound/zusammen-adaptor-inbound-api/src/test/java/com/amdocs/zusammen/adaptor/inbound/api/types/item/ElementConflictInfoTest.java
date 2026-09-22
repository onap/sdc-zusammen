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

import com.amdocs.zusammen.datatypes.Id;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ElementConflictInfoTest {

  @Test
  public void testElementInfosDefaultToUnset() {
    ElementConflictInfo conflict = new ElementConflictInfo();

    Assert.assertNull(conflict.getLocalElementInfo());
    Assert.assertNull(conflict.getRemoteElementInfo());
  }

  @Test
  public void testLocalAndRemoteElementInfosAreKeptApart() {
    ElementConflictInfo conflict = new ElementConflictInfo();

    conflict.setLocalElementInfo(elementInfo("local"));
    conflict.setRemoteElementInfo(elementInfo("remote"));

    Assert.assertEquals(conflict.getLocalElementInfo().getId().getValue(), "local");
    Assert.assertEquals(conflict.getRemoteElementInfo().getId().getValue(), "remote");
  }

  private ElementInfo elementInfo(String id) {
    ElementInfo elementInfo = new ElementInfo();
    elementInfo.setId(new Id(id));
    return elementInfo;
  }
}
