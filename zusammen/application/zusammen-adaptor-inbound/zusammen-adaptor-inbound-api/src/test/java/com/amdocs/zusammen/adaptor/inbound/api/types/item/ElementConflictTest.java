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

package com.amdocs.zusammen.adaptor.inbound.api.types.item;

import com.amdocs.zusammen.datatypes.Id;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ElementConflictTest {

  @Test
  public void testElementsDefaultToUnset() {
    ElementConflict conflict = new ElementConflict();

    Assert.assertNull(conflict.getLocalElement());
    Assert.assertNull(conflict.getRemoteElement());
  }

  @Test
  public void testLocalAndRemoteElementsAreKeptApart() {
    ElementConflict conflict = new ElementConflict();

    conflict.setLocalElement(element("local"));
    conflict.setRemoteElement(element("remote"));

    Assert.assertEquals(conflict.getLocalElement().getElementId().getValue(), "local");
    Assert.assertEquals(conflict.getRemoteElement().getElementId().getValue(), "remote");
  }

  private Element element(String elementId) {
    ZusammenElement element = new ZusammenElement();
    element.setElementId(new Id(elementId));
    return element;
  }
}
