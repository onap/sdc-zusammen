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
import com.amdocs.zusammen.commons.configuration.datatypes.PluginInfo;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurationAccessorTest {

    private static final String STATE_STORE = "zusammen_state_store";
    private static final String KEYSPACE = "zusammen.test.cassandra.keyspace";

    private static ConfigurationManager configurationManager;

    /**
     * Instantiated reflectively by AbstractFactoryBase, so it has to be public and static with a
     * public no-arg constructor.
     */
    public static class StubConfigurationManagerFactory extends ConfigurationManagerFactory {
        @Override
        public ConfigurationManager createInterface() {
            return configurationManager;
        }
    }

    @BeforeMethod
    public void setUp() {
        configurationManager = mock(ConfigurationManager.class);
        // AbstractComponentFactory's static initialiser re-registers the production mapping read
        // from factoryConfiguration.json. A class literal does not initialise the class, so the
        // factory has to be touched here — otherwise that initialiser runs on the first
        // getInstance() call and silently overwrites the stub registered below.
        ConfigurationManagerFactory.getInstance();
        AbstractFactoryBase.registerFactory(ConfigurationManagerFactory.class,
                StubConfigurationManagerFactory.class);
    }

    @AfterMethod
    public void tearDown() {
        // The factory registry and its instance cache are static, so the production mapping has to
        // be put back or every later test class resolves the stub instead.
        AbstractFactoryBase.registerFactory(ConfigurationManagerFactory.class,
                ConfigurationManagerFactoryImpl.class);
        System.clearProperty(KEYSPACE);
        configurationManager = null;
    }

    @Test
    public void testGetPropertyReturnsConfiguredValue() {
        when(configurationManager.getProperty(KEYSPACE)).thenReturn(Optional.of("zusammen"));

        String value = ConfigurationAccessor.getProperty(KEYSPACE);

        Assert.assertEquals(value, "zusammen");
    }

    @Test
    public void testGetPropertyPrefersSystemPropertyOverConfiguredValue() {
        when(configurationManager.getProperty(KEYSPACE)).thenReturn(Optional.of("from-file"));
        System.setProperty(KEYSPACE, "from-jvm");

        String value = ConfigurationAccessor.getProperty(KEYSPACE);

        Assert.assertEquals(value, "from-jvm");
    }

    @Test
    public void testGetPropertyOfUnknownNameFails() {
        when(configurationManager.getProperty(KEYSPACE)).thenReturn(Optional.empty());

        try {
            String ignored = ConfigurationAccessor.getProperty(KEYSPACE);
            Assert.fail("expected a RuntimeException, got " + ignored);
        } catch (RuntimeException re) {
            Assert.assertEquals(re.getMessage(), "property " + KEYSPACE + " does not exist");
        }
    }

    @Test
    public void testGetOptionalPropertyOfUnknownNameIsEmpty() {
        when(configurationManager.getProperty(KEYSPACE)).thenReturn(Optional.empty());

        Optional<String> value = ConfigurationAccessor.getOptionalProperty(KEYSPACE);

        Assert.assertFalse(value.isPresent());
    }

    @Test
    public void testGetPropertyWithDefaultReturnsConfiguredValue() {
        when(configurationManager.getProperty(KEYSPACE)).thenReturn(Optional.of("zusammen"));

        String value = ConfigurationAccessor.getProperty(KEYSPACE, "fallback");

        Assert.assertEquals(value, "zusammen");
    }

    @Test
    public void testGetPropertyWithDefaultReturnsDefaultWhenNotConfigured() {
        when(configurationManager.getProperty(KEYSPACE)).thenReturn(Optional.empty());

        String value = ConfigurationAccessor.getProperty(KEYSPACE, "fallback");

        Assert.assertEquals(value, "fallback");
    }

    @Test
    public void testGetPropertyWithDefaultReturnsDefaultWhenLookupFails() {
        when(configurationManager.getProperty(KEYSPACE))
                .thenThrow(new RuntimeException("no configuration file"));

        String value = ConfigurationAccessor.getProperty(KEYSPACE, "fallback");

        Assert.assertEquals(value, "fallback");
    }

    @Test
    public void testGetPluginPropertyReturnsConfiguredValue() {
        when(configurationManager.getPluginInfo(STATE_STORE))
                .thenReturn(pluginInfo("cassandra.nodes", "node-1"));

        String value = ConfigurationAccessor.getPluginProperty(STATE_STORE, "cassandra.nodes");

        Assert.assertEquals(value, "node-1");
    }

    @Test
    public void testGetPluginPropertyPrefersSystemPropertyOverConfiguredValue() {
        when(configurationManager.getPluginInfo(STATE_STORE))
                .thenReturn(pluginInfo(KEYSPACE, "from-file"));
        System.setProperty(KEYSPACE, "from-jvm");

        String value = ConfigurationAccessor.getPluginProperty(STATE_STORE, KEYSPACE);

        Assert.assertEquals(value, "from-jvm");
    }

    @Test
    public void testGetPluginPropertyOfUnknownNameFails() {
        when(configurationManager.getPluginInfo(STATE_STORE))
                .thenReturn(pluginInfo("cassandra.nodes", "node-1"));

        try {
            String ignored = ConfigurationAccessor.getPluginProperty(STATE_STORE, "cassandra.ssl");
            Assert.fail("expected a RuntimeException, got " + ignored);
        } catch (RuntimeException re) {
            Assert.assertEquals(re.getMessage(),
                    "property cassandra.ssl does not exist for plugin " + STATE_STORE);
        }
    }

    @Test
    public void testGetOptionalPluginPropertyOfUnknownNameIsEmpty() {
        when(configurationManager.getPluginInfo(STATE_STORE))
                .thenReturn(pluginInfo("cassandra.nodes", "node-1"));

        Optional<String> value =
                ConfigurationAccessor.getOptionalPluginProperty(STATE_STORE, "cassandra.ssl");

        Assert.assertFalse(value.isPresent());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetPluginPropertyOfUnsupportedPluginTypeFails() {
        when(configurationManager.getPluginInfo("zusammen_unknown_store"))
                .thenThrow(new RuntimeException("Plugin type:zusammen_unknown_store not supported."));

        ConfigurationAccessor.getPluginProperty("zusammen_unknown_store", "cassandra.nodes");
    }

    private static PluginInfo pluginInfo(String propertyName, String propertyValue) {
        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setImplementationClass("com.example.CassandraStateStore");
        pluginInfo.getProperties().put(propertyName, propertyValue);
        return pluginInfo;
    }
}
