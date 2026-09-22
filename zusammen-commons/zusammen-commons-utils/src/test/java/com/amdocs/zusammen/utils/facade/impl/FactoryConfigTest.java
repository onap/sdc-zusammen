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

public class FactoryConfigTest {

    private FactoryStaticState state;

    @BeforeMethod
    public void isolateStaticFactoryState() {
        state = FactoryStaticState.capture();
    }

    @AfterMethod
    public void restoreStaticFactoryState() {
        state.restore();
    }

    @Test
    public void testGetFactoriesMapDelegatesToFactoriesConfigImpl() {
        FactoryStaticState.setFactoriesConfig(
                Collections.singletonMap("com.example.Factory", "com.example.FactoryImpl"));

        Assert.assertSame(FactoryConfig.getFactoriesMap(), new FactoriesConfigImpl().getFactoriesMap());
        Assert.assertEquals(FactoryConfig.getFactoriesMap().get("com.example.Factory"),
                "com.example.FactoryImpl");
    }
}
