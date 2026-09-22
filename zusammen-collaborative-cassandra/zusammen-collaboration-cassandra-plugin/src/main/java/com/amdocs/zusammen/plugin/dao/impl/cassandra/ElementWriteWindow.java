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
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Issues the <code>element</code> row writes a publish makes without waiting for each of them, and
 * has them all reported before the publish returns.
 *
 * <p>The table is keyed by <code>((space, item_id, version_id, element_id), revision_id)</code>, so
 * every element is its own partition and there is nothing to batch: the only way to stop paying a
 * round trip per element is to have more than one in flight. A publish writes two to four of these
 * rows per element and reads one, and the reads are what keeps the loop sequential - so the window
 * removes the writes from the critical path and leaves the reads on it.
 *
 * <p>Two rules make that safe.
 *
 * <p><b>Read visibility.</b> A publish reads rows it has just written: a second child of the same
 * parent reads that parent before rewriting it, and an element whose child was published first finds
 * its own row already there and updates it in place instead of copying the previous revision's
 * <code>sub_element_ids</code> forward. A read that overtook the write would see the row as it was
 * before, and the row written from what it returned would be built from stale content. Every write
 * is therefore tracked against the row it touches, and a read of a row with a write still in flight
 * waits for it. Keyed on the row rather than on the table, because the element the loop reads is
 * almost never the element it wrote last, so the wait almost never happens.
 *
 * <p><b>Ordering between two writes of one row.</b> Not enforced, and it does not need to be. The
 * driver takes the client timestamp on the calling thread inside <code>executeAsync</code>, from the
 * default {@code AtomicMonotonicTimestampGenerator}, which this codebase does not replace. The
 * statement submitted first therefore carries the smaller timestamp, and Cassandra resolves the two
 * by timestamp rather than by arrival, so the row ends up as it would have done had both writes been
 * awaited in turn.
 *
 * <p>What this does change is what a failed publish leaves behind. Sequential writes left a prefix
 * of the publish applied; an indeterminate subset of the last {@link #WINDOW_SIZE} is applied now.
 * Both are torn - a publish is not a transaction and never was - but the second cannot be described
 * by "it got this far".
 *
 * <p>Like {@link VersionElementsWriteBuffer} and {@link ElementSynchronizationStateWriteBuffer} the
 * scope is a thread local, because the repositories are process-wide singletons and cannot hold
 * request state, and nothing here does anything at all unless a publish opened one.
 */
public final class ElementWriteWindow {

    /**
     * Deep enough that the ~600 element statements of a large publish cost round trips in the tens
     * rather than in the hundreds, and no deeper: a read of the element just written drains the
     * window down to that row anyway, so in practice it holds a handful, and its size is also the
     * number of writes that can be applied-but-unreported when a publish fails.
     */
    static final int WINDOW_SIZE = 16;

    private static final ThreadLocal<Window> SCOPE = new ThreadLocal<>();

    private ElementWriteWindow() {
    }

    /**
     * Starts a window on the calling thread. Until {@link #drain} runs, writes taken by
     * {@link #submit} may not have reached Cassandra.
     */
    public static void open() {
        SCOPE.set(new Window());
    }

    /**
     * Waits for every write the window took and reports the first that failed. Safe to call twice -
     * the second call has nothing left to wait for.
     */
    public static void drain() {
        Window window = SCOPE.get();
        if (window != null) {
            window.drainReportingFailures();
        }
    }

    /**
     * Ends the window on the calling thread. Waits for whatever is still in flight first, so a
     * publish never returns with writes of its own outstanding, and says nothing about how they went
     * - on this path the caller is already failing for another reason. Must run on every exit path,
     * because the request threads are pooled and long lived.
     */
    public static void discard() {
        Window window = SCOPE.get();
        SCOPE.remove();
        if (window != null) {
            window.drainSwallowingFailures();
        }
    }

    /**
     * Sends a write of one element row without waiting for it, or reports that no publish is
     * windowing on this thread and the caller has to send it itself. The statement is only asked for
     * once it is going to be sent this way, so callers outside a publish do not bind one to throw it
     * away.
     */
    static boolean submit(SessionContext context, String space, String itemId, String versionId, String elementId,
            String revisionId, Supplier<Statement> statement) {
        Window window = SCOPE.get();
        if (window == null) {
            return false;
        }
        window.send(context, new RowKey(space, itemId, versionId, elementId, revisionId), statement.get());
        return true;
    }

    /**
     * Waits for the writes of one element row, so that a read of it cannot see the row as it was
     * before them. Does nothing when that row has none outstanding, which is the usual case.
     */
    static void awaitRow(String space, String itemId, String versionId, String elementId, String revisionId) {
        Window window = SCOPE.get();
        if (window != null) {
            window.awaitRow(new RowKey(space, itemId, versionId, elementId, revisionId));
        }
    }

    private static final class Window {

        private final Deque<Pending> inFlight = new ArrayDeque<>();
        private final Map<RowKey, Integer> outstandingPerRow = new HashMap<>();

        private void send(SessionContext context, RowKey row, Statement statement) {
            while (inFlight.size() >= WINDOW_SIZE) {
                settleOldest();
            }
            ResultSetFuture future = CassandraDaoUtils.getSession(context).executeAsync(statement);
            inFlight.addLast(new Pending(row, future));
            outstandingPerRow.merge(row, 1, Integer::sum);
        }

        private void awaitRow(RowKey row) {
            while (outstandingPerRow.containsKey(row)) {
                settleOldest();
            }
        }

        private void drainReportingFailures() {
            RuntimeException firstFailure = null;
            while (!inFlight.isEmpty()) {
                try {
                    settleOldest();
                } catch (RuntimeException failure) {
                    if (firstFailure == null) {
                        firstFailure = failure;
                    }
                }
            }
            if (firstFailure != null) {
                throw firstFailure;
            }
        }

        private void drainSwallowingFailures() {
            while (!inFlight.isEmpty()) {
                try {
                    settleOldest();
                } catch (RuntimeException ignored) {
                    // Reported already, by whatever aborted the publish.
                }
            }
        }

        private void settleOldest() {
            Pending oldest = inFlight.removeFirst();
            outstandingPerRow.compute(oldest.row, (row, outstanding) -> outstanding == 1 ? null : outstanding - 1);
            oldest.future.getUninterruptibly();
        }
    }

    private static final class Pending {

        private final RowKey row;
        private final ResultSetFuture future;

        private Pending(RowKey row, ResultSetFuture future) {
            this.row = row;
            this.future = future;
        }
    }

    private static final class RowKey {

        private final String space;
        private final String itemId;
        private final String versionId;
        private final String elementId;
        private final String revisionId;

        private RowKey(String space, String itemId, String versionId, String elementId, String revisionId) {
            this.space = space;
            this.itemId = itemId;
            this.versionId = versionId;
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
            return Objects.equals(space, that.space)
                           && Objects.equals(itemId, that.itemId)
                           && Objects.equals(versionId, that.versionId)
                           && Objects.equals(elementId, that.elementId)
                           && Objects.equals(revisionId, that.revisionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(space, itemId, versionId, elementId, revisionId);
        }
    }
}
