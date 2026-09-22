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

public class CoreElementConflictInfoTest {

  @Test
  public void testElementInfosDefaultToUnset() {
    CoreElementConflictInfo conflict = new CoreElementConflictInfo();

    Assert.assertNull(conflict.getLocalCoreElementInfo());
    Assert.assertNull(conflict.getRemoteCoreElementInfo());
  }

  @Test
  public void testLocalAndRemoteElementInfosAreKeptApart() {
    CoreElementConflictInfo conflict = new CoreElementConflictInfo();

    conflict.setLocalCoreElementInfo(elementInfo("local"));
    conflict.setRemoteCoreElementInfo(elementInfo("remote"));

    Assert.assertEquals(conflict.getLocalCoreElementInfo().getId().getValue(), "local");
    Assert.assertEquals(conflict.getRemoteCoreElementInfo().getId().getValue(), "remote");
  }

  private CoreElementInfo elementInfo(String id) {
    CoreElementInfo elementInfo = new CoreElementInfo();
    elementInfo.setId(new Id(id));
    return elementInfo;
  }
}
