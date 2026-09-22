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

package com.amdocs.zusammen.adaptor.inbound.api.types.item;

import com.amdocs.zusammen.datatypes.Id;
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

public class ZusammenElementTest {

  @Test
  public void testElementStartsEmpty() {
    ZusammenElement element = new ZusammenElement();

    Assert.assertNull(element.getAction());
    Assert.assertNull(element.getElementId());
    Assert.assertNull(element.getInfo());
    Assert.assertNull(element.getRelations());
    Assert.assertTrue(element.getSubElements().isEmpty());
  }

  @Test
  public void testSetActionElementIdAndInfo() {
    ZusammenElement element = new ZusammenElement();
    Id elementId = new Id("element-1");
    Info info = new Info();
    info.setName("element name");

    element.setAction(Action.DELETE);
    element.setElementId(elementId);
    element.setInfo(info);

    Assert.assertEquals(element.getAction(), Action.DELETE);
    Assert.assertEquals(element.getElementId(), elementId);
    Assert.assertEquals(element.getInfo().getName(), "element name");
  }

  @Test
  public void testSetRelationsKeepsCallerCollection() {
    ZusammenElement element = new ZusammenElement();
    Relation relation = new Relation();
    relation.setType("depends-on");
    Collection<Relation> relations = Arrays.asList(relation);

    element.setRelations(relations);

    Assert.assertSame(element.getRelations(), relations);
  }

  @Test
  public void testContentRoundTripKeepsEachStreamSeparate() {
    ZusammenElement element = new ZusammenElement();

    element.setData(stream("the data"));
    element.setSearchableData(stream("the searchable data"));
    element.setVisualization(stream("the visualization"));

    Assert.assertEquals(content(element.getData()), "the data");
    Assert.assertEquals(content(element.getSearchableData()), "the searchable data");
    Assert.assertEquals(content(element.getVisualization()), "the visualization");
  }

  @Test
  public void testContentIsReadableMoreThanOnce() {
    ZusammenElement element = new ZusammenElement();
    element.setData(stream("the data"));

    Assert.assertEquals(content(element.getData()), "the data");
    Assert.assertEquals(content(element.getData()), "the data");
  }

  @Test
  public void testUnsetContentIsAnEmptyStreamRatherThanNull() {
    ZusammenElement element = new ZusammenElement();

    Assert.assertEquals(content(element.getData()), "");
    Assert.assertEquals(content(element.getSearchableData()), "");
    Assert.assertEquals(content(element.getVisualization()), "");
  }

  @Test
  public void testSettingNullContentClearsIt() {
    ZusammenElement element = new ZusammenElement();
    element.setData(stream("the data"));
    element.setSearchableData(stream("the searchable data"));
    element.setVisualization(stream("the visualization"));

    element.setData(null);
    element.setSearchableData(null);
    element.setVisualization(null);

    Assert.assertEquals(content(element.getData()), "");
    Assert.assertEquals(content(element.getSearchableData()), "");
    Assert.assertEquals(content(element.getVisualization()), "");
  }

  @Test
  public void testAddSubElementAppendsAndReturnsTheSameElement() {
    ZusammenElement element = new ZusammenElement();
    ZusammenElement first = new ZusammenElement();
    first.setElementId(new Id("sub-1"));
    ZusammenElement second = new ZusammenElement();
    second.setElementId(new Id("sub-2"));

    ZusammenElement returned = element.addSubElement(first).addSubElement(second);

    Assert.assertSame(returned, element);
    Assert.assertEquals(element.getSubElements().size(), 2);
    Assert.assertTrue(element.getSubElements().contains(first));
    Assert.assertTrue(element.getSubElements().contains(second));
  }

  @Test
  public void testAddSubElementWritesIntoTheCollectionThatWasSet() {
    ZusammenElement element = new ZusammenElement();
    Collection<Element> subElements = new ArrayList<>();
    element.setSubElements(subElements);

    element.addSubElement(new ZusammenElement());

    Assert.assertSame(element.getSubElements(), subElements);
    Assert.assertEquals(subElements.size(), 1);
  }

  private InputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  private String content(InputStream inputStream) {
    return new String(FileUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
  }
}
