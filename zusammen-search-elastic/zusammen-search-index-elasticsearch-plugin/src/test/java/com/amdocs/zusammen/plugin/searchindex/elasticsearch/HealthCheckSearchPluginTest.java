/*
 * Copyright © 2016-2017 European Support Limited
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


package com.amdocs.zusammen.plugin.searchindex.elasticsearch;


import com.amdocs.zusammen.plugin.searchindex.elasticsearch.dao.ElasticSearchDao;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.impl.SearchIndexElasticImpl;
import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.commons.health.data.HealthStatus;
import com.amdocs.zusammen.datatypes.SessionContext;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyObject;
import static org.testng.Assert.assertEquals;

public class HealthCheckSearchPluginTest {


    @Mock
    private ElasticSearchDao elasticSearchDaoMock;

    @Mock
    private ElementSearchIndex elementSearchIndex;

    @InjectMocks
    private SearchIndexElasticImpl searchIndexElastic;
    private String tenant;
    private String user;


    private void initSearchData() {
        tenant = "elementsearchindextest";
        user = "user";
    }


    public void injectColor(ClusterHealthStatus inColor) {
        initSearchData();
        MockitoAnnotations.initMocks(this);
        ClusterHealthResponse healthResponse = new ClusterHealthResponse("test",
                new String[]{}
                , ClusterState.PROTO);
        healthResponse.setStatus(inColor);
        Mockito.when(elasticSearchDaoMock.checkHealth(anyObject())).thenReturn(healthResponse);
        Mockito.when(elementSearchIndex.checkHealth(anyObject())).thenReturn(healthResponse);
    }

    private void checkResult(HealthStatus healthStatus) {
        SessionContext sessionContext = EsTestUtils.createSessionContext(tenant, user);
        HealthInfo healthInfo = searchIndexElastic.checkHealth(sessionContext).getValue();
        assertEquals(healthStatus, healthInfo.getHealthStatus());
    }

    @Test()
    public void testUp() throws Exception {
        injectColor(ClusterHealthStatus.GREEN);
        checkResult(HealthStatus.UP);
    }

   @Test()
    public void testDownYellow() throws Exception {
        injectColor(ClusterHealthStatus.YELLOW);
        checkResult(HealthStatus.DOWN);

    }
   @Test()
    public void testDownRed() throws Exception {
        injectColor(ClusterHealthStatus.RED);
        checkResult(HealthStatus.DOWN);

    }






}


