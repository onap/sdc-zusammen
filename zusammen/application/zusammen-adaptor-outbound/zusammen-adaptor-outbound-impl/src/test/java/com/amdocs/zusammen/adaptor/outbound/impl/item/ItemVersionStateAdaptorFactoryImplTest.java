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

package com.amdocs.zusammen.adaptor.outbound.impl.item;

import com.amdocs.zusammen.adaptor.outbound.api.item.ItemVersionStateAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.item.ItemVersionStateAdaptorFactory;
import com.amdocs.zusammen.datatypes.SessionContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ItemVersionStateAdaptorFactoryImplTest {

    @Test
    public void testFactoryConfigurationResolvesItemVersionStateAdaptorFactoryToThisImpl() {
        Assert.assertTrue(ItemVersionStateAdaptorFactory.getInstance()
                instanceof ItemVersionStateAdaptorFactoryImpl);
    }

    @Test
    public void testEveryFactoryHandsBackTheSameStatelessAdaptor() {
        ItemVersionStateAdaptor first =
                new ItemVersionStateAdaptorFactoryImpl().createInterface(new SessionContext());
        ItemVersionStateAdaptor second =
                new ItemVersionStateAdaptorFactoryImpl().createInterface(new SessionContext());

        Assert.assertTrue(first instanceof ItemVersionStateAdaptorImpl);
        Assert.assertSame(second, first);
    }
}
