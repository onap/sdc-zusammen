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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementSynchronizationStateRepositoryImpl.ElementSynchronizationStateAccessor;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementSynchronizationStateRepositoryImpl.VersionElementsAccessor;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ElementSynchronizationStateRepositoryImplTest {

    private static final String TENANT = "ElementSyncStateRepositoryImplTest_tenant";
    private static final String USER = "ElementSyncStateRepositoryImplTest_user";
    private static final String SPACE = "ElementSyncStateRepositoryImplTest_space";
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id VERSION_REVISION_ID = new Id("version-revision-3");
    private static final Id ELEMENT_ID = new Id("element-4");
    private static final Id ELEMENT_REVISION_ID = new Id("element-revision-5");
    private static final Date PUBLISH_TIME = new Date(1_700_000_000_000L);

    @Mock
    private ElementSynchronizationStateAccessor syncStateAccessor;
    @Mock
    private VersionElementsAccessor versionElementsAccessor;

    private AutoCloseable mocks;
    private SessionContext context;
    private ElementEntityContext elementContext;
    private ElementSynchronizationStateRepositoryImpl repository;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        CassandraAccessorSeam.install();
        CassandraAccessorSeam.registerAccessor(ElementSynchronizationStateAccessor.class, syncStateAccessor);
        CassandraAccessorSeam.registerAccessor(VersionElementsAccessor.class, versionElementsAccessor);

        context = new SessionContext();
        context.setUser(new UserInfo(USER));
        context.setTenant(TENANT);
        elementContext = new ElementEntityContext(SPACE, ITEM_ID, VERSION_ID, VERSION_REVISION_ID);
        repository = new ElementSynchronizationStateRepositoryImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        CassandraAccessorSeam.uninstall();
        mocks.close();
    }

    @Test
    public void testListMapsEveryRowToASynchronizationState() {
        doReturn(resultSetOfAll(Arrays.asList(syncStateRow("element-4", "element-revision-5", PUBLISH_TIME, true),
                syncStateRow("element-7", "element-revision-8", null, false))))
                .when(syncStateAccessor).list(SPACE, "item-1", "version-2");

        Collection<SynchronizationStateEntity> states = repository.list(context, elementContext);

        Assert.assertEquals(states.size(), 2);
        SynchronizationStateEntity dirty = findById(states, ELEMENT_ID);
        Assert.assertEquals(dirty.getRevisionId(), ELEMENT_REVISION_ID);
        Assert.assertEquals(dirty.getPublishTime(), PUBLISH_TIME);
        Assert.assertTrue(dirty.isDirty());

        SynchronizationStateEntity clean = findById(states, new Id("element-7"));
        Assert.assertEquals(clean.getRevisionId(), new Id("element-revision-8"));
        Assert.assertNull(clean.getPublishTime());
        Assert.assertFalse(clean.isDirty());
    }

    @Test
    public void testListReturnsEmptyCollectionWhenTheQueryYieldsNoRows() {
        doReturn(resultSetOfAll(null)).when(syncStateAccessor).list(SPACE, "item-1", "version-2");

        Assert.assertTrue(repository.list(context, elementContext).isEmpty());
    }

    @Test
    public void testDeleteAllDropsEveryElementStateOfTheVersion() {
        repository.deleteAll(context, elementContext);

        verify(syncStateAccessor).deleteAll(SPACE, "item-1", "version-2");
    }

    @Test
    public void testCreateWritesTheElementRevisionAndMarksTheVersionElementDirty() {
        repository.create(context, elementContext, syncState(true));

        verify(syncStateAccessor).update(PUBLISH_TIME, true, SPACE, "item-1", "version-2", "element-4",
                "element-revision-5");
        verify(versionElementsAccessor).addDirtyElements(Collections.singleton("element-4"), SPACE, "item-1",
                "version-2", "version-revision-3");
        verify(versionElementsAccessor, never()).removeDirtyElements(Mockito.anySet(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testCreateClearsTheDirtyMarkForANonDirtyState() {
        repository.create(context, elementContext, syncState(false));

        verify(syncStateAccessor).update(PUBLISH_TIME, false, SPACE, "item-1", "version-2", "element-4",
                "element-revision-5");
        verify(versionElementsAccessor).removeDirtyElements(Collections.singleton("element-4"), SPACE, "item-1",
                "version-2", "version-revision-3");
    }

    @Test
    public void testUpdateWritesTheElementRevisionAndMarksTheVersionElementDirty() {
        repository.update(context, elementContext, syncState(true));

        verify(syncStateAccessor).update(PUBLISH_TIME, true, SPACE, "item-1", "version-2", "element-4",
                "element-revision-5");
        verify(versionElementsAccessor).addDirtyElements(Collections.singleton("element-4"), SPACE, "item-1",
                "version-2", "version-revision-3");
    }

    @Test
    public void testMarkAsDirtySetsTheDirtyFlagOnTheVersionRevision() {
        repository.markAsDirty(context, elementContext, syncState(false));

        verify(syncStateAccessor).updateDirty(true, SPACE, "item-1", "version-2", "element-4", "version-revision-3");
        verify(versionElementsAccessor).addDirtyElements(Collections.singleton("element-4"), SPACE, "item-1",
                "version-2", "version-revision-3");
    }

    @Test
    public void testDeleteRemovesTheStateAndTheDirtyMark() {
        repository.delete(context, elementContext, syncState(true));

        verify(syncStateAccessor).delete(SPACE, "item-1", "version-2", "element-4", "version-revision-3");
        verify(versionElementsAccessor).removeDirtyElements(Collections.singleton("element-4"), SPACE, "item-1",
                "version-2", "version-revision-3");
    }

    @Test
    public void testGetReadsTheStateOfTheRequestedElementRevision() {
        doReturn(resultSetOf(syncStateRow("element-4", "element-revision-5", PUBLISH_TIME, true)))
                .when(syncStateAccessor).get(SPACE, "item-1", "version-2", "element-4", "element-revision-5");

        Optional<SynchronizationStateEntity> retrieved = repository.get(context, elementContext, syncState(false));

        Assert.assertTrue(retrieved.isPresent());
        Assert.assertEquals(retrieved.get().getId(), ELEMENT_ID);
        Assert.assertEquals(retrieved.get().getRevisionId(), ELEMENT_REVISION_ID);
        Assert.assertEquals(retrieved.get().getPublishTime(), PUBLISH_TIME);
        Assert.assertTrue(retrieved.get().isDirty());
    }

    @Test
    public void testGetReturnsEmptyWhenTheElementHasNoState() {
        doReturn(resultSetOf(null)).when(syncStateAccessor)
                .get(SPACE, "item-1", "version-2", "element-4", "element-revision-5");

        Assert.assertFalse(repository.get(context, elementContext, syncState(false)).isPresent());
    }

    private static SynchronizationStateEntity syncState(boolean dirty) {
        return new SynchronizationStateEntity(ELEMENT_ID, ELEMENT_REVISION_ID, PUBLISH_TIME, dirty);
    }

    private static SynchronizationStateEntity findById(Collection<SynchronizationStateEntity> states, Id id) {
        return states.stream().filter(state -> state.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("no state for " + id));
    }

    private static Row syncStateRow(String elementId, String revisionId, Date publishTime, boolean dirty) {
        Row row = Mockito.mock(Row.class);
        when(row.getString("element_id")).thenReturn(elementId);
        when(row.getString("revision_id")).thenReturn(revisionId);
        when(row.getTimestamp("publish_time")).thenReturn(publishTime);
        when(row.getBool("dirty")).thenReturn(dirty);
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
