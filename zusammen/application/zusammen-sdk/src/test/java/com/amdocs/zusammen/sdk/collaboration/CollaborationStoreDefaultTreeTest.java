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

package com.amdocs.zusammen.sdk.collaboration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CollaborationStoreDefaultTreeTest {

  private static final SessionContext CONTEXT = new SessionContext();
  private static final ElementContext ELEMENT_CONTEXT = new ElementContext(new Id("item"), new Id("version"));
  private CollaborationStore store;

  @BeforeMethod
  public void setUp() {
    store = mock(CollaborationStore.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
  }

  @Test
  public void testWalksLevelByLevelUsingEachParentsNamespace() {
    CollaborationElement root = element("root", Namespace.ROOT_NAMESPACE);
    Namespace rootChildren = new Namespace(Namespace.ROOT_NAMESPACE, root.getId());
    CollaborationElement a = element("a", rootChildren);
    CollaborationElement a1 = element("a1", new Namespace(rootChildren, a.getId()));
    doReturn(new Response<>(root)).when(store).getElement(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE, root.getId());
    doReturn(new Response<>(Collections.singletonList(a))).when(store).listElements(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE, root.getId());
    doReturn(new Response<>(Collections.singletonList(a1))).when(store).listElements(CONTEXT, ELEMENT_CONTEXT, rootChildren, a.getId());
    doReturn(new Response<>(Collections.emptyList())).when(store).listElements(any(), any(), any(), eq(a1.getId()));

    Response<Collection<CollaborationElement>> tree =
        store.listElementTree(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE, root.getId(), Integer.MAX_VALUE);

    Assert.assertEquals(ids(tree.getValue()), Arrays.asList(new Id("root"), new Id("a"), new Id("a1")));
  }

  @Test
  public void testDepthZeroDoesNotList() {
    CollaborationElement root = element("root", Namespace.ROOT_NAMESPACE);
    doReturn(new Response<>(root)).when(store).getElement(any(), any(), any(), any());

    Response<Collection<CollaborationElement>> tree =
        store.listElementTree(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE, root.getId(), 0);

    Assert.assertEquals(ids(tree.getValue()), Collections.singletonList(new Id("root")));
    Mockito.verify(store, Mockito.never()).listElements(any(), any(), any(), any());
  }

  @Test
  public void testMissingElementGivesAnEmptyTree() {
    doReturn(new Response<>((CollaborationElement) null)).when(store).getElement(any(), any(), any(), any());

    Assert.assertTrue(store.listElementTree(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE, new Id("x"), 2)
        .getValue().isEmpty());
  }

  @Test
  public void testAFailedListIsPassedThrough() {
    CollaborationElement root = element("root", Namespace.ROOT_NAMESPACE);
    ReturnCode failure = new ReturnCode(ErrorCode.CL_ELEMENT_GET, Module.ZCSP, "boom", null);
    doReturn(new Response<>(root)).when(store).getElement(any(), any(), any(), any());
    doReturn(new Response<>(failure)).when(store).listElements(any(), any(), any(), any());

    Response<Collection<CollaborationElement>> tree =
        store.listElementTree(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE, root.getId(), 1);

    Assert.assertFalse(tree.isSuccessful());
    Assert.assertSame(tree.getReturnCode(), failure);
  }

  @Test
  public void testABackPointerIsNotFollowed() {
    CollaborationElement root = element("root", Namespace.ROOT_NAMESPACE);
    CollaborationElement a = element("a", new Namespace(Namespace.ROOT_NAMESPACE, root.getId()));
    doReturn(new Response<>(root)).when(store).getElement(any(), any(), any(), any());
    doReturn(new Response<>(Collections.singletonList(a))).when(store).listElements(any(), any(), any(), eq(root.getId()));
    doReturn(new Response<>(Collections.singletonList(root))).when(store).listElements(any(), any(), any(), eq(a.getId()));

    Response<Collection<CollaborationElement>> tree =
        store.listElementTree(CONTEXT, ELEMENT_CONTEXT, Namespace.ROOT_NAMESPACE, root.getId(), Integer.MAX_VALUE);

    Assert.assertEquals(ids(tree.getValue()), Arrays.asList(new Id("root"), new Id("a")));
  }

  private static CollaborationElement element(String id, Namespace namespace) {
    return new CollaborationElement(new Id("item"), new Id("version"), namespace, new Id(id));
  }

  private static List<Id> ids(Collection<CollaborationElement> elements) {
    return elements.stream().map(CollaborationElement::getId).collect(Collectors.toList());
  }
}
