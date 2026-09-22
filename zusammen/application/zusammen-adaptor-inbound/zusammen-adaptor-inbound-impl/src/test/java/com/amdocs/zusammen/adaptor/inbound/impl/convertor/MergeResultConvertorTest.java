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
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.versionDataConflict;

import com.amdocs.zusammen.adaptor.inbound.api.types.item.Element;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementConflict;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.MergeResult;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeChange;
import com.amdocs.zusammen.core.api.types.CoreMergeConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeResult;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MergeResultConvertorTest {

    @Test
    public void testGetMergeResultCopiesChangeAndConflict() {
        ItemVersionChange changedVersion = itemVersionChange("version-id", Action.CREATE);
        CoreElement changedElement = new CoreElement();
        changedElement.setId(new Id("changed-id"));
        changedElement.setInfo(info("changed"));

        CoreMergeChange coreMergeChange = new CoreMergeChange();
        coreMergeChange.setChangedVersion(changedVersion);
        coreMergeChange.setChangedElements(Collections.singletonList(changedElement));

        CoreElement conflictingLocal = new CoreElement();
        conflictingLocal.setId(new Id("conflicting-local-id"));
        CoreElement conflictingRemote = new CoreElement();
        conflictingRemote.setId(new Id("conflicting-remote-id"));
        CoreElementConflict coreElementConflict = new CoreElementConflict();
        coreElementConflict.setLocalElement(conflictingLocal);
        coreElementConflict.setRemoteElement(conflictingRemote);

        ItemVersionDataConflict dataConflict = versionDataConflict("local-version", "remote-version");
        CoreMergeConflict coreMergeConflict = new CoreMergeConflict();
        coreMergeConflict.setVersionDataConflict(dataConflict);
        coreMergeConflict.setElementConflicts(Collections.singletonList(coreElementConflict));

        CoreMergeResult coreMergeResult = new CoreMergeResult();
        coreMergeResult.setChange(coreMergeChange);
        coreMergeResult.setConflict(coreMergeConflict);

        MergeResult result = MergeResultConvertor.getMergeResult(coreMergeResult);

        Assert.assertSame(result.getChange().getChangedVersion(), changedVersion);
        List<Element> changedElements = new ArrayList<>(result.getChange().getChangedElements());
        Assert.assertEquals(changedElements.size(), 1);
        Assert.assertEquals(changedElements.get(0).getElementId(), new Id("changed-id"));
        Assert.assertEquals(changedElements.get(0).getInfo().getName(), "changed");

        Assert.assertSame(result.getConflict().getVersionDataConflict(), dataConflict);
        List<ElementConflict> elementConflicts =
            new ArrayList<>(result.getConflict().getElementConflicts());
        Assert.assertEquals(elementConflicts.size(), 1);
        Assert.assertEquals(elementConflicts.get(0).getLocalElement().getElementId(),
            new Id("conflicting-local-id"));
        Assert.assertEquals(elementConflicts.get(0).getRemoteElement().getElementId(),
            new Id("conflicting-remote-id"));
    }

    @Test
    public void testGetMergeResultKeepsMissingChangeAndConflictNull() {
        MergeResult result = MergeResultConvertor.getMergeResult(new CoreMergeResult());

        Assert.assertNull(result.getChange());
        Assert.assertNull(result.getConflict());
    }
}
