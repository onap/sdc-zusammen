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

package com.amdocs.zusammen.adaptor.outbound.impl;

import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.sdk.collaboration.CollaborationStore;
import com.amdocs.zusammen.sdk.collaboration.CollaborationStoreFactory;
import com.amdocs.zusammen.sdk.collaboration.impl.CollaborationStoreFactoryImpl;
import com.amdocs.zusammen.sdk.searchindex.SearchIndex;
import com.amdocs.zusammen.sdk.searchindex.SearchIndexFactory;
import com.amdocs.zusammen.sdk.searchindex.impl.SearchIndexFactoryImpl;
import com.amdocs.zusammen.sdk.state.StateStore;
import com.amdocs.zusammen.sdk.state.StateStoreFactory;
import com.amdocs.zusammen.sdk.state.impl.StateStoreFactoryImpl;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;
import org.testng.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Installs stub SDK plugin factories so the adaptors under test resolve a mock store, and asserts
 * on the {@link ReturnCode} chains they build.
 */
public final class OutboundTestSupport {

    private OutboundTestSupport() {
    }

    public static class StubCollaborationStoreFactory extends CollaborationStoreFactory {
        static CollaborationStore store;
        static RuntimeException failure;
        static SessionContext lastContext;

        @Override
        public CollaborationStore createInterface(SessionContext context) {
            lastContext = context;
            if (failure != null) {
                throw failure;
            }
            return store;
        }
    }

    public static class StubStateStoreFactory extends StateStoreFactory {
        static StateStore store;
        static RuntimeException failure;
        static SessionContext lastContext;

        @Override
        public StateStore createInterface(SessionContext context) {
            lastContext = context;
            if (failure != null) {
                throw failure;
            }
            return store;
        }
    }

    public static class StubSearchIndexFactory extends SearchIndexFactory {
        static SearchIndex store;
        static RuntimeException failure;
        static SessionContext lastContext;

        @Override
        public SearchIndex createInterface(SessionContext context) {
            lastContext = context;
            if (failure != null) {
                throw failure;
            }
            return store;
        }
    }

    public static void installCollaborationStore(CollaborationStore store) {
        forceRegistrySeeding();
        StubCollaborationStoreFactory.store = store;
        StubCollaborationStoreFactory.failure = null;
        StubCollaborationStoreFactory.lastContext = null;
        AbstractFactoryBase.registerFactory(
                CollaborationStoreFactory.class, StubCollaborationStoreFactory.class);
    }

    public static void failCollaborationStoreResolution(RuntimeException failure) {
        installCollaborationStore(null);
        StubCollaborationStoreFactory.failure = failure;
    }

    public static SessionContext collaborationStoreResolutionContext() {
        return StubCollaborationStoreFactory.lastContext;
    }

    public static void restoreCollaborationStore() {
        StubCollaborationStoreFactory.store = null;
        StubCollaborationStoreFactory.failure = null;
        StubCollaborationStoreFactory.lastContext = null;
        AbstractFactoryBase.registerFactory(
                CollaborationStoreFactory.class, CollaborationStoreFactoryImpl.class);
    }

    public static void installStateStore(StateStore store) {
        forceRegistrySeeding();
        StubStateStoreFactory.store = store;
        StubStateStoreFactory.failure = null;
        StubStateStoreFactory.lastContext = null;
        AbstractFactoryBase.registerFactory(StateStoreFactory.class, StubStateStoreFactory.class);
    }

    public static void failStateStoreResolution(RuntimeException failure) {
        installStateStore(null);
        StubStateStoreFactory.failure = failure;
    }

    public static SessionContext stateStoreResolutionContext() {
        return StubStateStoreFactory.lastContext;
    }

    public static void restoreStateStore() {
        StubStateStoreFactory.store = null;
        StubStateStoreFactory.failure = null;
        StubStateStoreFactory.lastContext = null;
        AbstractFactoryBase.registerFactory(StateStoreFactory.class, StateStoreFactoryImpl.class);
    }

    public static void installSearchIndex(SearchIndex searchIndex) {
        forceRegistrySeeding();
        StubSearchIndexFactory.store = searchIndex;
        StubSearchIndexFactory.failure = null;
        StubSearchIndexFactory.lastContext = null;
        AbstractFactoryBase.registerFactory(SearchIndexFactory.class, StubSearchIndexFactory.class);
    }

    public static void failSearchIndexResolution(RuntimeException failure) {
        installSearchIndex(null);
        StubSearchIndexFactory.failure = failure;
    }

    public static SessionContext searchIndexResolutionContext() {
        return StubSearchIndexFactory.lastContext;
    }

    public static void restoreSearchIndex() {
        StubSearchIndexFactory.store = null;
        StubSearchIndexFactory.failure = null;
        StubSearchIndexFactory.lastContext = null;
        AbstractFactoryBase.registerFactory(SearchIndexFactory.class, SearchIndexFactoryImpl.class);
    }

    /**
     * {@code AbstractComponentFactory}'s static initialiser reloads every factoryConfiguration.json
     * into the same registry {@code registerFactory} writes to, and it is one-shot. A class literal
     * does not trigger it, so it has to be forced to run before a stub is registered — otherwise it
     * runs on the first {@code getInstance()} and overwrites the stub with the production mapping.
     */
    private static void forceRegistrySeeding() {
        CollaborationStoreFactory.getInstance();
        StateStoreFactory.getInstance();
        SearchIndexFactory.getInstance();
    }

    /**
     * {@link ReturnCode} has no getter for its {@code ErrorCode}, so the module/code pair is only
     * observable through the first line of {@code toString()}: {@code <MODULE>-<code>[-<message>]}.
     */
    public static void assertReturnCode(ReturnCode actual, Module module, int errorCode,
                                        String message) {
        Assert.assertNotNull(actual, "expected a return code");
        String rendered = actual.toString();
        int lineEnd = rendered.indexOf(System.lineSeparator());
        Assert.assertEquals(lineEnd < 0 ? rendered : rendered.substring(0, lineEnd),
                module.name() + "-" + errorCode + (message == null ? "" : "-" + message));
        Assert.assertEquals(actual.getMessage(), message);
    }

    public static Response<Void> failedResponse(ReturnCode returnCode) {
        return new Response<Void>(returnCode);
    }

    public static <T> Response<T> failedResponseOf(ReturnCode returnCode) {
        return new Response<T>(returnCode);
    }

    public static Response<Void> emptySuccess() {
        return new Response<Void>((Void) null);
    }

    public static <T> Response<T> successfulResponse(T value) {
        return new Response<T>(value);
    }

    public static ReturnCode pluginReturnCode(String message) {
        return new ReturnCode(12345, Module.ZDB, message, null);
    }

    public static Info info(String name) {
        Info info = new Info();
        info.setName(name);
        info.setDescription(name + "-description");
        info.addProperty(name + "-property", name + "-value");
        return info;
    }

    public static Namespace namespace(String value) {
        Namespace namespace = new Namespace();
        namespace.setValue(value);
        return namespace;
    }

    public static Relation relation(String type) {
        Relation relation = new Relation();
        relation.setType(type);
        return relation;
    }

    public static CoreElement coreElement(String id) {
        CoreElement element = new CoreElement();
        element.setId(new Id(id));
        element.setParentId(new Id(id + "-parent"));
        element.setNamespace(namespace(id + "-namespace"));
        element.setInfo(info(id + "-info"));
        element.setRelations(Collections.singletonList(relation(id + "-relation")));
        element.setData(stream(id + "-data"));
        element.setSearchableData(stream(id + "-searchable"));
        element.setVisualization(stream(id + "-visualization"));
        return element;
    }

    public static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    public static String text(InputStream input) {
        if (input == null) {
            return null;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        try {
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new IllegalStateException("could not read stream content", e);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
