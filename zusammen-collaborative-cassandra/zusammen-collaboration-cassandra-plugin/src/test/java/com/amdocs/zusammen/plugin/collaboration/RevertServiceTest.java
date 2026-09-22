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
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RevertServiceTest {

  private static final SessionContext CONTEXT =
      TestUtils.createSessionContext(new UserInfo("RevertServiceTest_user"), "test");
  private static final Id ITEM_ID = new Id("item");
  private static final Id VERSION_ID = new Id("version");
  private static final Id TARGET_REVISION_ID = new Id("revision-current");
  private static final Id SOURCE_REVISION_ID = new Id("revision-to-revert-to");

  private static final ElementContext SOURCE_CONTEXT =
      new ElementContext(ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);
  private static final ElementContext TARGET_CONTEXT = new ElementContext(ITEM_ID, VERSION_ID);

  @Mock
  private ElementPublicStore elementPublicStore;
  @Mock
  private ElementPrivateStore elementPrivateStore;

  private RevertService revertService;
  private AutoCloseable mocks;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    revertService = new RevertService(elementPublicStore, elementPrivateStore);
    when(elementPublicStore.listIds(any(), any())).thenReturn(Collections.emptyMap());
    when(elementPrivateStore.listSynchronizationStates(any(), any()))
        .thenReturn(Collections.emptyList());
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testRevertDoesNothingWhenBothRevisionsAreEmpty() {
    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    verify(elementPrivateStore, never()).create(any(), any(), any());
    verify(elementPrivateStore, never()).update(any(), any(), any());
    verify(elementPrivateStore, never()).delete(any(), any(), any());
    verify(elementPublicStore, never()).get(any(), any(), any());
  }

  @Test
  public void testRevertReadsSourceElementsAtTheRequestedRevisionAndTargetAtTheCurrentOne() {
    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    verify(elementPublicStore).listIds(CONTEXT, SOURCE_CONTEXT);
    verify(elementPublicStore).listIds(CONTEXT, TARGET_CONTEXT);
    verify(elementPrivateStore).listSynchronizationStates(CONTEXT, TARGET_CONTEXT);
  }

  @Test
  public void testRevertCreatesElementThatIsMissingFromTheCurrentState() {
    Id elementId = new Id("e1");
    ElementEntity sourceElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "old");
    sourceElements(elementId, new Id("er1"));
    publicElement(SOURCE_CONTEXT, elementId, sourceElement);

    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    ArgumentCaptor<ElementContext> contextCaptor = ArgumentCaptor.forClass(ElementContext.class);
    verify(elementPrivateStore).create(eq(CONTEXT), contextCaptor.capture(), eq(sourceElement));
    Assert.assertEquals(contextCaptor.getValue(), SOURCE_CONTEXT);
    verify(elementPrivateStore, never()).update(any(), any(), any());
    verify(elementPrivateStore, never()).delete(any(), any(), any());
  }

  @Test
  public void testRevertUpdatesElementWhoseRevisionDiffersFromTheSourceRevision() {
    Id elementId = new Id("e1");
    ElementEntity sourceElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "old");
    sourceElements(elementId, new Id("er1"));
    targetPublicElements(elementId, new Id("er2"));
    publicElement(SOURCE_CONTEXT, elementId, sourceElement);

    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    ArgumentCaptor<ElementContext> contextCaptor = ArgumentCaptor.forClass(ElementContext.class);
    verify(elementPrivateStore).update(eq(CONTEXT), contextCaptor.capture(), eq(sourceElement));
    Assert.assertEquals(contextCaptor.getValue(), SOURCE_CONTEXT);
    verify(elementPrivateStore, never()).create(any(), any(), any());
    verify(elementPrivateStore, never()).delete(any(), any(), any());
  }

  @Test
  public void testRevertLeavesElementUntouchedWhenItsRevisionAlreadyMatches() {
    Id elementId = new Id("e1");
    Id elementRevisionId = new Id("er1");
    sourceElements(elementId, elementRevisionId);
    targetPublicElements(elementId, elementRevisionId);

    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    verify(elementPublicStore, never()).get(any(), any(), any());
    verify(elementPrivateStore, never()).create(any(), any(), any());
    verify(elementPrivateStore, never()).update(any(), any(), any());
    verify(elementPrivateStore, never()).delete(any(), any(), any());
  }

  @Test
  public void testRevertOverwritesLocallyChangedElementEvenWhenItsPublicRevisionMatches() {
    Id elementId = new Id("e1");
    Id elementRevisionId = new Id("er1");
    ElementEntity sourceElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "old");
    sourceElements(elementId, elementRevisionId);
    targetPublicElements(elementId, elementRevisionId);
    privateSyncStates(new SynchronizationStateEntity(elementId, Id.ZERO, new Date(1L), true));
    publicElement(SOURCE_CONTEXT, elementId, sourceElement);

    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    verify(elementPrivateStore).update(CONTEXT, SOURCE_CONTEXT, sourceElement);
  }

  @Test
  public void testRevertKeepsCleanElementWhoseRevisionMatchesWhileOverwritingTheDirtyOne() {
    Id clean = new Id("clean");
    Id dirty = new Id("dirty");
    Id elementRevisionId = new Id("er1");
    ElementEntity dirtySourceElement = CollaborationTestFixtures.element(dirty, Id.ZERO, "old");

    Map<Id, Id> ids = new LinkedHashMap<>();
    ids.put(clean, elementRevisionId);
    ids.put(dirty, elementRevisionId);
    when(elementPublicStore.listIds(CONTEXT, SOURCE_CONTEXT)).thenReturn(ids);
    when(elementPublicStore.listIds(CONTEXT, TARGET_CONTEXT))
        .thenReturn(new LinkedHashMap<>(ids));
    privateSyncStates(new SynchronizationStateEntity(dirty, Id.ZERO, new Date(1L), true),
        new SynchronizationStateEntity(clean, Id.ZERO, new Date(1L), false));
    publicElement(SOURCE_CONTEXT, dirty, dirtySourceElement);

    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    verify(elementPrivateStore).update(CONTEXT, SOURCE_CONTEXT, dirtySourceElement);
    verify(elementPublicStore, never()).get(CONTEXT, SOURCE_CONTEXT, clean);
  }

  @Test
  public void testRevertDeletesElementThatDoesNotExistInTheSourceRevision() {
    Id elementId = new Id("e1");
    ElementEntity privateElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "new");
    targetPublicElements(elementId, new Id("er2"));
    when(elementPrivateStore.get(CONTEXT, TARGET_CONTEXT, elementId))
        .thenReturn(Optional.of(privateElement));

    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    ArgumentCaptor<ElementContext> contextCaptor = ArgumentCaptor.forClass(ElementContext.class);
    verify(elementPrivateStore).delete(eq(CONTEXT), contextCaptor.capture(), eq(privateElement));
    Assert.assertEquals(contextCaptor.getValue(), TARGET_CONTEXT);
    verify(elementPrivateStore, never()).create(any(), any(), any());
    verify(elementPrivateStore, never()).update(any(), any(), any());
  }

  @Test
  public void testRevertDeletesElementThatWasCreatedOnlyOnPrivate() {
    Id elementId = new Id("locally-created");
    ElementEntity privateElement = CollaborationTestFixtures.element(elementId, Id.ZERO, "new");
    privateSyncStates(new SynchronizationStateEntity(elementId, Id.ZERO, null, true));
    when(elementPrivateStore.get(CONTEXT, TARGET_CONTEXT, elementId))
        .thenReturn(Optional.of(privateElement));

    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    verify(elementPrivateStore).delete(CONTEXT, TARGET_CONTEXT, privateElement);
  }

  @Test
  public void testRevertSkipsDeletionWhenElementIsAlreadyAbsentFromPrivate() {
    Id elementId = new Id("e1");
    targetPublicElements(elementId, new Id("er2"));
    when(elementPrivateStore.get(CONTEXT, TARGET_CONTEXT, elementId)).thenReturn(Optional.empty());

    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    verify(elementPrivateStore, never()).delete(any(), any(), any());
  }

  @Test
  public void testRevertFailsWhenSourceElementToCreateIsMissingFromPublic() {
    Id elementId = new Id("e1");
    sourceElements(elementId, new Id("er1"));
    when(elementPublicStore.get(CONTEXT, SOURCE_CONTEXT, elementId)).thenReturn(Optional.empty());

    try {
      revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);
      Assert.fail("An element listed in the source revision but missing from public must fail");
    } catch (IllegalStateException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s, revision Id %s: Missing element with Id %s",
          ITEM_ID, VERSION_ID, SOURCE_REVISION_ID, elementId));
    }
    verify(elementPrivateStore, never()).create(any(), any(), any());
  }

  @Test
  public void testRevertFailsWhenSourceElementToUpdateIsMissingFromPublic() {
    Id elementId = new Id("e1");
    sourceElements(elementId, new Id("er1"));
    targetPublicElements(elementId, new Id("er2"));
    when(elementPublicStore.get(CONTEXT, SOURCE_CONTEXT, elementId)).thenReturn(Optional.empty());

    try {
      revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);
      Assert.fail("An element listed in the source revision but missing from public must fail");
    } catch (IllegalStateException expected) {
      Assert.assertTrue(expected.getMessage().contains(elementId.getValue()),
          "The failure must name the element: " + expected.getMessage());
    }
    verify(elementPrivateStore, never()).update(any(), any(), any());
  }

  @Test
  public void testRevertAppliesCreateUpdateAndDeleteInOnePass() {
    Id toCreate = new Id("to-create");
    Id toUpdate = new Id("to-update");
    Id toDelete = new Id("to-delete");
    ElementEntity createdElement = CollaborationTestFixtures.element(toCreate, Id.ZERO, "created");
    ElementEntity updatedElement = CollaborationTestFixtures.element(toUpdate, Id.ZERO, "updated");
    ElementEntity deletedElement = CollaborationTestFixtures.element(toDelete, Id.ZERO, "deleted");

    Map<Id, Id> source = new LinkedHashMap<>();
    source.put(toCreate, new Id("er1"));
    source.put(toUpdate, new Id("er1"));
    Map<Id, Id> target = new LinkedHashMap<>();
    target.put(toUpdate, new Id("er2"));
    target.put(toDelete, new Id("er2"));
    when(elementPublicStore.listIds(CONTEXT, SOURCE_CONTEXT)).thenReturn(source);
    when(elementPublicStore.listIds(CONTEXT, TARGET_CONTEXT)).thenReturn(target);
    publicElement(SOURCE_CONTEXT, toCreate, createdElement);
    publicElement(SOURCE_CONTEXT, toUpdate, updatedElement);
    when(elementPrivateStore.get(CONTEXT, TARGET_CONTEXT, toDelete))
        .thenReturn(Optional.of(deletedElement));

    revertService.revert(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_REVISION_ID);

    verify(elementPrivateStore).create(CONTEXT, SOURCE_CONTEXT, createdElement);
    verify(elementPrivateStore).update(CONTEXT, SOURCE_CONTEXT, updatedElement);
    verify(elementPrivateStore).delete(CONTEXT, TARGET_CONTEXT, deletedElement);
  }

  private void sourceElements(Id elementId, Id elementRevisionId) {
    when(elementPublicStore.listIds(CONTEXT, SOURCE_CONTEXT))
        .thenReturn(Collections.singletonMap(elementId, elementRevisionId));
  }

  private void targetPublicElements(Id elementId, Id elementRevisionId) {
    when(elementPublicStore.listIds(CONTEXT, TARGET_CONTEXT))
        .thenReturn(Collections.singletonMap(elementId, elementRevisionId));
  }

  private void privateSyncStates(SynchronizationStateEntity... syncStates) {
    when(elementPrivateStore.listSynchronizationStates(CONTEXT, TARGET_CONTEXT))
        .thenReturn(CollaborationTestFixtures.syncStates(syncStates));
  }

  private void publicElement(ElementContext elementContext, Id elementId, ElementEntity element) {
    when(elementPublicStore.get(CONTEXT, elementContext, elementId))
        .thenReturn(Optional.of(element));
  }
}
