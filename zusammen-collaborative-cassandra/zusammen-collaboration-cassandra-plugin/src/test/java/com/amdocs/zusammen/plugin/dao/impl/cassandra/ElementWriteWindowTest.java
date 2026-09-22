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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.plugin.ZusammenPluginConstants;
import com.amdocs.zusammen.plugin.collaboration.PublishService;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementRepositoryImpl.ElementAccessor;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementRepositoryImpl.VersionElementsAccessor;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.main.CassandraCollaborationStorePluginImpl;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationPublishResult;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ElementWriteWindowTest {

    private static final String TENANT = "ElementWriteWindowTest_tenant";
    private static final String USER = "ElementWriteWindowTest_user";
    private static final String PUBLIC_SPACE = ZusammenPluginConstants.PUBLIC_SPACE;
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id REVISION_ID = new Id("revision-3");
    private static final Id ELEMENT_ID = new Id("element-4");
    private static final Id PARENT_ID = new Id("parent-5");
    private static final Id ELEMENT_HASH = new Id("hash-6");
    private static final String NAMESPACE_VALUE = "parent-5/element-4";

    @Mock
    private ElementAccessor elementAccessor;
    @Mock
    private VersionElementsAccessor versionElementsAccessor;
    @Mock
    private ElementSynchronizationStateRepositoryImpl.VersionElementsAccessor dirtyElementIdsAccessor;

    private AutoCloseable mocks;
    private SessionContext context;
    private ElementRepositoryImpl repository;

    /** Every statement handed to {@code executeAsync}, in the order it was handed over. */
    private final List<Statement> issued = new ArrayList<>();
    /** Every statement whose future was looked at, in the order it was looked at. */
    private final List<Statement> settled = new ArrayList<>();
    /** What a statement does to the stubbed rows once its future is looked at. */
    private Consumer<Statement> effectOf = statement -> { };
    /** How a statement fails once its future is looked at. */
    private Function<Statement, RuntimeException> failureOf = statement -> null;

    @BeforeMethod
    public void setUp() {
        // TestNG reuses one instance for every method in the class, so the recorded traffic and the
        // stubbed behaviour of a future have to be put back by hand.
        issued.clear();
        settled.clear();
        effectOf = statement -> { };
        failureOf = statement -> null;

        mocks = MockitoAnnotations.openMocks(this);
        CassandraAccessorSeam.install();
        CassandraAccessorSeam.registerAccessor(ElementAccessor.class, elementAccessor);
        CassandraAccessorSeam.registerAccessor(VersionElementsAccessor.class, versionElementsAccessor);
        CassandraAccessorSeam.registerAccessor(ElementSynchronizationStateRepositoryImpl.VersionElementsAccessor.class,
                dirtyElementIdsAccessor);
        givenTheAccessorBindsTheStatementsItIsAskedFor();
        givenTheSessionSendsWithoutWaiting();

        context = new SessionContext();
        context.setUser(new UserInfo(USER));
        context.setTenant(TENANT);
        repository = new ElementRepositoryImpl();
        ElementWriteWindow.open();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        ElementWriteWindow.discard();
        VersionElementIdsCache.close();
        CassandraAccessorSeam.uninstall();
        mocks.close();
    }

    /**
     * The invariant the whole window hangs on. A publish rewrites a parent before adding a second
     * child to it, and it updates an element in place rather than copying the previous revision's
     * sub_element_ids forward when it finds the row already there - both decided by a read of a row
     * the publish itself has just written.
     */
    @Test
    public void testAReadWaitsForAWriteToTheSameRowThatIsStillInFlight() {
        Map<String, String> storedIds = new HashMap<>();
        givenStatefulVersionElements(storedIds);
        Row[] parentRow = new Row[1];
        doAnswer(invocation -> resultSetOf(parentRow[0])).when(elementAccessor)
                .get(PUBLIC_SPACE, "item-1", "version-2", "parent-5", "revision-3");
        effectOf = statement -> {
            if (ElementAccessor.ADD_SUB_ELEMENTS.equals(queryOf(statement))) {
                parentRow[0] = fullElementRow();
            }
        };

        repository.create(context, publicContext(), fullElement());

        Assert.assertTrue(repository.get(context, publicContext(), new ElementEntity(PARENT_ID)).isPresent(),
                "the sub element the create added to the parent is still in flight, so a read of that parent has "
                        + "to wait for it - a read that overtook it would rewrite the parent from the row as it was "
                        + "before");
    }

    @Test
    public void testAReadDoesNotWaitForWritesToOtherRows() {
        givenVersionElements(elementIdsRow(mapOf("element-4", "revision-3", "element-7", "revision-3")));
        givenElement("element-7", "revision-3", fullElementRow());
        ElementEntity element = fullElement();
        element.setParentId(null);

        repository.create(context, publicContext(), element);
        repository.get(context, publicContext(), new ElementEntity(new Id("element-7")));

        Assert.assertEquals(issued.size(), 1);
        Assert.assertTrue(settled.isEmpty(),
                "a read of another element must not drain the window, or the publish is back to one round trip at "
                        + "a time");
    }

    @Test
    public void testAReadOfAnotherRevisionOfTheSameElementIsNotDrained() {
        givenVersionElements(elementIdsRow(mapOf("element-4", "revision-9")));
        givenElement("element-4", "revision-9", fullElementRow());
        ElementEntity element = fullElement();
        element.setParentId(null);

        repository.create(context, publicContext(), element);
        repository.get(context, publicContext(), new ElementEntity(ELEMENT_ID));

        Assert.assertEquals(issued.size(), 1);
        Assert.assertTrue(settled.isEmpty());
    }

    @Test
    public void testEveryElementWriteOfACreateGoesThroughTheWindow() {
        repository.create(context, publicContext(), fullElement());

        verify(elementAccessor).createStatement(eq(PUBLIC_SPACE), eq("item-1"), eq("version-2"), eq("element-4"),
                eq("revision-3"), eq("parent-5"), eq(NAMESPACE_VALUE), Mockito.anyString(), Mockito.anyString(),
                Mockito.any(), Mockito.any(), Mockito.any(), eq(new HashSet<>(Arrays.asList("sub-9", "sub-10"))),
                eq("hash-6"));
        verify(elementAccessor).addSubElementsStatement(Collections.singleton("element-4"), PUBLIC_SPACE,
                "item-1", "version-2", "parent-5", "revision-3");
        verify(elementAccessor, never()).create(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anySet(),
                Mockito.anyString());
        Assert.assertEquals(issued.size(), 2);
    }

    @Test
    public void testAnUpdateInPlaceGoesThroughTheWindow() {
        givenVersionElements(elementIdsRow(mapOf("element-4", "revision-3")));

        repository.update(context, publicContext(), fullElement());

        verify(elementAccessor).updateStatement(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.any(), eq("hash-6"), eq("parent-5"), eq(PUBLIC_SPACE), eq("item-1"), eq("version-2"),
                eq("element-4"), eq("revision-3"));
        Assert.assertEquals(issued.size(), 1);
    }

    @Test
    public void testADeleteAndItsParentDetachGoThroughTheWindow() {
        givenVersionElements(elementIdsRow(mapOf("parent-5", "revision-3")));
        givenElement("parent-5", "revision-3", fullElementRow());

        repository.delete(context, publicContext(), fullElement());

        verify(elementAccessor).removeSubElementsStatement(Collections.singleton("element-4"), PUBLIC_SPACE,
                "item-1", "version-2", "parent-5", "revision-3");
        verify(elementAccessor).deleteStatement(PUBLIC_SPACE, "item-1", "version-2", "element-4", "revision-3");
        verify(elementAccessor, never()).delete(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    /**
     * cleanAllRevisions deletes every revision of an element at once, so it cannot be tracked against
     * a single row - and no publish reaches it, so there is nothing to gain by trying.
     */
    @Test
    public void testCleanAllRevisionsIsSentStraightAwayEvenInsideAWindow() {
        repository.cleanAllRevisions(context, publicContext(), fullElement());

        verify(elementAccessor).deleteAllRevisions(PUBLIC_SPACE, "item-1", "version-2", "element-4");
        Assert.assertTrue(issued.isEmpty());
    }

    @Test
    public void testWithoutAWindowTheWriteIsSentStraightAway() {
        ElementWriteWindow.discard();
        ElementEntity element = fullElement();
        element.setParentId(null);

        repository.create(context, publicContext(), element);

        verify(elementAccessor).create(eq(PUBLIC_SPACE), eq("item-1"), eq("version-2"), eq("element-4"),
                eq("revision-3"), eq((String) null), eq(NAMESPACE_VALUE), Mockito.anyString(), Mockito.anyString(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anySet(), eq("hash-6"));
        verify(elementAccessor, never()).createStatement(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anySet(),
                Mockito.anyString());
        Assert.assertTrue(issued.isEmpty());
    }

    @Test
    public void testTheWindowNeverHoldsMoreWritesThanItsSize() {
        for (int i = 0; i < ElementWriteWindow.WINDOW_SIZE + 3; i++) {
            ElementWriteWindow.submit(context, PUBLIC_SPACE, "item-1", "version-2", "element-" + i, "revision-3",
                    () -> new SimpleStatement("UPDATE element SET info=? WHERE element_id=?"));
        }

        Assert.assertEquals(issued.size(), ElementWriteWindow.WINDOW_SIZE + 3);
        Assert.assertEquals(settled.size(), 3);
        Assert.assertEquals(settled, issued.subList(0, 3), "the window has to make room by waiting for its oldest");
    }

    @Test
    public void testDrainLooksAtEveryWriteAndReportsTheFirstThatFailed() {
        RuntimeException first = new IllegalStateException("write of element-4 failed");
        RuntimeException second = new IllegalStateException("write of the parent failed");
        failureOf = statement -> ElementAccessor.ADD_SUB_ELEMENTS.equals(queryOf(statement)) ? second : first;

        repository.create(context, publicContext(), fullElement());

        try {
            ElementWriteWindow.drain();
            Assert.fail("a failed write must not be reported as a successful publish");
        } catch (RuntimeException reported) {
            Assert.assertSame(reported, first);
        }
        Assert.assertEquals(settled.size(), 2,
                "a future nobody looks at turns a failed write into silent data loss, so draining has to look at "
                        + "all of them even after the first failure");
    }

    @Test
    public void testDrainIsSafeToRunTwice() {
        ElementEntity element = fullElement();
        element.setParentId(null);
        repository.create(context, publicContext(), element);

        ElementWriteWindow.drain();
        ElementWriteWindow.drain();

        Assert.assertEquals(settled.size(), 1);
    }

    /**
     * The window is only correct if something actually drains it, and the entry point is the only
     * place that does. A publish that left its writes in flight would report success before Cassandra
     * had accepted them, with the rest of the suite still green.
     */
    @Test
    public void testPublishItemVersionWaitsForTheWritesThePublishIssued() throws Exception {
        CassandraCollaborationStorePluginImpl plugin = pluginPublishing(invocation -> {
            ElementEntity element = fullElement();
            element.setParentId(null);
            repository.create(context, publicContext(), element);
            return new CollaborationPublishResult();
        });

        plugin.publishItemVersion(context, ITEM_ID, VERSION_ID, "publish message");

        Assert.assertEquals(issued.size(), 1);
        Assert.assertEquals(settled, issued);
    }

    /**
     * Waiting for the writes is not enough on its own - the wait that ends the publish has to be the
     * one that reports, or a write that Cassandra refused is answered with a successful publish.
     */
    @Test
    public void testPublishItemVersionDoesNotReportSuccessWhenAWriteFailed() throws Exception {
        RuntimeException refused = new IllegalStateException("Cassandra refused the element row");
        failureOf = statement -> refused;
        CassandraCollaborationStorePluginImpl plugin = pluginPublishing(invocation -> {
            ElementEntity element = fullElement();
            element.setParentId(null);
            repository.create(context, publicContext(), element);
            return new CollaborationPublishResult();
        });

        try {
            plugin.publishItemVersion(context, ITEM_ID, VERSION_ID, "publish message");
            Assert.fail("a publish whose element row was refused must not answer with a result");
        } catch (RuntimeException reported) {
            Assert.assertSame(reported, refused);
        }
    }

    @Test
    public void testPublishItemVersionWaitsForItsWritesEvenWhenThePublishFails() throws Exception {
        CassandraCollaborationStorePluginImpl plugin = pluginPublishing(invocation -> {
            ElementEntity element = fullElement();
            element.setParentId(null);
            repository.create(context, publicContext(), element);
            throw new ZusammenException(new ReturnCode(4242, Module.ZCSP, "publish failed", null));
        });

        plugin.publishItemVersion(context, ITEM_ID, VERSION_ID, "publish message");

        Assert.assertEquals(issued.size(), 1);
        Assert.assertEquals(settled, issued,
                "a publish must not hand the request thread back with writes of its own still outstanding");
    }

    private CassandraCollaborationStorePluginImpl pluginPublishing(Answer<CollaborationPublishResult> publish)
            throws Exception {
        PublishService publishService = Mockito.mock(PublishService.class);
        when(publishService.publish(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenAnswer(publish);

        CassandraCollaborationStorePluginImpl plugin = new CassandraCollaborationStorePluginImpl();
        Field field = CassandraCollaborationStorePluginImpl.class.getDeclaredField("publishService");
        field.setAccessible(true);
        field.set(plugin, publishService);
        return plugin;
    }

    /**
     * The driver binds an {@code @Accessor} method that returns a {@link Statement} instead of
     * executing it; a simple statement carrying the query text stands in for the bound one, which is
     * also how a test tells the statements apart.
     */
    private void givenTheAccessorBindsTheStatementsItIsAskedFor() {
        when(elementAccessor.createStatement(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anySet(),
                Mockito.anyString())).thenReturn(new SimpleStatement(ElementAccessor.CREATE));
        when(elementAccessor.updateStatement(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new SimpleStatement(ElementAccessor.UPDATE_WITH_PARENT));
        when(elementAccessor.updateStatement(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString())).thenReturn(new SimpleStatement(ElementAccessor.UPDATE));
        when(elementAccessor.deleteStatement(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString())).thenReturn(new SimpleStatement(ElementAccessor.DELETE));
        when(elementAccessor.addSubElementsStatement(Mockito.anySet(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new SimpleStatement(ElementAccessor.ADD_SUB_ELEMENTS));
        when(elementAccessor.removeSubElementsStatement(Mockito.anySet(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new SimpleStatement(ElementAccessor.REMOVE_SUB_ELEMENTS));
    }

    private void givenTheSessionSendsWithoutWaiting() {
        when(CassandraAccessorSeam.session().executeAsync(Mockito.any(Statement.class))).thenAnswer(invocation -> {
            Statement statement = invocation.getArgument(0);
            issued.add(statement);
            return new PendingWrite(statement);
        });
    }

    private void givenElement(String elementId, String revisionId, Row row) {
        doReturn(resultSetOf(row)).when(elementAccessor)
                .get(PUBLIC_SPACE, "item-1", "version-2", elementId, revisionId);
    }

    private void givenVersionElements(Row row) {
        doReturn(resultSetOf(row)).when(versionElementsAccessor)
                .get(PUBLIC_SPACE, "item-1", "version-2", "revision-3");
    }

    /** Makes the version_elements row behave like Cassandra: what addElements writes, a read returns. */
    private void givenStatefulVersionElements(Map<String, String> stored) {
        givenVersionElements(elementIdsRow(stored));
        doAnswer(invocation -> {
            stored.putAll(invocation.<Map<String, String>>getArgument(0));
            return null;
        }).when(versionElementsAccessor)
                .addElements(Mockito.anyMap(), eq(PUBLIC_SPACE), eq("item-1"), eq("version-2"), eq("revision-3"));
    }

    private static String queryOf(Statement statement) {
        return ((SimpleStatement) statement).getQueryString();
    }

    private static ElementEntityContext publicContext() {
        return new ElementEntityContext(PUBLIC_SPACE, ITEM_ID, VERSION_ID, REVISION_ID);
    }

    private static ElementEntity fullElement() {
        ElementEntity element = new ElementEntity(ELEMENT_ID);
        element.setParentId(PARENT_ID);
        Namespace namespace = new Namespace();
        namespace.setValue(NAMESPACE_VALUE);
        element.setNamespace(namespace);
        Info info = new Info();
        info.setName("element-4-info");
        element.setInfo(info);
        element.setRelations(new ArrayList<>());
        element.setData(byteBuffer("data"));
        element.setSubElementIds(new HashSet<>(Arrays.asList(new Id("sub-9"), new Id("sub-10"))));
        element.setElementHash(ELEMENT_HASH);
        return element;
    }

    private static Row fullElementRow() {
        Row row = Mockito.mock(Row.class);
        when(row.getString("parent_id")).thenReturn("parent-5");
        when(row.getString("namespace")).thenReturn(NAMESPACE_VALUE);
        when(row.getString("info")).thenReturn(JsonUtil.object2Json(new Info()));
        when(row.getSet("sub_element_ids", String.class)).thenReturn(new HashSet<>(Arrays.asList("sub-9")));
        when(row.getString("element_hash")).thenReturn("hash-6");
        return row;
    }

    private static Row elementIdsRow(Map<String, String> elementIds) {
        Row row = Mockito.mock(Row.class);
        when(row.getMap("element_ids", String.class, String.class)).thenReturn(elementIds);
        return row;
    }

    private static Map<String, String> mapOf(String... keysAndValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private static ResultSet resultSetOf(Row row) {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.one()).thenReturn(row);
        return resultSet;
    }

    private static ByteBuffer byteBuffer(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A write that has reached Cassandra but has not been reported yet: nothing it changes is visible
     * until whoever holds it asks for the outcome.
     */
    private final class PendingWrite implements ResultSetFuture {

        private final Statement statement;
        private boolean done;

        private PendingWrite(Statement statement) {
            this.statement = statement;
        }

        @Override
        public ResultSet getUninterruptibly() {
            if (!done) {
                done = true;
                settled.add(statement);
                effectOf.accept(statement);
            }
            RuntimeException failure = failureOf.apply(statement);
            if (failure != null) {
                throw failure;
            }
            return null;
        }

        @Override
        public ResultSet getUninterruptibly(long timeout, TimeUnit unit) {
            return getUninterruptibly();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public ResultSet get() {
            return getUninterruptibly();
        }

        @Override
        public ResultSet get(long timeout, TimeUnit unit) {
            return getUninterruptibly();
        }

        @Override
        public void addListener(Runnable listener, Executor executor) {
            executor.execute(listener);
        }
    }
}
