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

import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ResponseTest {

  private static ReturnCode failure() {
    return new ReturnCode(ErrorCode.ZU_ITEM_CREATE, Module.ZDB, "item create failed", null);
  }

  @Test
  public void testValueConstructorProducesASuccessfulResponse() {
    Response<String> response = new Response<>("payload");
    Assert.assertTrue(response.isSuccessful());
    Assert.assertEquals(response.getValue(), "payload");
    Assert.assertNull(response.getReturnCode());
  }

  @Test
  public void testValueConstructorWithNullValueIsStillSuccessful() {
    Response<String> response = new Response<>((String) null);
    Assert.assertTrue(response.isSuccessful());
    Assert.assertNull(response.getValue());
  }

  @Test
  public void testReturnCodeConstructorProducesAFailedResponse() {
    ReturnCode returnCode = failure();
    Response<String> response = new Response<String>(returnCode);
    Assert.assertFalse(response.isSuccessful());
    Assert.assertSame(response.getReturnCode(), returnCode);
  }

  @Test
  public void testFailedResponseCarriesNoValue() {
    Assert.assertNull(new Response<String>(failure()).getValue());
  }

  @Test
  public void testFailedResponseExposesModuleAndErrorCodeThroughItsReturnCode() {
    Response<String> response = new Response<String>(failure());
    Assert.assertEquals(response.getReturnCode().getMessage(), "item create failed");
    Assert.assertTrue(response.getReturnCode().toString().startsWith("ZDB-11200"),
        "unexpected return code rendering: " + response.getReturnCode());
  }

  @Test
  public void testSettingAReturnCodeTurnsASuccessfulResponseIntoAFailure() {
    Response<String> response = new Response<>("payload");
    response.setReturnCode(failure());
    Assert.assertFalse(response.isSuccessful());
    Assert.assertEquals(response.getValue(), "payload");
  }

  @Test
  public void testClearingTheReturnCodeTurnsAFailedResponseIntoASuccess() {
    Response<String> response = new Response<String>(failure());
    response.setReturnCode(null);
    Assert.assertTrue(response.isSuccessful());
  }

  @Test
  public void testSetValue() {
    Response<String> response = new Response<>("before");
    response.setValue("after");
    Assert.assertEquals(response.getValue(), "after");
  }

  @Test
  public void testResponseOfACollectionValue() {
    List<String> value = Arrays.asList("a", "b");
    Response<List<String>> response = new Response<>(value);
    Assert.assertTrue(response.isSuccessful());
    Assert.assertEquals(response.getValue(), value);
  }
}
