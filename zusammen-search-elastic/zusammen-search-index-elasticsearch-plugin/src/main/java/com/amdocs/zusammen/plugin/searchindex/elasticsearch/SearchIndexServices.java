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


import io.netty.util.internal.StringUtil;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.searchindex.SearchCriteria;
import com.amdocs.zusammen.datatypes.searchindex.SearchResult;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.dao.ElasticSearchDao;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.dao.ElasticSearchDaoFactory;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsEnrichmentData;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchCriteria;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchResult;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchableData;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;
import com.amdocs.zusammen.utils.fileutils.FileUtils;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.index.IndexNotFoundException;

import java.io.ByteArrayInputStream;
import java.util.Objects;

public class SearchIndexServices {

  private ElasticSearchDao elasticSearchDao;

  public ClusterHealthResponse checkHealth(SessionContext sessionContext){
    return getElasticSearchDao(sessionContext).checkHealth(sessionContext);
  }

  public SearchResult search(SessionContext sessionContext, SearchCriteria searchCriteria) {
    checkSearchCriteriaInstance(searchCriteria);
    String index = getEsIndex(sessionContext);
    EsSearchResult searchResult = new EsSearchResult();

    searchResult.setSearchResponse(getElasticSearchDao(sessionContext)
        .search(sessionContext, index, (EsSearchCriteria) searchCriteria));
    return searchResult;
  }

  ElasticSearchDao getElasticSearchDao(SessionContext context) {
    if (Objects.isNull(elasticSearchDao)) {
      elasticSearchDao = ElasticSearchDaoFactory.getInstance().createInterface(context);
    }
    return elasticSearchDao;
  }

  EsSearchableData getEsSearchableData(SearchIndexElement element) {
    if (Objects.isNull(element.getSearchableData())) {
      EsSearchableData esSearchableData = new EsSearchableData();
      esSearchableData.setType(EsConstants.ELEMENT_DATA_DEFAULT_TYPE);
      return esSearchableData;
    }
    return JsonUtil.json2Object(element.getSearchableData(), EsSearchableData.class);
  }

  String getEsSource(EsSearchableData searchableData) {
    return new String(FileUtils.toByteArray(searchableData.getData()));
  }

  EsSearchableData createEnrichedElasticSearchableData(SessionContext sessionContext,
                                                       SearchIndexElement element) {
    EsSearchableData esSearchableData = getEsSearchableData(element);
    String searchableDataJson = null;
    if (Objects.nonNull(esSearchableData.getData())) {
      searchableDataJson = JsonUtil.inputStream2Json(esSearchableData.getData());
    }
    EsEnrichmentData enrichmentData = createEsEnrichmentData(sessionContext, element);
    String enrichmentDataJson = JsonUtil.object2Json(enrichmentData);

    String enrichedSearchableData =
        getEnrichedSearchableData(searchableDataJson, enrichmentDataJson);

    EsSearchableData enrichmentSearchable = new EsSearchableData();
    enrichmentSearchable.setData(new ByteArrayInputStream(enrichedSearchableData.getBytes()));
    enrichmentSearchable.setType(esSearchableData.getType());
    return enrichmentSearchable;
  }

  private String getEnrichedSearchableData(String searchableDataJson, String enrichmentDataJson) {
    if (Objects.nonNull(searchableDataJson)) {
      searchableDataJson = searchableDataJson.substring(0, searchableDataJson.length() - 1) + ",";
      enrichmentDataJson = enrichmentDataJson.substring(1);
      return searchableDataJson + enrichmentDataJson;
    }
    return enrichmentDataJson;
  }

  String createSearchableDataId(SessionContext sessionContext,
                                SearchIndexElement element) {
    String elasticSpace = getElasticSpaceForCreAndUpd(sessionContext, element.getSpace());
    return elasticSpace + "_" +
        element.getItemId() + "_" +
        element.getVersionId() + "_" +
        element.getId();
  }

  private EsEnrichmentData createEsEnrichmentData(SessionContext sessionContext,
                                                  SearchIndexElement element) {
    EsEnrichmentData enrichmentData = new EsEnrichmentData();
    enrichmentData
        .setSpaceName(getElasticSpaceForCreAndUpd(sessionContext, element.getSpace()));
    enrichmentData.setItemId(element.getItemId());
    enrichmentData.setVersionId(element.getVersionId());
    enrichmentData.setElementId(element.getId());
    enrichmentData.setNamespace(element.getNamespace().getValue());
    enrichmentData.setParentId(element.getParentId());
    enrichmentData.setInfo(element.getInfo());
    enrichmentData.setRelations(element.getRelations());
    return enrichmentData;
  }

  String getElasticSpaceForCreAndUpd(SessionContext sessionContext, Space space) {
    if (space.equals(Space.PRIVATE)) {
      return sessionContext.getUser().getUserName().toLowerCase().replaceAll("\\ ", "");
    } else if (space.equals(Space.PUBLIC)) {
      return Space.PUBLIC.name().toLowerCase();
    } else {
      throw new RuntimeException(
          Space.BOTH.name() + " is invalid Space value for Elastic"
              + " create and update actions");
    }
  }

  String getEsIndex(SessionContext sessionContext) {
    String tenant = sessionContext.getTenant();
    if (StringUtil.isNullOrEmpty(tenant)) {
      throw new RuntimeException("Mandatory Data is missing - Tenant value in session context");
    }
    return tenant.replaceAll("\\ |\\\"|\\*|\\/|\\<|\\||\\|\\,|\\>|\\\\|\\?|\\,", "").toLowerCase();
  }


  void validation(SearchIndexElement element) {
    if (Objects.isNull(element)) {
      throw new RuntimeException(
          "Mandatory Data is missing - Element searchable data");
    }
    StringBuffer errorMsg = new StringBuffer();
    dataValidation(element, errorMsg);
    if (Objects.isNull(element.getSpace())) {
      errorMsg.append("Mandatory Data is missing - Space").append("\n");
    }
    if (Objects.isNull(element.getItemId())) {
      errorMsg.append("Mandatory Data is missing - Item Id").append("\n");
    }
    if (Objects.isNull(element.getVersionId())) {
      errorMsg.append("Mandatory Data is missing - Version Id").append("\n");
    }
    if (Objects.isNull(element.getId())) {
      errorMsg.append("Mandatory Data is missing - Element Id").append("\n");
    }

    if (errorMsg.length() > 0) {
      throw new RuntimeException(errorMsg.toString());
    }
  }

  private void dataValidation(SearchIndexElement element, StringBuffer errorMsg) {
    if (Objects.nonNull(element.getSearchableData())) {
      try {
        getEsSearchableData(element);
      } catch (Exception exc) {
        errorMsg.append("Invalid searchableData, EsSearchableData object is expected").append("\n");
      }
    }
  }

  void checkIfSearchableDataExist(SessionContext sessionContext, String index,
                                  String type, String id)
      throws IndexNotFoundException {
    GetResponse getResponse =
        getElasticSearchDao(sessionContext).get(sessionContext, index, type, id);
    if (!getResponse.isExists()) {
      String notFoundErrorMsg = "Searchable data for tenant - '" + sessionContext.getTenant()
          + "', type - '" + type + "' and id - '" + id + "' was not found.";
      throw new RuntimeException(notFoundErrorMsg);
    }
  }

  void checkSearchCriteriaInstance(SearchCriteria searchCriteria) {
    if (!(searchCriteria instanceof EsSearchCriteria)) {
      throw new RuntimeException(
          "Invalid SearchCriteria, EsSearchCriteria object is expected");
    }
  }
}
