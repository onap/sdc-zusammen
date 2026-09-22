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

public class ReturnCodeTest {

  private static final String TAIL = System.lineSeparator() + "\t";

  @Test
  public void testConstructorWithoutNesting() {
    ReturnCode returnCode =
        new ReturnCode(ErrorCode.ZU_ITEM_GET, Module.ZDB, "item get failed", null);
    Assert.assertEquals(returnCode.getMessage(), "item get failed");
    Assert.assertNull(returnCode.getReturnCode());
  }

  @Test
  public void testConstructorWithNesting() {
    ReturnCode cause =
        new ReturnCode(ErrorCode.CL_ELEMENT_GET, Module.ZCSP, "plugin refused", null);
    ReturnCode returnCode =
        new ReturnCode(ErrorCode.ZU_ELEMENT_GET, Module.ZDB, "element get failed", cause);
    Assert.assertSame(returnCode.getReturnCode(), cause);
    Assert.assertEquals(returnCode.getReturnCode().getMessage(), "plugin refused");
  }

  @Test
  public void testSetMessage() {
    ReturnCode returnCode = new ReturnCode(ErrorCode.ZU_ITEM_GET, Module.ZDB, "before", null);
    returnCode.setMessage("after");
    Assert.assertEquals(returnCode.getMessage(), "after");
  }

  @Test
  public void testSetReturnCode() {
    ReturnCode returnCode = new ReturnCode(ErrorCode.ZU_ITEM_GET, Module.ZDB, "message", null);
    ReturnCode cause = new ReturnCode(ErrorCode.CL_ELEMENT_GET, Module.ZCSP, "cause", null);
    returnCode.setReturnCode(cause);
    Assert.assertSame(returnCode.getReturnCode(), cause);
  }

  /**
   * {@code errorCode} has no getter, so {@code toString} is the only way to observe the code and
   * module a return code was built with.
   */
  @Test
  public void testToStringRendersModuleCodeAndMessage() {
    ReturnCode returnCode = new ReturnCode(11500, Module.ZDB, "item get failed", null);
    Assert.assertEquals(returnCode.toString(), "ZDB-11500-item get failed" + TAIL);
  }

  @Test
  public void testToStringOmitsTheSeparatorWhenTheMessageIsNull() {
    ReturnCode returnCode = new ReturnCode(11500, Module.ZDB, null, null);
    Assert.assertEquals(returnCode.toString(), "ZDB-11500" + TAIL);
  }

  @Test
  public void testToStringAppendsTheNestedReturnCode() {
    ReturnCode cause = new ReturnCode(30700, Module.ZCSP, "plugin refused", null);
    ReturnCode returnCode = new ReturnCode(12100, Module.ZDB, "element get failed", cause);
    Assert.assertEquals(returnCode.toString(),
        "ZDB-12100-element get failed" + TAIL + "ZCSP-30700-plugin refused" + TAIL);
  }

  @Test
  public void testToStringWalksTheWholeNestedChain() {
    ReturnCode root = new ReturnCode(30700, Module.ZCSP, "root cause", null);
    ReturnCode middle = new ReturnCode(22600, Module.ZMDP, "middle", root);
    ReturnCode top = new ReturnCode(12100, Module.ZDB, "top", middle);
    Assert.assertEquals(top.toString(), "ZDB-12100-top" + TAIL + "ZMDP-22600-middle" + TAIL
        + "ZCSP-30700-root cause" + TAIL);
  }

  @Test
  public void testToStringReflectsAMessageChangedAfterConstruction() {
    ReturnCode returnCode = new ReturnCode(11500, Module.ZDB, null, null);
    returnCode.setMessage("added later");
    Assert.assertEquals(returnCode.toString(), "ZDB-11500-added later" + TAIL);
  }
}
