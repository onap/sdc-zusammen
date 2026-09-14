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
import org.slf4j.Logger;

public class ZusammenSLF4JLoggerImpl implements ZusammenLogger {

  private final Logger logger;

  public ZusammenSLF4JLoggerImpl(Logger logger) {
    this.logger = logger;
  }


  public void debug(String message) {
    logger.debug(message);
  }

  public void debug(String format, Object... args) {
    logger.debug(format,args);
  }

  public void debug(String msg, Throwable t) {
    logger.debug(msg,t);
  }

  public void error(String message) {
    logger.error(message);
  }

  public void error(String format, Object... args) {
    logger.error(format,args);
  }

  public void error(String msg, Throwable t) {
    logger.error(msg,t);
  }

  public void info(String message) {
    logger.info(message);
  }

  public void info(String format, Object... args) {
    logger.info(format,args);
  }

  public void info(String msg, Throwable t) {
    logger.info(msg,t);
  }

  public void trace(String message) {
    logger.trace(message);
  }

  public void trace(String format, Object... args) {
    logger.trace(format,args);
  }

  public void trace(String msg, Throwable t) {
    logger.trace(msg,t);
  }

  public void warn(String message) {
    logger.warn(message);
  }

  public void warn(String format, Object... args) {
    logger.warn(format,args);
  }

  public void warn(String msg, Throwable t) {
    logger.warn(msg,t);
  }
}
