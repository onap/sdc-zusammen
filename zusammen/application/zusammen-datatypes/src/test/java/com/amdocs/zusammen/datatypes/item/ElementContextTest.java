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

import org.testng.Assert;
import org.testng.annotations.Test;

public class ElementContextTest {

  @Test
  public void testDefaultConstructorLeavesAllIdsNull() {
    ElementContext context = new ElementContext();
    Assert.assertNull(context.getItemId());
    Assert.assertNull(context.getVersionId());
    Assert.assertNull(context.getRevisionId());
  }

  @Test
  public void testStringConstructorWrapsBothArgumentsInIds() {
    ElementContext context = new ElementContext("item-1", "version-2");
    Assert.assertEquals(context.getItemId(), new Id("item-1"));
    Assert.assertEquals(context.getVersionId(), new Id("version-2"));
    Assert.assertNull(context.getRevisionId());
  }

  @Test
  public void testTwoIdConstructorLeavesRevisionIdNull() {
    ElementContext context = new ElementContext(new Id("item-1"), new Id("version-2"));
    Assert.assertEquals(context.getItemId(), new Id("item-1"));
    Assert.assertEquals(context.getVersionId(), new Id("version-2"));
    Assert.assertNull(context.getRevisionId());
  }

  @Test
  public void testThreeIdConstructor() {
    ElementContext context =
        new ElementContext(new Id("item-1"), new Id("version-2"), new Id("revision-3"));
    Assert.assertEquals(context.getItemId(), new Id("item-1"));
    Assert.assertEquals(context.getVersionId(), new Id("version-2"));
    Assert.assertEquals(context.getRevisionId(), new Id("revision-3"));
  }

  @Test
  public void testRoundTrip() {
    ElementContext context = new ElementContext();
    context.setItemId(new Id("item-1"));
    context.setVersionId(new Id("version-2"));
    context.setRevisionId(new Id("revision-3"));
    Assert.assertEquals(context.getItemId(), new Id("item-1"));
    Assert.assertEquals(context.getVersionId(), new Id("version-2"));
    Assert.assertEquals(context.getRevisionId(), new Id("revision-3"));
  }

  @Test
  public void testEqualsIsReflexive() {
    ElementContext context = new ElementContext("item-1", "version-2");
    Assert.assertTrue(context.equals(context));
  }

  @Test
  public void testEqualsAndHashCodeOfEqualContexts() {
    ElementContext one =
        new ElementContext(new Id("item-1"), new Id("version-2"), new Id("revision-3"));
    ElementContext other =
        new ElementContext(new Id("item-1"), new Id("version-2"), new Id("revision-3"));
    Assert.assertEquals(one, other);
    Assert.assertEquals(other, one);
    Assert.assertEquals(one.hashCode(), other.hashCode());
  }

  @Test
  public void testEqualsOfDifferentItemId() {
    Assert.assertNotEquals(new ElementContext("item-1", "version-2"),
        new ElementContext("item-9", "version-2"));
  }

  @Test
  public void testEqualsOfDifferentVersionId() {
    Assert.assertNotEquals(new ElementContext("item-1", "version-2"),
        new ElementContext("item-1", "version-9"));
  }

  @Test
  public void testEqualsOfDifferentRevisionId() {
    ElementContext one =
        new ElementContext(new Id("item-1"), new Id("version-2"), new Id("revision-3"));
    ElementContext other =
        new ElementContext(new Id("item-1"), new Id("version-2"), new Id("revision-9"));
    Assert.assertNotEquals(one, other);
  }

  @Test
  public void testEqualsIsSymmetricWhenOnlyOneRevisionIdIsNull() {
    ElementContext withRevision =
        new ElementContext(new Id("item-1"), new Id("version-2"), new Id("revision-3"));
    ElementContext withoutRevision = new ElementContext("item-1", "version-2");
    Assert.assertFalse(withRevision.equals(withoutRevision));
    Assert.assertFalse(withoutRevision.equals(withRevision));
  }

  @Test
  public void testEqualsIsSymmetricWhenOnlyOneItemIdIsNull() {
    ElementContext withItemId = new ElementContext(new Id("item-1"), new Id("version-2"));
    ElementContext withoutItemId = new ElementContext(null, new Id("version-2"));
    Assert.assertFalse(withItemId.equals(withoutItemId));
    Assert.assertFalse(withoutItemId.equals(withItemId));
  }

  @Test
  public void testEqualsIsSymmetricWhenOnlyOneVersionIdIsNull() {
    ElementContext withVersionId = new ElementContext(new Id("item-1"), new Id("version-2"));
    ElementContext withoutVersionId = new ElementContext(new Id("item-1"), null);
    Assert.assertFalse(withVersionId.equals(withoutVersionId));
    Assert.assertFalse(withoutVersionId.equals(withVersionId));
  }

  @Test
  public void testEqualsAndHashCodeOfEmptyContexts() {
    Assert.assertEquals(new ElementContext(), new ElementContext());
    Assert.assertEquals(new ElementContext().hashCode(), new ElementContext().hashCode());
  }

  @Test
  public void testEqualsOfNull() {
    Assert.assertFalse(new ElementContext("item-1", "version-2").equals(null));
  }

  @Test
  public void testEqualsOfOtherType() {
    Assert.assertFalse(new ElementContext("item-1", "version-2").equals("item-1"));
  }
}
