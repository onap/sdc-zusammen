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
package com.amdocs.zusammen.commons.configuration.datatypes;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigurationInfoTest {

    @Test
    public void testLevelStartsAtZeroAndConfigurationStartsUnset() {
        ConfigurationInfo configurationInfo = new ConfigurationInfo();

        Assert.assertEquals(configurationInfo.getLevel(), 0);
        Assert.assertNull(configurationInfo.getConfiguration());
    }

    @Test
    public void testSettersRoundTripLevelAndConfiguration() {
        Configuration configuration = new Configuration();
        configuration.getProperties().put("cassandra.keyspace", "zusammen");

        ConfigurationInfo configurationInfo = new ConfigurationInfo();
        configurationInfo.setLevel(7);
        configurationInfo.setConfiguration(configuration);

        Assert.assertEquals(configurationInfo.getLevel(), 7);
        Assert.assertSame(configurationInfo.getConfiguration(), configuration);
    }
}
