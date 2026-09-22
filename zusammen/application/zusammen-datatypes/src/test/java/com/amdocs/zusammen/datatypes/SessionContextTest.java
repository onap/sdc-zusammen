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

public class SessionContextTest {

  private static SessionContext sessionContext(String userName, String tenant) {
    SessionContext context = new SessionContext();
    context.setUser(userName == null ? null : new UserInfo(userName));
    context.setTenant(tenant);
    return context;
  }

  @Test
  public void testFreshContextHasNoUserAndNoTenant() {
    SessionContext context = new SessionContext();
    Assert.assertNull(context.getUser());
    Assert.assertNull(context.getTenant());
  }

  @Test
  public void testRoundTrip() {
    SessionContext context = new SessionContext();
    UserInfo user = new UserInfo("session-user");
    context.setUser(user);
    context.setTenant("session-tenant");
    Assert.assertSame(context.getUser(), user);
    Assert.assertEquals(context.getTenant(), "session-tenant");
  }

  @Test
  public void testEqualsIsReflexive() {
    SessionContext context = sessionContext("u", "t");
    Assert.assertTrue(context.equals(context));
  }

  @Test
  public void testEqualsAndHashCodeOfEqualContexts() {
    SessionContext one = sessionContext("u", "t");
    SessionContext other = sessionContext("u", "t");
    Assert.assertEquals(one, other);
    Assert.assertEquals(other, one);
    Assert.assertEquals(one.hashCode(), other.hashCode());
  }

  @Test
  public void testEqualsOfDifferentUser() {
    Assert.assertNotEquals(sessionContext("u", "t"), sessionContext("other", "t"));
  }

  @Test
  public void testEqualsOfDifferentTenant() {
    Assert.assertNotEquals(sessionContext("u", "t"), sessionContext("u", "other"));
  }

  @Test
  public void testEqualsIsSymmetricWhenOnlyOneUserIsNull() {
    Assert.assertFalse(sessionContext(null, "t").equals(sessionContext("u", "t")));
    Assert.assertFalse(sessionContext("u", "t").equals(sessionContext(null, "t")));
  }

  @Test
  public void testEqualsIsSymmetricWhenOnlyOneTenantIsNull() {
    Assert.assertFalse(sessionContext("u", null).equals(sessionContext("u", "t")));
    Assert.assertFalse(sessionContext("u", "t").equals(sessionContext("u", null)));
  }

  @Test
  public void testEqualsAndHashCodeOfEmptyContexts() {
    SessionContext one = new SessionContext();
    SessionContext other = new SessionContext();
    Assert.assertEquals(one, other);
    Assert.assertEquals(one.hashCode(), other.hashCode());
  }

  @Test
  public void testEqualsOfNull() {
    Assert.assertFalse(sessionContext("u", "t").equals(null));
  }

  @Test
  public void testEqualsOfOtherType() {
    Assert.assertFalse(sessionContext("u", "t").equals("u"));
  }
}
