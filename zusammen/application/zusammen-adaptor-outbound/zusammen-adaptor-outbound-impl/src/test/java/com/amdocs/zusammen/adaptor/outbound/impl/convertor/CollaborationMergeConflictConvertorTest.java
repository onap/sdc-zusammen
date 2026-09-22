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
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.namespace;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.stream;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.text;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.versionDataConflict;

import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.core.api.types.CoreMergeConflict;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationMergeConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CollaborationMergeConflictConvertorTest {

    @Test
    public void testConvertCopiesVersionDataConflictAndElementConflictsInOrder() {
        ItemVersionDataConflict dataConflict = versionDataConflict("local-version", "remote-version");

        CollaborationMergeConflict source = new CollaborationMergeConflict();
        source.setVersionDataConflict(dataConflict);
        source.setElementConflicts(Arrays.asList(
            elementConflict("first-local-id", "first-remote-id"),
            elementConflict("second-local-id", "second-remote-id")));

        CoreMergeConflict result = CollaborationMergeConflictConvertor.convert(source);

        Assert.assertSame(result.getVersionDataConflict(), dataConflict);

        List<CoreElementConflict> elementConflicts =
            new ArrayList<>(result.getElementConflicts());
        Assert.assertEquals(elementConflicts.size(), 2);
        Assert.assertEquals(elementConflicts.get(0).getLocalElement().getId(),
            new Id("first-local-id"));
        Assert.assertEquals(elementConflicts.get(0).getLocalElement().getInfo().getName(),
            "first-local-id");
        Assert.assertEquals(text(elementConflicts.get(0).getLocalElement().getData()),
            "first-local-id-data");
        Assert.assertEquals(elementConflicts.get(0).getRemoteElement().getId(),
            new Id("first-remote-id"));
        Assert.assertEquals(elementConflicts.get(1).getLocalElement().getId(),
            new Id("second-local-id"));
        Assert.assertEquals(elementConflicts.get(1).getRemoteElement().getId(),
            new Id("second-remote-id"));
        Assert.assertEquals(text(elementConflicts.get(1).getRemoteElement().getData()),
            "second-remote-id-data");
    }

    @Test
    public void testConvertReturnsNullForNullCollaborationMergeConflict() {
        Assert.assertNull(CollaborationMergeConflictConvertor.convert(null));
    }

    @Test
    public void testConvertYieldsEmptyElementConflictsWhenThereAreNone() {
        CoreMergeConflict result =
            CollaborationMergeConflictConvertor.convert(new CollaborationMergeConflict());

        Assert.assertTrue(result.getElementConflicts().isEmpty());
        Assert.assertNull(result.getVersionDataConflict());
    }

    @Test
    public void testConvertKeepsMissingConflictSidesNull() {
        CollaborationElementConflict localOnly = new CollaborationElementConflict();
        localOnly.setLocalElement(element("local-id"));

        CollaborationMergeConflict source = new CollaborationMergeConflict();
        source.setElementConflicts(Collections.singletonList(localOnly));

        List<CoreElementConflict> elementConflicts =
            new ArrayList<>(CollaborationMergeConflictConvertor.convert(source)
                .getElementConflicts());

        Assert.assertEquals(elementConflicts.size(), 1);
        Assert.assertEquals(elementConflicts.get(0).getLocalElement().getId(), new Id("local-id"));
        Assert.assertNull(elementConflicts.get(0).getRemoteElement());
    }

    private static CollaborationElementConflict elementConflict(String localId, String remoteId) {
        CollaborationElementConflict conflict = new CollaborationElementConflict();
        conflict.setLocalElement(element(localId));
        conflict.setRemoteElement(element(remoteId));
        return conflict;
    }

    private static CollaborationElement element(String elementId) {
        CollaborationElement element = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), namespace("root-id"), new Id(elementId));
        element.setInfo(info(elementId));
        element.setData(stream(elementId + "-data"));
        return element;
    }
}
