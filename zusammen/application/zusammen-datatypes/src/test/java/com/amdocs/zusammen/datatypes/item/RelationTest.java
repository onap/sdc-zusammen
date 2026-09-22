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

public class RelationTest {

  @Test
  public void testFreshRelationHasNoFieldsSet() {
    Relation relation = new Relation();
    Assert.assertNull(relation.getType());
    Assert.assertNull(relation.getInfo());
    Assert.assertNull(relation.getEdge1());
    Assert.assertNull(relation.getEdge2());
    Assert.assertNull(relation.getDirection());
  }

  @Test
  public void testRoundTripOfAllFields() {
    Relation relation = new Relation();
    Info info = new Info();
    info.setName("relation-info");
    RelationEdge edge1 = new RelationEdge();
    edge1.setElementId(new Id("element-1"));
    RelationEdge edge2 = new RelationEdge();
    edge2.setElementId(new Id("element-2"));

    relation.setType("relation-type");
    relation.setInfo(info);
    relation.setEdge1(edge1);
    relation.setEdge2(edge2);
    relation.setDirection(RelationDirection.TWO_TO_ONE);

    Assert.assertEquals(relation.getType(), "relation-type");
    Assert.assertSame(relation.getInfo(), info);
    Assert.assertSame(relation.getEdge1(), edge1);
    Assert.assertSame(relation.getEdge2(), edge2);
    Assert.assertSame(relation.getDirection(), RelationDirection.TWO_TO_ONE);
  }
}
