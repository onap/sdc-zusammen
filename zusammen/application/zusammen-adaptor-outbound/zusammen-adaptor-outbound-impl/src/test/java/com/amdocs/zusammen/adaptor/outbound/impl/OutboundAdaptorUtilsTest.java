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

package com.amdocs.zusammen.adaptor.outbound.impl;

import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.sdk.state.StateStore;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OutboundAdaptorUtilsTest {

    private StateStore stateStore;

    @BeforeMethod
    public void setUp() {
        stateStore = Mockito.mock(StateStore.class);
        OutboundTestSupport.installStateStore(stateStore);
    }

    @AfterMethod
    public void tearDown() {
        OutboundTestSupport.restoreStateStore();
    }

    @Test
    public void testGetStateStoreReturnsThePluginResolvedByTheStateStoreFactory() {
        Assert.assertSame(OutboundAdaptorUtils.getStateStore(new SessionContext()), stateStore);
    }

    @Test
    public void testGetStateStorePassesTheSessionContextToTheFactory() {
        SessionContext context = new SessionContext();
        context.setTenant("a-tenant");

        OutboundAdaptorUtils.getStateStore(context);

        Assert.assertSame(OutboundTestSupport.stateStoreResolutionContext(), context);
    }
}
