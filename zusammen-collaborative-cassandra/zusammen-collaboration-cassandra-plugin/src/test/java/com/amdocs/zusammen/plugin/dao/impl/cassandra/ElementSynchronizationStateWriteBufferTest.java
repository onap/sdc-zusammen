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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.plugin.collaboration.PublishService;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementSynchronizationStateRepositoryImpl.ElementSynchronizationStateAccessor;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementSynchronizationStateRepositoryImpl.VersionElementsAccessor;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.main.CassandraCollaborationStorePluginImpl;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationPublishResult;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ElementSynchronizationStateWriteBufferTest {

    private static final String TENANT = "ElementSyncStateWriteBufferTest_tenant";
    private static final String USER = "ElementSyncStateWriteBufferTest_user";
    private static final String SPACE = "ElementSyncStateWriteBufferTest_space";
    private static final String OTHER_SPACE = "ElementSyncStateWriteBufferTest_other_space";
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id VERSION_REVISION_ID = new Id("version-revision-3");
    private static final Id ELEMENT_REVISION_ID = new Id("element-revision-5");
    private static final Date PUBLISH_TIME = new Date(1_700_000_000_000L);
    private static final Date LATER_PUBLISH_TIME = new Date(1_700_000_060_000L);

    /** The deployed cassandra.yaml warns above this size, so a chunk has to stay below it. */
    private static final int BATCH_SIZE_WARN_THRESHOLD_IN_BYTES = 5 * 1024;
    /** As long as an md5 digest, which is what a prepared statement puts on the wire. */
    private static final String PREPARED_STATEMENT_ID = "0123456789abcdef";

    @Mock
    private ElementSynchronizationStateAccessor syncStateAccessor;
    @Mock
    private VersionElementsAccessor versionElementsAccessor;
    @Mock
    private ElementRepositoryImpl.VersionElementsAccessor elementIdsAccessor;

    private AutoCloseable mocks;
    private SessionContext context;
    private ElementSynchronizationStateRepositoryImpl repository;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        CassandraAccessorSeam.install();
        CassandraAccessorSeam.registerAccessor(ElementSynchronizationStateAccessor.class, syncStateAccessor);
        CassandraAccessorSeam.registerAccessor(VersionElementsAccessor.class, versionElementsAccessor);
        CassandraAccessorSeam.registerAccessor(ElementRepositoryImpl.VersionElementsAccessor.class,
                elementIdsAccessor);
        givenTheAccessorBindsTheStatementsItIsAskedFor();

        context = new SessionContext();
        context.setUser(new UserInfo(USER));
        context.setTenant(TENANT);
        repository = new ElementSynchronizationStateRepositoryImpl();
        ElementSynchronizationStateWriteBuffer.open();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        ElementSynchronizationStateWriteBuffer.discard();
        CassandraAccessorSeam.uninstall();
        mocks.close();
    }

    @Test
    public void testStatesWrittenWhileBufferingAreSentAsOneUnloggedBatch() throws Exception {
        repository.update(context, elementContext(SPACE), syncState("element-4", false));
        repository.update(context, elementContext(SPACE), syncState("element-5", false));
        repository.update(context, elementContext(SPACE), syncState("element-6", false));

        verify(syncStateAccessor, never()).update(Mockito.any(), Mockito.anyBoolean(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Assert.assertTrue(sentBatches().isEmpty());

        ElementSynchronizationStateWriteBuffer.flush(context);

        List<BatchStatement> batches = sentBatches();
        Assert.assertEquals(batches.size(), 1);
        Assert.assertEquals(elementIdsIn(batches.get(0)), Arrays.asList("element-4", "element-5", "element-6"));
        Assert.assertEquals(batchTypeOf(batches.get(0)), BatchStatement.Type.UNLOGGED);
        Assert.assertEquals(batches.get(0).isIdempotent(), Boolean.TRUE);
    }

    @Test
    public void testEachPartitionIsBatchedOnItsOwn() {
        repository.update(context, elementContext(SPACE), syncState("element-4", false));
        repository.update(context, elementContext(OTHER_SPACE), syncState("element-5", false));

        ElementSynchronizationStateWriteBuffer.flush(context);

        List<BatchStatement> batches = sentBatches();
        Assert.assertEquals(batches.size(), 2);
        Assert.assertEquals(elementIdsIn(batches.get(0)), Arrays.asList("element-4"));
        Assert.assertEquals(elementIdsIn(batches.get(1)), Arrays.asList("element-5"));
    }

    /**
     * Sizes are asserted on statements shaped like the ones production sends - uuid keys, and the
     * query travelling as a prepared-statement id rather than as text - because that is what has to
     * fit under the server's threshold. The statements the rest of this class uses carry their query
     * string and are several times bigger.
     */
    @Test
    public void testBatchesAreChunkedToStayUnderTheServerWarningThreshold() {
        String itemId = UUID.randomUUID().toString();
        String versionId = UUID.randomUUID().toString();
        String revisionId = UUID.randomUUID().toString();
        for (int i = 0; i < 50; i++) {
            String elementId = UUID.randomUUID().toString();
            ElementSynchronizationStateWriteBuffer.accumulate(SPACE, itemId, versionId, elementId, revisionId,
                    () -> new SimpleStatement(PREPARED_STATEMENT_ID, PUBLISH_TIME, false, SPACE, itemId, versionId,
                            elementId, revisionId));
        }

        ElementSynchronizationStateWriteBuffer.flush(context);

        List<BatchStatement> batches = sentBatches();
        Assert.assertEquals(batches.size(), 3);
        Assert.assertEquals(batches.get(0).size(), 20);
        Assert.assertEquals(batches.get(1).size(), 20);
        Assert.assertEquals(batches.get(2).size(), 10);
        for (BatchStatement batch : batches) {
            Assert.assertTrue(
                    batch.requestSizeInBytes(ProtocolVersion.V4, CodecRegistry.DEFAULT_INSTANCE)
                            < BATCH_SIZE_WARN_THRESHOLD_IN_BYTES,
                    "chunk of " + batch.size() + " statements is too big for the server's warning threshold");
        }
    }

    /**
     * A batch applies all its mutations under one timestamp, so the second write of a row must not
     * join the first one - publishing an element writes its state, and publishing a child of it
     * writes the same row again through the parent update, with a different publish time.
     */
    @Test
    public void testARepeatWriteOfTheSameRowGoesIntoALaterBatch() {
        repository.update(context, elementContext(SPACE), syncState("element-4", PUBLISH_TIME));
        repository.update(context, elementContext(SPACE), syncState("element-4", LATER_PUBLISH_TIME));

        ElementSynchronizationStateWriteBuffer.flush(context);

        List<BatchStatement> batches = sentBatches();
        Assert.assertEquals(batches.size(), 2);
        Assert.assertEquals(publishTimesIn(batches.get(0)), Arrays.asList(PUBLISH_TIME));
        Assert.assertEquals(publishTimesIn(batches.get(1)), Arrays.asList(LATER_PUBLISH_TIME));
    }

    @Test
    public void testWritesToTwoRevisionsOfOneElementShareABatch() {
        repository.update(context, elementContext(SPACE), syncState("element-4", false));
        repository.markAsDirty(context, elementContext(SPACE), syncState("element-4", false));

        ElementSynchronizationStateWriteBuffer.flush(context);

        List<BatchStatement> batches = sentBatches();
        Assert.assertEquals(batches.size(), 1);
        Assert.assertEquals(revisionIdsIn(batches.get(0)),
                Arrays.asList(ELEMENT_REVISION_ID.getValue(), VERSION_REVISION_ID.getValue()));
    }

    @Test
    public void testUpdateBatchesTheStateOfTheElementRevision() {
        repository.update(context, elementContext(SPACE), syncState("element-4", true));

        verify(syncStateAccessor).updateStatement(PUBLISH_TIME, true, SPACE, "item-1", "version-2", "element-4",
                "element-revision-5");
    }

    @Test
    public void testCreateBatchesTheStateOfTheElementRevision() {
        repository.create(context, elementContext(SPACE), syncState("element-4", false));

        verify(syncStateAccessor).updateStatement(PUBLISH_TIME, false, SPACE, "item-1", "version-2", "element-4",
                "element-revision-5");
    }

    @Test
    public void testMarkAsDirtyBatchesTheDirtyFlagOfTheVersionRevision() {
        repository.markAsDirty(context, elementContext(SPACE), syncState("element-4", false));

        verify(syncStateAccessor).updateDirtyStatement(true, SPACE, "item-1", "version-2", "element-4",
                "version-revision-3");
    }

    @Test
    public void testDeleteBatchesTheRemovalOfTheVersionRevision() {
        repository.delete(context, elementContext(SPACE), syncState("element-4", true));

        verify(syncStateAccessor).deleteStatement(SPACE, "item-1", "version-2", "element-4", "version-revision-3");
    }

    @Test
    public void testDiscardedWritesAreNeverSent() {
        repository.update(context, elementContext(SPACE), syncState("element-4", false));

        ElementSynchronizationStateWriteBuffer.discard();
        ElementSynchronizationStateWriteBuffer.flush(context);

        Assert.assertTrue(sentBatches().isEmpty());
    }

    @Test
    public void testFlushingTwiceSendsTheBatchOnce() {
        repository.update(context, elementContext(SPACE), syncState("element-4", false));

        ElementSynchronizationStateWriteBuffer.flush(context);
        ElementSynchronizationStateWriteBuffer.flush(context);

        Assert.assertEquals(sentBatches().size(), 1);
    }

    @Test
    public void testWithoutABufferTheWriteIsSentStraightAway() {
        ElementSynchronizationStateWriteBuffer.discard();

        repository.update(context, elementContext(SPACE), syncState("element-4", false));

        verify(syncStateAccessor).update(PUBLISH_TIME, false, SPACE, "item-1", "version-2", "element-4",
                "element-revision-5");
        Assert.assertTrue(sentBatches().isEmpty());
    }

    /**
     * The buffer is only correct if something actually flushes it, and the entry point is the only
     * place that does. A publish that buffered and never flushed would drop every state it wrote with
     * the rest of the suite still green.
     */
    @Test
    public void testPublishItemVersionSendsWhatThePublishBuffered() throws Exception {
        CassandraCollaborationStorePluginImpl plugin = pluginPublishing(invocation -> {
            repository.update(context, elementContext(SPACE), syncState("element-4", false));
            return new CollaborationPublishResult();
        });

        plugin.publishItemVersion(context, ITEM_ID, VERSION_ID, "publish message");

        List<BatchStatement> batches = sentBatches();
        Assert.assertEquals(batches.size(), 1);
        Assert.assertEquals(elementIdsIn(batches.get(0)), Arrays.asList("element-4"));
    }

    @Test
    public void testPublishItemVersionSendsNothingWhenThePublishFails() throws Exception {
        CassandraCollaborationStorePluginImpl plugin = pluginPublishing(invocation -> {
            repository.update(context, elementContext(SPACE), syncState("element-4", false));
            throw new ZusammenException(new ReturnCode(4242, Module.ZCSP, "publish failed", null));
        });

        plugin.publishItemVersion(context, ITEM_ID, VERSION_ID, "publish message");

        Assert.assertTrue(sentBatches().isEmpty());
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
     * executing it. A simple statement stands in for the bound one: it carries the query text rather
     * than a prepared id, so a batch of these is bigger than what production sends, never smaller.
     */
    private void givenTheAccessorBindsTheStatementsItIsAskedFor() {
        when(syncStateAccessor.updateStatement(Mockito.any(), Mockito.anyBoolean(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(invocation -> new SimpleStatement(ElementSynchronizationStateAccessor.UPDATE,
                        invocation.getArguments()));
        when(syncStateAccessor.updateDirtyStatement(Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(invocation -> new SimpleStatement(ElementSynchronizationStateAccessor.UPDATE_DIRTY,
                        invocation.getArguments()));
        when(syncStateAccessor.deleteStatement(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(invocation -> new SimpleStatement(ElementSynchronizationStateAccessor.DELETE,
                        invocation.getArguments()));
    }

    private static List<BatchStatement> sentBatches() {
        ArgumentCaptor<Statement> sent = ArgumentCaptor.forClass(Statement.class);
        verify(CassandraAccessorSeam.session(), Mockito.atLeast(0)).execute(sent.capture());

        List<BatchStatement> batches = new ArrayList<>();
        for (Statement statement : sent.getAllValues()) {
            batches.add((BatchStatement) statement);
        }
        return batches;
    }

    private static List<String> elementIdsIn(BatchStatement batch) {
        // Every batched query ends with "element_id=? AND revision_id=?".
        return valuesAt(batch, -2);
    }

    private static List<String> revisionIdsIn(BatchStatement batch) {
        return valuesAt(batch, -1);
    }

    private static List<Date> publishTimesIn(BatchStatement batch) {
        List<Date> publishTimes = new ArrayList<>();
        for (Statement statement : batch.getStatements()) {
            publishTimes.add((Date) ((SimpleStatement) statement).getObject(0));
        }
        return publishTimes;
    }

    private static List<String> valuesAt(BatchStatement batch, int indexFromEnd) {
        List<String> values = new ArrayList<>();
        for (Statement statement : batch.getStatements()) {
            SimpleStatement bound = (SimpleStatement) statement;
            values.add((String) bound.getObject(bound.valuesCount() + indexFromEnd));
        }
        return values;
    }

    private static BatchStatement.Type batchTypeOf(BatchStatement batch) throws Exception {
        Field field = BatchStatement.class.getDeclaredField("batchType");
        field.setAccessible(true);
        return (BatchStatement.Type) field.get(batch);
    }

    private static ElementEntityContext elementContext(String space) {
        return new ElementEntityContext(space, ITEM_ID, VERSION_ID, VERSION_REVISION_ID);
    }

    private static SynchronizationStateEntity syncState(String elementId, boolean dirty) {
        return new SynchronizationStateEntity(new Id(elementId), ELEMENT_REVISION_ID, PUBLISH_TIME, dirty);
    }

    private static SynchronizationStateEntity syncState(String elementId, Date publishTime) {
        return new SynchronizationStateEntity(new Id(elementId), ELEMENT_REVISION_ID, publishTime, false);
    }
}
