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

package com.amdocs.zusammen.core.impl.item;

import com.amdocs.zusammen.adaptor.outbound.api.CollaborationAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.CollaborationAdaptorFactory;
import com.amdocs.zusammen.adaptor.outbound.api.SearchIndexAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.SearchIndexAdaptorFactory;
import com.amdocs.zusammen.adaptor.outbound.api.item.ElementStateAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.item.ElementStateAdaptorFactory;
import com.amdocs.zusammen.adaptor.outbound.impl.CollaborationAdaptorFactoryImpl;
import com.amdocs.zusammen.adaptor.outbound.impl.SearchIndexAdaptorFactoryImpl;
import com.amdocs.zusammen.adaptor.outbound.impl.item.ElementStateAdaptorFactoryImpl;
import com.amdocs.zusammen.core.impl.FactoryStubs;
import com.amdocs.zusammen.datatypes.SessionContext;

/**
 * The element command factories resolve their adaptors through static {@code getInstance()} calls
 * and expose no seam of their own, so a test steers them by registering these stubs. They are
 * instantiated reflectively by name, hence public with a no-arg constructor.
 */
public class OutboundAdaptorFactoryStubs {

    public static CollaborationAdaptor collaborationAdaptor;
    public static ElementStateAdaptor elementStateAdaptor;
    public static SearchIndexAdaptor searchIndexAdaptor;

    public static void installCollaborationAdaptor(CollaborationAdaptor adaptor) {
        collaborationAdaptor = adaptor;
        FactoryStubs.install(CollaborationAdaptorFactory.class, Collaboration.class);
    }

    public static void installElementStateAdaptor(ElementStateAdaptor adaptor) {
        elementStateAdaptor = adaptor;
        FactoryStubs.install(ElementStateAdaptorFactory.class, ElementState.class);
    }

    public static void installSearchIndexAdaptor(SearchIndexAdaptor adaptor) {
        searchIndexAdaptor = adaptor;
        FactoryStubs.install(SearchIndexAdaptorFactory.class, SearchIndex.class);
    }

    public static void uninstall() {
        collaborationAdaptor = null;
        elementStateAdaptor = null;
        searchIndexAdaptor = null;
        FactoryStubs.restore(CollaborationAdaptorFactory.class,
                CollaborationAdaptorFactoryImpl.class);
        FactoryStubs.restore(ElementStateAdaptorFactory.class,
                ElementStateAdaptorFactoryImpl.class);
        FactoryStubs.restore(SearchIndexAdaptorFactory.class, SearchIndexAdaptorFactoryImpl.class);
    }

    public static class Collaboration extends CollaborationAdaptorFactory {
        @Override
        public CollaborationAdaptor createInterface(SessionContext context) {
            return collaborationAdaptor;
        }
    }

    public static class ElementState extends ElementStateAdaptorFactory {
        @Override
        public ElementStateAdaptor createInterface(SessionContext context) {
            return elementStateAdaptor;
        }
    }

    public static class SearchIndex extends SearchIndexAdaptorFactory {
        @Override
        public SearchIndexAdaptor createInterface(SessionContext context) {
            return searchIndexAdaptor;
        }
    }
}
