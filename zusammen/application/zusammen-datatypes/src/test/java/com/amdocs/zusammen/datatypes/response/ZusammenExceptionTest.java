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

public class ZusammenExceptionTest {

  private static ReturnCode failure() {
    return new ReturnCode(ErrorCode.ZU_ITEM_VERSION_PUBLISH, Module.ZDB, "publish failed", null);
  }

  @Test
  public void testConstructorKeepsTheReturnCode() {
    ReturnCode returnCode = failure();
    Assert.assertSame(new ZusammenException(returnCode).getReturnCode(), returnCode);
  }

  @Test
  public void testSetReturnCode() {
    ZusammenException exception = new ZusammenException(failure());
    ReturnCode replacement =
        new ReturnCode(ErrorCode.ZU_ITEM_VERSION_SYNC, Module.ZCSM, "sync failed", null);
    exception.setReturnCode(replacement);
    Assert.assertSame(exception.getReturnCode(), replacement);
  }

  @Test
  public void testIsUnchecked() {
    Assert.assertTrue(new ZusammenException(failure()) instanceof RuntimeException);
  }

  @Test
  public void testReturnCodeSurvivesThrowAndCatch() {
    ReturnCode returnCode = failure();
    try {
      throw new ZusammenException(returnCode);
    } catch (ZusammenException caught) {
      Assert.assertSame(caught.getReturnCode(), returnCode);
      Assert.assertEquals(caught.getReturnCode().getMessage(), "publish failed");
    }
  }

  @Test
  public void testReturnCodeMayBeNull() {
    Assert.assertNull(new ZusammenException(null).getReturnCode());
  }
}
