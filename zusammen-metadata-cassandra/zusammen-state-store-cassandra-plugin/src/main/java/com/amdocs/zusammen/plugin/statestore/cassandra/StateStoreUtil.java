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

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.sdk.state.types.StateElement;

class StateStoreUtil {

  static String getSpaceName(SessionContext context, Space space) {
    switch (space) {
      case PUBLIC:
        return StateStoreConstants.PUBLIC_SPACE;
      case PRIVATE:
        return StateStoreUtil.getPrivateSpaceName(context);
      default:
        throw new IllegalArgumentException(String.format("Space %s is not supported.", space));
    }
  }

  static String getPrivateSpaceName(SessionContext context) {
    return context.getUser().getUserName();
  }

  static ElementEntity getElementEntity(StateElement element) {
    ElementEntity elementEntity = new ElementEntity(element.getId());
    elementEntity.setNamespace(element.getNamespace());
    elementEntity.setParentId(element.getParentId() == null
        ? StateStoreConstants.ROOT_ELEMENTS_PARENT_ID
        : element.getParentId());
    elementEntity.setInfo(element.getInfo());
    elementEntity.setRelations(element.getRelations());
    return elementEntity;
  }

  static StateElement getStateElement(ElementEntityContext elementEntityContext, ElementEntity
      elementEntity) {
    Id parentId = StateStoreConstants.ROOT_ELEMENTS_PARENT_ID.equals(elementEntity.getParentId())
        ? null
        : elementEntity.getParentId();
    StateElement element = new StateElement(elementEntityContext.getItemId(),
        elementEntityContext.getVersionId(), elementEntity.getNamespace(), elementEntity.getId());

    element.setParentId(parentId);
    element.setInfo(elementEntity.getInfo());
    element.setRelations(elementEntity.getRelations());
    element.setSubElements(elementEntity.getSubElementIds());
    return element;
  }
}
