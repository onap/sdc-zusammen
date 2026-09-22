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
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.versionDataConflict;

import com.amdocs.zusammen.core.api.types.CoreElementConflictInfo;
import com.amdocs.zusammen.core.api.types.CoreItemVersionConflict;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationItemVersionConflict;
import com.amdocs.zusammen.sdk.types.ElementConflictDescriptor;
import com.amdocs.zusammen.sdk.types.ElementDescriptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class CollaborationItemVersionConflictConvertorTest {

    @Test
    public void testConvertToCoreItemVersionConflictCopiesVersionDataConflict() {
        ItemVersionDataConflict dataConflict = versionDataConflict("local-version", "remote-version");

        CollaborationItemVersionConflict source = new CollaborationItemVersionConflict();
        source.setVersionDataConflict(dataConflict);

        CoreItemVersionConflict result = CollaborationItemVersionConflictConvertor
            .convertToCoreItemVersionConflict(source);

        Assert.assertSame(result.getVersionDataConflict(), dataConflict);
        Assert.assertEquals(result.getVersionDataConflict().getLocalData().getInfo().getName(),
            "local-version");
        Assert.assertEquals(result.getVersionDataConflict().getRemoteData().getInfo().getName(),
            "remote-version");
    }

    @Test
    public void testConvertToCoreItemVersionConflictCopiesElementConflictsInOrder() {
        CollaborationItemVersionConflict source = new CollaborationItemVersionConflict();
        source.addElementConflictDescriptor(
            conflictDescriptor("first-local-id", "first-remote-id"));
        source.addElementConflictDescriptor(
            conflictDescriptor("second-local-id", "second-remote-id"));

        List<CoreElementConflictInfo> conflictInfos =
            new ArrayList<>(CollaborationItemVersionConflictConvertor
                .convertToCoreItemVersionConflict(source).getElementConflictInfos());

        Assert.assertEquals(conflictInfos.size(), 2);
        Assert.assertEquals(conflictInfos.get(0).getLocalCoreElementInfo().getId(),
            new Id("first-local-id"));
        Assert.assertEquals(conflictInfos.get(0).getLocalCoreElementInfo().getInfo().getName(),
            "first-local-id");
        Assert.assertEquals(conflictInfos.get(0).getRemoteCoreElementInfo().getId(),
            new Id("first-remote-id"));
        Assert.assertEquals(conflictInfos.get(1).getLocalCoreElementInfo().getId(),
            new Id("second-local-id"));
        Assert.assertEquals(conflictInfos.get(1).getRemoteCoreElementInfo().getId(),
            new Id("second-remote-id"));
        Assert.assertEquals(conflictInfos.get(1).getRemoteCoreElementInfo().getInfo().getName(),
            "second-remote-id");
    }

    @Test
    public void testConvertToCoreItemVersionConflictYieldsEmptyElementConflictsWhenThereAreNone() {
        CoreItemVersionConflict result = CollaborationItemVersionConflictConvertor
            .convertToCoreItemVersionConflict(new CollaborationItemVersionConflict());

        Assert.assertTrue(result.getElementConflictInfos().isEmpty());
        Assert.assertNull(result.getVersionDataConflict());
    }

    private static ElementConflictDescriptor conflictDescriptor(String localId, String remoteId) {
        ElementDescriptor local = new ElementDescriptor(new Id("item-id"), new Id("version-id"),
            namespace("root-id"), new Id(localId));
        local.setInfo(info(localId));

        ElementDescriptor remote = new ElementDescriptor(new Id("item-id"), new Id("version-id"),
            namespace("root-id"), new Id(remoteId));
        remote.setInfo(info(remoteId));

        ElementConflictDescriptor descriptor = new ElementConflictDescriptor();
        descriptor.setLocalElementDescriptor(local);
        descriptor.setRemoteElementDescriptor(remote);
        return descriptor;
    }
}
