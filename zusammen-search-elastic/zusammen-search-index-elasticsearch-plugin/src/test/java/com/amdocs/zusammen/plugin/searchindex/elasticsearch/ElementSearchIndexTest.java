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


package com.amdocs.zusammen.plugin.searchindex.elasticsearch;


import com.amdocs.zusammen.plugin.searchindex.elasticsearch.dao.ElasticSearchDao;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchableData;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.index.IndexNotFoundException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;

public class ElementSearchIndexTest {

  @Mock
  private ElasticSearchDao elasticSearchDaoMock;
  @Mock
  private GetResponse getResponseMock;
  @Mock
  private DeleteResponse deleteResponseMock;
  @InjectMocks
  private ElementSearchIndex elementSearchIndex;

  @BeforeMethod(alwaysRun = true)
  public void injectDoubles() {
    MockitoAnnotations.initMocks(this);

    Mockito.when(elasticSearchDaoMock
        .create(anyObject(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new IndexResponse());
    Mockito.when(elasticSearchDaoMock.get(anyObject(), anyString(), anyString(), anyString()))
        .thenReturn(getResponseMock);
    Mockito.when(elasticSearchDaoMock.delete(anyObject(), anyString(), anyString(), anyString()))
        .thenReturn(deleteResponseMock);
  }

  private Id itemId = null;
  private Id versionId = null;
  private Id elementId = null;
  private String type;
  private String tenant;
  private String user;
  private String createName;
  private String updateName;


  private void initSearchData() {
    tenant = "elementsearchindextest";
    type = "type1";
    user = "user";
    createName = "createName";
    updateName = "updateName";
    if (Objects.isNull(itemId)) {
      itemId = new Id();
      versionId = new Id();
      elementId = new Id();
    }

  }

  @Test
  public void testCreateElement() throws Exception {
    initSearchData();

    String message = "create es data test";
    List<String> tags = new ArrayList<>();
    tags.add("a");
    tags.add("b");
    tags.add("c");

    SessionContext sessionContext = EsTestUtils.createSessionContext(tenant, user);
    EsSearchableData searchableData =
        EsTestUtils.createSearchableData(type, createName, message, tags);
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(searchableData, Space.PRIVATE, itemId, versionId,
            Namespace.ROOT_NAMESPACE, elementId
        );
    elementSearchIndex.createElement(sessionContext, element);
  }

  @Test
  public void testUpdateElement() throws Exception {
    initSearchData();
    List<String> tags = new ArrayList<>();
    tags.add("a");
    tags.add("b");

    SessionContext sessionContext = EsTestUtils.createSessionContext(tenant, user);
    EsSearchableData searchableData =
        EsTestUtils.createSearchableData(type, updateName, null, tags);
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(searchableData, Space.PRIVATE, itemId, versionId,
            Namespace.ROOT_NAMESPACE, elementId
        );
    Mockito.when(getResponseMock.isExists()).thenReturn(true);
    elementSearchIndex.updateElement(sessionContext, element);

  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "Searchable data for tenant - 'elementsearchindextest' "
          + "was not found.")
  public void testUpdateIndexNotFound() throws Exception {
    initSearchData();
    List<String> tags = new ArrayList<>();
    tags.add("a");
    tags.add("b");

    SessionContext sessionContext = EsTestUtils.createSessionContext(tenant, user);
    EsSearchableData searchableData =
        EsTestUtils.createSearchableData(type, updateName, null, tags);
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(searchableData, Space.PRIVATE, itemId, versionId,
            Namespace.ROOT_NAMESPACE, elementId
        );

    Mockito.when(elasticSearchDaoMock.get(anyObject(), anyString(), anyString(), anyString()))
        .thenThrow(new IndexNotFoundException("indexnotfound"));
    elementSearchIndex.updateElement(sessionContext, element);

  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = ".*Searchable data for tenant - " +
          "'elementsearchindextest', type - 'type1' and id - .* was not found.")
  public void testUpdateIdNotExist() throws Exception {
    initSearchData();
    List<String> tags = new ArrayList<>();
    tags.add("a");
    tags.add("b");

    SessionContext sessionContext = EsTestUtils.createSessionContext(tenant, user);
    EsSearchableData searchableData =
        EsTestUtils.createSearchableData(type, updateName, null, tags);
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(searchableData, Space.PRIVATE, new Id(), versionId,
            Namespace.ROOT_NAMESPACE, elementId
        );
    Mockito.when(getResponseMock.isExists()).thenReturn(false);
    elementSearchIndex.updateElement(sessionContext, element);
  }

  @Test
  public void testDeleteElement() throws Exception {
    initSearchData();

    SessionContext sessionContext = EsTestUtils.createSessionContext(tenant, user);
    EsSearchableData searchableData =
        EsTestUtils.createSearchableData(type, null, null, null);
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(searchableData, Space.PRIVATE, itemId, versionId,
            Namespace.ROOT_NAMESPACE, elementId
        );

    Mockito.when(deleteResponseMock.getResult()).thenReturn(DocWriteResponse.Result.UPDATED);
    elementSearchIndex.deleteElement(sessionContext, element);
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "Delete failed - Searchable data for tenant - "
          + "'elementsearchindextest', type - 'type1' and id - .* was not found.")
  public void testDeleteNotFound() throws Exception {
    initSearchData();

    SessionContext sessionContext = EsTestUtils.createSessionContext(tenant, user);
    EsSearchableData searchableData =
        EsTestUtils.createSearchableData(type, null, null, null);
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(searchableData, Space.PRIVATE, new Id(), versionId,
            Namespace.ROOT_NAMESPACE, elementId
        );

    Mockito.when(deleteResponseMock.getResult()).thenReturn(DocWriteResponse.Result.NOT_FOUND);
    elementSearchIndex.deleteElement(sessionContext, element);
  }
}


