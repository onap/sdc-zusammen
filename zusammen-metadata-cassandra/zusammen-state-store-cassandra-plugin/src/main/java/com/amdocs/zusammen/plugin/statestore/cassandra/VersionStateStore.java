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

import com.amdocs.zusammen.plugin.statestore.cassandra.dao.VersionDaoFactory;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ElementRepository;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ElementRepositoryFactory;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.VersionDao;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;

import java.util.Collection;
import java.util.Date;

class VersionStateStore {

  Collection<ItemVersion> listItemVersions(SessionContext context, Space space, Id itemId) {
    return getVersionDao(context).list(context, StateStoreUtil.getSpaceName(context, space), itemId);
  }

  boolean isItemVersionExist(SessionContext context, Space space, Id itemId, Id versionId) {
    return getVersionDao(context).get(context, StateStoreUtil.getSpaceName(context, space), itemId, versionId)
        .isPresent();
  }

  ItemVersion getItemVersion(SessionContext context, Space space, Id itemId, Id versionId) {
    return getVersionDao(context).get(context, StateStoreUtil.getSpaceName(context, space), itemId, versionId)
        .orElse(null);
  }

  void createItemVersion(SessionContext context, Space space, Id itemId, Id baseVersionId,
                         Id versionId, ItemVersionData data, Date creationTime) {
    String spaceName = StateStoreUtil.getSpaceName(context, space);

    getVersionDao(context)
        .create(context, spaceName, itemId, baseVersionId, versionId, data, creationTime);
    if (baseVersionId == null) {
      return;
    }
    copyElements(context, spaceName, itemId, baseVersionId, versionId);
  }

  void updateItemVersion(SessionContext context, Space space, Id itemId, Id versionId,
                         ItemVersionData data, Date modificationTime) {
    getVersionDao(context)
        .update(context, StateStoreUtil.getSpaceName(context, space), itemId, versionId, data, modificationTime);
  }

  void deleteItemVersion(SessionContext context, Space space, Id itemId, Id versionId) {
    String spaceName = StateStoreUtil.getSpaceName(context, space);

    deleteElements(context, spaceName, itemId, versionId);
    getVersionDao(context).delete(context, spaceName, itemId, versionId);
  }

  public void updateItemVersionModificationTime(SessionContext context, Space space, Id itemId,
                                                Id versionId, Date modificationTime) {
    getVersionDao(context)
        .updateItemVersionModificationTime(context, StateStoreUtil.getSpaceName(context, space), itemId, versionId,
            modificationTime);
  }


  private void copyElements(SessionContext context, String space, Id itemId, Id sourceVersionId,
                            Id targetVersionId) {
    ElementRepository elementRepository = getElementRepository(context);
    ElementEntityContext elementContext = new ElementEntityContext(space, itemId, sourceVersionId);

    Collection<ElementEntity> versionElements = elementRepository.list(context, elementContext);
    elementContext.setVersionId(targetVersionId);
    versionElements
        .forEach(elementEntity -> elementRepository.create(context, elementContext, elementEntity));
  }

  private void deleteElements(SessionContext context, String space, Id itemId, Id versionId) {
    ElementRepository elementRepository = getElementRepository(context);
    ElementEntityContext elementContext = new ElementEntityContext(space, itemId, versionId);

    Collection<ElementEntity> versionElements = elementRepository.list(context, elementContext);
    versionElements.stream()
        .peek(elementEntity -> elementEntity.setParentId(null))
        .forEach(elementEntity -> elementRepository.delete(context, elementContext, elementEntity));
  }

  protected VersionDao getVersionDao(SessionContext context) {
    return VersionDaoFactory.getInstance().createInterface(context);
  }

  protected ElementRepository getElementRepository(SessionContext context) {
    return ElementRepositoryFactory.getInstance().createInterface(context);
  }


}