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

import com.amdocs.zusammen.datatypes.Id;

import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RevisionTest {

  @Test
  public void testFreshRevisionHasNoFieldsSet() {
    Revision revision = new Revision();
    Assert.assertNull(revision.getRevisionId());
    Assert.assertNull(revision.getMessage());
    Assert.assertNull(revision.getUser());
    Assert.assertNull(revision.getTime());
  }

  @Test
  public void testRoundTripOfAllFields() {
    Revision revision = new Revision();
    Id revisionId = new Id("revision-1");
    Date time = new Date(1_000L);

    revision.setRevisionId(revisionId);
    revision.setMessage("revision-message");
    revision.setUser("revision-user");
    revision.setTime(time);

    Assert.assertSame(revision.getRevisionId(), revisionId);
    Assert.assertEquals(revision.getMessage(), "revision-message");
    Assert.assertEquals(revision.getUser(), "revision-user");
    Assert.assertEquals(revision.getTime(), time);
  }
}
