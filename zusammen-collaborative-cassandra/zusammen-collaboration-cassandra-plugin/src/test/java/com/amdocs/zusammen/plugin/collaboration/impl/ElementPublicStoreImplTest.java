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
import static org.mockito.Mockito.verify;

public class ElementPublicStoreImplTest {

  private static final UserInfo USER = new UserInfo("ElementPublicStoreImplTest_user");
  private static final String PUBLIC_SPACE = ZusammenPluginConstants.PUBLIC_SPACE;
  private static final SessionContext context = TestUtils.createSessionContext(USER, "test");
  private static final Id REVISION_ID = new Id("publicRevision");

  private AutoCloseable mocks;
  private ElementContext elementContext;

  @Mock
  private ElementRepository elementRepositoryMock;
  @Mock
  private ElementSynchronizationStateRepository elementSyncStateRepositoryMock;
  @Spy
  private ElementPublicStoreImpl elementPublicStore;

  @BeforeMethod
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    doReturn(elementRepositoryMock).when(elementPublicStore).getElementRepository(any());
    doReturn(elementSyncStateRepositoryMock).when(elementPublicStore)
        .getElementSyncStateRepository(any());
    elementContext = new ElementContext(new Id(), new Id(), REVISION_ID);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testGet() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    doReturn(Optional.of(element)).when(elementRepositoryMock).get(any(), any(), any());

    Optional<ElementEntity> retrieved =
        elementPublicStore.get(context, elementContext, element.getId());

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), element);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
    verify(elementRepositoryMock)
        .get(same(context), contextCaptor.capture(), elementCaptor.capture());
    assertPublicContext(contextCaptor.getValue());
    Assert.assertEquals(elementCaptor.getValue().getId(), element.getId());
  }

  @Test
  public void testGetWhenNotExist() throws Exception {
    doReturn(Optional.empty()).when(elementRepositoryMock).get(any(), any(), any());

    Assert.assertFalse(elementPublicStore.get(context, elementContext, new Id()).isPresent());
  }

  @Test
  public void testGetDescriptor() throws Exception {
    ElementEntity descriptor = new ElementEntity(new Id());
    doReturn(Optional.of(descriptor)).when(elementRepositoryMock).getDescriptor(any(), any(), any());

    Optional<ElementEntity> retrieved =
        elementPublicStore.getDescriptor(context, elementContext, descriptor.getId());

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), descriptor);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).getDescriptor(same(context), contextCaptor.capture(), any());
    assertPublicContext(contextCaptor.getValue());
  }

  @Test
  public void testListIds() throws Exception {
    Map<Id, Id> ids = new HashMap<>();
    ids.put(new Id("element"), new Id("elementRevision"));
    doReturn(ids).when(elementRepositoryMock).listIds(any(), any());

    Assert.assertEquals(elementPublicStore.listIds(context, elementContext), ids);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).listIds(same(context), contextCaptor.capture());
    assertPublicContext(contextCaptor.getValue());
  }

  @Test
  public void testListSynchronizationStatesKeepsOnlyTheRevisionTheVersionPointsAt()
      throws Exception {
    Id onRevisionA = new Id("onRevisionA");
    Id onRevisionB = new Id("onRevisionB");
    Id withoutState = new Id("withoutState");
    Map<Id, Id> ids = new HashMap<>();
    ids.put(onRevisionA, new Id("revisionA"));
    ids.put(onRevisionB, new Id("revisionB"));
    ids.put(withoutState, new Id("revisionB"));
    doReturn(ids).when(elementRepositoryMock).listIds(any(), any());

    SynchronizationStateEntity wantedOfA =
        new SynchronizationStateEntity(onRevisionA, new Id("revisionA"), new Date(1000L), false);
    SynchronizationStateEntity wantedOfB =
        new SynchronizationStateEntity(onRevisionB, new Id("revisionB"), new Date(2000L), false);
    // the same two elements as they were published in the version's earlier revisions, which the
    // partition read returns as well
    SynchronizationStateEntity staleOfA =
        new SynchronizationStateEntity(onRevisionA, new Id("revisionB"), new Date(3000L), false);
    SynchronizationStateEntity staleOfB =
        new SynchronizationStateEntity(onRevisionB, new Id("revisionA"), new Date(4000L), false);
    doReturn(Arrays.asList(staleOfA, wantedOfA, staleOfB, wantedOfB))
        .when(elementSyncStateRepositoryMock).listPerRevision(any(), any());

    Collection<SynchronizationStateEntity> syncStates =
        elementPublicStore.listSynchronizationStates(context, elementContext);

    Map<Id, Id> revisionIdsById = new HashMap<>();
    for (SynchronizationStateEntity syncState : syncStates) {
      revisionIdsById.put(syncState.getId(), syncState.getRevisionId());
    }
    Map<Id, Id> expectedRevisionIdsById = new HashMap<>();
    expectedRevisionIdsById.put(onRevisionA, new Id("revisionA"));
    expectedRevisionIdsById.put(onRevisionB, new Id("revisionB"));
    Assert.assertEquals(revisionIdsById, expectedRevisionIdsById);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementSyncStateRepositoryMock).listPerRevision(same(context), contextCaptor.capture());
    assertPublicContext(contextCaptor.getValue());
    verify(elementSyncStateRepositoryMock, never()).get(any(), any(), any());
    verify(elementSyncStateRepositoryMock, never()).list(any(), any());
  }

  @Test
  public void testListSynchronizationStatesWhenNoElements() throws Exception {
    doReturn(new HashMap<Id, Id>()).when(elementRepositoryMock).listIds(any(), any());
    doReturn(Arrays.asList(
        new SynchronizationStateEntity(new Id("gone"), new Id("revisionA"), new Date(1000L), false),
        new SynchronizationStateEntity(new Id("gone"), new Id("revisionB"), new Date(2000L), false)))
        .when(elementSyncStateRepositoryMock).listPerRevision(any(), any());

    Assert.assertTrue(
        elementPublicStore.listSynchronizationStates(context, elementContext).isEmpty());
    verify(elementSyncStateRepositoryMock, never()).get(any(), any(), any());
  }

  @Test
  public void testCreateRootElement() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    Date publishTime = new Date(1000L);

    elementPublicStore.create(context, elementContext, element, publishTime);

    verify(elementRepositoryMock, never()).get(any(), any(), any());
    verify(elementRepositoryMock, never()).update(any(), any(), any());

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock)
        .create(same(context), contextCaptor.capture(), same(element));
    assertPublicContext(contextCaptor.getValue());

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .create(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), element.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), REVISION_ID);
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
    Assert.assertFalse(syncStateCaptor.getValue().isDirty());
  }

  @Test
  public void testCreateSubElementRewritesParentFirst() throws Exception {
    ElementEntity parent = new ElementEntity(new Id("parent"));
    ElementEntity element = new ElementEntity(new Id("child"));
    element.setParentId(parent.getId());
    Date publishTime = new Date(2000L);
    doReturn(Optional.of(parent)).when(elementRepositoryMock).get(any(), any(), any());

    elementPublicStore.create(context, elementContext, element, publishTime);

    InOrder order = inOrder(elementRepositoryMock);
    order.verify(elementRepositoryMock).update(same(context), any(), same(parent));
    order.verify(elementRepositoryMock).create(same(context), any(), same(element));

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .update(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), parent.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), REVISION_ID);
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
    Assert.assertFalse(syncStateCaptor.getValue().isDirty());
  }

  @Test
  public void testCreateSubElementWhenParentNotOnPublic() throws Exception {
    ElementEntity element = new ElementEntity(new Id("child"));
    element.setParentId(new Id("parent"));
    doReturn(Optional.empty()).when(elementRepositoryMock).get(any(), any(), any());

    elementPublicStore.create(context, elementContext, element, new Date(2000L));

    verify(elementRepositoryMock, never()).update(any(), any(), any());
    verify(elementRepositoryMock).create(same(context), any(), same(element));
    verify(elementSyncStateRepositoryMock, never()).update(any(), any(), any());
    verify(elementSyncStateRepositoryMock).create(same(context), any(), any());
  }

  @Test
  public void testUpdateElementExistingInRevision() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    Set<Id> subElementIds = new HashSet<>(Collections.singletonList(new Id("sub")));
    element.setSubElementIds(subElementIds);
    Date publishTime = new Date(3000L);
    doReturn(Optional.of(new ElementEntity(element.getId())))
        .when(elementRepositoryMock).get(any(), any(), any());

    elementPublicStore.update(context, elementContext, element, publishTime);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).update(same(context), contextCaptor.capture(), same(element));
    assertPublicContext(contextCaptor.getValue());
    verify(elementRepositoryMock, never()).create(any(), any(), any());
    Assert.assertSame(element.getSubElementIds(), subElementIds);

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .update(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), element.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), REVISION_ID);
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
    Assert.assertFalse(syncStateCaptor.getValue().isDirty());
  }

  @Test
  public void testUpdateCreatesNewRevisionCarryingSubElementIdsOfLatest() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    ElementEntity latestRevision = new ElementEntity(element.getId());
    Set<Id> subElementIds = new HashSet<>(Arrays.asList(new Id("subA"), new Id("subB")));
    latestRevision.setSubElementIds(subElementIds);

    List<Id> revisionIdsSeenByGet = new ArrayList<>();
    doAnswer(invocation -> {
      ElementEntityContext seen = invocation.getArgument(1);
      revisionIdsSeenByGet.add(seen.getRevisionId());
      return revisionIdsSeenByGet.size() == 1 ? Optional.empty() : Optional.of(latestRevision);
    }).when(elementRepositoryMock).get(any(), any(), any());

    elementPublicStore.update(context, elementContext, element, new Date(4000L));

    Assert.assertEquals(revisionIdsSeenByGet, Arrays.asList(REVISION_ID, null));
    Assert.assertEquals(element.getSubElementIds(), subElementIds);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).create(same(context), contextCaptor.capture(), same(element));
    assertPublicContext(contextCaptor.getValue());
    verify(elementRepositoryMock, never()).update(any(), any(), any());

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .update(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), REVISION_ID);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testUpdateWhenElementIsNotOnPublicAtAll() throws Exception {
    doReturn(Optional.empty()).when(elementRepositoryMock).get(any(), any(), any());

    elementPublicStore
        .update(context, elementContext, new ElementEntity(new Id()), new Date(4000L));
  }

  @Test
  public void testDeleteRootElement() throws Exception {
    ElementEntity element = new ElementEntity(new Id());

    elementPublicStore.delete(context, elementContext, element, new Date(5000L));

    verify(elementRepositoryMock, never()).get(any(), any(), any());
    verify(elementRepositoryMock, never()).update(any(), any(), any());

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock).delete(same(context), contextCaptor.capture(), same(element));
    assertPublicContext(contextCaptor.getValue());

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .delete(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), element.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), REVISION_ID);
  }

  @Test
  public void testDeleteSubElementRewritesParentFirst() throws Exception {
    ElementEntity parent = new ElementEntity(new Id("parent"));
    ElementEntity element = new ElementEntity(new Id("child"));
    element.setParentId(parent.getId());
    Date publishTime = new Date(6000L);
    doReturn(Optional.of(parent)).when(elementRepositoryMock).get(any(), any(), any());

    elementPublicStore.delete(context, elementContext, element, publishTime);

    InOrder order = inOrder(elementRepositoryMock);
    order.verify(elementRepositoryMock).update(same(context), any(), same(parent));
    order.verify(elementRepositoryMock).delete(same(context), any(), same(element));

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(elementSyncStateRepositoryMock)
        .update(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), parent.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
  }

  @Test
  public void testCleanAllCleansEachElementOnceAcrossRevisions() throws Exception {
    Id firstId = new Id("first");
    Id secondId = new Id("second");
    doReturn(Arrays.asList(
        new SynchronizationStateEntity(firstId, new Id("revisionA")),
        new SynchronizationStateEntity(firstId, new Id("revisionB")),
        new SynchronizationStateEntity(secondId, new Id("revisionA"))))
        .when(elementSyncStateRepositoryMock).list(any(), any());

    elementPublicStore.cleanAll(context, elementContext);

    ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementRepositoryMock, org.mockito.Mockito.times(2))
        .cleanAllRevisions(same(context), contextCaptor.capture(), elementCaptor.capture());
    Set<Id> cleaned = new HashSet<>();
    for (ElementEntity cleanedElement : elementCaptor.getAllValues()) {
      cleaned.add(cleanedElement.getId());
    }
    Assert.assertEquals(cleaned, new HashSet<>(Arrays.asList(firstId, secondId)));
    assertPublicContext(contextCaptor.getValue());

    verify(elementSyncStateRepositoryMock).deleteAll(same(context), any());
  }

  @Test
  public void testCleanAllWhenNothingPublished() throws Exception {
    doReturn(Collections.emptyList()).when(elementSyncStateRepositoryMock).list(any(), any());

    elementPublicStore.cleanAll(context, elementContext);

    verify(elementRepositoryMock, never()).cleanAllRevisions(any(), any(), any());
    verify(elementSyncStateRepositoryMock).deleteAll(same(context), any());
  }

  private void assertPublicContext(ElementEntityContext captured) {
    Assert.assertEquals(captured.getSpace(), PUBLIC_SPACE);
    Assert.assertEquals(captured.getItemId(), elementContext.getItemId());
    Assert.assertEquals(captured.getVersionId(), elementContext.getVersionId());
    Assert.assertEquals(captured.getRevisionId(), REVISION_ID);
  }
}
