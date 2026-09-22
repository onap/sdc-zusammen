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

package com.amdocs.zusammen.adaptor.outbound.impl.item;

import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.assertReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.coreElement;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.emptySuccess;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.failedResponse;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.info;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.namespace;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.pluginReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.relation;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.successfulResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

import com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementInfo;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.sdk.state.StateStore;
import com.amdocs.zusammen.sdk.state.types.StateElement;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ElementStateAdaptorImplTest {

    private static final SessionContext CONTEXT = new SessionContext();
    private static final Id ITEM_ID = new Id("item-id");
    private static final Id VERSION_ID = new Id("version-id");
    private static final Id ELEMENT_ID = new Id("element-id");
    private static final ElementContext ELEMENT_CONTEXT = new ElementContext(ITEM_ID, VERSION_ID);

    @Mock
    private StateStore stateStore;

    private AutoCloseable mocks;
    private ElementStateAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        OutboundTestSupport.installStateStore(stateStore);
        adaptor = new ElementStateAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OutboundTestSupport.restoreStateStore();
        mocks.close();
    }

    @Test
    public void testStateStoreIsResolvedWithTheCallersSessionContext() {
        Mockito.when(stateStore.isElementExist(any(), any(), any()))
                .thenReturn(successfulResponse(Boolean.TRUE));

        adaptor.isExist(CONTEXT, ELEMENT_CONTEXT, ELEMENT_ID);

        Assert.assertSame(OutboundTestSupport.stateStoreResolutionContext(), CONTEXT);
    }

    @Test(dataProvider = "passThroughMethods")
    public void testStateStoreResponseIsReturnedUnchangedOnSuccess(Wrapped method) {
        Response<Void> storeResponse = emptySuccess();
        Mockito.when(method.storeCall.call(stateStore)).thenReturn(storeResponse);

        Assert.assertSame(method.adaptorCall.call(adaptor), storeResponse,
                method.label + " must hand the plugin response back untouched");
    }

    @Test(dataProvider = "methods")
    public void testUnsuccessfulStateStoreResponseIsNestedInMiddlewareReturnCode(Wrapped method) {
        ReturnCode storeReturnCode = pluginReturnCode("state store refused");
        Mockito.when(method.storeCall.call(stateStore)).thenReturn(failedResponse(storeReturnCode));

        Response<?> response = (Response<?>) method.adaptorCall.call(adaptor);

        Assert.assertFalse(response.isSuccessful(), method.label + " must report the failure");
        assertReturnCode(response.getReturnCode(), Module.ZSTM, method.middlewareCode, null);
        Assert.assertSame(response.getReturnCode().getReturnCode(), storeReturnCode,
                method.label + " must keep the plugin's own return code as the cause");
    }

    @Test(dataProvider = "methods")
    public void testStateStoreExceptionIsMappedToAMetadataPluginReturnCode(Wrapped method) {
        Mockito.when(method.storeCall.call(stateStore))
                .thenThrow(new IllegalStateException("state store unreachable"));

        Response<?> response = (Response<?>) method.adaptorCall.call(adaptor);

        Assert.assertFalse(response.isSuccessful(), method.label + " must report the failure");
        assertReturnCode(response.getReturnCode(), Module.ZSTM, method.middlewareCode, null);
        assertReturnCode(response.getReturnCode().getReturnCode(), Module.ZMDP, method.pluginCode,
                "state store unreachable");
        Assert.assertNull(response.getReturnCode().getReturnCode().getReturnCode());
    }

    @Test
    public void testListForwardsTheElementContextAndConvertsEveryStateElement() {
        Mockito.when(stateStore.listElements(any(), any(), any()))
                .thenReturn(successfulResponse(Arrays.asList(stateElement("first"),
                        stateElement("second"))));

        Response<Collection<CoreElementInfo>> response =
                adaptor.list(CONTEXT, ELEMENT_CONTEXT, ELEMENT_ID);

        Mockito.verify(stateStore).listElements(same(CONTEXT), eq(ELEMENT_CONTEXT), eq(ELEMENT_ID));
        Assert.assertEquals(idsOf(response.getValue()), Arrays.asList("first", "second"));
        CoreElementInfo first = response.getValue().iterator().next();
        Assert.assertEquals(first.getNamespace(), namespace("first-namespace"));
        Assert.assertEquals(first.getParentId(), new Id("first-parent"));
        Assert.assertEquals(first.getInfo().getName(), "first-info");
        Assert.assertEquals(first.getRelations().size(), 1);
        Assert.assertEquals(idsOf(first.getSubElements()),
                Collections.singletonList("first-child"));
    }

    @Test
    public void testListReturnsAnEmptyCollectionWhenTheStoreHasNoElements() {
        Mockito.when(stateStore.listElements(any(), any(), any()))
                .thenReturn(successfulResponse(Collections.<StateElement>emptyList()));

        Response<Collection<CoreElementInfo>> response =
                adaptor.list(CONTEXT, ELEMENT_CONTEXT, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertTrue(response.getValue().isEmpty());
    }

    @Test
    public void testIsExistHandsBackThePluginsAnswer() {
        Mockito.when(stateStore.isElementExist(any(), any(), any()))
                .thenReturn(successfulResponse(Boolean.FALSE));

        Response<Boolean> response = adaptor.isExist(CONTEXT, ELEMENT_CONTEXT, ELEMENT_ID);

        Mockito.verify(stateStore).isElementExist(same(CONTEXT), eq(ELEMENT_CONTEXT),
                eq(ELEMENT_ID));
        Assert.assertEquals(response.getValue(), Boolean.FALSE);
    }

    @Test
    public void testGetNamespaceIsResolvedFromTheItemIdRatherThanAnElementContext() {
        Namespace namespace = namespace("parent/child");
        Response<Namespace> storeResponse = successfulResponse(namespace);
        Mockito.when(stateStore.getElementNamespace(any(), any(), any())).thenReturn(storeResponse);

        Response<Namespace> response = adaptor.getNamespace(CONTEXT, ITEM_ID, ELEMENT_ID);

        Mockito.verify(stateStore).getElementNamespace(same(CONTEXT), eq(ITEM_ID), eq(ELEMENT_ID));
        Assert.assertSame(response, storeResponse);
    }

    @Test
    public void testGetConvertsTheStateElementToCoreElementInfo() {
        Mockito.when(stateStore.getElement(any(), any(), any()))
                .thenReturn(successfulResponse(stateElement("fetched")));

        Response<CoreElementInfo> response = adaptor.get(CONTEXT, ELEMENT_CONTEXT, ELEMENT_ID);

        Mockito.verify(stateStore).getElement(same(CONTEXT), eq(ELEMENT_CONTEXT), eq(ELEMENT_ID));
        CoreElementInfo elementInfo = response.getValue();
        Assert.assertEquals(elementInfo.getId(), new Id("fetched"));
        Assert.assertEquals(elementInfo.getNamespace(), namespace("fetched-namespace"));
        Assert.assertEquals(elementInfo.getParentId(), new Id("fetched-parent"));
        Assert.assertEquals(elementInfo.getInfo().getName(), "fetched-info");
    }

    @Test(dataProvider = "elementOperationsAndSpaces")
    public void testElementStateIsRecordedInTheRequestedSpace(Operation operation, Space space) {
        Mockito.when(operation.storeCall.call(stateStore, () -> any())).thenReturn(emptySuccess());

        operation.adaptorCall.call(adaptor, space);

        ArgumentCaptor<StateElement> captor = ArgumentCaptor.forClass(StateElement.class);
        operation.storeCall.call(Mockito.verify(stateStore), () -> captor.capture());
        Assert.assertEquals(captor.getValue().getSpace(), space,
                operation.label + " must record state in the space it was given");
    }

    @Test(dataProvider = "elementOperations")
    public void testStateElementIsConvertedFromTheCoreElementAndTheElementContext(
            Operation operation) {
        Mockito.when(operation.storeCall.call(stateStore, () -> any())).thenReturn(emptySuccess());

        operation.adaptorCall.call(adaptor, Space.PRIVATE);

        ArgumentCaptor<StateElement> captor = ArgumentCaptor.forClass(StateElement.class);
        operation.storeCall.call(Mockito.verify(stateStore), () -> captor.capture());
        StateElement forwarded = captor.getValue();
        Assert.assertEquals(forwarded.getItemId(), ITEM_ID);
        Assert.assertEquals(forwarded.getVersionId(), VERSION_ID);
        Assert.assertEquals(forwarded.getId(), new Id("stored-element"));
        Assert.assertEquals(forwarded.getParentId(), new Id("stored-element-parent"));
        Assert.assertEquals(forwarded.getNamespace(), namespace("stored-element-namespace"));
        Assert.assertEquals(forwarded.getInfo().getName(), "stored-element-info");
        Assert.assertEquals(forwarded.getRelations().size(), 1);
    }

    @Test
    public void testUpdateReportsAConversionFailureAsAnUpdateReturnCode() {
        Response<Void> response =
                adaptor.update(CONTEXT, ELEMENT_CONTEXT, Space.PRIVATE, unconvertibleElement());

        Assert.assertFalse(response.isSuccessful());
        assertReturnCode(response.getReturnCode(), Module.ZSTM, ErrorCode.MD_ELEMENT_UPDATE, null);
        assertReturnCode(response.getReturnCode().getReturnCode(), Module.ZMDP,
                ErrorCode.ST_ELEMENT_UPDATE, "unreadable element");
        Mockito.verifyNoInteractions(stateStore);
    }

    @DataProvider(name = "methods")
    public static Object[][] methods() {
        return rows(false);
    }

    @DataProvider(name = "passThroughMethods")
    public static Object[][] passThroughMethods() {
        return rows(true);
    }

    @DataProvider(name = "elementOperations")
    public static Object[][] elementOperations() {
        List<Object[]> rows = new ArrayList<>();
        for (Operation operation : operations()) {
            rows.add(new Object[] {operation});
        }
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "elementOperationsAndSpaces")
    public static Object[][] elementOperationsAndSpaces() {
        List<Object[]> rows = new ArrayList<>();
        for (Operation operation : operations()) {
            for (Space space : Space.values()) {
                rows.add(new Object[] {operation, space});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    private static Object[][] rows(boolean onlyPassThrough) {
        List<Object[]> rows = new ArrayList<>();
        for (Wrapped method : wrappedMethods()) {
            if (!onlyPassThrough || method.passesTheStoreResponseThrough) {
                rows.add(new Object[] {method});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    /**
     * The error-mapping table of {@link ElementStateAdaptorImpl}: for every method, which middleware
     * ({@link Module#ZSTM}) and plugin ({@link Module#ZMDP}) code the failure paths must produce.
     */
    private static List<Wrapped> wrappedMethods() {
        return Arrays.asList(
                new Wrapped("list", s -> s.listElements(any(), any(), any()),
                        a -> a.list(CONTEXT, ELEMENT_CONTEXT, ELEMENT_ID),
                        ErrorCode.MD_ELEMENT_LIST, ErrorCode.ST_ELEMENT_LIST, false),
                new Wrapped("isExist", s -> s.isElementExist(any(), any(), any()),
                        a -> a.isExist(CONTEXT, ELEMENT_CONTEXT, ELEMENT_ID),
                        ErrorCode.MD_ELEMENT_IS_EXIST, ErrorCode.ST_ELEMENT_IS_EXIST, true),
                new Wrapped("getNamespace", s -> s.getElementNamespace(any(), any(), any()),
                        a -> a.getNamespace(CONTEXT, ITEM_ID, ELEMENT_ID),
                        ErrorCode.MD_ELEMENT_GET, ErrorCode.ST_ELEMENT_GET, true),
                new Wrapped("get", s -> s.getElement(any(), any(), any()),
                        a -> a.get(CONTEXT, ELEMENT_CONTEXT, ELEMENT_ID),
                        ErrorCode.MD_ELEMENT_GET, ErrorCode.ST_ELEMENT_GET, false),
                new Wrapped("create", s -> s.createElement(any(), any()),
                        a -> a.create(CONTEXT, ELEMENT_CONTEXT, Space.PRIVATE,
                                coreElement("stored-element")),
                        ErrorCode.MD_ELEMENT_CREATE, ErrorCode.ST_ELEMENT_CREATE, true),
                new Wrapped("update", s -> s.updateElement(any(), any()),
                        a -> a.update(CONTEXT, ELEMENT_CONTEXT, Space.PRIVATE,
                                coreElement("stored-element")),
                        ErrorCode.MD_ELEMENT_UPDATE, ErrorCode.ST_ELEMENT_UPDATE, true),
                new Wrapped("delete", s -> s.deleteElement(any(), any()),
                        a -> a.delete(CONTEXT, ELEMENT_CONTEXT, Space.PRIVATE,
                                coreElement("stored-element")),
                        ErrorCode.MD_ELEMENT_DELETE, ErrorCode.ST_ELEMENT_DELETE, true));
    }

    private static List<Operation> operations() {
        return Arrays.asList(
                new Operation("create",
                        (store, element) -> store.createElement(same(CONTEXT), element.get()),
                        (adaptor, space) -> adaptor.create(CONTEXT, ELEMENT_CONTEXT, space,
                                coreElement("stored-element"))),
                new Operation("update",
                        (store, element) -> store.updateElement(same(CONTEXT), element.get()),
                        (adaptor, space) -> adaptor.update(CONTEXT, ELEMENT_CONTEXT, space,
                                coreElement("stored-element"))),
                new Operation("delete",
                        (store, element) -> store.deleteElement(same(CONTEXT), element.get()),
                        (adaptor, space) -> adaptor.delete(CONTEXT, ELEMENT_CONTEXT, space,
                                coreElement("stored-element"))));
    }

    private static CoreElement unconvertibleElement() {
        CoreElement element = Mockito.mock(CoreElement.class);
        Mockito.when(element.getNamespace())
                .thenThrow(new IllegalStateException("unreadable element"));
        return element;
    }

    private static StateElement stateElement(String id) {
        StateElement element = new StateElement(ITEM_ID, VERSION_ID, namespace(id + "-namespace"),
                new Id(id));
        element.setParentId(new Id(id + "-parent"));
        element.setInfo(info(id + "-info"));
        element.setRelations(Collections.singletonList(relation(id + "-relation")));
        Set<Id> subElements = new HashSet<>();
        subElements.add(new Id(id + "-child"));
        element.setSubElements(subElements);
        return element;
    }

    private static List<String> idsOf(Collection<CoreElementInfo> elements) {
        List<String> ids = new ArrayList<>();
        for (CoreElementInfo element : elements) {
            ids.add(element.getId().getValue());
        }
        return ids;
    }

    private interface StoreCall {
        Object call(StateStore store);
    }

    private interface AdaptorCall {
        Object call(ElementStateAdaptorImpl adaptor);
    }

    /**
     * The element argument arrives as a supplier so the Mockito matcher it produces is registered
     * after the one for the session context — matchers are matched to arguments in registration
     * order, and a plain parameter would be evaluated before the lambda body runs.
     */
    private interface ElementStoreCall {
        Object call(StateStore store, Supplier<StateElement> element);
    }

    private interface ElementAdaptorCall {
        Object call(ElementStateAdaptorImpl adaptor, Space space);
    }

    private static final class Wrapped {
        private final String label;
        private final StoreCall storeCall;
        private final AdaptorCall adaptorCall;
        private final int middlewareCode;
        private final int pluginCode;
        private final boolean passesTheStoreResponseThrough;

        private Wrapped(String label, StoreCall storeCall, AdaptorCall adaptorCall,
                        int middlewareCode, int pluginCode,
                        boolean passesTheStoreResponseThrough) {
            this.label = label;
            this.storeCall = storeCall;
            this.adaptorCall = adaptorCall;
            this.middlewareCode = middlewareCode;
            this.pluginCode = pluginCode;
            this.passesTheStoreResponseThrough = passesTheStoreResponseThrough;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class Operation {
        private final String label;
        private final ElementStoreCall storeCall;
        private final ElementAdaptorCall adaptorCall;

        private Operation(String label, ElementStoreCall storeCall,
                          ElementAdaptorCall adaptorCall) {
            this.label = label;
            this.storeCall = storeCall;
            this.adaptorCall = adaptorCall;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
