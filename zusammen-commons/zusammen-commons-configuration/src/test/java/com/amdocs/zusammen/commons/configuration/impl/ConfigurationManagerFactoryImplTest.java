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
package com.amdocs.zusammen.commons.configuration.impl;

import com.amdocs.zusammen.commons.configuration.ConfigurationManager;
import com.amdocs.zusammen.commons.configuration.ConfigurationManagerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigurationManagerFactoryImplTest {

    @Test
    public void testGetInstanceResolvesTheFactoryDeclaredInFactoryConfiguration() {
        ConfigurationManagerFactory factory = ConfigurationManagerFactory.getInstance();

        Assert.assertTrue(factory instanceof ConfigurationManagerFactoryImpl);
    }

    @Test
    public void testCreateInterfaceReturnsAConfigurationManager() {
        ConfigurationManager manager = new ConfigurationManagerFactoryImpl().createInterface();

        Assert.assertTrue(manager instanceof ConfigurationManagerImpl);
    }

    @Test
    public void testCreateInterfaceReturnsTheSameManagerForEveryFactoryInstance() {
        ConfigurationManager first = new ConfigurationManagerFactoryImpl().createInterface();
        ConfigurationManager second = new ConfigurationManagerFactoryImpl().createInterface();

        Assert.assertSame(second, first);
    }
}
