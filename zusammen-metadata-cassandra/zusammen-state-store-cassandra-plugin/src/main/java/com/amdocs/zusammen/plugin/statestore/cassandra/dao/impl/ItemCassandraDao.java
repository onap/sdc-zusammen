package com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Item;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ItemDao;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemCassandraDao implements ItemDao {

  @Override
  public void create(SessionContext context, Id itemId, Info itemInfo, Date creationTime) {
    getAccessor(context)
        .create(itemId.getValue(), JsonUtil.object2Json(itemInfo), creationTime, creationTime);
  }


  @Override
  public void update(SessionContext context, Id itemId, Info itemInfo, Date modificationTime) {
    getAccessor(context)
        .update(itemId.getValue(), JsonUtil.object2Json(itemInfo), modificationTime);
  }

  @Override
  public void updateItemModificationTime(SessionContext context, Id itemId, Date modificationTime) {
    getAccessor(context).updateModificationTime(itemId.getValue(), modificationTime);
  }

  @Override
  public void delete(SessionContext context, Id itemId) {
    getAccessor(context).delete(itemId.getValue());
  }

  @Override
  public Optional<Item> get(SessionContext context, Id itemId) {
    Row row = getAccessor(context).get(itemId.getValue()).one();
    return row == null ? Optional.empty() : Optional.of(createItem(row));
  }

  @Override
  public List<Item> list(SessionContext context) {
    List<Row> rows = getAccessor(context).list().all();
    return rows == null ? new ArrayList<>()
        : rows.stream().map(this::createItem).collect(Collectors.toList());
  }


  private Item createItem(Row row) {
    Item item = new Item();
    item.setId(new Id(row.getString(ItemField.ITEM_ID)));
    item.setInfo(JsonUtil.json2Object(row.getString(ItemField.ITEM_INFO), Info.class));
    item.setCreationTime(row.getTimestamp(ItemField.CREATION_TIME));
    item.setModificationTime(row.getTimestamp(ItemField.MODIFICATION_TIME));
    return item;
  }

  private ItemAccessor getAccessor(SessionContext context) {
    return CassandraDaoUtils.getAccessor(context, ItemAccessor.class);
  }


  @Accessor
  interface ItemAccessor {

    @Query("INSERT INTO item (item_id, item_info, creation_time, modification_time) " +
        "VALUES (?,?,?,?)")
    void create(String itemId, String itemInfo, Date creationTime, Date modificationTime);

    @Query("INSERT INTO item (item_id, item_info,modification_time) VALUES (?,?,?)")
    void update(String itemId, String itemInfo, Date modificationTime);

    @Query("INSERT INTO item (item_id, modification_time) VALUES (?,?)")
    void updateModificationTime(String itemId, Date modificationTime);


    @Query("DELETE FROM item WHERE item_id=?")
    void delete(String itemId);

    @Query("SELECT item_id, item_info, creation_time, modification_time FROM item WHERE item_id=?")
    ResultSet get(String itemId);

    @Query("SELECT item_id, item_info , creation_time, modification_time FROM item")
    ResultSet list();
  }

  private static final class ItemField {
    private static final String ITEM_ID = "item_id";
    private static final String ITEM_INFO = "item_info";
    private static final String CREATION_TIME = "creation_time";
    private static final String MODIFICATION_TIME = "modification_time";
  }
}
