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

package com.amdocs.zusammen.core.impl.item;

import com.amdocs.zusammen.adaptor.outbound.api.CollaborationAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.item.ItemVersionStateAdaptor;
import com.amdocs.zusammen.core.api.item.ElementManager;
import com.amdocs.zusammen.core.api.item.ItemManager;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.core.api.types.CoreItemVersionConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeChange;
import com.amdocs.zusammen.core.api.types.CoreMergeConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeResult;
import com.amdocs.zusammen.core.api.types.CorePublishResult;
import com.amdocs.zusammen.core.impl.Messages;
import com.amdocs.zusammen.core.impl.TestUtils;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionStatus;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.datatypes.itemversion.ItemVersionRevisions;
import com.amdocs.zusammen.datatypes.itemversion.Revision;
import com.amdocs.zusammen.datatypes.itemversion.Tag;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.amdocs.zusammen.datatypes.item.SynchronizationStatus.MERGING;
import static com.amdocs.zusammen.datatypes.item.SynchronizationStatus.OUT_OF_SYNC;
import static com.amdocs.zusammen.datatypes.item.SynchronizationStatus.UP_TO_DATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemVersionManagerImplTest {
  private static final UserInfo USER = new UserInfo("ItemVersionManagerImplTest_user");
  private static final SessionContext context = TestUtils.createSessionContext(USER, "test");

  @Mock
  private ItemVersionStateAdaptor stateAdaptorMock;
  @Mock
  private CollaborationAdaptor collaborationAdaptorMock;
  @Mock
  private ItemManager itemManagerMock;
  @Mock
  private ElementManager elementManagerMock;
  @InjectMocks
  @Spy
  private ItemVersionManagerImpl itemVersionManagerImpl;

  @BeforeMethod
  public void setUp()  {
    MockitoAnnotations.initMocks(this);
    when(itemVersionManagerImpl.getStateAdaptor(any())).thenReturn(stateAdaptorMock);
    when(itemVersionManagerImpl.getCollaborationAdaptor(any()))
        .thenReturn(collaborationAdaptorMock);
    when(itemVersionManagerImpl.getItemManager(any())).thenReturn(itemManagerMock);
    when(itemVersionManagerImpl.getElementManager(any())).thenReturn(elementManagerMock);
  }

  @Test
  public void testList()  {
    Id itemId = new Id();
    List<ItemVersion> retrievedVersions = Arrays.asList(
        TestUtils.createItemVersion(new Id(), new Id(), "v1"),
        TestUtils.createItemVersion(new Id(), new Id(), "v2"),
        TestUtils.createItemVersion(new Id(), new Id(), "v3"));
    doReturn(true).when(itemManagerMock).isExist(any(), any());
    doReturn(new Response<>(retrievedVersions)).when(stateAdaptorMock)
        .listItemVersions(context, Space.PRIVATE, itemId);

    Collection<ItemVersion> versions = itemVersionManagerImpl.list(context, Space.PRIVATE, itemId);
    Assert.assertEquals(versions, retrievedVersions);
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testListOnNonExistingItem()  {
    itemVersionManagerImpl.list(context, Space.PRIVATE, new Id());
  }

  @Test
  public void testGet()  {
    Id itemId = new Id();
    doReturn(true).when(itemManagerMock).isExist(context, itemId);

    Id versionId = new Id();
    ItemVersion retrievedVersion = TestUtils.createItemVersion(versionId, new Id(), "v1");
    doReturn(new Response<>(retrievedVersion)).when(stateAdaptorMock)
        .getItemVersion(context, Space.PRIVATE, itemId, retrievedVersion.getId());

    ItemVersion version =
        itemVersionManagerImpl.get(context, Space.PRIVATE, itemId, versionId,null);
    Assert.assertEquals(version, retrievedVersion);
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testGetOnNonExistingItem()  {
    itemVersionManagerImpl.get(context, Space.PRIVATE, new Id(), new Id(),null);
  }

  @Test
  public void testGetNonExisting()  {
    Id itemId = new Id();
    Id versionId = new Id();

    ItemVersion itemVersion = null;
    Response<ItemVersion> itemVersionResponse = new Response<>(itemVersion);

    doReturn(true).when(itemManagerMock).isExist(context, itemId);
    doReturn(itemVersionResponse).when(stateAdaptorMock)
        .getItemVersion(context, Space.PRIVATE, itemId, versionId);
    ItemVersion version = itemVersionManagerImpl.get(context, Space.PRIVATE, itemId, versionId,
        null);
    Assert.assertNull(version);
  }

  @Test
  public void testCreate()  {
    Id itemId = new Id();
    Id baseVersionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, baseVersionId);

    ItemVersionData data = new ItemVersionData();
    data.setInfo(TestUtils.createInfo("v1"));
    data.setRelations(Arrays.asList(new Relation(), new Relation()));

    doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock).createItemVersion
        (any(), any(), any(), any(), any());
    doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock).createItemVersion
        (any(), any(), any(), any(), any(), any(), any());
    Id versionId = itemVersionManagerImpl.create(context, itemId, baseVersionId, data);
    Assert.assertNotNull(versionId);

    verify(collaborationAdaptorMock)
        .createItemVersion(context, itemId, baseVersionId, versionId, data);
    verify(stateAdaptorMock)
        .createItemVersion(any(), any(), any(), any(), any(),
            any(), any());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testFailCreateWithNullId() {
    itemVersionManagerImpl.create(context, new Id(), null, null, new ItemVersionData());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testFailCreateWithExistingIdOnPrivate() {
    Id itemId = new Id("itemId");
    Id versionId = new Id("versionId");
    doReturn(new Response<>(true)).when(stateAdaptorMock).isItemVersionExist(context, Space.PRIVATE, itemId, versionId);

    itemVersionManagerImpl.create(context, itemId, versionId, null, new ItemVersionData());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testFailCreateWithExistingIdOnPublic() {
    Id itemId = new Id("itemId");
    Id versionId = new Id("versionId");
    doReturn(new Response<>(false)).when(stateAdaptorMock).isItemVersionExist(context, Space.PRIVATE, itemId, versionId);
    doReturn(new Response<>(true)).when(stateAdaptorMock).isItemVersionExist(context, Space.PUBLIC, itemId, versionId);

    itemVersionManagerImpl.create(context, itemId, versionId, null, new ItemVersionData());
  }

  @Test
  public void testCreateWithId()  {
    Id itemId = new Id();
    Id baseVersionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, baseVersionId);

    Id inputVersionId = new Id();
    doReturn(new Response<>(false)).when(stateAdaptorMock).isItemVersionExist(context, Space.PRIVATE, itemId, inputVersionId);
    doReturn(new Response<>(false)).when(stateAdaptorMock).isItemVersionExist(context, Space.PUBLIC, itemId, inputVersionId);

    ItemVersionData data = new ItemVersionData();
    data.setInfo(TestUtils.createInfo("v1"));
    data.setRelations(Arrays.asList(new Relation(), new Relation()));

    doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock).createItemVersion
                                                                               (any(), any(), any(), any(), any());
    doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock).createItemVersion
                                                                       (any(), any(), any(), any(), any(), any(), any());
    Id versionId = itemVersionManagerImpl.create(context, itemId, inputVersionId, baseVersionId, data);
    Assert.assertNotNull(versionId);

    verify(collaborationAdaptorMock)
            .createItemVersion(context, itemId, baseVersionId, versionId, data);
    verify(stateAdaptorMock)
            .createItemVersion(any(), any(), any(), any(), any(),
                    any(), any());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testCreateOnNonExistingItem()  {
    itemVersionManagerImpl.create(context, new Id(), new Id(), new ItemVersionData());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testCreateBasedOnNonExisting()  {
    Id itemId = new Id();
    Id baseVersionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, baseVersionId);
    itemVersionManagerImpl.create(context, itemId, baseVersionId, new ItemVersionData());
  }

  @Test
  public void testUpdate()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    ItemVersionData data = new ItemVersionData();
    data.setInfo(TestUtils.createInfo("v1 updated"));
    data.setRelations(Arrays.asList(new Relation(), new Relation()));
    doReturn(new Response<>(true))
        .when(collaborationAdaptorMock).updateItemVersion(context, itemId, versionId, data);

    doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
        .updateItemVersion(any(), any(), any(), any(), any(),
            any());

    itemVersionManagerImpl.update(context, itemId, versionId, data);

    verify(collaborationAdaptorMock).updateItemVersion(context, itemId, versionId, data);

    verify(stateAdaptorMock)
        .updateItemVersion(any(), any(), any(), any(), any(),
            any());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testUpdateOnNonExistingItem()  {
    itemVersionManagerImpl.update(context, new Id(), new Id(), new ItemVersionData());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testUpdateNonExisting()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, versionId);
    itemVersionManagerImpl.update(context, itemId, versionId, new ItemVersionData());
  }

  @Test
  public void testDelete()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock)
        .deleteItemVersion(context, itemId, versionId);
    doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
        .deleteItemVersion(context, Space.PRIVATE, itemId, versionId);

    itemVersionManagerImpl.delete(context, itemId, versionId);

    verify(collaborationAdaptorMock).deleteItemVersion(context, itemId, versionId);
    verify(stateAdaptorMock).deleteItemVersion(context, Space.PRIVATE, itemId, versionId);
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testDeleteOnNonExistingItem()  {
    itemVersionManagerImpl.delete(context, new Id(), new Id());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testDeleteNonExisting()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, versionId);
    itemVersionManagerImpl.delete(context, itemId, versionId);
  }

  @Test
  public void testTagVersion()  {
    testTag(null);
  }

  @Test
  public void testTagChange()  {
    testTag(new Id());
  }

  private void testTag(Id changeId) {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    Tag tag = new Tag("tagName", "tagDesc");
    doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock)
        .tagItemVersion(context, itemId, versionId, changeId, tag);

    doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
        .updateItemVersionModificationTime(eq(context), eq(Space.PRIVATE), eq(itemId),
            eq(versionId), any(Date.class));

    itemVersionManagerImpl.tag(context, itemId, versionId, changeId, tag);

    verify(collaborationAdaptorMock).tagItemVersion(context, itemId, versionId, changeId, tag);
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testTagOnNonExistingItem()  {
    itemVersionManagerImpl
        .tag(context, new Id(), new Id(), new Id(), new Tag("tagName", "tagDesc"));
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testTagNonExisting()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, versionId);
    itemVersionManagerImpl.tag(context, itemId, versionId, new Id(), new Tag("tagName", "tagDesc"));
  }

  @Test
  public void testSuccessfulPublishNew()  {
    testSuccessfulPublish(new Id(), new Id(), true);
  }

  @Test
  public void testSuccessfulPublishExisting()  {
    testSuccessfulPublish(new Id(), new Id(), false);
  }

  private void testSuccessfulPublish(Id itemId, Id versionId, boolean newVersion) {
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    doReturn(new Response<>(new ItemVersionStatus(UP_TO_DATE, false)))
        .when(collaborationAdaptorMock).getItemVersionStatus(context, itemId, versionId);

    String message = "publish message";
    CorePublishResult publishResult = new CorePublishResult();
    CoreMergeChange change = createMergeChange(versionId, newVersion);
    publishResult.setChange(change);
    doReturn(new Response<>(publishResult)).when(collaborationAdaptorMock)
        .publishItemVersion(context, itemId, versionId, message);

    mockItemVersionChangeSave(Space.PUBLIC, itemId, versionId, newVersion, change);

    itemVersionManagerImpl.publish(context, itemId, versionId, message);

    verifySaveChangedElements(itemId, versionId, Space.PUBLIC, change.getChangedElements());

  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testPublishOnNonExistingItem()  {
    itemVersionManagerImpl.publish(context, new Id(), new Id(), "");
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testPublishNonExisting()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, versionId);

    itemVersionManagerImpl.publish(context, itemId, versionId, "");
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testPublishOutOfSync()  {
    testPublishNotAllowed(new ItemVersionStatus(OUT_OF_SYNC, false));
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testPublishMerging()  {
    testPublishNotAllowed(new ItemVersionStatus(MERGING, false));
  }

  private void testPublishNotAllowed(ItemVersionStatus status) {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    doReturn(new Response<>(status)).when(collaborationAdaptorMock)
        .getItemVersionStatus(context, itemId, versionId);

    itemVersionManagerImpl.publish(context, itemId, versionId, "");
  }

  @Test
  public void testSuccessfulSyncNew()  {
    testSuccessfulSync(new Id(), new Id(), true);
  }

  @Test
  public void testSuccessfulSyncExisting()  {
    testSuccessfulSync(new Id(), new Id(), false);
  }

  private void testSuccessfulSync(Id itemId, Id versionId, boolean newVersion) {
    mockExistingVersion(Space.PUBLIC, itemId, versionId);

    CoreMergeResult retrievedSyncResult = new CoreMergeResult();
    CoreMergeChange change = createMergeChange(versionId, newVersion);
    retrievedSyncResult.setChange(change);
    doReturn(new Response<>(retrievedSyncResult))
        .when(collaborationAdaptorMock).syncItemVersion(context, itemId, versionId);

    mockItemVersionChangeSave(Space.PRIVATE, itemId, versionId, newVersion, change);

    CoreMergeResult syncResult = itemVersionManagerImpl.sync(context, itemId, versionId);
    Assert.assertEquals(syncResult, retrievedSyncResult);

    verifySaveChangedElements(itemId, versionId, Space.PRIVATE, change.getChangedElements());

  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testSyncOnNonExistingItem()  {
    itemVersionManagerImpl.sync(context, new Id(), new Id());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testSyncNonExisting()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PUBLIC, itemId, versionId);
    itemVersionManagerImpl.sync(context, itemId, versionId);
  }

  @Test
  public void testMerge()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    Id sourceVersionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, sourceVersionId);

    CoreMergeResult retrievedMergeResult = new CoreMergeResult();
    CoreMergeChange change = createMergeChange(sourceVersionId, false);
    retrievedMergeResult.setChange(change);
    doReturn(new Response<>(retrievedMergeResult))
        .when(collaborationAdaptorMock)
        .mergeItemVersion(context, itemId, versionId, sourceVersionId);

    mockItemVersionChangeSave(Space.PRIVATE, itemId, sourceVersionId, false, change);

    CoreMergeResult result =
        itemVersionManagerImpl.merge(context, itemId, versionId, sourceVersionId);

    Assert.assertEquals(result, retrievedMergeResult);
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testMergeOnNonExistingItem()  {
    itemVersionManagerImpl.merge(context, new Id(), new Id(), new Id());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testMergeNonExisting()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, versionId);

    itemVersionManagerImpl.merge(context, itemId, versionId, new Id());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testMergeNonExistingSource()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    Id sourceVersionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, sourceVersionId);

    itemVersionManagerImpl.merge(context, itemId, versionId, sourceVersionId);
  }

  @Test
  public void testListRevision()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    ItemVersionRevisions retrievedRevision = new ItemVersionRevisions();
    doReturn(new Response<>(retrievedRevision)).when(collaborationAdaptorMock)
        .listItemVersionRevisions(context, itemId, versionId);

    ItemVersionRevisions history = itemVersionManagerImpl.listRevisions(context, itemId, versionId);

    Assert.assertEquals(history, retrievedRevision);
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testListRevisionOnNonExistingItem()  {
    itemVersionManagerImpl.listRevisions(context, new Id(), new Id());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testListRevisionNonExisting()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, versionId);
    itemVersionManagerImpl.listRevisions(context, itemId, versionId);
  }

  @Test
  public void testResetRevision()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    Id changeRef = new Id("changeRef");
    CoreMergeChange mergeChange = createMergeChange(versionId, false);
    doReturn(new Response<>(mergeChange))
        .when(collaborationAdaptorMock)
        .resetItemVersionRevision(context, itemId, versionId, changeRef);

    doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock).updateItemVersion(eq(context), eq
        (Space.PRIVATE), eq(itemId), eq(versionId), eq(mergeChange.getChangedVersion()
        .getItemVersion().getData()), any(Date.class));

    itemVersionManagerImpl.resetRevision(context, itemId, versionId, changeRef);

    verifySaveChangedElements(itemId, versionId, Space.PRIVATE, mergeChange.getChangedElements());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testResetRevisionOnNonExistingItem()  {
    itemVersionManagerImpl.resetRevision(context, new Id(), new Id(), new Id("changeRef"));
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testResetRevisionNonExisting()  {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, versionId);
    itemVersionManagerImpl.resetRevision(context, itemId, versionId, new Id("changeRef"));
  }

  @Test
  public void testUpdateItemVersionModificationTime()  {
    Id itemId = new Id();
    Id versionId = new Id();
    Date modificationTime = new Date();
    Space space = Space.PRIVATE;
    itemVersionManagerImpl
        .updateModificationTime(context, space, itemId, versionId, modificationTime);

    verify(stateAdaptorMock)
        .updateItemVersionModificationTime(context, space, itemId, versionId, modificationTime);
    verify(itemManagerMock).updateModificationTime(context, itemId, modificationTime);
  }

  @Test
  public void testGetRevisionOfAVersionReadsFromTheCollaborationStore() {
    Id itemId = new Id();
    Id versionId = new Id();
    Id revisionId = new Id("revision");
    doReturn(true).when(itemManagerMock).isExist(context, itemId);

    ItemVersion retrievedVersion = TestUtils.createItemVersion(versionId, new Id(), "v1");
    doReturn(new Response<>(retrievedVersion)).when(collaborationAdaptorMock)
        .getItemVersion(context, Space.PRIVATE, itemId, versionId, revisionId);

    Assert.assertSame(
        itemVersionManagerImpl.get(context, Space.PRIVATE, itemId, versionId, revisionId),
        retrievedVersion);
    verify(stateAdaptorMock, never()).getItemVersion(any(), any(), any(), any());
  }

  @Test
  public void testListOnAMissingItemReportsItemNotExist() {
    Id itemId = new Id();
    doReturn(false).when(itemManagerMock).isExist(context, itemId);

    ReturnCode returnCode = TestUtils
        .captureFailure(() -> itemVersionManagerImpl.list(context, Space.PRIVATE, itemId));

    TestUtils.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_DOES_NOT_EXIST);
    Assert.assertEquals(returnCode.getMessage(), String.format(Messages.ITEM_NOT_EXIST, itemId));
  }

  @Test
  public void testListFailurePropagates() {
    Id itemId = new Id();
    doReturn(true).when(itemManagerMock).isExist(context, itemId);
    ReturnCode cause = stateStoreFailure();
    doReturn(new Response<Collection<ItemVersion>>(cause)).when(stateAdaptorMock)
        .listItemVersions(context, Space.PRIVATE, itemId);

    TestUtils.assertWrappedFailure(TestUtils
            .captureFailure(() -> itemVersionManagerImpl.list(context, Space.PRIVATE, itemId)),
        ErrorCode.ZU_ITEM_VERSION_LIST, cause);
  }

  @Test
  public void testUpdateOfAMissingVersionNamesTheVersionInTheMessage() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PRIVATE, itemId, versionId);

    ReturnCode returnCode = TestUtils.captureFailure(
        () -> itemVersionManagerImpl.update(context, itemId, versionId, new ItemVersionData()));

    Assert.assertEquals(returnCode.getMessage(), String
        .format(Messages.ITEM_VERSION_NOT_EXIST, itemId, versionId, Space.PRIVATE));
  }

  @Test
  public void testCreateWithAVersionIdCarryingNoValueIsRejected() {
    Id itemId = new Id();

    ReturnCode returnCode = TestUtils.captureFailure(() -> itemVersionManagerImpl
        .create(context, itemId, new Id(null), null, new ItemVersionData()));

    TestUtils.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_VERSION_CREATE);
    Assert.assertEquals(returnCode.getMessage(),
        String.format(Messages.VERSION_ID_TO_CREATE_CANNOT_BE_NULL, itemId));
    verify(collaborationAdaptorMock, never())
        .createItemVersion(any(), any(), any(), any(), any());
  }

  @Test
  public void testCreateWithoutABaseVersionSkipsTheBaseVersionCheck() {
    Id itemId = new Id();
    doReturn(true).when(itemManagerMock).isExist(context, itemId);
    ItemVersionData data = new ItemVersionData();
    data.setInfo(TestUtils.createInfo("v1"));
    doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock)
        .createItemVersion(any(), any(), any(), any(), any());
    doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
        .createItemVersion(any(), any(), any(), any(), any(), any(), any());

    Id versionId = itemVersionManagerImpl.create(context, itemId, null, data);

    Assert.assertNotNull(versionId);
    verify(collaborationAdaptorMock).createItemVersion(context, itemId, null, versionId, data);
    verify(stateAdaptorMock, never()).isItemVersionExist(any(), any(), any(), any());
    verify(itemManagerMock).updateModificationTime(eq(context), eq(itemId), any(Date.class));
  }

  @Test
  public void testCreateFailsWhenTheCollaborationStoreRejectsIt() {
    Id itemId = new Id();
    doReturn(true).when(itemManagerMock).isExist(context, itemId);
    ReturnCode cause = collaborationStoreFailure();
    doReturn(new Response<Void>(cause)).when(collaborationAdaptorMock)
        .createItemVersion(any(), any(), any(), any(), any());

    TestUtils.assertWrappedFailure(TestUtils.captureFailure(() -> itemVersionManagerImpl
        .create(context, itemId, null, new ItemVersionData())),
        ErrorCode.ZU_ITEM_VERSION_CREATE, cause);

    verify(stateAdaptorMock, never())
        .createItemVersion(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testPublishWithoutAResultSavesNothing() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);
    doReturn(new Response<>(new ItemVersionStatus(UP_TO_DATE, false)))
        .when(collaborationAdaptorMock).getItemVersionStatus(context, itemId, versionId);
    CorePublishResult noResult = null;
    doReturn(new Response<>(noResult)).when(collaborationAdaptorMock)
        .publishItemVersion(context, itemId, versionId, "publish message");

    itemVersionManagerImpl.publish(context, itemId, versionId, "publish message");

    verify(elementManagerMock, never()).saveMergeChange(any(), any(), any(), any());
  }

  @Test
  public void testSyncWithAnUnresolvedConflictSavesNothing() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PUBLIC, itemId, versionId);

    CoreMergeResult retrievedSyncResult = new CoreMergeResult();
    retrievedSyncResult.setConflict(unresolvedConflict());
    retrievedSyncResult.setChange(createMergeChange(versionId, false));
    doReturn(new Response<>(retrievedSyncResult))
        .when(collaborationAdaptorMock).syncItemVersion(context, itemId, versionId);

    Assert.assertSame(itemVersionManagerImpl.sync(context, itemId, versionId),
        retrievedSyncResult);
    verify(elementManagerMock, never()).saveMergeChange(any(), any(), any(), any());
  }

  @Test
  public void testSyncWithoutAResultSavesNothing() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PUBLIC, itemId, versionId);
    CoreMergeResult noResult = null;
    doReturn(new Response<>(noResult))
        .when(collaborationAdaptorMock).syncItemVersion(context, itemId, versionId);

    Assert.assertNull(itemVersionManagerImpl.sync(context, itemId, versionId));
    verify(elementManagerMock, never()).saveMergeChange(any(), any(), any(), any());
  }

  @Test
  public void testSuccessfulForceSync() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PUBLIC, itemId, versionId);

    CoreMergeResult retrievedResult = new CoreMergeResult();
    CoreMergeChange change = createMergeChange(versionId, false);
    retrievedResult.setChange(change);
    doReturn(new Response<>(retrievedResult))
        .when(collaborationAdaptorMock).forceSyncItemVersion(context, itemId, versionId);
    mockItemVersionChangeSave(Space.PRIVATE, itemId, versionId, false, change);

    Assert.assertSame(itemVersionManagerImpl.forceSync(context, itemId, versionId),
        retrievedResult);
    verifySaveChangedElements(itemId, versionId, Space.PRIVATE, change.getChangedElements());
  }

  @Test
  public void testForceSyncWithAnUnresolvedConflictSavesNothing() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PUBLIC, itemId, versionId);

    CoreMergeResult retrievedResult = new CoreMergeResult();
    retrievedResult.setConflict(unresolvedConflict());
    retrievedResult.setChange(createMergeChange(versionId, false));
    doReturn(new Response<>(retrievedResult))
        .when(collaborationAdaptorMock).forceSyncItemVersion(context, itemId, versionId);

    Assert.assertSame(itemVersionManagerImpl.forceSync(context, itemId, versionId),
        retrievedResult);
    verify(elementManagerMock, never()).saveMergeChange(any(), any(), any(), any());
  }

  @Test
  public void testForceSyncWithoutAResultSavesNothing() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PUBLIC, itemId, versionId);
    CoreMergeResult noResult = null;
    doReturn(new Response<>(noResult))
        .when(collaborationAdaptorMock).forceSyncItemVersion(context, itemId, versionId);

    Assert.assertNull(itemVersionManagerImpl.forceSync(context, itemId, versionId));
    verify(elementManagerMock, never()).saveMergeChange(any(), any(), any(), any());
  }

  @Test
  public void testMergeWithoutAResultSavesNothing() {
    Id itemId = new Id();
    Id versionId = new Id();
    Id sourceVersionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);
    mockExistingVersion(Space.PRIVATE, itemId, sourceVersionId);
    CoreMergeResult noResult = null;
    doReturn(new Response<>(noResult)).when(collaborationAdaptorMock)
        .mergeItemVersion(context, itemId, versionId, sourceVersionId);

    Assert.assertNull(itemVersionManagerImpl.merge(context, itemId, versionId, sourceVersionId));
    verify(elementManagerMock, never()).saveMergeChange(any(), any(), any(), any());
  }

  @Test(expectedExceptions = ZusammenException.class)
  public void testForceSyncNonExisting() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockNonExistingVersion(Space.PUBLIC, itemId, versionId);
    itemVersionManagerImpl.forceSync(context, itemId, versionId);
  }

  @Test
  public void testMergeWithAnUnresolvedConflictSavesNothing() {
    Id itemId = new Id();
    Id versionId = new Id();
    Id sourceVersionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);
    mockExistingVersion(Space.PRIVATE, itemId, sourceVersionId);

    CoreMergeResult retrievedResult = new CoreMergeResult();
    retrievedResult.setConflict(unresolvedConflict());
    retrievedResult.setChange(createMergeChange(sourceVersionId, false));
    doReturn(new Response<>(retrievedResult)).when(collaborationAdaptorMock)
        .mergeItemVersion(context, itemId, versionId, sourceVersionId);

    Assert.assertSame(itemVersionManagerImpl.merge(context, itemId, versionId, sourceVersionId),
        retrievedResult);
    verify(elementManagerMock, never()).saveMergeChange(any(), any(), any(), any());
  }

  @Test
  public void testGetRevision() {
    Id itemId = new Id();
    Id versionId = new Id();
    Id revisionId = new Id("revision");
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    Revision retrievedRevision = new Revision();
    doReturn(new Response<>(retrievedRevision)).when(collaborationAdaptorMock)
        .getItemVersionRevision(context, itemId, versionId, revisionId);

    Assert.assertSame(
        itemVersionManagerImpl.getRevision(context, itemId, versionId, revisionId),
        retrievedRevision);
  }

  @Test
  public void testGetRevisionFailurePropagates() {
    Id itemId = new Id();
    Id versionId = new Id();
    Id revisionId = new Id("revision");
    mockExistingVersion(Space.PRIVATE, itemId, versionId);
    ReturnCode cause = collaborationStoreFailure();
    doReturn(new Response<Revision>(cause)).when(collaborationAdaptorMock)
        .getItemVersionRevision(context, itemId, versionId, revisionId);

    TestUtils.assertWrappedFailure(TestUtils.captureFailure(
        () -> itemVersionManagerImpl.getRevision(context, itemId, versionId, revisionId)),
        ErrorCode.ZU_ITEM_VERSION_REVISION, cause);
  }

  @Test
  public void testRevertRevision() {
    Id itemId = new Id();
    Id versionId = new Id();
    Id revisionId = new Id("revision");
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    CoreMergeChange mergeChange = createMergeChange(versionId, false);
    doReturn(new Response<>(mergeChange)).when(collaborationAdaptorMock)
        .revertItemVersionRevision(context, itemId, versionId, revisionId);
    mockItemVersionChangeSave(Space.PRIVATE, itemId, versionId, false, mergeChange);

    itemVersionManagerImpl.revertRevision(context, itemId, versionId, revisionId);

    verifySaveChangedElements(itemId, versionId, Space.PRIVATE, mergeChange.getChangedElements());
  }

  @Test
  public void testRevertRevisionFailurePropagates() {
    Id itemId = new Id();
    Id versionId = new Id();
    Id revisionId = new Id("revision");
    mockExistingVersion(Space.PRIVATE, itemId, versionId);
    ReturnCode cause = collaborationStoreFailure();
    doReturn(new Response<CoreMergeChange>(cause)).when(collaborationAdaptorMock)
        .revertItemVersionRevision(context, itemId, versionId, revisionId);

    TestUtils.assertWrappedFailure(TestUtils.captureFailure(
        () -> itemVersionManagerImpl.revertRevision(context, itemId, versionId, revisionId)),
        ErrorCode.ZU_ITEM_VERSION_REVERT_REVISION, cause);
  }

  @Test
  public void testResetRevisionWithoutAChangeSavesNothing() {
    Id itemId = new Id();
    Id versionId = new Id();
    Id revisionId = new Id("revision");
    mockExistingVersion(Space.PRIVATE, itemId, versionId);
    CoreMergeChange noChange = null;
    doReturn(new Response<>(noChange)).when(collaborationAdaptorMock)
        .resetItemVersionRevision(context, itemId, versionId, revisionId);

    itemVersionManagerImpl.resetRevision(context, itemId, versionId, revisionId);

    verify(elementManagerMock, never()).saveMergeChange(any(), any(), any(), any());
    verify(stateAdaptorMock, never())
        .updateItemVersion(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testGetConflict() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);

    CoreItemVersionConflict retrievedConflict = new CoreItemVersionConflict();
    doReturn(new Response<>(retrievedConflict)).when(collaborationAdaptorMock)
        .getItemVersionConflict(context, itemId, versionId);

    Assert.assertSame(itemVersionManagerImpl.getConflict(context, itemId, versionId),
        retrievedConflict);
  }

  @Test
  public void testGetConflictFailurePropagates() {
    Id itemId = new Id();
    Id versionId = new Id();
    mockExistingVersion(Space.PRIVATE, itemId, versionId);
    ReturnCode cause = collaborationStoreFailure();
    doReturn(new Response<CoreItemVersionConflict>(cause)).when(collaborationAdaptorMock)
        .getItemVersionConflict(context, itemId, versionId);

    TestUtils.assertWrappedFailure(
        TestUtils.captureFailure(
            () -> itemVersionManagerImpl.getConflict(context, itemId, versionId)),
        ErrorCode.ZU_ITEM_VERSION_GET_CONFLICT, cause);
  }

  @Test
  public void testSaveMergeChangeWithoutAVersionChangeDoesNothing() {
    itemVersionManagerImpl.saveMergeChange(context, Space.PUBLIC, new Id(), null);

    verify(stateAdaptorMock, never())
        .createItemVersion(any(), any(), any(), any(), any(), any(), any());
    verify(stateAdaptorMock, never())
        .updateItemVersion(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testSaveMergeChangeFailurePropagates() {
    Id itemId = new Id();
    Id versionId = new Id();
    ItemVersionChange changedVersion = new ItemVersionChange();
    changedVersion.setAction(Action.UPDATE);
    changedVersion.setItemVersion(TestUtils.createItemVersion(versionId, new Id(), "v1"));

    ReturnCode cause = stateStoreFailure();
    doReturn(new Response<Void>(cause)).when(stateAdaptorMock)
        .updateItemVersion(eq(context), eq(Space.PUBLIC), eq(itemId), eq(versionId),
            eq(changedVersion.getItemVersion().getData()), any(Date.class));

    TestUtils.assertWrappedFailure(TestUtils.captureFailure(() -> itemVersionManagerImpl
            .saveMergeChange(context, Space.PUBLIC, itemId, changedVersion)),
        ErrorCode.ZU_ITEM_VERSION_SAVE_CHANGE, cause);
  }

  @Test
  public void testSaveMergeChangeOfAnUnsupportedActionIsRejected() {
    Id itemId = new Id();
    Id versionId = new Id();
    ItemVersionChange changedVersion = new ItemVersionChange();
    changedVersion.setAction(Action.DELETE);
    changedVersion.setItemVersion(TestUtils.createItemVersion(versionId, new Id(), "v1"));

    try {
      itemVersionManagerImpl.saveMergeChange(context, Space.PUBLIC, itemId, changedVersion);
      Assert.fail("Expected an unsupported-action failure");
    } catch (RuntimeException exception) {
      Assert.assertEquals(exception.getMessage(), String
          .format(Messages.UNSUPPORTED_VERSION_ACTION, itemId, versionId, Action.DELETE));
    }
    verify(stateAdaptorMock, never())
        .createItemVersion(any(), any(), any(), any(), any(), any(), any());
    verify(stateAdaptorMock, never())
        .updateItemVersion(any(), any(), any(), any(), any(), any());
  }

  private CoreMergeConflict unresolvedConflict() {
    CoreMergeConflict conflict = new CoreMergeConflict();
    conflict.setElementConflicts(Collections.singletonList(new CoreElementConflict()));
    return conflict;
  }

  private ReturnCode stateStoreFailure() {
    return new ReturnCode(ErrorCode.ST_ITEM_VERSION_GET, Module.ZMDP, "state store down", null);
  }

  private ReturnCode collaborationStoreFailure() {
    return new ReturnCode(ErrorCode.CL_ITEM_VERSION_GET, Module.ZCSP, "collaboration store down",
        null);
  }

  private void mockExistingVersion(Space space, Id itemId, Id versionId) {
    doReturn(true).when(itemManagerMock).isExist(context, itemId);
    doReturn(new Response<>(true)).when(stateAdaptorMock)
        .isItemVersionExist(context, space, itemId, versionId);
  }

  private void mockNonExistingVersion(Space space, Id itemId, Id versionId) {
    doReturn(true).when(itemManagerMock).isExist(context, itemId);
    doReturn(new Response<>(false)).when(stateAdaptorMock)
        .isItemVersionExist(context, space, itemId, versionId);
  }

  private void mockItemVersionChangeSave(Space space, Id itemId, Id versionId, boolean newVersion,
                                         CoreMergeChange change) {
    if (newVersion) {
      doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
          .createItemVersion(eq(context), eq(space), eq(itemId),
              eq(change.getChangedVersion().getItemVersion().getBaseId()), eq(versionId),
              eq(change.getChangedVersion().getItemVersion().getData()), any(Date.class));
    } else {
      doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
          .updateItemVersion(eq(context), eq(space), eq(itemId), eq(versionId),
              eq(change.getChangedVersion().getItemVersion().getData()), any(Date.class));
    }
  }

  private void verifySaveChangedElements(Id itemId, Id versionId, Space space,
                                         Collection<CoreElement> changedElements) {
    ElementContext elementContext = new ElementContext(itemId, versionId);
    verify(elementManagerMock)
        .saveMergeChange(eq(context), eq(space), eq(elementContext), eq(changedElements));
  }

  private CoreMergeChange createMergeChange(Id versionId, boolean newVersion) {
    CoreMergeChange change = new CoreMergeChange();

    ItemVersionChange changedVersion = new ItemVersionChange();
    changedVersion.setAction(newVersion ? Action.CREATE : Action.UPDATE);
    changedVersion.setItemVersion(TestUtils.createItemVersion(versionId, new Id(), "v1"));
    change.setChangedVersion(changedVersion);

    CoreElement element1 = createElement(Action.CREATE);
    CoreElement element2 = createElement(newVersion ? Action.CREATE : Action.UPDATE);
    CoreElement element3 = createElement(newVersion ? Action.CREATE : Action.DELETE);
    change.setChangedElements(Arrays.asList(element1, element2, element3));
    return change;
  }

  private CoreElement createElement(Action action) {
    CoreElement element = new CoreElement();
    element.setId(new Id());
    element.setAction(action);
    return element;
  }
}