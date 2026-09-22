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
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.stream;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.text;

import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreMergeChange;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeChange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollaborationMergeChangeConvertorTest {

    @Test
    public void testConvertToCoreMergeChangeCopiesChangedVersionAndElementsInOrder() {
        ItemVersionChange changedVersion = itemVersionChange("version-id", Action.UPDATE);

        CollaborationMergeChange source = new CollaborationMergeChange();
        source.setChangedVersion(changedVersion);
        source.setChangedElements(Arrays.asList(
            elementChange("first-changed-id", Action.CREATE),
            elementChange("second-changed-id", Action.DELETE)));

        CoreMergeChange result =
            CollaborationMergeChangeConvertor.convertToCoreMergeChange(source);

        Assert.assertSame(result.getChangedVersion(), changedVersion);
        Assert.assertEquals(result.getChangedVersion().getAction(), Action.UPDATE);
        Assert.assertEquals(result.getChangedVersion().getItemVersion().getId(),
            new Id("version-id"));

        List<CoreElement> changedElements = new ArrayList<>(result.getChangedElements());
        Assert.assertEquals(changedElements.size(), 2);
        Assert.assertEquals(changedElements.get(0).getId(), new Id("first-changed-id"));
        Assert.assertEquals(changedElements.get(0).getAction(), Action.CREATE);
        Assert.assertEquals(changedElements.get(0).getInfo().getName(), "first-changed-id");
        Assert.assertEquals(changedElements.get(0).getNamespace(), namespace("root-id"));
        Assert.assertEquals(text(changedElements.get(0).getData()), "first-changed-id-data");
        Assert.assertEquals(changedElements.get(1).getId(), new Id("second-changed-id"));
        Assert.assertEquals(changedElements.get(1).getAction(), Action.DELETE);
        Assert.assertEquals(changedElements.get(1).getInfo().getName(), "second-changed-id");
        Assert.assertEquals(text(changedElements.get(1).getData()), "second-changed-id-data");
    }

    @Test
    public void testConvertToCoreMergeChangeReturnsNullForNullCollaborationMergeChange() {
        Assert.assertNull(CollaborationMergeChangeConvertor.convertToCoreMergeChange(null));
    }

    @Test
    public void testConvertToCoreMergeChangeYieldsEmptyChangedElementsWhenThereAreNone() {
        CoreMergeChange result = CollaborationMergeChangeConvertor
            .convertToCoreMergeChange(new CollaborationMergeChange());

        Assert.assertTrue(result.getChangedElements().isEmpty());
        Assert.assertNull(result.getChangedVersion());
    }

    private static CollaborationElementChange elementChange(String elementId, Action action) {
        CollaborationElement element = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), namespace("root-id"), new Id(elementId));
        element.setInfo(info(elementId));
        element.setData(stream(elementId + "-data"));

        CollaborationElementChange change = new CollaborationElementChange();
        change.setElement(element);
        change.setAction(action);
        return change;
    }
}
