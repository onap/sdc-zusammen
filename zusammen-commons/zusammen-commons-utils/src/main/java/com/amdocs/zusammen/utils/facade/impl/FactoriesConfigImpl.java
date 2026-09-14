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


import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import com.amdocs.zusammen.utils.facade.api.FactoriesConfiguration;
import com.amdocs.zusammen.utils.fileutils.FileUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FactoriesConfigImpl implements FactoriesConfiguration {


  private static final String FACTORY_CONFIG_FILE_NAME = "factoryConfiguration.json";
  private static Map factoryMap = new HashMap();
  private static boolean initialized = false;

  @SuppressWarnings("unchecked")
  public Map<String, String> getFactoriesMap() {
    synchronized (this) {
      if (!initialized) {
        init();
        initialized = true;
      }
    }
    return factoryMap;
  }

  private void init() {
    List<InputStream> factoryConfigISList = FileUtils.getFileInputStreams(FACTORY_CONFIG_FILE_NAME);
    for (InputStream factoryConfigIS : factoryConfigISList) {
      factoryMap.putAll(JsonUtil.json2Object(factoryConfigIS, Map.class));
    }
  }


}

