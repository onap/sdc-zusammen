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

package com.amdocs.zusammen.adaptor.inbound.impl.health;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.commons.health.data.HealthStatus;
import com.amdocs.zusammen.core.api.health.HealthManager;
import com.amdocs.zusammen.core.api.health.HealthManagerFactory;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HealthAdaptorImplTest {

    private static final String PRODUCTION_FACTORY =
            "com.amdocs.zusammen.core.impl.health.HealthManagerFactoryImpl";

    public static class StubHealthManagerFactory extends HealthManagerFactory {

        static HealthManager manager;

        @Override
        public HealthManager createInterface(SessionContext context) {
            return manager;
        }
    }

    @Mock
    private HealthManager healthManager;

    private AutoCloseable mocks;
    private SessionContext context;
    private HealthAdaptorImpl adaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        // A class literal does not run a static initialiser. AbstractComponentFactory's one-shot load of
        // every factoryConfiguration.json has to happen before registerFactory, or the first factory
        // access reloads the production mapping over the stub.
        Class.forName(HealthManagerFactory.class.getName(), true,
                HealthManagerFactory.class.getClassLoader());
        StubHealthManagerFactory.manager = healthManager;
        AbstractFactoryBase.registerFactory(HealthManagerFactory.class,
                StubHealthManagerFactory.class);
        context = new SessionContext();
        context.setUser(new UserInfo("health-adaptor-user"));
        context.setTenant("zusammen-test-tenant");
        adaptor = new HealthAdaptorImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        restoreProductionFactory();
        StubHealthManagerFactory.manager = null;
        mocks.close();
    }

    @Test
    public void testGetHealthStatusReturnsEveryManagerHealthInfo() {
        when(healthManager.getHealthStatus(context)).thenReturn(Arrays.asList(
                new HealthInfo("cassandra", HealthStatus.UP, "all-nodes-reachable"),
                new HealthInfo("search-index", HealthStatus.DOWN, "no-plugin")));

        List<HealthInfo> healthStatus = new ArrayList<>(adaptor.getHealthStatus(context));

        Assert.assertEquals(healthStatus.size(), 2);
        Assert.assertEquals(healthStatus.get(0).getModuleName(), "cassandra");
        Assert.assertEquals(healthStatus.get(0).getHealthStatus(), HealthStatus.UP);
        Assert.assertEquals(healthStatus.get(1).getModuleName(), "search-index");
        Assert.assertEquals(healthStatus.get(1).getHealthStatus(), HealthStatus.DOWN);
        Assert.assertEquals(healthStatus.get(1).getDescription(), "no-plugin");
        verify(healthManager).getHealthStatus(context);
    }

    @Test
    public void testGetHealthStatusReportSerialisesManagerHealthInfoToJson() {
        when(healthManager.getHealthStatus(context)).thenReturn(Collections.singletonList(
                new HealthInfo("cassandra", HealthStatus.UP, "all-nodes-reachable")));

        String report = adaptor.getHealthStatusReport(context);

        Assert.assertEquals(report.replaceAll("\\s+", ""),
                "[{\"moduleName\":\"cassandra\",\"healthStatus\":\"UP\","
                        + "\"description\":\"all-nodes-reachable\"}]");
    }

    @Test
    public void testGetHealthStatusReportSerialisesAnEmptyStatusToAnEmptyJsonArray() {
        when(healthManager.getHealthStatus(context))
                .thenReturn(Collections.<HealthInfo>emptyList());

        Assert.assertEquals(adaptor.getHealthStatusReport(context), "[]");
    }

    @Test
    public void testGetVersionIsUnavailableWhenNotLoadedFromThePackagedJar() {
        // getVersion reads the jar manifest's Implementation-Version, which maven-jar-plugin writes
        // into the artifact only; under surefire the classes come from target/classes.
        Assert.assertNull(adaptor.getVersion());
    }

    @Test
    public void testHealthStatusFailurePropagatesUnwrappedToTheCaller() {
        ZusammenException failure = new ZusammenException(new ReturnCode(
                ErrorCode.HC_MISSING_PLUGIN, Module.ZHC, "no-state-store-plugin", null));
        when(healthManager.getHealthStatus(context)).thenThrow(failure);

        try {
            adaptor.getHealthStatus(context);
            Assert.fail("expected the manager failure to reach the caller");
        } catch (ZusammenException e) {
            Assert.assertSame(e, failure);
            Assert.assertEquals(e.getReturnCode().toString().split("\\R", 2)[0],
                    "ZHC-" + ErrorCode.HC_MISSING_PLUGIN + "-no-state-store-plugin");
        }
    }

    private void restoreProductionFactory() throws ClassNotFoundException {
        // There is no API to drop a registry entry, so the production mapping is put back by name -
        // zusammen-core-impl is a runtime scoped dependency, so it has no compile time class literal.
        AbstractFactoryBase.registerFactory(HealthManagerFactory.class,
                Class.forName(PRODUCTION_FACTORY, false, HealthManagerFactory.class.getClassLoader())
                        .asSubclass(HealthManagerFactory.class));
    }
}
