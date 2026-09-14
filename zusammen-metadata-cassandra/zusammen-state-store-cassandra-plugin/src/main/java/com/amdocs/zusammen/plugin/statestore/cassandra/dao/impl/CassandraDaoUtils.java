package com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl;

import com.amdocs.zusammen.commons.db.api.cassandra.CassandraConnectorFactory;
import com.amdocs.zusammen.commons.db.api.cassandra.types.CassandraContext;
import com.amdocs.zusammen.datatypes.SessionContext;

class CassandraDaoUtils {

  private CassandraDaoUtils() {
  }

  static <T> T getAccessor(SessionContext context, Class<T> classOfT) {
    return CassandraConnectorFactory.getInstance().createInterface()
        .getMappingManager(getCassandraContext(context))
        .createAccessor(classOfT);
  }

  private static CassandraContext getCassandraContext(SessionContext context) {
    CassandraContext cassandraContext = new CassandraContext();
    cassandraContext.setTenant(context.getTenant());
    return cassandraContext;
  }
}
