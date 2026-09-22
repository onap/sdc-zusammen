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

package com.amdocs.zusammen.adaptor.outbound.impl.convertor;

import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.info;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.itemVersionChange;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.namespace;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.versionDataConflict;

import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeResult;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollaborationMergeResultConvertorTest {

    @Test
    public void testConvertCopiesChangeAndConflict() {
        ItemVersionChange changedVersion = itemVersionChange("version-id", Action.UPDATE);

        CollaborationElementChange elementChange = new CollaborationElementChange();
        elementChange.setElement(element("changed-id"));
        elementChange.setAction(Action.CREATE);

        CollaborationMergeChange change = new CollaborationMergeChange();
        change.setChangedVersion(changedVersion);
        change.setChangedElements(Collections.singletonList(elementChange));

        CollaborationElementConflict elementConflict = new CollaborationElementConflict();
        elementConflict.setLocalElement(element("conflicting-local-id"));
        elementConflict.setRemoteElement(element("conflicting-remote-id"));

        ItemVersionDataConflict dataConflict = versionDataConflict("local-version", "remote-version");
        CollaborationMergeConflict conflict = new CollaborationMergeConflict();
        conflict.setVersionDataConflict(dataConflict);
        conflict.setElementConflicts(Collections.singletonList(elementConflict));

        CollaborationMergeResult source = new CollaborationMergeResult();
        source.setChange(change);
        source.setConflict(conflict);

        CoreMergeResult result = CollaborationMergeResultConvertor.convert(source);

        Assert.assertSame(result.getChange().getChangedVersion(), changedVersion);
        List<CoreElement> changedElements = new ArrayList<>(result.getChange().getChangedElements());
        Assert.assertEquals(changedElements.size(), 1);
        Assert.assertEquals(changedElements.get(0).getId(), new Id("changed-id"));
        Assert.assertEquals(changedElements.get(0).getAction(), Action.CREATE);

        Assert.assertSame(result.getConflict().getVersionDataConflict(), dataConflict);
        List<CoreElementConflict> elementConflicts =
            new ArrayList<>(result.getConflict().getElementConflicts());
        Assert.assertEquals(elementConflicts.size(), 1);
        Assert.assertEquals(elementConflicts.get(0).getLocalElement().getId(),
            new Id("conflicting-local-id"));
        Assert.assertEquals(elementConflicts.get(0).getRemoteElement().getId(),
            new Id("conflicting-remote-id"));
    }

    @Test
    public void testConvertKeepsMissingChangeAndConflictNull() {
        CoreMergeResult result =
            CollaborationMergeResultConvertor.convert(new CollaborationMergeResult());

        Assert.assertNull(result.getChange());
        Assert.assertNull(result.getConflict());
    }

    private static CollaborationElement element(String elementId) {
        CollaborationElement element = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), namespace("root-id"), new Id(elementId));
        element.setInfo(info(elementId));
        return element;
    }
}
