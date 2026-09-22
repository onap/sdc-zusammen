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

import com.amdocs.zusammen.core.api.health.HealthManager;
import com.amdocs.zusammen.core.impl.TestUtils;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HealthManagerFactoryImplTest {

    @Test
    public void testCreateInterfaceReturnsTheSameHealthManagerForEveryContext() {
        HealthManagerFactoryImpl factory = new HealthManagerFactoryImpl();
        SessionContext firstContext =
                TestUtils.createSessionContext(new UserInfo("user1"), "tenant1");
        SessionContext secondContext =
                TestUtils.createSessionContext(new UserInfo("user2"), "tenant2");

        HealthManager first = factory.createInterface(firstContext);
        HealthManager second = factory.createInterface(secondContext);

        Assert.assertTrue(first instanceof HealthManagerImpl);
        Assert.assertSame(second, first);
    }
}
