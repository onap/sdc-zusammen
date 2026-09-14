package com.amdocs.zusammen.plugin.statestore.cassandra.dao;

import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.utils.facade.api.AbstractComponentFactory;
import com.amdocs.zusammen.utils.facade.api.AbstractFactory;

public abstract class ItemDaoFactory extends AbstractComponentFactory<ItemDao> {
  public static ItemDaoFactory getInstance() {
    return AbstractFactory.getInstance(ItemDaoFactory.class);
  }

  public abstract ItemDao createInterface(SessionContext context);
}

