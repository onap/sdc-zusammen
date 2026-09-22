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
import java.util.Collection;
import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ItemVersionDataTest {

  @Test
  public void testFreshDataHasNoInfoAndNoRelations() {
    ItemVersionData data = new ItemVersionData();
    Assert.assertNull(data.getInfo());
    Assert.assertNull(data.getRelations());
  }

  @Test
  public void testRoundTripOfAllFields() {
    ItemVersionData data = new ItemVersionData();
    Info info = new Info();
    info.setName("version-info");
    Relation first = new Relation();
    first.setType("first-type");
    Relation second = new Relation();
    second.setType("second-type");
    Collection<Relation> relations = Arrays.asList(first, second);

    data.setInfo(info);
    data.setRelations(relations);

    Assert.assertSame(data.getInfo(), info);
    Assert.assertEquals(data.getRelations().size(), 2);
    Assert.assertSame(data.getRelations(), relations);
  }

  @Test
  public void testRelationsCanBeEmpty() {
    ItemVersionData data = new ItemVersionData();
    data.setRelations(Collections.<Relation>emptyList());
    Assert.assertTrue(data.getRelations().isEmpty());
  }
}
