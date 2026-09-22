package com.amdocs.zusammen.plugin.collaboration.impl;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.plugin.collaboration.ElementPublicStore;
import com.amdocs.zusammen.plugin.dao.ElementRepository;
import com.amdocs.zusammen.plugin.dao.ElementRepositoryFactory;
import com.amdocs.zusammen.plugin.dao.ElementSynchronizationStateRepository;
import com.amdocs.zusammen.plugin.dao.ElementSynchronizationStateRepositoryFactory;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.amdocs.zusammen.plugin.ZusammenPluginUtil.getSpaceName;

public class ElementPublicStoreImpl implements ElementPublicStore {

  private static final String ELEMENT_TO_UPDATE_DOES_NOT_EXIST =
      "Item Id %s, version Id %s: Element %s that should be updated on public does not exist there";

  @Override
  public Optional<ElementEntity> get(SessionContext context,
                                     ElementContext elementContext, Id elementId) {
    return getElementRepository(context)
        .get(context, new ElementEntityContext(getSpaceName(context, Space.PUBLIC), elementContext),
            new ElementEntity(elementId));
  }

  @Override
  public Optional<ElementEntity> getDescriptor(SessionContext context,
                                               ElementContext elementContext, Id elementId) {
    return getElementRepository(context).getDescriptor(context,
        new ElementEntityContext(getSpaceName(context, Space.PUBLIC), elementContext),
        new ElementEntity(elementId));
  }

  @Override
  public Collection<SynchronizationStateEntity> listSynchronizationStates(
      SessionContext context, ElementContext elementContext) {
    ElementEntityContext publicContext =
        new ElementEntityContext(getSpaceName(context, Space.PUBLIC), elementContext);

    Map<Id, Id> elementRevisionIds = getElementRepository(context).listIds(context, publicContext);
    if (elementRevisionIds.isEmpty()) {
      return new HashSet<>(); // the filter would drop every row anyway; this saves the read
    }

    // listPerRevision, not list: list de-duplicates on SynchronizationStateEntity.equals, which
    // compares the id alone, and here an element has a row per revision it was published in.
    Collection<SynchronizationStateEntity> synchronizationStateEntities = new HashSet<>();
    for (SynchronizationStateEntity syncState : getElementSyncStateRepository(context)
        .listPerRevision(context, publicContext)) {
      Id revisionIdInVersion = elementRevisionIds.get(syncState.getId());
      if (revisionIdInVersion != null && revisionIdInVersion.equals(syncState.getRevisionId())) {
        synchronizationStateEntities.add(syncState);
      }
    }

    return synchronizationStateEntities;
  }

  @Override
  public void create(SessionContext context, ElementContext elementContext,
                     ElementEntity element, Date publishTime) {
    ElementEntityContext publicContext =
        new ElementEntityContext(getSpaceName(context, Space.PUBLIC), elementContext);

    if (element.getParentId() != null) {
      updateParentElement(context, publicContext, element.getParentId(), publishTime);
    }
    getElementRepository(context).create(context, publicContext, element);
    getElementSyncStateRepository(context).create(context, publicContext,
        new SynchronizationStateEntity(element.getId(), elementContext.getRevisionId(),
            publishTime, false));
  }

  @Override
  public void update(SessionContext context, ElementContext elementContext,
                     ElementEntity element, Date publishTime) {
    update(context, new ElementEntityContext(getSpaceName(context, Space.PUBLIC), elementContext),
        element, publishTime);
  }

  @Override
  public void delete(SessionContext context, ElementContext elementContext,
                     ElementEntity element, Date publishTime) {
    ElementEntityContext publicContext =
        new ElementEntityContext(getSpaceName(context, Space.PUBLIC), elementContext);

    if (element.getParentId() != null) {
      updateParentElement(context, publicContext, element.getParentId(), publishTime);
    }

    getElementRepository(context).delete(context, publicContext, element);
    getElementSyncStateRepository(context)
        .delete(context, publicContext, new SynchronizationStateEntity(element.getId(),
            elementContext.getRevisionId()));
  }

  @Override
  public Map<Id, Id> listIds(SessionContext context, ElementContext elementContext) {
    return getElementRepository(context).listIds(context,
        new ElementEntityContext(getSpaceName(context, Space.PUBLIC), elementContext));
  }

  @Override
  public void cleanAll(SessionContext context, ElementContext elementContext) {
    ElementEntityContext publicContext =
        new ElementEntityContext(getSpaceName(context, Space.PUBLIC), elementContext);

    ElementSynchronizationStateRepository elementSyncStateRepository =
        getElementSyncStateRepository(context);

    Set<Id> allElementsIds = new HashSet<>();
    elementSyncStateRepository
        .list(context, publicContext) // in order to find all elements in all revisions
        .forEach(syncState -> allElementsIds.add(syncState.getId()));

    ElementRepository elementRepository = getElementRepository(context);
    allElementsIds.forEach(elementId -> elementRepository
        .cleanAllRevisions(context, publicContext, new ElementEntity(elementId)));

    elementSyncStateRepository.deleteAll(context, publicContext);
  }

  private void update(SessionContext context, ElementEntityContext publicContext,
                      ElementEntity element, Date publishTime) {
    ElementRepository elementRepository = getElementRepository(context);

    Optional<ElementEntity> publicElement = elementRepository.get(context, publicContext, element);
    if (publicElement.isPresent()) { // the element in this revision already exists
      elementRepository.update(context, publicContext, element);
    } else {
      Id revisionId = publicContext.getRevisionId();
      publicContext.setRevisionId(null); // get the element latest revision
      publicElement = elementRepository.get(context, publicContext, element);
      publicContext.setRevisionId(revisionId);

      element.setSubElementIds(publicElement.orElseThrow(() -> new IllegalStateException(String
          .format(ELEMENT_TO_UPDATE_DOES_NOT_EXIST, publicContext.getItemId(),
              publicContext.getVersionId(), element.getId()))).getSubElementIds());
      elementRepository.create(context, publicContext, element); // create a new element revision
    }
    getElementSyncStateRepository(context).update(context, publicContext,
        new SynchronizationStateEntity(element.getId(), publicContext.getRevisionId(), publishTime,
            false));
  }

  private void updateParentElement(SessionContext context, ElementEntityContext publicContext,
                                   Id parentElementId, Date publishTime) {
    ElementRepository elementRepository = getElementRepository(context);
    elementRepository.get(context, publicContext, new ElementEntity(parentElementId))
        .ifPresent(parentElement -> {
          elementRepository.update(context, publicContext, parentElement);
          getElementSyncStateRepository(context).update(context, publicContext,
              new SynchronizationStateEntity(parentElement.getId(), publicContext.getRevisionId(),
                  publishTime, false));
        });
  }

  protected ElementRepository getElementRepository(SessionContext context) {
    return ElementRepositoryFactory.getInstance().createInterface(context);
  }

  protected ElementSynchronizationStateRepository getElementSyncStateRepository(
      SessionContext context) {
    return ElementSynchronizationStateRepositoryFactory.getInstance().createInterface(context);
  }


}
