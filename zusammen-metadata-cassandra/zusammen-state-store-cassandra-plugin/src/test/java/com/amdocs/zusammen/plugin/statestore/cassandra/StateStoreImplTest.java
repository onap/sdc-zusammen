/*
 * Copyright © 2016 European Support Limited
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

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.sdk.state.types.StateElement;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;

public class StateStoreImplTest {
  private static final String TENANT = "test";

  private static final String USER = "StateStoreImplTest_user";
  private static final SessionContext context =
      TestUtils.createSessionContext(new UserInfo(USER), TENANT);
  private static final Space space = Space.PRIVATE;
  private static final Id itemId = new Id();
  private static final Id versionId = new Id();
  private static final ElementContext elementContext =
      TestUtils.createElementContext(itemId, versionId);
  private static final Id elementId = new Id();

  @Mock
  private ElementStateStore elementStateStoreMock;
  @Mock
  private VersionStateStore versionStateStoreMock;
  @Mock
  private ItemStateStore itemStateStoreMock;
  @InjectMocks
  private StateStoreImpl stateStore;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testListItems() throws Exception {
    stateStore.listItems(context);
    verify(itemStateStoreMock).listItems(context);
  }

  @Test
  public void testIsItemExist() throws Exception {
    stateStore.isItemExist(context, itemId);
    verify(itemStateStoreMock).isItemExist(context, itemId);
  }

  @Test
  public void testGetItem() throws Exception {
    stateStore.getItem(context, itemId);
    verify(itemStateStoreMock).getItem(context, itemId);
  }

  @Test
  public void testCreateItem() throws Exception {
    Info itemInfo = new Info();
    Date date = new Date();


    stateStore.createItem(context, itemId, itemInfo, date);

    verify(itemStateStoreMock).createItem(context, itemId, itemInfo, date);
  }

  @Test
  public void testUpdateItem() throws Exception {
    Id itemId = new Id();
    Info itemInfo = new Info();
    Date date = new Date();
    stateStore.updateItem(context, itemId, itemInfo, date);
    verify(itemStateStoreMock).updateItem(context, itemId, itemInfo, date);
  }

  @Test
  public void testDeleteItem() throws Exception {
    stateStore.deleteItem(context, itemId);
    verify(itemStateStoreMock).deleteItem(context, itemId);
  }

  @Test
  public void testListItemVersions() throws Exception {
    Space space = Space.PRIVATE;
    Id itemId = new Id();
    stateStore.listItemVersions(context, space, itemId);
    verify(versionStateStoreMock).listItemVersions(context, space, itemId);
  }

  @Test
  public void testIsItemVersionExist() throws Exception {
    stateStore.isItemVersionExist(context, space, itemId, versionId);
    verify(versionStateStoreMock).isItemVersionExist(context, space, itemId, versionId);
  }

  @Test
  public void testGetItemVersion() throws Exception {
    Space space = Space.PRIVATE;
    Id itemId = new Id();
    Id versionId = new Id();
    stateStore.getItemVersion(context, space, itemId, versionId);
    verify(versionStateStoreMock).getItemVersion(context, space, itemId, versionId);
  }

  @Test
  public void testCreateItemVersion() throws Exception {
    Id baseVersionId = new Id();
    ItemVersionData data = new ItemVersionData();
    stateStore
        .createItemVersion(context, space, itemId, baseVersionId, versionId, data, new Date());
    /*verify(versionStateStoreMock)
        .createItemVersion(context, space, itemId, baseVersionId, versionId, data, creationTime);*/
    verify(versionStateStoreMock)
        .createItemVersion(anyObject(), anyObject(), anyObject(), anyObject(), anyObject(),
            anyObject(), anyObject());
  }

  @Test
  public void testUpdateItemVersion() throws Exception {
    ItemVersionData data = new ItemVersionData();
    stateStore.updateItemVersion(context, space, itemId, versionId, data, new Date());
   /* verify(versionStateStoreMock).updateItemVersion(context, space, itemId, versionId, data,
        modificationTime);*/
    verify(versionStateStoreMock)
        .updateItemVersion(anyObject(), anyObject(), anyObject(), anyObject(), anyObject(),
            anyObject());
  }

  @Test
  public void testDeleteItemVersion() throws Exception {
    stateStore.deleteItemVersion(context, space, itemId, versionId);
    verify(versionStateStoreMock).deleteItemVersion(context, space, itemId, versionId);
  }

  @Test
  public void testListElements() throws Exception {
    stateStore.listElements(context, elementContext, elementId);
    verify(elementStateStoreMock).listElements(context, elementContext, elementId);
  }

  @Test
  public void testIsElementExist() throws Exception {
    stateStore.isElementExist(context, elementContext, elementId);
    verify(elementStateStoreMock).isElementExist(context, elementContext, elementId);
  }

  @Test
  public void testGetElement() throws Exception {
    stateStore.getElement(context, elementContext, elementId);
    verify(elementStateStoreMock).getElement(context, elementContext, elementId);
  }

  @Test
  public void testCreateElement() throws Exception {
    StateElement element = new StateElement(itemId, versionId, Namespace.ROOT_NAMESPACE, elementId);
    stateStore.createElement(context, element);
    verify(elementStateStoreMock).createElement(context, element);
  }

  @Test
  public void testUpdateElement() throws Exception {
    StateElement element = new StateElement(itemId, versionId, Namespace.ROOT_NAMESPACE, elementId);
    stateStore.updateElement(context, element);
    verify(elementStateStoreMock).updateElement(context, element);
  }

  @Test
  public void testDeleteElement() throws Exception {
    StateElement element = new StateElement(itemId, versionId, Namespace.ROOT_NAMESPACE, elementId);
    stateStore.deleteElement(context, element);
    verify(elementStateStoreMock).deleteElement(context, element);
  }
}