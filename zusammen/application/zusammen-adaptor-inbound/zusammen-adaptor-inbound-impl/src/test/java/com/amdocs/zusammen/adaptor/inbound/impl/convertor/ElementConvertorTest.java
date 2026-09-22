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
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.stream;
import static com.amdocs.zusammen.adaptor.inbound.impl.convertor.ConvertorFixtures.text;

import com.amdocs.zusammen.adaptor.inbound.api.types.item.Element;
import com.amdocs.zusammen.adaptor.inbound.api.types.item.ZusammenElement;
import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ElementConvertorTest {

    @Test
    public void testConvertCopiesEveryField() {
        Info elementInfo = info("element");
        Relation firstRelation = relation("first");
        Relation secondRelation = relation("second");

        CoreElement coreElement = new CoreElement();
        coreElement.setAction(Action.UPDATE);
        coreElement.setId(new Id("element-id"));
        coreElement.setParentId(new Id("parent-id"));
        coreElement.setNamespace(namespace("root-id/parent-id"));
        coreElement.setInfo(elementInfo);
        coreElement.setRelations(Arrays.asList(firstRelation, secondRelation));
        coreElement.setData(stream("element-data"));
        coreElement.setSearchableData(stream("element-searchable-data"));
        coreElement.setVisualization(stream("element-visualization"));

        Element element = ElementConvertor.convert(coreElement);

        Assert.assertEquals(element.getAction(), Action.UPDATE);
        Assert.assertEquals(element.getElementId(), new Id("element-id"));
        Assert.assertSame(element.getInfo(), elementInfo);
        Assert.assertEquals(element.getRelations(),
            Arrays.asList(firstRelation, secondRelation));
        Assert.assertEquals(text(element.getData()), "element-data");
        Assert.assertEquals(text(element.getSearchableData()), "element-searchable-data");
        Assert.assertEquals(text(element.getVisualization()), "element-visualization");
    }

    @Test
    public void testConvertCopiesSubElementsRecursivelyInOrder() {
        CoreElement grandChild = new CoreElement();
        grandChild.setId(new Id("grand-child-id"));
        grandChild.setInfo(info("grand-child"));
        grandChild.setData(stream("grand-child-data"));

        CoreElement firstChild = new CoreElement();
        firstChild.setId(new Id("first-child-id"));
        firstChild.setAction(Action.CREATE);
        firstChild.setInfo(info("first-child"));
        firstChild.setSubElements(Collections.singletonList(grandChild));

        CoreElement secondChild = new CoreElement();
        secondChild.setId(new Id("second-child-id"));
        secondChild.setAction(Action.DELETE);
        secondChild.setInfo(info("second-child"));

        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id("element-id"));
        coreElement.setSubElements(Arrays.asList(firstChild, secondChild));

        List<Element> subElements =
            new ArrayList<>(ElementConvertor.convert(coreElement).getSubElements());

        Assert.assertEquals(subElements.size(), 2);
        Assert.assertEquals(subElements.get(0).getElementId(), new Id("first-child-id"));
        Assert.assertEquals(subElements.get(0).getAction(), Action.CREATE);
        Assert.assertEquals(subElements.get(0).getInfo().getName(), "first-child");
        Assert.assertEquals(subElements.get(1).getElementId(), new Id("second-child-id"));
        Assert.assertEquals(subElements.get(1).getAction(), Action.DELETE);
        Assert.assertEquals(subElements.get(1).getInfo().getName(), "second-child");

        List<Element> grandChildren = new ArrayList<>(subElements.get(0).getSubElements());
        Assert.assertEquals(grandChildren.size(), 1);
        Assert.assertEquals(grandChildren.get(0).getElementId(), new Id("grand-child-id"));
        Assert.assertEquals(text(grandChildren.get(0).getData()), "grand-child-data");
    }

    @Test
    public void testConvertReturnsNullForNullCoreElement() {
        Assert.assertNull(ElementConvertor.convert(null));
    }

    @Test
    public void testConvertKeepsNullSubElementsNull() {
        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id("element-id"));
        coreElement.setSubElements(null);

        Assert.assertNull(ElementConvertor.convert(coreElement).getSubElements());
    }

    @Test
    public void testConvertYieldsEmptySubElementsForEmptyCoreSubElements() {
        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id("element-id"));
        coreElement.setSubElements(new ArrayList<CoreElement>());

        Assert.assertTrue(ElementConvertor.convert(coreElement).getSubElements().isEmpty());
    }

    @Test
    public void testConvertYieldsEmptyStreamsWhenCoreElementCarriesNoBinaryData() {
        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id("element-id"));

        Element element = ElementConvertor.convert(coreElement);

        Assert.assertEquals(text(element.getData()), "");
        Assert.assertEquals(text(element.getSearchableData()), "");
        Assert.assertEquals(text(element.getVisualization()), "");
    }

    @Test
    public void testConvertFromCopiesEveryField() {
        Info elementInfo = info("element");
        Relation firstRelation = relation("first");
        Relation secondRelation = relation("second");

        ZusammenElement element = new ZusammenElement();
        element.setAction(Action.DELETE);
        element.setElementId(new Id("element-id"));
        element.setInfo(elementInfo);
        element.setRelations(Arrays.asList(firstRelation, secondRelation));
        element.setData(stream("element-data"));
        element.setSearchableData(stream("element-searchable-data"));
        element.setVisualization(stream("element-visualization"));

        CoreElement coreElement = ElementConvertor.convertFrom(element);

        Assert.assertEquals(coreElement.getAction(), Action.DELETE);
        Assert.assertEquals(coreElement.getId(), new Id("element-id"));
        Assert.assertSame(coreElement.getInfo(), elementInfo);
        Assert.assertEquals(coreElement.getRelations(),
            Arrays.asList(firstRelation, secondRelation));
        Assert.assertEquals(text(coreElement.getData()), "element-data");
        Assert.assertEquals(text(coreElement.getSearchableData()), "element-searchable-data");
        Assert.assertEquals(text(coreElement.getVisualization()), "element-visualization");
    }

    @Test
    public void testConvertFromCopiesSubElementsRecursivelyInOrder() {
        ZusammenElement grandChild = new ZusammenElement();
        grandChild.setElementId(new Id("grand-child-id"));
        grandChild.setData(stream("grand-child-data"));

        ZusammenElement firstChild = new ZusammenElement();
        firstChild.setElementId(new Id("first-child-id"));
        firstChild.setInfo(info("first-child"));
        firstChild.addSubElement(grandChild);

        ZusammenElement secondChild = new ZusammenElement();
        secondChild.setElementId(new Id("second-child-id"));
        secondChild.setInfo(info("second-child"));

        ZusammenElement element = new ZusammenElement();
        element.setElementId(new Id("element-id"));
        element.addSubElement(firstChild);
        element.addSubElement(secondChild);

        List<CoreElement> subElements =
            new ArrayList<>(ElementConvertor.convertFrom(element).getSubElements());

        Assert.assertEquals(subElements.size(), 2);
        Assert.assertEquals(subElements.get(0).getId(), new Id("first-child-id"));
        Assert.assertEquals(subElements.get(0).getInfo().getName(), "first-child");
        Assert.assertEquals(subElements.get(1).getId(), new Id("second-child-id"));
        Assert.assertEquals(subElements.get(1).getInfo().getName(), "second-child");

        List<CoreElement> grandChildren = new ArrayList<>(subElements.get(0).getSubElements());
        Assert.assertEquals(grandChildren.size(), 1);
        Assert.assertEquals(grandChildren.get(0).getId(), new Id("grand-child-id"));
        Assert.assertEquals(text(grandChildren.get(0).getData()), "grand-child-data");
    }

    @Test
    public void testConvertFromYieldsEmptySubElementsForNullSubElements() {
        ZusammenElement element = new ZusammenElement();
        element.setElementId(new Id("element-id"));
        element.setSubElements(null);

        Assert.assertTrue(ElementConvertor.convertFrom(element).getSubElements().isEmpty());
    }

    @Test
    public void testConvertFromYieldsNullBinaryDataWhenElementCarriesNone() {
        ZusammenElement element = new ZusammenElement();
        element.setElementId(new Id("element-id"));

        CoreElement coreElement = ElementConvertor.convertFrom(element);

        Assert.assertNull(coreElement.getData());
        Assert.assertNull(coreElement.getSearchableData());
        Assert.assertNull(coreElement.getVisualization());
    }

    @Test
    public void testRoundTripFromCoreElementPreservesElementFields() {
        Info elementInfo = info("element");
        Relation elementRelation = relation("only");

        CoreElement child = new CoreElement();
        child.setId(new Id("child-id"));
        child.setInfo(info("child"));
        child.setData(stream("child-data"));

        CoreElement original = new CoreElement();
        original.setAction(Action.CREATE);
        original.setId(new Id("element-id"));
        original.setInfo(elementInfo);
        original.setRelations(Collections.singletonList(elementRelation));
        original.setData(stream("element-data"));
        original.setSearchableData(stream("element-searchable-data"));
        original.setVisualization(stream("element-visualization"));
        original.setSubElements(Collections.singletonList(child));

        CoreElement roundTripped =
            ElementConvertor.convertFrom(ElementConvertor.convert(original));

        Assert.assertEquals(roundTripped.getAction(), Action.CREATE);
        Assert.assertEquals(roundTripped.getId(), new Id("element-id"));
        Assert.assertSame(roundTripped.getInfo(), elementInfo);
        Assert.assertEquals(roundTripped.getRelations(),
            Collections.singletonList(elementRelation));
        Assert.assertEquals(text(roundTripped.getData()), "element-data");
        Assert.assertEquals(text(roundTripped.getSearchableData()), "element-searchable-data");
        Assert.assertEquals(text(roundTripped.getVisualization()), "element-visualization");

        List<CoreElement> subElements = new ArrayList<>(roundTripped.getSubElements());
        Assert.assertEquals(subElements.size(), 1);
        Assert.assertEquals(subElements.get(0).getId(), new Id("child-id"));
        Assert.assertEquals(subElements.get(0).getInfo().getName(), "child");
        Assert.assertEquals(text(subElements.get(0).getData()), "child-data");
    }
}
