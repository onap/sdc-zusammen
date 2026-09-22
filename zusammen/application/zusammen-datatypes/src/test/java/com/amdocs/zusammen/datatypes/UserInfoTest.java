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

public class UserInfoTest {

  @Test
  public void testDefaultConstructorLeavesUserNameNull() {
    Assert.assertNull(new UserInfo().getUserName());
  }

  @Test
  public void testUserNameConstructor() {
    Assert.assertEquals(new UserInfo("dana").getUserName(), "dana");
  }

  @Test
  public void testSetUserName() {
    UserInfo user = new UserInfo();
    user.setUserName("dana");
    Assert.assertEquals(user.getUserName(), "dana");
  }

  @Test
  public void testEqualsIsReflexive() {
    UserInfo user = new UserInfo("dana");
    Assert.assertTrue(user.equals(user));
  }

  @Test
  public void testEqualsAndHashCodeOfSameUserName() {
    UserInfo one = new UserInfo("dana");
    UserInfo other = new UserInfo("dana");
    Assert.assertEquals(one, other);
    Assert.assertEquals(other, one);
    Assert.assertEquals(one.hashCode(), other.hashCode());
  }

  @Test
  public void testEqualsOfDifferentUserName() {
    Assert.assertNotEquals(new UserInfo("dana"), new UserInfo("sam"));
  }

  @Test
  public void testEqualsAndHashCodeOfNullUserName() {
    Assert.assertEquals(new UserInfo(), new UserInfo());
    Assert.assertEquals(new UserInfo().hashCode(), 0);
  }

  @Test
  public void testEqualsIsSymmetricWhenOnlyOneUserNameIsNull() {
    Assert.assertFalse(new UserInfo().equals(new UserInfo("dana")));
    Assert.assertFalse(new UserInfo("dana").equals(new UserInfo()));
  }

  @Test
  public void testEqualsOfNull() {
    Assert.assertFalse(new UserInfo("dana").equals(null));
  }

  @Test
  public void testEqualsOfOtherType() {
    Assert.assertFalse(new UserInfo("dana").equals("dana"));
  }
}
