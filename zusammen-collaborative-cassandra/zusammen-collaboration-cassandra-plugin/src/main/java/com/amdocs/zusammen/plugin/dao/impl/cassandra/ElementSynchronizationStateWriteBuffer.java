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

import com.amdocs.zusammen.datatypes.SessionContext;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Collects the <code>element_synchronization_state</code> writes a publish makes and sends them as
 * a few batches at the end, instead of one statement per element.
 *
 * <p>The table is keyed by <code>((space, item_id, version_id), element_id, revision_id)</code>, so
 * all the writes a publish makes to one space land in a single partition and can go out as one
 * UNLOGGED batch per chunk - which the coordinator applies as a single local mutation rather than
 * one round trip per element. Batches are kept per partition and chunked, because a batch that
 * spans partitions or grows past a few kilobytes is what the server warns about, not batching as
 * such.
 *
 * <p>A batch carries <b>one timestamp for all of its mutations</b>, so two writes of the same cell
 * must not share one: publishing an element writes its sync state, and publishing its children
 * writes it again through the parent update, with a publish time that need not be the same. A
 * repeat write to a row therefore closes the chunk it collides with, which puts the two writes in
 * successive batches and keeps the later one later.
 *
 * <p>Nothing in a publish reads <code>element_synchronization_state</code> after writing it - both
 * <code>PublishService</code> branches list the states up front - so holding the writes back to the
 * end of the publish is invisible to it. A publish that fails discards its buffer rather than
 * flushing it.
 *
 * <p>Like {@link VersionElementsWriteBuffer} the scope is a thread local, because the repositories
 * are process-wide singletons and cannot hold request state.
 */
public final class ElementSynchronizationStateWriteBuffer {

    /**
     * The deployed cassandra.yaml warns at a batch of 5 kB. One of these writes is four uuids, a
     * space name, a timestamp and a flag, so twenty of them fit with room to spare and twenty-five
     * do not.
     */
    private static final int CHUNK_SIZE = 20;

    private static final ThreadLocal<Map<PartitionKey, PendingBatches>> SCOPE = new ThreadLocal<>();

    private ElementSynchronizationStateWriteBuffer() {
    }

    /**
     * Starts buffering on the calling thread. Until {@link #flush} runs, the accumulated writes have
     * not reached Cassandra.
     */
    public static void open() {
        SCOPE.set(new LinkedHashMap<>());
    }

    /**
     * Sends what was accumulated. Safe to call twice - the second call has nothing left to send.
     */
    public static void flush(SessionContext context) {
        Map<PartitionKey, PendingBatches> scope = SCOPE.get();
        if (scope == null || scope.isEmpty()) {
            return;
        }

        Session session = CassandraDaoUtils.getSession(context);
        for (PendingBatches pending : scope.values()) {
            for (BatchStatement batch : pending.batches()) {
                session.execute(batch);
            }
        }
        scope.clear();
    }

    /**
     * Ends buffering on the calling thread and drops anything not flushed. Must run on every exit
     * path, because the request threads are pooled and long lived.
     */
    public static void discard() {
        SCOPE.remove();
    }

    /**
     * Takes a write of one sync state row, or reports that no publish is buffering on this thread
     * and the caller has to send it itself. The statement is only asked for once it is going to be
     * kept, so callers outside a publish do not bind one to throw it away.
     */
    static boolean accumulate(String space, String itemId, String versionId, String elementId,
            String revisionId, Supplier<Statement> statement) {
        Map<PartitionKey, PendingBatches> scope = SCOPE.get();
        if (scope == null) {
            return false;
        }

        PartitionKey partition = new PartitionKey(space, itemId, versionId);
        PendingBatches pending = scope.get(partition);
        if (pending == null) {
            pending = new PendingBatches();
            scope.put(partition, pending);
        }
        pending.add(new RowKey(elementId, revisionId), statement.get());
        return true;
    }

    private static final class PendingBatches {

        private final List<BatchStatement> closed = new ArrayList<>();
        private final Set<RowKey> rowsInCurrent = new HashSet<>();
        private BatchStatement current = newBatch();

        private void add(RowKey row, Statement statement) {
            if (!rowsInCurrent.add(row)) {
                close();
                rowsInCurrent.add(row);
            }
            current.add(statement);
            if (current.size() == CHUNK_SIZE) {
                close();
            }
        }

        private void close() {
            closed.add(current);
            current = newBatch();
            rowsInCurrent.clear();
        }

        private List<BatchStatement> batches() {
            if (current.size() == 0) {
                return closed;
            }
            List<BatchStatement> all = new ArrayList<>(closed);
            all.add(current);
            return all;
        }

        private static BatchStatement newBatch() {
            BatchStatement batch = new BatchStatement(BatchStatement.Type.UNLOGGED);
            // Every write here sets whole columns of one row - no counter, no conditional update -
            // so a replay of the batch leaves the same row, and the driver may retry it on timeout.
            batch.setIdempotent(true);
            return batch;
        }
    }

    private static final class PartitionKey {

        private final String space;
        private final String itemId;
        private final String versionId;

        private PartitionKey(String space, String itemId, String versionId) {
            this.space = space;
            this.itemId = itemId;
            this.versionId = versionId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PartitionKey)) {
                return false;
            }
            PartitionKey that = (PartitionKey) other;
            return Objects.equals(space, that.space)
                           && Objects.equals(itemId, that.itemId)
                           && Objects.equals(versionId, that.versionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(space, itemId, versionId);
        }
    }

    private static final class RowKey {

        private final String elementId;
        private final String revisionId;

        private RowKey(String elementId, String revisionId) {
            this.elementId = elementId;
            this.revisionId = revisionId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RowKey)) {
                return false;
            }
            RowKey that = (RowKey) other;
            return Objects.equals(elementId, that.elementId)
                           && Objects.equals(revisionId, that.revisionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(elementId, revisionId);
        }
    }
}
