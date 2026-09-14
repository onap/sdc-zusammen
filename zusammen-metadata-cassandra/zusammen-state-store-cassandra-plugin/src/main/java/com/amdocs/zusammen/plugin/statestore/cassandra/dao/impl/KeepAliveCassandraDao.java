package com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.KeepAliveDao;
public class KeepAliveCassandraDao implements KeepAliveDao {

    @Override

 public boolean get(SessionContext context){
        ResultSet check = getAccessor(context).check();
        return check.getColumnDefinitions().contains("item_id");

 }
  private KeepAliveAccessor getAccessor(SessionContext context) {
    return CassandraDaoUtils.getAccessor(context, KeepAliveAccessor.class);
  }

  @Accessor
  interface KeepAliveAccessor {

    @Query("SELECT * FROM element LIMIT 1")
    ResultSet check();

  }

}
