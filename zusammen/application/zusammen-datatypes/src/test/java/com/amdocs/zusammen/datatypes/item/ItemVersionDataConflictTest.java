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

package com.amdocs.zusammen.datatypes.item;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ItemVersionDataConflictTest {

  @Test
  public void testFreshConflictHasNeitherSide() {
    ItemVersionDataConflict conflict = new ItemVersionDataConflict();
    Assert.assertNull(conflict.getLocalData());
    Assert.assertNull(conflict.getRemoteData());
  }

  @Test
  public void testLocalAndRemoteDataAreKeptApart() {
    ItemVersionDataConflict conflict = new ItemVersionDataConflict();
    ItemVersionData localData = new ItemVersionData();
    Info localInfo = new Info();
    localInfo.setName("local");
    localData.setInfo(localInfo);
    ItemVersionData remoteData = new ItemVersionData();
    Info remoteInfo = new Info();
    remoteInfo.setName("remote");
    remoteData.setInfo(remoteInfo);

    conflict.setLocalData(localData);
    conflict.setRemoteData(remoteData);

    Assert.assertEquals(conflict.getLocalData().getInfo().getName(), "local");
    Assert.assertEquals(conflict.getRemoteData().getInfo().getName(), "remote");
  }
}
