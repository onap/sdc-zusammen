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

import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.datatypes.Id;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ElementTreeAssemblerTest {

  @Test
  public void testReplacesStubChildrenWithTheLoadedElements() {
    CoreElement root = element("root", "a", "b");
    CoreElement a = element("a", "a1");
    CoreElement b = element("b");

    CoreElement tree = ElementTreeAssembler.assemble(new Id("root"), Arrays.asList(root, a, b));

    Assert.assertSame(tree, root);
    List<CoreElement> children = new ArrayList<>(tree.getSubElements());
    Assert.assertSame(children.get(0), a);
    Assert.assertSame(children.get(1), b);
    CoreElement a1Stub = a.getSubElements().iterator().next();
    Assert.assertEquals(a1Stub.getId(), new Id("a1"));
    Assert.assertNull(a1Stub.getInfo(), "a child past the loaded depth stays an id-only stub");
  }

  @Test
  public void testRootMissingFromTheReadGivesNull() {
    Assert.assertNull(ElementTreeAssembler.assemble(new Id("root"), Collections.emptyList()));
  }

  @Test(timeOut = 5000)
  public void testABackPointerStaysAStubSoTheTreeHasNoCycle() {
    CoreElement root = element("root", "a");
    CoreElement a = element("a", "root");

    CoreElement tree = ElementTreeAssembler.assemble(new Id("root"), Arrays.asList(root, a));

    CoreElement backPointer = tree.getSubElements().iterator().next().getSubElements().iterator().next();
    Assert.assertEquals(backPointer.getId(), new Id("root"));
    Assert.assertNotSame(backPointer, root);
    Assert.assertTrue(backPointer.getSubElements() == null || backPointer.getSubElements().isEmpty());
  }

  @Test(timeOut = 5000)
  public void testAnElementListedUnderTwoParentsIsAttachedOnce() {
    CoreElement root = element("root", "a", "b");
    CoreElement a = element("a", "shared");
    CoreElement b = element("b", "shared");
    CoreElement shared = element("shared");

    ElementTreeAssembler.assemble(new Id("root"), Arrays.asList(root, a, b, shared));

    Assert.assertSame(a.getSubElements().iterator().next(), shared);
    Assert.assertNotSame(b.getSubElements().iterator().next(), shared);
  }

  private static CoreElement element(String id, String... subIds) {
    CoreElement element = new CoreElement();
    element.setId(new Id(id));
    element.setSubElements(Arrays.stream(subIds).map(ElementTreeAssemblerTest::stub).collect(Collectors.toList()));
    return element;
  }

  private static CoreElement stub(String id) {
    CoreElement stub = new CoreElement();
    stub.setId(new Id(id));
    return stub;
  }
}
