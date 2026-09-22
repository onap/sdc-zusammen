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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Collects the collection updates a publish makes to <code>version_elements</code> and sends them
 * once per row at the end, instead of one statement per element.
 *
 * <p>A publish touches a single <code>version_elements</code> row per space - the table is keyed by
 * <code>((space, item_id, version_id), revision_id)</code> - and it touches that row four times per
 * element: twice to register element ids, twice to clear dirty markers. Both columns are collections
 * and both accessors already take a whole map or set, so the per-element updates of one row can be
 * accumulated and written as one statement per column and direction. The resulting row is the same,
 * because the accumulator keeps the last operation per element id, which is what applying the
 * updates one by one would leave behind.
 *
 * <p>Nothing reads these columns out of Cassandra while a publish is running:
 * {@link VersionElementIdsCache} answers the reads from memory and is kept up to date by the same
 * call sites. A publish that fails discards its buffer rather than flushing it, which leaves the
 * revision holding the content it was seeded with instead of a half-registered one.
 *
 * <p>Like {@link VersionElementIdsCache} the scope is a thread local, because the repositories are
 * process-wide singletons and cannot hold request state.
 */
public final class VersionElementsWriteBuffer {

    private static final ThreadLocal<Map<RowKey, PendingUpdates>> SCOPE = new ThreadLocal<>();

    private VersionElementsWriteBuffer() {
    }

    /**
     * Starts buffering on the calling thread. Until {@link #flush} runs, the accumulated updates
     * have not reached Cassandra.
     */
    public static void open() {
        SCOPE.set(new HashMap<>());
    }

    /**
     * Sends what was accumulated. Safe to call twice - the second call has nothing left to send.
     */
    public static void flush(SessionContext context) {
        Map<RowKey, PendingUpdates> scope = SCOPE.get();
        if (scope == null || scope.isEmpty()) {
            return;
        }

        ElementRepositoryImpl.VersionElementsAccessor elementIds =
                CassandraDaoUtils.getAccessor(context, ElementRepositoryImpl.VersionElementsAccessor.class);
        ElementSynchronizationStateRepositoryImpl.VersionElementsAccessor dirtyElementIds = CassandraDaoUtils
                .getAccessor(context, ElementSynchronizationStateRepositoryImpl.VersionElementsAccessor.class);

        for (Map.Entry<RowKey, PendingUpdates> row : scope.entrySet()) {
            RowKey key = row.getKey();
            PendingUpdates pending = row.getValue();
            if (!pending.addedElements.isEmpty()) {
                elementIds.addElements(pending.addedElements, key.space, key.itemId, key.versionId, key.revisionId);
            }
            if (!pending.removedElements.isEmpty()) {
                elementIds.removeElements(pending.removedElements, key.space, key.itemId, key.versionId,
                        key.revisionId);
            }
            if (!pending.addedDirtyElements.isEmpty()) {
                dirtyElementIds.addDirtyElements(pending.addedDirtyElements, key.space, key.itemId, key.versionId,
                        key.revisionId);
            }
            if (!pending.removedDirtyElements.isEmpty()) {
                dirtyElementIds.removeDirtyElements(pending.removedDirtyElements, key.space, key.itemId, key.versionId,
                        key.revisionId);
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

    static boolean addElements(String space, String itemId, String versionId, String revisionId,
            Map<String, String> elementIds) {
        PendingUpdates pending = pendingFor(space, itemId, versionId, revisionId);
        if (pending == null) {
            return false;
        }
        pending.removedElements.removeAll(elementIds.keySet());
        pending.addedElements.putAll(elementIds);
        return true;
    }

    static boolean removeElements(String space, String itemId, String versionId, String revisionId,
            Set<String> elementIds) {
        PendingUpdates pending = pendingFor(space, itemId, versionId, revisionId);
        if (pending == null) {
            return false;
        }
        pending.addedElements.keySet().removeAll(elementIds);
        pending.removedElements.addAll(elementIds);
        return true;
    }

    static boolean addDirtyElements(String space, String itemId, String versionId, String revisionId,
            Set<String> elementIds) {
        PendingUpdates pending = pendingFor(space, itemId, versionId, revisionId);
        if (pending == null) {
            return false;
        }
        pending.removedDirtyElements.removeAll(elementIds);
        pending.addedDirtyElements.addAll(elementIds);
        return true;
    }

    static boolean removeDirtyElements(String space, String itemId, String versionId, String revisionId,
            Set<String> elementIds) {
        PendingUpdates pending = pendingFor(space, itemId, versionId, revisionId);
        if (pending == null) {
            return false;
        }
        pending.addedDirtyElements.removeAll(elementIds);
        pending.removedDirtyElements.addAll(elementIds);
        return true;
    }

    private static PendingUpdates pendingFor(String space, String itemId, String versionId, String revisionId) {
        Map<RowKey, PendingUpdates> scope = SCOPE.get();
        if (scope == null) {
            return null;
        }
        RowKey key = new RowKey(space, itemId, versionId, revisionId);
        PendingUpdates pending = scope.get(key);
        if (pending == null) {
            pending = new PendingUpdates();
            scope.put(key, pending);
        }
        return pending;
    }

    private static final class PendingUpdates {

        private final Map<String, String> addedElements = new HashMap<>();
        private final Set<String> removedElements = new HashSet<>();
        private final Set<String> addedDirtyElements = new HashSet<>();
        private final Set<String> removedDirtyElements = new HashSet<>();
    }

    private static final class RowKey {

        private final String space;
        private final String itemId;
        private final String versionId;
        private final String revisionId;

        private RowKey(String space, String itemId, String versionId, String revisionId) {
            this.space = space;
            this.itemId = itemId;
            this.versionId = versionId;
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
                           && Objects.equals(revisionId, that.revisionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(space, itemId, versionId, revisionId);
        }
    }
}
