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


import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.commons.health.data.HealthStatus;
import com.amdocs.zusammen.commons.log.ZusammenLogger;
import com.amdocs.zusammen.commons.log.ZusammenLoggerFactory;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Item;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.HealthHelper;
import com.amdocs.zusammen.sdk.state.StateStore;
import com.amdocs.zusammen.sdk.state.types.StateElement;

import java.util.Collection;
import java.util.Date;

public class StateStoreImpl implements StateStore, HealthHelper {
  private ElementStateStore elementStateStore = new ElementStateStore();
  private VersionStateStore versionStateStore = new VersionStateStore();
  private ItemStateStore itemStateStore = new ItemStateStore(versionStateStore);
  public static final ZusammenLogger LOGGER = ZusammenLoggerFactory.getLogger(StateStoreImpl.class.getName());
  @Override
  public Response<HealthInfo> checkHealth(SessionContext sessionContext) {
    HealthInfo healthInfo = null;
    boolean queryResult = false;
    try {
       queryResult = getKeepAliveDao(sessionContext).get(sessionContext);
    }  catch (Throwable t){
      LOGGER.error(t.getMessage(),t);
      healthInfo = new HealthInfo(CASSANDRA_MODEL_NAME, HealthStatus.DOWN,t.getMessage());
      return new Response<>(healthInfo);
    }
    if(queryResult) {
       healthInfo = new HealthInfo(CASSANDRA_MODEL_NAME, HealthStatus.UP,"");
      LOGGER.info("Health info:"+ healthInfo);
    } else {
      healthInfo = new HealthInfo(CASSANDRA_MODEL_NAME, HealthStatus.DOWN,"DB Schema does not exist.");
      LOGGER.error("Health info:"+ healthInfo);
    }

    return new Response<>(healthInfo);
  }

  @Override
  public Response<Collection<Item>> listItems(SessionContext context) {
    return new Response<>(itemStateStore.listItems(context));
  }

  @Override
  public Response<Boolean> isItemExist(SessionContext context, Id itemId) {
    return new Response<>(itemStateStore.isItemExist(context, itemId));
  }

  @Override
  public Response<Item> getItem(SessionContext context, Id itemId) {
    return new Response<>(itemStateStore.getItem(context, itemId));
  }

  @Override
  public Response<Void> createItem(SessionContext context, Id itemId, Info itemInfo, Date
      creationTime) {
    itemStateStore.createItem(context, itemId, itemInfo, creationTime);
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Void> updateItem(SessionContext context, Id itemId, Info itemInfo, Date
      modificationTime) {
    itemStateStore.updateItem(context, itemId, itemInfo, modificationTime);
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Void> deleteItem(SessionContext context, Id itemId) {
    itemStateStore.deleteItem(context, itemId);
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Void> updateItemModificationTime(SessionContext context, Id itemId,
                                                   Date modificationTime) {
    itemStateStore.updateItemModificationTime(context, itemId,  modificationTime);
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Collection<ItemVersion>> listItemVersions(SessionContext context, Space space,
                                                            Id itemId) {
    return new Response<>(versionStateStore.listItemVersions(context, space, itemId));
  }

  @Override
  public Response<Boolean> isItemVersionExist(SessionContext context, Space space, Id itemId, Id
      versionId) {
    return new Response<>(versionStateStore.isItemVersionExist(context, space, itemId, versionId));
  }

  @Override
  public Response<ItemVersion> getItemVersion(SessionContext context, Space space, Id itemId, Id
      versionId) {
    return new Response<>(versionStateStore.getItemVersion(context, space, itemId, versionId));
  }

  @Override
  public Response<Void> createItemVersion(SessionContext context, Space space, Id itemId,
                                          Id baseVersionId,
                                          Id versionId, ItemVersionData data,
                                          Date creationTime) {
    versionStateStore
        .createItemVersion(context, space, itemId, baseVersionId, versionId, data, creationTime);
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Void> updateItemVersion(SessionContext context, Space space, Id itemId,
                                          Id versionId,
                                          ItemVersionData data,
                                          Date modificationTime) {
    versionStateStore.updateItemVersion(context, space, itemId, versionId, data, modificationTime);
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Void> deleteItemVersion(SessionContext context, Space space, Id itemId,
                                          Id versionId) {
    versionStateStore.deleteItemVersion(context, space, itemId, versionId);
    return new Response(Void.TYPE);
  }


  @Override
  public Response<Void> updateItemVersionModificationTime(SessionContext context,Space space, Id itemId,
                                                          Id versionId, Date modificationTime) {
    versionStateStore.updateItemVersionModificationTime(context,space,  itemId, versionId,
        modificationTime);
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Collection<StateElement>> listElements(SessionContext context,
                                                         ElementContext elementContext,
                                                         Id elementId) {
    return new Response<>(elementStateStore.listElements(context, elementContext, elementId));
  }

  @Override
  public Response<Boolean> isElementExist(SessionContext context, ElementContext elementContext,
                                          Id elementId) {
    return new Response<>(elementStateStore.isElementExist(context, elementContext, elementId));
  }

  @Override
  public Response<Namespace> getElementNamespace(SessionContext context, Id itemId, Id elementId) {
    return new Response<>(elementStateStore.getElementNamespace(context, itemId, elementId));
  }

  @Override
  public Response<StateElement> getElement(SessionContext context, ElementContext elementContext,
                                           Id elementId) {
    return new Response<>(elementStateStore.getElement(context, elementContext, elementId));
  }

  @Override
  public Response<Void> createElement(SessionContext context, StateElement element) {
    elementStateStore.createElement(context, element);
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Void> updateElement(SessionContext context, StateElement element) {
    elementStateStore.updateElement(context, element);
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Void> deleteElement(SessionContext context, StateElement element) {
    elementStateStore.deleteElement(context, element);
    return new Response(Void.TYPE);
  }



}
