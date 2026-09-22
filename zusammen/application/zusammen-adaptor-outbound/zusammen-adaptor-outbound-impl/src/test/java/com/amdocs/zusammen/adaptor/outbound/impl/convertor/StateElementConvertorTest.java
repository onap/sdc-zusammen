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

import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementInfo;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.sdk.state.types.StateElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StateElementConvertorTest {

    @Test
    public void testConvertFromCoreElementCopiesEveryField() {
        Info elementInfo = info("element");
        Relation firstRelation = relation("first");
        Relation secondRelation = relation("second");
        Namespace elementNamespace = namespace("root-id/parent-id");

        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id("element-id"));
        coreElement.setParentId(new Id("parent-id"));
        coreElement.setNamespace(elementNamespace);
        coreElement.setInfo(elementInfo);
        coreElement.setRelations(Arrays.asList(firstRelation, secondRelation));

        ElementContext elementContext = new ElementContext(new Id("item-id"), new Id("version-id"));

        StateElement result = StateElementConvertor
            .convertFromCoreElement(elementContext, Space.PUBLIC, coreElement);

        Assert.assertEquals(result.getItemId(), new Id("item-id"));
        Assert.assertEquals(result.getVersionId(), new Id("version-id"));
        Assert.assertEquals(result.getNamespace(), elementNamespace);
        Assert.assertEquals(result.getId(), new Id("element-id"));
        Assert.assertEquals(result.getParentId(), new Id("parent-id"));
        Assert.assertEquals(result.getSpace(), Space.PUBLIC);
        Assert.assertSame(result.getInfo(), elementInfo);
        Assert.assertEquals(result.getRelations(), Arrays.asList(firstRelation, secondRelation));
    }

    @Test
    public void testConvertToCoreElementInfoCopiesEveryField() {
        Info elementInfo = info("element");
        Relation elementRelation = relation("only");
        Namespace elementNamespace = namespace("root-id/parent-id");

        StateElement stateElement = new StateElement(new Id("item-id"), new Id("version-id"),
            elementNamespace, new Id("element-id"));
        stateElement.setParentId(new Id("parent-id"));
        stateElement.setInfo(elementInfo);
        stateElement.setRelations(Arrays.asList(elementRelation));

        CoreElementInfo result = StateElementConvertor.convertToCoreElementInfo(stateElement);

        Assert.assertEquals(result.getId(), new Id("element-id"));
        Assert.assertEquals(result.getParentId(), new Id("parent-id"));
        Assert.assertEquals(result.getNamespace(), elementNamespace);
        Assert.assertSame(result.getInfo(), elementInfo);
        Assert.assertEquals(result.getRelations(), Arrays.asList(elementRelation));
    }

    @Test
    public void testConvertToCoreElementInfoCarriesOnlyIdsOfSubElements() {
        StateElement stateElement = new StateElement(new Id("item-id"), new Id("version-id"),
            namespace("root-id"), new Id("element-id"));
        stateElement.setSubElements(ids("first-child-id", "second-child-id"));

        CoreElementInfo result = StateElementConvertor.convertToCoreElementInfo(stateElement);

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
    public void testConvertToCoreElementInfoYieldsEmptySubElementsForEmptySubElements() {
        StateElement stateElement = new StateElement(new Id("item-id"), new Id("version-id"),
            namespace("root-id"), new Id("element-id"));

        Assert.assertTrue(
            StateElementConvertor.convertToCoreElementInfo(stateElement).getSubElements().isEmpty());
    }

    @Test
    public void testConvertToCoreElementInfoYieldsEmptySubElementsForNullSubElements() {
        StateElement stateElement = new StateElement(new Id("item-id"), new Id("version-id"),
            namespace("root-id"), new Id("element-id"));
        stateElement.setSubElements(null);

        Assert.assertTrue(
            StateElementConvertor.convertToCoreElementInfo(stateElement).getSubElements().isEmpty());
    }
}
