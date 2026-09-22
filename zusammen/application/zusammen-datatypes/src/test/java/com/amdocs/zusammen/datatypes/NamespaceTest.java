/*
 * Add Copyright © 2016-2017 European Support Limited
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

/**
 * {@link Namespace#ROOT_NAMESPACE} is a shared mutable singleton and {@code setValue} is public, so
 * nothing here may call it on {@code ROOT_NAMESPACE} — that would corrupt every later namespace
 * computation in the JVM, including other test classes'.
 */
public class NamespaceTest {

  @Test
  public void testGetValueOfRoot() throws Exception {
    Namespace namespace = new Namespace();
    Assert.assertEquals(namespace.getValue(), Namespace.ROOT_NAMESPACE.getValue());
  }

  @Test
  public void testGetValueDirectOfSub() throws Exception {
    Id parentElementId = new Id();
    Namespace namespace = new Namespace(new Namespace(), parentElementId);
    Assert.assertEquals(namespace.getValue(), parentElementId.toString());
  }

  @Test
  public void testGetValueOfSub() throws Exception {
    Id grandparentElementId = new Id();
    Id parentElementId = new Id();
    Namespace namespace =
        new Namespace(new Namespace(new Namespace(), grandparentElementId), parentElementId);
    Assert.assertEquals(namespace.getValue(), grandparentElementId.toString() + Namespace
        .NAMESPACE_DELIMITER + parentElementId.toString());
  }

  @Test
  public void testSetValidValue() throws Exception {
    Namespace namespace = new Namespace();
    namespace.setValue("a/b");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetInvalidValue() throws Exception {
    Namespace namespace = new Namespace();
    namespace.setValue(null);
  }

  @Test
  public void testGetParentElementIdOfRoot() throws Exception {
    Namespace namespace = new Namespace();
    Assert.assertNull(namespace.getParentElementId());
  }

  @Test
  public void testGetParentElementIdOfDirectSub() throws Exception {
    Id parentElementId = new Id();
    Namespace namespace = new Namespace(new Namespace(), parentElementId);
    Assert.assertEquals(namespace.getParentElementId(), parentElementId);
  }

  @Test
  public void testGetParentElementIdOfSub() throws Exception {
    Id grandparentElementId = new Id();
    Id parentElementId = new Id();
    Namespace namespace =
        new Namespace(new Namespace(new Namespace(), grandparentElementId), parentElementId);
    Assert.assertEquals(namespace.getParentElementId(), parentElementId);
  }

  @Test
  public void testRootNamespaceValueIsEmpty() {
    Assert.assertEquals(Namespace.ROOT_NAMESPACE.getValue(), "");
  }

  @Test
  public void testFreshNamespaceEqualsRootNamespace() {
    Assert.assertEquals(new Namespace(), Namespace.ROOT_NAMESPACE);
    Assert.assertEquals(new Namespace().hashCode(), Namespace.ROOT_NAMESPACE.hashCode());
  }

  @Test
  public void testDelimiterIsSlash() {
    Assert.assertEquals(Namespace.NAMESPACE_DELIMITER, "/");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConstructorRejectsNullParentNamespace() {
    new Namespace(null, new Id("a"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConstructorRejectsNullParentElementId() {
    new Namespace(new Namespace(), null);
  }

  @Test
  public void testConstructorFromNonRootParentUsesDelimiter() {
    Namespace parent = new Namespace();
    parent.setValue("a/b");
    Assert.assertEquals(new Namespace(parent, new Id("c")).getValue(), "a/b/c");
  }

  @Test
  public void testToStringIsTheValue() {
    Namespace namespace = new Namespace();
    namespace.setValue("a/b");
    Assert.assertEquals(namespace.toString(), "a/b");
  }

  @Test
  public void testEqualsIsReflexive() {
    Namespace namespace = new Namespace(new Namespace(), new Id("a"));
    Assert.assertTrue(namespace.equals(namespace));
  }

  @Test
  public void testEqualsAndHashCodeOfSameValue() {
    Namespace one = new Namespace(new Namespace(), new Id("a"));
    Namespace other = new Namespace(new Namespace(), new Id("a"));
    Assert.assertEquals(one, other);
    Assert.assertEquals(other, one);
    Assert.assertEquals(one.hashCode(), other.hashCode());
  }

  @Test
  public void testEqualsOfDifferentValue() {
    Namespace one = new Namespace(new Namespace(), new Id("a"));
    Namespace other = new Namespace(new Namespace(), new Id("b"));
    Assert.assertNotEquals(one, other);
    Assert.assertNotEquals(other, one);
  }

  @Test
  public void testEqualsOfNull() {
    Assert.assertFalse(new Namespace().equals(null));
  }

  @Test
  public void testEqualsOfOtherType() {
    Assert.assertFalse(new Namespace().equals(""));
  }

  @Test
  public void testGetParentElementIdOfValueWithoutDelimiter() {
    Namespace namespace = new Namespace();
    namespace.setValue("lonely");
    Assert.assertEquals(namespace.getParentElementId(), new Id("lonely"));
  }

  @Test
  public void testGetParentElementIdOfDeepValue() {
    Namespace namespace = new Namespace();
    namespace.setValue("a/b/c");
    Assert.assertEquals(namespace.getParentElementId(), new Id("c"));
  }

}
