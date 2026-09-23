/*
 * Copyright © 2016-2017 European Support Limited
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
import com.amdocs.zusammen.adaptor.outbound.api.SearchIndexAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.item.ElementStateAdaptor;
import com.amdocs.zusammen.core.api.item.ItemManager;
import com.amdocs.zusammen.core.api.item.ItemVersionManager;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.core.api.types.CoreElementInfo;
import com.amdocs.zusammen.core.api.types.CoreMergeChange;
import com.amdocs.zusammen.core.api.types.CoreMergeConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeResult;
import com.amdocs.zusammen.core.impl.Messages;
import com.amdocs.zusammen.core.impl.TestUtils;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.datatypes.item.Resolution;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.searchindex.SearchCriteria;
import com.amdocs.zusammen.datatypes.searchindex.SearchResult;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElementManagerImplTest {
  private static final UserInfo USER = new UserInfo("ElementManagerImpl_user");

  @Mock
  private ElementStateAdaptor stateAdaptorMock;
  @Mock
  private CollaborationAdaptor collaborationAdaptorMock;
  @Mock
  private SearchIndexAdaptor searchIndexAdaptorMock;
  @Mock
  private ItemVersionManager versionManagerMock;
  @Mock
  private ItemManager itemManagerMock;
  @Mock
  private ElementHierarchyTraverser traverserMock;
  @Mock(name = "collaborativeStoreVisitor")
  private ElementVisitor collaborativeStoreVisitorMock;
  @Mock(name = "indexingVisitor")
  private ElementVisitor indexingVisitorMock;
  @InjectMocks
  @Spy
  private ElementManagerImpl elementManager;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(elementManager.getItemVersionManager(any())).thenReturn(versionManagerMock);
    when(elementManager.getItemManager(any())).thenReturn(itemManagerMock);
    when(elementManager.getStateAdaptor(any())).thenReturn(stateAdaptorMock);
    when(elementManager.getCollaborationAdaptor(any())).thenReturn(collaborationAdaptorMock);
    when(elementManager.getSearchIndexAdaptor(any())).thenReturn(searchIndexAdaptorMock);

    doReturn(true)
        .when(versionManagerMock).isExist(any(), any(), any(), any());
  }

  @Test
  public void testListRoots() throws Exception {
    testList(null);
  }

  @Test
  public void testListSubs() throws Exception {
    testList(new Id());
  }

  @Test
  public void testListRootsByChangeRef() throws Exception {
    testListByChangeRef(null);
  }

  @Test
  public void testListSubsByChangeRef() throws Exception {
    testListByChangeRef(new Id());
  }

  private void testList(Id parentElementId) {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id versionId = new Id();
    ElementContext elementContext = new ElementContext(itemId, versionId);

    Namespace namespace = parentElementId == null
        ? Namespace.ROOT_NAMESPACE
        : new Namespace(Namespace.ROOT_NAMESPACE, parentElementId);

    Collection<CoreElementInfo> retrievedElementInfos = Arrays.asList(
        createCoreElementInfo(new Id(), parentElementId, namespace, new Info(),
            Arrays.asList(new Relation(), new Relation()), new CoreElementInfo()),
        createCoreElementInfo(new Id(), parentElementId, namespace, new Info(),
            Collections.singletonList(new Relation()), new CoreElementInfo(),
            new CoreElementInfo()),
        createCoreElementInfo(new Id(), parentElementId, namespace, new Info(), new ArrayList<>()));
    Response<Collection<CoreElementInfo>> elementInfosResponse =
        new Response<>(retrievedElementInfos);

    doReturn(elementInfosResponse).when(stateAdaptorMock)
        .list(context, elementContext, parentElementId);

    Collection<CoreElementInfo> elementInfos =
        elementManager.list(context, elementContext, parentElementId);

    Assert.assertEquals(elementInfos, retrievedElementInfos);
  }


  private void testListByChangeRef(Id parentElementId) {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id versionId = new Id();
    Id revisionId = new Id("changeRef");
    ElementContext elementContext = new ElementContext(itemId, versionId, revisionId);

    Namespace namespace = parentElementId == null
        ? Namespace.ROOT_NAMESPACE
        : new Namespace(Namespace.ROOT_NAMESPACE, parentElementId);
    doReturn(new Response<>(namespace))
        .when(stateAdaptorMock).getNamespace(context, itemId, parentElementId);

    CoreElement sub1 = new CoreElement();
    sub1.setId(new Id());
    CoreElement sub2 = new CoreElement();
    sub2.setId(new Id());
    List<CoreElement> elements = Arrays.asList(
        createCoreElement(new Id(), Action.IGNORE, parentElementId, namespace,
            "element1", Arrays.asList(new Relation(), new Relation()), sub1, sub2),
        createCoreElement(new Id(), Action.IGNORE, parentElementId, namespace,
            "element2", Collections.singletonList(new Relation())));
    doReturn(new Response<>(elements)).when(collaborationAdaptorMock)
        .listElements(context, elementContext, namespace, parentElementId);

    Collection<CoreElementInfo> elementInfos =
        elementManager.list(context, elementContext, parentElementId);

    //Assert.assertEquals(elementInfos, retrievedElementInfos);
  }

  @Test
  public void testGetInfo() throws Exception {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id versionId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, versionId);

    CoreElementInfo retrievedElementInfo =
        createCoreElementInfo(elementId, new Id(), Namespace.ROOT_NAMESPACE);
    Response<CoreElementInfo> coreElementInfoResponse = new Response<>(retrievedElementInfo);
    doReturn(coreElementInfoResponse).when(stateAdaptorMock)
        .get(context, elementContext, elementId);

    CoreElementInfo elementInfo = elementManager.getInfo(context, elementContext, elementId);

    Assert.assertEquals(elementInfo, retrievedElementInfo);
  }

  @Test
  public void testGetInfoByChangeRef() throws Exception {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id versionId = new Id();
    Id elementId = new Id();
    Id revisionId = new Id("changeRef");
    ElementContext elementContext = new ElementContext(itemId, versionId, revisionId);


    Namespace namespace = Namespace.ROOT_NAMESPACE;
    doReturn(new Response<>(namespace))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);

    CoreElement sub1 = new CoreElement();
    sub1.setId(new Id());
    CoreElement sub2 = new CoreElement();
    sub2.setId(new Id());
    CoreElement retrievedCoreElement = createCoreElement(elementId, Action.IGNORE, null, namespace,
        "infoValue", Arrays.asList(new Relation(), new Relation()), sub1, sub2);

    Response<CoreElement> CoreElementRes = new Response<>(retrievedCoreElement);
    doReturn(CoreElementRes).when(collaborationAdaptorMock)
        .getElement(context, elementContext, namespace, elementId);

    CoreElementInfo elementInfo = elementManager.getInfo(context, elementContext, elementId);

    assertEquals(retrievedCoreElement, elementInfo);
  }

  private void assertEquals(CoreElement element, CoreElementInfo elementInfo) {
    Assert.assertEquals(elementInfo.getId(), element.getId());
    Assert.assertEquals(elementInfo.getInfo(), element.getInfo());
    Assert.assertEquals(elementInfo.getRelations(), element.getRelations());
    Assert.assertEquals(elementInfo.getSubElements().size(), element.getSubElements().size());

    Set<Id> subIds = elementInfo.getSubElements().stream()
        .map(CoreElementInfo::getId)
        .collect(Collectors.toSet());

    for (CoreElement sub : element.getSubElements()) {
      Assert.assertTrue(subIds.contains(sub.getId()));
    }
  }

  @Test
  public void testGet() throws Exception {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id versionId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, versionId);

    Namespace namespace = Namespace.ROOT_NAMESPACE;
    doReturn(new Response<>(namespace))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);

    CoreElement retrievedCoreElement = new CoreElement();
    Response<CoreElement> CoreElementRes = new Response<>(retrievedCoreElement);
    doReturn(CoreElementRes).when(collaborationAdaptorMock)
        .getElement(context, elementContext, namespace, elementId);

    CoreElement element = elementManager.get(context, elementContext, elementId);

    Assert.assertEquals(element, retrievedCoreElement);
  }

  @Test
  public void testGetTreeAssemblesTheCollaborationRead() throws Exception {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id versionId = new Id();
    Id elementId = new Id("root");
    ElementContext elementContext = new ElementContext(itemId, versionId);
    Namespace namespace = Namespace.ROOT_NAMESPACE;
    doReturn(new Response<>(namespace)).when(stateAdaptorMock).getNamespace(context, itemId, elementId);

    CoreElement root = new CoreElement();
    root.setId(elementId);
    CoreElement subStub = new CoreElement();
    subStub.setId(new Id("sub"));
    root.setSubElements(new ArrayList<>(Collections.singletonList(subStub)));
    CoreElement sub = new CoreElement();
    sub.setId(new Id("sub"));
    sub.setSubElements(new ArrayList<>());
    doReturn(new Response<>(Arrays.asList(root, sub))).when(collaborationAdaptorMock)
        .listElementTree(context, elementContext, namespace, elementId, 1);

    CoreElement tree = elementManager.getTree(context, elementContext, elementId, 1);

    Assert.assertSame(tree, root);
    Assert.assertSame(tree.getSubElements().iterator().next(), sub);
  }

  @Test
  public void testGetTreeOfAnElementWithoutNamespaceIsNull() throws Exception {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id elementId = new Id();
    doReturn(new Response<>((Namespace) null)).when(stateAdaptorMock).getNamespace(context, itemId, elementId);

    Assert.assertNull(elementManager.getTree(context, new ElementContext(itemId, new Id()), elementId, 2));
    verify(collaborationAdaptorMock, never()).listElementTree(any(), any(), any(), any(), anyInt());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetTreeRejectsANegativeDepth() throws Exception {
    elementManager.getTree(TestUtils.createSessionContext(USER, "test"),
        new ElementContext(new Id(), new Id()), new Id(), -1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetTreeRejectsANullElementId() throws Exception {
    elementManager.getTree(TestUtils.createSessionContext(USER, "test"),
        new ElementContext(new Id(), new Id()), null, 1);
  }

  private CoreElementInfo createCoreElementInfo(Id id, Id parentId, Namespace parentNamespace) {
    CoreElementInfo elementInfo = new CoreElementInfo();
    elementInfo.setId(id);
    elementInfo.setParentId(parentId);
    elementInfo.setNamespace(new Namespace(parentNamespace, parentId));
    return elementInfo;
  }

  @Test
  public void testSaveWithCreateRoot() throws Exception {
    CoreElement root = createCoreElement(null, Action.CREATE, null, null, "root", null);
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    ElementContext elementContext = new ElementContext(new Id(), new Id());

    CoreElement returnedRoot =
        elementManager.save(context, elementContext, root, "save create root!");

    Assert.assertEquals(returnedRoot.getParentId(), null);
    Assert.assertEquals(returnedRoot.getNamespace(), Namespace.ROOT_NAMESPACE);
    verify(traverserMock)
        .traverse(context, elementContext, Space.PRIVATE, root, collaborativeStoreVisitorMock);
    verify(traverserMock).traverse(context, elementContext, Space.PRIVATE, root,
        indexingVisitorMock);
  }

  @Test
  public void testSaveWithNonCreateRoot() throws Exception {
    CoreElement root = createCoreElement(new Id(), Action.UPDATE, null, null, "root", null);
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    ElementContext elementContext = new ElementContext(new Id(), new Id());

    Id parentElementId = new Id();
    Namespace namespace = new Namespace(Namespace.ROOT_NAMESPACE, parentElementId);
    doReturn(new Response<>(namespace))
        .when(stateAdaptorMock).getNamespace(context, elementContext.getItemId(), root.getId());

    CoreElement returnedRoot =
        elementManager.save(context, elementContext, root, "save non-create root!");

    Assert.assertEquals(returnedRoot.getId(), root.getId());
    Assert.assertEquals(root.getParentId(), parentElementId);
    Assert.assertEquals(root.getNamespace(), namespace);
    verify(traverserMock)
        .traverse(context, elementContext, Space.PRIVATE, root, collaborativeStoreVisitorMock);
    verify(traverserMock).traverse(context, elementContext, Space.PRIVATE, root,
        indexingVisitorMock);
  }

  @Test
  public void testSearch() throws Exception {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    SearchCriteria searchCriteria = new SearchCriteria() {
    };

    SearchResult retrievedSearchResult = new SearchResult() {
    };
    Response<SearchResult> searchResultResponse = new Response<>(retrievedSearchResult);
    doReturn(searchResultResponse).when(searchIndexAdaptorMock).search(context, searchCriteria);

    SearchResult searchResult = elementManager.search(context, searchCriteria);

    Assert.assertEquals(searchResult, retrievedSearchResult);
  }

  private CoreElementInfo createCoreElementInfo(Id elementId, Id parentId, Namespace
      namespace, Info info, Collection<Relation> relations, CoreElementInfo... subElements) {
    CoreElementInfo coreElementInfo = new CoreElementInfo();
    coreElementInfo.setId(elementId);
    coreElementInfo.setInfo(info);
    coreElementInfo.setRelations(relations);
    coreElementInfo.setSubElements(Arrays.asList(subElements));
    return coreElementInfo;
  }

  @Test
  public void testListByRevisionOfAnElementWithoutNamespaceIsEmpty() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id(), new Id("revision"));

    Namespace noNamespace = null;
    doReturn(new Response<>(noNamespace))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);

    Collection<CoreElementInfo> elementInfos =
        elementManager.list(context, elementContext, elementId);

    Assert.assertTrue(elementInfos.isEmpty());
    verify(collaborationAdaptorMock, never()).listElements(any(), any(), any(), any());
  }

  @Test
  public void testListFailurePropagates() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    ElementContext elementContext = new ElementContext(new Id(), new Id());
    ReturnCode cause = stateStoreFailure();
    doReturn(new Response<Collection<CoreElementInfo>>(cause))
        .when(stateAdaptorMock).list(context, elementContext, null);

    TestUtils.assertWrappedFailure(
        TestUtils.captureFailure(() -> elementManager.list(context, elementContext, null)),
        ErrorCode.ZU_ELEMENT_LIST, cause);
  }

  @Test
  public void testGetInfoByRevisionOfAnElementWithoutNamespaceIsNull() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id(), new Id("revision"));

    Namespace noNamespace = null;
    doReturn(new Response<>(noNamespace))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);

    Assert.assertNull(elementManager.getInfo(context, elementContext, elementId));
    verify(collaborationAdaptorMock, never()).getElement(any(), any(), any(), any());
  }

  @Test
  public void testGetInfoByRevisionOfAnElementTheStoreDoesNotHaveIsNull() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id(), new Id("revision"));

    doReturn(new Response<>(Namespace.ROOT_NAMESPACE))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);
    CoreElement noElement = null;
    doReturn(new Response<>(noElement)).when(collaborationAdaptorMock)
        .getElement(context, elementContext, Namespace.ROOT_NAMESPACE, elementId);

    Assert.assertNull(elementManager.getInfo(context, elementContext, elementId));
  }

  @Test
  public void testGetInfoFailurePropagates() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    ElementContext elementContext = new ElementContext(new Id(), new Id());
    Id elementId = new Id();
    ReturnCode cause = stateStoreFailure();
    doReturn(new Response<CoreElementInfo>(cause))
        .when(stateAdaptorMock).get(context, elementContext, elementId);

    TestUtils.assertWrappedFailure(
        TestUtils.captureFailure(() -> elementManager.getInfo(context, elementContext, elementId)),
        ErrorCode.ZU_ELEMENT_GET_INFO, cause);
  }

  @Test
  public void testGetOfAnElementWithoutNamespaceIsNull() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());

    Namespace noNamespace = null;
    doReturn(new Response<>(noNamespace))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);

    Assert.assertNull(elementManager.get(context, elementContext, elementId));
    verify(collaborationAdaptorMock, never()).getElement(any(), any(), any(), any());
  }

  @Test
  public void testGetFailurePropagates() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());
    doReturn(new Response<>(Namespace.ROOT_NAMESPACE))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);
    ReturnCode cause = collaborationStoreFailure();
    doReturn(new Response<CoreElement>(cause)).when(collaborationAdaptorMock)
        .getElement(context, elementContext, Namespace.ROOT_NAMESPACE, elementId);

    TestUtils.assertWrappedFailure(
        TestUtils.captureFailure(() -> elementManager.get(context, elementContext, elementId)),
        ErrorCode.ZU_ELEMENT_GET, cause);
  }

  @Test
  public void testGetConflict() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());
    Namespace namespace = new Namespace(Namespace.ROOT_NAMESPACE, new Id());
    doReturn(new Response<>(namespace))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);

    CoreElementConflict retrievedConflict = new CoreElementConflict();
    retrievedConflict.setLocalElement(new CoreElement());
    retrievedConflict.setRemoteElement(new CoreElement());
    doReturn(new Response<>(retrievedConflict)).when(collaborationAdaptorMock)
        .getElementConflict(context, elementContext, namespace, elementId);

    Assert.assertSame(elementManager.getConflict(context, elementContext, elementId),
        retrievedConflict);
  }

  @Test
  public void testGetConflictOfAnElementWithoutNamespaceIsNull() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());

    Namespace noNamespace = null;
    doReturn(new Response<>(noNamespace))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);

    Assert.assertNull(elementManager.getConflict(context, elementContext, elementId));
    verify(collaborationAdaptorMock, never()).getElementConflict(any(), any(), any(), any());
  }

  @Test
  public void testGetConflictFailurePropagates() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id elementId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());
    doReturn(new Response<>(Namespace.ROOT_NAMESPACE))
        .when(stateAdaptorMock).getNamespace(context, itemId, elementId);
    ReturnCode cause = collaborationStoreFailure();
    doReturn(new Response<CoreElementConflict>(cause)).when(collaborationAdaptorMock)
        .getElementConflict(context, elementContext, Namespace.ROOT_NAMESPACE, elementId);

    TestUtils.assertWrappedFailure(TestUtils
            .captureFailure(() -> elementManager.getConflict(context, elementContext, elementId)),
        ErrorCode.ZU_ELEMENT_GET_CONFLICT, cause);
  }

  @Test
  public void testSaveCommitsAndBumpsTheVersionModificationTime() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    ElementContext elementContext = new ElementContext(new Id(), new Id());
    CoreElement root = createCoreElement(null, Action.CREATE, null, null, "root", null);

    elementManager.save(context, elementContext, root, "commit message");

    verify(collaborationAdaptorMock).commitElements(context, elementContext, "commit message");
    verify(versionManagerMock).updateModificationTime(eq(context), eq(Space.PRIVATE),
        eq(elementContext.getItemId()), eq(elementContext.getVersionId()), any(Date.class));
  }

  @Test
  public void testResolveConflictOfACompletedMergeSavesTheChange() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());
    CoreElement element = createCoreElement(new Id(), Action.UPDATE, null, null, "element", null);

    Id parentElementId = new Id();
    Namespace namespace = new Namespace(Namespace.ROOT_NAMESPACE, parentElementId);
    doReturn(new Response<>(namespace))
        .when(stateAdaptorMock).getNamespace(context, itemId, element.getId());

    CoreElement changedElement = createCoreElement(new Id(), Action.UPDATE, null, null, "changed",
        null);
    ItemVersionChange changedVersion = new ItemVersionChange();
    changedVersion.setAction(Action.UPDATE);
    changedVersion.setItemVersion(
        TestUtils.createItemVersion(elementContext.getVersionId(), new Id(), "v1"));
    CoreMergeChange mergeChange = new CoreMergeChange();
    mergeChange.setChangedVersion(changedVersion);
    mergeChange.setChangedElements(Collections.singletonList(changedElement));

    CoreMergeResult retrievedResult = new CoreMergeResult();
    retrievedResult.setChange(mergeChange);
    doReturn(new Response<>(retrievedResult)).when(collaborationAdaptorMock)
        .resolveElementConflict(context, elementContext, element, Resolution.THEIRS);

    CoreMergeResult result =
        elementManager.resolveConflict(context, elementContext, element, Resolution.THEIRS);

    Assert.assertSame(result, retrievedResult);
    Assert.assertEquals(element.getNamespace(), namespace);
    Assert.assertEquals(element.getParentId(), parentElementId);
    verify(versionManagerMock).saveMergeChange(context, Space.PUBLIC, itemId, changedVersion);
    verify(indexingVisitorMock)
        .visit(context, elementContext, Space.PUBLIC, changedElement);
  }

  @Test
  public void testResolveConflictOfAnIncompleteMergeSavesNothing() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());
    CoreElement element = createCoreElement(new Id(), Action.UPDATE, null, null, "element", null);
    doReturn(new Response<>(Namespace.ROOT_NAMESPACE))
        .when(stateAdaptorMock).getNamespace(context, itemId, element.getId());

    CoreMergeConflict conflict = new CoreMergeConflict();
    conflict.setElementConflicts(Collections.singletonList(new CoreElementConflict()));
    CoreMergeResult retrievedResult = new CoreMergeResult();
    retrievedResult.setConflict(conflict);
    doReturn(new Response<>(retrievedResult)).when(collaborationAdaptorMock)
        .resolveElementConflict(context, elementContext, element, Resolution.OTHER);

    CoreMergeResult result =
        elementManager.resolveConflict(context, elementContext, element, Resolution.OTHER);

    Assert.assertSame(result, retrievedResult);
    verify(versionManagerMock, never()).saveMergeChange(any(), any(), any(), any());
    verify(indexingVisitorMock, never()).visit(any(), any(), any(), any());
  }

  @Test
  public void testResolveConflictWithoutAResultSavesNothing() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());
    CoreElement element = createCoreElement(new Id(), Action.UPDATE, null, null, "element", null);
    doReturn(new Response<>(Namespace.ROOT_NAMESPACE))
        .when(stateAdaptorMock).getNamespace(context, itemId, element.getId());

    CoreMergeResult noResult = null;
    doReturn(new Response<>(noResult)).when(collaborationAdaptorMock)
        .resolveElementConflict(context, elementContext, element, Resolution.THEIRS);

    Assert.assertNull(
        elementManager.resolveConflict(context, elementContext, element, Resolution.THEIRS));
    verify(versionManagerMock, never()).saveMergeChange(any(), any(), any(), any());
    verify(indexingVisitorMock, never()).visit(any(), any(), any(), any());
  }

  @Test
  public void testResolveConflictOfACompletedMergeWithoutAChangeSavesNothing() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());
    CoreElement element = createCoreElement(new Id(), Action.UPDATE, null, null, "element", null);
    doReturn(new Response<>(Namespace.ROOT_NAMESPACE))
        .when(stateAdaptorMock).getNamespace(context, itemId, element.getId());

    CoreMergeResult retrievedResult = new CoreMergeResult();
    doReturn(new Response<>(retrievedResult)).when(collaborationAdaptorMock)
        .resolveElementConflict(context, elementContext, element, Resolution.YOURS);

    Assert.assertSame(
        elementManager.resolveConflict(context, elementContext, element, Resolution.YOURS),
        retrievedResult);
    verify(versionManagerMock, never()).saveMergeChange(any(), any(), any(), any());
    verify(indexingVisitorMock, never()).visit(any(), any(), any(), any());
  }

  @Test
  public void testResolveConflictFailurePropagates() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    ElementContext elementContext = new ElementContext(itemId, new Id());
    CoreElement element = createCoreElement(new Id(), Action.UPDATE, null, null, "element", null);
    doReturn(new Response<>(Namespace.ROOT_NAMESPACE))
        .when(stateAdaptorMock).getNamespace(context, itemId, element.getId());

    ReturnCode cause = collaborationStoreFailure();
    doReturn(new Response<CoreMergeResult>(cause)).when(collaborationAdaptorMock)
        .resolveElementConflict(context, elementContext, element, Resolution.YOURS);

    TestUtils.assertWrappedFailure(TestUtils.captureFailure(() -> elementManager
            .resolveConflict(context, elementContext, element, Resolution.YOURS)),
        ErrorCode.ZU_ELEMENT_RESOLVE_CONFLICT, cause);
  }

  @Test
  public void testSearchFailurePropagates() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    SearchCriteria searchCriteria = new SearchCriteria() {
    };
    ReturnCode cause = new ReturnCode(ErrorCode.IN_SEARCH, Module.ZSIP, "index down", null);
    doReturn(new Response<SearchResult>(cause))
        .when(searchIndexAdaptorMock).search(context, searchCriteria);

    TestUtils.assertWrappedFailure(
        TestUtils.captureFailure(() -> elementManager.search(context, searchCriteria)),
        ErrorCode.ZU_SEARCH, cause);
  }

  @Test
  public void testSaveMergeChangeIndexesEveryChangedElement() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    ElementContext elementContext = new ElementContext(new Id(), new Id());
    CoreElement first = createCoreElement(new Id(), Action.CREATE, null, null, "first", null);
    CoreElement second = createCoreElement(new Id(), Action.DELETE, null, null, "second", null);

    elementManager
        .saveMergeChange(context, Space.PUBLIC, elementContext, Arrays.asList(first, second));

    verify(indexingVisitorMock).visit(context, elementContext, Space.PUBLIC, first);
    verify(indexingVisitorMock).visit(context, elementContext, Space.PUBLIC, second);
    verify(collaborativeStoreVisitorMock, never()).visit(any(), any(), any(), any());
  }

  @Test
  public void testSaveMergeChangeWithoutElementsDoesNothing() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    ElementContext elementContext = new ElementContext(new Id(), new Id());

    elementManager.saveMergeChange(context, Space.PUBLIC, elementContext, null);

    verify(indexingVisitorMock, never()).visit(any(), any(), any(), any());
  }

  @Test
  public void testOperationOnAMissingVersionOfAnExistingItemReportsTheMissingVersion() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id versionId = new Id();
    ElementContext elementContext = new ElementContext(itemId, versionId);
    doReturn(false).when(versionManagerMock).isExist(context, Space.PRIVATE, itemId, versionId);
    doReturn(true).when(itemManagerMock).isExist(context, itemId);

    ReturnCode returnCode =
        TestUtils.captureFailure(() -> elementManager.list(context, elementContext, null));

    TestUtils.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_VERSION_NOT_EXIST);
    Assert.assertEquals(returnCode.getMessage(), String
        .format(Messages.ITEM_VERSION_NOT_EXIST, itemId, versionId, Space.PRIVATE));
  }

  @Test
  public void testOperationOnAMissingItemReportsTheMissingItem() {
    SessionContext context = TestUtils.createSessionContext(USER, "test");
    Id itemId = new Id();
    Id versionId = new Id();
    ElementContext elementContext = new ElementContext(itemId, versionId);
    doReturn(false).when(versionManagerMock).isExist(context, Space.PRIVATE, itemId, versionId);
    doReturn(false).when(itemManagerMock).isExist(context, itemId);

    ReturnCode returnCode =
        TestUtils.captureFailure(() -> elementManager.list(context, elementContext, null));

    TestUtils.assertErrorCode(returnCode, Module.ZDB, ErrorCode.ZU_ITEM_VERSION_NOT_EXIST);
    Assert.assertEquals(returnCode.getMessage(),
        String.format(Messages.ITEM_NOT_EXIST, itemId));
  }

  private ReturnCode stateStoreFailure() {
    return new ReturnCode(ErrorCode.MD_ELEMENT_GET, Module.ZMDP, "state store down", null);
  }

  private ReturnCode collaborationStoreFailure() {
    return new ReturnCode(ErrorCode.CL_ELEMENT_GET, Module.ZCSP, "collaboration store down", null);
  }

  private CoreElement createCoreElement(Id elementId, Action action, Id parentId,
                                        Namespace namespace, String infoValue,
                                        Collection<Relation> relations,
                                        CoreElement... subElements) {
    CoreElement element = new CoreElement();
    element.setId(elementId);
    element.setAction(action);
    element.setParentId(parentId);
    element.setNamespace(namespace);
    element.setInfo(TestUtils.createInfo(infoValue));
    element.setRelations(relations);
    element.setSubElements(Arrays.asList(subElements));
    return element;
  }
}
