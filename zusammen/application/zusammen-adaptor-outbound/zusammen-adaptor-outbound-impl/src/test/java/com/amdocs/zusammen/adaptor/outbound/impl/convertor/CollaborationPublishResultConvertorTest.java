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
import com.amdocs.zusammen.core.api.types.CorePublishResult;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationPublishResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollaborationPublishResultConvertorTest {

    @Test
    public void testConvertCopiesPublishedChange() {
        ItemVersionChange changedVersion = itemVersionChange("version-id", Action.UPDATE);

        CollaborationElement element = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), namespace("root-id"), new Id("published-id"));
        element.setInfo(info("published"));
        element.setData(stream("published-data"));

        CollaborationElementChange elementChange = new CollaborationElementChange();
        elementChange.setElement(element);
        elementChange.setAction(Action.CREATE);

        CollaborationMergeChange change = new CollaborationMergeChange();
        change.setChangedVersion(changedVersion);
        change.setChangedElements(Collections.singletonList(elementChange));

        CollaborationPublishResult source = new CollaborationPublishResult();
        source.setChange(change);

        CorePublishResult result = CollaborationPublishResultConvertor.convert(source);

        Assert.assertSame(result.getChange().getChangedVersion(), changedVersion);
        List<CoreElement> changedElements = new ArrayList<>(result.getChange().getChangedElements());
        Assert.assertEquals(changedElements.size(), 1);
        Assert.assertEquals(changedElements.get(0).getId(), new Id("published-id"));
        Assert.assertEquals(changedElements.get(0).getAction(), Action.CREATE);
        Assert.assertEquals(changedElements.get(0).getInfo().getName(), "published");
        Assert.assertEquals(text(changedElements.get(0).getData()), "published-data");
    }

    @Test
    public void testConvertKeepsMissingChangeNull() {
        CorePublishResult result =
            CollaborationPublishResultConvertor.convert(new CollaborationPublishResult());

        Assert.assertNull(result.getChange());
    }
}
