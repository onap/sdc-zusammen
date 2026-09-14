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

import com.amdocs.zusammen.commons.health.data.HealthStatus;
import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.searchindex.SearchCriteria;
import com.amdocs.zusammen.datatypes.searchindex.SearchResult;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;

public class SearchIndexEmptyImpl implements com.amdocs.zusammen.sdk.searchindex.SearchIndex {

  @Override
  public Response<HealthInfo> checkHealth(SessionContext sessionContext) {
    HealthInfo healthInfo =new HealthInfo("SEARCH",
            HealthStatus.UP, "Cluster status is Green");
      return new Response<HealthInfo>( healthInfo);
  }

  @Override
  public Response<Void> createElement(SessionContext sessionContext, SearchIndexElement element) {
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Void> updateElement(SessionContext sessionContext, SearchIndexElement element) {
    return new Response(Void.TYPE);
  }

  @Override
  public Response<Void> deleteElement(SessionContext sessionContext, SearchIndexElement element) {
    return new Response(Void.TYPE);
  }

  @Override
  public Response<SearchResult> search(SessionContext sessionContext, SearchCriteria
      searchCriteria) {
    return new Response(new SearchResult() {
    });
  }

}