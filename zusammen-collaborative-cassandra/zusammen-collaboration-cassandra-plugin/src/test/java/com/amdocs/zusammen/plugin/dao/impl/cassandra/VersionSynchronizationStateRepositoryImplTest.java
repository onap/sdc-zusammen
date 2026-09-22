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

package com.amdocs.zusammen.plugin.dao.impl.cassandra;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.VersionSynchronizationStateRepositoryImpl.VersionSyncStateAccessor;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.dao.types.VersionContext;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class VersionSynchronizationStateRepositoryImplTest {

    private static final String TENANT = "VersionSyncStateRepositoryImplTest_tenant";
    private static final String USER = "VersionSyncStateRepositoryImplTest_user";
    private static final String SPACE = "VersionSyncStateRepositoryImplTest_space";
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id REVISION_ID = new Id("revision-3");
    private static final Date PUBLISH_TIME = new Date(1_700_000_000_000L);

    @Mock
    private VersionSyncStateAccessor syncStateAccessor;

    private AutoCloseable mocks;
    private SessionContext context;
    private VersionContext versionContext;
    private VersionSynchronizationStateRepositoryImpl repository;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        CassandraAccessorSeam.install();
        CassandraAccessorSeam.registerAccessor(VersionSyncStateAccessor.class, syncStateAccessor);

        context = new SessionContext();
        context.setUser(new UserInfo(USER));
        context.setTenant(TENANT);
        versionContext = new VersionContext(SPACE, ITEM_ID);
        repository = new VersionSynchronizationStateRepositoryImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        CassandraAccessorSeam.uninstall();
        mocks.close();
    }

    @Test
    public void testCreateWritesThePublishTimeOfTheVersionRevision() {
        repository.create(context, versionContext, syncState());

        verify(syncStateAccessor).updatePublishTime(PUBLISH_TIME, SPACE, "item-1", "version-2", "revision-3");
    }

    @Test
    public void testUpdatePublishTimeWritesThePublishTimeOfTheVersionRevision() {
        repository.updatePublishTime(context, versionContext, syncState());

        verify(syncStateAccessor).updatePublishTime(PUBLISH_TIME, SPACE, "item-1", "version-2", "revision-3");
    }

    @Test
    public void testListMapsEveryRevisionRowIncludingUserAndMessage() {
        doReturn(resultSetOfAll(Arrays.asList(
                revisionRow("version_id", "version-2", "revision-3", Collections.singleton("element-9")),
                revisionRow("version_id", "version-2", "revision-4", Collections.<String>emptySet()))))
                .when(syncStateAccessor).list(SPACE, "item-1", "version-2");

        List<SynchronizationStateEntity> states = repository.list(context, versionContext, syncState());

        Assert.assertEquals(states.size(), 2);
        Assert.assertEquals(states.get(0).getId(), VERSION_ID);
        Assert.assertEquals(states.get(0).getRevisionId(), REVISION_ID);
        Assert.assertEquals(states.get(0).getPublishTime(), PUBLISH_TIME);
        Assert.assertEquals(states.get(0).getUser(), "publisher");
        Assert.assertEquals(states.get(0).getMessage(), "a commit message");
        Assert.assertTrue(states.get(0).isDirty());
        Assert.assertEquals(states.get(1).getRevisionId(), new Id("revision-4"));
        Assert.assertFalse(states.get(1).isDirty());
    }

    @Test
    public void testListFallsBackToTheElementIdColumnWhenTheRowHasNoVersionId() {
        doReturn(resultSetOfAll(Collections.singletonList(
                revisionRow("element_id", "element-7", "revision-3", Collections.<String>emptySet()))))
                .when(syncStateAccessor).list(SPACE, "item-1", "version-2");

        List<SynchronizationStateEntity> states = repository.list(context, versionContext, syncState());

        Assert.assertEquals(states.get(0).getId(), new Id("element-7"));
    }

    @Test
    public void testListReturnsEmptyListWhenTheVersionHasNoRevisions() {
        doReturn(resultSetOfAll(null)).when(syncStateAccessor).list(SPACE, "item-1", "version-2");

        Assert.assertTrue(repository.list(context, versionContext, syncState()).isEmpty());
    }

    @Test
    public void testDeleteRemovesEveryRevisionOfTheVersion() {
        repository.delete(context, versionContext, syncState());

        verify(syncStateAccessor).delete(SPACE, "item-1", "version-2");
    }

    @Test
    public void testGetReportsTheVersionAsDirtyWhileDirtyElementsRemain() {
        doReturn(resultSetOf(revisionRow("version_id", "version-2", "revision-3", Collections.singleton("element-9"))))
                .when(syncStateAccessor).get(SPACE, "item-1", "version-2", "revision-3");

        Optional<SynchronizationStateEntity> retrieved = repository.get(context, versionContext, syncState());

        Assert.assertTrue(retrieved.isPresent());
        Assert.assertEquals(retrieved.get().getId(), VERSION_ID);
        Assert.assertEquals(retrieved.get().getRevisionId(), REVISION_ID);
        Assert.assertEquals(retrieved.get().getPublishTime(), PUBLISH_TIME);
        Assert.assertTrue(retrieved.get().isDirty());
    }

    @Test
    public void testGetReportsTheVersionAsCleanWhenNoDirtyElementsRemain() {
        doReturn(resultSetOf(
                revisionRow("version_id", "version-2", "revision-3", Collections.<String>emptySet())))
                .when(syncStateAccessor).get(SPACE, "item-1", "version-2", "revision-3");

        Assert.assertFalse(repository.get(context, versionContext, syncState()).get().isDirty());
    }

    @Test
    public void testGetReturnsEmptyWhenTheRevisionRowIsMissing() {
        doReturn(resultSetOf(null)).when(syncStateAccessor).get(SPACE, "item-1", "version-2", "revision-3");

        Assert.assertFalse(repository.get(context, versionContext, syncState()).isPresent());
    }

    private static SynchronizationStateEntity syncState() {
        SynchronizationStateEntity syncState = new SynchronizationStateEntity(VERSION_ID, REVISION_ID);
        syncState.setPublishTime(PUBLISH_TIME);
        return syncState;
    }

    private static Row revisionRow(String idColumn, String idValue, String revisionId, Set<String> dirtyElementIds) {
        ColumnDefinitions columnDefinitions = Mockito.mock(ColumnDefinitions.class);
        when(columnDefinitions.contains(idColumn)).thenReturn(true);
        Row row = Mockito.mock(Row.class);
        when(row.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(row.getString(idColumn)).thenReturn(idValue);
        when(row.getString("revision_id")).thenReturn(revisionId);
        when(row.getTimestamp("publish_time")).thenReturn(PUBLISH_TIME);
        when(row.getSet("dirty_element_ids", String.class)).thenReturn(new HashSet<>(dirtyElementIds));
        when(row.getString("user")).thenReturn("publisher");
        when(row.getString("message")).thenReturn("a commit message");
        return row;
    }

    private static ResultSet resultSetOf(Row row) {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.one()).thenReturn(row);
        return resultSet;
    }

    private static ResultSet resultSetOfAll(List<Row> rows) {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.all()).thenReturn(rows);
        return resultSet;
    }
}
