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

package com.amdocs.zusammen.core.impl.item;

import com.amdocs.zusammen.adaptor.outbound.api.SearchIndexAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.item.ElementStateAdaptor;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.impl.TestUtils;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class IndexingElementCommandFactoryTest {

    private static final SessionContext CONTEXT = TestUtils.createSessionContext(
            new UserInfo("IndexingElementCommandFactoryTest_user"), "test");
    private static final Space SPACE = Space.PRIVATE;

    @Mock
    private ElementStateAdaptor stateAdaptorMock;
    @Mock
    private SearchIndexAdaptor searchIndexAdaptorMock;
    private AutoCloseable mocks;
    private ElementCommandAbstarctFactory factory;
    private ElementContext elementContext;
    private CoreElement element;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        OutboundAdaptorFactoryStubs.installElementStateAdaptor(stateAdaptorMock);
        OutboundAdaptorFactoryStubs.installSearchIndexAdaptor(searchIndexAdaptorMock);
        factory = IndexingElementCommandFactory.init();
        elementContext = new ElementContext(new Id(), new Id());
        element = new CoreElement();
        element.setId(new Id());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OutboundAdaptorFactoryStubs.uninstall();
        mocks.close();
    }

    @Test
    public void testCreateWritesStateThenIndex() {
        element.setAction(Action.CREATE);
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
                .create(CONTEXT, elementContext, SPACE, element);
        doReturn(new Response<>(Void.TYPE)).when(searchIndexAdaptorMock)
                .createElement(CONTEXT, elementContext, SPACE, element);

        execute();

        verify(stateAdaptorMock).create(CONTEXT, elementContext, SPACE, element);
        verify(searchIndexAdaptorMock).createElement(CONTEXT, elementContext, SPACE, element);
    }

    @Test
    public void testCreateSkipsIndexingWhenTheStateWriteFails() {
        element.setAction(Action.CREATE);
        ReturnCode cause = stateFailure();
        doReturn(new Response<Void>(cause)).when(stateAdaptorMock)
                .create(CONTEXT, elementContext, SPACE, element);

        TestUtils.assertWrappedFailure(TestUtils.captureFailure(this::execute),
                ErrorCode.ZU_ELEMENT_CREATE, cause);

        verifyNoInteractions(searchIndexAdaptorMock);
    }

    @Test
    public void testCreateFailsWhenIndexingFails() {
        element.setAction(Action.CREATE);
        ReturnCode cause = indexFailure();
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
                .create(CONTEXT, elementContext, SPACE, element);
        doReturn(new Response<Void>(cause)).when(searchIndexAdaptorMock)
                .createElement(CONTEXT, elementContext, SPACE, element);

        TestUtils.assertWrappedFailure(TestUtils.captureFailure(this::execute),
                ErrorCode.ZU_ELEMENT_CREATE, cause);
    }

    @Test
    public void testUpdateWritesStateThenIndex() {
        element.setAction(Action.UPDATE);
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
                .update(CONTEXT, elementContext, SPACE, element);
        doReturn(new Response<>(Void.TYPE)).when(searchIndexAdaptorMock)
                .updateElement(CONTEXT, elementContext, SPACE, element);

        execute();

        verify(stateAdaptorMock).update(CONTEXT, elementContext, SPACE, element);
        verify(searchIndexAdaptorMock).updateElement(CONTEXT, elementContext, SPACE, element);
    }

    @Test
    public void testUpdateSkipsIndexingWhenTheStateWriteFails() {
        element.setAction(Action.UPDATE);
        ReturnCode cause = stateFailure();
        doReturn(new Response<Void>(cause)).when(stateAdaptorMock)
                .update(CONTEXT, elementContext, SPACE, element);

        TestUtils.assertWrappedFailure(TestUtils.captureFailure(this::execute),
                ErrorCode.ZU_ELEMENT_UPDATE, cause);

        verifyNoInteractions(searchIndexAdaptorMock);
    }

    @Test
    public void testUpdateFailsWhenIndexingFails() {
        element.setAction(Action.UPDATE);
        ReturnCode cause = indexFailure();
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
                .update(CONTEXT, elementContext, SPACE, element);
        doReturn(new Response<Void>(cause)).when(searchIndexAdaptorMock)
                .updateElement(CONTEXT, elementContext, SPACE, element);

        TestUtils.assertWrappedFailure(TestUtils.captureFailure(this::execute),
                ErrorCode.ZU_ELEMENT_UPDATE, cause);
    }

    @Test
    public void testDeleteWritesStateThenIndex() {
        element.setAction(Action.DELETE);
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
                .delete(CONTEXT, elementContext, SPACE, element);
        doReturn(new Response<>(Void.TYPE)).when(searchIndexAdaptorMock)
                .deleteElement(CONTEXT, elementContext, SPACE, element);

        execute();

        verify(stateAdaptorMock).delete(CONTEXT, elementContext, SPACE, element);
        verify(searchIndexAdaptorMock).deleteElement(CONTEXT, elementContext, SPACE, element);
    }

    @Test
    public void testDeleteSkipsIndexingWhenTheStateWriteFails() {
        element.setAction(Action.DELETE);
        ReturnCode cause = stateFailure();
        doReturn(new Response<Void>(cause)).when(stateAdaptorMock)
                .delete(CONTEXT, elementContext, SPACE, element);

        TestUtils.assertWrappedFailure(TestUtils.captureFailure(this::execute),
                ErrorCode.ZU_ELEMENT_DELETE, cause);

        verifyNoInteractions(searchIndexAdaptorMock);
    }

    @Test
    public void testDeleteFailsWhenIndexingFails() {
        element.setAction(Action.DELETE);
        ReturnCode cause = indexFailure();
        doReturn(new Response<>(Void.TYPE)).when(stateAdaptorMock)
                .delete(CONTEXT, elementContext, SPACE, element);
        doReturn(new Response<Void>(cause)).when(searchIndexAdaptorMock)
                .deleteElement(CONTEXT, elementContext, SPACE, element);

        TestUtils.assertWrappedFailure(TestUtils.captureFailure(this::execute),
                ErrorCode.ZU_ELEMENT_DELETE, cause);
    }

    private void execute() {
        factory.executeCommand(CONTEXT, elementContext, SPACE, element);
    }

    private ReturnCode stateFailure() {
        return new ReturnCode(ErrorCode.MD_ELEMENT_CREATE, Module.ZMDP, "state store down", null);
    }

    private ReturnCode indexFailure() {
        return new ReturnCode(ErrorCode.IN_ELEMENT_CREATE, Module.ZSIP, "index unavailable", null);
    }
}
