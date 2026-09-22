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

package com.amdocs.zusammen.adaptor.inbound.impl.convertor;

import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.info;
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.itemVersionChange;
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.stream;
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.text;

import com.amdocs.zusammen.adaptor.inbound.api.types.item.Element;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.MergeChange;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreMergeChange;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MergeChangeConvertorTest {

    @Test
    public void testConvertCopiesChangedVersionAndChangedElementsInOrder() {
        ItemVersionChange changedVersion = itemVersionChange("version-id", Action.UPDATE);

        CoreElement firstChanged = new CoreElement();
        firstChanged.setId(new Id("first-changed-id"));
        firstChanged.setAction(Action.CREATE);
        firstChanged.setInfo(info("first-changed"));
        firstChanged.setData(stream("first-changed-data"));

        CoreElement secondChanged = new CoreElement();
        secondChanged.setId(new Id("second-changed-id"));
        secondChanged.setAction(Action.DELETE);
        secondChanged.setInfo(info("second-changed"));

        CoreMergeChange coreMergeChange = new CoreMergeChange();
        coreMergeChange.setChangedVersion(changedVersion);
        coreMergeChange.setChangedElements(Arrays.asList(firstChanged, secondChanged));

        MergeChange result = MergeChangeConvertor.convert(coreMergeChange);

        Assert.assertSame(result.getChangedVersion(), changedVersion);
        Assert.assertEquals(result.getChangedVersion().getAction(), Action.UPDATE);
        Assert.assertEquals(result.getChangedVersion().getItemVersion().getId(),
            new Id("version-id"));

        List<Element> changedElements = new ArrayList<>(result.getChangedElements());
        Assert.assertEquals(changedElements.size(), 2);
        Assert.assertEquals(changedElements.get(0).getElementId(), new Id("first-changed-id"));
        Assert.assertEquals(changedElements.get(0).getAction(), Action.CREATE);
        Assert.assertEquals(changedElements.get(0).getInfo().getName(), "first-changed");
        Assert.assertEquals(text(changedElements.get(0).getData()), "first-changed-data");
        Assert.assertEquals(changedElements.get(1).getElementId(), new Id("second-changed-id"));
        Assert.assertEquals(changedElements.get(1).getAction(), Action.DELETE);
        Assert.assertEquals(changedElements.get(1).getInfo().getName(), "second-changed");
    }

    @Test
    public void testConvertReturnsNullForNullCoreMergeChange() {
        Assert.assertNull(MergeChangeConvertor.convert(null));
    }

    @Test
    public void testConvertYieldsEmptyChangedElementsForEmptyCoreChangedElements() {
        CoreMergeChange coreMergeChange = new CoreMergeChange();
        coreMergeChange.setChangedElements(new ArrayList<CoreElement>());

        MergeChange result = MergeChangeConvertor.convert(coreMergeChange);

        Assert.assertTrue(result.getChangedElements().isEmpty());
        Assert.assertNull(result.getChangedVersion());
    }
}
