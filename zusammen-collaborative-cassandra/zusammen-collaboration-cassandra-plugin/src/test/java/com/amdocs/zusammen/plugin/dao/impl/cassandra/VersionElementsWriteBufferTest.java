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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.plugin.collaboration.PublishService;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.main.CassandraCollaborationStorePluginImpl;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationPublishResult;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class VersionElementsWriteBufferTest {

    private static final String TENANT = "VersionElementsWriteBufferTest_tenant";
    private static final String USER = "VersionElementsWriteBufferTest_user";
    private static final String SPACE = "VersionElementsWriteBufferTest_space";
    private static final String OTHER_SPACE = "VersionElementsWriteBufferTest_other_space";
    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id REVISION_ID = new Id("revision-3");
    private static final Id ELEMENT_HASH = new Id("hash-9");
    private static final Date PUBLISH_TIME = new Date(1_700_000_000_000L);

    @Mock
    private ElementRepositoryImpl.ElementAccessor elementAccessor;
    @Mock
    private ElementRepositoryImpl.VersionElementsAccessor elementIdsAccessor;
    @Mock
    private ElementSynchronizationStateRepositoryImpl.ElementSynchronizationStateAccessor syncStateAccessor;
    @Mock
    private ElementSynchronizationStateRepositoryImpl.VersionElementsAccessor dirtyElementIdsAccessor;

    private AutoCloseable mocks;
    private SessionContext context;
    private ElementRepositoryImpl elementRepository;
    private ElementSynchronizationStateRepositoryImpl syncStateRepository;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        CassandraAccessorSeam.install();
        CassandraAccessorSeam.registerAccessor(ElementRepositoryImpl.ElementAccessor.class, elementAccessor);
        CassandraAccessorSeam.registerAccessor(ElementRepositoryImpl.VersionElementsAccessor.class, elementIdsAccessor);
        CassandraAccessorSeam
                .registerAccessor(ElementSynchronizationStateRepositoryImpl.ElementSynchronizationStateAccessor.class,
                        syncStateAccessor);
        CassandraAccessorSeam
                .registerAccessor(ElementSynchronizationStateRepositoryImpl.VersionElementsAccessor.class,
                        dirtyElementIdsAccessor);

        context = new SessionContext();
        context.setUser(new UserInfo(USER));
        context.setTenant(TENANT);
        elementRepository = new ElementRepositoryImpl();
        syncStateRepository = new ElementSynchronizationStateRepositoryImpl();
        VersionElementsWriteBuffer.open();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        VersionElementsWriteBuffer.discard();
        CassandraAccessorSeam.uninstall();
        mocks.close();
    }

    @Test
    public void testElementIdsRegisteredWhileBufferingAreSentAsOneStatement() {
        elementRepository.create(context, elementContext(SPACE), element("element-4"));
        elementRepository.create(context, elementContext(SPACE), element("element-5"));
        elementRepository.create(context, elementContext(SPACE), element("element-6"));

        verify(elementIdsAccessor, never()).addElements(Mockito.anyMap(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());

        VersionElementsWriteBuffer.flush(context);

        Map<String, String> expected = new HashMap<>();
        expected.put("element-4", "revision-3");
        expected.put("element-5", "revision-3");
        expected.put("element-6", "revision-3");
        verify(elementIdsAccessor, times(1)).addElements(expected, SPACE, "item-1", "version-2", "revision-3");
    }

    @Test
    public void testDirtyMarkersClearedWhileBufferingAreSentAsOneStatement() {
        syncStateRepository.update(context, elementContext(SPACE), publishedState("element-4"));
        syncStateRepository.update(context, elementContext(SPACE), publishedState("element-5"));

        verify(dirtyElementIdsAccessor, never()).removeDirtyElements(Mockito.anySet(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        VersionElementsWriteBuffer.flush(context);

        verify(dirtyElementIdsAccessor, times(1))
                .removeDirtyElements(new HashSet<>(Arrays.asList("element-4", "element-5")), SPACE, "item-1",
                        "version-2", "revision-3");
        verify(syncStateAccessor, times(2))
                .update(Mockito.any(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testAnElementDeletedAfterBeingRegisteredEndsUpRemoved() {
        ElementEntity element = element("element-4");
        elementRepository.create(context, elementContext(SPACE), element);
        elementRepository.delete(context, elementContext(SPACE), element);

        VersionElementsWriteBuffer.flush(context);

        verify(elementIdsAccessor).removeElements(Collections.singleton("element-4"), SPACE, "item-1", "version-2",
                "revision-3");
        verify(elementIdsAccessor, never()).addElements(Mockito.anyMap(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testAnElementRegisteredAfterBeingDeletedEndsUpRegistered() {
        ElementEntity element = element("element-4");
        elementRepository.delete(context, elementContext(SPACE), element);
        elementRepository.create(context, elementContext(SPACE), element);

        VersionElementsWriteBuffer.flush(context);

        verify(elementIdsAccessor).addElements(Collections.singletonMap("element-4", "revision-3"), SPACE, "item-1",
                "version-2", "revision-3");
        verify(elementIdsAccessor, never()).removeElements(Mockito.anySet(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testEachRowIsFlushedOnItsOwn() {
        elementRepository.create(context, elementContext(SPACE), element("element-4"));
        elementRepository.create(context, elementContext(OTHER_SPACE), element("element-5"));

        VersionElementsWriteBuffer.flush(context);

        verify(elementIdsAccessor).addElements(Collections.singletonMap("element-4", "revision-3"), SPACE, "item-1",
                "version-2", "revision-3");
        verify(elementIdsAccessor).addElements(Collections.singletonMap("element-5", "revision-3"), OTHER_SPACE,
                "item-1", "version-2", "revision-3");
    }

    @Test
    public void testDiscardedUpdatesAreNeverSent() {
        elementRepository.create(context, elementContext(SPACE), element("element-4"));

        VersionElementsWriteBuffer.discard();
        VersionElementsWriteBuffer.flush(context);

        verify(elementIdsAccessor, never()).addElements(Mockito.anyMap(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testFlushingTwiceSendsTheUpdatesOnce() {
        elementRepository.create(context, elementContext(SPACE), element("element-4"));

        VersionElementsWriteBuffer.flush(context);
        VersionElementsWriteBuffer.flush(context);

        verify(elementIdsAccessor, times(1)).addElements(Collections.singletonMap("element-4", "revision-3"), SPACE,
                "item-1", "version-2", "revision-3");
    }

    @Test
    public void testWithoutABufferTheUpdateIsSentStraightAway() {
        VersionElementsWriteBuffer.discard();

        elementRepository.create(context, elementContext(SPACE), element("element-4"));

        verify(elementIdsAccessor).addElements(Collections.singletonMap("element-4", "revision-3"), SPACE, "item-1",
                "version-2", "revision-3");
    }

    /**
     * The buffer is only correct if something actually flushes it, and the entry point is the only
     * place that does. A publish that buffered and never flushed would drop every registration with
     * the rest of the suite still green.
     */
    @Test
    public void testPublishItemVersionFlushesWhatThePublishBuffered() throws Exception {
        CassandraCollaborationStorePluginImpl plugin = pluginPublishing(invocation -> {
            elementRepository.create(context, elementContext(SPACE), element("element-4"));
            return new CollaborationPublishResult();
        });

        plugin.publishItemVersion(context, ITEM_ID, VERSION_ID, "publish message");

        verify(elementIdsAccessor).addElements(Collections.singletonMap("element-4", "revision-3"), SPACE, "item-1",
                "version-2", "revision-3");
    }

    @Test
    public void testPublishItemVersionSendsNothingWhenThePublishFails() throws Exception {
        CassandraCollaborationStorePluginImpl plugin = pluginPublishing(invocation -> {
            elementRepository.create(context, elementContext(SPACE), element("element-4"));
            throw new ZusammenException(new ReturnCode(4242, Module.ZCSP, "publish failed", null));
        });

        plugin.publishItemVersion(context, ITEM_ID, VERSION_ID, "publish message");

        verify(elementIdsAccessor, never()).addElements(Mockito.anyMap(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());
    }

    private CassandraCollaborationStorePluginImpl pluginPublishing(Answer<CollaborationPublishResult> publish)
            throws Exception {
        PublishService publishService = Mockito.mock(PublishService.class);
        Mockito.when(publishService.publish(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenAnswer(publish);

        CassandraCollaborationStorePluginImpl plugin = new CassandraCollaborationStorePluginImpl();
        Field field = CassandraCollaborationStorePluginImpl.class.getDeclaredField("publishService");
        field.setAccessible(true);
        field.set(plugin, publishService);
        return plugin;
    }

    private static ElementEntityContext elementContext(String space) {
        return new ElementEntityContext(space, ITEM_ID, VERSION_ID, REVISION_ID);
    }

    private static ElementEntity element(String elementId) {
        ElementEntity element = new ElementEntity(new Id(elementId));
        element.setElementHash(ELEMENT_HASH);
        return element;
    }

    private static SynchronizationStateEntity publishedState(String elementId) {
        return new SynchronizationStateEntity(new Id(elementId), REVISION_ID, PUBLISH_TIME, false);
    }
}
