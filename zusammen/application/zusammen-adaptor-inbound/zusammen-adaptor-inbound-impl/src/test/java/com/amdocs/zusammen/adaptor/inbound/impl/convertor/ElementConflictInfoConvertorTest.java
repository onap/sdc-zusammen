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
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.relation;

import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementConflictInfo;
import com.amdocs.zusammen.core.api.types.CoreElementConflictInfo;
import com.amdocs.zusammen.core.api.types.CoreElementInfo;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Relation;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

public class ElementConflictInfoConvertorTest {

    @Test
    public void testConvertCopiesBothSidesOfTheConflict() {
        Relation localRelation = relation("local");
        CoreElementInfo local = new CoreElementInfo();
        local.setId(new Id("local-id"));
        local.setInfo(info("local"));
        local.setRelations(Collections.singletonList(localRelation));

        CoreElementInfo remote = new CoreElementInfo();
        remote.setId(new Id("remote-id"));
        remote.setInfo(info("remote"));

        CoreElementConflictInfo source = new CoreElementConflictInfo();
        source.setLocalCoreElementInfo(local);
        source.setRemoteCoreElementInfo(remote);

        ElementConflictInfo result = ElementConflictInfoConvertor.convert(source);

        Assert.assertEquals(result.getLocalElementInfo().getId(), new Id("local-id"));
        Assert.assertEquals(result.getLocalElementInfo().getInfo().getName(), "local");
        Assert.assertEquals(result.getLocalElementInfo().getRelations(),
            Collections.singletonList(localRelation));
        Assert.assertEquals(result.getRemoteElementInfo().getId(), new Id("remote-id"));
        Assert.assertEquals(result.getRemoteElementInfo().getInfo().getName(), "remote");
    }

    @Test
    public void testConvertReturnsNullForNullCoreElementConflictInfo() {
        Assert.assertNull(ElementConflictInfoConvertor.convert(null));
    }

    @Test
    public void testConvertKeepsMissingLocalElementInfoNull() {
        CoreElementInfo remote = new CoreElementInfo();
        remote.setId(new Id("remote-id"));

        CoreElementConflictInfo source = new CoreElementConflictInfo();
        source.setRemoteCoreElementInfo(remote);

        ElementConflictInfo result = ElementConflictInfoConvertor.convert(source);

        Assert.assertNull(result.getLocalElementInfo());
        Assert.assertEquals(result.getRemoteElementInfo().getId(), new Id("remote-id"));
    }

    @Test
    public void testConvertKeepsMissingRemoteElementInfoNull() {
        CoreElementInfo local = new CoreElementInfo();
        local.setId(new Id("local-id"));

        CoreElementConflictInfo source = new CoreElementConflictInfo();
        source.setLocalCoreElementInfo(local);

        ElementConflictInfo result = ElementConflictInfoConvertor.convert(source);

        Assert.assertEquals(result.getLocalElementInfo().getId(), new Id("local-id"));
        Assert.assertNull(result.getRemoteElementInfo());
    }
}
