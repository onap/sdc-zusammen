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

package com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl;

import com.amdocs.zusammen.commons.db.api.cassandra.CassandraConnector;
import com.amdocs.zusammen.commons.db.api.cassandra.CassandraConnectorFactory;
import com.amdocs.zusammen.commons.db.impl.cassandra.CassandraConnectorFactoryImpl;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;

/**
 * Replaces the {@link CassandraConnector} that {@link CassandraDaoUtils} resolves, so the accessors
 * the DAOs talk to can be mocked without a Cassandra session.
 */
public final class CassandraSeam {

  private static CassandraConnector connector;

  private CassandraSeam() {
  }

  public static class StubCassandraConnectorFactory extends CassandraConnectorFactory {

    @Override
    public CassandraConnector createInterface() {
      return connector;
    }
  }

  static void install(CassandraConnector connectorMock) {
    // CassandraConnectorFactory's superclass reloads every factoryConfiguration.json on its first
    // initialisation, overwriting whatever registerFactory put in the registry. Resolving the real
    // factory first gets that one-shot load out of the way (it only constructs the connector; no
    // session is opened until a mapping manager is asked for).
    CassandraConnectorFactory.getInstance();

    connector = connectorMock;
    AbstractFactoryBase
        .registerFactory(CassandraConnectorFactory.class, StubCassandraConnectorFactory.class);
  }

  static void restore() {
    connector = null;
    AbstractFactoryBase
        .registerFactory(CassandraConnectorFactory.class, CassandraConnectorFactoryImpl.class);
  }
}
