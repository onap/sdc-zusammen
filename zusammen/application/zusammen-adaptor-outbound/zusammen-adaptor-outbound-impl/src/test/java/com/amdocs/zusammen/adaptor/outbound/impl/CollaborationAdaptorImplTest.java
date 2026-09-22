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

package com.amdocs.zusammen.adaptor.outbound.impl;

import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.assertReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.coreElement;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.emptySuccess;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.failedResponse;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.info;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.namespace;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.pluginReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.stream;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.successfulResponse;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.text;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.core.api.types.CoreItemVersionConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeChange;
import com.amdocs.zusammen.core.api.types.CoreMergeResult;
import com.amdocs.zusammen.core.api.types.CorePublishResult;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import com.amdocs.zusammen.datatypes.item.ItemVersionStatus;
import com.amdocs.zusammen.datatypes.item.Resolution;
import com.amdocs.zusammen.datatypes.item.SynchronizationStatus;
import com.amdocs.zusammen.datatypes.itemversion.ItemVersionRevisions;
import com.amdocs.zusammen.datatypes.itemversion.Revision;
import com.amdocs.zusammen.datatypes.itemversion.Tag;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.sdk.collaboration.CollaborationStore;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationItemVersionConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeResult;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationPublishResult;
import com.amdocs.zusammen.sdk.types.ElementConflictDescriptor;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CollaborationAdaptorImplTest {

    private static final SessionContext CONTEXT = new SessionContext();
    private static final Id ITEM_ID = new Id("item-id");
    private static final Id VERSION_ID = new Id("version-id");
    private static final Id BASE_VERSION_ID = new Id("base-version-id");
    private static final Id SOURCE_VERSION_ID = new Id("source-version-id");
    private static final Id REVISION_ID = new Id("revision-id");
    private static final Id CHANGE_ID = new Id("change-id");
    private static final Id ELEMENT_ID = new Id("element-id");
    private static final ElementContext ELEMENT_CONTEXT = new ElementContext(ITEM_ID, VERSION_ID);
    private static final ItemVersionData VERSION_DATA = new ItemVersionData();
    private static final Tag TAG = new Tag("tag-name", "tag-description");

    @Mock
    private CollaborationStore collaborationStore;

    private AutoCloseable mocks;
    private CollaborationAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        OutboundTestSupport.installCollaborationStore(collaborationStore);
        adaptor = new CollaborationAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OutboundTestSupport.restoreCollaborationStore();
        mocks.close();
    }

    @Test
    public void testStoreIsResolvedWithTheCallersSessionContext() {
        Mockito.when(collaborationStore.deleteItem(any(), any())).thenReturn(emptySuccess());

        adaptor.deleteItem(CONTEXT, ITEM_ID);

        Assert.assertSame(OutboundTestSupport.collaborationStoreResolutionContext(), CONTEXT);
    }

    @Test(dataProvider = "passThroughMethods")
    public void testStoreResponseIsReturnedUnchangedOnSuccess(Wrapped method) {
        Response<Void> storeResponse = emptySuccess();
        Mockito.when(method.storeCall.call(collaborationStore)).thenReturn(storeResponse);

        Assert.assertSame(method.adaptorCall.call(adaptor), storeResponse,
                method.label + " must hand the plugin response back untouched");
    }

    @Test(dataProvider = "methodsWrappingAnUnsuccessfulResponse")
    public void testUnsuccessfulStoreResponseIsNestedInMiddlewareReturnCode(Wrapped method) {
        ReturnCode pluginReturnCode = pluginReturnCode("plugin refused");
        Mockito.when(method.storeCall.call(collaborationStore))
                .thenReturn(failedResponse(pluginReturnCode));

        try {
            method.adaptorCall.call(adaptor);
            Assert.fail(method.label + " must raise ZusammenException on an unsuccessful response");
        } catch (ZusammenException e) {
            ReturnCode returnCode = e.getReturnCode();
            assertReturnCode(returnCode, Module.ZCSM, method.middlewareCode, null);
            Assert.assertSame(returnCode.getReturnCode(), pluginReturnCode,
                    method.label + " must keep the plugin's own return code as the cause");
        }
    }

    @Test(dataProvider = "methodsWrappingAStoreException")
    public void testStoreExceptionIsMappedToAPluginReturnCode(Wrapped method) {
        Mockito.when(method.storeCall.call(collaborationStore))
                .thenThrow(new IllegalStateException("store exploded"));

        try {
            method.adaptorCall.call(adaptor);
            Assert.fail(method.label + " must raise ZusammenException when the store throws");
        } catch (ZusammenException e) {
            ReturnCode returnCode = e.getReturnCode();
            assertReturnCode(returnCode, Module.ZCSM, method.middlewareCode, null);
            assertReturnCode(returnCode.getReturnCode(), Module.ZCSP, method.pluginCode,
                    "store exploded");
            Assert.assertNull(returnCode.getReturnCode().getReturnCode());
        }
    }

    @Test
    public void testUpdateItemReportsSuccess() {
        Response<Void> response = adaptor.updateItem(CONTEXT, ITEM_ID, info("renamed-item"));

        Assert.assertTrue(response.isSuccessful());
    }

    @Test
    public void testCreateItemForwardsItemIdAndInfo() {
        Mockito.when(collaborationStore.createItem(any(), any(), any())).thenReturn(emptySuccess());

        adaptor.createItem(CONTEXT, ITEM_ID, info("brand-new-item"));

        ArgumentCaptor<Info> infoCaptor = ArgumentCaptor.forClass(Info.class);
        Mockito.verify(collaborationStore).createItem(same(CONTEXT), eq(ITEM_ID),
                infoCaptor.capture());
        Assert.assertEquals(infoCaptor.getValue().getName(), "brand-new-item");
    }

    @Test
    public void testCreateItemFailsWhenTheStoreReportsAnUnsuccessfulResponse() {
        Mockito.when(collaborationStore.createItem(any(), any(), any()))
                .thenReturn(failedResponse(pluginReturnCode("plugin refused")));

        try {
            adaptor.createItem(CONTEXT, ITEM_ID, info("brand-new-item"));
            Assert.fail("createItem must raise ZusammenException on an unsuccessful response");
        } catch (ZusammenException e) {
            assertReturnCode(e.getReturnCode(), Module.ZCSM, ErrorCode.MD_ITEM_CREATE, null);
            Assert.assertNotNull(e.getReturnCode().getReturnCode());
        }
    }

    @Test
    public void testCreateItemVersionForwardsBaseVersionIdVersionIdAndData() {
        Mockito.when(collaborationStore.createItemVersion(any(), any(), any(), any(), any()))
                .thenReturn(emptySuccess());

        adaptor.createItemVersion(CONTEXT, ITEM_ID, BASE_VERSION_ID, VERSION_ID, VERSION_DATA);

        Mockito.verify(collaborationStore).createItemVersion(same(CONTEXT), eq(ITEM_ID),
                eq(BASE_VERSION_ID), eq(VERSION_ID), same(VERSION_DATA));
    }

    @Test
    public void testUpdateItemVersionForwardsVersionIdAndData() {
        Mockito.when(collaborationStore.updateItemVersion(any(), any(), any(), any()))
                .thenReturn(emptySuccess());

        adaptor.updateItemVersion(CONTEXT, ITEM_ID, VERSION_ID, VERSION_DATA);

        Mockito.verify(collaborationStore).updateItemVersion(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID), same(VERSION_DATA));
    }

    @Test
    public void testGetItemVersionStatusRewrapsThePluginValueInAFreshResponse() {
        ItemVersionStatus status = new ItemVersionStatus(SynchronizationStatus.OUT_OF_SYNC, true);
        Response<ItemVersionStatus> storeResponse = successfulResponse(status);
        Mockito.when(collaborationStore.getItemVersionStatus(any(), any(), any()))
                .thenReturn(storeResponse);

        Response<ItemVersionStatus> response =
                adaptor.getItemVersionStatus(CONTEXT, ITEM_ID, VERSION_ID);

        Assert.assertNotSame(response, storeResponse);
        Assert.assertSame(response.getValue(), status);
    }

    @Test
    public void testTagItemVersionForwardsChangeIdAndTag() {
        Mockito.when(collaborationStore.tagItemVersion(any(), any(), any(), any(), any()))
                .thenReturn(emptySuccess());

        adaptor.tagItemVersion(CONTEXT, ITEM_ID, VERSION_ID, CHANGE_ID, TAG);

        Mockito.verify(collaborationStore).tagItemVersion(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID), eq(CHANGE_ID), same(TAG));
    }

    @Test
    public void testPublishItemVersionForwardsMessageAndConvertsTheChange() {
        ItemVersionChange changedVersion = new ItemVersionChange();
        CollaborationPublishResult publishResult = new CollaborationPublishResult();
        publishResult.setChange(mergeChange(changedVersion, "published-element"));
        Mockito.when(collaborationStore.publishItemVersion(any(), any(), any(), any()))
                .thenReturn(successfulResponse(publishResult));

        Response<CorePublishResult> response =
                adaptor.publishItemVersion(CONTEXT, ITEM_ID, VERSION_ID, "publish message");

        Mockito.verify(collaborationStore).publishItemVersion(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID), eq("publish message"));
        CoreMergeChange change = response.getValue().getChange();
        Assert.assertSame(change.getChangedVersion(), changedVersion);
        Assert.assertEquals(idsOf(change.getChangedElements()),
                Collections.singletonList("published-element"));
        Assert.assertEquals(change.getChangedElements().iterator().next().getAction(),
                Action.UPDATE);
    }

    @Test
    public void testSyncItemVersionConvertsBothTheChangeAndTheConflict() {
        Mockito.when(collaborationStore.syncItemVersion(any(), any(), any()))
                .thenReturn(successfulResponse(mergeResult("synced-element", "conflicting-element")));

        Response<CoreMergeResult> response = adaptor.syncItemVersion(CONTEXT, ITEM_ID, VERSION_ID);

        Mockito.verify(collaborationStore).syncItemVersion(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID));
        CoreMergeResult result = response.getValue();
        Assert.assertEquals(idsOf(result.getChange().getChangedElements()),
                Collections.singletonList("synced-element"));
        Assert.assertEquals(result.getConflict().getElementConflicts().size(), 1);
        Assert.assertEquals(result.getConflict().getElementConflicts().iterator().next()
                .getLocalElement().getId(), new Id("conflicting-element"));
        Assert.assertFalse(result.isCompleted());
    }

    @Test
    public void testForceSyncItemVersionUsesTheStoresForceSyncEntryPoint() {
        Mockito.when(collaborationStore.forceSyncItemVersion(any(), any(), any()))
                .thenReturn(successfulResponse(mergeResult("force-synced-element", null)));

        Response<CoreMergeResult> response =
                adaptor.forceSyncItemVersion(CONTEXT, ITEM_ID, VERSION_ID);

        Mockito.verify(collaborationStore).forceSyncItemVersion(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID));
        Mockito.verify(collaborationStore, Mockito.never())
                .syncItemVersion(any(), any(), any());
        Assert.assertEquals(idsOf(response.getValue().getChange().getChangedElements()),
                Collections.singletonList("force-synced-element"));
        Assert.assertTrue(response.getValue().isCompleted());
    }

    @Test
    public void testMergeItemVersionForwardsTheSourceVersionSeparatelyFromTheTargetVersion() {
        Mockito.when(collaborationStore.mergeItemVersion(any(), any(), any(), any()))
                .thenReturn(successfulResponse(mergeResult("merged-element", null)));

        adaptor.mergeItemVersion(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_VERSION_ID);

        Mockito.verify(collaborationStore).mergeItemVersion(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID), eq(SOURCE_VERSION_ID));
    }

    @Test
    public void testListItemVersionRevisionsForwardsItemAndVersionIds() {
        ItemVersionRevisions revisions = new ItemVersionRevisions();
        revisions.addChange(new Revision());
        Mockito.when(collaborationStore.listItemVersionRevisions(any(), any(), any()))
                .thenReturn(successfulResponse(revisions));

        Response<ItemVersionRevisions> response =
                adaptor.listItemVersionRevisions(CONTEXT, ITEM_ID, VERSION_ID);

        Mockito.verify(collaborationStore).listItemVersionRevisions(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID));
        Assert.assertSame(response.getValue(), revisions);
    }

    @Test
    public void testGetItemVersionRevisionForwardsTheRevisionId() {
        Revision revision = new Revision();
        revision.setRevisionId(REVISION_ID);
        Mockito.when(collaborationStore.getItemVersionRevision(any(), any(), any(), any()))
                .thenReturn(successfulResponse(revision));

        Response<Revision> response =
                adaptor.getItemVersionRevision(CONTEXT, ITEM_ID, VERSION_ID, REVISION_ID);

        Mockito.verify(collaborationStore).getItemVersionRevision(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID), eq(REVISION_ID));
        Assert.assertSame(response.getValue(), revision);
    }

    @Test
    public void testResetItemVersionRevisionConvertsTheMergeChange() {
        ItemVersionChange changedVersion = new ItemVersionChange();
        Mockito.when(collaborationStore.resetItemVersionRevision(any(), any(), any(), any()))
                .thenReturn(successfulResponse(mergeChange(changedVersion, "reset-element")));

        Response<CoreMergeChange> response =
                adaptor.resetItemVersionRevision(CONTEXT, ITEM_ID, VERSION_ID, REVISION_ID);

        Mockito.verify(collaborationStore).resetItemVersionRevision(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID), eq(REVISION_ID));
        Mockito.verify(collaborationStore, Mockito.never())
                .revertItemVersionRevision(any(), any(), any(), any());
        Assert.assertSame(response.getValue().getChangedVersion(), changedVersion);
        Assert.assertEquals(idsOf(response.getValue().getChangedElements()),
                Collections.singletonList("reset-element"));
    }

    @Test
    public void testRevertItemVersionRevisionConvertsTheMergeChange() {
        ItemVersionChange changedVersion = new ItemVersionChange();
        Mockito.when(collaborationStore.revertItemVersionRevision(any(), any(), any(), any()))
                .thenReturn(successfulResponse(mergeChange(changedVersion, "reverted-element")));

        Response<CoreMergeChange> response =
                adaptor.revertItemVersionRevision(CONTEXT, ITEM_ID, VERSION_ID, REVISION_ID);

        Mockito.verify(collaborationStore).revertItemVersionRevision(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID), eq(REVISION_ID));
        Mockito.verify(collaborationStore, Mockito.never())
                .resetItemVersionRevision(any(), any(), any(), any());
        Assert.assertSame(response.getValue().getChangedVersion(), changedVersion);
        Assert.assertEquals(idsOf(response.getValue().getChangedElements()),
                Collections.singletonList("reverted-element"));
    }

    @Test
    public void testGetItemVersionConflictConvertsVersionDataAndElementDescriptors() {
        ItemVersionDataConflict dataConflict = new ItemVersionDataConflict();
        ElementConflictDescriptor descriptor = new ElementConflictDescriptor();
        descriptor.setLocalElementDescriptor(collaborationElement("local-element"));
        descriptor.setRemoteElementDescriptor(collaborationElement("remote-element"));
        CollaborationItemVersionConflict conflict = new CollaborationItemVersionConflict();
        conflict.setVersionDataConflict(dataConflict);
        conflict.addElementConflictDescriptor(descriptor);
        Mockito.when(collaborationStore.getItemVersionConflict(any(), any(), any()))
                .thenReturn(successfulResponse(conflict));

        Response<CoreItemVersionConflict> response =
                adaptor.getItemVersionConflict(CONTEXT, ITEM_ID, VERSION_ID);

        CoreItemVersionConflict converted = response.getValue();
        Assert.assertSame(converted.getVersionDataConflict(), dataConflict);
        Assert.assertEquals(converted.getElementConflictInfos().size(), 1);
        Assert.assertEquals(converted.getElementConflictInfos().iterator().next()
                .getLocalCoreElementInfo().getId(), new Id("local-element"));
        Assert.assertEquals(converted.getElementConflictInfos().iterator().next()
                .getRemoteCoreElementInfo().getId(), new Id("remote-element"));
    }

    @Test
    public void testCommitElementsUnpacksTheElementContextIntoItemAndVersionIds() {
        Mockito.when(collaborationStore.commitElements(any(), any(), any(), any()))
                .thenReturn(emptySuccess());

        adaptor.commitElements(CONTEXT, new ElementContext(ITEM_ID, VERSION_ID, REVISION_ID),
                "commit message");

        Mockito.verify(collaborationStore).commitElements(same(CONTEXT), eq(ITEM_ID),
                eq(VERSION_ID), eq("commit message"));
    }

    @Test
    public void testListElementsForwardsNamespaceAndElementIdAndConvertsEveryElement() {
        Namespace namespace = namespace("parent/child");
        Mockito.when(collaborationStore.listElements(any(), any(), any(), any()))
                .thenReturn(successfulResponse(Arrays.asList(collaborationElement("first"),
                        collaborationElement("second"))));

        Response<Collection<CoreElement>> response =
                adaptor.listElements(CONTEXT, ELEMENT_CONTEXT, namespace, ELEMENT_ID);

        Mockito.verify(collaborationStore).listElements(same(CONTEXT), eq(ELEMENT_CONTEXT),
                same(namespace), eq(ELEMENT_ID));
        Assert.assertEquals(idsOf(response.getValue()), Arrays.asList("first", "second"));
        CoreElement first = response.getValue().iterator().next();
        Assert.assertEquals(first.getNamespace(), namespace("first-namespace"));
        Assert.assertEquals(text(first.getData()), "first-data");
    }

    @Test
    public void testListElementsReturnsAnEmptyCollectionWhenTheStoreHasNoElements() {
        Mockito.when(collaborationStore.listElements(any(), any(), any(), any()))
                .thenReturn(successfulResponse(Collections.emptyList()));

        Response<Collection<CoreElement>> response =
                adaptor.listElements(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE,
                        ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertTrue(response.getValue().isEmpty());
    }

    @Test
    public void testGetElementForwardsNamespaceAndConvertsTheElement() {
        Namespace namespace = namespace("parent/child");
        Mockito.when(collaborationStore.getElement(any(), any(), any(), any()))
                .thenReturn(successfulResponse(collaborationElement("fetched")));

        Response<CoreElement> response =
                adaptor.getElement(CONTEXT, ELEMENT_CONTEXT, namespace, ELEMENT_ID);

        Mockito.verify(collaborationStore).getElement(same(CONTEXT), eq(ELEMENT_CONTEXT),
                same(namespace), eq(ELEMENT_ID));
        CoreElement element = response.getValue();
        Assert.assertEquals(element.getId(), new Id("fetched"));
        Assert.assertEquals(element.getNamespace(), namespace("fetched-namespace"));
        Assert.assertEquals(text(element.getVisualization()), "fetched-visualization");
    }

    @Test
    public void testGetElementConflictConvertsBothSidesOfTheConflict() {
        CollaborationElementConflict conflict = new CollaborationElementConflict();
        conflict.setLocalElement(collaborationElement("local"));
        conflict.setRemoteElement(collaborationElement("remote"));
        Mockito.when(collaborationStore.getElementConflict(any(), any(), any(), any()))
                .thenReturn(successfulResponse(conflict));

        Response<CoreElementConflict> response =
                adaptor.getElementConflict(CONTEXT, ELEMENT_CONTEXT, namespace("parent"),
                        ELEMENT_ID);

        Assert.assertEquals(response.getValue().getLocalElement().getId(), new Id("local"));
        Assert.assertEquals(response.getValue().getRemoteElement().getId(), new Id("remote"));
    }

    @Test
    public void testCreateElementForwardsAnElementCarryingTheElementContextIds() {
        Mockito.when(collaborationStore.createElement(any(), any())).thenReturn(emptySuccess());
        CoreElement element = coreElement("new-element");
        ElementContext elementContext =
                new ElementContext(new Id("owning-item"), new Id("owning-version"));

        adaptor.createElement(CONTEXT, elementContext, element);

        ArgumentCaptor<CollaborationElement> captor =
                ArgumentCaptor.forClass(CollaborationElement.class);
        Mockito.verify(collaborationStore).createElement(same(CONTEXT), captor.capture());
        CollaborationElement forwarded = captor.getValue();
        Assert.assertEquals(forwarded.getItemId(), new Id("owning-item"));
        Assert.assertEquals(forwarded.getVersionId(), new Id("owning-version"));
        Assert.assertEquals(forwarded.getId(), new Id("new-element"));
        Assert.assertEquals(forwarded.getParentId(), new Id("new-element-parent"));
        Assert.assertEquals(forwarded.getNamespace(), namespace("new-element-namespace"));
        Assert.assertSame(forwarded.getInfo(), element.getInfo());
        Assert.assertSame(forwarded.getRelations(), element.getRelations());
        Assert.assertEquals(text(forwarded.getData()), "new-element-data");
        Assert.assertEquals(text(forwarded.getSearchableData()), "new-element-searchable");
        Assert.assertEquals(text(forwarded.getVisualization()), "new-element-visualization");
    }

    @Test
    public void testUpdateElementRoutesToTheStoresUpdateEntryPoint() {
        Mockito.when(collaborationStore.updateElement(any(), any())).thenReturn(emptySuccess());

        adaptor.updateElement(CONTEXT, ELEMENT_CONTEXT, coreElement("changed-element"));

        ArgumentCaptor<CollaborationElement> captor =
                ArgumentCaptor.forClass(CollaborationElement.class);
        Mockito.verify(collaborationStore).updateElement(same(CONTEXT), captor.capture());
        CollaborationElement forwarded = captor.getValue();
        Assert.assertEquals(forwarded.getId(), new Id("changed-element"));
        Assert.assertEquals(text(forwarded.getData()), "changed-element-data");
        Mockito.verify(collaborationStore, Mockito.never()).createElement(any(), any());
        Mockito.verify(collaborationStore, Mockito.never()).deleteElement(any(), any());
    }

    @Test
    public void testDeleteElementRoutesToTheStoresDeleteEntryPoint() {
        Mockito.when(collaborationStore.deleteElement(any(), any())).thenReturn(emptySuccess());

        adaptor.deleteElement(CONTEXT, ELEMENT_CONTEXT, coreElement("doomed-element"));

        ArgumentCaptor<CollaborationElement> captor =
                ArgumentCaptor.forClass(CollaborationElement.class);
        Mockito.verify(collaborationStore).deleteElement(same(CONTEXT), captor.capture());
        CollaborationElement forwarded = captor.getValue();
        Assert.assertEquals(forwarded.getId(), new Id("doomed-element"));
        Assert.assertEquals(forwarded.getItemId(), ITEM_ID);
        Mockito.verify(collaborationStore, Mockito.never()).updateElement(any(), any());
    }

    @Test
    public void testResolveElementConflictForwardsTheChosenResolution() {
        Mockito.when(collaborationStore.resolveElementConflict(any(), any(), any()))
                .thenReturn(successfulResponse(mergeResult("resolved-element", null)));

        Response<CoreMergeResult> response = adaptor.resolveElementConflict(CONTEXT,
                ELEMENT_CONTEXT, coreElement("contested-element"), Resolution.THEIRS);

        ArgumentCaptor<CollaborationElement> elementCaptor =
                ArgumentCaptor.forClass(CollaborationElement.class);
        Mockito.verify(collaborationStore).resolveElementConflict(same(CONTEXT),
                elementCaptor.capture(), eq(Resolution.THEIRS));
        Assert.assertEquals(elementCaptor.getValue().getId(), new Id("contested-element"));
        Assert.assertEquals(idsOf(response.getValue().getChange().getChangedElements()),
                Collections.singletonList("resolved-element"));
    }

    @Test(dataProvider = "spaces")
    public void testGetItemVersionForwardsTheRequestedSpace(Space space) {
        ItemVersion itemVersion = new ItemVersion();
        Response<ItemVersion> storeResponse = successfulResponse(itemVersion);
        Mockito.when(collaborationStore.getItemVersion(any(), any(), any(), any(), any()))
                .thenReturn(storeResponse);

        Response<ItemVersion> response =
                adaptor.getItemVersion(CONTEXT, space, ITEM_ID, VERSION_ID, REVISION_ID);

        ArgumentCaptor<Space> spaceCaptor = ArgumentCaptor.forClass(Space.class);
        Mockito.verify(collaborationStore).getItemVersion(same(CONTEXT), spaceCaptor.capture(),
                eq(ITEM_ID), eq(VERSION_ID), eq(REVISION_ID));
        Assert.assertEquals(spaceCaptor.getValue(), space);
        Assert.assertSame(response, storeResponse);
    }

    @DataProvider(name = "spaces")
    public static Object[][] spaces() {
        return new Object[][] {{Space.PRIVATE}, {Space.PUBLIC}, {Space.BOTH}};
    }

    @DataProvider(name = "passThroughMethods")
    public static Object[][] passThroughMethods() {
        return rows(Selection.PASS_THROUGH);
    }

    @DataProvider(name = "methodsWrappingAnUnsuccessfulResponse")
    public static Object[][] methodsWrappingAnUnsuccessfulResponse() {
        return rows(Selection.NESTS_PLUGIN_RETURN_CODE);
    }

    @DataProvider(name = "methodsWrappingAStoreException")
    public static Object[][] methodsWrappingAStoreException() {
        return rows(Selection.CATCHES_STORE_EXCEPTION);
    }

    private static List<String> idsOf(Collection<CoreElement> elements) {
        List<String> ids = new ArrayList<>();
        for (CoreElement element : elements) {
            ids.add(element.getId().getValue());
        }
        return ids;
    }

    private static CollaborationElement collaborationElement(String id) {
        CollaborationElement element = new CollaborationElement(ITEM_ID, VERSION_ID,
                namespace(id + "-namespace"), new Id(id));
        element.setParentId(new Id(id + "-parent"));
        element.setInfo(info(id + "-info"));
        element.setRelations(Collections.singletonList(OutboundTestSupport.relation(id)));
        element.setData(stream(id + "-data"));
        element.setSearchableData(stream(id + "-searchable"));
        element.setVisualization(stream(id + "-visualization"));
        return element;
    }

    private static CollaborationMergeChange mergeChange(ItemVersionChange changedVersion,
                                                        String changedElementId) {
        CollaborationElementChange elementChange = new CollaborationElementChange();
        elementChange.setElement(collaborationElement(changedElementId));
        elementChange.setAction(Action.UPDATE);
        CollaborationMergeChange change = new CollaborationMergeChange();
        change.setChangedVersion(changedVersion);
        change.setChangedElements(Collections.singletonList(elementChange));
        return change;
    }

    private static CollaborationMergeResult mergeResult(String changedElementId,
                                                        String conflictingElementId) {
        CollaborationMergeResult result = new CollaborationMergeResult();
        result.setChange(mergeChange(new ItemVersionChange(), changedElementId));
        if (conflictingElementId != null) {
            CollaborationElementConflict elementConflict = new CollaborationElementConflict();
            elementConflict.setLocalElement(collaborationElement(conflictingElementId));
            elementConflict.setRemoteElement(collaborationElement(conflictingElementId));
            CollaborationMergeConflict conflict = new CollaborationMergeConflict();
            conflict.addElementConflict(elementConflict);
            result.setConflict(conflict);
        }
        return result;
    }

    private enum Selection {
        PASS_THROUGH, NESTS_PLUGIN_RETURN_CODE, CATCHES_STORE_EXCEPTION
    }

    private static Object[][] rows(Selection selection) {
        List<Object[]> rows = new ArrayList<>();
        for (Wrapped method : wrappedMethods()) {
            boolean include;
            switch (selection) {
                case PASS_THROUGH:
                    include = method.passesTheStoreResponseThrough;
                    break;
                case NESTS_PLUGIN_RETURN_CODE:
                    include = !method.rewrapsItsOwnFailure;
                    break;
                default:
                    include = method.pluginCode != 0;
                    break;
            }
            if (include) {
                rows.add(new Object[] {method});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    /**
     * The error-mapping table of {@link CollaborationAdaptorImpl}: for every method, which
     * middleware ({@link Module#ZCSM}) and plugin ({@link Module#ZCSP}) code the failure paths must
     * produce. A plugin code of 0 means the method has no catch block at all.
     */
    private static List<Wrapped> wrappedMethods() {
        return Arrays.asList(
                method("createItem", s -> s.createItem(any(), any(), any()),
                        a -> a.createItem(CONTEXT, ITEM_ID, info("item")),
                        ErrorCode.MD_ITEM_CREATE, ErrorCode.CL_ITEM_CREATE)
                        .passesTheStoreResponseThrough().rewrapsItsOwnFailure(),
                method("deleteItem", s -> s.deleteItem(any(), any()),
                        a -> a.deleteItem(CONTEXT, ITEM_ID),
                        ErrorCode.MD_ITEM_DELETE, ErrorCode.CL_ITEM_DELETE)
                        .passesTheStoreResponseThrough(),
                method("createItemVersion", s -> s.createItemVersion(any(), any(), any(), any(),
                                any()),
                        a -> a.createItemVersion(CONTEXT, ITEM_ID, BASE_VERSION_ID, VERSION_ID,
                                VERSION_DATA),
                        ErrorCode.MD_ITEM_VERSION_CREATE, ErrorCode.CL_ITEM_VERSION_CREATE)
                        .passesTheStoreResponseThrough(),
                method("updateItemVersion", s -> s.updateItemVersion(any(), any(), any(), any()),
                        a -> a.updateItemVersion(CONTEXT, ITEM_ID, VERSION_ID, VERSION_DATA),
                        ErrorCode.MD_ITEM_VERSION_UPDATE, ErrorCode.CL_ITEM_VERSION_UPDATE)
                        .passesTheStoreResponseThrough(),
                method("deleteItemVersion", s -> s.deleteItemVersion(any(), any(), any()),
                        a -> a.deleteItemVersion(CONTEXT, ITEM_ID, VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_DELETE, ErrorCode.CL_ITEM_VERSION_DELETE)
                        .passesTheStoreResponseThrough(),
                method("getItemVersionStatus", s -> s.getItemVersionStatus(any(), any(), any()),
                        a -> a.getItemVersionStatus(CONTEXT, ITEM_ID, VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_GET_STATUS,
                        ErrorCode.CL_ITEM_VERSION_GET_STATUS),
                method("tagItemVersion", s -> s.tagItemVersion(any(), any(), any(), any(), any()),
                        a -> a.tagItemVersion(CONTEXT, ITEM_ID, VERSION_ID, CHANGE_ID, TAG),
                        ErrorCode.MD_ITEM_VERSION_TAG, ErrorCode.CL_ITEM_VERSION_TAG)
                        .passesTheStoreResponseThrough(),
                method("publishItemVersion", s -> s.publishItemVersion(any(), any(), any(), any()),
                        a -> a.publishItemVersion(CONTEXT, ITEM_ID, VERSION_ID, "message"),
                        ErrorCode.MD_ITEM_VERSION_PUBLISH, ErrorCode.CL_ITEM_VERSION_PUBLISH),
                method("syncItemVersion", s -> s.syncItemVersion(any(), any(), any()),
                        a -> a.syncItemVersion(CONTEXT, ITEM_ID, VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_SYNC, ErrorCode.CL_ITEM_VERSION_SYNC),
                method("forceSyncItemVersion", s -> s.forceSyncItemVersion(any(), any(), any()),
                        a -> a.forceSyncItemVersion(CONTEXT, ITEM_ID, VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_FORCE_SYNC,
                        ErrorCode.CL_ITEM_VERSION_FORCE_SYNC),
                method("mergeItemVersion", s -> s.mergeItemVersion(any(), any(), any(), any()),
                        a -> a.mergeItemVersion(CONTEXT, ITEM_ID, VERSION_ID, SOURCE_VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_MERGE, ErrorCode.CL_ITEM_VERSION_MERGE),
                method("listItemVersionRevisions",
                        s -> s.listItemVersionRevisions(any(), any(), any()),
                        a -> a.listItemVersionRevisions(CONTEXT, ITEM_ID, VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_REVISIONS, ErrorCode.CL_ITEM_VERSION_REVISIONS)
                        .passesTheStoreResponseThrough(),
                method("getItemVersionRevision",
                        s -> s.getItemVersionRevision(any(), any(), any(), any()),
                        a -> a.getItemVersionRevision(CONTEXT, ITEM_ID, VERSION_ID, REVISION_ID),
                        ErrorCode.MD_ITEM_VERSION_REVISION, ErrorCode.CL_ITEM_VERSION_REVISION)
                        .passesTheStoreResponseThrough(),
                method("resetItemVersionRevision",
                        s -> s.resetItemVersionRevision(any(), any(), any(), any()),
                        a -> a.resetItemVersionRevision(CONTEXT, ITEM_ID, VERSION_ID, REVISION_ID),
                        ErrorCode.MD_ITEM_VERSION_RESET_REVISION,
                        ErrorCode.CL_ITEM_VERSION_RESET_REVISION),
                method("revertItemVersionRevision",
                        s -> s.revertItemVersionRevision(any(), any(), any(), any()),
                        a -> a.revertItemVersionRevision(CONTEXT, ITEM_ID, VERSION_ID, REVISION_ID),
                        ErrorCode.MD_ITEM_VERSION_REVERT_REVISION,
                        ErrorCode.CL_ITEM_VERSION_REVERT_REVISION),
                method("getItemVersionConflict",
                        s -> s.getItemVersionConflict(any(), any(), any()),
                        a -> a.getItemVersionConflict(CONTEXT, ITEM_ID, VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_GET_CONFLICT,
                        ErrorCode.CL_ITEM_VERSION_GET_CONFLICT),
                method("commitElements", s -> s.commitElements(any(), any(), any(), any()),
                        a -> a.commitElements(CONTEXT, ELEMENT_CONTEXT, "message"),
                        ErrorCode.MD_COMMIT, 0)
                        .passesTheStoreResponseThrough(),
                method("listElements", s -> s.listElements(any(), any(), any(), any()),
                        a -> a.listElements(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE,
                                ELEMENT_ID),
                        ErrorCode.MD_ELEMENT_GET_LIST, ErrorCode.CL_ELEMENT_GET_LIST),
                method("getElement", s -> s.getElement(any(), any(), any(), any()),
                        a -> a.getElement(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE,
                                ELEMENT_ID),
                        ErrorCode.MD_ELEMENT_GET, ErrorCode.CL_ELEMENT_GET),
                method("getElementConflict", s -> s.getElementConflict(any(), any(), any(), any()),
                        a -> a.getElementConflict(CONTEXT, ELEMENT_CONTEXT,
                                Namespace.ROOT_NAMESPACE, ELEMENT_ID),
                        ErrorCode.MD_ELEMENT_GET_CONFLICT, ErrorCode.CL_ELEMENT_GET_CONFLICT),
                method("createElement", s -> s.createElement(any(), any()),
                        a -> a.createElement(CONTEXT, ELEMENT_CONTEXT, coreElement("element")),
                        ErrorCode.MD_ELEMENT_CREATE, ErrorCode.CL_ELEMENT_CREATE)
                        .passesTheStoreResponseThrough(),
                method("updateElement", s -> s.updateElement(any(), any()),
                        a -> a.updateElement(CONTEXT, ELEMENT_CONTEXT, coreElement("element")),
                        ErrorCode.MD_ELEMENT_UPDATE, ErrorCode.CL_ELEMENT_UPDATE)
                        .passesTheStoreResponseThrough(),
                method("deleteElement", s -> s.deleteElement(any(), any()),
                        a -> a.deleteElement(CONTEXT, ELEMENT_CONTEXT, coreElement("element")),
                        ErrorCode.MD_ELEMENT_DELETE, ErrorCode.CL_ELEMENT_DELETE)
                        .passesTheStoreResponseThrough(),
                method("resolveElementConflict",
                        s -> s.resolveElementConflict(any(), any(), any()),
                        a -> a.resolveElementConflict(CONTEXT, ELEMENT_CONTEXT,
                                coreElement("element"), Resolution.YOURS),
                        ErrorCode.MD_ELEMENT_RESOLVE_CONFLICT,
                        ErrorCode.CL_ELEMENT_RESOLVE_CONFLICT),
                method("getItemVersion", s -> s.getItemVersion(any(), any(), any(), any(), any()),
                        a -> a.getItemVersion(CONTEXT, Space.PRIVATE, ITEM_ID, VERSION_ID,
                                REVISION_ID),
                        ErrorCode.MD_ITEM_VERSION_GET, ErrorCode.CL_ITEM_VERSION_GET)
                        .passesTheStoreResponseThrough());
    }

    private static Wrapped method(String label, StoreCall storeCall, AdaptorCall adaptorCall,
                                  int middlewareCode, int pluginCode) {
        return new Wrapped(label, storeCall, adaptorCall, middlewareCode, pluginCode);
    }

    private interface StoreCall {
        Object call(CollaborationStore store);
    }

    private interface AdaptorCall {
        Object call(CollaborationAdaptorImpl adaptor);
    }

    private static final class Wrapped {
        private final String label;
        private final StoreCall storeCall;
        private final AdaptorCall adaptorCall;
        private final int middlewareCode;
        private final int pluginCode;
        private boolean passesTheStoreResponseThrough;
        private boolean rewrapsItsOwnFailure;

        private Wrapped(String label, StoreCall storeCall, AdaptorCall adaptorCall,
                        int middlewareCode, int pluginCode) {
            this.label = label;
            this.storeCall = storeCall;
            this.adaptorCall = adaptorCall;
            this.middlewareCode = middlewareCode;
            this.pluginCode = pluginCode;
        }

        private Wrapped passesTheStoreResponseThrough() {
            passesTheStoreResponseThrough = true;
            return this;
        }

        private Wrapped rewrapsItsOwnFailure() {
            rewrapsItsOwnFailure = true;
            return this;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
