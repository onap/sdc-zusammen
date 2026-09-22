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

import com.amdocs.zusammen.datatypes.Id;

import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ItemTest {

  @Test
  public void testFreshItemHasNoFieldsSet() {
    Item item = new Item();
    Assert.assertNull(item.getId());
    Assert.assertNull(item.getInfo());
    Assert.assertNull(item.getCreationTime());
    Assert.assertNull(item.getModificationTime());
  }

  @Test
  public void testRoundTripOfAllFields() {
    Item item = new Item();
    Id id = new Id("item-1");
    Info info = new Info();
    info.setName("item-name");
    Date creationTime = new Date(1_000L);
    Date modificationTime = new Date(2_000L);

    item.setId(id);
    item.setInfo(info);
    item.setCreationTime(creationTime);
    item.setModificationTime(modificationTime);

    Assert.assertSame(item.getId(), id);
    Assert.assertSame(item.getInfo(), info);
    Assert.assertEquals(item.getCreationTime(), creationTime);
    Assert.assertEquals(item.getModificationTime(), modificationTime);
  }
}
