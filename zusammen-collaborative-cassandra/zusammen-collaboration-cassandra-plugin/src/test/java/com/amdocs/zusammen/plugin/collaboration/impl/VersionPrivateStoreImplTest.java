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

package com.amdocs.zusammen.plugin.collaboration.impl;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.plugin.collaboration.TestUtils;
import com.amdocs.zusammen.plugin.dao.VersionDao;
import com.amdocs.zusammen.plugin.dao.VersionSynchronizationStateRepository;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.dao.types.VersionContext;
import com.amdocs.zusammen.plugin.dao.types.VersionEntity;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class VersionPrivateStoreImplTest {

  private static final UserInfo USER = new UserInfo("VersionPrivateStoreImplTest_user");
  private static final String PRIVATE_SPACE = USER.getUserName();
  private static final SessionContext context = TestUtils.createSessionContext(USER, "test");

  private AutoCloseable mocks;

  @Mock
  private VersionDao versionDaoMock;
  @Mock
  private VersionSynchronizationStateRepository versionSyncStateRepositoryMock;
  @Spy
  private VersionPrivateStoreImpl versionPrivateStore;

  @BeforeMethod
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    doReturn(versionDaoMock).when(versionPrivateStore).getVersionDao(any());
    doReturn(versionSyncStateRepositoryMock).when(versionPrivateStore)
        .getVersionSyncStateRepository(any());
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testList() throws Exception {
    Id itemId = new Id();
    Collection<VersionEntity> versions =
        Arrays.asList(new VersionEntity(new Id()), new VersionEntity(new Id()));
    doReturn(versions).when(versionDaoMock).list(context, PRIVATE_SPACE, itemId);

    Assert.assertEquals(versionPrivateStore.list(context, itemId), versions);
  }

  @Test
  public void testGet() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    doReturn(Optional.of(version)).when(versionDaoMock)
        .get(context, PRIVATE_SPACE, itemId, version.getId());

    Optional<VersionEntity> retrieved = versionPrivateStore.get(context, itemId, version.getId());

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), version);
  }

  @Test
  public void testGetWhenNotExist() throws Exception {
    doReturn(Optional.empty()).when(versionDaoMock).get(any(), any(), any(), any());

    Assert.assertFalse(versionPrivateStore.get(context, new Id(), new Id()).isPresent());
  }

  @Test
  public void testGetSynchronizationState() throws Exception {
    Id itemId = new Id();
    Id versionId = new Id();
    SynchronizationStateEntity syncState =
        new SynchronizationStateEntity(versionId, Id.ZERO, new Date(), true);
    doReturn(Optional.of(syncState)).when(versionSyncStateRepositoryMock).get(any(), any(), any());

    Optional<SynchronizationStateEntity> retrieved =
        versionPrivateStore.getSynchronizationState(context, itemId, versionId);

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), syncState);

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .get(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    assertPrivateVersionContext(contextCaptor.getValue(), itemId);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), versionId);
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
  }

  @Test
  public void testCreate() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());

    versionPrivateStore.create(context, itemId, version);

    verify(versionDaoMock).create(same(context), eq(PRIVATE_SPACE), same(itemId), same(version));

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .create(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    assertPrivateVersionContext(contextCaptor.getValue(), itemId);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), version.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
    Assert.assertNull(syncStateCaptor.getValue().getPublishTime());
    Assert.assertTrue(syncStateCaptor.getValue().isDirty());
  }

  @Test
  public void testUpdateModificationTime() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    Date modificationTime = new Date(1000L);
    version.setModificationTime(modificationTime);

    versionPrivateStore.update(context, itemId, version);

    verify(versionDaoMock).updateModificationTime(same(context), eq(PRIVATE_SPACE), same(itemId),
        same(version.getId()), same(modificationTime));
    verifyNoInteractions(versionSyncStateRepositoryMock);
  }

  @Test
  public void testUpdatePublishTimeWhenDirty() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    Date publishTime = new Date(2000L);

    versionPrivateStore.update(context, itemId, version, publishTime, true);

    verifyNoInteractions(versionDaoMock);

    SynchronizationStateEntity updated = captureUpdatedPublishTime(itemId);
    Assert.assertEquals(updated.getId(), version.getId());
    Assert.assertEquals(updated.getRevisionId(), Id.ZERO);
    Assert.assertEquals(updated.getPublishTime(), publishTime);
    Assert.assertTrue(updated.isDirty());
  }

  @Test
  public void testUpdatePublishTimeWhenNotDirty() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());

    versionPrivateStore.update(context, itemId, version, new Date(2000L), false);

    Assert.assertFalse(captureUpdatedPublishTime(itemId).isDirty());
  }

  @Test
  public void testDelete() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());

    versionPrivateStore.delete(context, itemId, version);

    verify(versionDaoMock)
        .delete(same(context), eq(PRIVATE_SPACE), same(itemId), same(version.getId()));

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .delete(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    assertPrivateVersionContext(contextCaptor.getValue(), itemId);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), version.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
  }

  @Test
  public void testMarkAsPublished() throws Exception {
    Id itemId = new Id();
    Id versionId = new Id();
    Date publishTime = new Date(3000L);

    versionPrivateStore.markAsPublished(context, itemId, versionId, publishTime);

    SynchronizationStateEntity updated = captureUpdatedPublishTime(itemId);
    Assert.assertEquals(updated.getId(), versionId);
    Assert.assertEquals(updated.getRevisionId(), Id.ZERO);
    Assert.assertEquals(updated.getPublishTime(), publishTime);
    Assert.assertFalse(updated.isDirty());
  }

  @Test
  public void testCommitStagedCreate() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    Date publishTime = new Date(4000L);

    versionPrivateStore.commitStagedCreate(context, itemId, version, publishTime);

    verify(versionDaoMock).create(same(context), eq(PRIVATE_SPACE), same(itemId), same(version));

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .create(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), version.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), Id.ZERO);
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
    Assert.assertFalse(syncStateCaptor.getValue().isDirty());
  }

  @Test
  public void testCommitStagedUpdate() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    Date publishTime = new Date(5000L);

    versionPrivateStore.commitStagedUpdate(context, itemId, version, publishTime);

    verifyNoInteractions(versionDaoMock);

    SynchronizationStateEntity updated = captureUpdatedPublishTime(itemId);
    Assert.assertEquals(updated.getId(), version.getId());
    Assert.assertEquals(updated.getPublishTime(), publishTime);
    Assert.assertFalse(updated.isDirty());
  }

  // Pins current behaviour: version-level staged-ignore leaves the sync state NOT dirty, unlike
  // its element-level counterpart which turns dirty on. Flagged as a suspected defect, not blessed.
  @Test
  public void testCommitStagedIgnore() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    Date publishTime = new Date(6000L);

    versionPrivateStore.commitStagedIgnore(context, itemId, version, publishTime);

    verifyNoInteractions(versionDaoMock);

    SynchronizationStateEntity updated = captureUpdatedPublishTime(itemId);
    Assert.assertEquals(updated.getId(), version.getId());
    Assert.assertEquals(updated.getRevisionId(), Id.ZERO);
    Assert.assertEquals(updated.getPublishTime(), publishTime);
    Assert.assertFalse(updated.isDirty());
  }

  private SynchronizationStateEntity captureUpdatedPublishTime(Id expectedItemId) {
    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .updatePublishTime(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    assertPrivateVersionContext(contextCaptor.getValue(), expectedItemId);
    return syncStateCaptor.getValue();
  }

  private void assertPrivateVersionContext(VersionContext versionContext, Id expectedItemId) {
    Assert.assertEquals(versionContext.getSpace(), PRIVATE_SPACE);
    Assert.assertEquals(versionContext.getItemId(), expectedItemId);
  }
}
