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

public class ActionTest {

  @Test
  public void testValues() {
    Assert.assertEquals(Action.values(),
        new Action[] {Action.IGNORE, Action.CREATE, Action.UPDATE, Action.DELETE});
  }

  @Test
  public void testValueOfEachName() {
    for (Action action : Action.values()) {
      Assert.assertSame(Action.valueOf(action.name()), action);
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testValueOfUnknownName() {
    Action.valueOf("MERGE");
  }
}
