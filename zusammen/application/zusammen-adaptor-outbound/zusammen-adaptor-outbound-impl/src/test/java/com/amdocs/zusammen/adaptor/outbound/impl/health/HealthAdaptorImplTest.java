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

package com.amdocs.zusammen.adaptor.outbound.impl.health;

import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.assertReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.pluginReturnCode;
import static com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport.successfulResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;

import com.amdocs.zusammen.adaptor.outbound.impl.OutboundTestSupport;
import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.commons.health.data.HealthStatus;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.sdk.collaboration.CollaborationStore;
import com.amdocs.zusammen.sdk.searchindex.SearchIndex;
import com.amdocs.zusammen.sdk.state.StateStore;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.function.Consumer;
import java.util.function.Function;

public class HealthAdaptorImplTest {

    private static final SessionContext CONTEXT = new SessionContext();

    @Mock
    private CollaborationStore collaborationStore;
    @Mock
    private StateStore stateStore;
    @Mock
    private SearchIndex searchIndex;

    private AutoCloseable mocks;
    private HealthAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        OutboundTestSupport.installCollaborationStore(collaborationStore);
        OutboundTestSupport.installStateStore(stateStore);
        OutboundTestSupport.installSearchIndex(searchIndex);
        adaptor = new HealthAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OutboundTestSupport.restoreCollaborationStore();
        OutboundTestSupport.restoreStateStore();
        OutboundTestSupport.restoreSearchIndex();
        mocks.close();
    }

    @Test
    public void testCollaborationStatusIsTheCollaborationPluginsHealthInfo() {
        HealthInfo healthInfo = healthInfo("collaboration");
        Mockito.when(collaborationStore.checkHealth(any()))
                .thenReturn(successfulResponse(healthInfo));

        Assert.assertSame(adaptor.getCollaborationStatus(CONTEXT), healthInfo);
        Mockito.verify(collaborationStore).checkHealth(same(CONTEXT));
        Mockito.verifyNoInteractions(stateStore, searchIndex);
    }

    @Test
    public void testStateStatusIsTheStatePluginsHealthInfo() {
        HealthInfo healthInfo = healthInfo("state");
        Mockito.when(stateStore.checkHealth(any())).thenReturn(successfulResponse(healthInfo));

        Assert.assertSame(adaptor.getStateStatus(CONTEXT), healthInfo);
        Mockito.verify(stateStore).checkHealth(same(CONTEXT));
        Mockito.verifyNoInteractions(collaborationStore, searchIndex);
    }

    @Test
    public void testSearchStatusIsTheSearchIndexPluginsHealthInfo() {
        HealthInfo healthInfo = healthInfo("search");
        Mockito.when(searchIndex.checkHealth(any())).thenReturn(successfulResponse(healthInfo));

        Assert.assertSame(adaptor.getSearchStatus(CONTEXT), healthInfo);
        Mockito.verify(searchIndex).checkHealth(same(CONTEXT));
        Mockito.verifyNoInteractions(collaborationStore, stateStore);
    }

    @Test(dataProvider = "plugins")
    public void testMissingPluginIsReportedAsAHealthCheckReturnCode(Plugin plugin) {
        plugin.failResolution.accept(
                new IllegalStateException("no " + plugin.label + " plugin on the classpath"));

        try {
            plugin.statusCall.apply(adaptor);
            Assert.fail(plugin.label + " status must raise ZusammenException when unresolvable");
        } catch (ZusammenException e) {
            ReturnCode returnCode = e.getReturnCode();
            assertReturnCode(returnCode, Module.ZHC, ErrorCode.HC_MISSING_PLUGIN, null);
            Assert.assertNull(returnCode.getReturnCode());
        }
    }

    @Test(dataProvider = "plugins")
    public void testPluginZusammenExceptionIsPassedThroughUnwrapped(Plugin plugin) {
        ZusammenException original =
                new ZusammenException(pluginReturnCode(plugin.label + " misconfigured"));
        plugin.failResolution.accept(original);

        try {
            plugin.statusCall.apply(adaptor);
            Assert.fail(plugin.label + " status must propagate the plugin's ZusammenException");
        } catch (ZusammenException e) {
            Assert.assertSame(e, original);
        }
    }


    @DataProvider(name = "plugins")
    public static Object[][] plugins() {
        return new Object[][] {
                {new Plugin("collaboration", OutboundTestSupport::failCollaborationStoreResolution,
                        adaptor -> adaptor.getCollaborationStatus(CONTEXT))},
                {new Plugin("state", OutboundTestSupport::failStateStoreResolution,
                        adaptor -> adaptor.getStateStatus(CONTEXT))},
                {new Plugin("search", OutboundTestSupport::failSearchIndexResolution,
                        adaptor -> adaptor.getSearchStatus(CONTEXT))}};
    }

    private static HealthInfo healthInfo(String moduleName) {
        return new HealthInfo(moduleName, HealthStatus.UP, moduleName + " is fine");
    }

    private static final class Plugin {
        private final String label;
        private final Consumer<RuntimeException> failResolution;
        private final Function<HealthAdaptorImpl, HealthInfo> statusCall;

        private Plugin(String label, Consumer<RuntimeException> failResolution,
                       Function<HealthAdaptorImpl, HealthInfo> statusCall) {
            this.label = label;
            this.failResolution = failResolution;
            this.statusCall = statusCall;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
