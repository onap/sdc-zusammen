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
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.versionDataConflict;

import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementConflictInfo;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.ItemVersionConflict;
import com.amdocs.zusammen.core.api.types.CoreElementConflictInfo;
import com.amdocs.zusammen.core.api.types.CoreElementInfo;
import com.amdocs.zusammen.core.api.types.CoreItemVersionConflict;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class ItemVersionConflictConvertorTest {

    @Test
    public void testConvertCopiesVersionDataConflict() {
        ItemVersionDataConflict dataConflict = versionDataConflict("local-version", "remote-version");

        CoreItemVersionConflict source = new CoreItemVersionConflict();
        source.setVersionDataConflict(dataConflict);

        ItemVersionConflict result = ItemVersionConflictConvertor.convert(source);

        Assert.assertSame(result.getVersionDataConflict(), dataConflict);
        Assert.assertEquals(result.getVersionDataConflict().getLocalData().getInfo().getName(),
            "local-version");
        Assert.assertEquals(result.getVersionDataConflict().getRemoteData().getInfo().getName(),
            "remote-version");
    }

    @Test
    public void testConvertCopiesElementConflictInfosInOrder() {
        CoreItemVersionConflict source = new CoreItemVersionConflict();
        source.addElementConflict(elementConflictInfo("first-local", "first-remote"));
        source.addElementConflict(elementConflictInfo("second-local", "second-remote"));

        List<ElementConflictInfo> conflictInfos =
            new ArrayList<>(ItemVersionConflictConvertor.convert(source).getElementConflictInfos());

        Assert.assertEquals(conflictInfos.size(), 2);
        Assert.assertEquals(conflictInfos.get(0).getLocalElementInfo().getId(),
            new Id("first-local"));
        Assert.assertEquals(conflictInfos.get(0).getLocalElementInfo().getInfo().getName(),
            "first-local");
        Assert.assertEquals(conflictInfos.get(0).getRemoteElementInfo().getId(),
            new Id("first-remote"));
        Assert.assertEquals(conflictInfos.get(1).getLocalElementInfo().getId(),
            new Id("second-local"));
        Assert.assertEquals(conflictInfos.get(1).getRemoteElementInfo().getId(),
            new Id("second-remote"));
        Assert.assertEquals(conflictInfos.get(1).getRemoteElementInfo().getInfo().getName(),
            "second-remote");
    }

    @Test
    public void testConvertYieldsEmptyElementConflictInfosWhenThereAreNone() {
        ItemVersionConflict result =
            ItemVersionConflictConvertor.convert(new CoreItemVersionConflict());

        Assert.assertTrue(result.getElementConflictInfos().isEmpty());
        Assert.assertNull(result.getVersionDataConflict());
    }

    private static CoreElementConflictInfo elementConflictInfo(String localId, String remoteId) {
        CoreElementInfo local = new CoreElementInfo();
        local.setId(new Id(localId));
        local.setInfo(info(localId));

        CoreElementInfo remote = new CoreElementInfo();
        remote.setId(new Id(remoteId));
        remote.setInfo(info(remoteId));

        CoreElementConflictInfo conflictInfo = new CoreElementConflictInfo();
        conflictInfo.setLocalCoreElementInfo(local);
        conflictInfo.setRemoteCoreElementInfo(remote);
        return conflictInfo;
    }
}
