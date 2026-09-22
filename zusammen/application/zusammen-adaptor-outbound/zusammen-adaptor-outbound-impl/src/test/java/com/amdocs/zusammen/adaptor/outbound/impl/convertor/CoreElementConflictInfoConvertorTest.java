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
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.relation;

import com.amdocs.zusammen.core.api.types.CoreElementConflictInfo;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.sdk.types.ElementConflictDescriptor;
import com.amdocs.zusammen.sdk.types.ElementDescriptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

public class CoreElementConflictInfoConvertorTest {

    @Test
    public void testConvertToCoreElementInfoCopiesBothSidesOfTheConflict() {
        Relation localRelation = relation("local");

        ElementDescriptor local = descriptor("local-id");
        local.setInfo(info("local"));
        local.setRelations(Collections.singletonList(localRelation));

        ElementDescriptor remote = descriptor("remote-id");
        remote.setInfo(info("remote"));
        remote.setParentId(new Id("remote-parent-id"));

        ElementConflictDescriptor source = new ElementConflictDescriptor();
        source.setLocalElementDescriptor(local);
        source.setRemoteElementDescriptor(remote);

        CoreElementConflictInfo result =
            CoreElementConflictInfoConvertor.convertToCoreElementInfo(source);

        Assert.assertEquals(result.getLocalCoreElementInfo().getId(), new Id("local-id"));
        Assert.assertEquals(result.getLocalCoreElementInfo().getInfo().getName(), "local");
        Assert.assertEquals(result.getLocalCoreElementInfo().getRelations(),
            Collections.singletonList(localRelation));
        Assert.assertEquals(result.getRemoteCoreElementInfo().getId(), new Id("remote-id"));
        Assert.assertEquals(result.getRemoteCoreElementInfo().getInfo().getName(), "remote");
        Assert.assertEquals(result.getRemoteCoreElementInfo().getParentId(),
            new Id("remote-parent-id"));
    }

    @Test
    public void testConvertToCoreElementInfoKeepsMissingLocalDescriptorNull() {
        ElementConflictDescriptor source = new ElementConflictDescriptor();
        source.setRemoteElementDescriptor(descriptor("remote-id"));

        CoreElementConflictInfo result =
            CoreElementConflictInfoConvertor.convertToCoreElementInfo(source);

        Assert.assertNull(result.getLocalCoreElementInfo());
        Assert.assertEquals(result.getRemoteCoreElementInfo().getId(), new Id("remote-id"));
    }

    @Test
    public void testConvertToCoreElementInfoKeepsMissingRemoteDescriptorNull() {
        ElementConflictDescriptor source = new ElementConflictDescriptor();
        source.setLocalElementDescriptor(descriptor("local-id"));

        CoreElementConflictInfo result =
            CoreElementConflictInfoConvertor.convertToCoreElementInfo(source);

        Assert.assertEquals(result.getLocalCoreElementInfo().getId(), new Id("local-id"));
        Assert.assertNull(result.getRemoteCoreElementInfo());
    }

    @Test
    public void testConvertToCoreElementInfoYieldsEmptyConflictInfoWhenNoSidePresent() {
        CoreElementConflictInfo result = CoreElementConflictInfoConvertor
            .convertToCoreElementInfo(new ElementConflictDescriptor());

        Assert.assertNull(result.getLocalCoreElementInfo());
        Assert.assertNull(result.getRemoteCoreElementInfo());
    }

    private static ElementDescriptor descriptor(String elementId) {
        return new ElementDescriptor(new Id("item-id"), new Id("version-id"),
            namespace("root-id"), new Id(elementId));
    }
}
