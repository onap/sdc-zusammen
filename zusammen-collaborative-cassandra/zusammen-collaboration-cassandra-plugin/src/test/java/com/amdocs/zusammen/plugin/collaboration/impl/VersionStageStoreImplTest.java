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
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.plugin.collaboration.TestUtils;
import com.amdocs.zusammen.plugin.dao.VersionStageRepository;
import com.amdocs.zusammen.plugin.dao.types.StageEntity;
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

import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class VersionStageStoreImplTest {

  private static final UserInfo USER = new UserInfo("VersionStageStoreImplTest_user");
  private static final String PRIVATE_SPACE = USER.getUserName();
  private static final SessionContext context = TestUtils.createSessionContext(USER, "test");

  private AutoCloseable mocks;

  @Mock
  private VersionStageRepository versionStageRepositoryMock;
  @Spy
  private VersionStageStoreImpl versionStageStore;

  @BeforeMethod
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    // doReturn, not when(spy...): the latter runs the real getter, and resolving
    // VersionStageRepositoryFactory loads VersionStageRepositoryImpl, whose static initializer
    // registers Cassandra codecs and fails without a cassandra.keyspace property.
    doReturn(versionStageRepositoryMock).when(versionStageStore).getVersionStageRepository(any());
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testGet() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());
    StageEntity<VersionEntity> stage = new StageEntity<>(version, new Date());
    doReturn(Optional.of(stage)).when(versionStageRepositoryMock).get(any(), any(), any());

    Optional<StageEntity<VersionEntity>> retrieved =
        versionStageStore.get(context, itemId, version);

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), stage);

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    verify(versionStageRepositoryMock).get(same(context), contextCaptor.capture(), same(version));
    Assert.assertEquals(contextCaptor.getValue().getSpace(), PRIVATE_SPACE);
    Assert.assertEquals(contextCaptor.getValue().getItemId(), itemId);
  }

  @Test
  public void testGetWhenNotStaged() throws Exception {
    doReturn(Optional.empty()).when(versionStageRepositoryMock).get(any(), any(), any());

    Optional<StageEntity<VersionEntity>> retrieved =
        versionStageStore.get(context, new Id(), new VersionEntity(new Id()));

    Assert.assertFalse(retrieved.isPresent());
  }

  @Test
  public void testCreate() throws Exception {
    Id itemId = new Id();
    StageEntity<VersionEntity> stage =
        new StageEntity<>(new VersionEntity(new Id()), new Date(), Action.CREATE, false);

    versionStageStore.create(context, itemId, stage);

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    verify(versionStageRepositoryMock).create(same(context), contextCaptor.capture(), same(stage));
    Assert.assertEquals(contextCaptor.getValue().getSpace(), PRIVATE_SPACE);
    Assert.assertEquals(contextCaptor.getValue().getItemId(), itemId);
  }

  @Test
  public void testDelete() throws Exception {
    Id itemId = new Id();
    VersionEntity version = new VersionEntity(new Id());

    versionStageStore.delete(context, itemId, version);

    ArgumentCaptor<VersionContext> contextCaptor = ArgumentCaptor.forClass(VersionContext.class);
    verify(versionStageRepositoryMock)
        .delete(same(context), contextCaptor.capture(), same(version));
    Assert.assertEquals(contextCaptor.getValue().getSpace(), PRIVATE_SPACE);
    Assert.assertEquals(contextCaptor.getValue().getItemId(), itemId);
  }
}
