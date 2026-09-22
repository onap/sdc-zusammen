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

package com.amdocs.zusammen.adaptor.inbound.impl.item;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.adaptor.inbound.api.types.item.Element;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementConflict;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementConflictInfo;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.ItemVersionConflict;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.MergeResult;
import com.amdocs.zusammen.core.api.item.ItemVersionManager;
import com.amdocs.zusammen.core.api.item.ItemVersionManagerFactory;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.core.api.types.CoreElementConflictInfo;
import com.amdocs.zusammen.core.api.types.CoreElementInfo;
import com.amdocs.zusammen.core.api.types.CoreItemVersionConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeChange;
import com.amdocs.zusammen.core.api.types.CoreMergeConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeResult;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import com.amdocs.zusammen.datatypes.item.ItemVersionStatus;
import com.amdocs.zusammen.datatypes.item.SynchronizationStatus;
import com.amdocs.zusammen.datatypes.itemversion.ItemVersionRevisions;
import com.amdocs.zusammen.datatypes.itemversion.Revision;
import com.amdocs.zusammen.datatypes.itemversion.Tag;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class ItemVersionAdaptorImplTest {

    private static final String PRODUCTION_FACTORY =
            "com.amdocs.zusammen.core.impl.item.ItemVersionManagerFactoryImpl";
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-1");
    private static final Id REVISION_ID = new Id("revision-1");

    public static class StubItemVersionManagerFactory extends ItemVersionManagerFactory {

        static ItemVersionManager manager;

        @Override
        public ItemVersionManager createInterface(SessionContext context) {
            return manager;
        }
    }

    @Mock
    private ItemVersionManager versionManager;

    private AutoCloseable mocks;
    private SessionContext context;
    private ItemVersionAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        // A class literal does not run a static initialiser. AbstractComponentFactory's one-shot load of
        // every factoryConfiguration.json has to happen before registerFactory, or the first factory
        // access reloads the production mapping over the stub.
        Class.forName(ItemVersionManagerFactory.class.getName(), true,
                ItemVersionManagerFactory.class.getClassLoader());
        StubItemVersionManagerFactory.manager = versionManager;
        AbstractFactoryBase.registerFactory(ItemVersionManagerFactory.class,
                StubItemVersionManagerFactory.class);
        context = AdaptorTestSupport.sessionContext("item-version-adaptor-user");
        adaptor = new ItemVersionAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        AdaptorTestSupport.restoreProductionFactory(ItemVersionManagerFactory.class,
                PRODUCTION_FACTORY);
        StubItemVersionManagerFactory.manager = null;
        mocks.close();
    }

    @Test
    public void testListReturnsManagerItemVersions() {
        when(versionManager.list(context, Space.PRIVATE, ITEM_ID))
                .thenReturn(Arrays.asList(itemVersion("version-1"), itemVersion("version-2")));

        Response<Collection<ItemVersion>> response = adaptor.list(context, Space.PRIVATE, ITEM_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().size(), 2);
        Assert.assertEquals(response.getValue().iterator().next().getId().getValue(), "version-1");
        verify(versionManager).list(context, Space.PRIVATE, ITEM_ID);
    }

    @Test
    public void testListReturnsEmptyCollectionWhenItemHasNoVersions() {
        when(versionManager.list(context, Space.BOTH, ITEM_ID))
                .thenReturn(Collections.<ItemVersion>emptyList());

        Response<Collection<ItemVersion>> response = adaptor.list(context, Space.BOTH, ITEM_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertTrue(response.getValue().isEmpty());
    }

    @Test
    public void testListFailureIsReportedAsAnUnsuccessfulResponse() {
        when(versionManager.list(context, Space.PUBLIC, ITEM_ID)).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSIONS_LIST, Module.ZSTM, "no rows"));

        Response<Collection<ItemVersion>> response = adaptor.list(context, Space.PUBLIC, ITEM_ID);

        Assert.assertFalse(response.isSuccessful());
        Assert.assertNotNull(response.getReturnCode());
    }

    @Test
    public void testGetWithoutRevisionPassesNullRevisionIdToManager() {
        when(versionManager.get(eq(context), eq(Space.PRIVATE), eq(ITEM_ID), eq(VERSION_ID),
                any())).thenReturn(itemVersion("version-1"));

        Response<ItemVersion> response = adaptor.get(context, Space.PRIVATE, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        verify(versionManager).get(context, Space.PRIVATE, ITEM_ID, VERSION_ID, null);
    }

    @Test
    public void testGetReturnsManagerItemVersion() {
        when(versionManager.get(context, Space.PRIVATE, ITEM_ID, VERSION_ID, REVISION_ID))
                .thenReturn(itemVersion("version-1"));

        Response<ItemVersion> response =
                adaptor.get(context, Space.PRIVATE, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getId().getValue(), "version-1");
        Assert.assertEquals(response.getValue().getData().getInfo().getName(),
                "version-1-version-info");
    }

    @Test
    public void testGetFailureIsMappedToItemVersionGetError() {
        when(versionManager.get(context, Space.PRIVATE, ITEM_ID, VERSION_ID, REVISION_ID))
                .thenThrow(AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSION_GET, Module.ZSTM,
                        "unknown revision"));

        Response<ItemVersion> response =
                adaptor.get(context, Space.PRIVATE, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_GET);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ITEM_VERSION_GET);
    }

    @Test
    public void testCreatePassesBaseVersionAndDataToManager() {
        when(versionManager.create(eq(context), eq(ITEM_ID), eq(VERSION_ID),
                any(ItemVersionData.class))).thenReturn(new Id("created-version"));

        Response<Id> response =
                adaptor.create(context, ITEM_ID, VERSION_ID, itemVersionData("new-version"));

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getValue(), "created-version");
        ArgumentCaptor<ItemVersionData> captor = ArgumentCaptor.forClass(ItemVersionData.class);
        verify(versionManager).create(eq(context), eq(ITEM_ID), eq(VERSION_ID), captor.capture());
        Assert.assertEquals(captor.getValue().getInfo().getName(), "new-version");
        Assert.assertEquals(captor.getValue().getRelations().size(), 1);
    }

    @Test
    public void testCreateFailureIsMappedToItemVersionCreateError() {
        when(versionManager.create(eq(context), eq(ITEM_ID), eq(VERSION_ID),
                any(ItemVersionData.class))).thenThrow(AdaptorTestSupport
                .failure(ErrorCode.MD_ITEM_VERSION_CREATE, Module.ZSTM, "base missing"));

        Response<Id> response =
                adaptor.create(context, ITEM_ID, VERSION_ID, itemVersionData("new-version"));

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_CREATE);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ITEM_VERSION_CREATE);
    }

    @Test
    public void testCreateMapsUnexpectedExceptionToSystemError() {
        when(versionManager.create(eq(context), eq(ITEM_ID), eq(VERSION_ID),
                any(ItemVersionData.class)))
                .thenThrow(new IllegalStateException("connection pool exhausted"));

        Response<Id> response =
                adaptor.create(context, ITEM_ID, VERSION_ID, itemVersionData("new-version"));

        Assert.assertFalse(response.isSuccessful());
        ReturnCode returnCode = response.getReturnCode();
        Assert.assertEquals(returnCode.getMessage(), "connection pool exhausted");
        AdaptorTestSupport.assertErrorCode(returnCode, Module.ZDB, ErrorCode.SYSTEM_ERROR);
        Assert.assertNull(returnCode.getReturnCode());
    }

    @Test
    public void testCreateWithGivenVersionIdPassesAllIdsToManager() {
        Id baseVersionId = new Id("base-version");
        when(versionManager.create(eq(context), eq(ITEM_ID), eq(VERSION_ID), eq(baseVersionId),
                any(ItemVersionData.class))).thenReturn(VERSION_ID);

        Response<Id> response = adaptor.create(context, ITEM_ID, VERSION_ID, baseVersionId,
                itemVersionData("preallocated-version"));

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getValue(), "version-1");
        ArgumentCaptor<ItemVersionData> captor = ArgumentCaptor.forClass(ItemVersionData.class);
        verify(versionManager).create(eq(context), eq(ITEM_ID), eq(VERSION_ID), eq(baseVersionId),
                captor.capture());
        Assert.assertEquals(captor.getValue().getInfo().getName(), "preallocated-version");
    }

    @Test
    public void testCreateWithGivenVersionIdFailureIsMappedToItemVersionCreateError() {
        when(versionManager.create(eq(context), eq(ITEM_ID), eq(VERSION_ID), eq(REVISION_ID),
                any(ItemVersionData.class))).thenThrow(AdaptorTestSupport
                .failure(ErrorCode.MD_ITEM_VERSION_CREATE, Module.ZSTM, "already exists"));

        Response<Id> response = adaptor.create(context, ITEM_ID, VERSION_ID, REVISION_ID,
                itemVersionData("preallocated-version"));

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_CREATE);
    }

    @Test
    public void testCreateWithGivenVersionIdMapsUnexpectedExceptionToSystemError() {
        when(versionManager.create(eq(context), eq(ITEM_ID), eq(VERSION_ID), eq(REVISION_ID),
                any(ItemVersionData.class))).thenThrow(new IllegalStateException("node down"));

        Response<Id> response = adaptor.create(context, ITEM_ID, VERSION_ID, REVISION_ID,
                itemVersionData("preallocated-version"));

        Assert.assertFalse(response.isSuccessful());
        Assert.assertEquals(response.getReturnCode().getMessage(), "node down");
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.SYSTEM_ERROR);
    }

    @Test
    public void testUpdatePassesDataToManager() {
        Response<?> response =
                adaptor.update(context, ITEM_ID, VERSION_ID, itemVersionData("renamed-version"));

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<ItemVersionData> captor = ArgumentCaptor.forClass(ItemVersionData.class);
        verify(versionManager).update(eq(context), eq(ITEM_ID), eq(VERSION_ID), captor.capture());
        Assert.assertEquals(captor.getValue().getInfo().getName(), "renamed-version");
        Assert.assertEquals(captor.getValue().getInfo().getProperty("renamed-version-property"),
                "renamed-version-value");
    }

    @Test
    public void testUpdateFailureIsMappedToItemVersionUpdateError() {
        doThrow(AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSION_UPDATE, Module.ZSTM, "locked"))
                .when(versionManager)
                .update(eq(context), eq(ITEM_ID), eq(VERSION_ID), any(ItemVersionData.class));

        Response<?> response =
                adaptor.update(context, ITEM_ID, VERSION_ID, itemVersionData("renamed-version"));

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_UPDATE);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ITEM_VERSION_UPDATE);
    }

    @Test
    public void testDeletePassesIdsToManager() {
        Response<?> response = adaptor.delete(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        verify(versionManager).delete(context, ITEM_ID, VERSION_ID);
    }

    @Test
    public void testDeleteFailureIsMappedToItemVersionDeleteError() {
        doThrow(AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSION_DELETE, Module.ZSTM, "in use"))
                .when(versionManager).delete(context, ITEM_ID, VERSION_ID);

        Response<?> response = adaptor.delete(context, ITEM_ID, VERSION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_DELETE);
    }

    @Test
    public void testGetStatusReturnsManagerStatus() {
        when(versionManager.getStatus(context, ITEM_ID, VERSION_ID))
                .thenReturn(new ItemVersionStatus(SynchronizationStatus.OUT_OF_SYNC, true));

        Response<ItemVersionStatus> response = adaptor.getStatus(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getSynchronizationStatus(),
                SynchronizationStatus.OUT_OF_SYNC);
        Assert.assertTrue(response.getValue().isDirty());
    }

    @Test
    public void testGetStatusFailureIsMappedToItemVersionGetStatusError() {
        when(versionManager.getStatus(context, ITEM_ID, VERSION_ID)).thenThrow(AdaptorTestSupport
                .failure(ErrorCode.MD_ITEM_VERSION_GET_STATUS, Module.ZSTM, "no state"));

        Response<ItemVersionStatus> response = adaptor.getStatus(context, ITEM_ID, VERSION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_GET_STATUS);
    }

    @Test
    public void testTagPassesChangeIdAndTagToManager() {
        Tag tag = new Tag("release-1", "first release");

        Response<?> response = adaptor.tag(context, ITEM_ID, VERSION_ID, REVISION_ID, tag);

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(versionManager).tag(eq(context), eq(ITEM_ID), eq(VERSION_ID), eq(REVISION_ID),
                captor.capture());
        Assert.assertEquals(captor.getValue().getName(), "release-1");
        Assert.assertEquals(captor.getValue().getDescription(), "first release");
    }

    @Test
    public void testTagFailureIsMappedToItemVersionTagError() {
        doThrow(AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSION_TAG, Module.ZSTM, "duplicate"))
                .when(versionManager)
                .tag(eq(context), eq(ITEM_ID), eq(VERSION_ID), eq(REVISION_ID), any(Tag.class));

        Response<?> response = adaptor.tag(context, ITEM_ID, VERSION_ID, REVISION_ID,
                new Tag("release-1", "first release"));

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_TAG);
    }

    @Test
    public void testPublishPassesMessageToManager() {
        Response<?> response = adaptor.publish(context, ITEM_ID, VERSION_ID, "publishing");

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(versionManager).publish(eq(context), eq(ITEM_ID), eq(VERSION_ID), captor.capture());
        Assert.assertEquals(captor.getValue(), "publishing");
    }

    @Test
    public void testPublishFailureIsMappedToItemVersionPublishError() {
        doThrow(AdaptorTestSupport
                .failure(ErrorCode.MD_ITEM_VERSION_PUBLISH, Module.ZSTM, "nothing to publish"))
                .when(versionManager)
                .publish(eq(context), eq(ITEM_ID), eq(VERSION_ID), anyString());

        Response<?> response = adaptor.publish(context, ITEM_ID, VERSION_ID, "publishing");

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_PUBLISH);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ITEM_VERSION_PUBLISH);
    }

    @Test
    public void testSyncConvertsCoreMergeResult() {
        when(versionManager.sync(context, ITEM_ID, VERSION_ID)).thenReturn(coreMergeResult());

        Response<MergeResult> response = adaptor.sync(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        assertConvertedMergeResult(response.getValue());
        Assert.assertFalse(response.getValue().isSuccess());
    }

    @Test
    public void testSyncConvertsCoreMergeResultWithoutChangeAndConflict() {
        when(versionManager.sync(context, ITEM_ID, VERSION_ID)).thenReturn(new CoreMergeResult());

        Response<MergeResult> response = adaptor.sync(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNull(response.getValue().getChange());
        Assert.assertNull(response.getValue().getConflict());
        Assert.assertTrue(response.getValue().isSuccess());
    }

    @Test
    public void testSyncFailureIsMappedToItemVersionSyncError() {
        when(versionManager.sync(context, ITEM_ID, VERSION_ID)).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSION_SYNC, Module.ZSTM, "diverged"));

        Response<MergeResult> response = adaptor.sync(context, ITEM_ID, VERSION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_SYNC);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ITEM_VERSION_SYNC);
    }

    @Test
    public void testForceSyncConvertsCoreMergeResult() {
        when(versionManager.forceSync(context, ITEM_ID, VERSION_ID)).thenReturn(coreMergeResult());

        Response<MergeResult> response = adaptor.forceSync(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        assertConvertedMergeResult(response.getValue());
        verify(versionManager).forceSync(context, ITEM_ID, VERSION_ID);
    }

    @Test
    public void testForceSyncFailureIsMappedToItemVersionForceSyncError() {
        when(versionManager.forceSync(context, ITEM_ID, VERSION_ID)).thenThrow(AdaptorTestSupport
                .failure(ErrorCode.MD_ITEM_VERSION_FORCE_SYNC, Module.ZSTM, "nothing to sync"));

        Response<MergeResult> response = adaptor.forceSync(context, ITEM_ID, VERSION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_FORCE_SYNC);
    }

    @Test
    public void testMergePassesSourceVersionIdAndConvertsResult() {
        Id sourceVersionId = new Id("source-version");
        when(versionManager.merge(context, ITEM_ID, VERSION_ID, sourceVersionId))
                .thenReturn(coreMergeResult());

        Response<MergeResult> response =
                adaptor.merge(context, ITEM_ID, VERSION_ID, sourceVersionId);

        Assert.assertTrue(response.isSuccessful());
        assertConvertedMergeResult(response.getValue());
        verify(versionManager).merge(context, ITEM_ID, VERSION_ID, sourceVersionId);
    }

    @Test
    public void testMergeFailureIsMappedToItemVersionMergeError() {
        Id sourceVersionId = new Id("source-version");
        when(versionManager.merge(context, ITEM_ID, VERSION_ID, sourceVersionId))
                .thenThrow(AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSION_MERGE, Module.ZSTM,
                        "unmergeable"));

        Response<MergeResult> response =
                adaptor.merge(context, ITEM_ID, VERSION_ID, sourceVersionId);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_MERGE);
    }

    @Test
    public void testListRevisionsReturnsManagerRevisions() {
        ItemVersionRevisions revisions = new ItemVersionRevisions();
        revisions.addChange(revision("revision-1", "first"));
        revisions.addChange(revision("revision-2", "second"));
        when(versionManager.listRevisions(context, ITEM_ID, VERSION_ID)).thenReturn(revisions);

        Response<ItemVersionRevisions> response =
                adaptor.listRevisions(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getItemVersionRevisions().size(), 2);
        Assert.assertEquals(
                response.getValue().getItemVersionRevisions().get(0).getRevisionId().getValue(),
                "revision-1");
    }

    @Test
    public void testListRevisionsFailureIsMappedToItemVersionRevisionsError() {
        when(versionManager.listRevisions(context, ITEM_ID, VERSION_ID)).thenThrow(AdaptorTestSupport
                .failure(ErrorCode.MD_ITEM_VERSION_REVISIONS, Module.ZSTM, "not published"));

        Response<ItemVersionRevisions> response =
                adaptor.listRevisions(context, ITEM_ID, VERSION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_REVISIONS);
    }

    @Test
    public void testGetRevisionReturnsManagerRevision() {
        when(versionManager.getRevision(context, ITEM_ID, VERSION_ID, REVISION_ID))
                .thenReturn(revision("revision-1", "first"));

        Response<Revision> response =
                adaptor.getRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getRevisionId().getValue(), "revision-1");
        Assert.assertEquals(response.getValue().getMessage(), "first");
        Assert.assertEquals(response.getValue().getUser(), "first-user");
    }

    @Test
    public void testGetRevisionFailureIsMappedToItemVersionRevisionError() {
        when(versionManager.getRevision(context, ITEM_ID, VERSION_ID, REVISION_ID))
                .thenThrow(AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSION_REVISION,
                        Module.ZSTM, "unknown revision"));

        Response<Revision> response =
                adaptor.getRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_REVISION);
    }

    @Test
    public void testResetRevisionPassesRevisionIdToManager() {
        Response<?> response = adaptor.resetRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertTrue(response.isSuccessful());
        verify(versionManager).resetRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);
    }

    @Test
    public void testResetRevisionFailureIsMappedToItemVersionResetRevisionError() {
        doThrow(AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSION_RESET_REVISION, Module.ZSTM,
                "not on revision")).when(versionManager)
                .resetRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);

        Response<?> response = adaptor.resetRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_RESET_REVISION);
    }

    @Test
    public void testRevertRevisionPassesRevisionIdToManager() {
        Response<?> response = adaptor.revertRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertTrue(response.isSuccessful());
        verify(versionManager).revertRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);
    }

    @Test
    public void testRevertRevisionFailureIsMappedToItemVersionRevertRevisionError() {
        doThrow(AdaptorTestSupport.failure(ErrorCode.MD_ITEM_VERSION_REVERT_REVISION, Module.ZSTM,
                "revision is current")).when(versionManager)
                .revertRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);

        Response<?> response = adaptor.revertRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_REVERT_REVISION);
    }

    @Test
    public void testGetConflictConvertsCoreItemVersionConflict() {
        CoreItemVersionConflict coreConflict = new CoreItemVersionConflict();
        coreConflict.setVersionDataConflict(versionDataConflict());
        coreConflict.addElementConflict(coreElementConflictInfo());
        when(versionManager.getConflict(context, ITEM_ID, VERSION_ID)).thenReturn(coreConflict);

        Response<ItemVersionConflict> response =
                adaptor.getConflict(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        ItemVersionConflict conflict = response.getValue();
        Assert.assertEquals(conflict.getVersionDataConflict().getLocalData().getInfo().getName(),
                "local-version-data");
        Assert.assertEquals(conflict.getVersionDataConflict().getRemoteData().getInfo().getName(),
                "remote-version-data");
        Assert.assertEquals(conflict.getElementConflictInfos().size(), 1);
        ElementConflictInfo conflictInfo = conflict.getElementConflictInfos().iterator().next();
        Assert.assertEquals(conflictInfo.getLocalElementInfo().getId().getValue(), "local-info");
        Assert.assertEquals(conflictInfo.getLocalElementInfo().getInfo().getName(),
                "local-info-name");
        Assert.assertEquals(conflictInfo.getRemoteElementInfo().getId().getValue(), "remote-info");
    }

    @Test
    public void testGetConflictConvertsConflictWithoutElementConflicts() {
        CoreItemVersionConflict coreConflict = new CoreItemVersionConflict();
        coreConflict.setVersionDataConflict(versionDataConflict());
        when(versionManager.getConflict(context, ITEM_ID, VERSION_ID)).thenReturn(coreConflict);

        Response<ItemVersionConflict> response =
                adaptor.getConflict(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertTrue(response.getValue().getElementConflictInfos().isEmpty());
        Assert.assertNotNull(response.getValue().getVersionDataConflict());
    }

    @Test
    public void testGetConflictFailureIsMappedToItemVersionGetConflictError() {
        when(versionManager.getConflict(context, ITEM_ID, VERSION_ID)).thenThrow(AdaptorTestSupport
                .failure(ErrorCode.MD_ITEM_VERSION_GET_CONFLICT, Module.ZSTM, "not merging"));

        Response<ItemVersionConflict> response = adaptor.getConflict(context, ITEM_ID, VERSION_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_VERSION_GET_CONFLICT);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ITEM_VERSION_GET_CONFLICT);
    }

    private static void assertConvertedMergeResult(MergeResult mergeResult) {
        Assert.assertEquals(mergeResult.getChange().getChangedVersion().getAction(), Action.UPDATE);
        Assert.assertEquals(mergeResult.getChange().getChangedVersion().getItemVersion().getId()
                .getValue(), "changed-version");
        Assert.assertEquals(mergeResult.getChange().getChangedElements().size(), 1);
        Element changedElement = mergeResult.getChange().getChangedElements().iterator().next();
        Assert.assertEquals(changedElement.getElementId().getValue(), "changed-element");
        Assert.assertEquals(changedElement.getInfo().getName(), "changed-element-info");
        Assert.assertEquals(changedElement.getAction(), Action.CREATE);
        Assert.assertEquals(AdaptorTestSupport.text(changedElement.getData()),
                "changed-element-data");

        Assert.assertEquals(
                mergeResult.getConflict().getVersionDataConflict().getLocalData().getInfo().getName(),
                "local-version-data");
        Assert.assertEquals(mergeResult.getConflict().getElementConflicts().size(), 1);
        ElementConflict elementConflict =
                mergeResult.getConflict().getElementConflicts().iterator().next();
        Assert.assertEquals(elementConflict.getLocalElement().getElementId().getValue(),
                "local-element");
        Assert.assertEquals(elementConflict.getRemoteElement().getElementId().getValue(),
                "remote-element");
    }

    private static CoreMergeResult coreMergeResult() {
        CoreMergeChange change = new CoreMergeChange();
        change.setChangedVersion(itemVersionChange("changed-version"));
        change.setChangedElements(
                Collections.singletonList(coreElement("changed-element", Action.CREATE)));

        CoreElementConflict coreElementConflict = new CoreElementConflict();
        coreElementConflict.setLocalElement(coreElement("local-element", Action.UPDATE));
        coreElementConflict.setRemoteElement(coreElement("remote-element", Action.UPDATE));
        CoreMergeConflict conflict = new CoreMergeConflict();
        conflict.setVersionDataConflict(versionDataConflict());
        conflict.setElementConflicts(Collections.singletonList(coreElementConflict));

        CoreMergeResult coreMergeResult = new CoreMergeResult();
        coreMergeResult.setChange(change);
        coreMergeResult.setConflict(conflict);
        return coreMergeResult;
    }

    private static CoreElement coreElement(String elementId, Action action) {
        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id(elementId));
        coreElement.setAction(action);
        coreElement.setInfo(AdaptorTestSupport.info(elementId + "-info"));
        coreElement.setRelations(
                Collections.singletonList(AdaptorTestSupport.relation(elementId + "-relation")));
        coreElement.setData(AdaptorTestSupport.stream(elementId + "-data"));
        return coreElement;
    }

    private static CoreElementConflictInfo coreElementConflictInfo() {
        CoreElementConflictInfo conflictInfo = new CoreElementConflictInfo();
        conflictInfo.setLocalCoreElementInfo(coreElementInfo("local-info"));
        conflictInfo.setRemoteCoreElementInfo(coreElementInfo("remote-info"));
        return conflictInfo;
    }

    private static CoreElementInfo coreElementInfo(String elementId) {
        CoreElementInfo coreElementInfo = new CoreElementInfo();
        coreElementInfo.setId(new Id(elementId));
        coreElementInfo.setInfo(AdaptorTestSupport.info(elementId + "-name"));
        coreElementInfo.setSubElements(Collections.<CoreElementInfo>emptyList());
        return coreElementInfo;
    }

    private static ItemVersionDataConflict versionDataConflict() {
        ItemVersionDataConflict conflict = new ItemVersionDataConflict();
        conflict.setLocalData(itemVersionData("local-version-data"));
        conflict.setRemoteData(itemVersionData("remote-version-data"));
        return conflict;
    }

    private static ItemVersionChange itemVersionChange(String versionId) {
        ItemVersionChange change = new ItemVersionChange();
        change.setAction(Action.UPDATE);
        change.setItemVersion(itemVersion(versionId));
        return change;
    }

    private static ItemVersion itemVersion(String versionId) {
        ItemVersion itemVersion = new ItemVersion();
        itemVersion.setId(new Id(versionId));
        itemVersion.setBaseId(new Id(versionId + "-base"));
        itemVersion.setRevisionId(new Id(versionId + "-revision"));
        itemVersion.setData(itemVersionData(versionId + "-version-info"));
        itemVersion.setCreationTime(new Date(0L));
        return itemVersion;
    }

    private static ItemVersionData itemVersionData(String name) {
        ItemVersionData data = new ItemVersionData();
        data.setInfo(AdaptorTestSupport.info(name));
        data.setRelations(Collections.singletonList(AdaptorTestSupport.relation(name + "-relation")));
        return data;
    }

    private static Revision revision(String revisionId, String message) {
        Revision revision = new Revision();
        revision.setRevisionId(new Id(revisionId));
        revision.setMessage(message);
        revision.setUser(message + "-user");
        revision.setTime(new Date(0L));
        return revision;
    }
}
