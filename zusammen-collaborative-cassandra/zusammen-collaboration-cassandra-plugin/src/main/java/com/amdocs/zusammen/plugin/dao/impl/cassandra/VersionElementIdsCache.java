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

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Holds the <code>element_ids</code> map of a <code>version_elements</code> row for the duration of
 * one publish, and keeps it up to date as the publish writes to it.
 *
 * <p>Publishing resolves an element's revision out of that map and writes to the same map as it
 * goes, and the map belongs to a single row per space, because <code>version_elements</code> is
 * keyed by <code>((space, item_id, version_id), revision_id)</code>. Reading the row once and
 * applying the writes in memory therefore removes the repeated reads without changing any statement
 * the publish sends.
 *
 * <p>The scope is a thread local because the repositories are process-wide singletons -
 * {@link ElementRepositoryFactoryImpl} hands the same instance to every request thread - so this
 * state cannot live on them. Nothing in this plugin hands publish work to another thread; a publish
 * loop that ever did would have to pass this along explicitly instead.
 */
public final class VersionElementIdsCache {

    private static final ThreadLocal<Map<RowKey, Map<String, String>>> SCOPE = new ThreadLocal<>();

    private VersionElementIdsCache() {
    }

    /**
     * Starts caching on the calling thread. Publish does not re-enter the plugin's entry points, so
     * nesting is not accounted for: a second open replaces the first.
     */
    public static void open() {
        SCOPE.set(new HashMap<>());
    }

    /**
     * Ends caching on the calling thread. Must run on every exit path - the request threads are
     * pooled and long lived, and a map left behind would answer the next publish on that thread.
     */
    public static void close() {
        SCOPE.remove();
    }

    /**
     * Whether caching is active on the calling thread.
     */
    public static boolean isOpen() {
        return SCOPE.get() != null;
    }

    static Map<String, String> cached(ElementEntityContext elementContext) {
        Map<RowKey, Map<String, String>> scope = SCOPE.get();
        return scope == null ? null : scope.get(new RowKey(elementContext));
    }

    static void remember(ElementEntityContext elementContext, Map<String, String> elementIds) {
        Map<RowKey, Map<String, String>> scope = SCOPE.get();
        if (scope != null) {
            scope.put(new RowKey(elementContext), elementIds);
        }
    }

    /**
     * Applies to the cached row what was just added to <code>element_ids</code>. A row that has not
     * been read is deliberately left absent rather than seeded from the delta: the delta is not the
     * row's content, and the next read fetches the row with this write already applied.
     */
    static void added(ElementEntityContext elementContext, Map<String, String> elementIds) {
        Map<String, String> known = cached(elementContext);
        if (known != null) {
            known.putAll(elementIds);
        }
    }

    static void removed(ElementEntityContext elementContext, Set<String> elementIds) {
        Map<String, String> known = cached(elementContext);
        if (known != null) {
            known.keySet().removeAll(elementIds);
        }
    }

    private static final class RowKey {

        private final String space;
        private final String itemId;
        private final String versionId;
        private final String revisionId;

        private RowKey(ElementEntityContext elementContext) {
            this.space = elementContext.getSpace();
            this.itemId = value(elementContext.getItemId());
            this.versionId = value(elementContext.getVersionId());
            this.revisionId = value(elementContext.getRevisionId());
        }

        private static String value(Id id) {
            return id == null ? null : id.getValue();
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
