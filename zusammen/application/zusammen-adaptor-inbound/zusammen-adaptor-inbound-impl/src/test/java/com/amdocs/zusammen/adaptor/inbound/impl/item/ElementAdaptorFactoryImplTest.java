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

package com.amdocs.zusammen.adaptor.inbound.impl.item;

import com.amdocs.zusammen.adaptor.inbound.api.item.ElementAdaptor;
import com.amdocs.zusammen.adaptor.inbound.api.item.ElementAdaptorFactory;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ElementAdaptorFactoryImplTest {

    @Test
    public void testCreateInterfaceReturnsTheSameAdaptorForEveryContext() {
        ElementAdaptorFactoryImpl factory = new ElementAdaptorFactoryImpl();

        ElementAdaptor first = factory.createInterface(AdaptorTestSupport.sessionContext("user-1"));
        ElementAdaptor second = factory.createInterface(AdaptorTestSupport.sessionContext("user-2"));

        Assert.assertTrue(first instanceof ElementAdaptorImpl);
        Assert.assertSame(second, first);
    }

    @Test
    public void testFactoryConfigurationResolvesThisImplementation() {
        Assert.assertTrue(ElementAdaptorFactory.getInstance() instanceof ElementAdaptorFactoryImpl);
    }
}
