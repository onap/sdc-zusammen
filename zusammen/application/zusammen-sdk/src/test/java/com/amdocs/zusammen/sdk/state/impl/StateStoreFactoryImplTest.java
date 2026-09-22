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

package com.amdocs.zusammen.sdk.state.impl;

import com.amdocs.zusammen.commons.configuration.ConfigurationManager;
import com.amdocs.zusammen.commons.configuration.datatypes.PluginInfo;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.sdk.SdkConstants;
import com.amdocs.zusammen.sdk.StubConfigurationManagerFactory;
import com.amdocs.zusammen.sdk.StubPlugin;
import com.amdocs.zusammen.sdk.state.StateStore;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.mockito.Mockito.when;

public class StateStoreFactoryImplTest {

  private ConfigurationManager configurationManager;

  @BeforeMethod
  public void setUp() throws Exception {
    configurationManager = Mockito.mock(ConfigurationManager.class);
    StubConfigurationManagerFactory.install(configurationManager);
    clearPluginCache();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    clearPluginCache();
    StubConfigurationManagerFactory.restore();
  }

  @Test
  public void testCreateInterfaceInstantiatesThePluginConfiguredForTheStateStore() {
    givenConfiguredPlugin(StubPlugin.class.getName());

    StateStore stateStore = new StateStoreFactoryImpl().createInterface(new SessionContext());

    Assert.assertTrue(stateStore instanceof StubPlugin);
  }

  @Test
  public void testCreateInterfaceReusesTheSamePluginInstance() {
    givenConfiguredPlugin(StubPlugin.class.getName());

    StateStore first = new StateStoreFactoryImpl().createInterface(new SessionContext());
    StateStore second = new StateStoreFactoryImpl().createInterface(new SessionContext());

    Assert.assertSame(second, first);
  }

  @Test
  public void testCreateInterfaceReportsAStateStoreInitFailureWhenThePluginClassIsMissing() {
    givenConfiguredPlugin("com.amdocs.zusammen.sdk.state.MissingPlugin");

    try {
      new StateStoreFactoryImpl().createInterface(new SessionContext());
      Assert.fail("expected the unloadable plugin class to be reported");
    } catch (ZusammenException e) {
      Assert.assertTrue(e.getReturnCode().getMessage().contains("MissingPlugin"));
    }
  }

  private void givenConfiguredPlugin(String implementationClass) {
    PluginInfo pluginInfo = new PluginInfo();
    pluginInfo.setImplementationClass(implementationClass);
    when(configurationManager.getPluginInfo(SdkConstants.ZUSAMMEN_STATE_STORE))
        .thenReturn(pluginInfo);
  }

  private void clearPluginCache() throws Exception {
    Field field = StateStoreFactoryImpl.class.getDeclaredField("stateStore");
    field.setAccessible(true);
    field.set(null, null);
  }
}
