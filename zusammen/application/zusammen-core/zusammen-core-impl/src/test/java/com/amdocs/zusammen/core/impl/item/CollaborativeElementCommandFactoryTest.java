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

import com.amdocs.zusammen.adaptor.outbound.api.CollaborationAdaptor;
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
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class CollaborativeElementCommandFactoryTest {

    private static final SessionContext CONTEXT = TestUtils.createSessionContext(
            new UserInfo("CollaborativeElementCommandFactoryTest_user"), "test");

    @Mock
    private CollaborationAdaptor collaborationAdaptorMock;
    private AutoCloseable mocks;
    private ElementCommandAbstarctFactory factory;
    private ElementContext elementContext;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        OutboundAdaptorFactoryStubs.installCollaborationAdaptor(collaborationAdaptorMock);
        factory = CollaborativeElementCommandFactory.init();
        elementContext = new ElementContext(new Id(), new Id());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OutboundAdaptorFactoryStubs.uninstall();
        mocks.close();
    }

    @Test
    public void testCreateMintsAFreshElementId() {
        CoreElement element = createElement(Action.CREATE, new Id("id-from-the-caller"));
        doReturn(new Response<>(Void.TYPE))
                .when(collaborationAdaptorMock).createElement(CONTEXT, elementContext, element);

        factory.executeCommand(CONTEXT, elementContext, Space.PRIVATE, element);

        Assert.assertNotNull(element.getId());
        Assert.assertNotEquals(element.getId(), new Id("id-from-the-caller"));
        verify(collaborationAdaptorMock).createElement(CONTEXT, elementContext, element);
    }

    @Test
    public void testCreateFailurePropagatesAsElementCreateError() {
        CoreElement element = createElement(Action.CREATE, null);
        ReturnCode cause = pluginFailure();
        doReturn(new Response<Void>(cause))
                .when(collaborationAdaptorMock).createElement(CONTEXT, elementContext, element);

        ReturnCode returnCode = TestUtils.captureFailure(
                () -> factory.executeCommand(CONTEXT, elementContext, Space.PRIVATE, element));

        TestUtils.assertWrappedFailure(returnCode, ErrorCode.ZU_ELEMENT_CREATE, cause);
    }

    @Test
    public void testUpdateDelegatesToTheCollaborationAdaptor() {
        CoreElement element = createElement(Action.UPDATE, new Id());
        doReturn(new Response<>(Void.TYPE))
                .when(collaborationAdaptorMock).updateElement(CONTEXT, elementContext, element);

        factory.executeCommand(CONTEXT, elementContext, Space.PRIVATE, element);

        verify(collaborationAdaptorMock).updateElement(CONTEXT, elementContext, element);
    }

    @Test
    public void testUpdateFailurePropagatesAsElementUpdateError() {
        CoreElement element = createElement(Action.UPDATE, new Id());
        ReturnCode cause = pluginFailure();
        doReturn(new Response<Void>(cause))
                .when(collaborationAdaptorMock).updateElement(CONTEXT, elementContext, element);

        ReturnCode returnCode = TestUtils.captureFailure(
                () -> factory.executeCommand(CONTEXT, elementContext, Space.PRIVATE, element));

        TestUtils.assertWrappedFailure(returnCode, ErrorCode.ZU_ELEMENT_UPDATE, cause);
    }

    @Test
    public void testDeleteDelegatesToTheCollaborationAdaptor() {
        CoreElement element = createElement(Action.DELETE, new Id());
        doReturn(new Response<>(Void.TYPE))
                .when(collaborationAdaptorMock).deleteElement(CONTEXT, elementContext, element);

        factory.executeCommand(CONTEXT, elementContext, Space.PRIVATE, element);

        verify(collaborationAdaptorMock).deleteElement(CONTEXT, elementContext, element);
    }

    @Test
    public void testDeleteFailurePropagatesAsElementDeleteError() {
        CoreElement element = createElement(Action.DELETE, new Id());
        ReturnCode cause = pluginFailure();
        doReturn(new Response<Void>(cause))
                .when(collaborationAdaptorMock).deleteElement(CONTEXT, elementContext, element);

        ReturnCode returnCode = TestUtils.captureFailure(
                () -> factory.executeCommand(CONTEXT, elementContext, Space.PRIVATE, element));

        TestUtils.assertWrappedFailure(returnCode, ErrorCode.ZU_ELEMENT_DELETE, cause);
    }

    @Test
    public void testIgnoreActionHasNoRegisteredCommand() {
        Id elementId = new Id();
        CoreElement element = createElement(Action.IGNORE, elementId);

        factory.executeCommand(CONTEXT, elementContext, Space.PRIVATE, element);

        Assert.assertEquals(element.getId(), elementId);
        verifyNoInteractions(collaborationAdaptorMock);
    }

    private ReturnCode pluginFailure() {
        return new ReturnCode(ErrorCode.CL_ELEMENT_UPDATE, Module.ZCSP, "collaboration store down",
                null);
    }

    private CoreElement createElement(Action action, Id elementId) {
        CoreElement element = new CoreElement();
        element.setId(elementId);
        element.setAction(action);
        element.setInfo(TestUtils.createInfo("element_" + action));
        return element;
    }
}
