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
import com.amdocs.zusammen.plugin.dao.types.VersionEntity;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CommitStagingServiceTest {

  private static final SessionContext CONTEXT =
      TestUtils.createSessionContext(new UserInfo("CommitStagingServiceTest_user"), "test");
  private static final Id ITEM_ID = new Id("item");
  private static final Id VERSION_ID = new Id("version");
  private static final Date PUBLISH_TIME = new Date(1_500_000_000_000L);

  @Mock
  private VersionPrivateStore versionPrivateStore;
  @Mock
  private VersionStageStore versionStageStore;
  @Mock
  private ElementPrivateStore elementPrivateStore;
  @Mock
  private ElementStageStore elementStageStore;

  private CommitStagingService commitStagingService;
  private AutoCloseable mocks;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    commitStagingService = new CommitStagingService(versionPrivateStore, versionStageStore,
        elementPrivateStore, elementStageStore);
    when(versionStageStore.get(any(), any(), any())).thenReturn(Optional.empty());
    when(elementStageStore.listIds(any(), any())).thenReturn(Collections.emptyList());
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testCommitStagingDoesNothingWhenNothingIsStaged() {
    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    verifyNoInteractions(versionPrivateStore);
    verifyNoInteractions(elementPrivateStore);
    verify(versionStageStore, never()).delete(any(), any(), any());
    verify(elementStageStore, never()).delete(any(), any(), any());
    verify(elementStageStore, never()).hasConflicts(any(), any());
  }

  @Test
  public void testCommitStagingDoesNothingWhenStageHasConflicts() {
    stageVersion(Action.UPDATE);
    stageElements(Action.CREATE);
    when(elementStageStore.hasConflicts(any(), any())).thenReturn(true);

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    verifyNoInteractions(versionPrivateStore);
    verifyNoInteractions(elementPrivateStore);
    verify(versionStageStore, never()).delete(any(), any(), any());
    verify(elementStageStore, never()).delete(any(), any(), any());
  }

  @Test
  public void testCommitStagingReadsElementStageWithPrivateRevisionContext() {
    stageElements(Action.CREATE);

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    ArgumentCaptor<ElementContext> contextCaptor = ArgumentCaptor.forClass(ElementContext.class);
    verify(elementStageStore).listIds(eq(CONTEXT), contextCaptor.capture());

    ElementContext elementContext = contextCaptor.getValue();
    Assert.assertEquals(elementContext.getItemId(), ITEM_ID);
    Assert.assertEquals(elementContext.getVersionId(), VERSION_ID);
    Assert.assertEquals(elementContext.getRevisionId(), Id.ZERO);
  }

  @Test
  public void testCommitStagingCommitsVersionCreate() {
    stageVersion(Action.CREATE);

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    ArgumentCaptor<VersionEntity> versionCaptor = ArgumentCaptor.forClass(VersionEntity.class);
    verify(versionPrivateStore)
        .commitStagedCreate(eq(CONTEXT), eq(ITEM_ID), versionCaptor.capture(), eq(PUBLISH_TIME));
    Assert.assertEquals(versionCaptor.getValue().getId(), VERSION_ID);

    verify(versionPrivateStore, never()).commitStagedUpdate(any(), any(), any(), any());
    verify(versionPrivateStore, never()).commitStagedIgnore(any(), any(), any(), any());
    verify(versionStageStore).delete(eq(CONTEXT), eq(ITEM_ID), eq(versionCaptor.getValue()));
  }

  @Test
  public void testCommitStagingCommitsVersionUpdate() {
    stageVersion(Action.UPDATE);

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    ArgumentCaptor<VersionEntity> versionCaptor = ArgumentCaptor.forClass(VersionEntity.class);
    verify(versionPrivateStore)
        .commitStagedUpdate(eq(CONTEXT), eq(ITEM_ID), versionCaptor.capture(), eq(PUBLISH_TIME));
    Assert.assertEquals(versionCaptor.getValue().getId(), VERSION_ID);
    verify(versionStageStore).delete(eq(CONTEXT), eq(ITEM_ID), eq(versionCaptor.getValue()));
  }

  @Test
  public void testCommitStagingCommitsVersionIgnore() {
    stageVersion(Action.IGNORE);

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    verify(versionPrivateStore)
        .commitStagedIgnore(eq(CONTEXT), eq(ITEM_ID), any(VersionEntity.class), eq(PUBLISH_TIME));
    verify(versionStageStore).delete(any(), any(), any());
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testCommitStagingRejectsVersionStageDelete() {
    stageVersion(Action.DELETE);

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);
  }

  @Test
  public void testCommitStagingLeavesVersionStageInPlaceWhenActionIsRejected() {
    stageVersion(Action.DELETE);

    try {
      commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("A DELETE version stage must not be committed");
    } catch (UnsupportedOperationException expected) {
      verifyNoInteractions(versionPrivateStore);
      verify(versionStageStore, never()).delete(any(), any(), any());
    }
  }

  @Test
  public void testCommitStagingCommitsElementCreate() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "e1");
    stageElements(new StageEntity<>(element, PUBLISH_TIME, Action.CREATE, false));

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    ArgumentCaptor<ElementContext> contextCaptor = ArgumentCaptor.forClass(ElementContext.class);
    verify(elementPrivateStore).commitStagedCreate(eq(CONTEXT), contextCaptor.capture(),
        eq(element), eq(PUBLISH_TIME));
    Assert.assertEquals(contextCaptor.getValue().getRevisionId(), Id.ZERO);
    verify(elementStageStore).delete(eq(CONTEXT), any(ElementContext.class), eq(element));
  }

  @Test
  public void testCommitStagingCommitsElementUpdate() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "e1");
    stageElements(new StageEntity<>(element, PUBLISH_TIME, Action.UPDATE, false));

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    verify(elementPrivateStore).commitStagedUpdate(eq(CONTEXT), any(ElementContext.class),
        eq(element), eq(PUBLISH_TIME));
    verify(elementStageStore).delete(eq(CONTEXT), any(ElementContext.class), eq(element));
  }

  @Test
  public void testCommitStagingCommitsElementDelete() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "e1");
    stageElements(new StageEntity<>(element, null, Action.DELETE, false));

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    verify(elementPrivateStore)
        .commitStagedDelete(eq(CONTEXT), any(ElementContext.class), eq(element));
    verify(elementStageStore).delete(eq(CONTEXT), any(ElementContext.class), eq(element));
  }

  @Test
  public void testCommitStagingCommitsElementIgnore() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "e1");
    stageElements(new StageEntity<>(element, PUBLISH_TIME, Action.IGNORE, false));

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    verify(elementPrivateStore).commitStagedIgnore(eq(CONTEXT), any(ElementContext.class),
        eq(element), eq(PUBLISH_TIME));
    verify(elementStageStore).delete(eq(CONTEXT), any(ElementContext.class), eq(element));
  }

  @Test
  public void testCommitStagingCommitsEveryStagedElementWithItsOwnAction() {
    ElementEntity created = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "e1");
    ElementEntity deleted = CollaborationTestFixtures.element(new Id("e2"), Id.ZERO, "e2");
    stageElements(new StageEntity<>(created, PUBLISH_TIME, Action.CREATE, false),
        new StageEntity<>(deleted, null, Action.DELETE, false));

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    verify(elementPrivateStore).commitStagedCreate(eq(CONTEXT), any(ElementContext.class),
        eq(created), eq(PUBLISH_TIME));
    verify(elementPrivateStore)
        .commitStagedDelete(eq(CONTEXT), any(ElementContext.class), eq(deleted));
    verify(elementStageStore).delete(eq(CONTEXT), any(ElementContext.class), eq(created));
    verify(elementStageStore).delete(eq(CONTEXT), any(ElementContext.class), eq(deleted));
  }

  @Test
  public void testCommitStagingCommitsVersionBeforeElements() {
    stageVersion(Action.UPDATE);
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "e1");
    stageElements(new StageEntity<>(element, PUBLISH_TIME, Action.UPDATE, false));

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    InOrder inOrder = inOrder(versionPrivateStore, elementPrivateStore);
    inOrder.verify(versionPrivateStore)
        .commitStagedUpdate(any(), any(), any(VersionEntity.class), any());
    inOrder.verify(elementPrivateStore)
        .commitStagedUpdate(any(), any(), any(ElementEntity.class), any());
  }

  @Test
  public void testCommitStagingCommitsElementsWhenOnlyElementsAreStaged() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "e1");
    stageElements(new StageEntity<>(element, PUBLISH_TIME, Action.CREATE, false));

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    verifyNoInteractions(versionPrivateStore);
    verify(versionStageStore, never()).delete(any(), any(), any());
    verify(elementPrivateStore).commitStagedCreate(any(), any(), eq(element), eq(PUBLISH_TIME));
  }

  @Test
  public void testCommitStagingCommitsVersionWhenOnlyVersionIsStaged() {
    stageVersion(Action.UPDATE);

    commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);

    verify(versionPrivateStore)
        .commitStagedUpdate(any(), any(), any(VersionEntity.class), eq(PUBLISH_TIME));
    verifyNoInteractions(elementPrivateStore);
  }

  @Test
  public void testCommitStagingFailsWhenListedStagedElementCannotBeRead() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "e1");
    when(elementStageStore.listIds(any(), any()))
        .thenReturn(Collections.singletonList(element));
    when(elementStageStore.get(any(), any(), eq(element))).thenReturn(Optional.empty());

    try {
      commitStagingService.commitStaging(CONTEXT, ITEM_ID, VERSION_ID);
      Assert.fail("A staged element id returned by listIds but missing from the stage must fail");
    } catch (IllegalStateException expected) {
      Assert.assertEquals(expected.getMessage(), "Staged element id returned by list must exist");
    }
    verifyNoInteractions(elementPrivateStore);
  }

  private void stageVersion(Action action) {
    when(versionStageStore.get(eq(CONTEXT), eq(ITEM_ID), any(VersionEntity.class))).thenReturn(
        Optional.of(new StageEntity<>(new VersionEntity(VERSION_ID), PUBLISH_TIME, action, false)));
  }

  private void stageElements(Action action) {
    stageElements(new StageEntity<>(
        CollaborationTestFixtures.element(new Id("staged"), Id.ZERO, "staged"), PUBLISH_TIME,
        action, false));
  }

  @SafeVarargs
  private final void stageElements(StageEntity<ElementEntity>... stagedElements) {
    ElementEntity[] ids = new ElementEntity[stagedElements.length];
    for (int i = 0; i < stagedElements.length; i++) {
      ids[i] = stagedElements[i].getEntity();
      when(elementStageStore.get(any(), any(), eq(stagedElements[i].getEntity())))
          .thenReturn(Optional.of(stagedElements[i]));
    }
    when(elementStageStore.listIds(any(), any())).thenReturn(Arrays.asList(ids));
  }
}
