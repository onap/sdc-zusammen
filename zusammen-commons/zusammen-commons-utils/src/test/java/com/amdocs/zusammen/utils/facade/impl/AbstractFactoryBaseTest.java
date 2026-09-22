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

package com.amdocs.zusammen.utils.facade.impl;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AbstractFactoryBaseTest {

    public abstract static class SampleFactory extends AbstractFactoryBase {
        public abstract String label();
    }

    public static class FirstImpl extends SampleFactory {
        static int initCalls;
        static int stopCalls;

        @Override
        protected void init() {
            initCalls++;
        }

        @Override
        protected void stop() {
            stopCalls++;
        }

        @Override
        public String label() {
            return "first";
        }
    }

    public static class SecondImpl extends SampleFactory {
        @Override
        public String label() {
            return "second";
        }
    }

    public abstract static class OtherFactory extends AbstractFactoryBase {
    }

    public static class OtherImpl extends OtherFactory {
    }

    private FactoryStaticState state;

    @BeforeMethod
    public void isolateStaticFactoryState() {
        state = FactoryStaticState.capture();
        FactoryStaticState.clearRegistryAndCache();
        FirstImpl.initCalls = 0;
        FirstImpl.stopCalls = 0;
    }

    @AfterMethod
    public void restoreStaticFactoryState() {
        state.restore();
    }

    @Test
    public void testGetInstanceCreatesRegisteredImplementationAndInitialisesIt() {
        AbstractFactoryBase.registerFactory(SampleFactory.class, FirstImpl.class);

        SampleFactory factory = AbstractFactoryBase.getInstance(SampleFactory.class);

        Assert.assertEquals(factory.getClass(), FirstImpl.class);
        Assert.assertEquals(factory.label(), "first");
        Assert.assertEquals(FirstImpl.initCalls, 1);
    }

    @Test
    public void testGetInstanceReturnsTheSameCachedInstance() {
        AbstractFactoryBase.registerFactory(SampleFactory.class, FirstImpl.class);

        SampleFactory first = AbstractFactoryBase.getInstance(SampleFactory.class);
        SampleFactory second = AbstractFactoryBase.getInstance(SampleFactory.class);

        Assert.assertSame(second, first);
        Assert.assertEquals(FirstImpl.initCalls, 1);
    }

    @Test
    public void testRegisterFactoryDropsThePreviouslyCachedInstance() {
        AbstractFactoryBase.registerFactory(SampleFactory.class, FirstImpl.class);
        SampleFactory first = AbstractFactoryBase.getInstance(SampleFactory.class);

        AbstractFactoryBase.registerFactory(SampleFactory.class, SecondImpl.class);
        SampleFactory second = AbstractFactoryBase.getInstance(SampleFactory.class);

        Assert.assertEquals(second.label(), "second");
        Assert.assertNotSame(second, first);
    }

    @Test
    public void testRegisterFactoryByNameIsHonouredByGetInstance() {
        AbstractFactoryBase.registerFactory(SampleFactory.class.getName(), FirstImpl.class.getName());

        Assert.assertEquals(AbstractFactoryBase.getInstance(SampleFactory.class).label(), "first");
    }

    @Test
    public void testRegisterFactoryFailsWhenFactoryIsNull() {
        try {
            AbstractFactoryBase.registerFactory(null, FirstImpl.class);
            Assert.fail("expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(), "System Error - Mandatory input factory missing.");
        }
    }

    @Test
    public void testRegisterFactoryFailsWhenImplIsNull() {
        try {
            AbstractFactoryBase.registerFactory(SampleFactory.class, null);
            Assert.fail("expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(),
                    "System Error - Mandatory input factory impl missing.");
        }
    }

    @Test
    public void testGetInstanceFailsWhenFactoryTypeIsNull() {
        try {
            AbstractFactoryBase.getInstance(null);
            Assert.fail("expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(),
                    "System Error - Mandatory input factory type missing.");
        }
    }

    @Test
    public void testGetInstanceFailsWhenNoImplementationIsRegistered() {
        try {
            AbstractFactoryBase.getInstance(SampleFactory.class);
            Assert.fail("expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(),
                    "System Error - Mandatory input factory implementation missing.");
        }
    }

    @Test
    public void testUnregisterFactoryFailsWhenFactoryIsNull() {
        try {
            AbstractFactoryBase.unregisterFactory(null);
            Assert.fail("expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(), "System Error - Mandatory input factory missing.");
        }
    }

    @Test
    public void testUnregisterFactoryDropsTheInstanceButKeepsTheRegistration() {
        AbstractFactoryBase.registerFactory(SampleFactory.class, FirstImpl.class);
        SampleFactory first = AbstractFactoryBase.getInstance(SampleFactory.class);

        AbstractFactoryBase.unregisterFactory(SampleFactory.class);

        Assert.assertTrue(AbstractFactoryBase.isFactoryRegistered(SampleFactory.class));
        Assert.assertNotSame(AbstractFactoryBase.getInstance(SampleFactory.class), first);
        Assert.assertEquals(FirstImpl.initCalls, 2);
    }

    @Test
    public void testIsFactoryRegisteredFailsWhenFactoryTypeIsNull() {
        try {
            AbstractFactoryBase.isFactoryRegistered(null);
            Assert.fail("expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(),
                    "system Error - Mandatory input factory type missing.");
        }
    }

    @Test
    public void testIsFactoryRegisteredIsFalseForAnUnknownFactory() {
        Assert.assertFalse(AbstractFactoryBase.isFactoryRegistered(SampleFactory.class));
    }

    @Test
    public void testIsFactoryRegisteredDoesNotInstantiateTheImplementation() {
        AbstractFactoryBase.registerFactory(SampleFactory.class, FirstImpl.class);

        Assert.assertTrue(AbstractFactoryBase.isFactoryRegistered(SampleFactory.class));
        Assert.assertEquals(FirstImpl.initCalls, 0);
    }

    @Test
    public void testIsFactoryRegisteredIsTrueForAnAlreadyInstantiatedFactory() {
        AbstractFactoryBase.registerFactory(SampleFactory.class, FirstImpl.class);
        AbstractFactoryBase.getInstance(SampleFactory.class);

        Assert.assertTrue(AbstractFactoryBase.isFactoryRegistered(SampleFactory.class));
    }

    @Test
    public void testStopAllStopsEveryInstantiatedFactory() {
        AbstractFactoryBase.registerFactory(SampleFactory.class, FirstImpl.class);
        AbstractFactoryBase.registerFactory(OtherFactory.class, OtherImpl.class);
        AbstractFactoryBase.getInstance(SampleFactory.class);
        AbstractFactoryBase.getInstance(OtherFactory.class);

        AbstractFactoryBase.stopAll();

        Assert.assertEquals(FirstImpl.stopCalls, 1);
    }

    @Test
    public void testStopAllWithNothingInstantiated() {
        AbstractFactoryBase.registerFactory(SampleFactory.class, FirstImpl.class);

        AbstractFactoryBase.stopAll();

        Assert.assertEquals(FirstImpl.stopCalls, 0);
    }
}
