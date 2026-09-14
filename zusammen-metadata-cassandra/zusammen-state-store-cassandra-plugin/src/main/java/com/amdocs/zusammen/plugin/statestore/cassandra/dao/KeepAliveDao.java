package com.amdocs.zusammen.plugin.statestore.cassandra.dao;

import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.datatypes.SessionContext;

import java.util.Optional;

public interface KeepAliveDao {

  boolean get(SessionContext context);
}
