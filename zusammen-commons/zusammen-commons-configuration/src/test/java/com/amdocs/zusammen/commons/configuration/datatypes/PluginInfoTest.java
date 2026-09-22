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

import java.util.HashMap;
import java.util.Map;

public class PluginInfoTest {

    @Test
    public void testImplementationClassStartsUnsetAndPropertiesStartEmpty() {
        PluginInfo pluginInfo = new PluginInfo();

        Assert.assertNull(pluginInfo.getImplementationClass());
        Assert.assertTrue(pluginInfo.getProperties().isEmpty());
    }

    @Test
    public void testSettersRoundTripImplementationClassAndProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("cassandra.nodes", "node-1");
        properties.put("cassandra.ssl", "true");

        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setImplementationClass("com.example.CassandraStateStore");
        pluginInfo.setProperties(properties);

        Assert.assertEquals(pluginInfo.getImplementationClass(), "com.example.CassandraStateStore");
        Assert.assertEquals(pluginInfo.getProperties().size(), 2);
        Assert.assertEquals(pluginInfo.getProperties().get("cassandra.nodes"), "node-1");
        Assert.assertEquals(pluginInfo.getProperties().get("cassandra.ssl"), "true");
    }
}
