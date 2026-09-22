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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ItemVersionRevisionsTest {

  private static Revision revision(String revisionId) {
    Revision revision = new Revision();
    revision.setRevisionId(new Id(revisionId));
    return revision;
  }

  @Test
  public void testFreshRevisionsIsEmptyNotNull() {
    ItemVersionRevisions revisions = new ItemVersionRevisions();
    Assert.assertNotNull(revisions.getItemVersionRevisions());
    Assert.assertTrue(revisions.getItemVersionRevisions().isEmpty());
  }

  @Test
  public void testAddChangeAppendsInOrder() {
    ItemVersionRevisions revisions = new ItemVersionRevisions();
    revisions.addChange(revision("first"));
    revisions.addChange(revision("second"));

    List<Revision> stored = revisions.getItemVersionRevisions();
    Assert.assertEquals(stored.size(), 2);
    Assert.assertEquals(stored.get(0).getRevisionId(), new Id("first"));
    Assert.assertEquals(stored.get(1).getRevisionId(), new Id("second"));
  }

  @Test
  public void testAddChangeAcceptsDuplicates() {
    ItemVersionRevisions revisions = new ItemVersionRevisions();
    Revision revision = revision("same");
    revisions.addChange(revision);
    revisions.addChange(revision);
    Assert.assertEquals(revisions.getItemVersionRevisions().size(), 2);
  }

  @Test
  public void testSetItemVersionRevisionsReplacesTheList() {
    ItemVersionRevisions revisions = new ItemVersionRevisions();
    revisions.addChange(revision("dropped"));
    revisions.setItemVersionRevisions(Collections.singletonList(revision("kept")));

    Assert.assertEquals(revisions.getItemVersionRevisions().size(), 1);
    Assert.assertEquals(revisions.getItemVersionRevisions().get(0).getRevisionId(), new Id("kept"));
  }

  @Test
  public void testSetItemVersionRevisionsStoresTheCallerListWithoutCopying() {
    ItemVersionRevisions revisions = new ItemVersionRevisions();
    List<Revision> external = new ArrayList<>();
    revisions.setItemVersionRevisions(external);
    revisions.addChange(revision("written-through"));

    Assert.assertSame(revisions.getItemVersionRevisions(), external);
    Assert.assertEquals(external.size(), 1);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testAddChangeOnAnImmutableListPropagatesTheFailure() {
    ItemVersionRevisions revisions = new ItemVersionRevisions();
    revisions.setItemVersionRevisions(Collections.<Revision>emptyList());
    revisions.addChange(revision("rejected"));
  }
}
