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

package com.amdocs.zusammen.plugin.statestore.cassandra;

import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.commons.health.data.HealthStatus;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.KeepAliveDao;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyObject;
import static org.testng.Assert.*;
import static org.mockito.Mockito.when;

public class KeepAliveStoreTest {
    private static final String TENANT = "test";
    private static final String USER = "KeepAliveStoreTest_user";
    private static final SessionContext context =
            TestUtils.createSessionContext(new UserInfo(USER), TENANT);

    @Mock
    private KeepAliveDao keepAliveDao;
    @Spy
    @InjectMocks
    private StateStoreImpl stateStore;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(stateStore.getKeepAliveDao(anyObject())).thenReturn(keepAliveDao);
    }

    @Test
    public void testUp() {
        when(keepAliveDao.get(context)).thenReturn(true);
        Response<HealthInfo> healthInfoResponse = stateStore.checkHealth(context);
        HealthInfo value = healthInfoResponse.getValue();
        assertEquals(value.getHealthStatus(), HealthStatus.UP);
    }

    @Test
    public void testDown() {
        when(keepAliveDao.get(context)).thenReturn(false);
        Response<HealthInfo> healthInfoResponse = stateStore.checkHealth(context);
        HealthInfo value = healthInfoResponse.getValue();
        assertEquals(value.getHealthStatus(), HealthStatus.DOWN);
    }

}