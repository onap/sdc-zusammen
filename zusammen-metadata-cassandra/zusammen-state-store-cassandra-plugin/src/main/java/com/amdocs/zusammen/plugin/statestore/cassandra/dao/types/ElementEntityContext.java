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

package com.amdocs.zusammen.plugin.statestore.cassandra.dao.types;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.ElementContext;

public class ElementEntityContext {
  private String space;
  private Id itemId;
  private Id versionId;
  private Id revisionId;

  public ElementEntityContext(String space, ElementContext elementContext) {
    this(space, elementContext.getItemId(), elementContext.getVersionId());
    this.setRevisionId(elementContext.getRevisionId());
  }

  public ElementEntityContext(String space, Id itemId, Id versionId) {
    this.space = space;
    this.itemId = itemId;
    this.versionId = versionId;
  }

  public ElementEntityContext(String space, Id itemId, Id versionId,Id revisionId) {
   this(space,itemId,versionId);
    this.revisionId = revisionId;
  }

  public String getSpace() {
    return space;
  }

  public void setSpace(String space) {
    this.space = space;
  }

  public Id getItemId() {
    return itemId;
  }

  public void setItemId(Id itemId) {
    this.itemId = itemId;
  }

  public Id getVersionId() {
    return versionId;
  }

  public void setVersionId(Id versionId) {
    this.versionId = versionId;
  }


  public Id getRevisionId() {
    return revisionId;
  }

  public void setRevisionId(Id revisionId) {
    this.revisionId = revisionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ElementEntityContext that = (ElementEntityContext) o;

    if (space != null ? !space.equals(that.space) : that.space != null) {
      return false;
    }
    if (itemId != null ? !itemId.equals(that.itemId) : that.itemId != null) {
      return false;
    }
    return versionId != null ? versionId.equals(that.versionId) : that.versionId == null;
  }

  @Override
  public int hashCode() {
    int result = space != null ? space.hashCode() : 0;
    result = 31 * result + (itemId != null ? itemId.hashCode() : 0);
    result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
    return result;
  }
}
