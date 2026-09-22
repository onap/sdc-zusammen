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
package com.amdocs.zusammen.commons.log.impl;

import com.amdocs.zusammen.commons.log.ZusammenLogger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ZusammenSLF4JLoggerFactoryImplTest {

    private static final String KEY = "com.amdocs.zusammen.core.impl.item.ItemManagerImpl";

    private ZusammenSLF4JLoggerFactoryImpl factory;

    @BeforeMethod
    public void setUp() {
        factory = new ZusammenSLF4JLoggerFactoryImpl();
    }

    @Test
    public void testCreateInterfaceBeforeInitFails() {
        try {
            factory.createInterface(KEY);
            Assert.fail("expected a RuntimeException for an uninitialised factory");
        } catch (RuntimeException re) {
            Assert.assertEquals(re.getMessage(),
                    "Logget must be initialized before executing logger. Run method init()");
        }
    }

    @Test
    public void testCreateInterfaceAfterInitReturnsAnSlf4jBackedLogger() {
        factory.init();

        ZusammenLogger logger = factory.createInterface(KEY);

        Assert.assertTrue(logger instanceof ZusammenSLF4JLoggerImpl);
    }
}
