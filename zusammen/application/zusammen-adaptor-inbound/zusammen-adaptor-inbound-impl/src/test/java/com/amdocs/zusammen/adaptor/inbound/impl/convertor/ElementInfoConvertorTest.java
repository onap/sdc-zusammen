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
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.namespace;
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.relation;

import com.amdocs.zusammen.adaptor.inbound.api.types.item.ElementInfo;
import com.amdocs.zusammen.core.api.types.CoreElementInfo;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ElementInfoConvertorTest {

    @Test
    public void testConvertCopiesEveryField() {
        Info elementInfo = info("element");
        Relation firstRelation = relation("first");
        Relation secondRelation = relation("second");

        CoreElementInfo coreElementInfo = new CoreElementInfo();
        coreElementInfo.setId(new Id("element-id"));
        coreElementInfo.setParentId(new Id("parent-id"));
        coreElementInfo.setNamespace(namespace("root-id/parent-id"));
        coreElementInfo.setInfo(elementInfo);
        coreElementInfo.setRelations(Arrays.asList(firstRelation, secondRelation));

        ElementInfo result = ElementInfoConvertor.convert(coreElementInfo);

        Assert.assertEquals(result.getId(), new Id("element-id"));
        Assert.assertSame(result.getInfo(), elementInfo);
        Assert.assertEquals(result.getRelations(), Arrays.asList(firstRelation, secondRelation));
    }

    @Test
    public void testConvertCopiesSubElementsRecursivelyInOrder() {
        CoreElementInfo grandChild = new CoreElementInfo();
        grandChild.setId(new Id("grand-child-id"));
        grandChild.setInfo(info("grand-child"));

        CoreElementInfo firstChild = new CoreElementInfo();
        firstChild.setId(new Id("first-child-id"));
        firstChild.setInfo(info("first-child"));
        firstChild.setSubElements(Collections.singletonList(grandChild));

        CoreElementInfo secondChild = new CoreElementInfo();
        secondChild.setId(new Id("second-child-id"));
        secondChild.setInfo(info("second-child"));

        CoreElementInfo coreElementInfo = new CoreElementInfo();
        coreElementInfo.setId(new Id("element-id"));
        coreElementInfo.setSubElements(Arrays.asList(firstChild, secondChild));

        List<ElementInfo> subElements =
            new ArrayList<>(ElementInfoConvertor.convert(coreElementInfo).getSubElements());

        Assert.assertEquals(subElements.size(), 2);
        Assert.assertEquals(subElements.get(0).getId(), new Id("first-child-id"));
        Assert.assertEquals(subElements.get(0).getInfo().getName(), "first-child");
        Assert.assertEquals(subElements.get(1).getId(), new Id("second-child-id"));
        Assert.assertEquals(subElements.get(1).getInfo().getName(), "second-child");

        List<ElementInfo> grandChildren = new ArrayList<>(subElements.get(0).getSubElements());
        Assert.assertEquals(grandChildren.size(), 1);
        Assert.assertEquals(grandChildren.get(0).getId(), new Id("grand-child-id"));
        Assert.assertEquals(grandChildren.get(0).getInfo().getName(), "grand-child");
    }

    @Test
    public void testConvertReturnsNullForNullCoreElementInfo() {
        Assert.assertNull(ElementInfoConvertor.convert(null));
    }

    @Test
    public void testConvertKeepsNullSubElementsNull() {
        CoreElementInfo coreElementInfo = new CoreElementInfo();
        coreElementInfo.setId(new Id("element-id"));
        coreElementInfo.setSubElements(null);

        Assert.assertNull(ElementInfoConvertor.convert(coreElementInfo).getSubElements());
    }

    @Test
    public void testConvertYieldsEmptySubElementsForEmptyCoreSubElements() {
        CoreElementInfo coreElementInfo = new CoreElementInfo();
        coreElementInfo.setId(new Id("element-id"));
        coreElementInfo.setSubElements(new ArrayList<CoreElementInfo>());

        Assert.assertTrue(ElementInfoConvertor.convert(coreElementInfo).getSubElements().isEmpty());
    }
}
