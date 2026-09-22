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

package com.amdocs.zusammen.datatypes.itemversion;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TagTest {

  @Test
  public void testConstructorKeepsArgumentOrder() {
    Tag tag = new Tag("tag-name", "tag-description");
    Assert.assertEquals(tag.getName(), "tag-name");
    Assert.assertEquals(tag.getDescription(), "tag-description");
  }

  @Test
  public void testConstructorAcceptsNullDescription() {
    Tag tag = new Tag("tag-name", null);
    Assert.assertEquals(tag.getName(), "tag-name");
    Assert.assertNull(tag.getDescription());
  }

  @Test
  public void testRoundTrip() {
    Tag tag = new Tag("before-name", "before-description");
    tag.setName("after-name");
    tag.setDescription("after-description");
    Assert.assertEquals(tag.getName(), "after-name");
    Assert.assertEquals(tag.getDescription(), "after-description");
  }
}
