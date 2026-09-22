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

package com.amdocs.zusammen.plugin.collaboration;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.StageEntity;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.dao.types.VersionEntity;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeResult;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SyncServiceTest {

  private static final SessionContext CONTEXT =
      TestUtils.createSessionContext(new UserInfo("SyncServiceTest_user"), "test");
  private static final Id ITEM_ID = new Id("item");
  private static final Id VERSION_ID = new Id("version");
  private static final Id PUBLIC_REVISION_ID = new Id("public-revision");
  private static final Date LAST_SYNCED_PUBLISH_TIME = new Date(1_000_000_000_000L);
  private static final Date PUBLIC_PUBLISH_TIME = new Date(2_000_000_000_000L);

  /** Public elements are read at the revision the public version currently points at. */
  private static final ElementContext PUBLIC_ELEMENT_CONTEXT =
      new ElementContext(ITEM_ID, VERSION_ID, PUBLIC_REVISION_ID);

  @Mock
  private VersionPublicStore versionPublicStore;
  @Mock
  private VersionPrivateStore versionPrivateStore;
  @Mock
  private VersionStageStore versionStageStore;
  @Mock
  private ElementPublicStore elementPublicStore;
  @Mock
  private ElementPrivateStore elementPrivateStore;
  @Mock
  private ElementStageStore elementStageStore;

  private SyncService syncService;
  private AutoCloseable mocks;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    syncService = new SyncService(versionPublicStore, versionPrivateStore, versionStageStore,
        elementPublicStore, elementPrivateStore, elementStageStore);
    when(elementPublicStore.listSynchronizationStates(any(), any()))
        .thenReturn(Collections.emptyList());
    when(elementPrivateStore.listSynchronizationStates(any(), any()))
        .thenReturn(Collections.emptyList());
    when(elementPrivateStore.getDescriptor(any(), any(), any())).thenReturn(Optional.empty());
    // Id.ZERO is the version data element, the parent of every root element. A private space that
    // holds the version at all holds it, so the search for the root of a change stops there.
    when(elementPrivateStore.getDescriptor(any(), any(), eq(Id.ZERO)))
        .thenReturn(Optional.of(new ElementEntity(Id.ZERO)));
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testSyncRejectsAVersionThatWasNeverPublished() {
    when(versionPublicStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID, null))
        .thenReturn(Optional.empty());

    try {
      syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("A version that does not exist on public cannot be synced");
    } catch (UnsupportedOperationException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: Non existing version cannot be synced.",
          ITEM_ID, VERSION_ID));
    }
    verifyNoInteractions(versionStageStore);
    verifyNoInteractions(elementStageStore);
  }

  @Test
  public void testSyncDoesNothingWhenPrivateIsAlreadyAtTheLatestPublicRevision() {
    publicVersionSyncState();
    privateVersionSyncState(PUBLIC_PUBLISH_TIME);

    CollaborationMergeResult result = syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    verifyNoInteractions(versionStageStore);
    verifyNoInteractions(elementStageStore);
    verify(elementPublicStore, never()).listSynchronizationStates(any(), any());
    Assert.assertNotNull(result.getChange());
    Assert.assertNotNull(result.getConflict());
  }

  @Test
  public void testSyncReturnsAnInitialisedMergeResult() {
    givenPublicVersionAheadOfPrivate();

    CollaborationMergeResult result = syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    Assert.assertNotNull(result.getChange());
    Assert.assertNotNull(result.getConflict());
  }

  @Test
  public void testSyncStagesAVersionUpdateWhenTheVersionAlreadyExistsOnPrivate() {
    givenPublicVersionAheadOfPrivate();

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<VersionEntity> stage = capturedVersionStage();
    Assert.assertEquals(stage.getEntity().getId(), VERSION_ID);
    Assert.assertEquals(stage.getAction(), Action.UPDATE);
    Assert.assertEquals(stage.getPublishTime(), PUBLIC_PUBLISH_TIME);
    Assert.assertFalse(stage.isConflicted());
    verify(versionPublicStore, never()).get(any(), any(), any());
  }

  @Test
  public void testSyncStagesAVersionCreateWhenTheVersionIsNewToPrivate() {
    VersionEntity publicVersion = new VersionEntity(VERSION_ID);
    publicVersion.setBaseId(new Id("base"));
    publicVersion.setCreationTime(new Date(10L));
    publicVersion.setModificationTime(new Date(20L));
    givenVersionMissingFromPrivate(publicVersion);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<VersionEntity> stage = capturedVersionStage();
    Assert.assertSame(stage.getEntity(), publicVersion);
    Assert.assertEquals(stage.getAction(), Action.CREATE);
    Assert.assertEquals(stage.getPublishTime(), PUBLIC_PUBLISH_TIME);
    Assert.assertFalse(stage.isConflicted());
  }

  @Test
  public void testSyncFailsWhenTheVersionToCopyIsMissingFromPublic() {
    publicVersionSyncState();
    when(versionPrivateStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID))
        .thenReturn(Optional.empty());
    when(versionPublicStore.get(CONTEXT, ITEM_ID, VERSION_ID)).thenReturn(Optional.empty());

    try {
      syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("A public sync state without a public version must fail");
    } catch (IllegalArgumentException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: Non existing version cannot be synced.",
          ITEM_ID, VERSION_ID));
    }
    verifyNoInteractions(versionStageStore);
  }

  @Test
  public void testSyncStagesTheVersionBeforeItsElements() {
    givenPublicVersionAheadOfPrivate();
    ElementEntity publicElement = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "public");
    publicElementSyncStates(publicSyncState(publicElement.getId(), PUBLIC_PUBLISH_TIME));
    publicElement(publicElement);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    InOrder inOrder = inOrder(versionStageStore, elementStageStore);
    inOrder.verify(versionStageStore).create(any(), any(), any());
    inOrder.verify(elementStageStore).create(any(), any(), any());
  }

  @Test
  public void testSyncStagesElementsAtThePublicRevisionContext() {
    givenPublicVersionAheadOfPrivate();
    ElementEntity publicElement = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "public");
    publicElementSyncStates(publicSyncState(publicElement.getId(), PUBLIC_PUBLISH_TIME));
    publicElement(publicElement);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    ArgumentCaptor<ElementContext> contextCaptor = ArgumentCaptor.forClass(ElementContext.class);
    verify(elementStageStore).create(eq(CONTEXT), contextCaptor.capture(), any());
    Assert.assertEquals(contextCaptor.getValue(), PUBLIC_ELEMENT_CONTEXT);
  }

  @Test
  public void testSyncStagesAPlainUpdateForAnElementThatWasNotChangedLocally() {
    Id elementId = new Id("e1");
    ElementEntity publicElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "public");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(elementId, PUBLIC_PUBLISH_TIME));
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(elementId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false));
    publicElement(publicElement);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedElementStage();
    Assert.assertSame(stage.getEntity(), publicElement);
    Assert.assertEquals(stage.getAction(), Action.UPDATE);
    Assert.assertEquals(stage.getPublishTime(), PUBLIC_PUBLISH_TIME);
    Assert.assertFalse(stage.isConflicted());
    Assert.assertTrue(stage.getConflictDependents().isEmpty());
    verify(elementPrivateStore, never()).get(any(), any(), eq(elementId));
  }

  @Test
  public void testSyncStagesAConflictWhenTheElementChangedOnBothSides() {
    Id elementId = new Id("e1");
    ElementEntity publicElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "public");
    ElementEntity privateElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "local");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(elementId, PUBLIC_PUBLISH_TIME));
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(elementId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    publicElement(publicElement);
    privateElement(privateElement);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedElementStage();
    Assert.assertSame(stage.getEntity(), publicElement);
    Assert.assertEquals(stage.getAction(), Action.UPDATE);
    Assert.assertTrue(stage.isConflicted());
    Assert.assertTrue(stage.getConflictDependents().isEmpty());
  }

  @Test
  public void testSyncStagesAPlainUpdateWhenBothSidesEndedUpWithTheSameContent() {
    Id elementId = new Id("e1");
    ElementEntity publicElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "same");
    ElementEntity privateElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "same");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(elementId, PUBLIC_PUBLISH_TIME));
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(elementId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    publicElement(publicElement);
    privateElement(privateElement);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedElementStage();
    Assert.assertEquals(stage.getAction(), Action.UPDATE);
    Assert.assertFalse(stage.isConflicted(),
        "Identical content on both sides must not be reported as a conflict");
  }

  @Test
  public void testSyncOnlyConsidersPublicElementsPublishedSinceTheLastSync() {
    Id unchangedId = new Id("published-before-last-sync");
    Id changedId = new Id("published-after-last-sync");
    ElementEntity changed = CollaborationTestFixtures.element(changedId, Id.ZERO, "public");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(unchangedId, LAST_SYNCED_PUBLISH_TIME),
        publicSyncState(changedId, PUBLIC_PUBLISH_TIME));
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(unchangedId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false),
        CollaborationTestFixtures.syncState(changedId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false));
    publicElement(changed);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedElementStage();
    Assert.assertEquals(stage.getEntity().getId(), changedId);
    verify(elementPublicStore, never()).get(any(), any(), eq(unchangedId));
  }

  @Test
  public void testSyncStagesEveryPublicElementWhenTheVersionIsNewToPrivate() {
    ElementEntity first = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "first");
    ElementEntity second = CollaborationTestFixtures.element(new Id("e2"), Id.ZERO, "second");
    givenVersionMissingFromPrivate(new VersionEntity(VERSION_ID));
    publicElementSyncStates(publicSyncState(first.getId(), LAST_SYNCED_PUBLISH_TIME),
        publicSyncState(second.getId(), PUBLIC_PUBLISH_TIME));
    publicElement(first);
    publicElement(second);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    List<StageEntity<ElementEntity>> stages = capturedElementStages();
    Assert.assertEquals(stages.size(), 2);
    Assert.assertSame(stages.get(0).getEntity(), first);
    Assert.assertEquals(stages.get(0).getAction(), Action.CREATE);
    Assert.assertEquals(stages.get(0).getPublishTime(), LAST_SYNCED_PUBLISH_TIME);
    Assert.assertSame(stages.get(1).getEntity(), second);
    Assert.assertEquals(stages.get(1).getPublishTime(), PUBLIC_PUBLISH_TIME);
  }

  @Test
  public void testSyncStagesTheWholeNewPublicSubTreeOnceWhenStartingFromALeaf() {
    Id parentId = new Id("parent");
    Id childId = new Id("child");
    ElementEntity parent = CollaborationTestFixtures.element(parentId, Id.ZERO, "parent", childId);
    ElementEntity child = CollaborationTestFixtures.element(childId, parentId, "child");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(childId, PUBLIC_PUBLISH_TIME),
        publicSyncState(parentId, PUBLIC_PUBLISH_TIME));
    publicElement(parent);
    publicElement(child);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    List<StageEntity<ElementEntity>> stages = capturedElementStages();
    Assert.assertEquals(stages.size(), 2, "The tree must be staged once, not once per member");
    Assert.assertSame(stages.get(0).getEntity(), child);
    Assert.assertEquals(stages.get(0).getAction(), Action.CREATE);
    Assert.assertFalse(stages.get(0).isConflicted());
    Assert.assertSame(stages.get(1).getEntity(), parent);
    Assert.assertEquals(stages.get(1).getAction(), Action.CREATE);
    Assert.assertFalse(stages.get(1).isConflicted());
    Assert.assertTrue(stages.get(1).getConflictDependents().isEmpty());
  }

  @Test
  public void testSyncStagesTheVersionDataElementTreeWithoutSearchingForAParent() {
    Id childId = new Id("child");
    ElementEntity versionData =
        CollaborationTestFixtures.element(Id.ZERO, null, "version data", childId);
    ElementEntity child = CollaborationTestFixtures.element(childId, Id.ZERO, "child");
    givenVersionMissingFromPrivate(new VersionEntity(VERSION_ID));
    publicElementSyncStates(publicSyncState(Id.ZERO, PUBLIC_PUBLISH_TIME),
        publicSyncState(childId, PUBLIC_PUBLISH_TIME));
    publicElement(versionData);
    publicElement(child);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    List<StageEntity<ElementEntity>> stages = capturedElementStages();
    Assert.assertEquals(stages.size(), 2);
    Assert.assertSame(stages.get(0).getEntity(), child);
    Assert.assertSame(stages.get(1).getEntity(), versionData);
    verify(elementPrivateStore, never()).getDescriptor(any(), any(), any());
  }

  @Test
  public void testSyncStagesANewPublicTreeAsConflictedWhenItOverlapsALocalChange() {
    Id parentId = new Id("parent");
    Id childId = new Id("child");
    ElementEntity parent = CollaborationTestFixtures.element(parentId, Id.ZERO, "parent", childId);
    ElementEntity child = CollaborationTestFixtures.element(childId, parentId, "child");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(parentId, PUBLIC_PUBLISH_TIME),
        publicSyncState(childId, PUBLIC_PUBLISH_TIME));
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(childId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    publicElement(parent);
    publicElement(child);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    List<StageEntity<ElementEntity>> stages = capturedElementStages();
    Assert.assertEquals(stages.size(), 2);
    Assert.assertSame(stages.get(0).getEntity(), child);
    Assert.assertFalse(stages.get(0).isConflicted());
    Assert.assertSame(stages.get(1).getEntity(), parent);
    Assert.assertTrue(stages.get(1).isConflicted());
    Assert.assertEquals(stages.get(1).getConflictDependents(),
        Collections.singleton(new ElementEntity(childId)));
  }

  @Test
  public void testSyncStagesAConflictedTreeWhenThePublicElementWasDeletedLocally() {
    Id parentId = new Id("parent");
    Id childId = new Id("child");
    ElementEntity parent = CollaborationTestFixtures.element(parentId, Id.ZERO, "parent", childId);
    ElementEntity child = CollaborationTestFixtures.element(childId, parentId, "child");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(parentId, PUBLIC_PUBLISH_TIME),
        publicSyncState(childId, PUBLIC_PUBLISH_TIME));
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(parentId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    publicElement(parent);
    publicElement(child);
    when(elementPrivateStore.get(CONTEXT, PUBLIC_ELEMENT_CONTEXT, parentId))
        .thenReturn(Optional.empty());

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    List<StageEntity<ElementEntity>> stages = capturedElementStages();
    Assert.assertEquals(stages.size(), 2);
    Assert.assertSame(stages.get(0).getEntity(), child);
    Assert.assertFalse(stages.get(0).isConflicted());
    Assert.assertSame(stages.get(1).getEntity(), parent);
    Assert.assertTrue(stages.get(1).isConflicted(),
        "An element deleted locally and changed on public is a conflict");
    Assert.assertEquals(stages.get(1).getConflictDependents(),
        Collections.singleton(new ElementEntity(childId)));
  }

  @Test
  public void testSyncFailsWhenAPublicSyncStateHasNoElement() {
    Id elementId = new Id("e1");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(elementId, PUBLIC_PUBLISH_TIME));
    when(elementPublicStore.get(CONTEXT, PUBLIC_ELEMENT_CONTEXT, elementId))
        .thenReturn(Optional.empty());

    try {
      syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("A public sync state without an element must fail");
    } catch (IllegalStateException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: Sync state of element with Id %s "
              + "exists in public space while the element does not",
          ITEM_ID, VERSION_ID, elementId));
    }
  }

  @Test
  public void testSyncFailsWhenTheParentOfANewPublicElementIsMissing() {
    Id parentId = new Id("parent");
    Id childId = new Id("child");
    ElementEntity child = CollaborationTestFixtures.element(childId, parentId, "child");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(childId, PUBLIC_PUBLISH_TIME));
    publicElement(child);
    when(elementPublicStore.get(CONTEXT, PUBLIC_ELEMENT_CONTEXT, parentId))
        .thenReturn(Optional.empty());

    try {
      syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("A public element whose parent is missing must fail");
    } catch (IllegalStateException expected) {
      Assert.assertEquals(expected.getMessage(), String
          .format("Element %s exists while its parent element %s does not", childId, parentId));
    }
    verify(elementStageStore, never()).create(any(), any(), any());
  }

  @Test
  public void testSyncFailsWhenASubElementToStageIsMissing() {
    Id parentId = new Id("parent");
    Id childId = new Id("child");
    ElementEntity parent = CollaborationTestFixtures.element(parentId, Id.ZERO, "parent", childId);
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(parentId, PUBLIC_PUBLISH_TIME));
    publicElement(parent);
    when(elementPublicStore.get(CONTEXT, PUBLIC_ELEMENT_CONTEXT, childId))
        .thenReturn(Optional.empty());

    try {
      syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("A sub element that cannot be read must fail");
    } catch (IllegalStateException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: Element with Id %s which should be staged with action %s "
              + "does not exist", ITEM_ID, VERSION_ID, childId, Action.CREATE));
    }
  }

  @Test
  public void testSyncStagesADeleteWhenTheElementIsGoneFromBothSpaces() {
    Id elementId = new Id("e1");
    givenPublicVersionAheadOfPrivate();
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(elementId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false));
    when(elementPrivateStore.get(CONTEXT, PUBLIC_ELEMENT_CONTEXT, elementId))
        .thenReturn(Optional.empty());

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedElementStage();
    Assert.assertEquals(stage.getEntity().getId(), elementId);
    Assert.assertNull(stage.getEntity().getParentId());
    Assert.assertEquals(stage.getAction(), Action.DELETE);
    Assert.assertNull(stage.getPublishTime());
    Assert.assertFalse(stage.isConflicted());
  }

  @Test
  public void testSyncIgnoresALocallyCreatedElementThatWasNeverPublished() {
    Id elementId = new Id("locally-created");
    givenPublicVersionAheadOfPrivate();
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(elementId, Id.ZERO, null, true));

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    verify(elementStageStore, never()).create(any(), any(), any());
    verify(elementPrivateStore, never()).get(any(), any(), eq(elementId));
  }

  @Test
  public void testSyncStagesADeleteTreeWhenThePublicElementWasDeleted() {
    Id parentId = new Id("parent");
    Id childId = new Id("child");
    ElementEntity parent = CollaborationTestFixtures.element(parentId, Id.ZERO, "parent", childId);
    ElementEntity child = CollaborationTestFixtures.element(childId, parentId, "child");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(Id.ZERO, LAST_SYNCED_PUBLISH_TIME));
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(Id.ZERO, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false),
        CollaborationTestFixtures.syncState(childId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false),
        CollaborationTestFixtures.syncState(parentId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false));
    privateElement(parent);
    privateElement(child);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    List<StageEntity<ElementEntity>> stages = capturedElementStages();
    Assert.assertEquals(stages.size(), 2, "The tree must be staged once, not once per member");
    Assert.assertSame(stages.get(0).getEntity(), child);
    Assert.assertEquals(stages.get(0).getAction(), Action.DELETE);
    Assert.assertNull(stages.get(0).getPublishTime());
    Assert.assertFalse(stages.get(0).isConflicted());
    Assert.assertSame(stages.get(1).getEntity(), parent);
    Assert.assertEquals(stages.get(1).getAction(), Action.DELETE);
    Assert.assertFalse(stages.get(1).isConflicted());
  }

  @Test
  public void testSyncStagesAConflictedDeleteTreeWhenTheLocalTreeWasChanged() {
    Id parentId = new Id("parent");
    Id childId = new Id("child");
    ElementEntity parent = CollaborationTestFixtures.element(parentId, Id.ZERO, "parent", childId);
    ElementEntity child = CollaborationTestFixtures.element(childId, parentId, "child");
    givenPublicVersionAheadOfPrivate();
    publicElementSyncStates(publicSyncState(Id.ZERO, LAST_SYNCED_PUBLISH_TIME));
    privateElementSyncStates(
        CollaborationTestFixtures.syncState(Id.ZERO, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false),
        CollaborationTestFixtures.syncState(parentId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false),
        CollaborationTestFixtures.syncState(childId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    privateElement(parent);
    privateElement(child);

    syncService.sync(CONTEXT, ITEM_ID, VERSION_ID);

    List<StageEntity<ElementEntity>> stages = capturedElementStages();
    Assert.assertEquals(stages.size(), 2);
    Assert.assertSame(stages.get(1).getEntity(), parent);
    Assert.assertTrue(stages.get(1).isConflicted(),
        "Deleting a tree that holds an unpublished local change is a conflict");
    Assert.assertEquals(stages.get(1).getConflictDependents(),
        Collections.singleton(new ElementEntity(childId)));
  }

  private void givenPublicVersionAheadOfPrivate() {
    publicVersionSyncState();
    privateVersionSyncState(LAST_SYNCED_PUBLISH_TIME);
  }

  private void givenVersionMissingFromPrivate(VersionEntity publicVersion) {
    publicVersionSyncState();
    when(versionPrivateStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID))
        .thenReturn(Optional.empty());
    when(versionPublicStore.get(CONTEXT, ITEM_ID, VERSION_ID))
        .thenReturn(Optional.of(publicVersion));
  }

  private void publicVersionSyncState() {
    when(versionPublicStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID, null)).thenReturn(
        Optional.of(new SynchronizationStateEntity(VERSION_ID, PUBLIC_REVISION_ID,
            PUBLIC_PUBLISH_TIME, false)));
  }

  private void privateVersionSyncState(Date publishTime) {
    when(versionPrivateStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID)).thenReturn(
        Optional.of(new SynchronizationStateEntity(VERSION_ID, Id.ZERO, publishTime, false)));
  }

  private SynchronizationStateEntity publicSyncState(Id elementId, Date publishTime) {
    return new SynchronizationStateEntity(elementId, PUBLIC_REVISION_ID, publishTime, false);
  }

  private void publicElementSyncStates(SynchronizationStateEntity... syncStates) {
    when(elementPublicStore.listSynchronizationStates(CONTEXT, PUBLIC_ELEMENT_CONTEXT))
        .thenReturn(CollaborationTestFixtures.syncStates(syncStates));
  }

  private void privateElementSyncStates(SynchronizationStateEntity... syncStates) {
    when(elementPrivateStore.listSynchronizationStates(CONTEXT, PUBLIC_ELEMENT_CONTEXT))
        .thenReturn(CollaborationTestFixtures.syncStates(syncStates));
  }

  private void publicElement(ElementEntity element) {
    when(elementPublicStore.get(CONTEXT, PUBLIC_ELEMENT_CONTEXT, element.getId()))
        .thenReturn(Optional.of(element));
  }

  private void privateElement(ElementEntity element) {
    when(elementPrivateStore.get(CONTEXT, PUBLIC_ELEMENT_CONTEXT, element.getId()))
        .thenReturn(Optional.of(element));
    when(elementPrivateStore.getDescriptor(CONTEXT, PUBLIC_ELEMENT_CONTEXT, element.getId()))
        .thenReturn(Optional.of(element));
  }

  @SuppressWarnings("unchecked")
  private StageEntity<VersionEntity> capturedVersionStage() {
    ArgumentCaptor<StageEntity<VersionEntity>> stageCaptor =
        ArgumentCaptor.forClass(StageEntity.class);
    verify(versionStageStore).create(eq(CONTEXT), eq(ITEM_ID), stageCaptor.capture());
    return stageCaptor.getValue();
  }

  private StageEntity<ElementEntity> capturedElementStage() {
    List<StageEntity<ElementEntity>> stages = capturedElementStages();
    Assert.assertEquals(stages.size(), 1);
    return stages.get(0);
  }

  @SuppressWarnings("unchecked")
  private List<StageEntity<ElementEntity>> capturedElementStages() {
    ArgumentCaptor<StageEntity<ElementEntity>> stageCaptor =
        ArgumentCaptor.forClass(StageEntity.class);
    verify(elementStageStore, atLeastOnce())
        .create(eq(CONTEXT), eq(PUBLIC_ELEMENT_CONTEXT), stageCaptor.capture());
    return stageCaptor.getAllValues();
  }
}
