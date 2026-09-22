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

package com.amdocs.zusammen.sdk.collaboration.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.item.Action;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CollaborationElementChangeTest {

  @Test
  public void testChangeDefaultsToUnset() {
    CollaborationElementChange change = new CollaborationElementChange();

    Assert.assertNull(change.getElement());
    Assert.assertNull(change.getAction());
  }

  @Test
  public void testSetElementAndAction() {
    CollaborationElementChange change = new CollaborationElementChange();
    CollaborationElement element = new CollaborationElement(new Id("item-1"), new Id("version-2"),
        new Namespace(), new Id("element-3"));

    change.setElement(element);
    change.setAction(Action.UPDATE);

    Assert.assertSame(change.getElement(), element);
    Assert.assertEquals(change.getAction(), Action.UPDATE);
  }
}
