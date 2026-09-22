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

import com.amdocs.zusammen.adaptor.outbound.api.SearchIndexAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.SearchIndexAdaptorFactory;
import com.amdocs.zusammen.datatypes.SessionContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SearchIndexAdaptorFactoryImplTest {

    @Test
    public void testFactoryConfigurationResolvesSearchIndexAdaptorFactoryToThisImpl() {
        Assert.assertTrue(
                SearchIndexAdaptorFactory.getInstance() instanceof SearchIndexAdaptorFactoryImpl);
    }

    @Test
    public void testEveryFactoryHandsBackTheSameStatelessAdaptor() {
        SearchIndexAdaptor first =
                new SearchIndexAdaptorFactoryImpl().createInterface(new SessionContext());
        SearchIndexAdaptor second =
                new SearchIndexAdaptorFactoryImpl().createInterface(new SessionContext());

        Assert.assertTrue(first instanceof SearchIndexAdaptorImpl);
        Assert.assertSame(second, first);
    }
}
