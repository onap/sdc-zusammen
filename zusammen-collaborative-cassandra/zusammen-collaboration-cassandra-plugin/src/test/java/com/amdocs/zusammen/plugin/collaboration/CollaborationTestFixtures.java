/*
 * Copyright © 2026 Deutsche Telekom AG
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

package com.amdocs.zusammen.plugin.collaboration;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builders shared by the service tests in this package. {@link TestUtils} is owned by the
 * pre-existing tests and is left untouched.
 */
final class CollaborationTestFixtures {

  private CollaborationTestFixtures() {
  }

  static ElementEntity element(Id id, Id parentId, String content, Id... subElementIds) {
    ElementEntity element = new ElementEntity(id);
    element.setParentId(parentId);
    element.setNamespace(new Namespace());
    element.setInfo(TestUtils.createInfo(content));
    element.setData(ByteBuffer.wrap(content.getBytes()));
    element.setSearchableData(ByteBuffer.wrap(("searchable_" + content).getBytes()));
    element.setVisualization(ByteBuffer.wrap(("visual_" + content).getBytes()));
    element.setElementHash(new Id("hash_" + content));
    element.setSubElementIds(idSet(subElementIds));
    return element;
  }

  static Set<Id> idSet(Id... ids) {
    return new LinkedHashSet<>(Arrays.asList(ids));
  }

  static SynchronizationStateEntity syncState(Id id, Id revisionId, Date publishTime,
                                              boolean dirty) {
    return new SynchronizationStateEntity(id, revisionId, publishTime, dirty);
  }

  /**
   * The production code iterates the collections these mocks return, and several decisions depend
   * on which element is visited first, so the fixtures must preserve insertion order.
   */
  static Collection<SynchronizationStateEntity> syncStates(SynchronizationStateEntity... states) {
    return new LinkedHashSet<>(Arrays.asList(states));
  }
}
