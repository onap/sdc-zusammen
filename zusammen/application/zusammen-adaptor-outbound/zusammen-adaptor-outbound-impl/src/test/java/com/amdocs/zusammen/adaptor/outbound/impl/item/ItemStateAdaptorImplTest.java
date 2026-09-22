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
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.info;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.pluginReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.successfulResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

import com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Item;
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

public class ItemStateAdaptorImplTest {

    private static final SessionContext CONTEXT = new SessionContext();
    private static final Id ITEM_ID = new Id("item-id");
    private static final Date CREATION_TIME = new Date(1_000_000L);
    private static final Date MODIFICATION_TIME = new Date(2_000_000L);

    @Mock
    private StateStore stateStore;

    private AutoCloseable mocks;
    private ItemStateAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        OutboundTestSupport.installStateStore(stateStore);
        adaptor = new ItemStateAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OutboundTestSupport.restoreStateStore();
        mocks.close();
    }

    @Test
    public void testStateStoreIsResolvedWithTheCallersSessionContext() {
        Mockito.when(stateStore.deleteItem(any(), any())).thenReturn(emptySuccess());

        adaptor.deleteItem(CONTEXT, ITEM_ID);

        Assert.assertSame(OutboundTestSupport.stateStoreResolutionContext(), CONTEXT);
    }

    @Test(dataProvider = "methods")
    public void testStateStoreResponseIsReturnedUnchangedOnSuccess(Wrapped method) {
        Response<Void> storeResponse = emptySuccess();
        Mockito.when(method.storeCall.call(stateStore)).thenReturn(storeResponse);

        Assert.assertSame(method.adaptorCall.call(adaptor), storeResponse,
                method.label + " must hand the plugin response back untouched");
    }

    @Test(dataProvider = "methods")
    public void testUnsuccessfulStateStoreResponseIsNestedInMiddlewareReturnCode(Wrapped method) {
        ReturnCode storeReturnCode = pluginReturnCode("state store refused");
        Mockito.when(method.storeCall.call(stateStore))
                .thenReturn(failedResponse(storeReturnCode));

        Response<?> response = (Response<?>) method.adaptorCall.call(adaptor);

        Assert.assertFalse(response.isSuccessful(), method.label + " must report the failure");
        assertReturnCode(response.getReturnCode(), Module.ZSTM, method.middlewareCode, null);
        Assert.assertSame(response.getReturnCode().getReturnCode(), storeReturnCode,
                method.label + " must keep the plugin's own return code as the cause");
    }

    @Test(dataProvider = "methods")
    public void testStateStoreExceptionIsMappedToAMetadataPluginReturnCode(Wrapped method) {
        Mockito.when(method.storeCall.call(stateStore))
                .thenThrow(new IllegalStateException("state store unreachable"));

        Response<?> response = (Response<?>) method.adaptorCall.call(adaptor);

        Assert.assertFalse(response.isSuccessful(), method.label + " must report the failure");
        assertReturnCode(response.getReturnCode(), Module.ZSTM, method.middlewareCode, null);
        assertReturnCode(response.getReturnCode().getReturnCode(), Module.ZMDP, method.pluginCode,
                "state store unreachable");
        Assert.assertNull(response.getReturnCode().getReturnCode().getReturnCode());
    }

    @Test
    public void testListItemsHandsBackThePluginsItems() {
        Item item = new Item();
        item.setId(ITEM_ID);
        Response<Collection<Item>> storeResponse =
                successfulResponse(Collections.<Item>singletonList(item));
        Mockito.when(stateStore.listItems(any())).thenReturn(storeResponse);

        Response<Collection<Item>> response = adaptor.listItems(CONTEXT);

        Mockito.verify(stateStore).listItems(same(CONTEXT));
        Assert.assertEquals(response.getValue().size(), 1);
        Assert.assertSame(response.getValue().iterator().next(), item);
    }

    @Test
    public void testIsItemExistHandsBackThePluginsAnswer() {
        Response<Boolean> storeResponse = successfulResponse(Boolean.FALSE);
        Mockito.when(stateStore.isItemExist(any(), any())).thenReturn(storeResponse);

        Response<Boolean> response = adaptor.isItemExist(CONTEXT, ITEM_ID);

        Mockito.verify(stateStore).isItemExist(same(CONTEXT), eq(ITEM_ID));
        Assert.assertEquals(response.getValue(), Boolean.FALSE);
    }

    @Test
    public void testGetItemForwardsTheItemIdAndHandsBackThePluginsItem() {
        Item item = new Item();
        item.setInfo(info("stored-item"));
        Mockito.when(stateStore.getItem(any(), any())).thenReturn(successfulResponse(item));

        Response<Item> response = adaptor.getItem(CONTEXT, ITEM_ID);

        Mockito.verify(stateStore).getItem(same(CONTEXT), eq(ITEM_ID));
        Assert.assertSame(response.getValue(), item);
    }

    @Test
    public void testCreateItemForwardsInfoAndCreationTime() {
        Mockito.when(stateStore.createItem(any(), any(), any(), any())).thenReturn(emptySuccess());

        adaptor.createItem(CONTEXT, ITEM_ID, info("fresh-item"), CREATION_TIME);

        ArgumentCaptor<Info> infoCaptor = ArgumentCaptor.forClass(Info.class);
        Mockito.verify(stateStore).createItem(same(CONTEXT), eq(ITEM_ID), infoCaptor.capture(),
                eq(CREATION_TIME));
        Assert.assertEquals(infoCaptor.getValue().getName(), "fresh-item");
    }

    @Test
    public void testUpdateItemForwardsInfoAndModificationTime() {
        Mockito.when(stateStore.updateItem(any(), any(), any(), any())).thenReturn(emptySuccess());

        adaptor.updateItem(CONTEXT, ITEM_ID, info("renamed-item"), MODIFICATION_TIME);

        ArgumentCaptor<Info> infoCaptor = ArgumentCaptor.forClass(Info.class);
        Mockito.verify(stateStore).updateItem(same(CONTEXT), eq(ITEM_ID), infoCaptor.capture(),
                eq(MODIFICATION_TIME));
        Assert.assertEquals(infoCaptor.getValue().getName(), "renamed-item");
    }

    @Test
    public void testUpdateItemModificationTimeTouchesTheTimestampWithoutRewritingTheInfo() {
        Mockito.when(stateStore.updateItemModificationTime(any(), any(), any()))
                .thenReturn(emptySuccess());

        adaptor.updateItemModificationTime(CONTEXT, ITEM_ID, MODIFICATION_TIME);

        Mockito.verify(stateStore).updateItemModificationTime(same(CONTEXT), eq(ITEM_ID),
                eq(MODIFICATION_TIME));
        Mockito.verify(stateStore, Mockito.never()).updateItem(any(), any(), any(), any());
    }

    @DataProvider(name = "methods")
    public static Object[][] methods() {
        List<Object[]> rows = new ArrayList<>();
        for (Wrapped method : wrappedMethods()) {
            rows.add(new Object[] {method});
        }
        return rows.toArray(new Object[0][]);
    }

    /**
     * The error-mapping table of {@link ItemStateAdaptorImpl}: for every method, which middleware
     * ({@link Module#ZSTM}) and plugin ({@link Module#ZMDP}) code the failure paths must produce.
     */
    private static List<Wrapped> wrappedMethods() {
        return Arrays.asList(
                new Wrapped("listItems", s -> s.listItems(any()),
                        a -> a.listItems(CONTEXT),
                        ErrorCode.MD_ITEM_LIST, ErrorCode.ST_ITEM_LIST),
                new Wrapped("isItemExist", s -> s.isItemExist(any(), any()),
                        a -> a.isItemExist(CONTEXT, ITEM_ID),
                        ErrorCode.MD_ITEM_IS_EXIST, ErrorCode.ST_ITEM_IS_EXIST),
                new Wrapped("getItem", s -> s.getItem(any(), any()),
                        a -> a.getItem(CONTEXT, ITEM_ID),
                        ErrorCode.MD_ITEM_GET, ErrorCode.ST_ITEM_GET),
                new Wrapped("createItem", s -> s.createItem(any(), any(), any(), any()),
                        a -> a.createItem(CONTEXT, ITEM_ID, info("item"), CREATION_TIME),
                        ErrorCode.MD_ITEM_CREATE, ErrorCode.ST_ITEM_CREATE),
                new Wrapped("updateItem", s -> s.updateItem(any(), any(), any(), any()),
                        a -> a.updateItem(CONTEXT, ITEM_ID, info("item"), MODIFICATION_TIME),
                        ErrorCode.MD_ITEM_UPDATE, ErrorCode.ST_ITEM_UPDATE),
                new Wrapped("deleteItem", s -> s.deleteItem(any(), any()),
                        a -> a.deleteItem(CONTEXT, ITEM_ID),
                        ErrorCode.MD_ITEM_DELETE, ErrorCode.ST_ITEM_DELETE),
                new Wrapped("updateItemModificationTime",
                        s -> s.updateItemModificationTime(any(), any(), any()),
                        a -> a.updateItemModificationTime(CONTEXT, ITEM_ID, MODIFICATION_TIME),
                        ErrorCode.MD_ITEM_UPDATE, ErrorCode.ST_ITEM_UPDATE));
    }

    private interface StoreCall {
        Object call(StateStore store);
    }

    private interface AdaptorCall {
        Object call(ItemStateAdaptorImpl adaptor);
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
