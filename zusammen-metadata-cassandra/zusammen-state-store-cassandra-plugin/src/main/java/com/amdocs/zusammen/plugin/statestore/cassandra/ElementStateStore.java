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
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ElementRepository;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ElementRepositoryFactory;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.sdk.state.types.StateElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

class ElementStateStore {

  Collection<StateElement> listElements(SessionContext context, ElementContext elementContext,
                                        Id elementId) {
    ElementEntityContext elementEntityContext =
        new ElementEntityContext(StateStoreUtil.getPrivateSpaceName(context), elementContext);

    if (elementId == null) {
      elementId = StateStoreConstants.ROOT_ELEMENTS_PARENT_ID;
    }

    ElementRepository elementRepository = getElementRepository(context);
    String elementIdValue = elementId.getValue();
    Collection<StateElement> subElements = new ArrayList<>();

    Optional<ElementEntity> element =
        elementRepository.get(context, elementEntityContext, new ElementEntity(elementId));
    if (element.isPresent() && element.get().getSubElementIds() != null) {
      for (Id subElementId : element.get().getSubElementIds()) {
        ElementEntity subElement =
            elementRepository.get(context, elementEntityContext, new ElementEntity(subElementId))
                .orElseThrow(() -> new IllegalStateException(String.format(
                    "List sub elements error: item %s, version %s - " +
                        "element %s, which appears as sub element of element %s, does not exist",
                    elementContext.getItemId().getValue(), elementContext.getVersionId().getValue(),
                    subElementId, elementIdValue)));
        subElements.add(StateStoreUtil.getStateElement(elementEntityContext, subElement));
      }
    }
    return subElements;
  }

  boolean isElementExist(SessionContext context, ElementContext elementContext,
                         Id elementId) {
    return getElementRepository(context).get(context,
        new ElementEntityContext(StateStoreUtil.getPrivateSpaceName(context), elementContext),
        new ElementEntity(elementId)).isPresent();
  }

  Namespace getElementNamespace(SessionContext context, Id itemId, Id elementId) {
    return getElementRepository(context)
        .getNamespace(context,
            new ElementEntityContext(null, itemId, null),
            new ElementEntity(elementId))
        .orElse(null);
  }

  StateElement getElement(SessionContext context, ElementContext elementContext,
                          Id elementId) {
    ElementEntityContext elementEntityContext =
        new ElementEntityContext(StateStoreUtil.getPrivateSpaceName(context), elementContext);
    return getElementRepository(context)
        .get(context, elementEntityContext, new ElementEntity(elementId))
        .map(elementEntity -> StateStoreUtil.getStateElement(elementEntityContext, elementEntity))
        .orElse(null);
  }

  void createElement(SessionContext context, StateElement element) {
    getElementRepository(context)
        .create(context,
            new ElementEntityContext(StateStoreUtil.getSpaceName(context, element.getSpace()),
                element.getItemId(), element.getVersionId()),
            StateStoreUtil.getElementEntity(element));
  }

  void updateElement(SessionContext context, StateElement element) {
    getElementRepository(context)
        .update(context,
            new ElementEntityContext(StateStoreUtil.getSpaceName(context, element.getSpace()),
                element.getItemId(), element.getVersionId()),
            StateStoreUtil.getElementEntity(element));
  }

  void deleteElement(SessionContext context, StateElement element) {
    deleteElementHierarchy(getElementRepository(context),
        context,
        new ElementEntityContext(StateStoreUtil.getSpaceName(context, element.getSpace()),
            element.getItemId(), element.getVersionId()),
        StateStoreUtil.getElementEntity(element));
  }

  private void deleteElementHierarchy(ElementRepository elementRepository, SessionContext context,
                                      ElementEntityContext elementEntityContext,
                                      ElementEntity elementEntity) {
    Optional<ElementEntity> retrieved =
        elementRepository.get(context, elementEntityContext, elementEntity);
    if (!retrieved.isPresent()) {
      return;
    }
    retrieved.get().getSubElementIds().stream()
        .map(ElementEntity::new)
        .forEach(subElementEntity -> deleteElementHierarchy(
            elementRepository, context, elementEntityContext, subElementEntity));

    // only for the first one the parentId will populated (so it'll be removed from its parent)
    elementRepository.delete(context, elementEntityContext, elementEntity);
  }

  protected ElementRepository getElementRepository(SessionContext context) {
    return ElementRepositoryFactory.getInstance().createInterface(context);
  }
}