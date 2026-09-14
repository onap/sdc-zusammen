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

package com.amdocs.zusammen.commons.db.impl.cassandra;

import com.amdocs.zusammen.commons.configuration.impl.ConfigurationAccessor;

import java.util.List;
import java.util.Optional;

class CassandraConfig {
  private static final String NODES = "cassandra.nodes";
  private static final String KEYSPACE = "cassandra.keyspace";
  private static final String USER = "cassandra.user";
  private static final String PASSWORD = "cassandra.password";
  private static final String AUTHENTICATE = "cassandra.authenticate";
  private static final String SSL = "cassandra.ssl";
  private static final String TRUST_STORE = "cassandra.truststore";
  private static final String TRUST_STORE_PASSWORD = "cassandra.truststore.password";
  private static final String DATA_CENTER = "cassandra.datacenter";
  private static final String CONSISTENCY_LEVEL = "cassandra.consistency.level";
  private static final String PORT = "cassandra.port";
  private static final String RECONNECT_DELAY = "cassandra.reconnection.delay";

  static String[] getNodes() {
    Object nodesObject = ConfigurationAccessor.getProperty(NODES);
    if(nodesObject instanceof List){
      List<String> nodes =  (List<String>)nodesObject;
      return nodes.toArray(new String[nodes.size()]);
    }else{
      return ((String)nodesObject).split(",");
    }


  }

  static String getKeyspace() {
    return ConfigurationAccessor.getProperty(KEYSPACE);
  }

  static String getUser() {
    return ConfigurationAccessor.getProperty(USER);
  }

  static String getPassword() {
    return ConfigurationAccessor.getProperty(PASSWORD);
  }

  static boolean isAuthenticate() {
    return getBoolean(ConfigurationAccessor.getProperty(AUTHENTICATE));
  }

  static boolean isSsl() {
    return getBoolean(ConfigurationAccessor.getProperty(SSL));
  }

  static Optional<Integer> getPort() {
    Optional<String> cassandraPort = ConfigurationAccessor.getOptionalProperty(PORT);
    return cassandraPort.map(Integer::valueOf);
  }

  /**
   * Gets Cassandra reconnection timeout
   *
   * @return
   */
  public static Optional<Long> getReconnectionDelay() {
    Optional<String> cassandraReconnectTimeout = ConfigurationAccessor.getOptionalProperty(RECONNECT_DELAY);
    return cassandraReconnectTimeout.map(Long::parseLong);
  }

  static Optional<String> getTrustStore() {
    return ConfigurationAccessor.getOptionalProperty(TRUST_STORE);
  }

  static Optional<String> getTrustStorePassword() {
    return ConfigurationAccessor.getOptionalProperty(TRUST_STORE_PASSWORD);
  }

  static boolean getBoolean(Object value){
    if(value instanceof String){
      return "true".equals(value);
    }
    return ((Boolean)value).booleanValue();

  }

  public static Optional<String>  getDataCenter() {
    return ConfigurationAccessor.getOptionalProperty(DATA_CENTER);
  }

  public static Optional<String>  getConsistencyLevel() {
    return ConfigurationAccessor.getOptionalProperty(CONSISTENCY_LEVEL);
  }
}
