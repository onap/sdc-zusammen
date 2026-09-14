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

import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntity;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ElementRepository;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.VersionDao;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class VersionStateStoreTest {
  private static final String TENANT = "test";
  private static final String USER = "ItemStateStoreTest_user";
  private static final SessionContext context =
      TestUtils.createSessionContext(new UserInfo(USER), TENANT);

  @Mock
  private VersionDao versionDaoMock;
  @Mock
  private ElementRepository elementRepositoryMock;
  @Spy
  @InjectMocks
  private VersionStateStore versionStateStore;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(versionStateStore.getVersionDao(anyObject())).thenReturn(versionDaoMock);
    when(versionStateStore.getElementRepository(anyObject())).thenReturn(elementRepositoryMock);
  }

  @Test
  public void testListPrivateItemVersions() throws Exception {
    testListItemVersions(Space.PRIVATE, USER);
  }

  @Test
  public void testListPublicItemVersions() throws Exception {
    testListItemVersions(Space.PUBLIC, StateStoreConstants.PUBLIC_SPACE);
  }

  @Test
  public void testIsPrivateItemVersionExist() throws Exception {
    testIsItemVersionExist(Space.PRIVATE, USER);
  }

  @Test
  public void testIsPublicItemVersionExist() throws Exception {
    testIsItemVersionExist(Space.PUBLIC, StateStoreConstants.PUBLIC_SPACE);
  }

  @Test
  public void testIsItemVersionExistWhenNot() throws Exception {
    Id itemId = new Id();
    Id versionId = new Id();
    doReturn(Optional.empty()).when(versionDaoMock).get(context, USER, itemId, versionId);

    boolean itemExist =
        versionStateStore.isItemVersionExist(context, Space.PRIVATE, itemId, versionId);
    Assert.assertFalse(itemExist);
  }

  @Test
  public void testGetPrivateItemVersion() throws Exception {
    testGetItemVersion(Space.PRIVATE, USER);
  }

  @Test
  public void testGetPublicItemVersion() throws Exception {
    testGetItemVersion(Space.PUBLIC, StateStoreConstants.PUBLIC_SPACE);
  }


  @Test
  public void testGetNonExistingItemVersion() throws Exception {
    Id itemId = new Id();
    Id versionId = new Id();
    doReturn(Optional.empty()).when(versionDaoMock).get(context, USER, itemId, versionId);

    ItemVersion itemVersion =
        versionStateStore.getItemVersion(context, Space.PRIVATE, itemId, versionId);
    Assert.assertNull(itemVersion);
  }

  @Test
  public void testCreatePrivateItemVersion() throws Exception {
    testCreateItemVersion(Space.PRIVATE, USER, null);
  }

  @Test
  public void testCreatePrivateItemVersionBasedOn() throws Exception {
    testCreateItemVersion(Space.PRIVATE, USER, new Id());
  }

  @Test
  public void testCreatePublicItemVersion() throws Exception {
    testCreateItemVersion(Space.PUBLIC, StateStoreConstants.PUBLIC_SPACE, null);
  }

  @Test
  public void testCreatePublicItemVersionBasedOn() throws Exception {
    testCreateItemVersion(Space.PUBLIC, StateStoreConstants.PUBLIC_SPACE, new Id());
  }

  @Test
  public void testUpdatePrivateItemVersion() throws Exception {
    testUpdateItemVersion(Space.PRIVATE, USER);
  }

  @Test
  public void testUpdatePublicItemVersion() throws Exception {
    testUpdateItemVersion(Space.PUBLIC, StateStoreConstants.PUBLIC_SPACE);
  }

  @Test
  public void testDeletePrivateItemVersion() throws Exception {
    testDeleteItemVersion(Space.PRIVATE, USER);
  }

  @Test
  public void testDeletePublicItemVersion() throws Exception {
    testDeleteItemVersion(Space.PUBLIC, StateStoreConstants.PUBLIC_SPACE);
  }

  private void testIsItemVersionExist(Space space, String spaceName) {
    Id itemId = new Id();
    ItemVersion retrievedVersion = TestUtils.createItemVersion(new Id(), null, "v1");
    doReturn(Optional.of(retrievedVersion)).when(versionDaoMock)
        .get(context, spaceName, itemId, retrievedVersion.getId());

    boolean itemExist =
        versionStateStore.isItemVersionExist(context, space, itemId, retrievedVersion.getId());
    Assert.assertTrue(itemExist);
  }

  private void testGetItemVersion(Space space, String spaceName) throws Exception {
    Id itemId = new Id();
    ItemVersion retrievedVersion = TestUtils.createItemVersion(new Id(), null, "v1");
    doReturn(Optional.of(retrievedVersion)).when(versionDaoMock)
        .get(context, spaceName, itemId, retrievedVersion.getId());

    ItemVersion itemVersion =
        versionStateStore.getItemVersion(context, space, itemId, retrievedVersion.getId());
    Assert.assertEquals(itemVersion, retrievedVersion);
  }

  private void testListItemVersions(Space space, String spaceName) {
    Id itemId = new Id();
    ItemVersion v1 = TestUtils.createItemVersion(new Id(), null, "v1");
    ItemVersion v2 = TestUtils.createItemVersion(new Id(), v1.getId(), "v2");
    ItemVersion v3 = TestUtils.createItemVersion(new Id(), v2.getId(), "v3");
    List<ItemVersion> retrievedVersions = Arrays.asList(v1, v2, v3);
    doReturn(retrievedVersions).when(versionDaoMock).list(context, spaceName, itemId);

    Collection<ItemVersion> itemVersions =
        versionStateStore.listItemVersions(context, space, itemId);
    Assert.assertEquals(itemVersions, retrievedVersions);
  }

  private void testCreateItemVersion(Space space, String spaceName, Id baseId) {
    Id itemId = new Id();
    ItemVersion v1 = TestUtils.createItemVersion(new Id(), baseId, "v1");
    List<ElementEntity> baseVersionElements = mockVersionElements(spaceName, itemId, baseId);

    versionStateStore
        .createItemVersion(context, space, itemId, baseId, v1.getId(), v1.getData(), new Date());

    /*verify(versionDaoMock)
        .create(context, spaceName, itemId, baseId, v1.getId(), v1.getData(), creationTime);*/
    verify(versionDaoMock)
        .create(anyObject(),anyObject(),anyObject(),anyObject(),anyObject(),anyObject(),anyObject());

    if (baseId != null) {
      baseVersionElements.forEach(element ->
          verify(elementRepositoryMock).create(eq(context),
              eq(new ElementEntityContext(spaceName, itemId, v1.getId())),
              eq(element)));
    } else {
      verifyZeroInteractions(elementRepositoryMock);
    }
  }

  private void testUpdateItemVersion(Space space, String spaceName) {
    Id itemId = new Id();
    ItemVersion retrievedVersion = TestUtils.createItemVersion(new Id(), null, "v1");
    doReturn(Optional.of(retrievedVersion)).when(versionDaoMock)
        .get(context, spaceName, itemId, retrievedVersion.getId());

    ItemVersionData updatedData = new ItemVersionData();
    updatedData.setInfo(TestUtils.createInfo("v1 updated"));
    updatedData.setRelations(
        Arrays.asList(new Relation(), new Relation(), new Relation(), new Relation()));
    versionStateStore.updateItemVersion(
        context, space, itemId, retrievedVersion.getId(), updatedData, new Date());

    /*verify(versionDaoMock)
        .update(context, spaceName, itemId, retrievedVersion.getId(), updatedData, modificationTime);*/
    verify(versionDaoMock)
        .update(anyObject(),anyObject(),anyObject(),anyObject(),anyObject(),anyObject());

  }

  private void testDeleteItemVersion(Space space, String spaceName) {
    Id itemId = new Id();
    Id versionId = new Id();

    List<ElementEntity> versionElements = mockVersionElements(spaceName, itemId, versionId);
    versionStateStore.deleteItemVersion(context, space, itemId, versionId);

    versionElements.forEach(element ->
        verify(elementRepositoryMock).delete(eq(context),
            eq(new ElementEntityContext(spaceName, itemId, versionId)),
            eq(element)));
    verify(versionDaoMock).delete(context, spaceName, itemId, versionId);
  }

  private List<ElementEntity> mockVersionElements(String spaceName, Id itemId, Id versionId) {
    ElementEntity elm1 = new ElementEntity(new Id());
    ElementEntity elm2 = new ElementEntity(new Id());
    List<ElementEntity> baseVersionElements = Arrays.asList(elm1, elm2);
    doReturn(baseVersionElements).when(elementRepositoryMock)
        .list(eq(context), eq(new ElementEntityContext(spaceName, itemId, versionId)));
    return baseVersionElements;
  }
}