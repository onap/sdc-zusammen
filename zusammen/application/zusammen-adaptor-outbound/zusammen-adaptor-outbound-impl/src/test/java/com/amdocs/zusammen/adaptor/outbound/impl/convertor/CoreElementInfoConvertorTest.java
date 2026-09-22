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

import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.ids;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.info;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.namespace;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.relation;

import com.amdocs.zusammen.core.api.types.CoreElementInfo;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.sdk.types.ElementDescriptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CoreElementInfoConvertorTest {

    @Test
    public void testConvertToCoreElementInfoCopiesEveryField() {
        Info elementInfo = info("element");
        Relation firstRelation = relation("first");
        Relation secondRelation = relation("second");
        Namespace elementNamespace = namespace("root-id/parent-id");

        ElementDescriptor source = new ElementDescriptor(new Id("item-id"), new Id("version-id"),
            elementNamespace, new Id("element-id"));
        source.setParentId(new Id("parent-id"));
        source.setInfo(elementInfo);
        source.setRelations(Arrays.asList(firstRelation, secondRelation));

        CoreElementInfo result = CoreElementInfoConvertor.convertToCoreElementInfo(source);

        Assert.assertEquals(result.getId(), new Id("element-id"));
        Assert.assertEquals(result.getParentId(), new Id("parent-id"));
        Assert.assertEquals(result.getNamespace(), elementNamespace);
        Assert.assertSame(result.getInfo(), elementInfo);
        Assert.assertEquals(result.getRelations(), Arrays.asList(firstRelation, secondRelation));
        Assert.assertTrue(result.getSubElements().isEmpty());
    }

    @Test
    public void testConvertToCoreElementInfoCarriesOnlyIdsOfSubElements() {
        ElementDescriptor source = new ElementDescriptor(new Id("item-id"), new Id("version-id"),
            namespace("root-id"), new Id("element-id"));
        source.setSubElements(ids("first-child-id", "second-child-id"));

        CoreElementInfo result = CoreElementInfoConvertor.convertToCoreElementInfo(source);

        Assert.assertEquals(result.getSubElements().size(), 2);
        Set<Id> subElementIds = new HashSet<>();
        for (CoreElementInfo subElement : result.getSubElements()) {
            subElementIds.add(subElement.getId());
            Assert.assertNull(subElement.getInfo());
            Assert.assertNull(subElement.getParentId());
        }
        Assert.assertEquals(subElementIds, ids("first-child-id", "second-child-id"));
    }

    @Test
    public void testConvertToCoreElementInfoReturnsNullForNullDescriptor() {
        Assert.assertNull(CoreElementInfoConvertor.convertToCoreElementInfo(null));
    }
}
