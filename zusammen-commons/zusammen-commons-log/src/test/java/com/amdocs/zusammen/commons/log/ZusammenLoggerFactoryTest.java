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
package com.amdocs.zusammen.commons.log;

import com.amdocs.zusammen.commons.log.impl.ZusammenSLF4JLoggerImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ZusammenLoggerFactoryTest {

    @Test
    public void testGetLoggerResolvesTheFactoryDeclaredInFactoryConfiguration() {
        ZusammenLogger logger = ZusammenLoggerFactory.getLogger(getClass().getName());

        Assert.assertTrue(logger instanceof ZusammenSLF4JLoggerImpl);
    }

    /**
     * getLogger() works without the caller ever calling init(): AbstractFactoryBase invokes init()
     * itself when it instantiates the factory, which is the only thing that lifts the "must be
     * initialized" guard in ZusammenSLF4JLoggerFactoryImpl.
     */
    @Test
    public void testGetLoggerReturnsAnIndependentLoggerPerCall() {
        ZusammenLogger first = ZusammenLoggerFactory.getLogger("com.amdocs.zusammen.ItemA");
        ZusammenLogger second = ZusammenLoggerFactory.getLogger("com.amdocs.zusammen.ItemB");

        Assert.assertNotNull(first);
        Assert.assertNotSame(second, first);
    }
}
