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

package com.amdocs.zusammen.utils.facade.api;


import com.amdocs.zusammen.utils.common.CommonMethods;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;
import com.amdocs.zusammen.utils.facade.impl.FactoryConfig;

import java.util.Map;

public abstract class AbstractComponentFactory<I> extends AbstractFactory<I> {

  private static class RegistryImpl implements Registry {
    @Override
    public void register(String factory, String impl) {
      AbstractFactoryBase.registerFactory(factory, impl);
    }
  }

  static {
    Registry registry = new RegistryImpl();
    InitializationHelper.registerFactoryMapping(registry);
  }

  interface Registry {
    void register(String factory, String impl);
  }

  static class InitializationHelper {


    private static boolean isRegistered = false;

    private InitializationHelper() {
    }

    static synchronized boolean registerFactoryMapping(Registry registry) {

      boolean done = !isRegistered;

      if (!isRegistered) {
        registerFactoryMappingImpl(registry);
        isRegistered = true;
      }

      return done;
    }

    private static void registerFactoryMappingImpl(Registry registry) {
      Map<String, String> factoryMap = FactoryConfig.getFactoriesMap();

      try {
        for (Map.Entry<String, String> entry : factoryMap.entrySet()) {
          String abstractClassName = entry.getKey();
          //Class<?> tempAbstract = Class.forName(abstractClassName, false, InitializationHelper.class.getClassLoader());

                    /*if (!AbstractComponentFactory.class.isAssignableFrom(tempAbstract)) {
                        throw new CoreException(
                                new ErrorCode.ErrorCodeBuilder().withID("E0002").withMessage("Unexpected type:"+tempAbstract.getClass().getName()+".").withCategory(ErrorCategory.SYSTEM).build());

                    }*/

          //Class<? extends AbstractFactoryBase> abstractType = unsecureCast(tempAbstract);

          String concreteTypeName = entry.getValue();

          if (CommonMethods.isEmpty(concreteTypeName)) {
            throw new RuntimeException(
                "System Error - Missing configuration value:" + concreteTypeName + ".");


          }

          //Class<?> tempConcrete = Class.forName(concreteTypeName, false, InitializationHelper.class.getClassLoader());

                    /*if (!abstractType.isAssignableFrom(tempConcrete)) {
                        throw new CoreException(
                                new ErrorCode.ErrorCodeBuilder().withID("E0003").withMessage("Incompatible types: specified type["+nameOf(tempConcrete)+"] type["+nameOf(abstractType)+"].").withCategory(ErrorCategory.SYSTEM).build());
                    }*/

          registry.register(abstractClassName, concreteTypeName);
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> unsecureCast(Class<?> cls) {
      return (Class<T>) cls;
    }

    private static String nameOf(Class<?> clazz) {
      return (clazz != null) ? clazz.getName() : "null";
    }
  }

}
