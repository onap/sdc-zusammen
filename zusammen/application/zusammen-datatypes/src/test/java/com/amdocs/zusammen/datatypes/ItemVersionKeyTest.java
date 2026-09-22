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

public class ItemVersionKeyTest {

  @Test
  public void testDefaultConstructorLeavesBothIdsNull() {
    ItemVersionKey key = new ItemVersionKey();
    Assert.assertNull(key.getItemId());
    Assert.assertNull(key.getVersionId());
  }

  @Test
  public void testTwoArgumentConstructorKeepsArgumentOrder() {
    ItemVersionKey key = new ItemVersionKey("item-1", "version-2");
    Assert.assertEquals(key.getItemId(), "item-1");
    Assert.assertEquals(key.getVersionId(), "version-2");
  }

  @Test
  public void testRoundTrip() {
    ItemVersionKey key = new ItemVersionKey();
    key.setItemId("item-1");
    key.setVersionId("version-2");
    Assert.assertEquals(key.getItemId(), "item-1");
    Assert.assertEquals(key.getVersionId(), "version-2");
  }

  @Test
  public void testEqualsIsReflexive() {
    ItemVersionKey key = new ItemVersionKey("item-1", "version-2");
    Assert.assertTrue(key.equals(key));
  }

  @Test
  public void testEqualsAndHashCodeOfEqualKeys() {
    ItemVersionKey one = new ItemVersionKey("item-1", "version-2");
    ItemVersionKey other = new ItemVersionKey("item-1", "version-2");
    Assert.assertEquals(one, other);
    Assert.assertEquals(other, one);
    Assert.assertEquals(one.hashCode(), other.hashCode());
  }

  @Test
  public void testEqualsOfDifferentItemId() {
    Assert.assertNotEquals(new ItemVersionKey("item-1", "version-2"),
        new ItemVersionKey("item-9", "version-2"));
  }

  @Test
  public void testEqualsOfDifferentVersionId() {
    Assert.assertNotEquals(new ItemVersionKey("item-1", "version-2"),
        new ItemVersionKey("item-1", "version-9"));
  }

  @Test
  public void testEqualsDoesNotConfuseSwappedIds() {
    Assert.assertNotEquals(new ItemVersionKey("a", "b"), new ItemVersionKey("b", "a"));
  }

  @Test
  public void testEqualsIsSymmetricWhenOnlyOneItemIdIsNull() {
    Assert.assertFalse(new ItemVersionKey(null, "b").equals(new ItemVersionKey("a", "b")));
    Assert.assertFalse(new ItemVersionKey("a", "b").equals(new ItemVersionKey(null, "b")));
  }

  @Test
  public void testEqualsIsSymmetricWhenOnlyOneVersionIdIsNull() {
    Assert.assertFalse(new ItemVersionKey("a", null).equals(new ItemVersionKey("a", "b")));
    Assert.assertFalse(new ItemVersionKey("a", "b").equals(new ItemVersionKey("a", null)));
  }

  @Test
  public void testEqualsAndHashCodeOfEmptyKeys() {
    Assert.assertEquals(new ItemVersionKey(), new ItemVersionKey());
    Assert.assertEquals(new ItemVersionKey().hashCode(), new ItemVersionKey().hashCode());
  }

  @Test
  public void testEqualsOfNull() {
    Assert.assertFalse(new ItemVersionKey("a", "b").equals(null));
  }

  @Test
  public void testEqualsOfOtherType() {
    Assert.assertFalse(new ItemVersionKey("a", "b").equals("a"));
  }
}
