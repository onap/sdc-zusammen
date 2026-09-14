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

package com.amdocs.zusammen.commons.log;


public interface ZusammenLogger {
  public void debug(String message);
  public void debug(String format,Object ... args);
  public void debug(String msg,Throwable t);
  public void error(String message);
  public void error(String format,Object ... args);
  public void error(String msg,Throwable t);
  public void info(String message);
  public void info(String format,Object ... args);
  public void info(String msg,Throwable t);
  public void trace(String message);
  public void trace(String format,Object ... args);
  public void trace(String msg,Throwable t);
  public void warn(String message);
  public void warn(String format,Object ... args);
  public void warn(String msg,Throwable t);



}
