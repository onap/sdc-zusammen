/*
 * Copyright © 2016-2017 European Support Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amdocs.zusammen.plugin.statestore.cassandra;

import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ItemDao;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Item;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ItemDaoFactory;

import java.util.Collection;
import java.util.Date;

class ItemStateStore {

  private VersionStateStore versionStateStore;

  ItemStateStore(VersionStateStore versionStateStore) {
    this.versionStateStore = versionStateStore;
  }

  Collection<Item> listItems(SessionContext context) {
    return getItemDao(context).list(context);
  }

  boolean isItemExist(SessionContext context, Id itemId) {
    return getItemDao(context).get(context, itemId).isPresent();
  }

  Item getItem(SessionContext context, Id itemId) {
    return getItemDao(context).get(context, itemId).orElse(null);
  }

  void createItem(SessionContext context, Id itemId, Info itemInfo, Date creationTime) {
    getItemDao(context).create(context, itemId, itemInfo,creationTime);
  }

  void updateItem(SessionContext context, Id itemId, Info itemInfo, Date modificationTime) {
    getItemDao(context).update(context, itemId, itemInfo,modificationTime);
  }

  void deleteItem(SessionContext context, Id itemId) {
    Collection<ItemVersion> versions =
        versionStateStore.listItemVersions(context, Space.PRIVATE, itemId);
    versions.forEach(itemVersion ->
        versionStateStore.deleteItemVersion(context, Space.PRIVATE, itemId, itemVersion.getId()));

    getItemDao(context).delete(context, itemId);
  }

  protected ItemDao getItemDao(SessionContext context) {
    return ItemDaoFactory.getInstance().createInterface(context);
  }

  public void updateItemModificationTime(SessionContext context, Id itemId, Date modificationTime) {
    getItemDao(context).updateItemModificationTime(context, itemId, modificationTime);
  }
}