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
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.stream;
import static com.amdocs.zusammen.adaptor.outbound.impl.convertor.ConvertorFixtures.text;

import com.amdocs.zusammen.core.api.types.CoreElement;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SearchIndexElementConvertorTest {

    @Test
    public void testConvertFromCoreElementCopiesIdentitySpaceAndSearchableData() {
        Namespace elementNamespace = namespace("root-id/parent-id");

        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id("element-id"));
        coreElement.setNamespace(elementNamespace);
        coreElement.setInfo(info("element"));
        coreElement.setSearchableData(stream("element-searchable-data"));

        ElementContext elementContext = new ElementContext(new Id("item-id"), new Id("version-id"));

        SearchIndexElement result = SearchIndexElementConvertor
            .convertFromCoreElement(elementContext, coreElement, Space.PUBLIC);

        Assert.assertEquals(result.getItemId(), new Id("item-id"));
        Assert.assertEquals(result.getVersionId(), new Id("version-id"));
        Assert.assertEquals(result.getNamespace(), elementNamespace);
        Assert.assertEquals(result.getId(), new Id("element-id"));
        Assert.assertEquals(result.getSpace(), Space.PUBLIC);
        Assert.assertEquals(text(result.getSearchableData()), "element-searchable-data");
    }

    @Test
    public void testConvertFromCoreElementYieldsNullSearchableDataWhenCoreElementCarriesNone() {
        CoreElement coreElement = new CoreElement();
        coreElement.setId(new Id("element-id"));
        coreElement.setNamespace(namespace("root-id"));

        SearchIndexElement result = SearchIndexElementConvertor.convertFromCoreElement(
            new ElementContext(new Id("item-id"), new Id("version-id")), coreElement,
            Space.PRIVATE);

        Assert.assertNull(result.getSearchableData());
        Assert.assertEquals(result.getSpace(), Space.PRIVATE);
    }
}
