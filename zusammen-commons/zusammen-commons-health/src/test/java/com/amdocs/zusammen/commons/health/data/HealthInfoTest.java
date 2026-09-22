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
package com.amdocs.zusammen.commons.health.data;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HealthInfoTest {

    private static final String MODULE_NAME = "zusammen_collaborative_store";
    private static final String DESCRIPTION = "cassandra cluster reachable";

    @Test
    public void testNoArgConstructorLeavesEveryFieldUnset() {
        HealthInfo healthInfo = new HealthInfo();

        Assert.assertNull(healthInfo.getModuleName());
        Assert.assertNull(healthInfo.getHealthStatus());
        Assert.assertNull(healthInfo.getDescription());
    }

    @Test
    public void testFullConstructorPopulatesEveryField() {
        HealthInfo healthInfo = new HealthInfo(MODULE_NAME, HealthStatus.UP, DESCRIPTION);

        Assert.assertEquals(healthInfo.getModuleName(), MODULE_NAME);
        Assert.assertSame(healthInfo.getHealthStatus(), HealthStatus.UP);
        Assert.assertEquals(healthInfo.getDescription(), DESCRIPTION);
    }

    @Test
    public void testSettersRoundTripEveryField() {
        HealthInfo healthInfo = new HealthInfo();

        healthInfo.setModuleName(MODULE_NAME);
        healthInfo.setHealthStatus(HealthStatus.DOWN);
        healthInfo.setDescription(DESCRIPTION);

        Assert.assertEquals(healthInfo.getModuleName(), MODULE_NAME);
        Assert.assertSame(healthInfo.getHealthStatus(), HealthStatus.DOWN);
        Assert.assertEquals(healthInfo.getDescription(), DESCRIPTION);
    }

    @Test
    public void testToStringRendersEveryField() {
        HealthInfo healthInfo = new HealthInfo(MODULE_NAME, HealthStatus.DOWN, DESCRIPTION);

        String rendered = healthInfo.toString();

        Assert.assertTrue(rendered.contains(MODULE_NAME), rendered);
        Assert.assertTrue(rendered.contains("DOWN"), rendered);
        Assert.assertTrue(rendered.contains(DESCRIPTION), rendered);
    }
}
