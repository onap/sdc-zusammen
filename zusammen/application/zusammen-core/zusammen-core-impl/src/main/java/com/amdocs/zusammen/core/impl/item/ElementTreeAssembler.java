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

package com.amdocs.zusammen.core.impl.item;

import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.datatypes.Id;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ElementTreeAssembler {

  private ElementTreeAssembler() {
  }

  /**
   * Links a flat subtree read into a tree rooted at {@code rootId}: each id-only stub child is
   * replaced by the element read for it. An element is attached at most once, so a sub-element id
   * pointing back up the tree stays a stub instead of closing a cycle that the recursive convertors
   * would never leave.
   */
  static CoreElement assemble(Id rootId, Collection<CoreElement> flat) {
    Map<Id, CoreElement> byId = new LinkedHashMap<>();
    flat.forEach(element -> byId.putIfAbsent(element.getId(), element));
    CoreElement root = byId.get(rootId);
    if (root == null) {
      return null;
    }
    Set<Id> attached = new HashSet<>();
    attached.add(rootId);
    List<CoreElement> level = new ArrayList<>();
    level.add(root);
    while (!level.isEmpty()) {
      List<CoreElement> next = new ArrayList<>();
      for (CoreElement parent : level) {
        if (parent.getSubElements() == null) {
          continue;
        }
        List<CoreElement> children = new ArrayList<>(parent.getSubElements().size());
        for (CoreElement stub : parent.getSubElements()) {
          CoreElement loaded = byId.get(stub.getId());
          if (loaded != null && attached.add(stub.getId())) {
            children.add(loaded);
            next.add(loaded);
          } else {
            children.add(stub);
          }
        }
        parent.setSubElements(children);
      }
      level = next;
    }
    return root;
  }
}
