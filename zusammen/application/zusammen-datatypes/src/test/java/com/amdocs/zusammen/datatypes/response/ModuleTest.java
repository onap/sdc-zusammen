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

package com.amdocs.zusammen.datatypes.response;

import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ModuleTest {

  @Test
  public void testValues() {
    Assert.assertEquals(Module.values(), new Module[] {Module.ZCSP, Module.ZSIP, Module.ZMDP,
        Module.ZDB, Module.ZCSM, Module.ZSTM, Module.ZSIM, Module.ZHC});
  }

  @Test
  public void testGetDescription() {
    Assert.assertEquals(Module.ZCSP.getDescription(), "Zusammen Collaboration Store Plugin");
    Assert.assertEquals(Module.ZSIP.getDescription(), "Zusammen Search Index Plugin");
    Assert.assertEquals(Module.ZMDP.getDescription(), "Zusammen Metadata Plugin");
    Assert.assertEquals(Module.ZDB.getDescription(), "Zusammen Database (core)");
    Assert.assertEquals(Module.ZCSM.getDescription(), "Zusammen Collaboration Store Middleware");
    Assert.assertEquals(Module.ZSTM.getDescription(), "Zusammen State (Metadata) Middleware");
    Assert.assertEquals(Module.ZSIM.getDescription(), "Zusammen Search Index Middleware");
    Assert.assertEquals(Module.ZHC.getDescription(), "Zusammen Health Check");
  }

  @Test
  public void testDescriptionsAreDistinct() {
    Set<String> descriptions = new HashSet<>();
    for (Module module : Module.values()) {
      Assert.assertTrue(descriptions.add(module.getDescription()),
          "duplicate description: " + module.getDescription());
    }
  }

  @Test
  public void testValueOfEachName() {
    for (Module module : Module.values()) {
      Assert.assertSame(Module.valueOf(module.name()), module);
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testValueOfUnknownName() {
    Module.valueOf("ZZZ");
  }
}
