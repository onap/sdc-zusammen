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

package com.amdocs.zusammen.plugin.searchindex.elasticsearch.impl;

import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.commons.health.data.HealthStatus;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.searchindex.SearchCriteria;
import com.amdocs.zusammen.datatypes.searchindex.SearchResult;
import com.amdocs.zusammen.sdk.searchindex.SearchIndex;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SearchIndexEmptyImplTest {

    private SessionContext context;
    private SearchIndex searchIndex;

    @BeforeMethod
    public void setUp() {
        context = new SessionContext();
        context.setUser(new UserInfo("SearchIndexEmptyImplTest_user"));
        context.setTenant("test");
        searchIndex = new SearchIndexEmptyImpl();
    }

    @Test
    public void testCheckHealthReportsUpForTheSearchModule() {
        Response<HealthInfo> response = searchIndex.checkHealth(context);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(response.getValue().getHealthStatus(), HealthStatus.UP);
        Assert.assertEquals(response.getValue().getModuleName(), "SEARCH");
    }

    @Test
    public void testCreateElementReportsSuccess() {
        Assert.assertTrue(searchIndex.createElement(context, element()).isSuccessful());
    }

    @Test
    public void testUpdateElementReportsSuccess() {
        Assert.assertTrue(searchIndex.updateElement(context, element()).isSuccessful());
    }

    @Test
    public void testDeleteElementReportsSuccess() {
        Assert.assertTrue(searchIndex.deleteElement(context, element()).isSuccessful());
    }

    @Test
    public void testCreateElementAcceptsANullElement() {
        Assert.assertTrue(searchIndex.createElement(context, null).isSuccessful());
    }

    @Test
    public void testSearchReturnsAResultCarryingNothing() {
        Response<SearchResult> response = searchIndex.search(context, new SearchCriteria() {
        });

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNotNull(response.getValue());
    }

    @Test
    public void testSearchAcceptsNullCriteria() {
        Assert.assertNotNull(searchIndex.search(context, null).getValue());
    }

    private static SearchIndexElement element() {
        SearchIndexElement element = new SearchIndexElement(new Id("item-1"),
                new Id("version-1"), new Namespace(), new Id("element-1"));
        element.setSpace(Space.PRIVATE);
        return element;
    }
}
