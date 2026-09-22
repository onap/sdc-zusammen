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
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.stream;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.text;

import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.core.api.types.CoreElementConflict;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementConflict;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollaborationElementConvertorTest {

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
        coreElement.setData(stream("element-data"));
        coreElement.setSearchableData(stream("element-searchable-data"));
        coreElement.setVisualization(stream("element-visualization"));

        ElementContext elementContext =
            new ElementContext(new Id("item-id"), new Id("version-id"), new Id("revision-id"));

        CollaborationElement result =
            CollaborationElementConvertor.convertFromCoreElement(coreElement, elementContext);

        Assert.assertEquals(result.getItemId(), new Id("item-id"));
        Assert.assertEquals(result.getVersionId(), new Id("version-id"));
        Assert.assertEquals(result.getNamespace(), elementNamespace);
        Assert.assertEquals(result.getId(), new Id("element-id"));
        Assert.assertEquals(result.getParentId(), new Id("parent-id"));
        Assert.assertSame(result.getInfo(), elementInfo);
        Assert.assertEquals(result.getRelations(), Arrays.asList(firstRelation, secondRelation));
        Assert.assertEquals(text(result.getData()), "element-data");
        Assert.assertEquals(text(result.getSearchableData()), "element-searchable-data");
        Assert.assertEquals(text(result.getVisualization()), "element-visualization");
    }

    @Test
    public void testConvertFromCoreElementKeepsBinaryDataNullWhenCoreElementCarriesNone() {
        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id("element-id"));

        CollaborationElement result = CollaborationElementConvertor.convertFromCoreElement(
            coreElement, new ElementContext(new Id("item-id"), new Id("version-id")));

        Assert.assertNull(result.getData());
        Assert.assertNull(result.getSearchableData());
        Assert.assertNull(result.getVisualization());
    }

    @Test
    public void testConvertToCoreElementCopiesEveryField() {
        Info elementInfo = info("element");
        Relation elementRelation = relation("only");
        Namespace elementNamespace = namespace("root-id/parent-id");

        CollaborationElement element = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), elementNamespace, new Id("element-id"));
        element.setParentId(new Id("parent-id"));
        element.setInfo(elementInfo);
        element.setRelations(Collections.singletonList(elementRelation));
        element.setData(stream("element-data"));
        element.setSearchableData(stream("element-searchable-data"));
        element.setVisualization(stream("element-visualization"));

        CoreElement result = CollaborationElementConvertor.convertToCoreElement(element);

        Assert.assertEquals(result.getId(), new Id("element-id"));
        Assert.assertEquals(result.getNamespace(), elementNamespace);
        Assert.assertEquals(result.getParentId(), new Id("parent-id"));
        Assert.assertSame(result.getInfo(), elementInfo);
        Assert.assertEquals(result.getRelations(), Collections.singletonList(elementRelation));
        Assert.assertEquals(text(result.getData()), "element-data");
        Assert.assertEquals(text(result.getSearchableData()), "element-searchable-data");
        Assert.assertEquals(text(result.getVisualization()), "element-visualization");
    }

    @Test
    public void testConvertToCoreElementCarriesOnlyIdsOfSubElements() {
        CollaborationElement element = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), namespace("root-id"), new Id("element-id"));
        element.setSubElements(ids("first-child-id", "second-child-id"));

        CoreElement result = CollaborationElementConvertor.convertToCoreElement(element);

        List<CoreElement> subElements = new ArrayList<>(result.getSubElements());
        Assert.assertEquals(subElements.size(), 2);
        Set<Id> subElementIds = new HashSet<>();
        for (CoreElement subElement : subElements) {
            subElementIds.add(subElement.getId());
            Assert.assertNull(subElement.getInfo());
            Assert.assertNull(subElement.getNamespace());
        }
        Assert.assertEquals(subElementIds, ids("first-child-id", "second-child-id"));
    }

    @Test
    public void testConvertToCoreElementYieldsEmptySubElementsWhenThereAreNone() {
        CollaborationElement element = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), namespace("root-id"), new Id("element-id"));

        Assert.assertTrue(
            CollaborationElementConvertor.convertToCoreElement(element).getSubElements().isEmpty());
    }

    @Test
    public void testConvertToCoreElementReturnsNullForNullCollaborationElement() {
        Assert.assertNull(CollaborationElementConvertor.convertToCoreElement(
            (CollaborationElement) null));
    }

    @Test
    public void testConvertToCoreElementCopiesBothSidesOfTheConflict() {
        CollaborationElement local = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), namespace("root-id"), new Id("local-id"));
        local.setInfo(info("local"));
        local.setData(stream("local-data"));

        CollaborationElement remote = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), namespace("root-id"), new Id("remote-id"));
        remote.setInfo(info("remote"));
        remote.setData(stream("remote-data"));

        CollaborationElementConflict conflict = new CollaborationElementConflict();
        conflict.setLocalElement(local);
        conflict.setRemoteElement(remote);

        CoreElementConflict result = CollaborationElementConvertor.convertToCoreElement(conflict);

        Assert.assertEquals(result.getLocalElement().getId(), new Id("local-id"));
        Assert.assertEquals(result.getLocalElement().getInfo().getName(), "local");
        Assert.assertEquals(text(result.getLocalElement().getData()), "local-data");
        Assert.assertEquals(result.getRemoteElement().getId(), new Id("remote-id"));
        Assert.assertEquals(result.getRemoteElement().getInfo().getName(), "remote");
        Assert.assertEquals(text(result.getRemoteElement().getData()), "remote-data");
    }

    @Test
    public void testConvertToCoreElementKeepsMissingConflictSidesNull() {
        CollaborationElement local = new CollaborationElement(new Id("item-id"),
            new Id("version-id"), namespace("root-id"), new Id("local-id"));

        CollaborationElementConflict conflict = new CollaborationElementConflict();
        conflict.setLocalElement(local);

        CoreElementConflict result = CollaborationElementConvertor.convertToCoreElement(conflict);

        Assert.assertEquals(result.getLocalElement().getId(), new Id("local-id"));
        Assert.assertNull(result.getRemoteElement());
    }

    @Test
    public void testRoundTripFromCoreElementPreservesStoredFields() {
        Info elementInfo = info("element");
        Relation elementRelation = relation("only");
        Namespace elementNamespace = namespace("root-id/parent-id");

        CoreElement original = new CoreElement();
        original.setAction(Action.UPDATE);
        original.setId(new Id("element-id"));
        original.setParentId(new Id("parent-id"));
        original.setNamespace(elementNamespace);
        original.setInfo(elementInfo);
        original.setRelations(Collections.singletonList(elementRelation));
        original.setData(stream("element-data"));
        original.setSearchableData(stream("element-searchable-data"));
        original.setVisualization(stream("element-visualization"));

        CoreElement roundTripped = CollaborationElementConvertor.convertToCoreElement(
            CollaborationElementConvertor.convertFromCoreElement(original,
                new ElementContext(new Id("item-id"), new Id("version-id"))));

        Assert.assertEquals(roundTripped.getId(), new Id("element-id"));
        Assert.assertEquals(roundTripped.getParentId(), new Id("parent-id"));
        Assert.assertEquals(roundTripped.getNamespace(), elementNamespace);
        Assert.assertSame(roundTripped.getInfo(), elementInfo);
        Assert.assertEquals(roundTripped.getRelations(),
            Collections.singletonList(elementRelation));
        Assert.assertEquals(text(roundTripped.getData()), "element-data");
        Assert.assertEquals(text(roundTripped.getSearchableData()), "element-searchable-data");
        Assert.assertEquals(text(roundTripped.getVisualization()), "element-visualization");
    }
}
