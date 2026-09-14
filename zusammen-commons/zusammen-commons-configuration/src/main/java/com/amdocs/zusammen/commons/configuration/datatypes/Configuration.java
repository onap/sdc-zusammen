/*
 * Copyright © 2016-2017 European Support Limited
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

import java.util.HashMap;
import java.util.Map;

public class Configuration {
  private Map<String, Object> properties = new HashMap<>();
  private Map<String, PluginInfo> plugins = new HashMap<>();

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  public Map<String, PluginInfo> getPlugins() {
    return plugins;
  }

  public void setPlugins(Map<String, PluginInfo> plugins) {
    this.plugins = plugins;
  }
}
