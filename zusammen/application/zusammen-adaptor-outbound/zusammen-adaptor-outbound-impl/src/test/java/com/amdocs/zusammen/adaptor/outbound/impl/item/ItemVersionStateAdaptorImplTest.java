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

package com.amdocs.zusammen.adaptor.outbound.impl.item;

import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.assertReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.emptySuccess;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.failedResponse;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.pluginReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.successfulResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

import com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.sdk.state.StateStore;
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
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

public class ItemVersionStateAdaptorImplTest {

    private static final SessionContext CONTEXT = new SessionContext();
    private static final Id ITEM_ID = new Id("item-id");
    private static final Id VERSION_ID = new Id("version-id");
    private static final Id BASE_VERSION_ID = new Id("base-version-id");
    private static final ItemVersionData VERSION_DATA = new ItemVersionData();
    private static final Date CREATION_TIME = new Date(1_000_000L);
    private static final Date MODIFICATION_TIME = new Date(2_000_000L);

    @Mock
    private StateStore stateStore;

    private AutoCloseable mocks;
    private ItemVersionStateAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        OutboundTestSupport.installStateStore(stateStore);
        adaptor = new ItemVersionStateAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OutboundTestSupport.restoreStateStore();
        mocks.close();
    }

    @Test
    public void testStateStoreIsResolvedWithTheCallersSessionContext() {
        Mockito.when(stateStore.deleteItemVersion(any(), any(), any(), any()))
                .thenReturn(emptySuccess());

        adaptor.deleteItemVersion(CONTEXT, Space.PRIVATE, ITEM_ID, VERSION_ID);

        Assert.assertSame(OutboundTestSupport.stateStoreResolutionContext(), CONTEXT);
    }

    @Test(dataProvider = "methodsAndSpaces")
    public void testRequestedSpaceIsForwardedToTheStateStore(Wrapped method, Space space) {
        Mockito.when(method.storeCall.call(stateStore, () -> any())).thenReturn(emptySuccess());

        method.adaptorCall.call(adaptor, space);

        ArgumentCaptor<Space> spaceCaptor = ArgumentCaptor.forClass(Space.class);
        method.storeCall.call(Mockito.verify(stateStore), () -> spaceCaptor.capture());
        Assert.assertEquals(spaceCaptor.getValue(), space,
                method.label + " must address the space it was given");
    }

    @Test(dataProvider = "methods")
    public void testStateStoreResponseIsReturnedUnchangedOnSuccess(Wrapped method) {
        Response<Void> storeResponse = emptySuccess();
        Mockito.when(method.storeCall.call(stateStore, () -> any())).thenReturn(storeResponse);

        Assert.assertSame(method.adaptorCall.call(adaptor, Space.PRIVATE), storeResponse,
                method.label + " must hand the plugin response back untouched");
    }

    @Test(dataProvider = "methods")
    public void testUnsuccessfulStateStoreResponseIsNestedInMiddlewareReturnCode(Wrapped method) {
        ReturnCode storeReturnCode = pluginReturnCode("state store refused");
        Mockito.when(method.storeCall.call(stateStore, () -> any()))
                .thenReturn(failedResponse(storeReturnCode));

        Response<?> response = (Response<?>) method.adaptorCall.call(adaptor, Space.PUBLIC);

        Assert.assertFalse(response.isSuccessful(), method.label + " must report the failure");
        assertReturnCode(response.getReturnCode(), Module.ZSTM, method.middlewareCode, null);
        Assert.assertSame(response.getReturnCode().getReturnCode(), storeReturnCode,
                method.label + " must keep the plugin's own return code as the cause");
    }

    @Test(dataProvider = "methods")
    public void testStateStoreExceptionIsMappedToAMetadataPluginReturnCode(Wrapped method) {
        Mockito.when(method.storeCall.call(stateStore, () -> any()))
                .thenThrow(new IllegalStateException("state store unreachable"));

        Response<?> response = (Response<?>) method.adaptorCall.call(adaptor, Space.PRIVATE);

        Assert.assertFalse(response.isSuccessful(), method.label + " must report the failure");
        assertReturnCode(response.getReturnCode(), Module.ZSTM, method.middlewareCode, null);
        assertReturnCode(response.getReturnCode().getReturnCode(), Module.ZMDP, method.pluginCode,
                "state store unreachable");
        Assert.assertNull(response.getReturnCode().getReturnCode().getReturnCode());
    }

    @Test
    public void testListItemVersionsHandsBackThePluginsVersions() {
        ItemVersion itemVersion = new ItemVersion();
        itemVersion.setId(VERSION_ID);
        Response<Collection<ItemVersion>> storeResponse =
                successfulResponse(Collections.<ItemVersion>singletonList(itemVersion));
        Mockito.when(stateStore.listItemVersions(any(), any(), any())).thenReturn(storeResponse);

        Response<Collection<ItemVersion>> response =
                adaptor.listItemVersions(CONTEXT, Space.PUBLIC, ITEM_ID);

        Mockito.verify(stateStore).listItemVersions(same(CONTEXT), eq(Space.PUBLIC), eq(ITEM_ID));
        Assert.assertSame(response.getValue().iterator().next(), itemVersion);
    }

    @Test
    public void testIsItemVersionExistHandsBackThePluginsAnswer() {
        Response<Boolean> storeResponse = successfulResponse(Boolean.TRUE);
        Mockito.when(stateStore.isItemVersionExist(any(), any(), any(), any()))
                .thenReturn(storeResponse);

        Response<Boolean> response =
                adaptor.isItemVersionExist(CONTEXT, Space.PRIVATE, ITEM_ID, VERSION_ID);

        Mockito.verify(stateStore).isItemVersionExist(same(CONTEXT), eq(Space.PRIVATE), eq(ITEM_ID),
                eq(VERSION_ID));
        Assert.assertEquals(response.getValue(), Boolean.TRUE);
    }

    @Test
    public void testGetItemVersionForwardsTheVersionIdAndHandsBackThePluginsVersion() {
        ItemVersion itemVersion = new ItemVersion();
        Mockito.when(stateStore.getItemVersion(any(), any(), any(), any()))
                .thenReturn(successfulResponse(itemVersion));

        Response<ItemVersion> response =
                adaptor.getItemVersion(CONTEXT, Space.PUBLIC, ITEM_ID, VERSION_ID);

        Mockito.verify(stateStore).getItemVersion(same(CONTEXT), eq(Space.PUBLIC), eq(ITEM_ID),
                eq(VERSION_ID));
        Assert.assertSame(response.getValue(), itemVersion);
    }

    @Test
    public void testCreateItemVersionForwardsBaseVersionIdVersionIdDataAndCreationTime() {
        Mockito.when(stateStore.createItemVersion(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySuccess());

        adaptor.createItemVersion(CONTEXT, Space.PRIVATE, ITEM_ID, BASE_VERSION_ID, VERSION_ID,
                VERSION_DATA, CREATION_TIME);

        Mockito.verify(stateStore).createItemVersion(same(CONTEXT), eq(Space.PRIVATE), eq(ITEM_ID),
                eq(BASE_VERSION_ID), eq(VERSION_ID), same(VERSION_DATA), eq(CREATION_TIME));
    }

    @Test
    public void testUpdateItemVersionForwardsDataAndModificationTime() {
        Mockito.when(stateStore.updateItemVersion(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySuccess());

        adaptor.updateItemVersion(CONTEXT, Space.PRIVATE, ITEM_ID, VERSION_ID, VERSION_DATA,
                MODIFICATION_TIME);

        Mockito.verify(stateStore).updateItemVersion(same(CONTEXT), eq(Space.PRIVATE), eq(ITEM_ID),
                eq(VERSION_ID), same(VERSION_DATA), eq(MODIFICATION_TIME));
    }

    @Test
    public void testUpdateItemVersionModificationTimeTouchesTheTimestampWithoutRewritingTheData() {
        Mockito.when(stateStore.updateItemVersionModificationTime(any(), any(), any(), any(), any()))
                .thenReturn(emptySuccess());

        adaptor.updateItemVersionModificationTime(CONTEXT, Space.PUBLIC, ITEM_ID, VERSION_ID,
                MODIFICATION_TIME);

        Mockito.verify(stateStore).updateItemVersionModificationTime(same(CONTEXT),
                eq(Space.PUBLIC), eq(ITEM_ID), eq(VERSION_ID), eq(MODIFICATION_TIME));
        Mockito.verify(stateStore, Mockito.never())
                .updateItemVersion(any(), any(), any(), any(), any(), any());
    }

    @DataProvider(name = "methods")
    public static Object[][] methods() {
        List<Object[]> rows = new ArrayList<>();
        for (Wrapped method : wrappedMethods()) {
            rows.add(new Object[] {method});
        }
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "methodsAndSpaces")
    public static Object[][] methodsAndSpaces() {
        List<Object[]> rows = new ArrayList<>();
        for (Wrapped method : wrappedMethods()) {
            for (Space space : Space.values()) {
                rows.add(new Object[] {method, space});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    /**
     * The error-mapping table of {@link ItemVersionStateAdaptorImpl}: for every method, which
     * middleware ({@link Module#ZSTM}) and plugin ({@link Module#ZMDP}) code the failure paths must
     * produce.
     */
    private static List<Wrapped> wrappedMethods() {
        return Arrays.asList(
                new Wrapped("listItemVersions",
                        (s, space) -> s.listItemVersions(same(CONTEXT), space.get(), eq(ITEM_ID)),
                        (a, space) -> a.listItemVersions(CONTEXT, space, ITEM_ID),
                        ErrorCode.MD_ITEM_VERSIONS_LIST, ErrorCode.ST_ITEM_VERSIONS_LIST),
                new Wrapped("isItemVersionExist",
                        (s, space) -> s.isItemVersionExist(same(CONTEXT), space.get(), eq(ITEM_ID),
                                eq(VERSION_ID)),
                        (a, space) -> a.isItemVersionExist(CONTEXT, space, ITEM_ID, VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_IS_EXIST, ErrorCode.ST_ITEM_VERSION_IS_EXIST),
                new Wrapped("getItemVersion",
                        (s, space) -> s.getItemVersion(same(CONTEXT), space.get(), eq(ITEM_ID),
                                eq(VERSION_ID)),
                        (a, space) -> a.getItemVersion(CONTEXT, space, ITEM_ID, VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_GET, ErrorCode.ST_ITEM_VERSION_GET),
                new Wrapped("createItemVersion",
                        (s, space) -> s.createItemVersion(same(CONTEXT), space.get(), eq(ITEM_ID),
                                eq(BASE_VERSION_ID), eq(VERSION_ID), same(VERSION_DATA),
                                eq(CREATION_TIME)),
                        (a, space) -> a.createItemVersion(CONTEXT, space, ITEM_ID, BASE_VERSION_ID,
                                VERSION_ID, VERSION_DATA, CREATION_TIME),
                        ErrorCode.MD_ITEM_VERSION_CREATE, ErrorCode.ST_ITEM_VERSION_CREATE),
                new Wrapped("updateItemVersion",
                        (s, space) -> s.updateItemVersion(same(CONTEXT), space.get(), eq(ITEM_ID),
                                eq(VERSION_ID), same(VERSION_DATA), eq(MODIFICATION_TIME)),
                        (a, space) -> a.updateItemVersion(CONTEXT, space, ITEM_ID, VERSION_ID,
                                VERSION_DATA, MODIFICATION_TIME),
                        ErrorCode.MD_ITEM_VERSION_UPDATE, ErrorCode.ST_ITEM_VERSION_UPDATE),
                new Wrapped("deleteItemVersion",
                        (s, space) -> s.deleteItemVersion(same(CONTEXT), space.get(), eq(ITEM_ID),
                                eq(VERSION_ID)),
                        (a, space) -> a.deleteItemVersion(CONTEXT, space, ITEM_ID, VERSION_ID),
                        ErrorCode.MD_ITEM_VERSION_DELETE, ErrorCode.ST_ITEM_VERSION_DELETE),
                new Wrapped("updateItemVersionModificationTime",
                        (s, space) -> s.updateItemVersionModificationTime(same(CONTEXT),
                                space.get(), eq(ITEM_ID), eq(VERSION_ID), eq(MODIFICATION_TIME)),
                        (a, space) -> a.updateItemVersionModificationTime(CONTEXT, space, ITEM_ID,
                                VERSION_ID, MODIFICATION_TIME),
                        ErrorCode.MD_ITEM_VERSION_UPDATE, ErrorCode.ST_ITEM_VERSION_UPDATE));
    }

    /**
     * The space argument arrives as a supplier so the Mockito matcher it produces is registered
     * after the one for the session context — matchers are matched to arguments in registration
     * order, and a plain parameter would be evaluated before the lambda body runs.
     */
    private interface StoreCall {
        Object call(StateStore store, Supplier<Space> space);
    }

    private interface AdaptorCall {
        Object call(ItemVersionStateAdaptorImpl adaptor, Space space);
    }

    private static final class Wrapped {
        private final String label;
        private final StoreCall storeCall;
        private final AdaptorCall adaptorCall;
        private final int middlewareCode;
        private final int pluginCode;

        private Wrapped(String label, StoreCall storeCall, AdaptorCall adaptorCall,
                        int middlewareCode, int pluginCode) {
            this.label = label;
            this.storeCall = storeCall;
            this.adaptorCall = adaptorCall;
            this.middlewareCode = middlewareCode;
            this.pluginCode = pluginCode;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
