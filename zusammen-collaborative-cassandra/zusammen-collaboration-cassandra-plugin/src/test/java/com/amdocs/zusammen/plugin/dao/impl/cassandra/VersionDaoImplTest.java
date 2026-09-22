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
import com.amdocs.zusammen.plugin.dao.impl.cassandra.VersionDaoImpl.VersionAccessor;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.VersionDaoImpl.VersionElementsAccessor;
import com.amdocs.zusammen.plugin.dao.types.VersionEntity;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class VersionDaoImplTest {

    private static final String TENANT = "VersionDaoImplTest_tenant";
    private static final String USER = "VersionDaoImplTest_user";
    private static final String SPACE = "VersionDaoImplTest_space";
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id BASE_VERSION_ID = new Id("base-version-3");
    private static final Id REVISION_ID = new Id("revision-4");
    private static final Date CREATION_TIME = new Date(1_600_000_000_000L);
    private static final Date MODIFICATION_TIME = new Date(1_600_000_111_000L);
    private static final Date PUBLISH_TIME = new Date(1_700_000_000_000L);

    @Mock
    private VersionAccessor versionAccessor;
    @Mock
    private VersionElementsAccessor versionElementsAccessor;

    private AutoCloseable mocks;
    private SessionContext context;
    private VersionDaoImpl versionDao;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        CassandraAccessorSeam.install();
        CassandraAccessorSeam.registerAccessor(VersionAccessor.class, versionAccessor);
        CassandraAccessorSeam.registerAccessor(VersionElementsAccessor.class, versionElementsAccessor);

        context = new SessionContext();
        context.setUser(new UserInfo(USER));
        context.setTenant(TENANT);
        versionDao = new VersionDaoImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        CassandraAccessorSeam.uninstall();
        mocks.close();
    }

    @Test
    public void testCreateWritesTheBaseVersionAndTheTimestamps() {
        versionDao.create(context, SPACE, ITEM_ID, version(BASE_VERSION_ID));

        verify(versionAccessor).create(SPACE, "item-1", "version-2", "base-version-3", CREATION_TIME,
                MODIFICATION_TIME);
    }

    @Test
    public void testCreateWritesANullBaseVersionForARootVersion() {
        versionDao.create(context, SPACE, ITEM_ID, version(null));

        verify(versionAccessor).create(SPACE, "item-1", "version-2", null, CREATION_TIME, MODIFICATION_TIME);
    }

    @Test
    public void testDeleteRemovesTheVersionRow() {
        versionDao.delete(context, SPACE, ITEM_ID, VERSION_ID);

        verify(versionAccessor).delete(SPACE, "item-1", "version-2");
    }

    @Test
    public void testUpdateModificationTimeWritesOnlyTheTimestamp() {
        versionDao.updateModificationTime(context, SPACE, ITEM_ID, VERSION_ID, MODIFICATION_TIME);

        verify(versionAccessor).updateModificationTime(MODIFICATION_TIME, SPACE, "item-1", "version-2");
    }

    @Test
    public void testListMapsEveryRowToAVersionEntity() {
        doReturn(resultSetOfAll(Arrays.asList(versionRow("version-2", "base-version-3"),
                versionRow("version-5", "base-version-6")))).when(versionAccessor).list(SPACE, "item-1");

        Collection<VersionEntity> versions = versionDao.list(context, SPACE, ITEM_ID);

        List<VersionEntity> ordered = new ArrayList<>(versions);
        Assert.assertEquals(ordered.size(), 2);
        Assert.assertEquals(ordered.get(0).getId(), VERSION_ID);
        Assert.assertEquals(ordered.get(0).getBaseId(), BASE_VERSION_ID);
        Assert.assertEquals(ordered.get(0).getCreationTime(), CREATION_TIME);
        Assert.assertEquals(ordered.get(0).getModificationTime(), MODIFICATION_TIME);
        Assert.assertEquals(ordered.get(1).getId(), new Id("version-5"));
        Assert.assertEquals(ordered.get(1).getBaseId(), new Id("base-version-6"));
    }

    @Test
    public void testListReturnsEmptyCollectionWhenTheItemHasNoVersions() {
        doReturn(resultSetOfAll(null)).when(versionAccessor).list(SPACE, "item-1");

        Assert.assertTrue(versionDao.list(context, SPACE, ITEM_ID).isEmpty());
    }

    @Test
    public void testGetReturnsTheVersionWithItsBaseAndTimestamps() {
        doReturn(resultSetOf(versionRow("version-2", "base-version-3"))).when(versionAccessor)
                .get(SPACE, "item-1", "version-2");

        Optional<VersionEntity> retrieved = versionDao.get(context, SPACE, ITEM_ID, VERSION_ID);

        Assert.assertTrue(retrieved.isPresent());
        Assert.assertEquals(retrieved.get().getId(), VERSION_ID);
        Assert.assertEquals(retrieved.get().getBaseId(), BASE_VERSION_ID);
        Assert.assertEquals(retrieved.get().getCreationTime(), CREATION_TIME);
        Assert.assertEquals(retrieved.get().getModificationTime(), MODIFICATION_TIME);
    }

    @Test
    public void testGetReturnsEmptyWhenTheVersionRowIsMissing() {
        doReturn(resultSetOf(null)).when(versionAccessor).get(SPACE, "item-1", "version-2");

        Assert.assertFalse(versionDao.get(context, SPACE, ITEM_ID, VERSION_ID).isPresent());
    }

    @Test
    public void testCheckHealthIsTrueWhenTheVersionTableAnswersWithItsKeyColumn() {
        doReturn(resultSetWithColumn("version_id")).when(versionAccessor).checkHealth();

        Assert.assertTrue(versionDao.checkHealth(context));
    }

    @Test
    public void testCheckHealthIsFalseWhenTheVersionColumnIsAbsent() {
        doReturn(resultSetWithColumn("something_else")).when(versionAccessor).checkHealth();

        Assert.assertFalse(versionDao.checkHealth(context));
    }

    @Test
    public void testCreateVersionElementsUnwrapsTheIdsAndStampsTheSessionUser() {
        Map<Id, Id> versionElementIds = new HashMap<>();
        versionElementIds.put(new Id("element-7"), new Id("element-revision-8"));
        versionElementIds.put(new Id("element-9"), new Id("element-revision-10"));

        versionDao.createVersionElements(context, SPACE, ITEM_ID, VERSION_ID, REVISION_ID, versionElementIds,
                PUBLISH_TIME, "published by test");

        Map<String, String> expected = new HashMap<>();
        expected.put("element-7", "element-revision-8");
        expected.put("element-9", "element-revision-10");
        verify(versionElementsAccessor).create(SPACE, "item-1", "version-2", "revision-4", expected, PUBLISH_TIME,
                "published by test", USER);
    }

    @Test
    public void testCreateVersionElementsWritesNullWhenThereAreNoElements() {
        versionDao.createVersionElements(context, SPACE, ITEM_ID, VERSION_ID, REVISION_ID, null, PUBLISH_TIME, null);

        verify(versionElementsAccessor).create(SPACE, "item-1", "version-2", "revision-4", null, PUBLISH_TIME, null,
                USER);
    }

    private static VersionEntity version(Id baseId) {
        VersionEntity version = new VersionEntity(VERSION_ID);
        version.setBaseId(baseId);
        version.setCreationTime(CREATION_TIME);
        version.setModificationTime(MODIFICATION_TIME);
        return version;
    }

    private static Row versionRow(String versionId, String baseVersionId) {
        Row row = Mockito.mock(Row.class);
        when(row.getString("version_id")).thenReturn(versionId);
        when(row.getString("base_version_id")).thenReturn(baseVersionId);
        when(row.getTimestamp("creation_time")).thenReturn(CREATION_TIME);
        when(row.getTimestamp("modification_time")).thenReturn(MODIFICATION_TIME);
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

    private static ResultSet resultSetWithColumn(String columnName) {
        ColumnDefinitions columnDefinitions = Mockito.mock(ColumnDefinitions.class);
        when(columnDefinitions.contains(columnName)).thenReturn(true);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        return resultSet;
    }
}
