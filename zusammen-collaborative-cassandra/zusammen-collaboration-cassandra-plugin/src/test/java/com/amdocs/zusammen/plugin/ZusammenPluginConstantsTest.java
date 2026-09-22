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

package com.amdocs.zusammen.plugin;

import com.amdocs.zusammen.datatypes.Id;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Both constants end up as data in Cassandra - PUBLIC_SPACE is written to the {@code space}
 * partition key column and ROOT_ELEMENTS_PARENT_ID to {@code parent_id} - so changing either value
 * orphans every row already stored under the old one.
 */
public class ZusammenPluginConstantsTest {

    @Test
    public void testPublicSpaceValue() {
        Assert.assertEquals(ZusammenPluginConstants.PUBLIC_SPACE, "public");
    }

    @Test
    public void testRootElementsParentIdValue() {
        Assert.assertEquals(ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID, Id.ZERO);
        Assert.assertEquals(ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID.getValue(),
                "00000000000000000000000000000000");
    }
}
