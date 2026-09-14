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


import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchableData;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.index.IndexNotFoundException;

public class ElementSearchIndex extends SearchIndexServices {

  public void createElement(SessionContext sessionContext, SearchIndexElement element) {
    validation(element);
    EsSearchableData enrichedSearchableData =
        createEnrichedElasticSearchableData(sessionContext, element);
    String index = getEsIndex(sessionContext);
    String source = getEsSource(enrichedSearchableData);
    String searchableDataId = createSearchableDataId(sessionContext, element);

    getElasticSearchDao(sessionContext)
        .create(sessionContext, index, enrichedSearchableData.getType(), source, searchableDataId);
  }

  public void updateElement(SessionContext sessionContext, SearchIndexElement element) {
    try {
      validation(element);
      String searchableDataId = createSearchableDataId(sessionContext, element);
      EsSearchableData esSearchableData = getEsSearchableData(element);

      checkIfSearchableDataExist(sessionContext, getEsIndex(sessionContext),
          esSearchableData.getType(), searchableDataId);
      createElement(sessionContext, element);

    } catch (IndexNotFoundException indexNotFoundExc) {
      String missingIndex = "Searchable data for tenant - '" + sessionContext.getTenant()
          + "' was not found.";
      throw new RuntimeException(missingIndex);
    } catch (Exception exc) {
      throw new RuntimeException(exc);
    }
  }

  public void deleteElement(SessionContext sessionContext, SearchIndexElement element) {
    validation(element);
    String searchableDataId = createSearchableDataId(sessionContext, element);
    String index = getEsIndex(sessionContext);
    EsSearchableData esSearchableData = getEsSearchableData(element);

    DeleteResponse response = getElasticSearchDao(sessionContext)
        .delete(sessionContext, index, esSearchableData.getType(), searchableDataId);

    if (response.getResult().getLowercase().equals("not_found")) {
      String notFoundErrorMsg = "Delete failed - Searchable data for tenant - '" +
          sessionContext.getTenant() + "', type - '" + esSearchableData.getType() + "' and id - '"
          + searchableDataId + "' was not found.";
      throw new RuntimeException(notFoundErrorMsg);
    }
  }
}
