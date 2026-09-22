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
package com.amdocs.zusammen.commons.health.service;

import com.amdocs.zusammen.commons.health.data.HealthInfo;
import com.amdocs.zusammen.commons.health.data.HealthStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Iterator;

public class HealthCheckServiceTest {

    private static final String MODULE_NAME = "zusammen_collaborative_store";

    private static class CassandraHealthCheckService extends HealthCheckService<String> {

        private final HealthStatus status;
        private String seenContext;

        CassandraHealthCheckService(HealthStatus status) {
            super(MODULE_NAME);
            this.status = status;
        }

        @Override
        protected HealthInfo checkHealth(String context) {
            seenContext = context;
            return new HealthInfo(MODULE_NAME, status, "checked with " + context);
        }
    }

    @Test
    public void testGetHealthStatusWrapsTheSingleCheckResult() {
        CassandraHealthCheckService service =
                new CassandraHealthCheckService(HealthStatus.UP);

        Collection<HealthInfo> healthInfos = service.getHealthStatus("cluster-1");

        Assert.assertEquals(healthInfos.size(), 1);
        HealthInfo healthInfo = healthInfos.iterator().next();
        Assert.assertEquals(healthInfo.getModuleName(), MODULE_NAME);
        Assert.assertSame(healthInfo.getHealthStatus(), HealthStatus.UP);
        Assert.assertEquals(healthInfo.getDescription(), "checked with cluster-1");
    }

    @Test
    public void testGetHealthStatusPassesTheContextToCheckHealth() {
        CassandraHealthCheckService service =
                new CassandraHealthCheckService(HealthStatus.DOWN);

        service.getHealthStatus("cluster-2");

        Assert.assertEquals(service.seenContext, "cluster-2");
    }

    @Test
    public void testGetHealthStatusReportsADownComponent() {
        CassandraHealthCheckService service =
                new CassandraHealthCheckService(HealthStatus.DOWN);

        Iterator<HealthInfo> healthInfos = service.getHealthStatus("cluster-3").iterator();

        Assert.assertSame(healthInfos.next().getHealthStatus(), HealthStatus.DOWN);
        Assert.assertFalse(healthInfos.hasNext());
    }

    @Test
    public void testGetHealthStatusReturnsAFreshCollectionPerCall() {
        CassandraHealthCheckService service =
                new CassandraHealthCheckService(HealthStatus.UP);

        Collection<HealthInfo> first = service.getHealthStatus("cluster-4");
        Collection<HealthInfo> second = service.getHealthStatus("cluster-4");

        Assert.assertNotSame(second, first);
        first.clear();
        Assert.assertEquals(second.size(), 1);
    }
}
