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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.core.api.item.ItemManager;
import com.amdocs.zusammen.core.api.item.ItemManagerFactory;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Item;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
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

public class ItemAdaptorImplTest {

    private static final String PRODUCTION_FACTORY =
            "com.amdocs.zusammen.core.impl.item.ItemManagerFactoryImpl";

    public static class StubItemManagerFactory extends ItemManagerFactory {

        static ItemManager manager;

        @Override
        public ItemManager createInterface(SessionContext context) {
            return manager;
        }
    }

    @Mock
    private ItemManager itemManager;

    private AutoCloseable mocks;
    private SessionContext context;
    private ItemAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        // A class literal does not run a static initialiser. AbstractComponentFactory's one-shot load of
        // every factoryConfiguration.json has to happen before registerFactory, or the first factory
        // access reloads the production mapping over the stub.
        Class.forName(ItemManagerFactory.class.getName(), true,
                ItemManagerFactory.class.getClassLoader());
        StubItemManagerFactory.manager = itemManager;
        AbstractFactoryBase.registerFactory(ItemManagerFactory.class, StubItemManagerFactory.class);
        context = AdaptorTestSupport.sessionContext("item-adaptor-user");
        adaptor = new ItemAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        AdaptorTestSupport.restoreProductionFactory(ItemManagerFactory.class, PRODUCTION_FACTORY);
        StubItemManagerFactory.manager = null;
        mocks.close();
    }

    @Test
    public void testListReturnsManagerItems() {
        Item first = item("item-1", "first");
        Item second = item("item-2", "second");
        when(itemManager.list(context)).thenReturn(Arrays.asList(first, second));

        Response<Collection<Item>> response = adaptor.list(context);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().size(), 2);
        Assert.assertEquals(response.getValue().iterator().next().getId().getValue(), "item-1");
        verify(itemManager).list(context);
    }

    @Test
    public void testListReturnsEmptyCollectionWhenNoItemsExist() {
        when(itemManager.list(context)).thenReturn(Collections.<Item>emptyList());

        Response<Collection<Item>> response = adaptor.list(context);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertTrue(response.getValue().isEmpty());
    }

    @Test
    public void testListFailureIsMappedToItemListError() {
        when(itemManager.list(context)).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.ST_ITEM_LIST, Module.ZSTM, "state store down"));

        Response<Collection<Item>> response = adaptor.list(context);

        Assert.assertFalse(response.isSuccessful());
        Assert.assertNull(response.getValue());
        ReturnCode returnCode = response.getReturnCode();
        AdaptorTestSupport.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_LIST);
        Assert.assertNull(returnCode.getMessage());
        AdaptorTestSupport.assertErrorCode(returnCode.getReturnCode(), Module.ZSTM,
                ErrorCode.ST_ITEM_LIST);
        Assert.assertEquals(returnCode.getReturnCode().getMessage(), "state store down");
    }

    @Test
    public void testGetReturnsManagerItem() {
        when(itemManager.get(context, new Id("item-7"))).thenReturn(item("item-7", "seventh"));

        Response<Item> response = adaptor.get(context, new Id("item-7"));

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getId().getValue(), "item-7");
        Assert.assertEquals(response.getValue().getInfo().getName(), "seventh");
    }

    @Test
    public void testGetFailureIsMappedToItemGetError() {
        when(itemManager.get(context, new Id("item-7"))).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.ST_ITEM_GET, Module.ZSTM, "no such item"));

        Response<Item> response = adaptor.get(context, new Id("item-7"));

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_GET);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.ST_ITEM_GET);
    }

    @Test
    public void testCreatePassesInfoToManagerAndReturnsNewId() {
        when(itemManager.create(eq(context), any(Info.class))).thenReturn(new Id("new-item"));

        Response<Id> response = adaptor.create(context, AdaptorTestSupport.info("created-item"));

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getValue(), "new-item");
        ArgumentCaptor<Info> captor = ArgumentCaptor.forClass(Info.class);
        verify(itemManager).create(eq(context), captor.capture());
        Assert.assertEquals(captor.getValue().getName(), "created-item");
        Assert.assertEquals(captor.getValue().getDescription(), "created-item-description");
        Assert.assertEquals(captor.getValue().getProperty("created-item-property"),
                "created-item-value");
    }

    @Test
    public void testCreatePassesNullInfoToManager() {
        when(itemManager.create(context, (Info) null)).thenReturn(new Id("new-item"));

        Response<Id> response = adaptor.create(context, (Info) null);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getValue(), "new-item");
        verify(itemManager).create(context, (Info) null);
    }

    @Test
    public void testCreateFailureIsMappedToItemCreateError() {
        when(itemManager.create(eq(context), any(Info.class))).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.ST_ITEM_CREATE, Module.ZSTM, "duplicate"));

        Response<Id> response = adaptor.create(context, AdaptorTestSupport.info("created-item"));

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_CREATE);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.ST_ITEM_CREATE);
    }

    @Test
    public void testCreateWithGivenIdPassesIdAndInfoToManager() {
        when(itemManager.create(eq(context), any(Id.class), any(Info.class)))
                .thenReturn(new Id("chosen-id"));

        Response<Id> response = adaptor.create(context, new Id("chosen-id"),
                AdaptorTestSupport.info("preallocated-item"));

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getValue(), "chosen-id");
        ArgumentCaptor<Id> idCaptor = ArgumentCaptor.forClass(Id.class);
        ArgumentCaptor<Info> infoCaptor = ArgumentCaptor.forClass(Info.class);
        verify(itemManager).create(eq(context), idCaptor.capture(), infoCaptor.capture());
        Assert.assertEquals(idCaptor.getValue().getValue(), "chosen-id");
        Assert.assertEquals(infoCaptor.getValue().getName(), "preallocated-item");
    }

    @Test
    public void testCreateWithGivenIdFailureIsMappedToItemCreateError() {
        when(itemManager.create(eq(context), any(Id.class), any(Info.class))).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.ST_ITEM_CREATE, Module.ZSTM, "duplicate"));

        Response<Id> response =
                adaptor.create(context, new Id("chosen-id"), AdaptorTestSupport.info("item"));

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_CREATE);
    }

    @Test
    public void testUpdatePassesIdAndInfoToManager() {
        Response<?> response =
                adaptor.update(context, new Id("item-9"), AdaptorTestSupport.info("renamed-item"));

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<Id> idCaptor = ArgumentCaptor.forClass(Id.class);
        ArgumentCaptor<Info> infoCaptor = ArgumentCaptor.forClass(Info.class);
        verify(itemManager).update(eq(context), idCaptor.capture(), infoCaptor.capture());
        Assert.assertEquals(idCaptor.getValue().getValue(), "item-9");
        Assert.assertEquals(infoCaptor.getValue().getName(), "renamed-item");
    }

    @Test
    public void testUpdateFailureIsMappedToItemUpdateError() {
        doThrow(AdaptorTestSupport.failure(ErrorCode.ST_ITEM_UPDATE, Module.ZSTM, "conflict"))
                .when(itemManager).update(eq(context), any(Id.class), any(Info.class));

        Response<?> response =
                adaptor.update(context, new Id("item-9"), AdaptorTestSupport.info("item"));

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_UPDATE);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.ST_ITEM_UPDATE);
    }

    @Test
    public void testDeletePassesIdToManager() {
        Response<?> response = adaptor.delete(context, new Id("item-9"));

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<Id> idCaptor = ArgumentCaptor.forClass(Id.class);
        verify(itemManager).delete(eq(context), idCaptor.capture());
        Assert.assertEquals(idCaptor.getValue().getValue(), "item-9");
    }

    @Test
    public void testDeleteFailureIsMappedToItemDeleteError() {
        doThrow(AdaptorTestSupport.failure(ErrorCode.ST_ITEM_DELETE, Module.ZSTM, "still in use"))
                .when(itemManager).delete(eq(context), any(Id.class));

        Response<?> response = adaptor.delete(context, new Id("item-9"));

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ITEM_DELETE);
        Assert.assertEquals(response.getReturnCode().getReturnCode().getMessage(), "still in use");
    }

    private static Item item(String id, String name) {
        Item item = new Item();
        item.setId(new Id(id));
        item.setInfo(AdaptorTestSupport.info(name));
        item.setCreationTime(new Date(0L));
        item.setModificationTime(new Date(1L));
        return item;
    }
}
