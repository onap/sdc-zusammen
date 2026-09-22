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
import com.amdocs.zusammen.plugin.ZusammenPluginConstants;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class VersionPublicStoreImplTest {

  private static final UserInfo USER = new UserInfo("VersionPublicStoreImplTest_user");
  private static final String PUBLIC_SPACE = ZusammenPluginConstants.PUBLIC_SPACE;
  private static final SessionContext context = TestUtils.createSessionContext(USER, "test");

  private AutoCloseable mocks;

  @Mock
  private VersionDao versionDaoMock;
  @Mock
  private VersionSynchronizationStateRepository versionSyncStateRepositoryMock;
  @Spy
  private VersionPublicStoreImpl versionPublicStore;

  @BeforeMethod
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    doReturn(versionDaoMock).when(versionPublicStore).getVersionDao(any());
    doReturn(versionSyncStateRepositoryMock).when(versionPublicStore)
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
    doReturn(versions).when(versionDaoMock).list(context, PUBLIC_SPACE, itemId);

    Assert.assertEquals(versionPublicStore.list(context, itemId), versions);
  }

  @Test
  public void testGet() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    doReturn(Optional.of(version)).when(versionDaoMock)
        .get(context, PUBLIC_SPACE, itemId, version.getId());

    Optional<VersionEntity> retrieved = versionPublicStore.get(context, itemId, version.getId());

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), version);
  }

  @Test
  public void testGetWhenNotExist() throws Exception {
    doReturn(Optional.empty()).when(versionDaoMock).get(any(), any(), any(), any());

    Assert.assertFalse(versionPublicStore.get(context, new Id(), new Id()).isPresent());
  }

  @Test
  public void testListSynchronizationStatesIsOrderedByPublishTimeDescending() throws Exception {
    Id itemId = new Id();
    Id versionId = new Id();
    SynchronizationStateEntity oldest = syncState(new Id("r1"), 1000L);
    SynchronizationStateEntity middle = syncState(new Id("r2"), 2000L);
    SynchronizationStateEntity newest = syncState(new Id("r3"), 3000L);
    doReturn(new ArrayList<>(Arrays.asList(oldest, newest, middle)))
        .when(versionSyncStateRepositoryMock).list(any(), any(), any());

    List<SynchronizationStateEntity> syncStates =
        versionPublicStore.listSynchronizationStates(context, itemId, versionId);

    Assert.assertEquals(syncStates, Arrays.asList(newest, middle, oldest));

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .list(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    Assert.assertEquals(contextCaptor.getValue().getSpace(), PUBLIC_SPACE);
    Assert.assertEquals(contextCaptor.getValue().getItemId(), itemId);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), versionId);
    Assert.assertNull(syncStateCaptor.getValue().getRevisionId());
  }

  @Test
  public void testListSynchronizationStatesWhenNonePublished() throws Exception {
    doReturn(new ArrayList<SynchronizationStateEntity>())
        .when(versionSyncStateRepositoryMock).list(any(), any(), any());

    Assert.assertTrue(versionPublicStore
        .listSynchronizationStates(context, new Id(), new Id()).isEmpty());
  }

  @Test
  public void testGetSynchronizationStateOfGivenRevision() throws Exception {
    Id itemId = new Id();
    Id versionId = new Id();
    Id revisionId = new Id("revision");
    SynchronizationStateEntity syncState = syncState(revisionId, 1000L);
    doReturn(Optional.of(syncState)).when(versionSyncStateRepositoryMock).get(any(), any(), any());

    Optional<SynchronizationStateEntity> retrieved =
        versionPublicStore.getSynchronizationState(context, itemId, versionId, revisionId);

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), syncState);
    verify(versionSyncStateRepositoryMock, never()).list(any(), any(), any());

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .get(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    Assert.assertEquals(contextCaptor.getValue().getSpace(), PUBLIC_SPACE);
    Assert.assertEquals(contextCaptor.getValue().getItemId(), itemId);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), versionId);
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), revisionId);
  }

  @Test
  public void testGetSynchronizationStateWithoutRevisionUsesLatestPublished() throws Exception {
    Id latestRevisionId = new Id("latest");
    doReturn(new ArrayList<>(Arrays.asList(syncState(new Id("older"), 1000L),
        syncState(latestRevisionId, 5000L)))).when(versionSyncStateRepositoryMock)
        .list(any(), any(), any());
    doReturn(Optional.of(syncState(latestRevisionId, 5000L)))
        .when(versionSyncStateRepositoryMock).get(any(), any(), any());

    Id versionId = new Id();
    Optional<SynchronizationStateEntity> retrieved =
        versionPublicStore.getSynchronizationState(context, new Id(), versionId, null);

    Assert.assertTrue(retrieved.isPresent());

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock).get(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), versionId);
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), latestRevisionId);
  }

  @Test
  public void testGetSynchronizationStateWithoutRevisionWhenNeverPublished() throws Exception {
    doReturn(new ArrayList<SynchronizationStateEntity>())
        .when(versionSyncStateRepositoryMock).list(any(), any(), any());

    Assert.assertFalse(versionPublicStore
        .getSynchronizationState(context, new Id(), new Id(), null).isPresent());
    verify(versionSyncStateRepositoryMock, never()).get(any(), any(), any());
  }

  @Test
  public void testCreate() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    Id revisionId = new Id("revision");
    Map<Id, Id> versionElementIds = new HashMap<>();
    versionElementIds.put(new Id("element"), new Id("elementRevision"));
    Date publishTime = new Date(7000L);

    versionPublicStore.create(context, itemId, version, revisionId, versionElementIds, publishTime,
        "first publish");

    verify(versionDaoMock).create(same(context), eq(PUBLIC_SPACE), same(itemId), same(version));
    verify(versionDaoMock).createVersionElements(same(context), eq(PUBLIC_SPACE), same(itemId),
        same(version.getId()), same(revisionId), same(versionElementIds), same(publishTime),
        eq("first publish"));

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .create(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    Assert.assertEquals(contextCaptor.getValue().getSpace(), PUBLIC_SPACE);
    Assert.assertEquals(contextCaptor.getValue().getItemId(), itemId);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), version.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), revisionId);
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
    Assert.assertFalse(syncStateCaptor.getValue().isDirty());
  }

  @Test
  public void testUpdateStampsModificationTimeWithPublishTime() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    version.setModificationTime(new Date(1L));
    Id revisionId = new Id("revision");
    Map<Id, Id> versionElementIds = Collections.emptyMap();
    Date publishTime = new Date(8000L);

    versionPublicStore.update(context, itemId, version, revisionId, versionElementIds, publishTime,
        "next publish");

    verify(versionDaoMock).updateModificationTime(same(context), eq(PUBLIC_SPACE), same(itemId),
        same(version.getId()), same(publishTime));
    verify(versionDaoMock).createVersionElements(same(context), eq(PUBLIC_SPACE), same(itemId),
        same(version.getId()), same(revisionId), same(versionElementIds), same(publishTime),
        eq("next publish"));
    verify(versionDaoMock, never()).create(any(), any(), any(), any());

    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .updatePublishTime(same(context), any(), syncStateCaptor.capture());
    Assert.assertEquals(syncStateCaptor.getValue().getId(), version.getId());
    Assert.assertEquals(syncStateCaptor.getValue().getRevisionId(), revisionId);
    Assert.assertEquals(syncStateCaptor.getValue().getPublishTime(), publishTime);
    Assert.assertFalse(syncStateCaptor.getValue().isDirty());
  }

  @Test
  public void testDeleteRemovesSyncStateOfAllRevisions() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());

    versionPublicStore.delete(context, itemId, version);

    verify(versionDaoMock)
        .delete(same(context), eq(PUBLIC_SPACE), same(itemId), same(version.getId()));

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    ArgumentCaptor<SynchronizationStateEntity> syncStateCaptor =
        ArgumentCaptor.forClass(SynchronizationStateEntity.class);
    verify(versionSyncStateRepositoryMock)
        .delete(same(context), contextCaptor.capture(), syncStateCaptor.capture());
    Assert.assertEquals(contextCaptor.getValue().getSpace(), PUBLIC_SPACE);
    Assert.assertEquals(syncStateCaptor.getValue().getId(), version.getId());
    Assert.assertNull(syncStateCaptor.getValue().getRevisionId());
  }

  @Test
  public void testCheckHealthWhenHealthy() throws Exception {
    doReturn(true).when(versionDaoMock).checkHealth(context);

    Assert.assertTrue(versionPublicStore.checkHealth(context));
  }

  @Test
  public void testCheckHealthWhenUnhealthy() throws Exception {
    doReturn(false).when(versionDaoMock).checkHealth(context);

    Assert.assertFalse(versionPublicStore.checkHealth(context));
  }

  private static SynchronizationStateEntity syncState(Id revisionId, long publishTime) {
    return new SynchronizationStateEntity(new Id("version_" + revisionId.getValue()), revisionId,
        new Date(publishTime), false);
  }
}
