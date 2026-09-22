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
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.dao.types.VersionEntity;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationPublishResult;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PublishServiceTest {

  private static final SessionContext CONTEXT =
      TestUtils.createSessionContext(new UserInfo("PublishServiceTest_user"), "test");
  private static final Id ITEM_ID = new Id("item");
  private static final Id VERSION_ID = new Id("version");
  private static final String MESSAGE = "publish message";
  private static final Date LAST_PUBLISH_TIME = new Date(1_000_000_000_000L);
  private static final ElementContext PRIVATE_ELEMENT_CONTEXT =
      new ElementContext(ITEM_ID, VERSION_ID, Id.ZERO);

  @Mock
  private VersionPublicStore versionPublicStore;
  @Mock
  private VersionPrivateStore versionPrivateStore;
  @Mock
  private ElementPublicStore elementPublicStore;
  @Mock
  private ElementPrivateStore elementPrivateStore;

  private PublishService publishService;
  private AutoCloseable mocks;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    publishService = new PublishService(versionPublicStore, versionPrivateStore, elementPublicStore,
        elementPrivateStore);
    when(elementPublicStore.listIds(any(), any())).thenReturn(Collections.emptyMap());
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
  public void testPublishFailsWhenVersionDoesNotExistOnPrivate() {
    when(versionPrivateStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID))
        .thenReturn(Optional.empty());

    try {
      publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);
      Assert.fail("Publishing a version that is not on private must fail");
    } catch (IllegalArgumentException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: Non existing version cannot be pushed.",
          ITEM_ID, VERSION_ID));
    }
    verify(versionPublicStore, never()).create(any(), any(), any(), any(), any(), any(), any());
    verify(versionPublicStore, never()).update(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testPublishFailsWhenThereAreNoChangesToPublish() {
    privateVersionSyncState(LAST_PUBLISH_TIME, false);

    try {
      publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);
      Assert.fail("Publishing a version with no local changes must fail");
    } catch (ZusammenException expected) {
      Assert.assertEquals(expected.getReturnCode().getMessage(), String
          .format("Item Id %s, version Id %s: There are no changes to publish.", ITEM_ID,
              VERSION_ID));
      Assert.assertTrue(expected.getReturnCode().toString().startsWith("ZCSP-60000"),
          "The return code must carry ErrorCode.NO_CHANGES_TO_PUBLISH for module ZCSP, was: "
              + expected.getReturnCode());
    }
    verify(versionPrivateStore, never()).markAsPublished(any(), any(), any(), any());
  }

  @Test
  public void testPublishFailsWhenPrivateIsOutOfSyncWithPublic() {
    privateVersionSyncState(LAST_PUBLISH_TIME, true);
    publicVersionSyncState(new Date(2_000_000_000_000L));

    try {
      publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);
      Assert.fail("Publishing an out of sync version must fail");
    } catch (UnsupportedOperationException expected) {
      Assert.assertEquals(expected.getMessage(), "Out of sync item version can not be publish");
    }
    verify(versionPublicStore, never()).update(any(), any(), any(), any(), any(), any(), any());
    verify(versionPrivateStore, never()).markAsPublished(any(), any(), any(), any());
  }

  @Test
  public void testFirstPublicationCreatesThePublicVersionFromThePrivateOne() {
    privateVersionSyncState(null, true);
    VersionEntity privateVersion = new VersionEntity(VERSION_ID);
    privateVersion.setBaseId(new Id("base"));
    privateVersion.setCreationTime(new Date(500L));
    privateVersion.setModificationTime(new Date(600L));
    when(versionPrivateStore.get(CONTEXT, ITEM_ID, VERSION_ID))
        .thenReturn(Optional.of(privateVersion));
    Map<Id, Id> carriedOverElementIds =
        Collections.singletonMap(new Id("carried"), new Id("carried-revision"));
    when(elementPublicStore.listIds(CONTEXT, new ElementContext(ITEM_ID, VERSION_ID)))
        .thenReturn(carriedOverElementIds);

    publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    ArgumentCaptor<Id> revisionCaptor = ArgumentCaptor.forClass(Id.class);
    ArgumentCaptor<Date> publishTimeCaptor = ArgumentCaptor.forClass(Date.class);
    verify(versionPublicStore).create(eq(CONTEXT), eq(ITEM_ID), eq(privateVersion),
        revisionCaptor.capture(), eq(carriedOverElementIds), publishTimeCaptor.capture(),
        eq(MESSAGE));
    verify(versionPublicStore, never()).update(any(), any(), any(), any(), any(), any(), any());

    Assert.assertNotNull(UUID.fromString(revisionCaptor.getValue().getValue()));
    verify(versionPrivateStore)
        .markAsPublished(CONTEXT, ITEM_ID, VERSION_ID, publishTimeCaptor.getValue());
  }

  @Test
  public void testFirstPublicationFailsWhenThePrivateVersionIsMissing() {
    privateVersionSyncState(null, true);
    when(versionPrivateStore.get(CONTEXT, ITEM_ID, VERSION_ID)).thenReturn(Optional.empty());

    try {
      publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);
      Assert.fail("A dirty sync state without a private version must fail");
    } catch (IllegalArgumentException expected) {
      Assert.assertEquals(expected.getMessage(), String.format(
          "Item Id %s, version Id %s: Non existing version cannot be pushed.",
          ITEM_ID, VERSION_ID));
    }
    verify(versionPrivateStore, never()).markAsPublished(any(), any(), any(), any());
  }

  @Test
  public void testRepublicationAddsARevisionToTheExistingPublicVersion() {
    privateVersionSyncState(LAST_PUBLISH_TIME, true);
    publicVersionSyncState(LAST_PUBLISH_TIME);

    publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    ArgumentCaptor<VersionEntity> versionCaptor = ArgumentCaptor.forClass(VersionEntity.class);
    ArgumentCaptor<Date> publishTimeCaptor = ArgumentCaptor.forClass(Date.class);
    verify(versionPublicStore).update(eq(CONTEXT), eq(ITEM_ID), versionCaptor.capture(),
        any(Id.class), eq(Collections.<Id, Id>emptyMap()), publishTimeCaptor.capture(),
        eq(MESSAGE));
    verify(versionPublicStore, never()).create(any(), any(), any(), any(), any(), any(), any());

    Assert.assertEquals(versionCaptor.getValue().getId(), VERSION_ID);
    Assert.assertTrue(publishTimeCaptor.getValue().after(LAST_PUBLISH_TIME),
        "The new revision must carry a publish time later than the previous one");
    verify(versionPrivateStore, never()).get(any(), any(), any());
  }

  @Test
  public void testPublishUsesAFreshRevisionIdForEveryPublication() {
    privateVersionSyncState(LAST_PUBLISH_TIME, true);
    publicVersionSyncState(LAST_PUBLISH_TIME);

    publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);
    publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    ArgumentCaptor<Id> revisionCaptor = ArgumentCaptor.forClass(Id.class);
    verify(versionPublicStore, times(2)).update(any(), any(), any(), revisionCaptor.capture(),
        any(), any(), any());
    Assert.assertNotEquals(revisionCaptor.getAllValues().get(0),
        revisionCaptor.getAllValues().get(1));
  }

  @Test
  public void testFirstPublicationCopiesEveryPrivateElementToPublic() {
    ElementEntity dirtyElement = CollaborationTestFixtures.element(new Id("dirty"), Id.ZERO, "d");
    ElementEntity cleanElement = CollaborationTestFixtures.element(new Id("clean"), Id.ZERO, "c");
    Date previousPublishTime = new Date(900L);
    givenFirstPublication();
    privateElementSyncStates(
        new SynchronizationStateEntity(dirtyElement.getId(), Id.ZERO, null, true),
        new SynchronizationStateEntity(cleanElement.getId(), Id.ZERO, previousPublishTime, false));
    privateElement(dirtyElement);
    privateElement(cleanElement);

    CollaborationPublishResult result =
        publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    Date publishTime = capturedVersionPublishTimeOfCreate();
    verify(elementPublicStore)
        .create(eq(CONTEXT), any(ElementContext.class), eq(dirtyElement), eq(publishTime));
    verify(elementPublicStore).create(eq(CONTEXT), any(ElementContext.class), eq(cleanElement),
        eq(previousPublishTime));
    verify(elementPrivateStore)
        .markAsPublished(eq(CONTEXT), any(ElementContext.class), eq(dirtyElement.getId()),
            eq(publishTime));
    verify(elementPrivateStore, never())
        .markAsPublished(any(), any(), eq(cleanElement.getId()), any());

    Assert.assertEquals(result.getChange().getChangedElements().size(), 2);
    for (CollaborationElementChange change : result.getChange().getChangedElements()) {
      Assert.assertEquals(change.getAction(), Action.CREATE);
    }
    Assert.assertNull(result.getChange().getChangedVersion());
  }

  @Test
  public void testFirstPublicationSkipsSyncStateWithoutAnElement() {
    Id elementId = new Id("ghost");
    givenFirstPublication();
    privateElementSyncStates(new SynchronizationStateEntity(elementId, Id.ZERO, null, true));
    when(elementPrivateStore.get(any(), any(), eq(elementId))).thenReturn(Optional.empty());

    CollaborationPublishResult result =
        publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    verify(elementPublicStore, never()).create(any(), any(), any(), any());
    verify(elementPrivateStore, never()).markAsPublished(any(), any(), any(), any());
    Assert.assertTrue(result.getChange().getChangedElements().isEmpty());
  }

  @Test
  public void testFirstPublicationReportsTheVersionDataElementAsAVersionChange() {
    ElementEntity versionDataElement =
        CollaborationTestFixtures.element(Id.ZERO, null, "version data");
    givenFirstPublication();
    privateElementSyncStates(new SynchronizationStateEntity(Id.ZERO, Id.ZERO, null, true));
    privateElement(versionDataElement);

    CollaborationPublishResult result =
        publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    Assert.assertTrue(result.getChange().getChangedElements().isEmpty());
    Assert.assertNotNull(result.getChange().getChangedVersion());
    Assert.assertEquals(result.getChange().getChangedVersion().getAction(), Action.CREATE);
    Assert.assertEquals(result.getChange().getChangedVersion().getItemVersion().getId(),
        VERSION_ID);
    Assert.assertEquals(
        result.getChange().getChangedVersion().getItemVersion().getData().getInfo().getName(),
        "version data");
  }

  @Test
  public void testRepublicationIgnoresElementsThatWereNotChangedLocally() {
    ElementEntity dirtyElement = CollaborationTestFixtures.element(new Id("dirty"), Id.ZERO, "d");
    Id cleanElementId = new Id("clean");
    givenRepublication();
    privateElementSyncStates(
        new SynchronizationStateEntity(dirtyElement.getId(), Id.ZERO, LAST_PUBLISH_TIME, true),
        new SynchronizationStateEntity(cleanElementId, Id.ZERO, LAST_PUBLISH_TIME, false));
    privateElement(dirtyElement);

    CollaborationPublishResult result =
        publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    verify(elementPrivateStore, never()).get(any(), any(), eq(cleanElementId));
    Assert.assertEquals(result.getChange().getChangedElements().size(), 1);
    Assert.assertEquals(
        result.getChange().getChangedElements().iterator().next().getElement().getId(),
        dirtyElement.getId());
  }

  @Test
  public void testRepublicationUpdatesAnElementThatIsAlreadyOnPublic() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "updated");
    givenRepublication();
    privateElementSyncStates(
        new SynchronizationStateEntity(element.getId(), Id.ZERO, LAST_PUBLISH_TIME, true));
    publicElementSyncStates(new SynchronizationStateEntity(element.getId(), new Id("public-rev"),
        LAST_PUBLISH_TIME, false));
    privateElement(element);

    CollaborationPublishResult result =
        publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    Date publishTime = capturedVersionPublishTimeOfUpdate();
    verify(elementPublicStore)
        .update(eq(CONTEXT), any(ElementContext.class), eq(element), eq(publishTime));
    verify(elementPublicStore, never()).create(any(), any(), any(), any());
    verify(elementPrivateStore)
        .markAsPublished(CONTEXT, PRIVATE_ELEMENT_CONTEXT, element.getId(), publishTime);
    Assert.assertEquals(result.getChange().getChangedElements().iterator().next().getAction(),
        Action.UPDATE);
  }

  @Test
  public void testRepublicationCreatesAnElementThatIsNotYetOnPublic() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "new");
    givenRepublication();
    privateElementSyncStates(
        new SynchronizationStateEntity(element.getId(), Id.ZERO, null, true));
    publicElementSyncStates(new SynchronizationStateEntity(new Id("other"), new Id("public-rev"),
        LAST_PUBLISH_TIME, false));
    privateElement(element);

    CollaborationPublishResult result =
        publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    Date publishTime = capturedVersionPublishTimeOfUpdate();
    verify(elementPublicStore)
        .create(eq(CONTEXT), any(ElementContext.class), eq(element), eq(publishTime));
    verify(elementPublicStore, never()).update(any(), any(), any(), any());
    Assert.assertEquals(result.getChange().getChangedElements().iterator().next().getAction(),
        Action.CREATE);
  }

  @Test
  public void testRepublicationReadsPrivateElementsWithThePrivateRevisionContext() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "new");
    givenRepublication();
    privateElementSyncStates(
        new SynchronizationStateEntity(element.getId(), Id.ZERO, null, true));
    privateElement(element);

    publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    verify(elementPrivateStore).get(CONTEXT, PRIVATE_ELEMENT_CONTEXT, element.getId());
  }

  @Test
  public void testRepublicationDeletesFromPublicAnElementThatWasDeletedLocally() {
    Id elementId = new Id("e1");
    ElementEntity publicElement =
        CollaborationTestFixtures.element(elementId, Id.ZERO, "public content");
    givenRepublication();
    privateElementSyncStates(
        new SynchronizationStateEntity(elementId, Id.ZERO, LAST_PUBLISH_TIME, true));
    when(elementPrivateStore.get(any(), any(), eq(elementId))).thenReturn(Optional.empty());
    when(elementPublicStore.get(any(), any(), eq(elementId)))
        .thenReturn(Optional.of(publicElement));

    CollaborationPublishResult result =
        publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    Date publishTime = capturedVersionPublishTimeOfUpdate();
    verify(elementPublicStore)
        .delete(eq(CONTEXT), any(ElementContext.class), eq(publicElement), eq(publishTime));
    verify(elementPrivateStore)
        .markDeletionAsPublished(CONTEXT, PRIVATE_ELEMENT_CONTEXT, elementId, publishTime);
    verify(elementPrivateStore, never()).markAsPublished(any(), any(), any(), any());

    CollaborationElementChange change = result.getChange().getChangedElements().iterator().next();
    Assert.assertEquals(change.getAction(), Action.DELETE);
    Assert.assertEquals(change.getElement().getId(), elementId);
  }

  @Test
  public void testRepublicationFailsWhenTheElementToDeleteIsMissingFromPublic() {
    Id elementId = new Id("e1");
    givenRepublication();
    privateElementSyncStates(
        new SynchronizationStateEntity(elementId, Id.ZERO, LAST_PUBLISH_TIME, true));
    when(elementPrivateStore.get(any(), any(), eq(elementId))).thenReturn(Optional.empty());
    when(elementPublicStore.get(any(), any(), eq(elementId))).thenReturn(Optional.empty());

    try {
      publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);
      Assert.fail("Deleting an element that is not on public must fail");
    } catch (IllegalStateException expected) {
      Assert.assertEquals(expected.getMessage(),
          "Element that should be deleted from public must exist there");
    }
    verify(elementPrivateStore, never()).markDeletionAsPublished(any(), any(), any(), any());
  }

  @Test
  public void testPublishedElementsCarryTheRevisionIdOfTheNewPublicRevision() {
    ElementEntity element = CollaborationTestFixtures.element(new Id("e1"), Id.ZERO, "new");
    givenRepublication();
    privateElementSyncStates(
        new SynchronizationStateEntity(element.getId(), Id.ZERO, null, true));
    privateElement(element);

    publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    ArgumentCaptor<Id> versionRevisionCaptor = ArgumentCaptor.forClass(Id.class);
    verify(versionPublicStore).update(any(), any(), any(), versionRevisionCaptor.capture(), any(),
        any(), any());
    ArgumentCaptor<ElementContext> elementContextCaptor =
        ArgumentCaptor.forClass(ElementContext.class);
    verify(elementPublicStore)
        .create(any(), elementContextCaptor.capture(), eq(element), any());

    Assert.assertEquals(elementContextCaptor.getValue(), new ElementContext(ITEM_ID, VERSION_ID,
        versionRevisionCaptor.getValue()));
  }

  @Test
  public void testPublishReturnsAnInitialisedChangeWhenNothingWasPublished() {
    givenRepublication();

    CollaborationPublishResult result =
        publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    Assert.assertNotNull(result.getChange());
    Assert.assertTrue(result.getChange().getChangedElements().isEmpty());
    Assert.assertNull(result.getChange().getChangedVersion());
  }

  @Test
  public void testRepublicationReportsTheVersionDataElementAsAVersionChange() {
    ElementEntity versionDataElement =
        CollaborationTestFixtures.element(Id.ZERO, null, "version data");
    givenRepublication();
    privateElementSyncStates(
        new SynchronizationStateEntity(Id.ZERO, Id.ZERO, LAST_PUBLISH_TIME, true));
    publicElementSyncStates(
        new SynchronizationStateEntity(Id.ZERO, new Id("public-rev"), LAST_PUBLISH_TIME, false));
    privateElement(versionDataElement);

    CollaborationPublishResult result =
        publishService.publish(CONTEXT, ITEM_ID, VERSION_ID, MESSAGE);

    Assert.assertTrue(result.getChange().getChangedElements().isEmpty());
    Assert.assertEquals(result.getChange().getChangedVersion().getAction(), Action.UPDATE);
    Assert.assertEquals(
        result.getChange().getChangedVersion().getItemVersion().getData().getRelations(),
        versionDataElement.getRelations());
  }

  private void givenFirstPublication() {
    privateVersionSyncState(null, true);
    when(versionPrivateStore.get(CONTEXT, ITEM_ID, VERSION_ID))
        .thenReturn(Optional.of(new VersionEntity(VERSION_ID)));
  }

  private void givenRepublication() {
    privateVersionSyncState(LAST_PUBLISH_TIME, true);
    publicVersionSyncState(LAST_PUBLISH_TIME);
  }

  private void privateVersionSyncState(Date publishTime, boolean dirty) {
    when(versionPrivateStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID)).thenReturn(
        Optional.of(new SynchronizationStateEntity(VERSION_ID, Id.ZERO, publishTime, dirty)));
  }

  private void publicVersionSyncState(Date publishTime) {
    when(versionPublicStore.getSynchronizationState(CONTEXT, ITEM_ID, VERSION_ID, null)).thenReturn(
        Optional.of(
            new SynchronizationStateEntity(VERSION_ID, new Id("public-rev"), publishTime, false)));
  }

  private void privateElementSyncStates(SynchronizationStateEntity... syncStates) {
    Collection<SynchronizationStateEntity> states =
        CollaborationTestFixtures.syncStates(syncStates);
    when(elementPrivateStore.listSynchronizationStates(eq(CONTEXT), any(ElementContext.class)))
        .thenReturn(states);
  }

  private void publicElementSyncStates(SynchronizationStateEntity... syncStates) {
    when(elementPublicStore.listSynchronizationStates(eq(CONTEXT), any(ElementContext.class)))
        .thenReturn(CollaborationTestFixtures.syncStates(syncStates));
  }

  private void privateElement(ElementEntity element) {
    when(elementPrivateStore.get(any(), any(), eq(element.getId())))
        .thenReturn(Optional.of(element));
  }

  private Date capturedVersionPublishTimeOfCreate() {
    ArgumentCaptor<Date> publishTimeCaptor = ArgumentCaptor.forClass(Date.class);
    verify(versionPublicStore).create(any(), any(), any(), any(), any(),
        publishTimeCaptor.capture(), any());
    return publishTimeCaptor.getValue();
  }

  private Date capturedVersionPublishTimeOfUpdate() {
    ArgumentCaptor<Date> publishTimeCaptor = ArgumentCaptor.forClass(Date.class);
    verify(versionPublicStore).update(any(), any(), any(), any(), any(),
        publishTimeCaptor.capture(), any());
    return publishTimeCaptor.getValue();
  }
}
