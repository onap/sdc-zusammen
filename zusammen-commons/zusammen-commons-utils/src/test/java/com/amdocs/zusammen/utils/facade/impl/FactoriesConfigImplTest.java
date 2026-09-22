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

package com.amdocs.zusammen.utils.facade.impl;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

public class FactoriesConfigImplTest {

    private FactoryStaticState state;

    @BeforeMethod
    public void isolateStaticFactoryState() {
        state = FactoryStaticState.capture();
        FactoryStaticState.resetFactoriesConfig();
    }

    @AfterMethod
    public void restoreStaticFactoryState() {
        state.restore();
    }

    /**
     * factoryConfiguration.json is merged from every copy on the classpath, so a stray copy under
     * src/test/resources of any module would silently steer factory resolution everywhere.
     */
    @Test
    public void testGetFactoriesMapIsEmptyWhenNoConfigurationIsOnTheClasspath() {
        Assert.assertTrue(new FactoriesConfigImpl().getFactoriesMap().isEmpty());
    }

    @Test
    public void testGetFactoriesMapIsInitialisedOnlyOnceAndSharedAcrossInstances() {
        FactoriesConfigImpl first = new FactoriesConfigImpl();
        Map<String, String> initial = first.getFactoriesMap();
        initial.put("marker", "value");

        Map<String, String> reread = new FactoriesConfigImpl().getFactoriesMap();

        Assert.assertSame(reread, initial);
        Assert.assertEquals(reread.get("marker"), "value");
    }

    @Test
    public void testGetFactoriesMapReturnsWhateverWasConfigured() {
        FactoryStaticState.setFactoriesConfig(
                Collections.singletonMap("com.example.Factory", "com.example.FactoryImpl"));

        Assert.assertEquals(new FactoriesConfigImpl().getFactoriesMap().get("com.example.Factory"),
                "com.example.FactoryImpl");
    }
}
