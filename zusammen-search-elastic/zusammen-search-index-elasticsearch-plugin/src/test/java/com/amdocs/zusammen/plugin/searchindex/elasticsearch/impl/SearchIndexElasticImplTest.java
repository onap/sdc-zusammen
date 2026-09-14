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


package com.amdocs.zusammen.plugin.searchindex.elasticsearch.impl;

import com.amdocs.zusammen.plugin.searchindex.elasticsearch.ElementSearchIndex;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.SearchIndexServices;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchResult;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyObject;

public class SearchIndexElasticImplTest {

  @Mock
  ElementSearchIndex elementSearchIndexMock;
  @Mock
  SearchIndexServices searchIndexServicesMock;
  @InjectMocks
  SearchIndexElasticImpl searchIndexElasticImpl;


  @BeforeMethod(alwaysRun = true)
  public void injectDoubles() {
    MockitoAnnotations.initMocks(this);
    Mockito.doNothing().when(elementSearchIndexMock).createElement(anyObject(), anyObject());
    Mockito.doNothing().when(elementSearchIndexMock).updateElement(anyObject(), anyObject());
    Mockito.doNothing().when(elementSearchIndexMock).deleteElement(anyObject(), anyObject());
    Mockito.when(searchIndexServicesMock.search(anyObject(), anyObject())).thenReturn(new EsSearchResult());
  }

  @Test
  public void testCreateElement() throws Exception {
    searchIndexElasticImpl.createElement(null, null);
  }

  @Test
  public void testUpdateElement() throws Exception {
    searchIndexElasticImpl.updateElement(null, null);
  }

  @Test
  public void testDeleteElement() throws Exception {
    searchIndexElasticImpl.deleteElement(null, null);
  }

  @Test
  public void testSearch() throws Exception {
    searchIndexElasticImpl.search(null, null);
  }

}