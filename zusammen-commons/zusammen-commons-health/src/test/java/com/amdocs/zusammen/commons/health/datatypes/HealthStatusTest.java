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

public class HealthStatusTest {

    @Test
    public void testUpAndDownAreTheOnlyStatuses() {
        Assert.assertEquals(HealthStatus.values().length, 2);
        Assert.assertSame(HealthStatus.valueOf("UP"), HealthStatus.UP);
        Assert.assertSame(HealthStatus.valueOf("DOWN"), HealthStatus.DOWN);
    }

    @Test
    public void testToStringIsTheWireName() {
        Assert.assertEquals(HealthStatus.UP.toString(), "UP");
        Assert.assertEquals(HealthStatus.DOWN.toString(), "DOWN");
    }

    @Test
    public void testToValueResolvesAWireName() {
        Assert.assertSame(HealthStatus.toValue("UP"), HealthStatus.UP);
        Assert.assertSame(HealthStatus.toValue("DOWN"), HealthStatus.DOWN);
    }

    @Test
    public void testToValueOfAnUnknownNameIsNull() {
        Assert.assertNull(HealthStatus.toValue("DEGRADED"));
    }

    @Test
    public void testToValueIsCaseSensitive() {
        Assert.assertNull(HealthStatus.toValue("up"));
    }

    @Test
    public void testToValueOfNullIsNull() {
        Assert.assertNull(HealthStatus.toValue(null));
    }
}
