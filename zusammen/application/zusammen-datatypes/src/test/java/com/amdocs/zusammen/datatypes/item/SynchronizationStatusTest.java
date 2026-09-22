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

public class SynchronizationStatusTest {

  @Test
  public void testValues() {
    Assert.assertEquals(SynchronizationStatus.values(), new SynchronizationStatus[] {
        SynchronizationStatus.UP_TO_DATE,
        SynchronizationStatus.OUT_OF_SYNC,
        SynchronizationStatus.MERGING});
  }

  @Test
  public void testToStringIsTheDisplayName() {
    Assert.assertEquals(SynchronizationStatus.UP_TO_DATE.toString(), "Up to date");
    Assert.assertEquals(SynchronizationStatus.OUT_OF_SYNC.toString(), "Out of sync");
    Assert.assertEquals(SynchronizationStatus.MERGING.toString(), "Merging");
  }

  @Test
  public void testNameIsNotTheDisplayName() {
    Assert.assertEquals(SynchronizationStatus.UP_TO_DATE.name(), "UP_TO_DATE");
  }

  @Test
  public void testValueOfTakesTheNameNotTheDisplayName() {
    Assert.assertSame(SynchronizationStatus.valueOf("OUT_OF_SYNC"),
        SynchronizationStatus.OUT_OF_SYNC);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testValueOfDisplayName() {
    SynchronizationStatus.valueOf("Out of sync");
  }
}
