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

import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchCriteria;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchableData;
import io.netty.util.internal.StringUtil;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EsTestUtils {

  public static EsSearchableData createInvalidSearchableData() {
    EsSearchableData searchableData = new EsSearchableData();

    String invalidJsonData = "{\n" +
        "    \"abc\":\"a\"\n" +
        "    \"dfs\":\"b\"\n" +
        "}";
    searchableData.setData(new ByteArrayInputStream(invalidJsonData.getBytes()));
    searchableData.setType("a");
    return searchableData;
  }

  public static EsSearchableData createSearchableData(String type, String name, String message,
                                                      List<String> tags) {
    EsSearchableData searchableData = new EsSearchableData();
    searchableData.setType(type);
    Optional<String> jsonData = getJson(name, message, tags);
    jsonData.ifPresent(s -> searchableData.setData(jsonToInputStream(s)));
    return searchableData;
  }

  public static Optional<String> getJson(String name, String message, List<String> tags) {
    if (Objects.isNull(name) && Objects.isNull(message)
        && (Objects.isNull(tags) || tags.size() == 0)) {
      return Optional.empty();
    }
    StringBuilder stringBuilder = new StringBuilder();
    if (Objects.nonNull(tags)) {
      for (int i = 0; i < tags.size(); i++) {
        String tagVal = tags.get(i);
        if (i > 0) {
          stringBuilder.append(",");
        }
        stringBuilder.append("\"").append(tagVal).append("\"");
      }
    }
    StringBuilder jsonData = new StringBuilder();
    jsonData.append("{\n");
    if (!StringUtil.isNullOrEmpty(name)) {
      jsonData.append("  \"name\":\"").append(name).append("\"");
    }
    if (!StringUtil.isNullOrEmpty(message)) {
      if (jsonData.length() > 0) {
        jsonData.append(",\n");
      }
      jsonData.append("  \"message\":\"").append(message).append("\"");
    }
    if (Objects.nonNull(tags) && tags.size() > 0) {
      if (jsonData.length() > 0) {
        jsonData.append(",\n");
      }
      jsonData.append("  \"tags\": [").append(stringBuilder.toString());
      jsonData.append("]  \n");
    }
    jsonData.append("}");
    return Optional.of(jsonData.toString());
  }

  public static String wrapperTermQuery(String json) {
    return "{\"term\":" + json + "}";
  }


  public static SessionContext createSessionContext(String tenant, String user) {
    SessionContext sessionContext = new SessionContext();
    sessionContext.setTenant(tenant);
    sessionContext.setUser(new UserInfo(user));
    return sessionContext;
  }

  public static EsSearchCriteria createSearchCriteria(List<String> types, Integer fromPage,
                                                      Integer pageSize, String jsonQuery
  ) {
    EsSearchCriteria searchCriteria = new EsSearchCriteria();
    searchCriteria.setTypes(types);
    searchCriteria.setFromPage(fromPage);
    searchCriteria.setPageSize(pageSize);
    if (Objects.nonNull(jsonQuery)) {
      searchCriteria.setQuery(jsonToInputStream(jsonQuery));
    }
    return searchCriteria;
  }

  private static InputStream jsonToInputStream(String json) {
    return new ByteArrayInputStream(json.getBytes());

  }

  public static InputStream objectToInputStrem(Object object) {
    return new ByteArrayInputStream(JsonUtil.object2Json(object).getBytes());
  }

  public static SearchIndexElement createSearchIndexElement(EsSearchableData searchableData,
                                                            Space space, Id itemId, Id versionId,
                                                            Namespace namespace, Id elementId) {
    SearchIndexElement element = new SearchIndexElement(itemId, versionId, namespace, elementId);
    if (Objects.nonNull(searchableData)) {
      element.setSearchableData(EsTestUtils.objectToInputStrem(searchableData));
    }
    element.setSpace(space);
    return element;
  }
}
