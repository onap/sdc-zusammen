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

package com.amdocs.zusammen.sdk.collaboration.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.utils.fileutils.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CollaborationElementTest {

  private static final Id ITEM_ID = new Id("item-1");
  private static final Id VERSION_ID = new Id("version-2");
  private static final Id ELEMENT_ID = new Id("element-3");

  @Test
  public void testConstructorPopulatesIdentity() {
    Namespace namespace = new Namespace(new Namespace(), new Id("parent-4"));
    CollaborationElement element =
        new CollaborationElement(ITEM_ID, VERSION_ID, namespace, ELEMENT_ID);

    Assert.assertEquals(element.getItemId(), ITEM_ID);
    Assert.assertEquals(element.getVersionId(), VERSION_ID);
    Assert.assertEquals(element.getNamespace(), namespace);
    Assert.assertEquals(element.getId(), ELEMENT_ID);
  }

  @Test
  public void testInheritedDescriptorPropertiesAreSettable() {
    CollaborationElement element = newElement();
    Info info = new Info();
    info.setName("collaboration element");

    element.setParentId(new Id("parent-5"));
    element.setInfo(info);

    Assert.assertEquals(element.getParentId().getValue(), "parent-5");
    Assert.assertEquals(element.getInfo().getName(), "collaboration element");
  }

  @Test
  public void testContentRoundTripKeepsEachStreamSeparate() {
    CollaborationElement element = newElement();

    element.setData(stream("the data"));
    element.setSearchableData(stream("the searchable data"));
    element.setVisualization(stream("the visualization"));

    Assert.assertEquals(content(element.getData()), "the data");
    Assert.assertEquals(content(element.getSearchableData()), "the searchable data");
    Assert.assertEquals(content(element.getVisualization()), "the visualization");
  }

  @Test
  public void testContentIsReadableMoreThanOnce() {
    CollaborationElement element = newElement();
    element.setData(stream("the data"));

    Assert.assertEquals(content(element.getData()), "the data");
    Assert.assertEquals(content(element.getData()), "the data");
  }

  @Test
  public void testContentIsNullWhenNotSet() {
    CollaborationElement element = newElement();

    Assert.assertNull(element.getData());
    Assert.assertNull(element.getSearchableData());
    Assert.assertNull(element.getVisualization());
  }

  @Test
  public void testSettingNullContentClearsIt() {
    CollaborationElement element = newElement();
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
    CollaborationElement element = newElement();

    element.setData(stream(""));
    element.setSearchableData(stream(""));
    element.setVisualization(stream(""));

    Assert.assertNull(element.getData());
    Assert.assertNull(element.getSearchableData());
    Assert.assertNull(element.getVisualization());
  }

  private CollaborationElement newElement() {
    return new CollaborationElement(ITEM_ID, VERSION_ID, new Namespace(), ELEMENT_ID);
  }

  private InputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  private String content(InputStream inputStream) {
    return new String(FileUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
  }
}
