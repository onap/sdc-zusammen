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

import com.amdocs.zusammen.commons.configuration.datatypes.Configuration;
import com.amdocs.zusammen.commons.configuration.datatypes.PluginInfo;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Optional;

public class ConfigurationManagerImplTest {

    private static final String STATE_STORE = "zusammen_state_store";
    private static final String SYSTEM_PROPERTY = "zusammen.test.cassandra.keyspace";

    private ConfigurationManagerImpl configurationManager;

    @BeforeMethod
    public void setUp() {
        configurationManager = new ConfigurationManagerImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        System.clearProperty(SYSTEM_PROPERTY);
        // The aggregated configuration is held in a static field, so a fixture injected by one test
        // would otherwise be seen by every later test in this JVM.
        setAggregatedConfiguration(new Configuration());
    }

    @Test
    public void testGetPropertyOfUnknownNameIsEmptyWhenNoDescriptorIsOnTheClasspath() {
        Optional<String> property = configurationManager.getProperty("cassandra.keyspace");

        Assert.assertFalse(property.isPresent());
    }

    @Test
    public void testGetPropertyReturnsValueFromAggregatedConfiguration() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getProperties().put("cassandra.keyspace", "zusammen");
        setAggregatedConfiguration(configuration);

        Optional<String> property = configurationManager.getProperty("cassandra.keyspace");

        Assert.assertTrue(property.isPresent());
        Assert.assertEquals(property.get(), "zusammen");
    }

    @Test
    public void testGetPropertyFallsBackToSystemPropertyWhenNotConfigured() {
        System.setProperty(SYSTEM_PROPERTY, "from-jvm");

        Optional<String> property = configurationManager.getProperty(SYSTEM_PROPERTY);

        Assert.assertTrue(property.isPresent());
        Assert.assertEquals(property.get(), "from-jvm");
    }


    @Test
    public void testGetPluginInfoReturnsConfiguredPlugin() throws Exception {
        PluginInfo stateStore = new PluginInfo();
        stateStore.setImplementationClass("com.example.CassandraStateStore");
        stateStore.getProperties().put("cassandra.nodes", "node-1");
        Configuration configuration = new Configuration();
        configuration.getPlugins().put(STATE_STORE, stateStore);
        setAggregatedConfiguration(configuration);

        PluginInfo pluginInfo = configurationManager.getPluginInfo(STATE_STORE);

        Assert.assertEquals(pluginInfo.getImplementationClass(), "com.example.CassandraStateStore");
        Assert.assertEquals(pluginInfo.getProperties().get("cassandra.nodes"), "node-1");
    }

    @Test
    public void testGetPluginInfoOfUnsupportedTypeFails() {
        try {
            configurationManager.getPluginInfo("zusammen_unknown_store");
            Assert.fail("expected a RuntimeException for an unsupported plugin type");
        } catch (RuntimeException re) {
            Assert.assertEquals(re.getMessage(), "Plugin type:zusammen_unknown_store not supported.");
        }
    }


    private static void setAggregatedConfiguration(Configuration configuration) throws Exception {
        Field field = ConfigurationManagerImpl.class.getDeclaredField("configurationInfo");
        field.setAccessible(true);
        field.set(null, configuration);
    }
}
