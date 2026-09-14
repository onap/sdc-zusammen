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


package com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes;

import com.amdocs.zusammen.datatypes.searchindex.SearchCriteria;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EsSearchCriteria implements SearchCriteria {

  private List<String> types;
  private Integer fromPage;
  private Integer pageSize;
  private InputStream query;

  public List<String> getTypes() {
    return types;
  }

  public void setTypes(List<String> types) {
    this.types = types;
  }

  public void addType(String type) {
    if (Objects.isNull(types)) {
      types = new ArrayList<>();
    }
    types.add(type);
  }

  public Integer getFromPage() {
    return fromPage;
  }

  public void setFromPage(Integer fromPage) {
    this.fromPage = fromPage;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public InputStream getQuery() {
    return query;
  }

  public void setQuery(InputStream query) {
    this.query = query;
  }

}
