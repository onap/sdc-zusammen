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

package com.amdocs.zusammen.core.impl;

import com.amdocs.zusammen.commons.log.ZusammenLogger;
import com.amdocs.zusammen.commons.log.ZusammenLoggerFactory;
import com.amdocs.zusammen.datatypes.response.ErrorCode;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ValidationUtilTest {

    private static final ZusammenLogger LOGGER =
            ZusammenLoggerFactory.getLogger(ValidationUtilTest.class.getName());

    @Test
    public void testSuccessfulResponsePasses() {
        ValidationUtil.validateResponse(new Response<>("value"), LOGGER, ErrorCode.ZU_ELEMENT_GET);
    }

    @Test
    public void testFailedResponseIsWrappedInZusammenException() {
        ReturnCode cause = new ReturnCode(ErrorCode.CL_ELEMENT_GET, Module.ZCSP, "plugin down", null);

        ReturnCode returnCode = TestUtils.captureFailure(() -> ValidationUtil
                .validateResponse(new Response<String>(cause), LOGGER, ErrorCode.ZU_ELEMENT_GET));

        TestUtils.assertWrappedFailure(returnCode, ErrorCode.ZU_ELEMENT_GET, cause);
        Assert.assertNull(returnCode.getMessage());
    }

    @Test
    public void testFailedResponseIsReportedAgainstTheGivenErrorCode() {
        ReturnCode cause = new ReturnCode(ErrorCode.MD_ELEMENT_UPDATE, Module.ZMDP, "no row", null);

        ReturnCode returnCode = TestUtils.captureFailure(() -> ValidationUtil
                .validateResponse(new Response<Void>(cause), LOGGER, ErrorCode.ZU_ELEMENT_UPDATE));

        TestUtils.assertWrappedFailure(returnCode, ErrorCode.ZU_ELEMENT_UPDATE, cause);
    }
}
