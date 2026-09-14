package com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.VersionDao;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class VersionCassandraDao implements VersionDao {

  @Override
  public void create(SessionContext context, String space, Id itemId, Id baseVersionId,
                     Id versionId, ItemVersionData data, Date creationTime) {
    String baseVersion = baseVersionId != null ? baseVersionId.toString() : null;

    getAccessor(context)
        .create(space, itemId.toString(), versionId.toString(), baseVersion, creationTime,
            creationTime, JsonUtil.object2Json(data.getInfo()),
            JsonUtil.object2Json(data.getRelations()));
  }

  @Override
  public void update(SessionContext context, String space, Id itemId, Id versionId,
                     ItemVersionData data, Date modificationTime) {
    getAccessor(context)
        .update(JsonUtil.object2Json(data.getInfo()), JsonUtil.object2Json(data
                .getRelations()),
            modificationTime, space, itemId.toString(), versionId.toString());
  }

  @Override
  public void delete(SessionContext context, String space, Id itemId, Id versionId) {
    getAccessor(context).delete(space, itemId.toString(), versionId.toString());
  }

  @Override
  public void updateItemVersionModificationTime(SessionContext context, String space, Id itemId,
                                                Id versionId, Date modificationTime) {
    getAccessor(context)
        .updateModificationTime(modificationTime,
            space, itemId.toString(), versionId.toString());
  }

  @Override
  public Collection<ItemVersion> list(SessionContext context, String space, Id itemId) {
    List<Row> rows = getAccessor(context).list(space, itemId.toString()).all();
    return rows == null ? new ArrayList<>() :
        rows.stream().map(this::createItemVersion).collect(Collectors.toList());
  }

  @Override
  public Optional<ItemVersion> get(SessionContext context, String space, Id itemId,
                                   Id versionId) {
    Row row = getAccessor(context).get(space, itemId.toString(), versionId.toString()).one();
    return row == null ? Optional.empty() : Optional.of(createItemVersion(row));
  }

  private ItemVersion createItemVersion(Row row) {
    ItemVersion itemVersion = new ItemVersion();
    itemVersion.setId(new Id(row.getString(VersionField.VERSION_ID)));
    itemVersion.setBaseId(new Id(row.getString(VersionField.BASE_VERSION_ID)));
    itemVersion.setData(new ItemVersionData());
    itemVersion.getData()
        .setInfo(JsonUtil.json2Object(row.getString(VersionField.INFO), Info.class));
    itemVersion.getData()
        .setRelations(JsonUtil.json2Object(row.getString(VersionField.RELATIONS),
            new TypeToken<ArrayList<Relation>>() {
            }.getType()));
    itemVersion.setCreationTime(row.getTimestamp(VersionField.CREATION_TIME));
    itemVersion.setModificationTime(row.getTimestamp(VersionField.MODIFICATION_TIME));
    return itemVersion;
  }

  private VersionAccessor getAccessor(SessionContext context) {
    return CassandraDaoUtils.getAccessor(context, VersionAccessor.class);
  }

  @Accessor
  interface VersionAccessor {

    @Query(
        "INSERT INTO version (space, item_id, version_id, base_version_id, creation_time, " +
            "modification_time, info, relations) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
    void create(String space, String itemId, String versionId, String baseVersionId,
                Date creationTime, Date modificationTime, String info, String relations);


    @Query("UPDATE version SET info=?, relations=? ,modification_time= ? WHERE space=? AND " +
        "item_id=? AND version_id=?")
    void update(String info, String relations, Date modificationTime, String space, String itemId,
                String versionId);

    @Query("UPDATE version SET modification_time=? WHERE space=? AND item_id=? AND version_id=? ")
    void updateModificationTime(Date modificationTime, String space, String itemId,
                                String versionId);


    @Query("DELETE FROM version WHERE space=? AND item_id=? AND version_id=?")
    void delete(String space, String itemId, String versionId);

    @Query("SELECT version_id, base_version_id, info, relations, creation_time, modification_time" +
        " FROM version " +
        "WHERE space=? AND item_id=? AND version_id=?")
    ResultSet get(String space, String itemId, String versionId);

    @Query(
        "SELECT version_id, base_version_id, info, relations, creation_time, modification_time FROM version " +
            "WHERE space=? AND item_id=?")
    ResultSet list(String space, String itemId);
  }

  private static final class VersionField {
    private static final String VERSION_ID = "version_id";
    private static final String BASE_VERSION_ID = "base_version_id";
    private static final String INFO = "info";
    private static final String RELATIONS = "relations";
    private static final String CREATION_TIME = "creation_time";
    private static final String MODIFICATION_TIME = "modification_time";
  }

}
