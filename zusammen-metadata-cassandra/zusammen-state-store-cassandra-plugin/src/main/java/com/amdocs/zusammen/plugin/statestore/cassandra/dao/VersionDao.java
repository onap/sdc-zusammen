package com.amdocs.zusammen.plugin.statestore.cassandra.dao;


import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

public interface VersionDao {
  Collection<ItemVersion> list(SessionContext context, String space, Id itemId);

  Optional<ItemVersion> get(SessionContext context, String space, Id itemId, Id versionId);

  void create(SessionContext context, String space, Id itemId, Id baseVersionId, Id versionId,
              ItemVersionData data, Date creationTime);

  void update(SessionContext context, String space, Id itemId, Id versionId, ItemVersionData data,
              Date modificationTime);

  void delete(SessionContext context, String space, Id itemId, Id versionId);

  void updateItemVersionModificationTime(SessionContext context, String spaceName, Id itemId,
                                         Id versionId, Date modificationTime);
}
