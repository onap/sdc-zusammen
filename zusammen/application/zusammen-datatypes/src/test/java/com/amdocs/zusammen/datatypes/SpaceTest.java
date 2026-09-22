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

package com.amdocs.zusammen.datatypes;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SpaceTest {

  @Test
  public void testValues() {
    Assert.assertEquals(Space.values(), new Space[] {Space.PUBLIC, Space.PRIVATE, Space.BOTH});
  }

  @Test
  public void testValueOfEachName() {
    for (Space space : Space.values()) {
      Assert.assertSame(Space.valueOf(space.name()), space);
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testValueOfUnknownName() {
    Space.valueOf("SHARED");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testValueOfIsCaseSensitive() {
    Space.valueOf("public");
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testValueOfNull() {
    Space.valueOf(null);
  }
}
