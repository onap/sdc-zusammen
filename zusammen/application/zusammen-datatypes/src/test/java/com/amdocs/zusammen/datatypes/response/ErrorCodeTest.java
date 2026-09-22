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

import org.testng.Assert;
import org.testng.annotations.Test;

public class ErrorCodeTest {

  @Test
  public void testGetErrorCode() {
    Assert.assertEquals(new ErrorCode(ErrorCode.ZU_ITEM_CREATE, Module.ZDB).getErrorCode(),
        ErrorCode.ZU_ITEM_CREATE);
  }

  @Test
  public void testSetErrorCode() {
    ErrorCode errorCode = new ErrorCode(ErrorCode.ZU_ITEM_CREATE, Module.ZDB);
    errorCode.setErrorCode(ErrorCode.ZU_ITEM_DELETE);
    Assert.assertEquals(errorCode.getErrorCode(), ErrorCode.ZU_ITEM_DELETE);
  }

  /**
   * {@code module} has no getter, so {@code toString} is the only way to observe which module an
   * error code was built for.
   */
  @Test
  public void testToStringJoinsModuleNameAndCode() {
    Assert.assertEquals(new ErrorCode(12345, Module.ZCSP).toString(), "ZCSP-12345");
  }

  @Test
  public void testToStringUsesTheModuleNameNotItsDescription() {
    Assert.assertEquals(new ErrorCode(ErrorCode.SYSTEM_ERROR, Module.ZHC).toString(), "ZHC-10000");
  }

  @Test
  public void testToStringReflectsAnUpdatedErrorCode() {
    ErrorCode errorCode = new ErrorCode(1, Module.ZMDP);
    errorCode.setErrorCode(2);
    Assert.assertEquals(errorCode.toString(), "ZMDP-2");
  }
}
