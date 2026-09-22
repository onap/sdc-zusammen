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
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.VersionStageRepositoryImpl.VersionStageAccessor;
import com.amdocs.zusammen.plugin.dao.types.StageEntity;
import com.amdocs.zusammen.plugin.dao.types.VersionContext;
import com.amdocs.zusammen.plugin.dao.types.VersionEntity;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.extras.codecs.enums.EnumNameCodec;
import java.util.Date;
import java.util.Optional;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class VersionStageRepositoryImplTest {

    private static final String TENANT = "VersionStageRepositoryImplTest_tenant";
    private static final String USER = "VersionStageRepositoryImplTest_user";
    private static final String SPACE = "VersionStageRepositoryImplTest_space";
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id BASE_VERSION_ID = new Id("base-version-3");
    private static final Date CREATION_TIME = new Date(1_600_000_000_000L);
    private static final Date MODIFICATION_TIME = new Date(1_600_000_111_000L);
    private static final Date PUBLISH_TIME = new Date(1_700_000_000_000L);

    @Mock
    private VersionStageAccessor versionStageAccessor;

    private AutoCloseable mocks;
    private SessionContext context;
    private VersionContext versionContext;
    private VersionStageRepositoryImpl repository;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        CassandraAccessorSeam.install();
        CassandraAccessorSeam.registerAccessor(VersionStageAccessor.class, versionStageAccessor);

        context = new SessionContext();
        context.setUser(new UserInfo(USER));
        context.setTenant(TENANT);
        versionContext = new VersionContext(SPACE, ITEM_ID);
        // The class initialiser calls CassandraDaoUtils.registerCodecs, so the seam has to be in place
        // before the class is first touched - hence no field initialiser here.
        repository = new VersionStageRepositoryImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        CassandraAccessorSeam.uninstall();
        mocks.close();
    }

    @Test
    public void testClassInitialisationRegistersTheActionEnumCodec() {
        TypeCodec<Action> codec = CassandraAccessorSeam.codecRegistry().codecFor(DataType.varchar(), Action.class);

        Assert.assertTrue(codec instanceof EnumNameCodec);
    }

    @Test
    public void testGetReturnsTheStagedVersionWithItsStageState() {
        doReturn(resultSetOf(versionStageRow(Action.UPDATE))).when(versionStageAccessor)
                .get(SPACE, "item-1", "version-2");

        Optional<StageEntity<VersionEntity>> retrieved =
                repository.get(context, versionContext, new VersionEntity(VERSION_ID));

        Assert.assertTrue(retrieved.isPresent());
        StageEntity<VersionEntity> stage = retrieved.get();
        Assert.assertEquals(stage.getPublishTime(), PUBLISH_TIME);
        Assert.assertEquals(stage.getAction(), Action.UPDATE);
        Assert.assertEquals(stage.getEntity().getId(), VERSION_ID);
        Assert.assertEquals(stage.getEntity().getBaseId(), BASE_VERSION_ID);
        Assert.assertEquals(stage.getEntity().getCreationTime(), CREATION_TIME);
        Assert.assertEquals(stage.getEntity().getModificationTime(), MODIFICATION_TIME);
    }

    @Test
    public void testGetReturnsEmptyWhenTheVersionIsNotStaged() {
        doReturn(resultSetOf(null)).when(versionStageAccessor).get(SPACE, "item-1", "version-2");

        Assert.assertFalse(repository.get(context, versionContext, new VersionEntity(VERSION_ID)).isPresent());
    }

    @Test
    public void testCreateWritesTheVersionAndItsStageState() {
        VersionEntity version = new VersionEntity(VERSION_ID);
        version.setBaseId(BASE_VERSION_ID);
        version.setCreationTime(CREATION_TIME);
        version.setModificationTime(MODIFICATION_TIME);

        repository.create(context, versionContext,
                new StageEntity<>(version, PUBLISH_TIME, Action.CREATE, false));

        verify(versionStageAccessor).create(SPACE, "item-1", "version-2", "base-version-3", CREATION_TIME,
                MODIFICATION_TIME, PUBLISH_TIME, Action.CREATE);
    }

    @Test
    public void testCreateWritesNullsForTheColumnsTheVersionDoesNotCarry() {
        repository.create(context, versionContext,
                new StageEntity<>(new VersionEntity(VERSION_ID), null, Action.DELETE, false));

        verify(versionStageAccessor).create(SPACE, "item-1", "version-2", null, null, null, null, Action.DELETE);
    }

    @Test
    public void testDeleteRemovesTheStageRow() {
        repository.delete(context, versionContext, new VersionEntity(VERSION_ID));

        verify(versionStageAccessor).delete(SPACE, "item-1", "version-2");
    }

    private static Row versionStageRow(Action action) {
        Row row = Mockito.mock(Row.class);
        when(row.getString("base_version_id")).thenReturn("base-version-3");
        when(row.getTimestamp("creation_time")).thenReturn(CREATION_TIME);
        when(row.getTimestamp("modification_time")).thenReturn(MODIFICATION_TIME);
        when(row.getTimestamp("publish_time")).thenReturn(PUBLISH_TIME);
        when(row.getString("action")).thenReturn(action.name());
        return row;
    }

    private static ResultSet resultSetOf(Row row) {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.one()).thenReturn(row);
        return resultSet;
    }
}
