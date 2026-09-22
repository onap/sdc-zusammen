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

import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementConflict;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Action;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ElementConflictConvertorTest {

    @Test
    public void testConvertCopiesBothSidesOfTheConflict() {
        CoreElement local = new CoreElement();
        local.setId(new Id("local-id"));
        local.setAction(Action.UPDATE);
        local.setInfo(info("local"));
        local.setData(stream("local-data"));

        CoreElement remote = new CoreElement();
        remote.setId(new Id("remote-id"));
        remote.setAction(Action.DELETE);
        remote.setInfo(info("remote"));
        remote.setData(stream("remote-data"));

        CoreElementConflict coreElementConflict = new CoreElementConflict();
        coreElementConflict.setLocalElement(local);
        coreElementConflict.setRemoteElement(remote);

        ElementConflict result = ElementConflictConvertor.convert(coreElementConflict);

        Assert.assertEquals(result.getLocalElement().getElementId(), new Id("local-id"));
        Assert.assertEquals(result.getLocalElement().getAction(), Action.UPDATE);
        Assert.assertEquals(result.getLocalElement().getInfo().getName(), "local");
        Assert.assertEquals(text(result.getLocalElement().getData()), "local-data");
        Assert.assertEquals(result.getRemoteElement().getElementId(), new Id("remote-id"));
        Assert.assertEquals(result.getRemoteElement().getAction(), Action.DELETE);
        Assert.assertEquals(result.getRemoteElement().getInfo().getName(), "remote");
        Assert.assertEquals(text(result.getRemoteElement().getData()), "remote-data");
    }

    @Test
    public void testConvertReturnsNullForNullCoreElementConflict() {
        Assert.assertNull(ElementConflictConvertor.convert(null));
    }

    @Test
    public void testConvertKeepsMissingLocalElementNull() {
        CoreElement remote = new CoreElement();
        remote.setId(new Id("remote-id"));

        CoreElementConflict coreElementConflict = new CoreElementConflict();
        coreElementConflict.setRemoteElement(remote);

        ElementConflict result = ElementConflictConvertor.convert(coreElementConflict);

        Assert.assertNull(result.getLocalElement());
        Assert.assertEquals(result.getRemoteElement().getElementId(), new Id("remote-id"));
    }

    @Test
    public void testConvertKeepsMissingRemoteElementNull() {
        CoreElement local = new CoreElement();
        local.setId(new Id("local-id"));

        CoreElementConflict coreElementConflict = new CoreElementConflict();
        coreElementConflict.setLocalElement(local);

        ElementConflict result = ElementConflictConvertor.convert(coreElementConflict);

        Assert.assertEquals(result.getLocalElement().getElementId(), new Id("local-id"));
        Assert.assertNull(result.getRemoteElement());
    }
}
