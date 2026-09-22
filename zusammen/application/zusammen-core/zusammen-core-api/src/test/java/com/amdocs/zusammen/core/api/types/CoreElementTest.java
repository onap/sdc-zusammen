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

package com.amdocs.zusammen.core.api.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.utils.fileutils.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class CoreElementTest {

  @Test
  public void testActionDefaultsToIgnore() {
    Assert.assertEquals(new CoreElement().getAction(), Action.IGNORE);
  }

  @Test
  public void testElementStartsEmpty() {
    CoreElement element = new CoreElement();

    Assert.assertNull(element.getId());
    Assert.assertNull(element.getParentId());
    Assert.assertNull(element.getNamespace());
    Assert.assertNull(element.getInfo());
    Assert.assertTrue(element.getRelations().isEmpty());
    Assert.assertTrue(element.getSubElements().isEmpty());
  }

  @Test
  public void testSetIdentityAndInfo() {
    CoreElement element = new CoreElement();
    Id id = new Id("element-1");
    Id parentId = new Id("parent-2");
    Namespace namespace = new Namespace(new Namespace(), parentId);
    Info info = new Info();
    info.setName("core element");

    element.setAction(Action.CREATE);
    element.setId(id);
    element.setParentId(parentId);
    element.setNamespace(namespace);
    element.setInfo(info);

    Assert.assertEquals(element.getAction(), Action.CREATE);
    Assert.assertEquals(element.getId(), id);
    Assert.assertEquals(element.getParentId(), parentId);
    Assert.assertEquals(element.getNamespace(), namespace);
    Assert.assertEquals(element.getInfo().getName(), "core element");
  }

  @Test
  public void testSetRelationsKeepsCallerCollection() {
    CoreElement element = new CoreElement();
    Relation relation = new Relation();
    relation.setType("depends-on");
    Collection<Relation> relations = Arrays.asList(relation);

    element.setRelations(relations);

    Assert.assertSame(element.getRelations(), relations);
  }

  @Test
  public void testSetSubElementsKeepsCallerCollection() {
    CoreElement element = new CoreElement();
    Collection<CoreElement> subElements = new ArrayList<>();
    CoreElement subElement = new CoreElement();
    subElement.setId(new Id("sub-3"));
    subElements.add(subElement);

    element.setSubElements(subElements);

    Assert.assertSame(element.getSubElements(), subElements);
    Assert.assertEquals(element.getSubElements().iterator().next().getId().getValue(), "sub-3");
  }

  @Test
  public void testContentRoundTripKeepsEachStreamSeparate() {
    CoreElement element = new CoreElement();

    element.setData(stream("the data"));
    element.setSearchableData(stream("the searchable data"));
    element.setVisualization(stream("the visualization"));

    Assert.assertEquals(content(element.getData()), "the data");
    Assert.assertEquals(content(element.getSearchableData()), "the searchable data");
    Assert.assertEquals(content(element.getVisualization()), "the visualization");
  }

  @Test
  public void testContentIsReadableMoreThanOnce() {
    CoreElement element = new CoreElement();
    element.setData(stream("the data"));

    Assert.assertEquals(content(element.getData()), "the data");
    Assert.assertEquals(content(element.getData()), "the data");
  }

  @Test
  public void testContentIsNullWhenNotSet() {
    CoreElement element = new CoreElement();

    Assert.assertNull(element.getData());
    Assert.assertNull(element.getSearchableData());
    Assert.assertNull(element.getVisualization());
  }

  @Test
  public void testSettingNullContentClearsIt() {
    CoreElement element = new CoreElement();
    element.setData(stream("the data"));
    element.setSearchableData(stream("the searchable data"));
    element.setVisualization(stream("the visualization"));

    element.setData(null);
    element.setSearchableData(null);
    element.setVisualization(null);

    Assert.assertNull(element.getData());
    Assert.assertNull(element.getSearchableData());
    Assert.assertNull(element.getVisualization());
  }

  @Test
  public void testEmptyContentIsReportedAsNull() {
    CoreElement element = new CoreElement();

    element.setData(stream(""));
    element.setSearchableData(stream(""));
    element.setVisualization(stream(""));

    Assert.assertNull(element.getData());
    Assert.assertNull(element.getSearchableData());
    Assert.assertNull(element.getVisualization());
  }

  private InputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  private String content(InputStream inputStream) {
    return new String(FileUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
  }
}
