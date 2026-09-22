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

package com.amdocs.zusammen.sdk;

import com.amdocs.zusammen.commons.configuration.ConfigurationManager;
import com.amdocs.zusammen.commons.configuration.ConfigurationManagerFactory;
import com.amdocs.zusammen.commons.configuration.impl.ConfigurationManagerFactoryImpl;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;

/**
 * Lets a test feed a stub {@link ConfigurationManager} to the plugin factories, which reach it
 * through {@code ConfigurationManagerFactory.getInstance()}.
 */
public class StubConfigurationManagerFactory extends ConfigurationManagerFactory {

  private static ConfigurationManager configurationManager;

  public static void install(ConfigurationManager stub) {
    configurationManager = stub;
    AbstractFactoryBase.registerFactory(ConfigurationManagerFactory.class,
        StubConfigurationManagerFactory.class);
  }

  /**
   * The factory registry is static and has no removal API, so the production mapping has to be
   * registered back; that also drops the cached stub factory instance.
   */
  public static void restore() {
    configurationManager = null;
    AbstractFactoryBase.registerFactory(ConfigurationManagerFactory.class,
        ConfigurationManagerFactoryImpl.class);
  }

  @Override
  public ConfigurationManager createInterface() {
    return configurationManager;
  }
}
