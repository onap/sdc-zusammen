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
package com.amdocs.zusammen.commons.health.datatypes;

import org.testng.Assert;
import org.testng.annotations.Test;

public class HealthInfoTest {

    private static final String COMPONENT = "zusammen_state_store";
    private static final String VERSION = "1.0.4";
    private static final String DESCRIPTION = "keyspace zusammen_dox is writable";

    @Test
    public void testNoArgConstructorLeavesEveryFieldUnset() {
        HealthInfo healthInfo = new HealthInfo();

        Assert.assertNull(healthInfo.getHealthCheckComponent());
        Assert.assertNull(healthInfo.getHealthStatus());
        Assert.assertNull(healthInfo.getVersion());
        Assert.assertNull(healthInfo.getDescription());
    }

    @Test
    public void testFullConstructorPopulatesEveryField() {
        HealthInfo healthInfo =
                new HealthInfo(COMPONENT, HealthStatus.UP, VERSION, DESCRIPTION);

        Assert.assertEquals(healthInfo.getHealthCheckComponent(), COMPONENT);
        Assert.assertSame(healthInfo.getHealthStatus(), HealthStatus.UP);
        Assert.assertEquals(healthInfo.getVersion(), VERSION);
        Assert.assertEquals(healthInfo.getDescription(), DESCRIPTION);
    }

    @Test
    public void testSettersRoundTripEveryField() {
        HealthInfo healthInfo = new HealthInfo();

        healthInfo.setHealthCheckComponent(COMPONENT);
        healthInfo.setHealthStatus(HealthStatus.DOWN);
        healthInfo.setVersion(VERSION);
        healthInfo.setDescription(DESCRIPTION);

        Assert.assertEquals(healthInfo.getHealthCheckComponent(), COMPONENT);
        Assert.assertSame(healthInfo.getHealthStatus(), HealthStatus.DOWN);
        Assert.assertEquals(healthInfo.getVersion(), VERSION);
        Assert.assertEquals(healthInfo.getDescription(), DESCRIPTION);
    }

    @Test
    public void testToStringRendersEveryField() {
        HealthInfo healthInfo =
                new HealthInfo(COMPONENT, HealthStatus.DOWN, VERSION, DESCRIPTION);

        String rendered = healthInfo.toString();

        Assert.assertTrue(rendered.contains(COMPONENT), rendered);
        Assert.assertTrue(rendered.contains("DOWN"), rendered);
        Assert.assertTrue(rendered.contains(VERSION), rendered);
        Assert.assertTrue(rendered.contains(DESCRIPTION), rendered);
    }
}
