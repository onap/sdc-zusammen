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

public class ConfigurationTest {

    @Test
    public void testPropertiesAndPluginsStartOutEmptyAndMutable() {
        Configuration configuration = new Configuration();

        Assert.assertTrue(configuration.getProperties().isEmpty());
        Assert.assertTrue(configuration.getPlugins().isEmpty());

        configuration.getProperties().put("cassandra.keyspace", "zusammen");
        configuration.getPlugins().put("zusammen_state_store", new PluginInfo());

        Assert.assertEquals(configuration.getProperties().size(), 1);
        Assert.assertEquals(configuration.getPlugins().size(), 1);
    }

    @Test
    public void testSetPropertiesReplacesTheWholeMap() {
        Configuration configuration = new Configuration();
        configuration.getProperties().put("cassandra.keyspace", "discarded");

        Map<String, Object> properties = new HashMap<>();
        properties.put("cassandra.port", "9042");
        configuration.setProperties(properties);

        Assert.assertEquals(configuration.getProperties().size(), 1);
        Assert.assertEquals(configuration.getProperties().get("cassandra.port"), "9042");
    }

    @Test
    public void testSetPluginsReplacesTheWholeMap() {
        Configuration configuration = new Configuration();
        configuration.getPlugins().put("zusammen_state_store", new PluginInfo());

        PluginInfo searchIndex = new PluginInfo();
        searchIndex.setImplementationClass("com.example.EmptySearchIndex");
        Map<String, PluginInfo> plugins = new HashMap<>();
        plugins.put("zusammen_search_index", searchIndex);
        configuration.setPlugins(plugins);

        Assert.assertEquals(configuration.getPlugins().size(), 1);
        Assert.assertSame(configuration.getPlugins().get("zusammen_search_index"), searchIndex);
    }
}
