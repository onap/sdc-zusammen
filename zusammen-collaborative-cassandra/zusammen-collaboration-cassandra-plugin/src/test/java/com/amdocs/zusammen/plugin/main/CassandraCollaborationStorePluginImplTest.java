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

package com.amdocs.zusammen.plugin.main;

import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.commons.health.data.HealthStatus;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionStatus;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.datatypes.item.Resolution;
import com.amdocs.zusammen.datatypes.item.SynchronizationStatus;
import com.amdocs.zusammen.datatypes.itemversion.ItemVersionRevisions;
import com.amdocs.zusammen.datatypes.itemversion.Revision;
import com.amdocs.zusammen.datatypes.itemversion.Tag;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.plugin.ZusammenPluginConstants;
import com.amdocs.zusammen.plugin.collaboration.CommitStagingService;
import com.amdocs.zusammen.plugin.collaboration.DiscardChangesService;
import com.amdocs.zusammen.plugin.collaboration.ElementPrivateStore;
import com.amdocs.zusammen.plugin.collaboration.ElementPublicStore;
import com.amdocs.zusammen.plugin.collaboration.ElementStageStore;
import com.amdocs.zusammen.plugin.collaboration.PublishService;
import com.amdocs.zusammen.plugin.collaboration.RevertService;
import com.amdocs.zusammen.plugin.collaboration.SyncService;
import com.amdocs.zusammen.plugin.collaboration.TestUtils;
import com.amdocs.zusammen.plugin.collaboration.VersionPrivateStore;
import com.amdocs.zusammen.plugin.collaboration.VersionPublicStore;
import com.amdocs.zusammen.plugin.collaboration.VersionStageStore;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.StageEntity;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.dao.types.VersionEntity;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationItemVersionConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeResult;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationPublishResult;
import com.amdocs.zusammen.sdk.types.ElementConflictDescriptor;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CassandraCollaborationStorePluginImplTest {

    private static final String USER = "CassandraCollaborationStorePluginImplTest_user";
    private static final String TENANT = "CassandraCollaborationStorePluginImplTest_tenant";

    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id BASE_VERSION_ID = new Id("base-version-3");
    private static final Id OTHER_VERSION_ID = new Id("other-version-4");
    private static final Id REVISION_ID = new Id("revision-5");
    private static final Id ELEMENT_ID = new Id("element-6");
    private static final Id OTHER_ELEMENT_ID = new Id("other-element-7");
    private static final Id PARENT_ID = new Id("parent-8");

    private static final SessionContext context =
            TestUtils.createSessionContext(new UserInfo(USER), TENANT);

    @Mock
    private VersionPrivateStore versionPrivateStore;
    @Mock
    private VersionPublicStore versionPublicStore;
    @Mock
    private VersionStageStore versionStageStore;
    @Mock
    private ElementPrivateStore elementPrivateStore;
    @Mock
    private ElementPublicStore elementPublicStore;
    @Mock
    private ElementStageStore elementStageStore;
    @Mock
    private PublishService publishService;
    @Mock
    private DiscardChangesService discardChangesService;
    @Mock
    private SyncService syncService;
    @Mock
    private CommitStagingService commitStagingService;
    @Mock
    private RevertService revertService;

    @InjectMocks
    private CassandraCollaborationStorePluginImpl plugin;

    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    /**
     * The subject hard-instantiates all eleven collaborators as private fields, so they are only
     * replaced by reflection. A {@code @Mock} whose type matches no field is skipped silently, which
     * would leave the real collaborator - and its Cassandra calls - in place.
     */
    @Test
    public void testAllCollaboratorsAreReplacedByMocks() throws Exception {
        List<String> fieldNames = Arrays.asList("versionPrivateStore", "versionPublicStore",
                "versionStageStore", "elementPrivateStore", "elementPublicStore", "elementStageStore",
                "publishService", "discardChangesService", "syncService", "commitStagingService",
                "revertService");

        for (String fieldName : fieldNames) {
            Field field = CassandraCollaborationStorePluginImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Assert.assertTrue(Mockito.mockingDetails(field.get(plugin)).isMock(),
                    fieldName + " was not replaced by a mock");
        }
    }

    @Test
    public void testCreateItemIsDoneByTheStateStore() {
        Response<Void> response = plugin.createItem(context, ITEM_ID, TestUtils.createInfo("item"));

        Assert.assertTrue(response.isSuccessful());
        Mockito.verifyNoInteractions(versionPrivateStore, versionPublicStore, elementPrivateStore,
                elementPublicStore);
    }

    @Test
    public void testDeleteItemDeletesPrivateAndPublicVersions() {
        VersionEntity publicVersion = new VersionEntity(OTHER_VERSION_ID);
        when(versionPrivateStore.list(context, ITEM_ID))
                .thenReturn(Collections.singletonList(new VersionEntity(VERSION_ID)));
        when(versionPublicStore.list(context, ITEM_ID))
                .thenReturn(Collections.singletonList(publicVersion));

        Response<Void> response = plugin.deleteItem(context, ITEM_ID);

        Assert.assertTrue(response.isSuccessful());
        verify(elementPrivateStore).cleanAll(context, new ElementContext(ITEM_ID, VERSION_ID));
        verify(elementStageStore).deleteAll(context, new ElementContext(ITEM_ID, VERSION_ID));
        Assert.assertEquals(deletedPrivateVersion().getId(), VERSION_ID);

        verify(elementPublicStore).cleanAll(context, new ElementContext(ITEM_ID, OTHER_VERSION_ID));
        verify(versionPublicStore).delete(context, ITEM_ID, publicVersion);
    }

    @Test
    public void testDeleteItemWithoutVersions() {
        when(versionPrivateStore.list(context, ITEM_ID)).thenReturn(Collections.emptyList());
        when(versionPublicStore.list(context, ITEM_ID)).thenReturn(Collections.emptyList());

        Assert.assertTrue(plugin.deleteItem(context, ITEM_ID).isSuccessful());

        Mockito.verifyNoInteractions(elementPrivateStore, elementPublicStore, elementStageStore);
    }

    @Test
    public void testCreateItemVersionWithoutBaseVersion() {
        ItemVersionData itemVersionData = itemVersionData("created-version");

        Response<Void> response =
                plugin.createItemVersion(context, ITEM_ID, null, VERSION_ID, itemVersionData);

        Assert.assertTrue(response.isSuccessful());
        VersionEntity createdVersion = createdPrivateVersion();
        Assert.assertEquals(createdVersion.getId(), VERSION_ID);
        Assert.assertNull(createdVersion.getBaseId());
        Assert.assertNotNull(createdVersion.getCreationTime());
        Assert.assertEquals(createdVersion.getModificationTime(), createdVersion.getCreationTime());

        ArgumentCaptor<ElementEntity> versionDataCaptor =
                ArgumentCaptor.forClass(ElementEntity.class);
        verify(elementPrivateStore).create(eq(context), eq(new ElementContext(ITEM_ID, VERSION_ID)),
                versionDataCaptor.capture());
        Assert.assertEquals(versionDataCaptor.getValue().getId(),
                ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID);
        Assert.assertSame(versionDataCaptor.getValue().getInfo(), itemVersionData.getInfo());
        Assert.assertSame(versionDataCaptor.getValue().getRelations(),
                itemVersionData.getRelations());
        verify(elementPrivateStore, never()).update(any(SessionContext.class),
                any(ElementContext.class), any(ElementEntity.class));
        Mockito.verifyNoInteractions(elementPublicStore);
    }

    @Test
    public void testCreateItemVersionFromBaseVersionCopiesItsElementsIntoTheNewVersion() {
        ElementContext baseContext = new ElementContext(ITEM_ID, BASE_VERSION_ID);
        ElementContext newContext = new ElementContext(ITEM_ID, VERSION_ID);
        ElementEntity baseElement = elementEntity(ELEMENT_ID, PARENT_ID, "base-element");
        Date publishTime = new Date(7000L);
        when(elementPrivateStore.listIds(context, baseContext))
                .thenReturn(Collections.singletonMap(ELEMENT_ID, PARENT_ID));
        when(elementPrivateStore.get(context, baseContext, ELEMENT_ID))
                .thenReturn(Optional.of(baseElement));
        when(elementPrivateStore.getSynchronizationState(context, baseContext, ELEMENT_ID))
                .thenReturn(Optional
                        .of(new SynchronizationStateEntity(ELEMENT_ID, Id.ZERO, publishTime, false)));

        Response<Void> response = plugin
                .createItemVersion(context, ITEM_ID, BASE_VERSION_ID, VERSION_ID,
                        itemVersionData("created-from-base"));

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(createdPrivateVersion().getBaseId(), BASE_VERSION_ID);
        verify(elementPrivateStore)
                .commitStagedCreate(context, newContext, baseElement, publishTime);
        verify(elementPrivateStore)
                .update(eq(context), eq(newContext), any(ElementEntity.class));
        verify(elementPrivateStore, never()).create(any(SessionContext.class),
                any(ElementContext.class), any(ElementEntity.class));
    }

    @Test
    public void testUpdateItemVersionTouchesTheVersionModificationTime() {
        ItemVersionData itemVersionData = itemVersionData("updated-version");
        when(elementPrivateStore.update(eq(context), eq(new ElementContext(ITEM_ID, VERSION_ID)),
                any(ElementEntity.class))).thenReturn(true);

        Response<Void> response =
                plugin.updateItemVersion(context, ITEM_ID, VERSION_ID, itemVersionData);

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<VersionEntity> versionCaptor = ArgumentCaptor.forClass(VersionEntity.class);
        verify(versionPrivateStore).update(eq(context), eq(ITEM_ID), versionCaptor.capture());
        Assert.assertEquals(versionCaptor.getValue().getId(), VERSION_ID);
        Assert.assertNotNull(versionCaptor.getValue().getModificationTime());
    }

    @Test
    public void testUpdateItemVersionWhenVersionDataUnchanged() {
        when(elementPrivateStore.update(eq(context), any(ElementContext.class),
                any(ElementEntity.class))).thenReturn(false);

        Assert.assertTrue(plugin
                .updateItemVersion(context, ITEM_ID, VERSION_ID, itemVersionData("unchanged"))
                .isSuccessful());

        Mockito.verifyNoInteractions(versionPrivateStore);
    }

    @Test
    public void testDeleteItemVersionClearsStageAndPrivate() {
        Response<Void> response = plugin.deleteItemVersion(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        verify(elementStageStore).deleteAll(context, elementContext);
        verify(elementPrivateStore).cleanAll(context, elementContext);
        Assert.assertEquals(deletedPrivateVersion().getId(), VERSION_ID);
        ArgumentCaptor<VersionEntity> stageCaptor = ArgumentCaptor.forClass(VersionEntity.class);
        verify(versionStageStore).delete(eq(context), eq(ITEM_ID), stageCaptor.capture());
        Assert.assertEquals(stageCaptor.getValue().getId(), VERSION_ID);
        Mockito.verifyNoInteractions(elementPublicStore, versionPublicStore);
    }

    @Test
    public void testGetItemVersionStatusWhenStagedIsMerging() {
        when(versionStageStore.get(eq(context), eq(ITEM_ID), any(VersionEntity.class)))
                .thenReturn(Optional
                        .of(new StageEntity<>(new VersionEntity(VERSION_ID), new Date(8000L))));

        Response<ItemVersionStatus> response =
                plugin.getItemVersionStatus(context, ITEM_ID, VERSION_ID);

        Assert.assertEquals(response.getValue().getSynchronizationStatus(),
                SynchronizationStatus.MERGING);
        Assert.assertTrue(response.getValue().isDirty());
        Mockito.verifyNoInteractions(versionPublicStore, versionPrivateStore);
    }

    @Test
    public void testGetItemVersionStatusWhenNotPublishedIsUpToDateAndDirty() {
        stageIsEmpty();
        when(versionPublicStore.getSynchronizationState(context, ITEM_ID, VERSION_ID, null))
                .thenReturn(Optional.empty());

        Response<ItemVersionStatus> response =
                plugin.getItemVersionStatus(context, ITEM_ID, VERSION_ID);

        Assert.assertEquals(response.getValue().getSynchronizationStatus(),
                SynchronizationStatus.UP_TO_DATE);
        Assert.assertTrue(response.getValue().isDirty());
        Mockito.verifyNoInteractions(versionPrivateStore);
    }

    @Test
    public void testGetItemVersionStatusWhenPublishTimesMatchIsUpToDate() {
        stageIsEmpty();
        publicVersionPublishedAt(new Date(9000L));
        when(versionPrivateStore.getSynchronizationState(context, ITEM_ID, VERSION_ID))
                .thenReturn(Optional.of(
                        new SynchronizationStateEntity(VERSION_ID, Id.ZERO, new Date(9000L), true)));

        Response<ItemVersionStatus> response =
                plugin.getItemVersionStatus(context, ITEM_ID, VERSION_ID);

        Assert.assertEquals(response.getValue().getSynchronizationStatus(),
                SynchronizationStatus.UP_TO_DATE);
        Assert.assertTrue(response.getValue().isDirty());
    }

    @Test
    public void testGetItemVersionStatusWhenPublishTimesDifferIsOutOfSync() {
        stageIsEmpty();
        publicVersionPublishedAt(new Date(9000L));
        when(versionPrivateStore.getSynchronizationState(context, ITEM_ID, VERSION_ID))
                .thenReturn(Optional.of(
                        new SynchronizationStateEntity(VERSION_ID, Id.ZERO, new Date(10000L), false)));

        Response<ItemVersionStatus> response =
                plugin.getItemVersionStatus(context, ITEM_ID, VERSION_ID);

        Assert.assertEquals(response.getValue().getSynchronizationStatus(),
                SynchronizationStatus.OUT_OF_SYNC);
        Assert.assertFalse(response.getValue().isDirty());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetItemVersionStatusFailsWhenPrivateVersionIsMissing() {
        stageIsEmpty();
        publicVersionPublishedAt(new Date(9000L));
        when(versionPrivateStore.getSynchronizationState(context, ITEM_ID, VERSION_ID))
                .thenReturn(Optional.empty());

        plugin.getItemVersionStatus(context, ITEM_ID, VERSION_ID);
    }

    @Test
    public void testTagItemVersionReportsSuccess() {
        Response<Void> response = plugin.tagItemVersion(context, ITEM_ID, VERSION_ID, REVISION_ID,
                new Tag("tag-name", "tag-description"));

        Assert.assertTrue(response.isSuccessful());
    }

    @Test
    public void testPublishItemVersion() {
        CollaborationPublishResult publishResult = new CollaborationPublishResult();
        when(publishService.publish(context, ITEM_ID, VERSION_ID, "publish message"))
                .thenReturn(publishResult);

        Response<CollaborationPublishResult> response =
                plugin.publishItemVersion(context, ITEM_ID, VERSION_ID, "publish message");

        Assert.assertTrue(response.isSuccessful());
        Assert.assertSame(response.getValue(), publishResult);
    }

    @Test
    public void testPublishItemVersionWrapsAZusammenException() {
        ReturnCode cause = new ReturnCode(4242, Module.ZCSP, "publish failed", null);
        when(publishService.publish(context, ITEM_ID, VERSION_ID, null))
                .thenThrow(new ZusammenException(cause));

        Response<CollaborationPublishResult> response =
                plugin.publishItemVersion(context, ITEM_ID, VERSION_ID, null);

        Assert.assertFalse(response.isSuccessful());
        Assert.assertNull(response.getValue());
        Assert.assertSame(response.getReturnCode().getReturnCode(), cause);
    }

    @Test
    public void testSyncItemVersionCommitsTheStagingItCreated() {
        CollaborationMergeResult mergeResult = new CollaborationMergeResult();
        when(syncService.sync(context, ITEM_ID, VERSION_ID)).thenReturn(mergeResult);

        Response<CollaborationMergeResult> response =
                plugin.syncItemVersion(context, ITEM_ID, VERSION_ID);

        Assert.assertSame(response.getValue(), mergeResult);
        InOrder inOrder = Mockito.inOrder(syncService, commitStagingService);
        inOrder.verify(syncService).sync(context, ITEM_ID, VERSION_ID);
        inOrder.verify(commitStagingService).commitStaging(context, ITEM_ID, VERSION_ID);
    }

    @Test
    public void testForceSyncItemVersionDiscardsChangesBeforeSyncing() {
        CollaborationMergeResult mergeResult = new CollaborationMergeResult();
        when(syncService.sync(context, ITEM_ID, VERSION_ID)).thenReturn(mergeResult);

        Response<CollaborationMergeResult> response =
                plugin.forceSyncItemVersion(context, ITEM_ID, VERSION_ID);

        Assert.assertSame(response.getValue(), mergeResult);
        InOrder inOrder = Mockito.inOrder(discardChangesService, commitStagingService, syncService);
        inOrder.verify(discardChangesService).discardChanges(context, ITEM_ID, VERSION_ID);
        inOrder.verify(commitStagingService).commitStaging(context, ITEM_ID, VERSION_ID);
        inOrder.verify(syncService).sync(context, ITEM_ID, VERSION_ID);
        inOrder.verify(commitStagingService).commitStaging(context, ITEM_ID, VERSION_ID);
        verify(commitStagingService, times(2)).commitStaging(context, ITEM_ID, VERSION_ID);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testMergeItemVersionIsUnsupported() {
        plugin.mergeItemVersion(context, ITEM_ID, VERSION_ID, OTHER_VERSION_ID);
    }

    @Test
    public void testGetItemVersionConflictWithoutConflicts() {
        conflictedDescriptorsAre(Collections.emptyList());

        Response<CollaborationItemVersionConflict> response =
                plugin.getItemVersionConflict(context, ITEM_ID, VERSION_ID);

        Assert.assertNull(response.getValue().getVersionDataConflict());
        Assert.assertTrue(response.getValue().getElementConflictDescriptors().isEmpty());
    }

    @Test
    public void testGetItemVersionConflictOfUpdatedVersionData() {
        ElementEntity remoteVersionData = elementEntity(Id.ZERO, null, "remote-version-data");
        ElementEntity localVersionData = elementEntity(Id.ZERO, null, "local-version-data");
        conflictedDescriptorsAre(Collections.singletonList(
                new StageEntity<>(remoteVersionData, new Date(11000L), Action.UPDATE, true)));
        when(elementPrivateStore.getDescriptor(context, conflictContext(),
                ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID))
                .thenReturn(Optional.of(localVersionData));

        Response<CollaborationItemVersionConflict> response =
                plugin.getItemVersionConflict(context, ITEM_ID, VERSION_ID);

        Assert.assertSame(response.getValue().getVersionDataConflict().getRemoteData().getInfo(),
                remoteVersionData.getInfo());
        Assert.assertSame(response.getValue().getVersionDataConflict().getLocalData().getInfo(),
                localVersionData.getInfo());
        Assert.assertTrue(response.getValue().getElementConflictDescriptors().isEmpty());
    }

    @Test
    public void testGetItemVersionConflictOfCreatedVersionDataHasNoLocalData() {
        conflictedDescriptorsAre(Collections.singletonList(
                new StageEntity<>(elementEntity(Id.ZERO, null, "remote-version-data"),
                        new Date(11000L), Action.CREATE, true)));

        Response<CollaborationItemVersionConflict> response =
                plugin.getItemVersionConflict(context, ITEM_ID, VERSION_ID);

        Assert.assertNotNull(response.getValue().getVersionDataConflict().getRemoteData());
        Assert.assertNull(response.getValue().getVersionDataConflict().getLocalData());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetItemVersionConflictFailsWhenPrivateVersionDataIsMissing() {
        conflictedDescriptorsAre(Collections.singletonList(
                new StageEntity<>(elementEntity(Id.ZERO, null, "remote-version-data"),
                        new Date(11000L), Action.UPDATE, true)));
        when(elementPrivateStore.getDescriptor(context, conflictContext(),
                ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID)).thenReturn(Optional.empty());

        plugin.getItemVersionConflict(context, ITEM_ID, VERSION_ID);
    }

    @Test
    public void testGetItemVersionConflictOfCreatedElement() {
        ElementEntity remoteElement = elementEntity(ELEMENT_ID, PARENT_ID, "remote-element");
        conflictedDescriptorsAre(Collections.singletonList(
                new StageEntity<>(remoteElement, new Date(12000L), Action.CREATE, true)));

        ElementConflictDescriptor conflict =
                onlyElementConflict(plugin.getItemVersionConflict(context, ITEM_ID, VERSION_ID));

        Assert.assertEquals(conflict.getRemoteElementDescriptor().getId(), ELEMENT_ID);
        Assert.assertEquals(conflict.getRemoteElementDescriptor().getItemId(), ITEM_ID);
        Assert.assertEquals(conflict.getRemoteElementDescriptor().getVersionId(), VERSION_ID);
        Assert.assertEquals(conflict.getRemoteElementDescriptor().getNamespace(),
                remoteElement.getNamespace());
        Assert.assertNull(conflict.getLocalElementDescriptor());
    }

    @Test
    public void testGetItemVersionConflictOfUpdatedElement() {
        ElementEntity remoteElement = elementEntity(ELEMENT_ID, PARENT_ID, "remote-element");
        ElementEntity localElement = elementEntity(ELEMENT_ID, PARENT_ID, "local-element");
        conflictedDescriptorsAre(Collections.singletonList(
                new StageEntity<>(remoteElement, new Date(12000L), Action.UPDATE, true)));
        when(elementPrivateStore.getDescriptor(context, conflictContext(), ELEMENT_ID))
                .thenReturn(Optional.of(localElement));

        ElementConflictDescriptor conflict =
                onlyElementConflict(plugin.getItemVersionConflict(context, ITEM_ID, VERSION_ID));

        Assert.assertSame(conflict.getRemoteElementDescriptor().getInfo(),
                remoteElement.getInfo());
        Assert.assertSame(conflict.getLocalElementDescriptor().getInfo(), localElement.getInfo());
    }

    @Test
    public void testGetItemVersionConflictOfElementUpdatedOnPublicAndDeletedOnPrivate() {
        conflictedDescriptorsAre(Collections.singletonList(
                new StageEntity<>(elementEntity(ELEMENT_ID, PARENT_ID, "remote-element"),
                        new Date(12000L), Action.UPDATE, true)));
        when(elementPrivateStore.getDescriptor(context, conflictContext(), ELEMENT_ID))
                .thenReturn(Optional.empty());

        ElementConflictDescriptor conflict =
                onlyElementConflict(plugin.getItemVersionConflict(context, ITEM_ID, VERSION_ID));

        Assert.assertNotNull(conflict.getRemoteElementDescriptor());
        Assert.assertNull(conflict.getLocalElementDescriptor());
    }

    @Test
    public void testGetItemVersionConflictOfDeletedElement() {
        conflictedDescriptorsAre(Collections.singletonList(
                new StageEntity<>(elementEntity(ELEMENT_ID, PARENT_ID, "local-element"),
                        new Date(12000L), Action.DELETE, true)));

        ElementConflictDescriptor conflict =
                onlyElementConflict(plugin.getItemVersionConflict(context, ITEM_ID, VERSION_ID));

        Assert.assertEquals(conflict.getLocalElementDescriptor().getId(), ELEMENT_ID);
        Assert.assertNull(conflict.getRemoteElementDescriptor());
    }

    @Test
    public void testGetItemVersionConflictOfIgnoredElement() {
        conflictedDescriptorsAre(Collections.singletonList(
                new StageEntity<>(elementEntity(ELEMENT_ID, PARENT_ID, "ignored-element"),
                        new Date(12000L), Action.IGNORE, true)));

        ElementConflictDescriptor conflict =
                onlyElementConflict(plugin.getItemVersionConflict(context, ITEM_ID, VERSION_ID));

        Assert.assertNull(conflict.getLocalElementDescriptor());
        Assert.assertNull(conflict.getRemoteElementDescriptor());
    }

    @Test
    public void testListItemVersionRevisions() {
        SynchronizationStateEntity syncState =
                new SynchronizationStateEntity(VERSION_ID, REVISION_ID, new Date(13000L), false);
        syncState.setUser("revision-user");
        syncState.setMessage("revision message");
        when(versionPublicStore.listSynchronizationStates(context, ITEM_ID, VERSION_ID))
                .thenReturn(Collections.singletonList(syncState));

        Response<ItemVersionRevisions> response =
                plugin.listItemVersionRevisions(context, ITEM_ID, VERSION_ID);

        Assert.assertEquals(response.getValue().getItemVersionRevisions().size(), 1);
        Revision revision = response.getValue().getItemVersionRevisions().get(0);
        Assert.assertEquals(revision.getRevisionId(), REVISION_ID);
        Assert.assertEquals(revision.getTime(), new Date(13000L));
        Assert.assertEquals(revision.getUser(), "revision-user");
        Assert.assertEquals(revision.getMessage(), "revision message");
    }

    @Test
    public void testListItemVersionRevisionsWhenNeverPublished() {
        when(versionPublicStore.listSynchronizationStates(context, ITEM_ID, VERSION_ID))
                .thenReturn(Collections.emptyList());

        Response<ItemVersionRevisions> response =
                plugin.listItemVersionRevisions(context, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.getValue().getItemVersionRevisions().isEmpty());
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testGetItemVersionRevisionIsUnsupported() {
        plugin.getItemVersionRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testResetItemVersionRevisionIsUnsupported() {
        plugin.resetItemVersionRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);
    }

    @Test
    public void testRevertItemVersionRevision() {
        publicVersionExistsWithData();

        Response<CollaborationMergeChange> response =
                plugin.revertItemVersionRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertNotNull(response.getValue());
        InOrder inOrder = Mockito.inOrder(discardChangesService, syncService, revertService);
        inOrder.verify(discardChangesService).discardChanges(context, ITEM_ID, VERSION_ID);
        inOrder.verify(syncService).sync(context, ITEM_ID, VERSION_ID);
        inOrder.verify(revertService).revert(context, ITEM_ID, VERSION_ID, REVISION_ID);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRevertItemVersionRevisionFailsWhenVersionIsNotPublic() {
        when(versionPublicStore.get(context, ITEM_ID, VERSION_ID)).thenReturn(Optional.empty());

        plugin.revertItemVersionRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRevertItemVersionRevisionFailsWhenRevisionHasNoVersionData() {
        when(versionPublicStore.get(context, ITEM_ID, VERSION_ID))
                .thenReturn(Optional.of(new VersionEntity(VERSION_ID)));
        when(elementPublicStore.getDescriptor(context, revisionContext(),
                ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID)).thenReturn(Optional.empty());

        plugin.revertItemVersionRevision(context, ITEM_ID, VERSION_ID, REVISION_ID);
    }

    @Test
    public void testCommitElementsIsNotNeededByThisPlugin() {
        Response<Void> response = plugin.commitElements(context, ITEM_ID, VERSION_ID, "message");

        Assert.assertTrue(response.isSuccessful());
        Mockito.verifyNoInteractions(elementPrivateStore, elementPublicStore, commitStagingService);
    }

    @Test
    public void testListElements() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        ElementEntity sub = elementEntity(OTHER_ELEMENT_ID, ELEMENT_ID, "sub-element");
        when(elementPrivateStore.listSubs(context, elementContext, ELEMENT_ID))
                .thenReturn(Collections.singletonList(sub));

        Response<Collection<CollaborationElement>> response =
                plugin.listElements(context, elementContext, namespace("requested-namespace"),
                        ELEMENT_ID);

        Assert.assertEquals(response.getValue().size(), 1);
        CollaborationElement element = response.getValue().iterator().next();
        Assert.assertEquals(element.getId(), OTHER_ELEMENT_ID);
        Assert.assertEquals(element.getParentId(), ELEMENT_ID);
        Assert.assertEquals(element.getItemId(), ITEM_ID);
        Assert.assertEquals(element.getVersionId(), VERSION_ID);
        Assert.assertEquals(element.getNamespace(), sub.getNamespace());
    }

    @Test
    public void testListElementsWithoutSubElements() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        when(elementPrivateStore.listSubs(context, elementContext, ELEMENT_ID))
                .thenReturn(Collections.emptyList());

        Response<Collection<CollaborationElement>> response = plugin
                .listElements(context, elementContext, Namespace.ROOT_NAMESPACE, ELEMENT_ID);

        Assert.assertTrue(response.getValue().isEmpty());
    }

    @Test
    public void testGetElement() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        ElementEntity elementEntity = elementEntity(ELEMENT_ID, PARENT_ID, "element");
        elementEntity.setData(ByteBuffer.wrap("element-data".getBytes(StandardCharsets.UTF_8)));
        when(elementPrivateStore.get(context, elementContext, ELEMENT_ID))
                .thenReturn(Optional.of(elementEntity));

        Response<CollaborationElement> response = plugin
                .getElement(context, elementContext, Namespace.ROOT_NAMESPACE, ELEMENT_ID);

        Assert.assertEquals(response.getValue().getId(), ELEMENT_ID);
        Assert.assertEquals(response.getValue().getNamespace(), elementEntity.getNamespace());
        Assert.assertSame(response.getValue().getInfo(), elementEntity.getInfo());
        Assert.assertNotNull(response.getValue().getData());
    }

    @Test
    public void testGetElementWhenNotFound() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        when(elementPrivateStore.get(context, elementContext, ELEMENT_ID))
                .thenReturn(Optional.empty());

        Response<CollaborationElement> response = plugin
                .getElement(context, elementContext, Namespace.ROOT_NAMESPACE, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNull(response.getValue());
    }

    @Test
    public void testGetElementConflictOfUpdatedElement() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        ElementEntity remoteElement = elementEntity(ELEMENT_ID, PARENT_ID, "remote-element");
        ElementEntity localElement = elementEntity(ELEMENT_ID, PARENT_ID, "local-element");
        conflictedElementIs(elementContext,
                new StageEntity<>(remoteElement, new Date(14000L), Action.UPDATE, true));
        when(elementPrivateStore.get(context, elementContext, ELEMENT_ID))
                .thenReturn(Optional.of(localElement));

        Response<CollaborationElementConflict> response = plugin
                .getElementConflict(context, elementContext, Namespace.ROOT_NAMESPACE, ELEMENT_ID);

        Assert.assertSame(response.getValue().getRemoteElement().getInfo(),
                remoteElement.getInfo());
        Assert.assertSame(response.getValue().getLocalElement().getInfo(), localElement.getInfo());
        Assert.assertEquals(conflictedElementQuery().getId(), ELEMENT_ID);
    }

    @Test
    public void testGetElementConflictOfCreatedElement() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        conflictedElementIs(elementContext,
                new StageEntity<>(elementEntity(ELEMENT_ID, PARENT_ID, "remote-element"),
                        new Date(14000L), Action.CREATE, true));

        Response<CollaborationElementConflict> response = plugin
                .getElementConflict(context, elementContext, Namespace.ROOT_NAMESPACE, ELEMENT_ID);

        Assert.assertNotNull(response.getValue().getRemoteElement());
        Assert.assertNull(response.getValue().getLocalElement());
    }

    @Test
    public void testGetElementConflictOfDeletedElement() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        conflictedElementIs(elementContext,
                new StageEntity<>(elementEntity(ELEMENT_ID, PARENT_ID, "local-element"),
                        new Date(14000L), Action.DELETE, true));

        Response<CollaborationElementConflict> response = plugin
                .getElementConflict(context, elementContext, Namespace.ROOT_NAMESPACE, ELEMENT_ID);

        Assert.assertNull(response.getValue().getRemoteElement());
        Assert.assertEquals(response.getValue().getLocalElement().getId(), ELEMENT_ID);
    }

    @Test
    public void testGetElementConflictOfIgnoredElement() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        conflictedElementIs(elementContext,
                new StageEntity<>(elementEntity(ELEMENT_ID, PARENT_ID, "ignored-element"),
                        new Date(14000L), Action.IGNORE, true));

        Response<CollaborationElementConflict> response = plugin
                .getElementConflict(context, elementContext, Namespace.ROOT_NAMESPACE, ELEMENT_ID);

        Assert.assertNull(response.getValue().getRemoteElement());
        Assert.assertNull(response.getValue().getLocalElement());
    }

    @Test
    public void testGetElementConflictWhenElementIsNotConflicted() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        when(elementStageStore.getConflicted(eq(context), eq(elementContext),
                any(ElementEntity.class))).thenReturn(Optional.empty());

        Response<CollaborationElementConflict> response = plugin
                .getElementConflict(context, elementContext, Namespace.ROOT_NAMESPACE, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNull(response.getValue());
    }

    @Test
    public void testCreateElement() {
        CollaborationElement element = collaborationElement();

        Response<Void> response = plugin.createElement(context, element);

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
        verify(elementPrivateStore).create(eq(context), eq(new ElementContext(ITEM_ID, VERSION_ID)),
                elementCaptor.capture());
        ElementEntity created = elementCaptor.getValue();
        Assert.assertEquals(created.getId(), ELEMENT_ID);
        Assert.assertEquals(created.getParentId(), PARENT_ID);
        Assert.assertEquals(created.getNamespace(), element.getNamespace());
        Assert.assertSame(created.getInfo(), element.getInfo());
        Assert.assertEquals(new String(created.getData().array(), StandardCharsets.UTF_8),
                "created-element-data");
        Assert.assertNotNull(created.getElementHash());
    }

    @Test
    public void testUpdateElement() {
        CollaborationElement element = collaborationElement();

        Response<Void> response = plugin.updateElement(context, element);

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
        verify(elementPrivateStore).update(eq(context), eq(new ElementContext(ITEM_ID, VERSION_ID)),
                elementCaptor.capture());
        Assert.assertEquals(elementCaptor.getValue().getId(), ELEMENT_ID);
        Assert.assertEquals(elementCaptor.getValue().getNamespace(), element.getNamespace());
    }

    @Test
    public void testDeleteElement() {
        CollaborationElement element = collaborationElement();

        Response<Void> response = plugin.deleteElement(context, element);

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
        verify(elementPrivateStore).delete(eq(context), eq(new ElementContext(ITEM_ID, VERSION_ID)),
                elementCaptor.capture());
        Assert.assertEquals(elementCaptor.getValue().getId(), ELEMENT_ID);
    }

    @Test
    public void testResolveElementConflictCommitsTheStaging() {
        CollaborationElement element = collaborationElement();

        Response<CollaborationMergeResult> response =
                plugin.resolveElementConflict(context, element, Resolution.THEIRS);

        Assert.assertNotNull(response.getValue());
        ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
        verify(elementStageStore).resolveConflict(eq(context),
                eq(new ElementContext(ITEM_ID, VERSION_ID)), elementCaptor.capture(),
                eq(Resolution.THEIRS));
        Assert.assertEquals(elementCaptor.getValue().getId(), ELEMENT_ID);
        verify(commitStagingService).commitStaging(context, ITEM_ID, VERSION_ID);
    }

    @Test
    public void testGetItemVersionOfRevision() {
        VersionEntity versionEntity = new VersionEntity(VERSION_ID);
        versionEntity.setBaseId(BASE_VERSION_ID);
        versionEntity.setCreationTime(new Date(15000L));
        versionEntity.setModificationTime(new Date(16000L));
        ElementEntity versionData = elementEntity(Id.ZERO, null, "revision-version-data");
        when(versionPublicStore.get(context, ITEM_ID, VERSION_ID))
                .thenReturn(Optional.of(versionEntity));
        when(elementPublicStore.getDescriptor(context, revisionContext(),
                ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID))
                .thenReturn(Optional.of(versionData));

        Response<ItemVersion> response =
                plugin.getItemVersion(context, Space.PUBLIC, ITEM_ID, VERSION_ID, REVISION_ID);

        ItemVersion itemVersion = response.getValue();
        Assert.assertEquals(itemVersion.getId(), VERSION_ID);
        Assert.assertEquals(itemVersion.getBaseId(), BASE_VERSION_ID);
        Assert.assertEquals(itemVersion.getCreationTime(), new Date(15000L));
        Assert.assertEquals(itemVersion.getModificationTime(), new Date(16000L));
        Assert.assertSame(itemVersion.getData().getInfo(), versionData.getInfo());
    }

    @Test
    public void testGetItemVersionWhenNotPublished() {
        when(versionPublicStore.get(context, ITEM_ID, VERSION_ID)).thenReturn(Optional.empty());

        Response<ItemVersion> response =
                plugin.getItemVersion(context, Space.PUBLIC, ITEM_ID, VERSION_ID, REVISION_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNull(response.getValue());
        Mockito.verifyNoInteractions(elementPublicStore);
    }

    @Test
    public void testCheckHealthWhenSchemaAvailable() {
        when(versionPublicStore.checkHealth(context)).thenReturn(true);

        HealthInfo healthInfo = plugin.checkHealth(context).getValue();

        Assert.assertEquals(healthInfo.getHealthStatus(), HealthStatus.UP);
        Assert.assertEquals(healthInfo.getModuleName(), Module.ZCSP.getDescription());
        Assert.assertEquals(healthInfo.getDescription(), "");
    }

    @Test
    public void testCheckHealthWhenSchemaMissing() {
        when(versionPublicStore.checkHealth(context)).thenReturn(false);

        HealthInfo healthInfo = plugin.checkHealth(context).getValue();

        Assert.assertEquals(healthInfo.getHealthStatus(), HealthStatus.DOWN);
        Assert.assertEquals(healthInfo.getDescription(), "No Schema Available");
    }

    private void stageIsEmpty() {
        when(versionStageStore.get(eq(context), eq(ITEM_ID), any(VersionEntity.class)))
                .thenReturn(Optional.empty());
    }

    private void publicVersionPublishedAt(Date publishTime) {
        when(versionPublicStore.getSynchronizationState(context, ITEM_ID, VERSION_ID, null))
                .thenReturn(Optional.of(
                        new SynchronizationStateEntity(VERSION_ID, REVISION_ID, publishTime, false)));
    }

    private void publicVersionExistsWithData() {
        when(versionPublicStore.get(context, ITEM_ID, VERSION_ID))
                .thenReturn(Optional.of(new VersionEntity(VERSION_ID)));
        when(elementPublicStore.getDescriptor(context, revisionContext(),
                ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID))
                .thenReturn(Optional.of(elementEntity(Id.ZERO, null, "revision-version-data")));
    }

    private void conflictedDescriptorsAre(Collection<StageEntity<ElementEntity>> stagedElements) {
        when(elementStageStore.listConflictedDescriptors(context, conflictContext()))
                .thenReturn(stagedElements);
    }

    private void conflictedElementIs(ElementContext elementContext,
                                     StageEntity<ElementEntity> stagedElement) {
        when(elementStageStore.getConflicted(eq(context), eq(elementContext),
                any(ElementEntity.class))).thenReturn(Optional.of(stagedElement));
    }

    private ElementEntity conflictedElementQuery() {
        ArgumentCaptor<ElementEntity> captor = ArgumentCaptor.forClass(ElementEntity.class);
        verify(elementStageStore)
                .getConflicted(eq(context), any(ElementContext.class), captor.capture());
        return captor.getValue();
    }

    private VersionEntity createdPrivateVersion() {
        ArgumentCaptor<VersionEntity> captor = ArgumentCaptor.forClass(VersionEntity.class);
        verify(versionPrivateStore).create(eq(context), eq(ITEM_ID), captor.capture());
        return captor.getValue();
    }

    private VersionEntity deletedPrivateVersion() {
        ArgumentCaptor<VersionEntity> captor = ArgumentCaptor.forClass(VersionEntity.class);
        verify(versionPrivateStore).delete(eq(context), eq(ITEM_ID), captor.capture());
        return captor.getValue();
    }

    private static ElementConflictDescriptor onlyElementConflict(
            Response<CollaborationItemVersionConflict> response) {
        Collection<ElementConflictDescriptor> conflicts =
                response.getValue().getElementConflictDescriptors();
        Assert.assertEquals(conflicts.size(), 1);
        Assert.assertNull(response.getValue().getVersionDataConflict());
        return conflicts.iterator().next();
    }

    private static ElementContext conflictContext() {
        return new ElementContext(ITEM_ID, VERSION_ID, Id.ZERO);
    }

    private static ElementContext revisionContext() {
        return new ElementContext(ITEM_ID, VERSION_ID, REVISION_ID);
    }

    private static ElementEntity elementEntity(Id id, Id parentId, String name) {
        ElementEntity elementEntity = new ElementEntity(id);
        elementEntity.setParentId(parentId);
        elementEntity.setNamespace(namespace(name + "-namespace"));
        elementEntity.setInfo(TestUtils.createInfo(name));
        elementEntity.setRelations(Arrays.asList(new Relation()));
        return elementEntity;
    }

    private static CollaborationElement collaborationElement() {
        CollaborationElement element = new CollaborationElement(ITEM_ID, VERSION_ID,
                namespace("collaboration-element-namespace"), ELEMENT_ID);
        element.setParentId(PARENT_ID);
        element.setInfo(TestUtils.createInfo("collaboration-element"));
        element.setData(new ByteArrayInputStream(
                "created-element-data".getBytes(StandardCharsets.UTF_8)));
        return element;
    }

    private static Namespace namespace(String value) {
        Namespace namespace = new Namespace();
        namespace.setValue(value);
        return namespace;
    }

    private static ItemVersionData itemVersionData(String name) {
        ItemVersionData itemVersionData = new ItemVersionData();
        Info info = TestUtils.createInfo(name);
        itemVersionData.setInfo(info);
        itemVersionData.setRelations(Arrays.asList(new Relation()));
        return itemVersionData;
    }
}
