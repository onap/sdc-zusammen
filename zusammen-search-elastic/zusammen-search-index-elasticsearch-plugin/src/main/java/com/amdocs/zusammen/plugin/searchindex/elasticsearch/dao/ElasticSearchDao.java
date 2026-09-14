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


package com.amdocs.zusammen.plugin.searchindex.elasticsearch.dao;


import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchCriteria;
import com.amdocs.zusammen.datatypes.SessionContext;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.IndexNotFoundException;

public interface ElasticSearchDao {

  IndexResponse create(SessionContext context, String index, String type, String source, String id);

  GetResponse get(SessionContext sessionContext, String index, String type, String id)
      throws IndexNotFoundException;

  /*
  Update by merging documents
   */
  UpdateResponse update(SessionContext context, String index, String type, String source,
                        String id);

  DeleteResponse delete(SessionContext context, String index, String type, String id);

  SearchResponse search(SessionContext context, String index, EsSearchCriteria searchCriteria);

  ClusterHealthResponse checkHealth(SessionContext context);
}
