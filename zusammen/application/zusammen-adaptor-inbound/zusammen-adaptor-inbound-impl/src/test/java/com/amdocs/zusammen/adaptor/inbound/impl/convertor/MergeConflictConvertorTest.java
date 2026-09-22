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
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.stream;
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.text;
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.versionDataConflict;

import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementConflict;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.MergeConflict;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeConflict;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MergeConflictConvertorTest {

    @Test
    public void testConvertCopiesVersionDataConflictAndElementConflictsInOrder() {
        ItemVersionDataConflict dataConflict = versionDataConflict("local-version", "remote-version");

        CoreMergeConflict coreMergeConflict = new CoreMergeConflict();
        coreMergeConflict.setVersionDataConflict(dataConflict);
        coreMergeConflict.setElementConflicts(Arrays.asList(
            elementConflict("first-local", "first-remote"),
            elementConflict("second-local", "second-remote")));

        MergeConflict result = MergeConflictConvertor.convert(coreMergeConflict);

        Assert.assertSame(result.getVersionDataConflict(), dataConflict);

        List<ElementConflict> elementConflicts = new ArrayList<>(result.getElementConflicts());
        Assert.assertEquals(elementConflicts.size(), 2);
        Assert.assertEquals(elementConflicts.get(0).getLocalElement().getElementId(),
            new Id("first-local"));
        Assert.assertEquals(text(elementConflicts.get(0).getLocalElement().getData()),
            "first-local-data");
        Assert.assertEquals(elementConflicts.get(0).getRemoteElement().getElementId(),
            new Id("first-remote"));
        Assert.assertEquals(elementConflicts.get(1).getLocalElement().getElementId(),
            new Id("second-local"));
        Assert.assertEquals(elementConflicts.get(1).getRemoteElement().getElementId(),
            new Id("second-remote"));
        Assert.assertEquals(elementConflicts.get(1).getRemoteElement().getInfo().getName(),
            "second-remote");
    }

    @Test
    public void testConvertReturnsNullForNullCoreMergeConflict() {
        Assert.assertNull(MergeConflictConvertor.convert(null));
    }

    @Test
    public void testConvertYieldsEmptyElementConflictsForEmptyCoreElementConflicts() {
        CoreMergeConflict coreMergeConflict = new CoreMergeConflict();
        coreMergeConflict.setElementConflicts(new ArrayList<CoreElementConflict>());

        MergeConflict result = MergeConflictConvertor.convert(coreMergeConflict);

        Assert.assertTrue(result.getElementConflicts().isEmpty());
        Assert.assertNull(result.getVersionDataConflict());
    }

    @Test
    public void testConvertKeepsMissingConflictSidesNull() {
        CoreElement local = new CoreElement();
        local.setId(new Id("local-id"));

        CoreElementConflict localOnly = new CoreElementConflict();
        localOnly.setLocalElement(local);

        CoreMergeConflict coreMergeConflict = new CoreMergeConflict();
        coreMergeConflict.setElementConflicts(Collections.singletonList(localOnly));

        List<ElementConflict> elementConflicts =
            new ArrayList<>(MergeConflictConvertor.convert(coreMergeConflict)
                .getElementConflicts());

        Assert.assertEquals(elementConflicts.size(), 1);
        Assert.assertEquals(elementConflicts.get(0).getLocalElement().getElementId(),
            new Id("local-id"));
        Assert.assertNull(elementConflicts.get(0).getRemoteElement());
    }

    @Test
    public void testConvertMapsNullElementConflictEntryToNull() {
        CoreMergeConflict coreMergeConflict = new CoreMergeConflict();
        coreMergeConflict.setElementConflicts(
            Collections.<CoreElementConflict>singletonList(null));

        List<ElementConflict> elementConflicts =
            new ArrayList<>(MergeConflictConvertor.convert(coreMergeConflict)
                .getElementConflicts());

        Assert.assertEquals(elementConflicts.size(), 1);
        Assert.assertNull(elementConflicts.get(0));
    }

    private static CoreElementConflict elementConflict(String localId, String remoteId) {
        CoreElement local = new CoreElement();
        local.setId(new Id(localId));
        local.setInfo(info(localId));
        local.setData(stream(localId + "-data"));

        CoreElement remote = new CoreElement();
        remote.setId(new Id(remoteId));
        remote.setInfo(info(remoteId));
        remote.setData(stream(remoteId + "-data"));

        CoreElementConflict conflict = new CoreElementConflict();
        conflict.setLocalElement(local);
        conflict.setRemoteElement(remote);
        return conflict;
    }
}
