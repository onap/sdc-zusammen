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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * The factory registry, the factory instance cache, the merged factories configuration and the
 * component-factory "already registered" flag are all static and survive for the whole JVM. Tests
 * that touch them capture the state first and restore it afterwards, so the order in which TestNG
 * runs the test classes cannot make one class inherit another's stub.
 */
public final class FactoryStaticState {

    private static final String INITIALIZATION_HELPER =
            "com.amdocs.zusammen.utils.facade.api.AbstractComponentFactory$InitializationHelper";

    private static final Field REGISTRY = field(AbstractFactoryBase.class, "registry");
    private static final Field FACTORY_CACHE = field(AbstractFactoryBase.class, "factoryMap");
    private static final Field CONFIG_MAP = field(FactoriesConfigImpl.class, "factoryMap");
    private static final Field CONFIG_INITIALIZED = field(FactoriesConfigImpl.class, "initialized");
    private static final Field COMPONENT_REGISTERED =
            field(loadWithoutInitialising(INITIALIZATION_HELPER), "isRegistered");

    private final Map<Object, Object> registry;
    private final Map<Object, Object> factoryCache;
    private final Map<Object, Object> configMap;
    private final boolean configInitialised;
    private final boolean componentFactoryRegistered;

    private FactoryStaticState() {
        this.registry = new HashMap<>(liveRegistry());
        this.factoryCache = new HashMap<>(liveFactoryCache());
        this.configMap = new HashMap<>(liveConfigMap());
        this.configInitialised = getBoolean(CONFIG_INITIALIZED);
        this.componentFactoryRegistered = getBoolean(COMPONENT_REGISTERED);
    }

    public static FactoryStaticState capture() {
        return new FactoryStaticState();
    }

    public void restore() {
        replaceContent(liveRegistry(), registry);
        replaceContent(liveFactoryCache(), factoryCache);
        replaceContent(liveConfigMap(), configMap);
        setBoolean(CONFIG_INITIALIZED, configInitialised);
        setBoolean(COMPONENT_REGISTERED, componentFactoryRegistered);
    }

    public static void clearRegistryAndCache() {
        liveRegistry().clear();
        liveFactoryCache().clear();
    }

    public static void setFactoriesConfig(Map<String, String> factories) {
        replaceContent(liveConfigMap(), factories);
        setBoolean(CONFIG_INITIALIZED, true);
    }

    public static void resetFactoriesConfig() {
        liveConfigMap().clear();
        setBoolean(CONFIG_INITIALIZED, false);
    }

    public static void setComponentFactoryRegistered(boolean registered) {
        setBoolean(COMPONENT_REGISTERED, registered);
    }

    private static Map<Object, Object> liveRegistry() {
        return get(REGISTRY);
    }

    private static Map<Object, Object> liveFactoryCache() {
        return get(FACTORY_CACHE);
    }

    private static Map<Object, Object> liveConfigMap() {
        return get(CONFIG_MAP);
    }

    private static void replaceContent(Map<Object, Object> target, Map<?, ?> content) {
        target.clear();
        target.putAll(content);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> get(Field field) {
        try {
            return (Map<Object, Object>) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean getBoolean(Field field) {
        try {
            return field.getBoolean(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setBoolean(Field field, boolean value) {
        try {
            field.setBoolean(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Class<?> loadWithoutInitialising(String className) {
        try {
            return Class.forName(className, false, FactoryStaticState.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
