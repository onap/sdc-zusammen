/*
 * Copyright © 2016 Amdocs Software Systems Limited
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

package com.amdocs.zusammen.commons.log.impl;

import com.amdocs.zusammen.commons.log.ZusammenLogger;
import com.amdocs.zusammen.commons.log.ZusammenLoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ZusammenSLF4JLoggerFactoryImpl extends ZusammenLoggerFactory {

  private boolean initialized = false;
  private Map<String, ZusammenLogger> loggerMap = new HashMap();

  public void init() {
    initialized = true;

  }

  @Override
  protected ZusammenLogger createInterface(String key) {
    ZusammenLogger zusammenLogger;
    synchronized (this) {

      if (initialized) {
        zusammenLogger = loggerMap.get(key);
        if (zusammenLogger == null) {
          zusammenLogger = new ZusammenSLF4JLoggerImpl(LoggerFactory.getLogger(this.getClass()));
        }
      } else {
        throw new RuntimeException("Logget must be initialized before executing logger. Run " +
            "method init()");
      }
    }
    return zusammenLogger;
  }
}
