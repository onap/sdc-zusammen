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

import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.assertReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.coreElement;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.emptySuccess;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.failedResponse;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.namespace;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.pluginReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.successfulResponse;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.text;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.datatypes.searchindex.SearchCriteria;
import com.amdocs.zusammen.datatypes.searchindex.SearchResult;
import com.amdocs.zusammen.sdk.searchindex.SearchIndex;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;
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
import java.util.List;
import java.util.function.Supplier;

public class SearchIndexAdaptorImplTest {

    private static final SessionContext CONTEXT = new SessionContext();
    private static final Id ITEM_ID = new Id("item-id");
    private static final Id VERSION_ID = new Id("version-id");
    private static final ElementContext ELEMENT_CONTEXT = new ElementContext(ITEM_ID, VERSION_ID);

    @Mock
    private SearchIndex searchIndex;

    private AutoCloseable mocks;
    private SearchIndexAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        OutboundTestSupport.installSearchIndex(searchIndex);
        adaptor = new SearchIndexAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OutboundTestSupport.restoreSearchIndex();
        mocks.close();
    }

    @Test
    public void testSearchIndexIsResolvedWithTheCallersSessionContext() {
        Mockito.when(searchIndex.search(any(), any()))
                .thenReturn(successfulResponse(Mockito.mock(SearchResult.class)));

        adaptor.search(CONTEXT, Mockito.mock(SearchCriteria.class));

        Assert.assertSame(OutboundTestSupport.searchIndexResolutionContext(), CONTEXT);
    }

    @Test(dataProvider = "elementOperationsAndSpaces")
    public void testElementIsIndexedInTheRequestedSpace(Operation operation, Space space) {
        Mockito.when(operation.storeCall.call(searchIndex, () -> any())).thenReturn(emptySuccess());

        operation.adaptorCall.call(adaptor, space);

        ArgumentCaptor<SearchIndexElement> captor =
                ArgumentCaptor.forClass(SearchIndexElement.class);
        operation.storeCall.call(Mockito.verify(searchIndex), () -> captor.capture());
        Assert.assertEquals(captor.getValue().getSpace(), space,
                operation.label + " must index into the space it was given");
    }

    @Test(dataProvider = "elementOperations")
    public void testElementIsConvertedFromTheCoreElementAndTheElementContext(Operation operation) {
        Mockito.when(operation.storeCall.call(searchIndex, () -> any())).thenReturn(emptySuccess());

        operation.adaptorCall.call(adaptor, Space.PRIVATE);

        ArgumentCaptor<SearchIndexElement> captor =
                ArgumentCaptor.forClass(SearchIndexElement.class);
        operation.storeCall.call(Mockito.verify(searchIndex), () -> captor.capture());
        SearchIndexElement forwarded = captor.getValue();
        Assert.assertEquals(forwarded.getItemId(), ITEM_ID);
        Assert.assertEquals(forwarded.getVersionId(), VERSION_ID);
        Assert.assertEquals(forwarded.getId(), new Id("indexed-element"));
        Assert.assertEquals(forwarded.getNamespace(), namespace("indexed-element-namespace"));
        Assert.assertEquals(text(forwarded.getSearchableData()), "indexed-element-searchable");
    }

    @Test(dataProvider = "elementOperations")
    public void testSearchIndexResponseIsReturnedUnchangedOnSuccess(Operation operation) {
        Response<Void> storeResponse = emptySuccess();
        Mockito.when(operation.storeCall.call(searchIndex, () -> any())).thenReturn(storeResponse);

        Assert.assertSame(operation.adaptorCall.call(adaptor, Space.PUBLIC), storeResponse);
    }

    @Test(dataProvider = "elementOperations")
    public void testStoreExceptionIsMappedToASearchIndexPluginReturnCode(Operation operation) {
        Mockito.when(operation.storeCall.call(searchIndex, () -> any()))
                .thenThrow(new IllegalStateException("index unreachable"));

        try {
            operation.adaptorCall.call(adaptor, Space.PRIVATE);
            Assert.fail(operation.label + " must raise ZusammenException when the index throws");
        } catch (ZusammenException e) {
            assertReturnCode(e.getReturnCode(), Module.ZSIM, operation.middlewareCode, null);
            assertReturnCode(e.getReturnCode().getReturnCode(), Module.ZSIP,
                    operation.pluginCode, "index unreachable");
            Assert.assertNull(e.getReturnCode().getReturnCode().getReturnCode());
        }
    }

    @Test(dataProvider = "elementOperations")
    public void testUnsuccessfulSearchIndexResponseIsRaisedAsAMiddlewareFailure(
            Operation operation) {
        Mockito.when(operation.storeCall.call(searchIndex, () -> any()))
                .thenReturn(failedResponse(pluginReturnCode("index refused")));

        try {
            operation.adaptorCall.call(adaptor, Space.PRIVATE);
            Assert.fail(operation.label + " must raise ZusammenException on a failed response");
        } catch (ZusammenException e) {
            assertReturnCode(e.getReturnCode(), Module.ZSIM, operation.middlewareCode, null);
            Assert.assertNotNull(e.getReturnCode().getReturnCode());
        }
    }

    @Test
    public void testSearchForwardsTheCriteriaAndReturnsThePluginResponse() {
        SearchCriteria criteria = Mockito.mock(SearchCriteria.class);
        Response<SearchResult> storeResponse =
                successfulResponse(Mockito.mock(SearchResult.class));
        Mockito.when(searchIndex.search(any(), any())).thenReturn(storeResponse);

        Response<SearchResult> response = adaptor.search(CONTEXT, criteria);

        Mockito.verify(searchIndex).search(same(CONTEXT), same(criteria));
        Assert.assertSame(response, storeResponse);
    }

    @Test
    public void testSearchExceptionIsMappedToTheSearchErrorCodes() {
        Mockito.when(searchIndex.search(any(), any()))
                .thenThrow(new IllegalStateException("query rejected"));

        try {
            adaptor.search(CONTEXT, Mockito.mock(SearchCriteria.class));
            Assert.fail("search must raise ZusammenException when the index throws");
        } catch (ZusammenException e) {
            assertReturnCode(e.getReturnCode(), Module.ZSIM, ErrorCode.MD_SEARCH, null);
            assertReturnCode(e.getReturnCode().getReturnCode(), Module.ZSIP, ErrorCode.IN_SEARCH,
                    "query rejected");
        }
    }

    @Test
    public void testUnsuccessfulSearchResponseIsRaisedAsASearchFailure() {
        Response<SearchResult> failed =
                OutboundTestSupport.failedResponseOf(pluginReturnCode("index refused"));
        Mockito.when(searchIndex.search(any(), any())).thenReturn(failed);

        try {
            adaptor.search(CONTEXT, Mockito.mock(SearchCriteria.class));
            Assert.fail("search must raise ZusammenException on a failed response");
        } catch (ZusammenException e) {
            assertReturnCode(e.getReturnCode(), Module.ZSIM, ErrorCode.MD_SEARCH, null);
            Assert.assertNotNull(e.getReturnCode().getReturnCode());
        }
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

    private static List<Operation> operations() {
        return Arrays.asList(
                new Operation("createElement",
                        (index, element) -> index.createElement(same(CONTEXT), element.get()),
                        (adaptor, space) -> adaptor.createElement(CONTEXT, ELEMENT_CONTEXT, space,
                                coreElement("indexed-element")),
                        ErrorCode.MD_ELEMENT_CREATE, ErrorCode.IN_ELEMENT_CREATE),
                new Operation("updateElement",
                        (index, element) -> index.updateElement(same(CONTEXT), element.get()),
                        (adaptor, space) -> adaptor.updateElement(CONTEXT, ELEMENT_CONTEXT, space,
                                coreElement("indexed-element")),
                        ErrorCode.MD_ELEMENT_UPDATE, ErrorCode.IN_ELEMENT_UPDATE),
                new Operation("deleteElement",
                        (index, element) -> index.deleteElement(same(CONTEXT), element.get()),
                        (adaptor, space) -> adaptor.deleteElement(CONTEXT, ELEMENT_CONTEXT, space,
                                coreElement("indexed-element")),
                        ErrorCode.MD_ELEMENT_DELETE, ErrorCode.IN_ELEMENT_DELETE));
    }

    /**
     * The element argument arrives as a supplier so the Mockito matcher it produces is registered
     * after the one for the session context — matchers are matched to arguments in registration
     * order, and a plain parameter would be evaluated before the lambda body runs.
     */
    private interface StoreCall {
        Object call(SearchIndex index, Supplier<SearchIndexElement> element);
    }

    private interface AdaptorCall {
        Object call(SearchIndexAdaptorImpl adaptor, Space space);
    }

    private static final class Operation {
        private final String label;
        private final StoreCall storeCall;
        private final AdaptorCall adaptorCall;
        private final int middlewareCode;
        private final int pluginCode;

        private Operation(String label, StoreCall storeCall, AdaptorCall adaptorCall,
                          int middlewareCode, int pluginCode) {
            this.label = label;
            this.storeCall = storeCall;
            this.adaptorCall = adaptorCall;
            this.middlewareCode = middlewareCode;
            this.pluginCode = pluginCode;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
