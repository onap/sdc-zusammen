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

public class ItemVersionTest {

  @Test
  public void testFreshItemVersionHasNoFieldsSet() {
    ItemVersion itemVersion = new ItemVersion();
    Assert.assertNull(itemVersion.getId());
    Assert.assertNull(itemVersion.getRevisionId());
    Assert.assertNull(itemVersion.getBaseId());
    Assert.assertNull(itemVersion.getCreationTime());
    Assert.assertNull(itemVersion.getModificationTime());
    Assert.assertNull(itemVersion.getData());
  }

  @Test
  public void testRoundTripOfAllFields() {
    ItemVersion itemVersion = new ItemVersion();
    Id id = new Id("version-1");
    Id revisionId = new Id("revision-2");
    Id baseId = new Id("base-3");
    Date creationTime = new Date(1_000L);
    Date modificationTime = new Date(2_000L);
    ItemVersionData data = new ItemVersionData();

    itemVersion.setId(id);
    itemVersion.setRevisionId(revisionId);
    itemVersion.setBaseId(baseId);
    itemVersion.setCreationTime(creationTime);
    itemVersion.setModificationTime(modificationTime);
    itemVersion.setData(data);

    Assert.assertSame(itemVersion.getId(), id);
    Assert.assertSame(itemVersion.getRevisionId(), revisionId);
    Assert.assertSame(itemVersion.getBaseId(), baseId);
    Assert.assertEquals(itemVersion.getCreationTime(), creationTime);
    Assert.assertEquals(itemVersion.getModificationTime(), modificationTime);
    Assert.assertSame(itemVersion.getData(), data);
  }
}
