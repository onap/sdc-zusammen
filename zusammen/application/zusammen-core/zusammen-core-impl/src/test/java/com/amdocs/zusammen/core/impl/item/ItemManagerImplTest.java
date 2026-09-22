/*
 * Add Copyright © 2016-2017 European Support Limited
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.adaptor.outbound.api.CollaborationAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.item.ItemStateAdaptor;
import com.amdocs.zusammen.adaptor.outbound.impl.CollaborationAdaptorImpl;
import com.amdocs.zusammen.adaptor.outbound.impl.item.ItemStateAdaptorImpl;
import com.amdocs.zusammen.core.impl.Messages;
import com.amdocs.zusammen.core.impl.TestUtils;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Item;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ItemManagerImplTest {

    private static final UserInfo USER = new UserInfo("ItemVersionManagerImplTest_user");
    private static final SessionContext context = TestUtils.createSessionContext(USER, "test");

    @Spy
    private ItemManagerImpl itemManagerImpl;
    @Mock
    private ItemStateAdaptor stateAdaptorMock = new ItemStateAdaptorImpl();
    @Mock
    private CollaborationAdaptor collaborationAdaptorMock = new CollaborationAdaptorImpl();

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(itemManagerImpl.getStateAdaptor(context)).thenReturn(stateAdaptorMock);
        when(itemManagerImpl.getCollaborationAdaptor(context)).thenReturn(collaborationAdaptorMock);
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
                .createItem(any(), any(), any(), any());
        doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock)
                .createItem(any(), any(), any());
    }

    @Test
    public void testList() {
        List<Item> retrievedItems = Arrays.asList(createItem(new Id(), "item1"), createItem(new Id(), "item2"),
                createItem(new Id(), "item3"));
        Response<List<Item>> itemListResponse = new Response<>(retrievedItems);
        doReturn(itemListResponse).when(stateAdaptorMock).listItems(context);

        Collection<Item> items = itemManagerImpl.list(context);
        Assert.assertEquals(items, retrievedItems);
    }

    @Test
    public void testIsExist() {
        Id itemId = new Id();
        doReturn(new Response<>(true)).when(stateAdaptorMock).isItemExist(context, itemId);

        boolean exist = itemManagerImpl.isExist(context, itemId);
        Assert.assertTrue(exist);
    }

    @Test
    public void testIsExistWhenNot() {
        Id itemId = new Id();
        doReturn(new Response<>(false)).when(stateAdaptorMock).isItemExist(context, itemId);
        boolean exist = itemManagerImpl.isExist(context, itemId);
        Assert.assertFalse(exist);
    }

    @Test
    public void testGet() {
        Item retrievedItem = createItem(new Id(), "item1");
        Response<Item> itemResponse = new Response<>(retrievedItem);
        doReturn(itemResponse).when(stateAdaptorMock).getItem(context, retrievedItem.getId());

        Item item = itemManagerImpl.get(context, retrievedItem.getId());
        Assert.assertEquals(item, retrievedItem);
    }

    @Test
    public void testGetNonExisting() {
        Id itemId = new Id();
        Item retrieveItem = null;
        Response<Item> itemResponse = new Response<>(retrieveItem);
        doReturn(itemResponse).when(stateAdaptorMock).getItem(context, itemId);
        Item item = itemManagerImpl.get(context, itemId);
        Assert.assertNull(item);
    }

    @Test
    public void testCreate() {
        Info info = TestUtils.createInfo("item1");
        Id itemId = itemManagerImpl.create(context, info);
        Assert.assertNotNull(itemId);

        verify(collaborationAdaptorMock).createItem(context, itemId, info);
        verify(stateAdaptorMock).createItem(eq(context), eq(itemId), eq(info), any(Date.class));
    }

    @Test(expectedExceptions = ZusammenException.class)
    public void testFailCreateWithNullId() {
        Info info = TestUtils.createInfo("item1");
        itemManagerImpl.create(context, null, info);
    }

    @Test(expectedExceptions = ZusammenException.class)
    public void testFailCreateWithExistingId() {
        Id itemId = new Id("id1");
        doReturn(new Response<>(true)).when(stateAdaptorMock).isItemExist(context, itemId);

        Info info = TestUtils.createInfo("item1");
        itemManagerImpl.create(context, itemId, info);
    }

    @Test
    public void testCreateWithId() {
        Id itemId = new Id("id1");
        doReturn(new Response<>(false)).when(stateAdaptorMock).isItemExist(context, itemId);

        Info info = TestUtils.createInfo("item1");
        Id returnedItemId = itemManagerImpl.create(context, itemId, info);
        Assert.assertEquals(returnedItemId, itemId);

        verify(collaborationAdaptorMock).createItem(context, itemId, info);
        verify(stateAdaptorMock).createItem(eq(context), eq(itemId), eq(info), any(Date.class));
    }

    @Test
    public void testUpdate() {
        Id itemId = new Id();
        Info info = TestUtils.createInfo("item1");
        doReturn(new Response<>(true)).when(stateAdaptorMock).isItemExist(context, itemId);
        //doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock).updateItem(context, itemId, info);
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
                .updateItem(eq(context), eq(itemId), eq(info), any(Date.class));
        doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock).updateItem(context, itemId, info);

        itemManagerImpl.update(context, itemId, info);

        verify(collaborationAdaptorMock).updateItem(context, itemId, info);
        verify(stateAdaptorMock).updateItem(eq(context), eq(itemId), eq(info), any(Date.class));
    }

    @Test(expectedExceptions = ZusammenException.class/*,
      expectedExceptionsMessageRegExp = ITEM_NOT_EXIST*/)
    public void testUpdateNonExisting() {
        Id itemId = new Id();
        Info info = new Info();

        doReturn(new Response<>(false)).when(stateAdaptorMock).isItemExist(context, itemId);

        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
                .updateItem(eq(context), eq(itemId), eq(info), any(Date.class));
        doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock).updateItem(context, itemId, info);

        itemManagerImpl.update(context, itemId, info);
    }

    @Test
    public void testDelete() {
        Id itemId = new Id();
        doReturn(new Response<>(true)).when(stateAdaptorMock).isItemExist(context, itemId);
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock).deleteItem(eq(context), eq(itemId));
        doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock).deleteItem(eq(context), eq(itemId));
        itemManagerImpl.delete(context, itemId);

        verify(collaborationAdaptorMock).deleteItem(context, itemId);
        verify(stateAdaptorMock).deleteItem(context, itemId);
    }

    @Test(expectedExceptions = ZusammenException.class)
    public void testDeleteNonExisting() {
        Id itemId = new Id();
        doReturn(new Response<>(false)).when(stateAdaptorMock).isItemExist(context, itemId);
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock).deleteItem(context, itemId);
        doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock).deleteItem(context, itemId);
        itemManagerImpl.delete(context, itemId);
    }

    @Test
    public void testListFailurePropagates() {
        ReturnCode cause = stateStoreFailure();
        doReturn(new Response<Collection<Item>>(cause)).when(stateAdaptorMock).listItems(context);

        TestUtils.assertWrappedFailure(TestUtils.captureFailure(() -> itemManagerImpl.list(context)),
                ErrorCode.ZU_ITEM_LIST, cause);
    }

    @Test
    public void testIsExistFailurePropagates() {
        Id itemId = new Id();
        ReturnCode cause = stateStoreFailure();
        doReturn(new Response<Boolean>(cause)).when(stateAdaptorMock).isItemExist(context, itemId);

        TestUtils.assertWrappedFailure(
                TestUtils.captureFailure(() -> itemManagerImpl.isExist(context, itemId)),
                ErrorCode.ZU_ITEM_IS_EXIST, cause);
    }

    @Test
    public void testGetFailurePropagates() {
        Id itemId = new Id();
        ReturnCode cause = stateStoreFailure();
        doReturn(new Response<Item>(cause)).when(stateAdaptorMock).getItem(context, itemId);

        TestUtils.assertWrappedFailure(
                TestUtils.captureFailure(() -> itemManagerImpl.get(context, itemId)),
                ErrorCode.ZU_ITEM_GET, cause);
    }

    @Test
    public void testCreateWithNullIdIsRejectedBeforeAnyStoreIsTouched() {
        Info info = TestUtils.createInfo("item1");

        ReturnCode returnCode =
                TestUtils.captureFailure(() -> itemManagerImpl.create(context, null, info));

        TestUtils.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_CREATE);
        Assert.assertEquals(returnCode.getMessage(), Messages.ITEM_ID_TO_CREATE_CANNOT_BE_NULL);
        verify(collaborationAdaptorMock, never()).createItem(any(), any(), any());
        verify(stateAdaptorMock, never()).createItem(any(), any(), any(), any());
    }

    @Test
    public void testCreateWithAnIdCarryingNoValueIsRejected() {
        Info info = TestUtils.createInfo("item1");

        ReturnCode returnCode = TestUtils
                .captureFailure(() -> itemManagerImpl.create(context, new Id(null), info));

        TestUtils.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_CREATE);
        Assert.assertEquals(returnCode.getMessage(), Messages.ITEM_ID_TO_CREATE_CANNOT_BE_NULL);
        verify(collaborationAdaptorMock, never()).createItem(any(), any(), any());
    }

    @Test
    public void testCreateWithExistingIdIsRejected() {
        Id itemId = new Id("id1");
        doReturn(new Response<>(true)).when(stateAdaptorMock).isItemExist(context, itemId);
        Info info = TestUtils.createInfo("item1");

        ReturnCode returnCode =
                TestUtils.captureFailure(() -> itemManagerImpl.create(context, itemId, info));

        TestUtils.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_CREATE);
        Assert.assertEquals(returnCode.getMessage(),
                String.format(Messages.ITEM_ֹID_ALREADY_EXIST, itemId));
        verify(collaborationAdaptorMock, never()).createItem(any(), any(), any());
    }

    @Test
    public void testCreateFailsWhenTheCollaborationStoreRejectsItAndTheStateStoreIsNotTouched() {
        Info info = TestUtils.createInfo("item1");
        ReturnCode cause = collaborationStoreFailure();
        doReturn(new Response<Void>(cause)).when(collaborationAdaptorMock)
                .createItem(any(), any(), any());

        TestUtils.assertWrappedFailure(
                TestUtils.captureFailure(() -> itemManagerImpl.create(context, info)),
                ErrorCode.ZU_ITEM_CREATE, cause);

        verify(stateAdaptorMock, never()).createItem(any(), any(), any(), any());
    }

    @Test
    public void testCreateFailsWhenTheStateStoreRejectsIt() {
        Info info = TestUtils.createInfo("item1");
        ReturnCode cause = stateStoreFailure();
        doReturn(new Response<Void>(cause)).when(stateAdaptorMock)
                .createItem(any(), any(), any(), any());

        TestUtils.assertWrappedFailure(
                TestUtils.captureFailure(() -> itemManagerImpl.create(context, info)),
                ErrorCode.ZU_ITEM_CREATE, cause);
    }

    @Test
    public void testUpdateOfNonExistingItemReportsItemNotExist() {
        Id itemId = new Id();
        doReturn(new Response<>(false)).when(stateAdaptorMock).isItemExist(context, itemId);

        ReturnCode returnCode = TestUtils.captureFailure(
                () -> itemManagerImpl.update(context, itemId, TestUtils.createInfo("item1")));

        TestUtils.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_DOES_NOT_EXIST);
        Assert.assertEquals(returnCode.getMessage(),
                String.format(Messages.ITEM_NOT_EXIST, itemId));
    }

    @Test
    public void testUpdateFailsWhenTheCollaborationStoreRejectsItAndTheStateStoreIsNotTouched() {
        Id itemId = new Id();
        Info info = TestUtils.createInfo("item1");
        doReturn(new Response<>(true)).when(stateAdaptorMock).isItemExist(context, itemId);
        ReturnCode cause = collaborationStoreFailure();
        doReturn(new Response<Void>(cause)).when(collaborationAdaptorMock)
                .updateItem(context, itemId, info);

        TestUtils.assertWrappedFailure(
                TestUtils.captureFailure(() -> itemManagerImpl.update(context, itemId, info)),
                ErrorCode.ZU_ITEM_UPDATE, cause);

        verify(stateAdaptorMock, never()).updateItem(any(), any(), any(), any());
    }

    @Test
    public void testUpdateFailsWhenTheStateStoreRejectsIt() {
        Id itemId = new Id();
        Info info = TestUtils.createInfo("item1");
        doReturn(new Response<>(true)).when(stateAdaptorMock).isItemExist(context, itemId);
        doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock)
                .updateItem(context, itemId, info);
        ReturnCode cause = stateStoreFailure();
        doReturn(new Response<Void>(cause)).when(stateAdaptorMock)
                .updateItem(eq(context), eq(itemId), eq(info), any(Date.class));

        TestUtils.assertWrappedFailure(
                TestUtils.captureFailure(() -> itemManagerImpl.update(context, itemId, info)),
                ErrorCode.ZU_ITEM_UPDATE, cause);
    }

    @Test
    public void testDeleteOfNonExistingItemReportsItemNotExist() {
        Id itemId = new Id();
        doReturn(new Response<>(false)).when(stateAdaptorMock).isItemExist(context, itemId);

        ReturnCode returnCode =
                TestUtils.captureFailure(() -> itemManagerImpl.delete(context, itemId));

        TestUtils.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_DOES_NOT_EXIST);
        Assert.assertEquals(returnCode.getMessage(),
                String.format(Messages.ITEM_NOT_EXIST, itemId));
    }

    @Test
    public void testDeleteFailsWhenTheCollaborationStoreRejectsItAndTheStateStoreIsNotTouched() {
        Id itemId = new Id();
        doReturn(new Response<>(true)).when(stateAdaptorMock).isItemExist(context, itemId);
        ReturnCode cause = collaborationStoreFailure();
        doReturn(new Response<Void>(cause)).when(collaborationAdaptorMock)
                .deleteItem(context, itemId);

        TestUtils.assertWrappedFailure(
                TestUtils.captureFailure(() -> itemManagerImpl.delete(context, itemId)),
                ErrorCode.ZU_ITEM_DELETE, cause);

        verify(stateAdaptorMock, never()).deleteItem(any(), any());
    }

    @Test
    public void testDeleteFailsWhenTheStateStoreRejectsIt() {
        Id itemId = new Id();
        doReturn(new Response<>(true)).when(stateAdaptorMock).isItemExist(context, itemId);
        doReturn(new Response<>(Void.TYPE)).when(collaborationAdaptorMock)
                .deleteItem(context, itemId);
        ReturnCode cause = stateStoreFailure();
        doReturn(new Response<Void>(cause)).when(stateAdaptorMock).deleteItem(context, itemId);

        TestUtils.assertWrappedFailure(
                TestUtils.captureFailure(() -> itemManagerImpl.delete(context, itemId)),
                ErrorCode.ZU_ITEM_DELETE, cause);
    }

    @Test
    public void testUpdateModificationTimeDelegatesToTheStateStore() {
        Id itemId = new Id();
        Date modificationTime = new Date();

        itemManagerImpl.updateModificationTime(context, itemId, modificationTime);

        verify(stateAdaptorMock).updateItemModificationTime(context, itemId, modificationTime);
        verify(collaborationAdaptorMock, never()).updateItem(any(), any(), any());
    }

    private ReturnCode stateStoreFailure() {
        return new ReturnCode(ErrorCode.ST_ITEM_GET, Module.ZMDP, "state store down", null);
    }

    private ReturnCode collaborationStoreFailure() {
        return new ReturnCode(ErrorCode.CL_ITEM_CREATE, Module.ZCSP, "collaboration store down",
                null);
    }

    private Item createItem(Id id, String name) {
        Item item = new Item();
        item.setId(id);
        item.setInfo(TestUtils.createInfo(name));
        return item;
    }
}