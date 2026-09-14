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
package com.amdocs.zusammen.commons.configuration.utils;


import com.amdocs.zusammen.commons.configuration.datatypes.Configuration;
import com.amdocs.zusammen.commons.configuration.datatypes.ConfigurationInfo;
import com.amdocs.zusammen.commons.configuration.datatypes.PluginInfo;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class AggregateConfigurationFiles {

  public static Configuration aggregate(List<InputStream> configurationFiles) {
    Configuration configuration = new Configuration();
    configurationFiles.stream()
        .map(configurationFile -> JsonUtil.json2Object(configurationFile, ConfigurationInfo.class))
        .sorted(Comparator.comparingInt(ConfigurationInfo::getLevel))
        .forEach(configurationInfo
            -> overrideConfiguration(configuration, configurationInfo.getConfiguration()));
    return configuration;
  }

  private static void overrideConfiguration(Configuration baseConfiguration,
                                            Configuration overrideConfiguration) {
    overrideProperties(baseConfiguration.getProperties(), overrideConfiguration.getProperties());
    overridePlugins(baseConfiguration, overrideConfiguration);
  }

  private static void overrideProperties(Map<String, Object> baseProperties,
                                         Map<String, Object> overrideProperties) {
    if (baseProperties.size() > 0) {
      overrideProperties.keySet().forEach(
          propertyKey -> overrideProperty(propertyKey, baseProperties, overrideProperties));
    }
    baseProperties.putAll(overrideProperties);
  }

  private static void overrideProperty(String propertyKey,
                                       Map<String, Object> basePropertyInfo,
                                       Map<String, Object> overridePropertyInfo) {
    Object propertyValue = overridePropertyInfo.get(propertyKey);
    if (propertyValue == null) {
      basePropertyInfo.remove(propertyKey);
    } else {
      basePropertyInfo.put(propertyKey, propertyValue);
    }
  }

  private static void overridePlugins(Configuration baseConfiguration,
                                      Configuration overrideConfiguration) {
    if (overrideConfiguration.getPlugins().size() == 0) {
      return;
    }
    if (baseConfiguration.getPlugins().size() == 0) {
      baseConfiguration.setPlugins(overrideConfiguration.getPlugins());
    } else {
      overrideConfiguration.getPlugins().entrySet().forEach(pluginEntry -> {
        if (baseConfiguration.getPlugins().containsKey(pluginEntry.getKey())) {
          overridePlugin(baseConfiguration.getPlugins().get(pluginEntry.getKey()),
              pluginEntry.getValue());
        } else {
          baseConfiguration.getPlugins().put(pluginEntry.getKey(), pluginEntry.getValue());
        }
      });
    }
  }

  private static void overridePlugin(PluginInfo basePluginInfo, PluginInfo overridePluginInfo) {
    if (overridePluginInfo.getImplementationClass() != null) {
      basePluginInfo.setImplementationClass(overridePluginInfo.getImplementationClass());
    }
    overrideProperties(basePluginInfo.getProperties(), overridePluginInfo.getProperties());
  }
}
