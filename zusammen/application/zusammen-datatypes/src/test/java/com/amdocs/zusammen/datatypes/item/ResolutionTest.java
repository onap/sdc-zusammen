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

public class ResolutionTest {

  @Test
  public void testValues() {
    Assert.assertEquals(Resolution.values(),
        new Resolution[] {Resolution.THEIRS, Resolution.YOURS, Resolution.OTHER});
  }

  @Test
  public void testValueOfEachName() {
    for (Resolution resolution : Resolution.values()) {
      Assert.assertSame(Resolution.valueOf(resolution.name()), resolution);
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testValueOfUnknownName() {
    Resolution.valueOf("MINE");
  }
}
