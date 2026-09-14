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

package com.amdocs.zusammen.plugin.statestore.cassandra;

import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ItemDao;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Item;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemStateStoreTest {
  private static final String TENANT = "test";
  private static final String USER = "ItemStateStoreTest_user";
  private static final SessionContext context =
      TestUtils.createSessionContext(new UserInfo(USER), TENANT);

  @Mock
  private ItemDao itemDaoMock;
  @Mock
  private VersionStateStore versionStateStore;
  @Spy
  @InjectMocks
  private ItemStateStore itemStateStore;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(itemStateStore.getItemDao(anyObject())).thenReturn(itemDaoMock);
  }

  @Test
  public void testListItems() throws Exception {
    List<Item> retrievedItems = Arrays.asList(createItem("item1"), createItem("item2"));
    doReturn(retrievedItems).when(itemDaoMock).list(context);

    Collection<Item> items = itemStateStore.listItems(context);
    Assert.assertEquals(items, retrievedItems);
  }

  @Test
  public void testIsItemExist() throws Exception {
    Item retrievedItem = createItem("item1");
    doReturn(Optional.of(retrievedItem)).when(itemDaoMock).get(context, retrievedItem.getId());

    boolean itemExist = itemStateStore.isItemExist(context, retrievedItem.getId());
    Assert.assertTrue(itemExist);
  }

  @Test
  public void testIsItemExistWhenNot() throws Exception {
    Id itemId = new Id();
    doReturn(Optional.empty()).when(itemDaoMock).get(context, itemId);

    boolean itemExist = itemStateStore.isItemExist(context, itemId);
    Assert.assertFalse(itemExist);
  }

  @Test
  public void testGetItem() throws Exception {
    Item retrievedItem = createItem("item1");
    doReturn(Optional.of(retrievedItem)).when(itemDaoMock).get(context, retrievedItem.getId());

    Item item = itemStateStore.getItem(context, retrievedItem.getId());
    Assert.assertNotNull(item);
    Assert.assertEquals(item.getId(), retrievedItem.getId());
    Assert.assertEquals(item.getInfo(), retrievedItem.getInfo());
  }

  @Test
  public void testGetNonExistingItem() throws Exception {
    Id itemId = new Id();
    doReturn(Optional.empty()).when(itemDaoMock).get(context, itemId);

    Item item = itemStateStore.getItem(context, itemId);
    Assert.assertNull(item);
  }

  @Test
  public void testCreateItem() throws Exception {
    Item item = createItem("item1");
    Date date = new Date();
    itemStateStore.createItem(context, item.getId(), item.getInfo(), date);

    verify(itemDaoMock).create(context, item.getId(), item.getInfo(), date);
  }

  @Test
  public void testUpdateItem() throws Exception {
    Item item = createItem("item1");
    doReturn(Optional.of(item)).when(itemDaoMock).get(context, item.getId());
    Date date = new Date();

    Info updatedInfo = TestUtils.createInfo("item1 updated");
    itemStateStore.updateItem(context, item.getId(), updatedInfo, date);

    verify(itemDaoMock).update(context, item.getId(), updatedInfo, date);
  }

  @Test
  public void testDeleteItem() throws Exception {
    Id itemId = new Id();
    ItemVersion v1 = TestUtils.createItemVersion(new Id(), null, "v1");
    ItemVersion v2 = TestUtils.createItemVersion(new Id(), v1.getId(), "v2");
    ItemVersion v3 = TestUtils.createItemVersion(new Id(), v2.getId(), "v3");
    Space space = Space.PRIVATE;
    doReturn(Arrays.asList(v1, v2, v3))
        .when(versionStateStore).listItemVersions(context, space, itemId);

    itemStateStore.deleteItem(context, itemId);

    verify(versionStateStore).listItemVersions(context, space, itemId);
    verify(versionStateStore, times(3))
        .deleteItemVersion(anyObject(), anyObject(), anyObject(), anyObject());
    verify(versionStateStore).deleteItemVersion(context, Space.PRIVATE, itemId, v1.getId());
    verify(versionStateStore).deleteItemVersion(context, Space.PRIVATE, itemId, v2.getId());
    verify(versionStateStore).deleteItemVersion(context, Space.PRIVATE, itemId, v2.getId());
    verify(itemDaoMock).delete(context, itemId);
  }

  private Item createItem(String name) {
    Item item1 = new Item();
    item1.setId(new Id());
    item1.setInfo(TestUtils.createInfo(name));
    return item1;
  }
}