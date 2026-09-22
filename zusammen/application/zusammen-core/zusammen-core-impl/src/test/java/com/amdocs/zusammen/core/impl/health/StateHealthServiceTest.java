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

package com.amdocs.zusammen.core.impl.health;

import com.amdocs.zusammen.adaptor.outbound.api.health.HealthAdaptor;
import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.commons.health.data.HealthStatus;
import com.amdocs.zusammen.core.impl.TestUtils;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.mockito.Mockito.doReturn;

public class StateHealthServiceTest {

    private static final SessionContext CONTEXT = TestUtils
            .createSessionContext(new UserInfo("StateHealthServiceTest_user"), "test");

    private static final HealthInfo COLLABORATION_STATUS =
            new HealthInfo("collaboration", HealthStatus.UP, "collaboration is up");
    private static final HealthInfo STATE_STATUS =
            new HealthInfo("state", HealthStatus.DOWN, "state is down");
    private static final HealthInfo SEARCH_STATUS =
            new HealthInfo("search", HealthStatus.UP, "search is up");

    @Mock
    private HealthAdaptor healthAdaptorMock;
    private AutoCloseable mocks;
    private StateHealthService service;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new StateHealthService();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        HealthAdaptorFactoryStubs.uninstall();
        mocks.close();
    }

    @Test
    public void testHealthStatusComesFromTheMatchingAdaptorCall() {
        doReturn(COLLABORATION_STATUS).when(healthAdaptorMock).getCollaborationStatus(CONTEXT);
        doReturn(STATE_STATUS).when(healthAdaptorMock).getStateStatus(CONTEXT);
        doReturn(SEARCH_STATUS).when(healthAdaptorMock).getSearchStatus(CONTEXT);
        HealthAdaptorFactoryStubs.installAdaptor(healthAdaptorMock);

        Collection<HealthInfo> healthInfos = service.getHealthStatus(CONTEXT);

        Assert.assertEquals(healthInfos.size(), 1);
        Assert.assertSame(healthInfos.iterator().next(), STATE_STATUS);
    }

    @Test
    public void testMissingAdaptorSurfacesAsHealthCheckFailure() {
        HealthAdaptorFactoryStubs.installUnresolvableAdaptor();

        ReturnCode returnCode = TestUtils.captureFailure(() -> service.getHealthStatus(CONTEXT));

        TestUtils.assertErrorCode(returnCode, Module.ZHC, ErrorCode.HC_MISSING_PLUGIN);
    }
}
