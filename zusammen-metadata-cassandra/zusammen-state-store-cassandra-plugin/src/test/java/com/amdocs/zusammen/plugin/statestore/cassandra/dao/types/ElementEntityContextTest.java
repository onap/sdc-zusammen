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

package com.amdocs.zusammen.plugin.statestore.cassandra.dao.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ElementEntityContextTest {
  private static final String SPACE = "space";

  @Test
  public void testConstructFromElementContext() {
    ElementContext elementContext = new ElementContext();
    elementContext.setItemId(new Id("item"));
    elementContext.setVersionId(new Id("version"));
    elementContext.setRevisionId(new Id("revision"));

    ElementEntityContext context = new ElementEntityContext(SPACE, elementContext);

    Assert.assertEquals(context.getSpace(), SPACE);
    Assert.assertEquals(context.getItemId(), new Id("item"));
    Assert.assertEquals(context.getVersionId(), new Id("version"));
    Assert.assertEquals(context.getRevisionId(), new Id("revision"));
  }

  @Test
  public void testConstructWithoutRevisionId() {
    ElementEntityContext context =
        new ElementEntityContext(SPACE, new Id("item"), new Id("version"));

    Assert.assertEquals(context.getSpace(), SPACE);
    Assert.assertEquals(context.getItemId(), new Id("item"));
    Assert.assertEquals(context.getVersionId(), new Id("version"));
    Assert.assertNull(context.getRevisionId());
  }

  @Test
  public void testConstructWithRevisionId() {
    ElementEntityContext context = new ElementEntityContext(SPACE, new Id("item"),
        new Id("version"), new Id("revision"));

    Assert.assertEquals(context.getRevisionId(), new Id("revision"));
  }

  @Test
  public void testEverySetterIsReadBackByItsGetter() {
    ElementEntityContext context =
        new ElementEntityContext("original", new Id("original"), new Id("original"));

    context.setSpace(SPACE);
    context.setItemId(new Id("item"));
    context.setVersionId(new Id("version"));
    context.setRevisionId(new Id("revision"));

    Assert.assertEquals(context.getSpace(), SPACE);
    Assert.assertEquals(context.getItemId(), new Id("item"));
    Assert.assertEquals(context.getVersionId(), new Id("version"));
    Assert.assertEquals(context.getRevisionId(), new Id("revision"));
  }

  @Test
  public void testEqualsOfIdenticalValues() {
    ElementEntityContext context = create(SPACE, "item", "version");
    ElementEntityContext other = create(SPACE, "item", "version");

    Assert.assertEquals(context, other);
    Assert.assertEquals(context.hashCode(), other.hashCode());
  }

  @Test
  public void testEqualsOfTheSameInstance() {
    ElementEntityContext context = create(SPACE, "item", "version");

    Assert.assertEquals(context, context);
  }

  @Test
  public void testEqualsOfDifferentSpace() {
    Assert.assertNotEquals(create(SPACE, "item", "version"),
        create("other space", "item", "version"));
  }

  @Test
  public void testEqualsOfDifferentItemId() {
    Assert.assertNotEquals(create(SPACE, "item", "version"),
        create(SPACE, "other item", "version"));
  }

  @Test
  public void testEqualsOfDifferentVersionId() {
    Assert.assertNotEquals(create(SPACE, "item", "version"),
        create(SPACE, "item", "other version"));
  }

  @Test
  public void testEqualsWhenAllFieldsAreNullOnBothSides() {
    ElementEntityContext context = new ElementEntityContext(null, null, null);
    ElementEntityContext other = new ElementEntityContext(null, null, null);

    Assert.assertEquals(context, other);
    Assert.assertEquals(context.hashCode(), other.hashCode());
  }

  @Test
  public void testEqualsWhenOnlyOneSideHasASpace() {
    Assert.assertNotEquals(new ElementEntityContext(null, new Id("item"), new Id("version")),
        create(SPACE, "item", "version"));
    Assert.assertNotEquals(create(SPACE, "item", "version"),
        new ElementEntityContext(null, new Id("item"), new Id("version")));
  }

  @Test
  public void testEqualsWhenOnlyOneSideHasAnItemId() {
    Assert.assertNotEquals(new ElementEntityContext(SPACE, null, new Id("version")),
        create(SPACE, "item", "version"));
    Assert.assertNotEquals(create(SPACE, "item", "version"),
        new ElementEntityContext(SPACE, null, new Id("version")));
  }

  @Test
  public void testEqualsWhenOnlyOneSideHasAVersionId() {
    Assert.assertNotEquals(new ElementEntityContext(SPACE, new Id("item"), null),
        create(SPACE, "item", "version"));
    Assert.assertNotEquals(create(SPACE, "item", "version"),
        new ElementEntityContext(SPACE, new Id("item"), null));
  }

  @Test
  public void testEqualsOfNullAndOfAnotherType() {
    ElementEntityContext context = create(SPACE, "item", "version");

    Assert.assertFalse(context.equals(null));
    Assert.assertFalse(context.equals(SPACE));
  }

  private ElementEntityContext create(String space, String itemId, String versionId) {
    return new ElementEntityContext(space, new Id(itemId), new Id(versionId));
  }
}
