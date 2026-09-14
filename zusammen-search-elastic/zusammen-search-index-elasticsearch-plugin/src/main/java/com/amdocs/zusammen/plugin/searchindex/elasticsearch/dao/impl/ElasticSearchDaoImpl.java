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

package com.amdocs.zusammen.plugin.searchindex.elasticsearch.dao.impl;

import com.amdocs.zusammen.plugin.searchindex.elasticsearch.EsClientServices;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.dao.ElasticSearchDao;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchCriteria;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Objects;

public class ElasticSearchDaoImpl implements ElasticSearchDao {

  private EsClientServices esClientServices = new EsClientServices();

  @Override
  public IndexResponse create(SessionContext context, String index, String type,
                              String source, String id) {
    return getTransportClient(context).prepareIndex(index, type, id).setSource(source).get();
  }

  @Override
  public GetResponse get(SessionContext context, String index, String type,
                         String id) throws IndexNotFoundException {
    return getTransportClient(context).prepareGet(index, type, id).get();
  }

  //Update by merging documents
  @Override
  public UpdateResponse update(SessionContext context, String index, String type,
                               String source, String id) {
    return getTransportClient(context).prepareUpdate(index, type, id).setDoc(source).get();
  }


  @Override
  public DeleteResponse delete(SessionContext context, String index, String type,
                               String id) {
    return getTransportClient(context).prepareDelete(index, type, id).get();
  }


  @Override
  public ClusterHealthResponse checkHealth(SessionContext context) {
    return getTransportClient(context).admin().cluster().health(new ClusterHealthRequest()).actionGet();
  }

  @Override
  public SearchResponse search(SessionContext context, String index,
                               EsSearchCriteria searchCriteria) {
    SearchRequestBuilder searchRequestBuilder = getTransportClient(context).prepareSearch(index);
    searchRequestBuilder.setSearchType(SearchType.DEFAULT);
    searchRequestBuilder.setExplain(false);

    if (Objects.nonNull(searchCriteria.getQuery())) {
      searchRequestBuilder.setQuery(
          QueryBuilders.wrapperQuery(JsonUtil.inputStream2Json(searchCriteria.getQuery())));
    }
    if (Objects.nonNull(searchCriteria.getFromPage())) {
      searchRequestBuilder.setFrom(searchCriteria.getFromPage());
    }
    if (Objects.nonNull(searchCriteria.getPageSize())) {
      searchRequestBuilder.setSize(searchCriteria.getPageSize());
    }

    if (Objects.nonNull(searchCriteria.getTypes()) && searchCriteria.getTypes().size() > 0) {
      searchRequestBuilder.setTypes(searchCriteria.getTypes().toArray(new String[searchCriteria
          .getTypes().size()]));
    }

    return searchRequestBuilder.get();
  }

  private TransportClient getTransportClient(SessionContext context) {
    return esClientServices.start(context);
  }
}
