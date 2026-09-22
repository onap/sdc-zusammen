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

package com.amdocs.zusammen.core.impl;

import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;

public class FactoryStubs {

  /**
   * Installs {@code impl} as the implementation of {@code factory}.
   *
   * <p>The class literal alone does not initialise {@code factory}, and
   * {@code AbstractComponentFactory}'s static initialiser reloads every
   * {@code factoryConfiguration.json} on the classpath into the same static registry
   * {@code registerFactory} writes to. Initialising the factory first therefore matters: otherwise
   * the first call that touches it runs that initialiser and silently discards the stub, so only
   * the first test to use the factory gets the production implementation.
   */
  public static <F extends AbstractFactoryBase> void install(Class<F> factory,
                                                             Class<? extends F> impl) {
    initialise(factory);
    AbstractFactoryBase.registerFactory(factory, impl);
  }

  /**
   * Puts the production implementation back. There is no API for removing a registry entry, so a
   * stub can only be undone by naming what it displaced.
   */
  public static <F extends AbstractFactoryBase> void restore(Class<F> factory,
                                                             Class<? extends F> productionImpl) {
    AbstractFactoryBase.registerFactory(factory, productionImpl);
    AbstractFactoryBase.unregisterFactory(factory);
  }

  private static void initialise(Class<?> factory) {
    try {
      Class.forName(factory.getName(), true, factory.getClassLoader());
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
