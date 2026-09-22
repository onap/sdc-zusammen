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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DiscardChangesServiceTest {

  private static final SessionContext CONTEXT =
      TestUtils.createSessionContext(new UserInfo("DiscardChangesServiceTest_user"), "test");
  private static final Id ITEM_ID = new Id("item");
  private static final Id VERSION_ID = new Id("version");
  private static final Id LAST_SYNCED_REVISION_ID = new Id("revision-1");
  private static final Id NEWER_REVISION_ID = new Id("revision-2");
  private static final Date LAST_SYNCED_PUBLISH_TIME = new Date(1_000_000_000_000L);
  private static final Date NEWER_PUBLISH_TIME = new Date(2_000_000_000_000L);

  /** Element reads and writes must happen against the revision private last synced with. */
  private static final ElementContext LAST_SYNCED_CONTEXT =
      new ElementContext(ITEM_ID, VERSION_ID, LAST_SYNCED_REVISION_ID);

  @Mock
  private VersionPublicStore versionPublicStore;
  @Mock
  private VersionPrivateStore versionPrivateStore;
  @Mock
  private ElementPublicStore elementPublicStore;
  @Mock
  private ElementPrivateStore elementPrivateStore;
  @Mock
  private ElementStageStore elementStageStore;

  private DiscardChangesService discardChangesService;
  private AutoCloseable mocks;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    discardChangesService = new DiscardChangesService(versionPublicStore, versionPrivateStore,
        elementPublicStore, elementPrivateStore, elementStageStore);
    when(elementPublicStore.listSynchronizationStates(any(), any()))
        .thenReturn(Collections.emptyList());
    when(elementPrivateStore.listSynchronizationStates(any(), any()))
        .thenReturn(Collections.emptyList());
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testDiscardChangesDoesNothingWhenVersionDoesNotExistOnPrivate() {
    when(versionPrivateStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID))
        .thenReturn(Optional.empty());

    discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);

    verifyNoInteractions(versionPublicStore);
    verifyNoInteractions(elementPublicStore);
    verifyNoInteractions(elementPrivateStore);
    verifyNoInteractions(elementStageStore);
  }

  @Test
  public void testDiscardChangesRejectsVersionThatWasNeverPublished() {
    privateVersionSyncState(null);

    try {
      discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("Changes of a never published version cannot be discarded");
    } catch (UnsupportedOperationException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: Changes of unpublished version cannot be discarded",
          ITEM_ID, VERSION_ID));
    }
    verifyNoInteractions(elementStageStore);
  }

  @Test
  public void testDiscardChangesFailsWhenPrivateRevisionIsUnknownToPublic() {
    privateVersionSyncState(LAST_SYNCED_PUBLISH_TIME);
    when(versionPublicStore.listSynchronizationStates(CONTEXT, ITEM_ID, VERSION_ID))
        .thenReturn(Collections.singletonList(
            new SynchronizationStateEntity(VERSION_ID, NEWER_REVISION_ID, NEWER_PUBLISH_TIME,
                false)));

    try {
      discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("A private publish time with no matching public revision must fail");
    } catch (IllegalStateException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: private version revision (publish time %s) "
              + "was not found on public", ITEM_ID, VERSION_ID, LAST_SYNCED_PUBLISH_TIME));
    }
    verifyNoInteractions(elementStageStore);
  }

  @Test
  public void testDiscardChangesResolvesTheRevisionPrivateLastSyncedWith() {
    givenPrivateLastSyncedWithFirstRevision();

    discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);

    verify(elementPublicStore).listSynchronizationStates(CONTEXT, LAST_SYNCED_CONTEXT);
    verify(elementPrivateStore).listSynchronizationStates(CONTEXT, LAST_SYNCED_CONTEXT);
  }

  @Test
  public void testDiscardChangesStagesNothingWhenNoPrivateElementIsDirty() {
    givenPrivateLastSyncedWithFirstRevision();
    privateElementSyncStates(
        new SynchronizationStateEntity(new Id("e1"), Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false));

    discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);

    verify(elementStageStore, never()).create(any(), any(), any());
  }

  @Test
  public void testDiscardChangesStagesDeleteForLocallyCreatedElement() {
    Id elementId = new Id("locally-created");
    ElementEntity privateElement =
        CollaborationTestFixtures.element(elementId, Id.ZERO, "local content");
    givenPrivateLastSyncedWithFirstRevision();
    privateElementSyncStates(new SynchronizationStateEntity(elementId, Id.ZERO, null, true));
    privateElement(elementId, privateElement);

    discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedStage();
    Assert.assertSame(stage.getEntity(), privateElement);
    Assert.assertEquals(stage.getAction(), Action.DELETE);
    Assert.assertNull(stage.getPublishTime());
    Assert.assertFalse(stage.isConflicted());
  }

  @Test
  public void testDiscardChangesFailsWhenLocallyCreatedDirtyElementIsMissing() {
    Id elementId = new Id("locally-created");
    givenPrivateLastSyncedWithFirstRevision();
    privateElementSyncStates(new SynchronizationStateEntity(elementId, Id.ZERO, null, true));
    when(elementPrivateStore.get(CONTEXT, LAST_SYNCED_CONTEXT, elementId))
        .thenReturn(Optional.empty());

    try {
      discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("An unpublished dirty sync state without an element must fail");
    } catch (IllegalStateException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: Sync state of unpublished element with Id %s "
              + "exists in private space while the element does not",
          ITEM_ID, VERSION_ID, elementId));
    }
  }

  @Test
  public void testDiscardChangesStagesPublicContentAsUpdateForLocallyModifiedElement() {
    Id elementId = new Id("e1");
    ElementEntity publicElement =
        CollaborationTestFixtures.element(elementId, Id.ZERO, "public content");
    givenPrivateLastSyncedWithFirstRevision();
    privateElementSyncStates(
        new SynchronizationStateEntity(elementId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    privateElement(elementId, CollaborationTestFixtures.element(elementId, Id.ZERO, "local"));
    publicElementSyncStates(
        new SynchronizationStateEntity(elementId, LAST_SYNCED_REVISION_ID,
            LAST_SYNCED_PUBLISH_TIME, false));
    publicElement(elementId, publicElement);

    discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedStage();
    Assert.assertSame(stage.getEntity(), publicElement);
    Assert.assertEquals(stage.getAction(), Action.UPDATE);
    Assert.assertEquals(stage.getPublishTime(), LAST_SYNCED_PUBLISH_TIME);
    Assert.assertFalse(stage.isConflicted());
  }

  @Test
  public void testDiscardChangesStagesPublicContentAsCreateForLocallyDeletedElement() {
    Id elementId = new Id("e1");
    ElementEntity publicElement =
        CollaborationTestFixtures.element(elementId, Id.ZERO, "public content");
    givenPrivateLastSyncedWithFirstRevision();
    privateElementSyncStates(
        new SynchronizationStateEntity(elementId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    when(elementPrivateStore.get(CONTEXT, LAST_SYNCED_CONTEXT, elementId))
        .thenReturn(Optional.empty());
    publicElementSyncStates(
        new SynchronizationStateEntity(elementId, LAST_SYNCED_REVISION_ID,
            LAST_SYNCED_PUBLISH_TIME, false));
    publicElement(elementId, publicElement);

    discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedStage();
    Assert.assertSame(stage.getEntity(), publicElement);
    Assert.assertEquals(stage.getAction(), Action.CREATE);
    Assert.assertEquals(stage.getPublishTime(), LAST_SYNCED_PUBLISH_TIME);
  }

  @Test
  public void testDiscardChangesFailsWhenPublicSyncStateHasNoElement() {
    Id elementId = new Id("e1");
    givenPrivateLastSyncedWithFirstRevision();
    privateElementSyncStates(
        new SynchronizationStateEntity(elementId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    privateElement(elementId, CollaborationTestFixtures.element(elementId, Id.ZERO, "local"));
    publicElementSyncStates(
        new SynchronizationStateEntity(elementId, LAST_SYNCED_REVISION_ID,
            LAST_SYNCED_PUBLISH_TIME, false));
    when(elementPublicStore.get(CONTEXT, LAST_SYNCED_CONTEXT, elementId))
        .thenReturn(Optional.empty());

    try {
      discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("A public sync state without an element must fail");
    } catch (IllegalStateException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: Sync state of element with Id %s "
              + "exists in public space while the element does not",
          ITEM_ID, VERSION_ID, elementId));
    }
  }

  @Test
  public void testDiscardChangesStagesDeleteForElementNoLongerOnPublic() {
    Id elementId = new Id("e1");
    ElementEntity privateElement =
        CollaborationTestFixtures.element(elementId, Id.ZERO, "local content");
    givenPrivateLastSyncedWithFirstRevision();
    privateElementSyncStates(
        new SynchronizationStateEntity(elementId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    privateElement(elementId, privateElement);

    discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedStage();
    Assert.assertSame(stage.getEntity(), privateElement);
    Assert.assertEquals(stage.getAction(), Action.DELETE);
    Assert.assertNull(stage.getPublishTime());
    verify(elementPublicStore, never()).get(any(), any(), any());
  }

  @Test
  public void testDiscardChangesStagesDeleteOfBareEntityWhenElementIsGoneFromBothSpaces() {
    Id elementId = new Id("e1");
    givenPrivateLastSyncedWithFirstRevision();
    privateElementSyncStates(
        new SynchronizationStateEntity(elementId, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true));
    when(elementPrivateStore.get(CONTEXT, LAST_SYNCED_CONTEXT, elementId))
        .thenReturn(Optional.empty());

    discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);

    StageEntity<ElementEntity> stage = capturedStage();
    Assert.assertEquals(stage.getEntity().getId(), elementId);
    Assert.assertNull(stage.getEntity().getParentId());
    Assert.assertEquals(stage.getAction(), Action.DELETE);
    Assert.assertNull(stage.getPublishTime());
  }

  @Test
  public void testDiscardChangesStagesEveryDirtyElementAndSkipsTheCleanOnes() {
    Id restored = new Id("restored");
    Id dropped = new Id("dropped");
    Id untouched = new Id("untouched");
    ElementEntity restoredPublicElement =
        CollaborationTestFixtures.element(restored, Id.ZERO, "public content");
    ElementEntity droppedPrivateElement =
        CollaborationTestFixtures.element(dropped, Id.ZERO, "local content");

    givenPrivateLastSyncedWithFirstRevision();
    privateElementSyncStates(
        new SynchronizationStateEntity(restored, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, true),
        new SynchronizationStateEntity(dropped, Id.ZERO, null, true),
        new SynchronizationStateEntity(untouched, Id.ZERO, LAST_SYNCED_PUBLISH_TIME, false));
    privateElement(restored, CollaborationTestFixtures.element(restored, Id.ZERO, "local"));
    privateElement(dropped, droppedPrivateElement);
    publicElementSyncStates(new SynchronizationStateEntity(restored, LAST_SYNCED_REVISION_ID,
        LAST_SYNCED_PUBLISH_TIME, false));
    publicElement(restored, restoredPublicElement);

    discardChangesService.discardChanges(CONTEXT, ITEM_ID, VERSION_ID);

    List<StageEntity<ElementEntity>> stages = capturedStages();
    Assert.assertEquals(stages.size(), 2);
    for (StageEntity<ElementEntity> stage : stages) {
      if (restored.equals(stage.getEntity().getId())) {
        Assert.assertSame(stage.getEntity(), restoredPublicElement);
        Assert.assertEquals(stage.getAction(), Action.UPDATE);
      } else {
        Assert.assertSame(stage.getEntity(), droppedPrivateElement);
        Assert.assertEquals(stage.getAction(), Action.DELETE);
      }
    }
  }

  private void givenPrivateLastSyncedWithFirstRevision() {
    privateVersionSyncState(LAST_SYNCED_PUBLISH_TIME);
    when(versionPublicStore.listSynchronizationStates(CONTEXT, ITEM_ID, VERSION_ID)).thenReturn(
        Arrays.asList(
            new SynchronizationStateEntity(VERSION_ID, NEWER_REVISION_ID, NEWER_PUBLISH_TIME,
                false),
            new SynchronizationStateEntity(VERSION_ID, LAST_SYNCED_REVISION_ID,
                LAST_SYNCED_PUBLISH_TIME, false)));
  }

  private void privateVersionSyncState(Date publishTime) {
    when(versionPrivateStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID)).thenReturn(
        Optional.of(new SynchronizationStateEntity(VERSION_ID, Id.ZERO, publishTime, true)));
  }

  private void privateElementSyncStates(SynchronizationStateEntity... syncStates) {
    when(elementPrivateStore.listSynchronizationStates(CONTEXT, LAST_SYNCED_CONTEXT))
        .thenReturn(CollaborationTestFixtures.syncStates(syncStates));
  }

  private void publicElementSyncStates(SynchronizationStateEntity... syncStates) {
    when(elementPublicStore.listSynchronizationStates(CONTEXT, LAST_SYNCED_CONTEXT))
        .thenReturn(CollaborationTestFixtures.syncStates(syncStates));
  }

  private void privateElement(Id elementId, ElementEntity element) {
    when(elementPrivateStore.get(CONTEXT, LAST_SYNCED_CONTEXT, elementId))
        .thenReturn(Optional.of(element));
  }

  private void publicElement(Id elementId, ElementEntity element) {
    when(elementPublicStore.get(CONTEXT, LAST_SYNCED_CONTEXT, elementId))
        .thenReturn(Optional.of(element));
  }

  private StageEntity<ElementEntity> capturedStage() {
    List<StageEntity<ElementEntity>> stages = capturedStages();
    Assert.assertEquals(stages.size(), 1);
    return stages.get(0);
  }

  @SuppressWarnings("unchecked")
  private List<StageEntity<ElementEntity>> capturedStages() {
    ArgumentCaptor<StageEntity<ElementEntity>> stageCaptor =
        ArgumentCaptor.forClass(StageEntity.class);
    verify(elementStageStore, atLeastOnce())
        .create(eq(CONTEXT), eq(LAST_SYNCED_CONTEXT), stageCaptor.capture());
    return stageCaptor.getAllValues();
  }
}
