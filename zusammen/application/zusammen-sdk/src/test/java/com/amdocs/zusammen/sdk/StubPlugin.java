/*
 * Copyright © 2026 Deutsche Telekom AG
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

package com.amdocs.zusammen.sdk;

import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Item;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionStatus;
import com.amdocs.zusammen.datatypes.item.Resolution;
import com.amdocs.zusammen.datatypes.itemversion.ItemVersionRevisions;
import com.amdocs.zusammen.datatypes.itemversion.Revision;
import com.amdocs.zusammen.datatypes.itemversion.Tag;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.searchindex.SearchCriteria;
import com.amdocs.zusammen.datatypes.searchindex.SearchResult;
import com.amdocs.zusammen.sdk.collaboration.CollaborationStore;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationItemVersionConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeResult;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationPublishResult;
import com.amdocs.zusammen.sdk.searchindex.SearchIndex;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;
import com.amdocs.zusammen.sdk.state.StateStore;
import com.amdocs.zusammen.sdk.state.types.StateElement;

import java.util.Collection;
import java.util.Date;

/**
 * Stand-in plugin for the *FactoryImpl tests. The factories instantiate the plugin by class name
 * ({@code CommonMethods.newInstance}), so a Mockito mock cannot be used; all three SPIs are
 * implemented by this one class so that a single class name serves every factory test. None of the
 * methods is ever invoked.
 */
public class StubPlugin implements CollaborationStore, StateStore, SearchIndex {

  @Override
  public Response<HealthInfo> checkHealth(SessionContext sessionContext) {
    return null;
  }

  @Override
  public Response<Void> createItem(SessionContext context, Id itemId, Info itemInfo) {
    return null;
  }

  @Override
  public Response<Void> deleteItem(SessionContext context, Id itemId) {
    return null;
  }

  @Override
  public Response<Void> createItemVersion(SessionContext context, Id itemId, Id baseVersionId,
                                          Id versionId, ItemVersionData data) {
    return null;
  }

  @Override
  public Response<Void> updateItemVersion(SessionContext context, Id itemId, Id versionId,
                                          ItemVersionData data) {
    return null;
  }

  @Override
  public Response<Void> deleteItemVersion(SessionContext context, Id itemId, Id versionId) {
    return null;
  }

  @Override
  public Response<ItemVersionStatus> getItemVersionStatus(SessionContext context, Id itemId,
                                                          Id versionId) {
    return null;
  }

  @Override
  public Response<Void> tagItemVersion(SessionContext context, Id itemId, Id versionId, Id changeId,
                                       Tag tag) {
    return null;
  }

  @Override
  public Response<CollaborationPublishResult> publishItemVersion(SessionContext context, Id itemId,
                                                                 Id versionId, String message) {
    return null;
  }

  @Override
  public Response<CollaborationMergeResult> syncItemVersion(SessionContext context, Id itemId,
                                                            Id versionId) {
    return null;
  }

  @Override
  public Response<CollaborationMergeResult> forceSyncItemVersion(SessionContext context, Id itemId,
                                                                 Id versionId) {
    return null;
  }

  @Override
  public Response<CollaborationMergeResult> mergeItemVersion(SessionContext context, Id itemId,
                                                             Id versionId, Id sourceVersionId) {
    return null;
  }

  @Override
  public Response<CollaborationItemVersionConflict> getItemVersionConflict(SessionContext context,
                                                                          Id itemId,
                                                                          Id versionId) {
    return null;
  }

  @Override
  public Response<ItemVersionRevisions> listItemVersionRevisions(SessionContext context, Id itemId,
                                                                 Id versionId) {
    return null;
  }

  @Override
  public Response<Revision> getItemVersionRevision(SessionContext context, Id itemId, Id versionId,
                                                   Id revisionId) {
    return null;
  }

  @Override
  public Response<CollaborationMergeChange> resetItemVersionRevision(SessionContext context,
                                                                     Id itemId, Id versionId,
                                                                     Id revisionId) {
    return null;
  }

  @Override
  public Response<CollaborationMergeChange> revertItemVersionRevision(SessionContext context,
                                                                      Id itemId, Id versionId,
                                                                      Id revisionId) {
    return null;
  }

  @Override
  public Response<Void> commitElements(SessionContext context, Id itemId, Id versionId,
                                       String message) {
    return null;
  }

  @Override
  public Response<Collection<CollaborationElement>> listElements(SessionContext context,
                                                                 ElementContext elementContext,
                                                                 Namespace namespace,
                                                                 Id elementId) {
    return null;
  }

  @Override
  public Response<CollaborationElement> getElement(SessionContext context,
                                                   ElementContext elementContext,
                                                   Namespace namespace, Id elementId) {
    return null;
  }

  @Override
  public Response<CollaborationElementConflict> getElementConflict(SessionContext context,
                                                                   ElementContext elementContext,
                                                                   Namespace namespace,
                                                                   Id elementId) {
    return null;
  }

  @Override
  public Response<Void> createElement(SessionContext context, CollaborationElement element) {
    return null;
  }

  @Override
  public Response<Void> updateElement(SessionContext context, CollaborationElement element) {
    return null;
  }

  @Override
  public Response<Void> deleteElement(SessionContext context, CollaborationElement element) {
    return null;
  }

  @Override
  public Response<CollaborationMergeResult> resolveElementConflict(SessionContext context,
                                                                   CollaborationElement element,
                                                                   Resolution resolution) {
    return null;
  }

  @Override
  public Response<ItemVersion> getItemVersion(SessionContext context, Space space, Id itemId,
                                              Id versionId, Id revisionId) {
    return null;
  }

  @Override
  public Response<Collection<Item>> listItems(SessionContext context) {
    return null;
  }

  @Override
  public Response<Boolean> isItemExist(SessionContext context, Id itemId) {
    return null;
  }

  @Override
  public Response<Item> getItem(SessionContext context, Id itemId) {
    return null;
  }

  @Override
  public Response<Void> createItem(SessionContext context, Id itemId, Info itemInfo,
                                   Date creationTime) {
    return null;
  }

  @Override
  public Response<Void> updateItem(SessionContext context, Id itemId, Info itemInfo,
                                   Date modificationTime) {
    return null;
  }

  @Override
  public Response<Collection<ItemVersion>> listItemVersions(SessionContext context, Space space,
                                                            Id itemId) {
    return null;
  }

  @Override
  public Response<Boolean> isItemVersionExist(SessionContext context, Space space, Id itemId,
                                              Id versionId) {
    return null;
  }

  @Override
  public Response<ItemVersion> getItemVersion(SessionContext context, Space space, Id itemId,
                                              Id versionId) {
    return null;
  }

  @Override
  public Response<Void> createItemVersion(SessionContext context, Space space, Id itemId,
                                          Id baseVersionId, Id versionId, ItemVersionData data,
                                          Date creationTime) {
    return null;
  }

  @Override
  public Response<Void> updateItemVersion(SessionContext context, Space space, Id itemId,
                                          Id versionId, ItemVersionData data,
                                          Date modificationTime) {
    return null;
  }

  @Override
  public Response<Void> deleteItemVersion(SessionContext context, Space space, Id itemId,
                                          Id versionId) {
    return null;
  }

  @Override
  public Response<Collection<StateElement>> listElements(SessionContext context,
                                                         ElementContext elementContext,
                                                         Id elementId) {
    return null;
  }

  @Override
  public Response<Boolean> isElementExist(SessionContext context, ElementContext elementContext,
                                          Id elementId) {
    return null;
  }

  @Override
  public Response<Namespace> getElementNamespace(SessionContext context, Id itemId, Id elementId) {
    return null;
  }

  @Override
  public Response<StateElement> getElement(SessionContext context, ElementContext elementContext,
                                           Id elementId) {
    return null;
  }

  @Override
  public Response<Void> createElement(SessionContext context, StateElement element) {
    return null;
  }

  @Override
  public Response<Void> updateElement(SessionContext context, StateElement element) {
    return null;
  }

  @Override
  public Response<Void> deleteElement(SessionContext context, StateElement element) {
    return null;
  }

  @Override
  public Response<Void> updateItemModificationTime(SessionContext context, Id itemId,
                                                   Date modificationTime) {
    return null;
  }

  @Override
  public Response<Void> updateItemVersionModificationTime(SessionContext context, Space space,
                                                          Id itemId, Id versionId,
                                                          Date modificationTime) {
    return null;
  }

  @Override
  public Response<Void> createElement(SessionContext sessionContext,
                                      SearchIndexElement elementSearchableData) {
    return null;
  }

  @Override
  public Response<Void> updateElement(SessionContext sessionContext,
                                      SearchIndexElement elementSearchableData) {
    return null;
  }

  @Override
  public Response<Void> deleteElement(SessionContext sessionContext,
                                      SearchIndexElement elementSearchableData) {
    return null;
  }

  @Override
  public Response<SearchResult> search(SessionContext sessionContext,
                                       SearchCriteria searchCriteria) {
    return null;
  }
}
