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

package com.amdocs.zusammen.plugin.collaboration.impl;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.plugin.ZusammenPluginConstants;
import com.amdocs.zusammen.plugin.collaboration.TestUtils;
import com.amdocs.zusammen.plugin.dao.ElementRepository;
import com.amdocs.zusammen.plugin.dao.ElementSynchronizationStateRepository;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ElementPrivateStoreImplTest {

  private static final UserInfo USER = new UserInfo("ElementPrivateStoreImplTest_user");
  private static final String PRIVATE_SPACE = USER.getUserName();
  private static final SessionContext context = TestUtils.createSessionContext(USER, "test");
  /**
   * Deliberately not {@link Id#ZERO}: the private store is expected to overwrite the incoming
   * revision with Id.ZERO on every element access, and a non-zero value here is what proves it.
   */
  private static final Id INCOMING_REVISION_ID = new Id("incomingRevision");

  private AutoCloseable mocks;
  private ElementContext elementContext;

  @Mock
  private ElementRepository elementRepositoryMock;
  @Mock
  private ElementSynchronizationStateRepository elementSyncStateRepositoryMock;
  @Spy
  private ElementPrivateStoreImpl elementPrivateStore;

  @BeforeMethod
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    doReturn(elementRepositoryMock).when(elementPrivateStore).getElementRepository(any());
    doReturn(elementSyncStateRepositoryMock).when(elementPrivateStore)
        .getElementSyncStateRepository(any());
    elementContext = new ElementContext(new Id(), new Id(), INCOMING_REVISION_ID);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testListIdsPassesTheIncomingRevisionThrough() throws Exception {
    Map<Id, Id> ids = new HashMap<>();
    ids.put(new Id("element"), new Id("elementRevision"));
    doReturn(ids).when(elementRepositoryMock).listIds(any(), any());

    Assert.assertEquals(elementPrivateStore.listIds(context, elementContext), ids);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).listIds(same(context), contextCaptor.capture());
    assertPrivateContext(contextCaptor.getValue(), INCOMING_REVISION_ID);
  }

  @Test
  public void testCleanAllCleansEveryElementKnownToTheSyncState() throws Exception {
    Id firstId = new Id("first");
    Id secondId = new Id("second");
    doReturn(Arrays.asList(new SynchronizationStateEntity(firstId, Id.ZERO),
        new SynchronizationStateEntity(secondId, Id.ZERO)))
        .when(elementSyncStateRepositoryMock).list(any(), any());

    elementPrivateStore.cleanAll(context, elementContext);

    ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
    verify(elementRepositoryMock, times(2))
        .cleanAllRevisions(same(context), any(), elementCaptor.capture());
    Assert.assertEquals(idsOf(elementCaptor.getAllValues()),
        new HashSet<>(Arrays.asList(firstId, secondId)));

    verify(elementSyncStateRepositoryMock).deleteAll(same(context), any());
  }

  @Test
  public void testCleanAllWhenNothingStored() throws Exception {
    doReturn(Collections.emptyList()).when(elementSyncStateRepositoryMock).list(any(), any());

    elementPrivateStore.cleanAll(context, elementContext);

    verify(elementRepositoryMock, never()).cleanAllRevisions(any(), any(), any());
    verify(elementSyncStateRepositoryMock).deleteAll(same(context), any());
  }

  @Test
  public void testListSubsOfRootWhenElementIdIsNull() throws Exception {
    ElementEntity root = new ElementEntity(ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID);
    ElementEntity sub = new ElementEntity(new Id("sub"));
    root.setSubElementIds(new LinkedHashSet<>(Collections.singletonList(sub.getId())));
    stubElements(root, sub);

    Collection<ElementEntity> subs = elementPrivateStore.listSubs(context, elementContext, null);

    Assert.assertEquals(new ArrayList<>(subs), Collections.singletonList(sub));

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
    verify(elementRepositoryMock, times(2))
        .get(same(context), contextCaptor.capture(), elementCaptor.capture());
    assertPrivateContext(contextCaptor.getAllValues().get(0), Id.ZERO);
    Assert.assertEquals(elementCaptor.getAllValues().get(0).getId(),
        ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID);
  }

  @Test
  public void testListSubsReturnsEverySubElement() throws Exception {
    ElementEntity parent = new ElementEntity(new Id("parent"));
    ElementEntity firstSub = new ElementEntity(new Id("firstSub"));
    ElementEntity secondSub = new ElementEntity(new Id("secondSub"));
    parent.setSubElementIds(
        new LinkedHashSet<>(Arrays.asList(firstSub.getId(), secondSub.getId())));
    stubElements(parent, firstSub, secondSub);

    Collection<ElementEntity> subs =
        elementPrivateStore.listSubs(context, elementContext, parent.getId());

    Assert.assertEquals(idsOf(subs),
        new HashSet<>(Arrays.asList(firstSub.getId(), secondSub.getId())));
  }

  @Test
  public void testListSubsWhenElementHasNoSubs() throws Exception {
    ElementEntity parent = new ElementEntity(new Id("parent"));
    stubElements(parent);

    Assert.assertTrue(
        elementPrivateStore.listSubs(context, elementContext, parent.getId()).isEmpty());
  }

  @Test
  public void testListSubsWhenElementDoesNotExist() throws Exception {
    doReturn(Optional.empty()).when(elementRepositoryMock).get(any(), any(), any());

    Assert.assertTrue(elementPrivateStore.listSubs(context, elementContext, new Id()).isEmpty());
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testListSubsWhenSubElementDoesNotExist() throws Exception {
    ElementEntity parent = new ElementEntity(new Id("parent"));
    parent.setSubElementIds(new LinkedHashSet<>(Collections.singletonList(new Id("ghost"))));
    stubElements(parent);

    elementPrivateStore.listSubs(context, elementContext, parent.getId());
  }

  @Test
  public void testGet() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    doReturn(Optional.of(element)).when(elementRepositoryMock).get(any(), any(), any());

    Optional<ElementEntity> retrieved =
        elementPrivateStore.get(context, elementContext, element.getId());

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), element);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
    verify(elementRepositoryMock)
        .get(same(context), contextCaptor.capture(), elementCaptor.capture());
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);
    Assert.assertEquals(elementCaptor.getValue().getId(), element.getId());
  }

  @Test
  public void testGetWhenNotExist() throws Exception {
    doReturn(Optional.empty()).when(elementRepositoryMock).get(any(), any(), any());

    Assert.assertFalse(elementPrivateStore.get(context, elementContext, new Id()).isPresent());
  }

  @Test
  public void testGetDescriptor() throws Exception {
    ElementEntity descriptor = new ElementEntity(new Id());
    doReturn(Optional.of(descriptor))
        .when(elementRepositoryMock).getDescriptor(any(), any(), any());

    Optional<ElementEntity> retrieved =
        elementPrivateStore.getDescriptor(context, elementContext, descriptor.getId());

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), descriptor);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).getDescriptor(same(context), contextCaptor.capture(), any());
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);
  }

  @Test
  public void testListSynchronizationStates() throws Exception {
    Collection<SynchronizationStateEntity> syncStates = Arrays.asList(
        new SynchronizationStateEntity(new Id("first"), Id.ZERO),
        new SynchronizationStateEntity(new Id("second"), Id.ZERO));
    doReturn(syncStates).when(elementSyncStateRepositoryMock).list(any(), any());

    Assert.assertEquals(
        elementPrivateStore.listSynchronizationStates(context, elementContext), syncStates);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementSyncStateRepositoryMock).list(same(context), contextCaptor.capture());
    assertPrivateContext(contextCaptor.getValue(), INCOMING_REVISION_ID);
  }

  @Test
  public void testGetSynchronizationState() throws Exception {
    Id elementId = new Id();
    SynchronizationStateEntity syncState =
        new SynchronizationStateEntity(elementId, Id.ZERO, new Date(1000L), true);
    doReturn(Optional.of(syncState)).when(elementSyncStateRepositoryMock).get(any(), any(), any());

    Optional<SynchronizationStateEntity> retrieved =
        elementPrivateStore.getSynchronizationState(context, elementContext, elementId);

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), syncState);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .get(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), elementId);
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
  }

  @Test
  public void testCreateMarksTheElementDirtyAndUnpublished() throws Exception {
    ElementEntity element = new ElementEntity(new Id());

    elementPrivateStore.create(context, elementContext, element);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).create(same(context), contextCaptor.capture(), same(element));
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);

    SynchronizationStateEntity created = captureCreatedSyncState();
    Assert.assertEquals(created.getId(), element.getId());
    Assert.assertEquals(created.getRevisionId(), Id.ZERO);
    Assert.assertNull(created.getPublishTime());
    Assert.assertTrue(created.isDirty());
  }

  @Test
  public void testUpdateWhenDataChanged() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    element.setElementHash(new Id("newHash"));
    doReturn(Optional.of(new Id("storedHash")))
        .when(elementRepositoryMock).getHash(any(), any(), any());

    Assert.assertTrue(elementPrivateStore.update(context, elementContext, element));

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).update(same(context), contextCaptor.capture(), same(element));
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .markAsDirty(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), element.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
  }

  @Test
  public void testUpdateWhenDataUnchanged() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    element.setElementHash(new Id("sameHash"));
    doReturn(Optional.of(new Id("sameHash")))
        .when(elementRepositoryMock).getHash(any(), any(), any());

    Assert.assertFalse(elementPrivateStore.update(context, elementContext, element));

    verify(elementRepositoryMock, never()).update(any(), any(), any());
    verifyNoInteractions(elementSyncStateRepositoryMock);
  }

  @Test
  public void testUpdateWhenElementHasNoStoredHash() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    element.setElementHash(new Id("newHash"));
    doReturn(Optional.empty()).when(elementRepositoryMock).getHash(any(), any(), any());

    Assert.assertTrue(elementPrivateStore.update(context, elementContext, element));

    verify(elementRepositoryMock).update(same(context), any(), same(element));
    verify(elementSyncStateRepositoryMock).markAsDirty(same(context), any(), any());
  }

  @Test
  public void testDeleteNeverPublishedElementDropsItsSyncState() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    stubElements(element);
    stubSyncState(new SynchronizationStateEntity(element.getId(), Id.ZERO, null, true));

    elementPrivateStore.delete(context, elementContext, element);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).delete(same(context), contextCaptor.capture(), same(element));
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .delete(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), element.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
    verify(elementSyncStateRepositoryMock, never()).markAsDirty(any(), any(), any());
  }

  @Test
  public void testDeletePublishedElementKeepsSyncStateAsDirty() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    stubElements(element);
    stubSyncState(
        new SynchronizationStateEntity(element.getId(), Id.ZERO, new Date(1000L), false));

    elementPrivateStore.delete(context, elementContext, element);

    verify(elementRepositoryMock).delete(same(context), any(), same(element));

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .markAsDirty(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), element.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
    verify(elementSyncStateRepositoryMock, never()).delete(any(), any(), any());
  }

  @Test
  public void testDeleteRemovesTheWholeSubElementHierarchyBottomUp() throws Exception {
    ElementEntity root = new ElementEntity(new Id("root"));
    ElementEntity child = new ElementEntity(new Id("child"));
    ElementEntity grandChild = new ElementEntity(new Id("grandChild"));
    ElementEntity sibling = new ElementEntity(new Id("sibling"));
    root.setSubElementIds(new LinkedHashSet<>(Arrays.asList(child.getId(), sibling.getId())));
    child.setSubElementIds(new LinkedHashSet<>(Collections.singletonList(grandChild.getId())));
    stubElements(root, child, grandChild, sibling);
    stubSyncStatePerElement();

    elementPrivateStore.delete(context, elementContext, root);

    ArgumentCaptor<ElementEntity> deletedCaptor = ArgumentCaptor.forClass(ElementEntity.class);
    verify(elementRepositoryMock, times(4))
        .delete(same(context), any(), deletedCaptor.capture());
    Assert.assertEquals(idsOf(deletedCaptor.getAllValues()), new HashSet<>(Arrays
        .asList(root.getId(), child.getId(), grandChild.getId(), sibling.getId())));

    List<Id> deletionOrder = new ArrayList<>();
    for (ElementEntity deleted : deletedCaptor.getAllValues()) {
      deletionOrder.add(deleted.getId());
    }
    Assert.assertTrue(
        deletionOrder.indexOf(grandChild.getId()) < deletionOrder.indexOf(child.getId()),
        "a sub element must be deleted before its parent");
    Assert.assertEquals(deletionOrder.get(3), root.getId());

    verify(elementSyncStateRepositoryMock, times(4)).delete(same(context), any(), any());
  }

  @Test
  public void testDeleteOnlyTheDeletedElementCarriesItsParentId() throws Exception {
    ElementEntity root = new ElementEntity(new Id("root"));
    root.setParentId(new Id("theParent"));
    ElementEntity child = new ElementEntity(new Id("child"));
    root.setSubElementIds(new LinkedHashSet<>(Collections.singletonList(child.getId())));
    stubElements(root, child);
    stubSyncStatePerElement();

    elementPrivateStore.delete(context, elementContext, root);

    ArgumentCaptor<ElementEntity> deletedCaptor = ArgumentCaptor.forClass(ElementEntity.class);
    verify(elementRepositoryMock, times(2))
        .delete(same(context), any(), deletedCaptor.capture());
    Assert.assertNull(deletedCaptor.getAllValues().get(0).getParentId());
    Assert.assertEquals(deletedCaptor.getAllValues().get(1).getParentId(), new Id("theParent"));
  }

  @Test
  public void testDeleteWhenElementDoesNotExist() throws Exception {
    doReturn(Optional.empty()).when(elementRepositoryMock).get(any(), any(), any());

    elementPrivateStore.delete(context, elementContext, new ElementEntity(new Id()));

    verify(elementRepositoryMock, never()).delete(any(), any(), any());
    verifyNoInteractions(elementSyncStateRepositoryMock);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testDeleteWhenSynchronizationStateIsMissing() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    stubElements(element);
    doReturn(Optional.empty()).when(elementSyncStateRepositoryMock).get(any(), any(), any());

    elementPrivateStore.delete(context, elementContext, element);
  }

  @Test
  public void testMarkAsPublished() throws Exception {
    Id elementId = new Id();
    Date publishTime = new Date(2000L);

    elementPrivateStore.markAsPublished(context, elementContext, elementId, publishTime);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .update(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), elementId);
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
    Assert.assertFalse(syncStateCaptor.getValue().isDirty());
    verifyNoInteractions(elementRepositoryMock);
  }

  @Test
  public void testMarkDeletionAsPublished() throws Exception {
    Id elementId = new Id();

    elementPrivateStore.markDeletionAsPublished(context, elementContext, elementId, new Date(2000L));

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .delete(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), elementId);
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
    verifyNoInteractions(elementRepositoryMock);
  }

  @Test
  public void testCommitStagedCreateIsNotDirty() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    Date publishTime = new Date(3000L);

    elementPrivateStore.commitStagedCreate(context, elementContext, element, publishTime);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).create(same(context), contextCaptor.capture(), same(element));
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);

    SynchronizationStateEntity created = captureCreatedSyncState();
    Assert.assertEquals(created.getId(), element.getId());
    Assert.assertEquals(created.getRevisionId(), Id.ZERO);
    Assert.assertEquals(created.getPublishTime(), publishTime);
    Assert.assertFalse(created.isDirty());
  }

  @Test
  public void testCommitStagedUpdateClearsTheDirtyFlag() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    Date publishTime = new Date(4000L);

    elementPrivateStore.commitStagedUpdate(context, elementContext, element, publishTime);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).update(same(context), contextCaptor.capture(), same(element));
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .update(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), element.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
    Assert.assertFalse(syncStateCaptor.getValue().isDirty());
  }

  @Test
  public void testCommitStagedDelete() throws Exception {
    ElementEntity element = new ElementEntity(new Id());

    elementPrivateStore.commitStagedDelete(context, elementContext, element);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).delete(same(context), contextCaptor.capture(), same(element));
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .delete(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), element.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
    verify(elementRepositoryMock, never()).get(any(), any(), any());
  }

  @Test
  public void testCommitStagedIgnoreKeepsTheElementDirty() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    Date publishTime = new Date(5000L);

    elementPrivateStore.commitStagedIgnore(context, elementContext, element, publishTime);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .update(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    Assert.assertEquals(contextCaptor.getValue().getSpace(), PRIVATE_SPACE);
    Assert.assertEquals(contextCaptor.getValue().getItemId(), elementContext.getItemId());
    Assert.assertEquals(contextCaptor.getValue().getVersionId(), elementContext.getVersionId());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), element.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
    Assert.assertTrue(syncStateCaptor.getValue().isDirty());
    verifyNoInteractions(elementRepositoryMock);
  }

  private void stubElements(ElementEntity... stored) {
    Map<Id, ElementEntity> storedById = new HashMap<>();
    for (ElementEntity element : stored) {
      storedById.put(element.getId(), element);
    }
    doAnswer(invocation -> {
      ElementEntity requested = invocation.getArgument(2);
      return Optional.ofNullable(storedById.get(requested.getId()));
    }).when(elementRepositoryMock).get(any(), any(), any());
  }

  private void stubSyncState(SynchronizationStateEntity syncState) {
    doReturn(Optional.of(syncState)).when(elementSyncStateRepositoryMock).get(any(), any(), any());
  }

  private void stubSyncStatePerElement() {
    doAnswer(invocation -> {
      SynchronizationStateEntity requested = invocation.getArgument(2);
      return Optional.of(new SynchronizationStateEntity(requested.getId(), Id.ZERO, null, true));
    }).when(elementSyncStateRepositoryMock).get(any(), any(), any());
  }

  private SynchronizationStateEntity captureCreatedSyncState() {
    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .create(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    assertPrivateContext(contextCaptor.getValue(), Id.ZERO);
    return syncStateCaptor.getValue();
  }

  private void assertPrivateContext(ElementEntityContext captured, Id expectedRevisionId) {
    Assert.assertEquals(captured.getSpace(), PRIVATE_SPACE);
    Assert.assertEquals(captured.getItemId(), elementContext.getItemId());
    Assert.assertEquals(captured.getVersionId(), elementContext.getVersionId());
    Assert.assertEquals(captured.getRevisionId(), expectedRevisionId);
  }

  private static Set<Id> idsOf(Collection<ElementEntity> elements) {
    Set<Id> ids = new HashSet<>();
    for (ElementEntity element : elements) {
      ids.add(element.getId());
    }
    return ids;
  }
}
