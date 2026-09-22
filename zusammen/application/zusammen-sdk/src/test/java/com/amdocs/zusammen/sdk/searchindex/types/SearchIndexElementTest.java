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

package com.amdocs.zusammen.sdk.searchindex.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.utils.fileutils.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SearchIndexElementTest {

  private static final Id ITEM_ID = new Id("item-1");
  private static final Id VERSION_ID = new Id("version-2");
  private static final Id ELEMENT_ID = new Id("element-3");

  @Test
  public void testConstructorPopulatesIdentity() {
    Namespace namespace = new Namespace(new Namespace(), new Id("parent-4"));
    SearchIndexElement element =
        new SearchIndexElement(ITEM_ID, VERSION_ID, namespace, ELEMENT_ID);

    Assert.assertEquals(element.getItemId(), ITEM_ID);
    Assert.assertEquals(element.getVersionId(), VERSION_ID);
    Assert.assertEquals(element.getNamespace(), namespace);
    Assert.assertEquals(element.getId(), ELEMENT_ID);
  }

  @Test
  public void testSpaceDefaultsToPrivate() {
    Assert.assertEquals(newElement().getSpace(), Space.PRIVATE);
  }

  @Test
  public void testSetSpace() {
    SearchIndexElement element = newElement();

    element.setSpace(Space.BOTH);

    Assert.assertEquals(element.getSpace(), Space.BOTH);
  }

  @Test
  public void testInheritedDescriptorPropertiesAreSettable() {
    SearchIndexElement element = newElement();
    Info info = new Info();
    info.addProperty("indexed", Boolean.TRUE);

    element.setInfo(info);

    Assert.assertEquals(element.getInfo().getProperty("indexed"), Boolean.TRUE);
  }

  @Test
  public void testSearchableDataRoundTrip() {
    SearchIndexElement element = newElement();

    element.setSearchableData(stream("the searchable data"));

    Assert.assertEquals(content(element.getSearchableData()), "the searchable data");
  }

  @Test
  public void testSearchableDataIsReadableMoreThanOnce() {
    SearchIndexElement element = newElement();
    element.setSearchableData(stream("the searchable data"));

    Assert.assertEquals(content(element.getSearchableData()), "the searchable data");
    Assert.assertEquals(content(element.getSearchableData()), "the searchable data");
  }

  @Test
  public void testSearchableDataIsNullWhenNotSet() {
    Assert.assertNull(newElement().getSearchableData());
  }

  @Test
  public void testEmptySearchableDataIsReportedAsNull() {
    SearchIndexElement element = newElement();

    element.setSearchableData(stream(""));

    Assert.assertNull(element.getSearchableData());
  }

  @Test
  public void testSettingNullSearchableDataClearsIt() {
    SearchIndexElement element = newElement();
    element.setSearchableData(stream("the searchable data"));

    element.setSearchableData(null);

    Assert.assertNull(element.getSearchableData());
  }

  private SearchIndexElement newElement() {
    return new SearchIndexElement(ITEM_ID, VERSION_ID, new Namespace(), ELEMENT_ID);
  }

  private InputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  private String content(InputStream inputStream) {
    return new String(FileUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
  }
}
