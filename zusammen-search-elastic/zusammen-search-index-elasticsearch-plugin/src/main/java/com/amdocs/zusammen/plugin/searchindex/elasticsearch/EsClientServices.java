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


import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.utils.common.CommonMethods;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class EsClientServices {
  private TransportClient client = null;

  public TransportClient start(SessionContext context) {
    if (Objects.nonNull(client)) {
      return client;
    }

    EsConfig config = new EsConfig();
    String host = config.getHost();
    String clusterName = config.getClusterName();
    int transportPort = config.getTransportPort();

    if (CommonMethods.isEmpty(host)) {
      throw new RuntimeException("Elastic Search 'host' in configuration file is empty");
    }
    if (CommonMethods.isEmpty(clusterName)) {
      throw new RuntimeException("Elastic Search 'cluster name' in configuration file is empty");
    }

    try {
      Settings settings = Settings.builder().put("cluster.name", clusterName).build();
      client = new PreBuiltTransportClient(settings).addTransportAddress(
          new InetSocketTransportAddress(InetAddress.getByName(host),
              transportPort));
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
    return client;
  }

  public void stop(SessionContext context) {
    if (Objects.nonNull(client)) {
      client.close();
      client = null;
    }
  }
}
