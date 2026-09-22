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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

public class InfoTest {

  @Test
  public void testFreshInfoHasEmptyProperties() {
    Info info = new Info();
    Assert.assertNotNull(info.getProperties());
    Assert.assertTrue(info.getProperties().isEmpty());
    Assert.assertNull(info.getName());
    Assert.assertNull(info.getDescription());
  }

  @Test
  public void testRoundTrip() {
    Info info = new Info();
    info.setName("info-name");
    info.setDescription("info-description");
    Map<String, Object> properties = new HashMap<>();
    properties.put("key", "value");
    info.setProperties(properties);
    Assert.assertEquals(info.getName(), "info-name");
    Assert.assertEquals(info.getDescription(), "info-description");
    Assert.assertEquals(info.getProperties(), properties);
  }

  @Test
  public void testAddPropertyThenGetProperty() {
    Info info = new Info();
    info.addProperty("stringKey", "stringValue");
    info.addProperty("intKey", 42);
    String stringValue = info.getProperty("stringKey");
    Integer intValue = info.getProperty("intKey");
    Assert.assertEquals(stringValue, "stringValue");
    Assert.assertEquals(intValue, Integer.valueOf(42));
  }

  @Test
  public void testAddPropertyOfCollectionValue() {
    Info info = new Info();
    List<String> value = Arrays.asList("a", "b");
    info.addProperty("listKey", value);
    List<String> retrieved = info.getProperty("listKey");
    Assert.assertEquals(retrieved, value);
  }

  @Test
  public void testAddPropertyOverwritesExistingValue() {
    Info info = new Info();
    info.addProperty("key", "first");
    info.addProperty("key", "second");
    Assert.assertEquals(info.getProperty("key"), "second");
    Assert.assertEquals(info.getProperties().size(), 1);
  }

  @Test
  public void testAddPropertyOfNullValue() {
    Info info = new Info();
    info.addProperty("key", null);
    Assert.assertNull(info.getProperty("key"));
    Assert.assertTrue(info.getProperties().containsKey("key"));
  }

  @Test
  public void testGetPropertyOfUnknownName() {
    Assert.assertNull(new Info().getProperty("absent"));
  }

  /**
   * {@code getProperty} is an unchecked cast, so a type mismatch surfaces at the caller's
   * assignment rather than inside {@code Info}.
   */
  @Test(expectedExceptions = ClassCastException.class)
  public void testGetPropertyOfWrongTypeFailsAtTheCallSite() {
    Info info = new Info();
    info.addProperty("key", "not-a-number");
    Integer wrong = info.getProperty("key");
    Assert.fail("expected a ClassCastException, got " + wrong);
  }

  @Test
  public void testSetPropertiesStoresTheCallerMapWithoutCopying() {
    Info info = new Info();
    Map<String, Object> properties = new HashMap<>();
    info.setProperties(properties);
    properties.put("addedLater", "value");
    Assert.assertSame(info.getProperties(), properties);
    Assert.assertEquals(info.getProperty("addedLater"), "value");
  }

  @Test
  public void testAddPropertyWritesIntoTheMapSetByTheCaller() {
    Info info = new Info();
    Map<String, Object> properties = new HashMap<>();
    info.setProperties(properties);
    info.addProperty("key", "value");
    Assert.assertEquals(properties.get("key"), "value");
  }
}
