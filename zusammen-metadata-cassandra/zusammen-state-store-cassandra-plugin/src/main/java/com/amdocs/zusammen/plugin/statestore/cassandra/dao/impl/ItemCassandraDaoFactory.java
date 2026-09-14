package com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl;

import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ItemDao;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ItemDaoFactory;
import com.amdocs.zusammen.datatypes.SessionContext;

public class ItemCassandraDaoFactory extends ItemDaoFactory {

  private static final ItemDao INSTANCE = new ItemCassandraDao();

  @Override
  public ItemDao createInterface(SessionContext context) {
    return INSTANCE;
  }
}
