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


package com.amdocs.zusammen.plugin.searchindex.elasticsearch;

import com.amdocs.zusammen.commons.configuration.impl.ConfigurationAccessor;
import com.amdocs.zusammen.sdk.SdkConstants;

public class EsConfig {
  private final String host = "es.host";
  private final String transportPort = "es.transport.tcp.port";
  private final String clusterName = "es.clusterName";

  public String getHost() {
    return ConfigurationAccessor.getPluginProperty(SdkConstants.ZUSAMMEN_SEARCH_INDEX, host);
  }

  public int getTransportPort() {
    String port =
        ConfigurationAccessor.getPluginProperty(SdkConstants.ZUSAMMEN_SEARCH_INDEX, transportPort);
    return Integer.parseInt(port);
  }

  public String getClusterName() {
    return ConfigurationAccessor
        .getPluginProperty(SdkConstants.ZUSAMMEN_SEARCH_INDEX, clusterName);
  }
}
