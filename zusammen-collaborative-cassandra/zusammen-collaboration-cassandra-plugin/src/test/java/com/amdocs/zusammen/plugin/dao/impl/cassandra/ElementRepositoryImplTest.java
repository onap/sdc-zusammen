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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.plugin.ZusammenPluginConstants;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementRepositoryImpl.ElementAccessor;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementRepositoryImpl.ElementNamespaceAccessor;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementRepositoryImpl.VersionElementsAccessor;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.gson.reflect.TypeToken;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ElementRepositoryImplTest {

    private static final String TENANT = "ElementRepositoryImplTest_tenant";
    private static final String USER = "ElementRepositoryImplTest_user";
    private static final String SPACE = "ElementRepositoryImplTest_space";
    private static final String PUBLIC_SPACE = ZusammenPluginConstants.PUBLIC_SPACE;
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id REVISION_ID = new Id("revision-3");
    private static final Id ELEMENT_ID = new Id("element-4");
    private static final Id PARENT_ID = new Id("parent-5");
    private static final Id ELEMENT_HASH = new Id("hash-6");
    private static final String NAMESPACE_VALUE = "parent-5/element-4";
    private static final String ZERO_REVISION = Id.ZERO.getValue();

    @Mock
    private ElementAccessor elementAccessor;
    @Mock
    private VersionElementsAccessor versionElementsAccessor;
    @Mock
    private ElementNamespaceAccessor elementNamespaceAccessor;

    private AutoCloseable mocks;
    private SessionContext context;
    private ElementRepositoryImpl repository;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        CassandraAccessorSeam.install();
        CassandraAccessorSeam.registerAccessor(ElementAccessor.class, elementAccessor);
        CassandraAccessorSeam.registerAccessor(VersionElementsAccessor.class, versionElementsAccessor);
        CassandraAccessorSeam.registerAccessor(ElementNamespaceAccessor.class, elementNamespaceAccessor);

        context = new SessionContext();
        context.setUser(new UserInfo(USER));
        context.setTenant(TENANT);
        repository = new ElementRepositoryImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        CassandraAccessorSeam.uninstall();
        mocks.close();
    }

    @Test
    public void testListIdsWithExplicitRevisionMapsElementIdsToRevisionIds() {
        Map<String, String> stored = new HashMap<>();
        stored.put("element-4", "revision-3");
        stored.put("element-7", "revision-8");
        givenVersionElements(SPACE, "revision-3", versionElementsRow(stored));

        Map<Id, Id> ids = repository.listIds(context, elementContext(REVISION_ID));

        Map<Id, Id> expected = new HashMap<>();
        expected.put(new Id("element-4"), new Id("revision-3"));
        expected.put(new Id("element-7"), new Id("revision-8"));
        Assert.assertEquals(ids, expected);
    }

    @Test
    public void testListIdsReturnsEmptyMapWhenVersionHasNoElementsRow() {
        givenVersionElements(SPACE, "revision-3", null);

        Assert.assertTrue(repository.listIds(context, elementContext(REVISION_ID)).isEmpty());
    }

    @Test
    public void testListIdsResolvesAndStoresLatestRevisionWhenContextHasNone() {
        givenRevisions(Arrays.asList(revisionRow("revision-old", new Date(1_000L)),
                revisionRow("revision-latest", new Date(3_000L)),
                revisionRow("revision-middle", new Date(2_000L))));
        givenVersionElements(SPACE, "revision-latest",
                versionElementsRow(Collections.singletonMap("element-4", "revision-latest")));

        ElementEntityContext elementContext = elementContext(null);
        Map<Id, Id> ids = repository.listIds(context, elementContext);

        Assert.assertEquals(elementContext.getRevisionId(), new Id("revision-latest"));
        Assert.assertEquals(ids, Collections.singletonMap(ELEMENT_ID, new Id("revision-latest")));
    }

    @Test
    public void testListIdsReturnsEmptyMapWhenNoRevisionExists() {
        givenRevisions(new ArrayList<Row>());

        ElementEntityContext elementContext = elementContext(null);

        Assert.assertTrue(repository.listIds(context, elementContext).isEmpty());
        Assert.assertNull(elementContext.getRevisionId());
        verify(versionElementsAccessor, never()).get(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
    }

    @Test
    public void testListIdsReturnsEmptyMapWhenRevisionQueryYieldsNull() {
        givenRevisions(null);

        Assert.assertTrue(repository.listIds(context, elementContext(null)).isEmpty());
    }

    @Test
    public void testCreateWritesElementRowAndRegistersItUnderTheVersion() {
        ElementEntity element = fullElement();
        element.setParentId(null);

        repository.create(context, elementContext(REVISION_ID), element);

        ArgumentCaptor<String> info = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> relations = ArgumentCaptor.forClass(String.class);
        verify(elementAccessor).create(eq(SPACE), eq("item-1"), eq("version-2"), eq("element-4"), eq("revision-3"),
                eq((String) null), eq(NAMESPACE_VALUE), info.capture(), relations.capture(), eq(element.getData()),
                eq(element.getSearchableData()), eq(element.getVisualization()),
                eq(new HashSet<>(Arrays.asList("sub-9", "sub-10"))), eq("hash-6"));
        Assert.assertEquals(JsonUtil.json2Object(info.getValue(), Info.class).getName(), "element-4-info");
        Assert.assertEquals(relationsOf(relations.getValue()).size(), 2);

        verify(versionElementsAccessor).addElements(Collections.singletonMap("element-4", "revision-3"), SPACE,
                "item-1", "version-2", "revision-3");
        verify(elementAccessor, never()).addSubElements(Mockito.anySet(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testCreateWritesNullParentAndNamespaceWhenElementHasNeither() {
        ElementEntity element = new ElementEntity(ELEMENT_ID);
        element.setElementHash(ELEMENT_HASH);

        repository.create(context, elementContext(REVISION_ID), element);

        verify(elementAccessor).create(eq(SPACE), eq("item-1"), eq("version-2"), eq("element-4"), eq("revision-3"),
                eq((String) null), eq((String) null), Mockito.anyString(), Mockito.anyString(), eq((ByteBuffer) null),
                eq((ByteBuffer) null), eq((ByteBuffer) null), eq(Collections.<String>emptySet()), eq("hash-6"));
    }

    @Test
    public void testCreateAddsElementToItsParentAndRegistersTheParentRevision() {
        repository.create(context, elementContext(REVISION_ID), fullElement());

        verify(elementAccessor).addSubElements(Collections.singleton("element-4"), SPACE, "item-1", "version-2",
                "parent-5", "revision-3");
        verify(versionElementsAccessor).addElements(Collections.singletonMap("parent-5", "revision-3"), SPACE,
                "item-1", "version-2", "revision-3");
    }

    @Test
    public void testUpdateUpdatesInPlaceWhenElementAlreadyLivesOnTheContextRevision() {
        givenVersionElements(SPACE, "revision-3",
                versionElementsRow(Collections.singletonMap("element-4", "revision-3")));
        ElementEntity element = fullElement();
        element.setParentId(null);

        repository.update(context, elementContext(REVISION_ID), element);

        verify(elementAccessor).update(Mockito.anyString(), Mockito.anyString(), eq(element.getData()),
                eq(element.getSearchableData()), eq(element.getVisualization()), eq("hash-6"), eq(SPACE), eq("item-1"),
                eq("version-2"), eq("element-4"), eq("revision-3"));
        verify(versionElementsAccessor).addElements(Collections.singletonMap("element-4", "revision-3"), SPACE,
                "item-1", "version-2", "revision-3");
        verify(elementAccessor, never()).create(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anySet(),
                Mockito.anyString());
    }

    @Test
    public void testUpdateAlsoWritesParentIdWhenElementHasAParent() {
        givenVersionElements(SPACE, "revision-3",
                versionElementsRow(Collections.singletonMap("element-4", "revision-3")));

        repository.update(context, elementContext(REVISION_ID), fullElement());

        verify(elementAccessor).update(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.any(), eq("hash-6"), eq("parent-5"), eq(SPACE), eq("item-1"), eq("version-2"), eq("element-4"),
                eq("revision-3"));
    }

    @Test
    public void testUpdateCreatesANewElementRowWhenTheElementLivesOnAnEarlierRevision() {
        givenVersionElements(SPACE, "revision-3",
                versionElementsRow(Collections.singletonMap("element-4", "revision-0")));

        repository.update(context, elementContext(REVISION_ID), fullElement());

        verify(elementAccessor).create(eq(SPACE), eq("item-1"), eq("version-2"), eq("element-4"), eq("revision-3"),
                eq("parent-5"), eq(NAMESPACE_VALUE), Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anySet(), eq("hash-6"));
    }

    @Test
    public void testDeleteRemovesTheElementRowAndItsVersionEntry() {
        ElementEntity element = fullElement();
        element.setParentId(null);

        repository.delete(context, elementContext(REVISION_ID), element);

        verify(elementAccessor).delete(SPACE, "item-1", "version-2", "element-4", "revision-3");
        verify(versionElementsAccessor).removeElements(Collections.singleton("element-4"), SPACE, "item-1", "version-2",
                "revision-3");
        verify(elementAccessor, never()).removeSubElements(Mockito.anySet(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testDeleteDetachesTheElementFromItsParent() {
        givenElement(SPACE, "parent-5", ZERO_REVISION, fullElementRow());

        repository.delete(context, elementContext(REVISION_ID), fullElement());

        verify(elementAccessor).removeSubElements(Collections.singleton("element-4"), SPACE, "item-1", "version-2",
                "parent-5", "revision-3");
        verify(versionElementsAccessor).addElements(Collections.singletonMap("parent-5", "revision-3"), SPACE,
                "item-1", "version-2", "revision-3");
        verify(elementAccessor).delete(SPACE, "item-1", "version-2", "element-4", "revision-3");
    }

    @Test
    public void testDeleteSkipsTheParentUpdateWhenTheParentIsGone() {
        givenElement(SPACE, "parent-5", ZERO_REVISION, null);

        repository.delete(context, elementContext(REVISION_ID), fullElement());

        verify(elementAccessor, never()).removeSubElements(Mockito.anySet(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        verify(elementAccessor).delete(SPACE, "item-1", "version-2", "element-4", "revision-3");
    }

    @Test
    public void testCleanAllRevisionsDeletesTheElementAcrossRevisions() {
        repository.cleanAllRevisions(context, elementContext(REVISION_ID), fullElement());

        verify(elementAccessor).deleteAllRevisions(SPACE, "item-1", "version-2", "element-4");
        verifyNoMoreInteractions(elementAccessor);
    }

    @Test
    public void testGetReadsRevisionZeroInAPrivateSpaceAndMapsEveryColumn() {
        givenElement(SPACE, "element-4", ZERO_REVISION, fullElementRow());

        Optional<ElementEntity> retrieved =
                repository.get(context, elementContext(REVISION_ID), new ElementEntity(ELEMENT_ID));

        Assert.assertTrue(retrieved.isPresent());
        ElementEntity element = retrieved.get();
        Assert.assertEquals(element.getId(), ELEMENT_ID);
        Assert.assertEquals(element.getParentId(), PARENT_ID);
        Assert.assertEquals(element.getNamespace().getValue(), NAMESPACE_VALUE);
        Assert.assertEquals(element.getInfo().getName(), "element-4-info");
        Assert.assertEquals(element.getInfo().getDescription(), "element-4-description");
        Assert.assertEquals(element.getRelations().size(), 2);
        Assert.assertEquals(element.getSubElementIds(), new HashSet<>(Arrays.asList(new Id("sub-9"), new Id("sub-10"))));
        Assert.assertEquals(element.getData(), byteBuffer("data"));
        Assert.assertEquals(element.getSearchableData(), byteBuffer("searchable"));
        Assert.assertEquals(element.getVisualization(), byteBuffer("visualization"));
        Assert.assertEquals(element.getElementHash(), ELEMENT_HASH);
    }

    @Test
    public void testGetReturnsEmptyWhenTheElementRowIsMissing() {
        givenElement(SPACE, "element-4", ZERO_REVISION, null);

        Assert.assertFalse(
                repository.get(context, elementContext(REVISION_ID), new ElementEntity(ELEMENT_ID)).isPresent());
    }

    @Test
    public void testGetInPublicSpaceReadsTheElementRevisionRecordedForTheVersion() {
        givenVersionElements(PUBLIC_SPACE, "revision-3",
                versionElementsRow(Collections.singletonMap("element-4", "revision-1")));
        givenElement(PUBLIC_SPACE, "element-4", "revision-1", fullElementRow());

        Optional<ElementEntity> retrieved = repository.get(context,
                new ElementEntityContext(PUBLIC_SPACE, ITEM_ID, VERSION_ID, REVISION_ID),
                new ElementEntity(ELEMENT_ID));

        Assert.assertTrue(retrieved.isPresent());
        Assert.assertEquals(retrieved.get().getElementHash(), ELEMENT_HASH);
    }

    @Test
    public void testGetInPublicSpaceReturnsEmptyWhenTheVersionDoesNotContainTheElement() {
        givenVersionElements(PUBLIC_SPACE, "revision-3",
                versionElementsRow(Collections.singletonMap("element-99", "revision-1")));

        Assert.assertFalse(repository.get(context,
                new ElementEntityContext(PUBLIC_SPACE, ITEM_ID, VERSION_ID, REVISION_ID),
                new ElementEntity(ELEMENT_ID)).isPresent());
        verify(elementAccessor, never()).get(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testGetInPublicSpaceResolvesTheLatestRevisionWhenTheContextHasNone() {
        givenRevisions(PUBLIC_SPACE, Collections.singletonList(revisionRow("revision-latest", new Date(3_000L))));
        givenVersionElements(PUBLIC_SPACE, "revision-latest",
                versionElementsRow(Collections.singletonMap("element-4", "revision-1")));
        givenElement(PUBLIC_SPACE, "element-4", "revision-1", fullElementRow());

        ElementEntityContext publicContext = new ElementEntityContext(PUBLIC_SPACE, ITEM_ID, VERSION_ID, null);
        Optional<ElementEntity> retrieved = repository.get(context, publicContext, new ElementEntity(ELEMENT_ID));

        Assert.assertTrue(retrieved.isPresent());
        Assert.assertEquals(publicContext.getRevisionId(), new Id("revision-latest"));
    }

    @Test
    public void testGetDescriptorInPublicSpaceReturnsEmptyWhenTheVersionDoesNotContainTheElement() {
        givenVersionElements(PUBLIC_SPACE, "revision-3",
                versionElementsRow(Collections.singletonMap("element-99", "revision-1")));

        Assert.assertFalse(repository.getDescriptor(context,
                new ElementEntityContext(PUBLIC_SPACE, ITEM_ID, VERSION_ID, REVISION_ID),
                new ElementEntity(ELEMENT_ID)).isPresent());
    }

    @Test
    public void testGetHashInPublicSpaceReturnsEmptyWhenTheVersionDoesNotContainTheElement() {
        givenVersionElements(PUBLIC_SPACE, "revision-3",
                versionElementsRow(Collections.singletonMap("element-99", "revision-1")));

        Assert.assertFalse(repository.getHash(context,
                new ElementEntityContext(PUBLIC_SPACE, ITEM_ID, VERSION_ID, REVISION_ID),
                new ElementEntity(ELEMENT_ID)).isPresent());
    }

    @Test
    public void testGetDescriptorLeavesTheContentColumnsUnread() {
        givenElementDescriptor(fullElementRow());

        Optional<ElementEntity> retrieved =
                repository.getDescriptor(context, elementContext(REVISION_ID), new ElementEntity(ELEMENT_ID));

        Assert.assertTrue(retrieved.isPresent());
        ElementEntity element = retrieved.get();
        Assert.assertEquals(element.getInfo().getName(), "element-4-info");
        Assert.assertEquals(element.getSubElementIds().size(), 2);
        Assert.assertNull(element.getData());
        Assert.assertNull(element.getSearchableData());
        Assert.assertNull(element.getVisualization());
        Assert.assertNull(element.getElementHash());
    }

    @Test
    public void testGetDescriptorMapsAbsentColumnsToRootNamespaceAndNoParent() {
        givenElementDescriptor(Mockito.mock(Row.class));

        ElementEntity element =
                repository.getDescriptor(context, elementContext(REVISION_ID), new ElementEntity(ELEMENT_ID)).get();

        Assert.assertNull(element.getParentId());
        Assert.assertEquals(element.getNamespace(), Namespace.ROOT_NAMESPACE);
        Assert.assertNull(element.getInfo());
        Assert.assertNull(element.getRelations());
        Assert.assertTrue(element.getSubElementIds().isEmpty());
    }

    @Test
    public void testGetDescriptorReturnsEmptyWhenTheElementRowIsMissing() {
        givenElementDescriptor(null);

        Assert.assertFalse(
                repository.getDescriptor(context, elementContext(REVISION_ID), new ElementEntity(ELEMENT_ID))
                        .isPresent());
    }

    @Test
    public void testCreateNamespaceWritesTheElementNamespaceRow() {
        repository.createNamespace(context, elementContext(REVISION_ID), fullElement());

        verify(elementNamespaceAccessor).create("item-1", "element-4", NAMESPACE_VALUE);
    }

    @Test
    public void testGetHashReturnsTheStoredHash() {
        Row row = Mockito.mock(Row.class);
        when(row.getString("element_hash")).thenReturn("hash-6");
        doReturn(resultSetOf(row)).when(elementAccessor)
                .getHash(SPACE, "item-1", "version-2", "element-4", ZERO_REVISION);

        Optional<Id> hash = repository.getHash(context, elementContext(REVISION_ID), new ElementEntity(ELEMENT_ID));

        Assert.assertEquals(hash, Optional.of(ELEMENT_HASH));
    }

    @Test
    public void testGetHashReturnsEmptyWhenTheElementRowIsMissing() {
        doReturn(resultSetOf(null)).when(elementAccessor)
                .getHash(SPACE, "item-1", "version-2", "element-4", ZERO_REVISION);

        Assert.assertFalse(
                repository.getHash(context, elementContext(REVISION_ID), new ElementEntity(ELEMENT_ID)).isPresent());
    }

    private void givenElement(String space, String elementId, String revisionId, Row row) {
        doReturn(resultSetOf(row)).when(elementAccessor).get(space, "item-1", "version-2", elementId, revisionId);
    }

    private void givenElementDescriptor(Row row) {
        doReturn(resultSetOf(row)).when(elementAccessor)
                .getDescriptor(SPACE, "item-1", "version-2", "element-4", ZERO_REVISION);
    }

    private void givenVersionElements(String space, String revisionId, Row row) {
        doReturn(resultSetOf(row)).when(versionElementsAccessor).get(space, "item-1", "version-2", revisionId);
    }

    private void givenRevisions(List<Row> rows) {
        givenRevisions(SPACE, rows);
    }

    private void givenRevisions(String space, List<Row> rows) {
        doReturn(resultSetOfAll(rows)).when(versionElementsAccessor).listRevisions(space, "item-1", "version-2");
    }

    private static ElementEntityContext elementContext(Id revisionId) {
        return new ElementEntityContext(SPACE, ITEM_ID, VERSION_ID, revisionId);
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

    private static Row fullElementRow() {
        Row row = Mockito.mock(Row.class);
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
        return row;
    }

    private static Row versionElementsRow(Map<String, String> elementIds) {
        Row row = Mockito.mock(Row.class);
        when(row.getMap("element_ids", String.class, String.class)).thenReturn(elementIds);
        return row;
    }

    private static Row revisionRow(String revisionId, Date publishTime) {
        Row row = Mockito.mock(Row.class);
        when(row.getString("revision_id")).thenReturn(revisionId);
        when(row.getTimestamp("publish_time")).thenReturn(publishTime);
        return row;
    }

    private static ResultSet resultSetOf(Row row) {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.one()).thenReturn(row);
        return resultSet;
    }

    private static ResultSet resultSetOfAll(List<Row> rows) {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.all()).thenReturn(rows == null ? null : new ArrayList<>(rows));
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

    private static Collection<Relation> relationsOf(String json) {
        return JsonUtil.json2Object(json, new TypeToken<ArrayList<Relation>>() { }.getType());
    }

    private static ByteBuffer byteBuffer(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }
}
