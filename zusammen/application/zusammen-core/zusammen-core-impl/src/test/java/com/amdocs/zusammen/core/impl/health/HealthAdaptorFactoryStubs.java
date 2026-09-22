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

import com.amdocs.zusammen.adaptor.outbound.api.health.HealthAdaptor;
import com.amdocs.zusammen.adaptor.outbound.api.health.HealthAdaptorFactory;
import com.amdocs.zusammen.adaptor.outbound.impl.health.HealthAdaptorFactoryImpl;
import com.amdocs.zusammen.core.impl.FactoryStubs;
import com.amdocs.zusammen.datatypes.SessionContext;

/**
 * The health classes reach their adaptor through {@code HealthAdaptorFactory.getInstance()}, so a
 * test steers them by registering one of these stubs. Both are instantiated reflectively by name,
 * hence public with a no-arg constructor.
 */
public class HealthAdaptorFactoryStubs {

    public static HealthAdaptor adaptor;

    public static void installAdaptor(HealthAdaptor healthAdaptor) {
        adaptor = healthAdaptor;
        FactoryStubs.install(HealthAdaptorFactory.class, Returning.class);
    }

    public static void installUnresolvableAdaptor() {
        adaptor = null;
        FactoryStubs.install(HealthAdaptorFactory.class, Failing.class);
    }

    public static void uninstall() {
        adaptor = null;
        FactoryStubs.restore(HealthAdaptorFactory.class, HealthAdaptorFactoryImpl.class);
    }

    public static class Returning extends HealthAdaptorFactory {
        @Override
        public HealthAdaptor createInterface(SessionContext context) {
            return adaptor;
        }
    }

    public static class Failing extends HealthAdaptorFactory {
        @Override
        public HealthAdaptor createInterface(SessionContext context) {
            throw new IllegalStateException("no health plugin on the classpath");
        }
    }
}
