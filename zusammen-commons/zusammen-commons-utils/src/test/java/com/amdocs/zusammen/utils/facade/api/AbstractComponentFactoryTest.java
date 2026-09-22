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

package com.amdocs.zusammen.utils.facade.api;

import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;
import com.amdocs.zusammen.utils.facade.impl.FactoryStaticState;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AbstractComponentFactoryTest {

    public abstract static class GreeterFactory extends AbstractComponentFactory<String> {
        public abstract String createInterface();
    }

    public static class GreeterFactoryImpl extends GreeterFactory {
        @Override
        public String createInterface() {
            return "greeter";
        }
    }

    private static class RecordingRegistry implements AbstractComponentFactory.Registry {
        private final Map<String, String> registered = new LinkedHashMap<>();

        @Override
        public void register(String factory, String impl) {
            registered.put(factory, impl);
        }
    }

    private FactoryStaticState state;

    @BeforeMethod
    public void isolateStaticFactoryState() {
        state = FactoryStaticState.capture();
        FactoryStaticState.clearRegistryAndCache();
        FactoryStaticState.setComponentFactoryRegistered(false);
    }

    @AfterMethod
    public void restoreStaticFactoryState() {
        state.restore();
    }

    @Test
    public void testRegisterFactoryMappingForwardsEveryConfiguredEntryToTheRegistry() {
        Map<String, String> config = new HashMap<>();
        config.put("com.example.AFactory", "com.example.AFactoryImpl");
        config.put("com.example.BFactory", "com.example.BFactoryImpl");
        FactoryStaticState.setFactoriesConfig(config);
        RecordingRegistry registry = new RecordingRegistry();

        boolean done = AbstractComponentFactory.InitializationHelper.registerFactoryMapping(registry);

        Assert.assertTrue(done);
        Assert.assertEquals(registry.registered, config);
    }

    @Test
    public void testRegisterFactoryMappingIsAppliedOnlyOnce() {
        FactoryStaticState.setFactoriesConfig(
                Collections.singletonMap("com.example.AFactory", "com.example.AFactoryImpl"));

        Assert.assertTrue(AbstractComponentFactory.InitializationHelper
                .registerFactoryMapping(new RecordingRegistry()));

        RecordingRegistry second = new RecordingRegistry();
        Assert.assertFalse(
                AbstractComponentFactory.InitializationHelper.registerFactoryMapping(second));
        Assert.assertTrue(second.registered.isEmpty());
    }

    @Test
    public void testRegisterFactoryMappingRejectsAnEntryWithoutAnImplementation() {
        FactoryStaticState.setFactoriesConfig(
                Collections.singletonMap("com.example.AFactory", ""));

        try {
            AbstractComponentFactory.InitializationHelper
                    .registerFactoryMapping(new RecordingRegistry());
            Assert.fail("expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(), "System Error - Missing configuration value:.");
        }
    }

    /**
     * The registration happens in AbstractComponentFactory's static initialiser, which the JVM runs
     * at most once — this is the only test that may load a concrete subclass, or the mapping would
     * already be in place and nothing would be observed.
     */
    @Test
    public void testLoadingAComponentFactoryWiresTheConfiguredImplementation() {
        FactoryStaticState.setFactoriesConfig(Collections.singletonMap(GreeterFactory.class.getName(),
                GreeterFactoryImpl.class.getName()));

        new GreeterFactoryImpl();

        Assert.assertTrue(AbstractFactoryBase.isFactoryRegistered(GreeterFactory.class));
        Assert.assertEquals(AbstractFactoryBase.getInstance(GreeterFactory.class).createInterface(),
                "greeter");
    }
}
