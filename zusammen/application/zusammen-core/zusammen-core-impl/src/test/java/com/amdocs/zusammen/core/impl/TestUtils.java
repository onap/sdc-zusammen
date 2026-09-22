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

package com.amdocs.zusammen.core.impl;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import org.testng.Assert;

import java.util.Arrays;

public class TestUtils {
  public static Info createInfo(String value) {
    Info info = new Info();
    info.setName(value);
    info.addProperty("Name", "name_" + value);
    info.addProperty("Desc", "desc_" + value);
    return info;
  }

  public static SessionContext createSessionContext(UserInfo user, String tenant) {
    SessionContext context = new SessionContext();
    context.setUser(user);
    context.setTenant(tenant);
    return context;
  }

  public static ItemVersion createItemVersion(Id id, Id baseId, String name) {
    ItemVersion version = new ItemVersion();
    version.setId(id);
    version.setBaseId(baseId);
    ItemVersionData data = new ItemVersionData();
    data.setInfo(TestUtils.createInfo(name));
    data.setRelations(Arrays.asList(new Relation(), new Relation()));
    version.setData(data);
    return version;
  }

  /**
   * Runs an action that is expected to fail and hands back the {@link ReturnCode} the resulting
   * {@link ZusammenException} carries, so the caller can assert on it.
   */
  public static ReturnCode captureFailure(Runnable action) {
    try {
      action.run();
    } catch (ZusammenException exception) {
      Assert.assertNotNull(exception.getReturnCode(), "ZusammenException without a ReturnCode");
      return exception.getReturnCode();
    }
    Assert.fail("Expected a ZusammenException but none was thrown");
    return null;
  }

  public static void assertErrorCode(ReturnCode returnCode, Module module, int errorCode) {
    // ReturnCode keeps its ErrorCode private with no getter, so toString is the only way in:
    // it starts with "<module>-<errorCode>".
    Assert.assertTrue(returnCode.toString().startsWith(module.name() + "-" + errorCode),
        "expected error code " + module.name() + "-" + errorCode + " but got " + returnCode);
  }

  /**
   * Asserts that a plugin/adaptor level failure was wrapped rather than swallowed: the core
   * error code is reported and the originating ReturnCode is kept as the cause.
   */
  public static void assertWrappedFailure(ReturnCode returnCode, int errorCode, ReturnCode cause) {
    assertErrorCode(returnCode, Module.ZDB, errorCode);
    Assert.assertSame(returnCode.getReturnCode(), cause);
  }
}
