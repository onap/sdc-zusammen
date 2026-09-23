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

package com.amdocs.zusammen.adaptor.inbound.impl.item;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.adaptor.inbound.api.types.item.Element;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementConflict;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementInfo;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.ZusammenElement;
import com.amdocs.zusammen.core.api.item.ElementManager;
import com.amdocs.zusammen.core.api.item.ElementManagerFactory;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.core.api.types.CoreElementInfo;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.datatypes.item.Resolution;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.searchindex.SearchCriteria;
import com.amdocs.zusammen.datatypes.searchindex.SearchResult;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ElementAdaptorImplTest {

    private static final String PRODUCTION_FACTORY =
            "com.amdocs.zusammen.core.impl.item.ElementManagerFactoryImpl";
    private static final Id ELEMENT_ID = new Id("element-1");

    public static class StubElementManagerFactory extends ElementManagerFactory {

        static ElementManager manager;

        @Override
        public ElementManager createInterface(SessionContext context) {
            return manager;
        }
    }

    @Mock
    private ElementManager elementManager;

    private AutoCloseable mocks;
    private SessionContext context;
    private ElementContext elementContext;
    private ElementAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        // A class literal does not run a static initialiser. AbstractComponentFactory's one-shot load of
        // every factoryConfiguration.json has to happen before registerFactory, or the first factory
        // access reloads the production mapping over the stub.
        Class.forName(ElementManagerFactory.class.getName(), true,
                ElementManagerFactory.class.getClassLoader());
        StubElementManagerFactory.manager = elementManager;
        AbstractFactoryBase.registerFactory(ElementManagerFactory.class,
                StubElementManagerFactory.class);
        context = AdaptorTestSupport.sessionContext("element-adaptor-user");
        elementContext = new ElementContext(new Id("item-1"), new Id("version-1"));
        adaptor = new ElementAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        AdaptorTestSupport.restoreProductionFactory(ElementManagerFactory.class, PRODUCTION_FACTORY);
        StubElementManagerFactory.manager = null;
        mocks.close();
    }

    @Test
    public void testListConvertsEveryCoreElementInfo() {
        when(elementManager.list(context, elementContext, ELEMENT_ID)).thenReturn(
                Arrays.asList(coreElementInfo("child-1"), coreElementInfo("child-2")));

        Response<Collection<ElementInfo>> response =
                adaptor.list(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        List<ElementInfo> elementInfos = new ArrayList<>(response.getValue());
        Assert.assertEquals(elementInfos.size(), 2);
        Assert.assertEquals(elementInfos.get(0).getId().getValue(), "child-1");
        Assert.assertEquals(elementInfos.get(0).getInfo().getName(), "child-1-info");
        Assert.assertEquals(elementInfos.get(0).getRelations().size(), 1);
        Assert.assertEquals(elementInfos.get(0).getRelations().iterator().next().getType(),
                "child-1-relation");
        Assert.assertEquals(elementInfos.get(1).getId().getValue(), "child-2");
        verify(elementManager).list(context, elementContext, ELEMENT_ID);
    }

    @Test
    public void testListReturnsEmptyCollectionWhenVersionHasNoElements() {
        when(elementManager.list(context, elementContext, ELEMENT_ID))
                .thenReturn(Collections.<CoreElementInfo>emptyList());

        Response<Collection<ElementInfo>> response =
                adaptor.list(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertTrue(response.getValue().isEmpty());
    }

    @Test
    public void testListFailureIsMappedToElementListError() {
        when(elementManager.list(context, elementContext, ELEMENT_ID)).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.MD_ELEMENT_LIST, Module.ZSTM, "no elements"));

        Response<Collection<ElementInfo>> response =
                adaptor.list(context, elementContext, ELEMENT_ID);

        Assert.assertFalse(response.isSuccessful());
        Assert.assertNull(response.getValue());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ELEMENT_LIST);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ELEMENT_LIST);
    }

    @Test
    public void testGetInfoConvertsSubElementsRecursively() {
        CoreElementInfo coreElementInfo = coreElementInfo("element-1");
        coreElementInfo.setSubElements(Collections.singletonList(coreElementInfo("sub-element")));
        when(elementManager.getInfo(context, elementContext, ELEMENT_ID)).thenReturn(coreElementInfo);

        Response<ElementInfo> response = adaptor.getInfo(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getId().getValue(), "element-1");
        Assert.assertEquals(response.getValue().getSubElements().size(), 1);
        Assert.assertEquals(
                response.getValue().getSubElements().iterator().next().getId().getValue(),
                "sub-element");
    }

    @Test
    public void testGetInfoReturnsNullValueWhenElementIsUnknown() {
        when(elementManager.getInfo(context, elementContext, ELEMENT_ID)).thenReturn(null);

        Response<ElementInfo> response = adaptor.getInfo(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNull(response.getValue());
    }

    @Test
    public void testGetInfoFailureIsMappedToElementGetInfoError() {
        when(elementManager.getInfo(context, elementContext, ELEMENT_ID)).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.MD_ELEMENT_GET, Module.ZSTM, "not found"));

        Response<ElementInfo> response = adaptor.getInfo(context, elementContext, ELEMENT_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ELEMENT_GET_INFO);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ELEMENT_GET);
    }

    @Test
    public void testGetConvertsEveryCoreElementField() {
        CoreElement coreElement = coreElement("element-1");
        coreElement.setSubElements(Collections.singletonList(coreElement("sub-element")));
        when(elementManager.get(context, elementContext, ELEMENT_ID)).thenReturn(coreElement);

        Response<Element> response = adaptor.get(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Element element = response.getValue();
        Assert.assertEquals(element.getElementId().getValue(), "element-1");
        Assert.assertEquals(element.getAction(), Action.UPDATE);
        Assert.assertEquals(element.getInfo().getName(), "element-1-info");
        Assert.assertEquals(element.getRelations().size(), 1);
        Assert.assertEquals(AdaptorTestSupport.text(element.getData()), "element-1-data");
        Assert.assertEquals(AdaptorTestSupport.text(element.getSearchableData()),
                "element-1-searchable-data");
        Assert.assertEquals(AdaptorTestSupport.text(element.getVisualization()),
                "element-1-visualization");
        Assert.assertEquals(element.getSubElements().size(), 1);
        Assert.assertEquals(
                element.getSubElements().iterator().next().getElementId().getValue(),
                "sub-element");
    }

    @Test
    public void testGetConvertsElementWithoutSubElements() {
        CoreElement coreElement = coreElement("element-1");
        coreElement.setSubElements(Collections.<CoreElement>emptyList());
        when(elementManager.get(context, elementContext, ELEMENT_ID)).thenReturn(coreElement);

        Response<Element> response = adaptor.get(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertTrue(response.getValue().getSubElements().isEmpty());
    }

    @Test
    public void testGetFailureIsMappedToElementGetError() {
        when(elementManager.get(context, elementContext, ELEMENT_ID)).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.MD_ELEMENT_GET, Module.ZSTM, "not found"));

        Response<Element> response = adaptor.get(context, elementContext, ELEMENT_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ELEMENT_GET);
    }

    @Test
    public void testGetTreeConvertsEveryCoreElementField() {
        CoreElement coreElement = coreElement("element-1");
        coreElement.setSubElements(Collections.singletonList(coreElement("sub-element")));
        when(elementManager.getTree(context, elementContext, ELEMENT_ID, 1)).thenReturn(coreElement);

        Response<Element> response = adaptor.getTree(context, elementContext, ELEMENT_ID, 1);

        Assert.assertTrue(response.isSuccessful());
        Element element = response.getValue();
        Assert.assertEquals(element.getElementId().getValue(), "element-1");
        Assert.assertEquals(element.getSubElements().size(), 1);
        Assert.assertEquals(
                element.getSubElements().iterator().next().getElementId().getValue(),
                "sub-element");
    }

    @Test
    public void testGetTreeReturnsNullValueWhenElementIsUnknown() {
        when(elementManager.getTree(context, elementContext, ELEMENT_ID, 1)).thenReturn(null);

        Response<Element> response = adaptor.getTree(context, elementContext, ELEMENT_ID, 1);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNull(response.getValue());
    }

    @Test
    public void testGetTreeFailureIsMappedToElementGetError() {
        when(elementManager.getTree(context, elementContext, ELEMENT_ID, 1)).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.MD_ELEMENT_GET, Module.ZSTM, "not found"));

        Response<Element> response = adaptor.getTree(context, elementContext, ELEMENT_ID, 1);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ELEMENT_GET);
    }

    @Test
    public void testGetConflictConvertsLocalAndRemoteElement() {
        CoreElementConflict coreElementConflict = new CoreElementConflict();
        coreElementConflict.setLocalElement(coreElement("local-element"));
        coreElementConflict.setRemoteElement(coreElement("remote-element"));
        when(elementManager.getConflict(context, elementContext, ELEMENT_ID))
                .thenReturn(coreElementConflict);

        Response<ElementConflict> response =
                adaptor.getConflict(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getLocalElement().getElementId().getValue(),
                "local-element");
        Assert.assertEquals(
                AdaptorTestSupport.text(response.getValue().getLocalElement().getData()),
                "local-element-data");
        Assert.assertEquals(response.getValue().getRemoteElement().getElementId().getValue(),
                "remote-element");
    }

    @Test
    public void testGetConflictReturnsNullValueWhenElementIsNotInConflict() {
        when(elementManager.getConflict(context, elementContext, ELEMENT_ID)).thenReturn(null);

        Response<ElementConflict> response =
                adaptor.getConflict(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNull(response.getValue());
    }

    @Test
    public void testGetConflictFailureIsMappedToElementGetConflictError() {
        when(elementManager.getConflict(context, elementContext, ELEMENT_ID))
                .thenThrow(AdaptorTestSupport.failure(ErrorCode.MD_ELEMENT_GET_CONFLICT,
                        Module.ZSTM, "not merging"));

        Response<ElementConflict> response =
                adaptor.getConflict(context, elementContext, ELEMENT_ID);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ELEMENT_GET_CONFLICT);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ELEMENT_GET_CONFLICT);
    }

    @Test
    public void testSaveConvertsElementToCoreElementAndBack() {
        when(elementManager.save(eq(context), eq(elementContext), any(CoreElement.class),
                eq("saving"))).thenReturn(coreElement("saved-element"));

        Response<Element> response =
                adaptor.save(context, elementContext, element("element-1"), "saving");

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getElementId().getValue(), "saved-element");
        Assert.assertEquals(AdaptorTestSupport.text(response.getValue().getData()),
                "saved-element-data");

        ArgumentCaptor<CoreElement> captor = ArgumentCaptor.forClass(CoreElement.class);
        verify(elementManager)
                .save(eq(context), eq(elementContext), captor.capture(), eq("saving"));
        CoreElement sent = captor.getValue();
        Assert.assertEquals(sent.getId().getValue(), "element-1");
        Assert.assertEquals(sent.getAction(), Action.CREATE);
        Assert.assertEquals(sent.getInfo().getName(), "element-1-info");
        Assert.assertEquals(sent.getRelations().size(), 1);
        Assert.assertEquals(sent.getRelations().iterator().next().getType(), "element-1-relation");
        Assert.assertEquals(AdaptorTestSupport.text(sent.getData()), "element-1-data");
        Assert.assertEquals(AdaptorTestSupport.text(sent.getSearchableData()),
                "element-1-searchable-data");
        Assert.assertEquals(AdaptorTestSupport.text(sent.getVisualization()),
                "element-1-visualization");
        Assert.assertEquals(sent.getSubElements().size(), 1);
        Assert.assertEquals(sent.getSubElements().iterator().next().getId().getValue(),
                "sub-element");
    }

    @Test
    public void testSaveConvertsElementWithoutSubElementsToAnEmptyCollection() {
        ZusammenElement zusammenElement = element("element-1");
        zusammenElement.setSubElements(null);
        when(elementManager.save(eq(context), eq(elementContext), any(CoreElement.class),
                eq("saving"))).thenReturn(coreElement("saved-element"));

        Response<Element> response =
                adaptor.save(context, elementContext, zusammenElement, "saving");

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<CoreElement> captor = ArgumentCaptor.forClass(CoreElement.class);
        verify(elementManager)
                .save(eq(context), eq(elementContext), captor.capture(), eq("saving"));
        Assert.assertTrue(captor.getValue().getSubElements().isEmpty());
    }

    @Test
    public void testSaveFailureIsMappedToElementSaveError() {
        when(elementManager.save(eq(context), eq(elementContext), any(CoreElement.class),
                eq("saving"))).thenThrow(AdaptorTestSupport
                .failure(ErrorCode.MD_ELEMENT_UPDATE, Module.ZSTM, "stale element"));

        Response<Element> response =
                adaptor.save(context, elementContext, element("element-1"), "saving");

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ELEMENT_SAVE);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ELEMENT_UPDATE);
    }

    @Test
    public void testResolveConflictPassesConvertedElementAndResolutionToManager() {
        Response<?> response = adaptor.resolveConflict(context, elementContext, element("element-1"),
                Resolution.THEIRS);

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<CoreElement> elementCaptor = ArgumentCaptor.forClass(CoreElement.class);
        ArgumentCaptor<Resolution> resolutionCaptor = ArgumentCaptor.forClass(Resolution.class);
        verify(elementManager).resolveConflict(eq(context), eq(elementContext),
                elementCaptor.capture(), resolutionCaptor.capture());
        Assert.assertEquals(elementCaptor.getValue().getId().getValue(), "element-1");
        Assert.assertEquals(AdaptorTestSupport.text(elementCaptor.getValue().getData()),
                "element-1-data");
        Assert.assertEquals(resolutionCaptor.getValue(), Resolution.THEIRS);
    }

    @Test
    public void testResolveConflictFailureNestsTheManagerFailureAsTheCause() {
        when(elementManager.resolveConflict(eq(context), eq(elementContext), any(CoreElement.class),
                eq(Resolution.YOURS))).thenThrow(AdaptorTestSupport
                .failure(ErrorCode.MD_ELEMENT_RESOLVE_CONFLICT, Module.ZSTM, "no conflict"));

        Response<?> response = adaptor.resolveConflict(context, elementContext, element("element-1"),
                Resolution.YOURS);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSTM,
                ErrorCode.MD_ELEMENT_RESOLVE_CONFLICT);
    }

    @Test
    public void testSearchReturnsManagerSearchResult() {
        SearchCriteria searchCriteria = mock(SearchCriteria.class);
        SearchResult searchResult = mock(SearchResult.class);
        when(elementManager.search(context, searchCriteria)).thenReturn(searchResult);

        Response<SearchResult> response = adaptor.search(context, searchCriteria);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertSame(response.getValue(), searchResult);
        verify(elementManager).search(context, searchCriteria);
    }

    @Test
    public void testSearchFailureIsMappedToElementSearchError() {
        SearchCriteria searchCriteria = mock(SearchCriteria.class);
        when(elementManager.search(context, searchCriteria)).thenThrow(
                AdaptorTestSupport.failure(ErrorCode.MD_SEARCH, Module.ZSIM, "index unavailable"));

        Response<SearchResult> response = adaptor.search(context, searchCriteria);

        Assert.assertFalse(response.isSuccessful());
        AdaptorTestSupport.assertErrorCode(response.getReturnCode(), Module.ZDB,
                ErrorCode.ZU_ELEMENT_SEARCH);
        AdaptorTestSupport.assertErrorCode(response.getReturnCode().getReturnCode(), Module.ZSIM,
                ErrorCode.MD_SEARCH);
    }

    private static CoreElementInfo coreElementInfo(String elementId) {
        CoreElementInfo coreElementInfo = new CoreElementInfo();
        coreElementInfo.setId(new Id(elementId));
        coreElementInfo.setInfo(AdaptorTestSupport.info(elementId + "-info"));
        coreElementInfo.setRelations(
                Collections.singletonList(AdaptorTestSupport.relation(elementId + "-relation")));
        coreElementInfo.setSubElements(Collections.<CoreElementInfo>emptyList());
        return coreElementInfo;
    }

    private static CoreElement coreElement(String elementId) {
        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id(elementId));
        coreElement.setAction(Action.UPDATE);
        coreElement.setInfo(AdaptorTestSupport.info(elementId + "-info"));
        coreElement.setRelations(
                Collections.singletonList(AdaptorTestSupport.relation(elementId + "-relation")));
        coreElement.setData(AdaptorTestSupport.stream(elementId + "-data"));
        coreElement.setSearchableData(AdaptorTestSupport.stream(elementId + "-searchable-data"));
        coreElement.setVisualization(AdaptorTestSupport.stream(elementId + "-visualization"));
        return coreElement;
    }

    private static ZusammenElement element(String elementId) {
        ZusammenElement element = new ZusammenElement();
        element.setElementId(new Id(elementId));
        element.setAction(Action.CREATE);
        element.setInfo(AdaptorTestSupport.info(elementId + "-info"));
        Collection<Relation> relations =
                Collections.singletonList(AdaptorTestSupport.relation(elementId + "-relation"));
        element.setRelations(relations);
        element.setData(AdaptorTestSupport.stream(elementId + "-data"));
        element.setSearchableData(AdaptorTestSupport.stream(elementId + "-searchable-data"));
        element.setVisualization(AdaptorTestSupport.stream(elementId + "-visualization"));

        ZusammenElement subElement = new ZusammenElement();
        subElement.setElementId(new Id("sub-element"));
        subElement.setAction(Action.IGNORE);
        subElement.setInfo(AdaptorTestSupport.info("sub-element-info"));
        subElement.setData(AdaptorTestSupport.stream("sub-element-data"));
        element.addSubElement(subElement);
        return element;
    }
}
