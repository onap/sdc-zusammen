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

package com.amdocs.zusammen.sdk.searchindex.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.searchindex.SearchCriteria;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ElementSearchCriteriaTest {

  @Test
  public void testCriteriaDefaultsToUnset() {
    ElementSearchCriteria criteria = new ElementSearchCriteria();

    Assert.assertNull(criteria.getSearchCriteria());
    Assert.assertNull(criteria.getElementId());
  }

  @Test
  public void testSetSearchCriteriaAndElementId() {
    ElementSearchCriteria criteria = new ElementSearchCriteria();
    SearchCriteria searchCriteria = new SearchCriteria() {
    };
    Id elementId = new Id("element-1");

    criteria.setSearchCriteria(searchCriteria);
    criteria.setElementId(elementId);

    Assert.assertSame(criteria.getSearchCriteria(), searchCriteria);
    Assert.assertEquals(criteria.getElementId(), elementId);
  }
}
