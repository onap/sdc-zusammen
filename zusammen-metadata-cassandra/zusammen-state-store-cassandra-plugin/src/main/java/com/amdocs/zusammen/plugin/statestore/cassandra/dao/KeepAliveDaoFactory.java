package com.amdocs.zusammen.plugin.statestore.cassandra.dao;

import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.utils.facade.api.AbstractComponentFactory;
import com.amdocs.zusammen.utils.facade.api.AbstractFactory;

public abstract class KeepAliveDaoFactory extends AbstractComponentFactory<KeepAliveDao> {
  public static KeepAliveDaoFactory getInstance() {
    return AbstractFactory.getInstance(KeepAliveDaoFactory.class);
  }

  public abstract KeepAliveDao createInterface(SessionContext context);
}

