
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

package com.amdocs.zusammen.commons.configuration.impl;

import com.amdocs.zusammen.commons.configuration.ConfigurationManager;
import com.amdocs.zusammen.commons.configuration.datatypes.Configuration;
import com.amdocs.zusammen.commons.configuration.datatypes.PluginInfo;
import com.amdocs.zusammen.commons.configuration.utils.AggregateConfigurationFiles;
import com.amdocs.zusammen.utils.fileutils.FileUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

class ConfigurationManagerImpl implements ConfigurationManager {
  private static Configuration configurationInfo;

  ConfigurationManagerImpl() {
    synchronized (ConfigurationManagerImpl.class) {

      List<InputStream> configurationFileInputStreamList =
          FileUtils.getFileInputStreams("zusammen.json");
      configurationInfo = AggregateConfigurationFiles.aggregate(configurationFileInputStreamList);

    }
  }

  @Override
  public PluginInfo getPluginInfo(String pluginType) {
    PluginInfo pluginInfo = configurationInfo.getPlugins().get(pluginType);
    if (pluginInfo == null) {
      throw new RuntimeException("Plugin type:" + pluginType + " not supported.");
    }
    return pluginInfo;
  }

  @Override
  public <T> Optional<T> getProperty(String propertyName) {
    Object value = configurationInfo.getProperties().get(propertyName);
    if (value == null) {
      value = System.getProperty(propertyName);
      if (value == null) {
        return Optional.empty();
      }
    }
    return Optional.of((T) value);
  }
}
