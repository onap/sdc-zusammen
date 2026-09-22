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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementStageRepositoryImpl.ElementStageAccessor;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementStageRepositoryImpl.StageElementsAccessor;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.StageEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ElementStageRepositoryImplTest {

    private static final String TENANT = "ElementStageRepositoryImplTest_tenant";
    private static final String USER = "ElementStageRepositoryImplTest_user";
    private static final String SPACE = "ElementStageRepositoryImplTest_space";
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id REVISION_ID = new Id("revision-3");
    private static final Id ELEMENT_ID = new Id("element-4");
    private static final Id PARENT_ID = new Id("parent-5");
    private static final Id ELEMENT_HASH = new Id("hash-6");
    private static final String NAMESPACE_VALUE = "parent-5/element-4";
    private static final Date PUBLISH_TIME = new Date(1_700_000_000_000L);

    @Mock
    private ElementStageAccessor elementStageAccessor;
    @Mock
    private StageElementsAccessor stageElementsAccessor;

    private AutoCloseable mocks;
    private SessionContext context;
    private ElementEntityContext elementContext;
    private ElementStageRepositoryImpl repository;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        CassandraAccessorSeam.install();
        CassandraAccessorSeam.registerAccessor(ElementStageAccessor.class, elementStageAccessor);
        CassandraAccessorSeam.registerAccessor(StageElementsAccessor.class, stageElementsAccessor);

        context = new SessionContext();
        context.setUser(new UserInfo(USER));
        context.setTenant(TENANT);
        elementContext = new ElementEntityContext(SPACE, ITEM_ID, VERSION_ID, REVISION_ID);
        repository = new ElementStageRepositoryImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        CassandraAccessorSeam.uninstall();
        mocks.close();
    }

    @Test
    public void testListIdsTurnsStagedElementIdsIntoElementEntities() {
        Row row = Mockito.mock(Row.class);
        when(row.getSet("stage_element_ids", String.class))
                .thenReturn(new HashSet<>(Arrays.asList("element-4", "element-7")));
        doReturn(resultSetOf(row)).when(stageElementsAccessor).get(SPACE, "item-1", "version-2", "revision-3");

        Collection<ElementEntity> elements = repository.listIds(context, elementContext);

        Assert.assertEquals(new HashSet<>(elements),
                new HashSet<>(Arrays.asList(new ElementEntity(ELEMENT_ID), new ElementEntity(new Id("element-7")))));
    }

    @Test
    public void testListIdsReturnsEmptyCollectionWhenTheVersionHasNoStageRow() {
        doReturn(resultSetOf(null)).when(stageElementsAccessor).get(SPACE, "item-1", "version-2", "revision-3");

        Assert.assertTrue(repository.listIds(context, elementContext).isEmpty());
    }

    @Test
    public void testListConflictedIdsReadsTheConflictColumn() {
        Row row = Mockito.mock(Row.class);
        when(row.getSet("conflict_element_ids", String.class)).thenReturn(Collections.singleton("element-4"));
        doReturn(resultSetOf(row)).when(stageElementsAccessor)
                .getConflicted(SPACE, "item-1", "version-2", "revision-3");

        Collection<ElementEntity> elements = repository.listConflictedIds(context, elementContext);

        Assert.assertEquals(elements, Collections.singletonList(new ElementEntity(ELEMENT_ID)));
    }

    @Test
    public void testListConflictedIdsReturnsEmptyCollectionWhenTheVersionHasNoStageRow() {
        doReturn(resultSetOf(null)).when(stageElementsAccessor)
                .getConflicted(SPACE, "item-1", "version-2", "revision-3");

        Assert.assertTrue(repository.listConflictedIds(context, elementContext).isEmpty());
    }

    @Test
    public void testCreateWritesTheStageRowAndRegistersTheElementAsStaged() {
        ElementEntity element = fullElement();
        element.setParentId(null);
        StageEntity<ElementEntity> stage = new StageEntity<>(element, PUBLISH_TIME, Action.UPDATE, false);
        stage.setConflictDependents(Collections.singleton(new ElementEntity(new Id("dependent-8"))));

        repository.create(context, elementContext, stage);

        ArgumentCaptor<String> info = ArgumentCaptor.forClass(String.class);
        verify(elementStageAccessor).create(eq(SPACE), eq("item-1"), eq("version-2"), eq("element-4"),
                eq((String) null), eq(NAMESPACE_VALUE), info.capture(), Mockito.anyString(), eq(element.getData()),
                eq(element.getSearchableData()), eq(element.getVisualization()),
                eq(new HashSet<>(Arrays.asList("sub-9", "sub-10"))), eq("hash-6"), eq(PUBLISH_TIME), eq(Action.UPDATE),
                eq(false), eq(Collections.singleton("dependent-8")));
        Assert.assertEquals(JsonUtil.json2Object(info.getValue(), Info.class).getName(), "element-4-info");

        verify(stageElementsAccessor).add(Collections.singleton("element-4"), SPACE, "item-1", "version-2",
                "revision-3");
        verify(stageElementsAccessor, never()).addConflictElements(Mockito.anySet(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        verify(elementStageAccessor, never()).addSubElements(Mockito.anySet(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testCreateRegistersAConflictedElementInTheConflictColumn() {
        StageEntity<ElementEntity> stage = new StageEntity<>(fullElement(), PUBLISH_TIME, Action.CREATE, true);

        repository.create(context, elementContext, stage);

        verify(stageElementsAccessor).addConflictElements(Collections.singleton("element-4"), SPACE, "item-1",
                "version-2", "revision-3");
        verify(elementStageAccessor).addSubElements(Collections.singleton("element-4"), SPACE, "item-1", "version-2",
                "parent-5");
    }

    @Test
    public void testCreateWritesNullsForTheColumnsTheElementDoesNotCarry() {
        StageEntity<ElementEntity> stage =
                new StageEntity<>(new ElementEntity(ELEMENT_ID), null, Action.IGNORE, false);

        repository.create(context, elementContext, stage);

        verify(elementStageAccessor).create(eq(SPACE), eq("item-1"), eq("version-2"), eq("element-4"),
                eq((String) null), eq((String) null), Mockito.anyString(), Mockito.anyString(), eq((ByteBuffer) null),
                eq((ByteBuffer) null), eq((ByteBuffer) null), eq(Collections.<String>emptySet()), eq((String) null),
                eq((Date) null), eq(Action.IGNORE), eq(false), eq(Collections.<String>emptySet()));
    }

    @Test
    public void testMarkAsNotConflictedWithActionUpdatesTheStateAndClearsTheConflict() {
        repository.markAsNotConflicted(context, elementContext, new ElementEntity(ELEMENT_ID), Action.DELETE);

        verify(elementStageAccessor).updateState(Action.DELETE, false, SPACE, "item-1", "version-2", "element-4");
        verify(stageElementsAccessor).removeConflictElements(Collections.singleton("element-4"), SPACE, "item-1",
                "version-2", "revision-3");
    }

    @Test
    public void testMarkAsNotConflictedKeepsTheActionAndClearsTheConflict() {
        repository.markAsNotConflicted(context, elementContext, new ElementEntity(ELEMENT_ID));

        verify(elementStageAccessor).markAsNotConflicted(SPACE, "item-1", "version-2", "element-4");
        verify(stageElementsAccessor).removeConflictElements(Collections.singleton("element-4"), SPACE, "item-1",
                "version-2", "revision-3");
        verify(elementStageAccessor, never()).updateState(Mockito.any(), Mockito.anyBoolean(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testUpdateClearsTheConflictWhenTheElementIsNoLongerConflicted() {
        ElementEntity element = fullElement();

        repository.update(context, elementContext, element, Action.UPDATE, false);

        verify(elementStageAccessor).update(Mockito.anyString(), Mockito.anyString(), eq(element.getData()),
                eq(element.getSearchableData()), eq(element.getVisualization()), eq("hash-6"), eq(Action.UPDATE),
                eq(false), eq(SPACE), eq("item-1"), eq("version-2"), eq("element-4"));
        verify(stageElementsAccessor).removeConflictElements(Collections.singleton("element-4"), SPACE, "item-1",
                "version-2", "revision-3");
    }

    @Test
    public void testUpdateLeavesTheConflictColumnAloneForAConflictedElement() {
        repository.update(context, elementContext, fullElement(), Action.UPDATE, true);

        verify(elementStageAccessor).update(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.any(), eq("hash-6"), eq(Action.UPDATE), eq(true), eq(SPACE), eq("item-1"), eq("version-2"),
                eq("element-4"));
        verify(stageElementsAccessor, never()).removeConflictElements(Mockito.anySet(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testDeleteRemovesTheStageRowAndTheStagedRegistration() {
        ElementEntity element = fullElement();
        element.setParentId(null);

        repository.delete(context, elementContext, element);

        verify(elementStageAccessor).delete(SPACE, "item-1", "version-2", "element-4");
        verify(stageElementsAccessor).remove(Collections.singleton("element-4"), SPACE, "item-1", "version-2",
                "revision-3");
        verify(elementStageAccessor, never()).removeSubElements(Mockito.anySet(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testDeleteDetachesTheElementFromItsStagedParent() {
        repository.delete(context, elementContext, fullElement());

        verify(elementStageAccessor).removeSubElements(Collections.singleton("element-4"), SPACE, "item-1", "version-2",
                "parent-5");
        verify(elementStageAccessor).delete(SPACE, "item-1", "version-2", "element-4");
    }

    @Test
    public void testGetReturnsTheStagedElementWithItsStageState() {
        doReturn(resultSetOf(fullStageRow())).when(elementStageAccessor)
                .get(SPACE, "item-1", "version-2", "element-4");

        Optional<StageEntity<ElementEntity>> retrieved =
                repository.get(context, elementContext, new ElementEntity(ELEMENT_ID));

        Assert.assertTrue(retrieved.isPresent());
        StageEntity<ElementEntity> stage = retrieved.get();
        Assert.assertEquals(stage.getPublishTime(), PUBLISH_TIME);
        Assert.assertEquals(stage.getAction(), Action.UPDATE);
        Assert.assertTrue(stage.isConflicted());
        Assert.assertEquals(stage.getConflictDependents(),
                Collections.singleton(new ElementEntity(new Id("dependent-8"))));

        ElementEntity element = stage.getEntity();
        Assert.assertEquals(element.getId(), ELEMENT_ID);
        Assert.assertEquals(element.getParentId(), PARENT_ID);
        Assert.assertEquals(element.getNamespace().getValue(), NAMESPACE_VALUE);
        Assert.assertEquals(element.getInfo().getName(), "element-4-info");
        Assert.assertEquals(element.getRelations().size(), 2);
        Assert.assertEquals(element.getData(), byteBuffer("data"));
        Assert.assertEquals(element.getSearchableData(), byteBuffer("searchable"));
        Assert.assertEquals(element.getVisualization(), byteBuffer("visualization"));
        Assert.assertEquals(element.getElementHash(), ELEMENT_HASH);
    }

    @Test
    public void testGetReturnsEmptyWhenTheElementIsNotStaged() {
        doReturn(resultSetOf(null)).when(elementStageAccessor).get(SPACE, "item-1", "version-2", "element-4");

        Assert.assertFalse(repository.get(context, elementContext, new ElementEntity(ELEMENT_ID)).isPresent());
    }

    @Test
    public void testGetDescriptorLeavesTheContentColumnsUnread() {
        doReturn(resultSetOf(fullStageRow())).when(elementStageAccessor)
                .getDescriptor(SPACE, "item-1", "version-2", "element-4");

        StageEntity<ElementEntity> stage =
                repository.getDescriptor(context, elementContext, new ElementEntity(ELEMENT_ID)).get();

        Assert.assertEquals(stage.getAction(), Action.UPDATE);
        Assert.assertEquals(stage.getEntity().getId(), ELEMENT_ID);
        Assert.assertEquals(stage.getEntity().getSubElementIds().size(), 2);
        Assert.assertNull(stage.getEntity().getData());
        Assert.assertNull(stage.getEntity().getElementHash());
    }

    @Test
    public void testGetDescriptorReturnsEmptyWhenTheElementIsNotStaged() {
        doReturn(resultSetOf(null)).when(elementStageAccessor)
                .getDescriptor(SPACE, "item-1", "version-2", "element-4");

        Assert.assertFalse(
                repository.getDescriptor(context, elementContext, new ElementEntity(ELEMENT_ID)).isPresent());
    }

    private static ElementEntity fullElement() {
        ElementEntity element = new ElementEntity(ELEMENT_ID);
        element.setParentId(PARENT_ID);
        Namespace namespace = new Namespace();
        namespace.setValue(NAMESPACE_VALUE);
        element.setNamespace(namespace);
        element.setInfo(info());
        element.setRelations(Arrays.asList(relation("contains"), relation("uses")));
        element.setData(byteBuffer("data"));
        element.setSearchableData(byteBuffer("searchable"));
        element.setVisualization(byteBuffer("visualization"));
        element.setSubElementIds(new HashSet<>(Arrays.asList(new Id("sub-9"), new Id("sub-10"))));
        element.setElementHash(ELEMENT_HASH);
        return element;
    }

    private static Row fullStageRow() {
        Row row = Mockito.mock(Row.class);
        when(row.getString("element_id")).thenReturn("element-4");
        when(row.getString("parent_id")).thenReturn("parent-5");
        when(row.getString("namespace")).thenReturn(NAMESPACE_VALUE);
        when(row.getString("info")).thenReturn(JsonUtil.object2Json(info()));
        when(row.getString("relations"))
                .thenReturn(JsonUtil.object2Json(Arrays.asList(relation("contains"), relation("uses"))));
        when(row.getSet("sub_element_ids", String.class))
                .thenReturn(new HashSet<>(Arrays.asList("sub-9", "sub-10")));
        when(row.getBytes("data")).thenReturn(byteBuffer("data"));
        when(row.getBytes("searchable_data")).thenReturn(byteBuffer("searchable"));
        when(row.getBytes("visualization")).thenReturn(byteBuffer("visualization"));
        when(row.getString("element_hash")).thenReturn("hash-6");
        when(row.getTimestamp("publish_time")).thenReturn(PUBLISH_TIME);
        when(row.getString("action")).thenReturn(Action.UPDATE.name());
        when(row.getBool("conflicted")).thenReturn(true);
        when(row.getSet("conflict_dependent_ids", String.class)).thenReturn(Collections.singleton("dependent-8"));
        return row;
    }

    private static ResultSet resultSetOf(Row row) {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.one()).thenReturn(row);
        return resultSet;
    }

    private static Info info() {
        Info info = new Info();
        info.setName("element-4-info");
        info.setDescription("element-4-description");
        info.addProperty("property", "value");
        return info;
    }

    private static Relation relation(String type) {
        Relation relation = new Relation();
        relation.setType(type);
        return relation;
    }

    private static ByteBuffer byteBuffer(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }
}
