package com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl;

import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.KeepAliveDao;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.KeepAliveDaoFactory;
public class KeepAliveCassandraDaoFactory extends KeepAliveDaoFactory {

  private static final KeepAliveDao INSTANCE = new KeepAliveCassandraDao();

  @Override
  public KeepAliveDao createInterface(SessionContext context) {
    return INSTANCE;
  }
}
