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

public class IdTest {

  @Test
  public void testZeroValue() {
    Assert.assertEquals(Id.ZERO.getValue(), "00000000000000000000000000000000");
  }

  @Test
  public void testDefaultConstructorGeneratesDashlessUuid() {
    Id id = new Id();
    Assert.assertEquals(id.getValue().length(), 32);
    Assert.assertFalse(id.getValue().contains("-"));
  }

  @Test
  public void testDefaultConstructorGeneratesDistinctValues() {
    Assert.assertNotEquals(new Id().getValue(), new Id().getValue());
  }

  @Test
  public void testValueConstructorKeepsValueAsIs() {
    Assert.assertEquals(new Id("abc").getValue(), "abc");
  }

  @Test
  public void testSetValue() {
    Id id = new Id("before");
    id.setValue("after");
    Assert.assertEquals(id.getValue(), "after");
  }

  @Test
  public void testToStringOfValue() {
    Assert.assertEquals(new Id("abc").toString(), "abc");
  }

  @Test
  public void testToStringOfNullValue() {
    Assert.assertEquals(new Id(null).toString(), "");
  }

  @Test
  public void testEqualsIsReflexive() {
    Id id = new Id("abc");
    Assert.assertTrue(id.equals(id));
  }

  @Test
  public void testEqualsAndHashCodeOfSameValue() {
    Id one = new Id("abc");
    Id other = new Id("abc");
    Assert.assertEquals(one, other);
    Assert.assertEquals(other, one);
    Assert.assertEquals(one.hashCode(), other.hashCode());
  }

  @Test
  public void testEqualsOfDifferentValue() {
    Assert.assertNotEquals(new Id("abc"), new Id("xyz"));
    Assert.assertNotEquals(new Id("xyz"), new Id("abc"));
  }

  @Test
  public void testEqualsOfNull() {
    Assert.assertFalse(new Id("abc").equals(null));
  }

  @Test
  public void testEqualsOfOtherType() {
    Assert.assertFalse(new Id("abc").equals("abc"));
  }

  @Test
  public void testEqualsAndHashCodeOfNullValue() {
    Id one = new Id(null);
    Id other = new Id(null);
    Assert.assertEquals(one, other);
    Assert.assertEquals(one.hashCode(), 0);
    Assert.assertEquals(other.hashCode(), 0);
  }

  @Test
  public void testEqualsIsSymmetricWhenOnlyOneValueIsNull() {
    Assert.assertFalse(new Id(null).equals(new Id("abc")));
    Assert.assertFalse(new Id("abc").equals(new Id(null)));
  }

  @Test
  public void testHashCodeMatchesValueHashCode() {
    Assert.assertEquals(new Id("abc").hashCode(), "abc".hashCode());
  }
}
