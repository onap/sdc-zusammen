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

package com.amdocs.zusammen.sdk;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SdkConstantsTest {

  @Test
  public void testCollaborativeStorePluginTypeKey() {
    Assert.assertEquals(SdkConstants.ZUSAMMEN_COLLABORATIVE_STORE, "zusammen_collaborative_store");
  }

  @Test
  public void testStateStorePluginTypeKey() {
    Assert.assertEquals(SdkConstants.ZUSAMMEN_STATE_STORE, "zusammen_state_store");
  }

  @Test
  public void testSearchIndexPluginTypeKey() {
    Assert.assertEquals(SdkConstants.ZUSAMMEN_SEARCH_INDEX, "zusammen_search_index");
  }
}
