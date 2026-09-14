/*
 * Copyright © 2016-2017 European Support Limited
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

import com.amdocs.zusammen.utils.common.CommonMethods;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.amdocs.zusammen.utils.common.CommonMethods.isEmpty;
import static com.amdocs.zusammen.utils.common.CommonMethods.newInstance;


public abstract class AbstractFactoryBase {

  /**
   * Temporary registry of default implementations. The map keeps class names rather then class
   * types to allow unloading of those classes from memory by garbage collector if factory is not
   * actually used.
   */
  private static Map<String, String> registry = new ConcurrentHashMap<>();

  /**
   * Cached factory instances.
   */
  private static Map<String, AbstractFactoryBase> factoryMap =
      new ConcurrentHashMap<>();

  /**
   * Registers implementor for an abstract factory. The method accepts Java classes rather then
   * class names to ensure type safety at compilation time.
   *
   * @param <I>     Java interface type instantiated by abstract factory
   * @param <F>     Type specific abstract factory for concrete Java interface
   * @param factory Java class of a type specific abstract factory
   * @param impl    Java class of type specific factory implementor
   */
  public static <I, F extends AbstractFactoryBase> void registerFactory(Class<F> factory,
                                                                        Class<? extends F> impl) {
    if (factory == null) {
      throw new RuntimeException("System Error - Mandatory input factory missing.");

    }

    if (impl == null) {
      throw new RuntimeException("System Error - Mandatory input factory impl missing.");

    }
    if (factoryMap != null && factoryMap.containsKey(factory.getName())) {
      factoryMap.remove(factory.getName());
    }
    registry.put(factory.getName(), impl.getName());
  } // registerFactory

  // TODO: Remove
  protected static void registerFactory(String factoryName, String implName) {
    registry.put(factoryName, implName);
  } // registerFactory

  public static <F extends AbstractFactoryBase> void unregisterFactory(Class<F> factory) {
    if (factory == null) {
      throw new RuntimeException("System Error - Mandatory input factory missing.");

    }
    if (factoryMap != null) {
      factoryMap.remove(factory.getName());
    }
  }

  /**
   * Instantiates the configured implementation of an abstract factory.
   *
   * @param <I>         Java interface type instantiated by abstract factory
   * @param <F>         Type specific abstract factory for concrete Java interface
   * @param factoryType Java class of type specific abstract factory
   * @return Instance of implementation class
   */
  @SuppressWarnings("unchecked")
  public static <I, F extends AbstractFactoryBase> F getInstance(Class<F> factoryType) {
    if (factoryType == null) {
      throw new RuntimeException("System Error - Mandatory input factory type missing.");


    }
    // Pick up factory instance from cache
    F factory = (F) factoryMap.get(factoryType.getName());
    // Check for the first time access
    if (factory == null) {
      // Synchronize factory instantiation
      synchronized (factoryType) {
        // Re-check the factory instance
        factory = (F) factoryMap.get(factoryType.getName());
        if (factory == null) {
          // Get the implementation class name
          String implName = registry.get(factoryType.getName());

          if (CommonMethods.isEmpty(implName)) {
            throw new RuntimeException(
                "System Error - Mandatory input factory implementation missing.");

          }

          factory = CommonMethods.newInstance(implName, factoryType);

          factory.init();

          // Cache the instantiated singleton
          factoryMap.put(factoryType.getName(), factory);
        }
      }
    }

    return factory;

  } // getInstance


  public static <F extends AbstractFactoryBase> boolean isFactoryRegistered(Class<F> factoryType) {
    boolean isFactoryRegistered = false;
    if (factoryType == null) {
      throw new RuntimeException("system Error - Mandatory input factory type missing.");
    }
    // Pick up factory instance from cache
    F factory = (F) factoryMap.get(factoryType.getName());
    // Check for the first time access
    if (factory != null) {
      isFactoryRegistered = true;
    } else {
      // Get the implementation class name
      String implName = registry.get(factoryType.getName());
      if (!CommonMethods.isEmpty(implName)) {
        isFactoryRegistered = true;
      }
    }
    return isFactoryRegistered;
  }


  protected void init() {
  }

  protected void stop() {
  }

  static public void stopAll() {
    Collection<AbstractFactoryBase> factorylist = factoryMap.values();
    for (AbstractFactoryBase factory : factorylist) {
      factory.stop();
    }
  }

}
