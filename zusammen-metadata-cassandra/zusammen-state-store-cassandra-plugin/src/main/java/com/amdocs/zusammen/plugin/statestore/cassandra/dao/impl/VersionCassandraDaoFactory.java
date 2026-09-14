package com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl;

import com.amdocs.zusammen.plugin.statestore.cassandra.dao.VersionDaoFactory;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.VersionDao;

public class VersionCassandraDaoFactory extends VersionDaoFactory {

  private static final VersionDao INSTANCE = new VersionCassandraDao();

  @Override
  public VersionDao createInterface(SessionContext context) {
    return INSTANCE;
  }
}
