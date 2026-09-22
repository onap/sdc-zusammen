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
package com.amdocs.zusammen.commons.log.impl;

import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ZusammenSLF4JLoggerImplTest {

    private static final String MESSAGE = "element 5f2c was not found";
    private static final String FORMAT = "element {} of item {} was not found";
    private static final String FIRST_ARGUMENT = "5f2c";
    private static final String SECOND_ARGUMENT = "a41b";

    /**
     * Wrapping the arguments keeps the verification bound to slf4j's {@code (String, Object...)}
     * overload, which is the one the wrapper calls. Spelling them out as two arguments would bind
     * to {@code (String, Object, Object)} instead and never match.
     */
    private static final Object[] ARGUMENTS = new Object[] {FIRST_ARGUMENT, SECOND_ARGUMENT};

    private Logger slf4jLogger;
    private ZusammenSLF4JLoggerImpl logger;
    private RuntimeException cause;

    @BeforeMethod
    public void setUp() {
        slf4jLogger = mock(Logger.class);
        logger = new ZusammenSLF4JLoggerImpl(slf4jLogger);
        cause = new RuntimeException("element store is unreachable");
    }

    /**
     * Every test verifies exactly one call, so this also proves no other slf4j level was touched.
     */
    @AfterMethod
    public void tearDown() {
        verifyNoMoreInteractions(slf4jLogger);
        slf4jLogger = null;
        logger = null;
        cause = null;
    }

    @Test
    public void testDebugMessageReachesSlf4jAtDebugLevel() {
        logger.debug(MESSAGE);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(slf4jLogger).debug(message.capture());
        Assert.assertEquals(message.getValue(), MESSAGE);
    }

    @Test
    public void testDebugFormatAndArgumentsReachSlf4jUnformatted() {
        logger.debug(FORMAT, FIRST_ARGUMENT, SECOND_ARGUMENT);

        verify(slf4jLogger).debug(FORMAT, ARGUMENTS);
    }

    @Test
    public void testDebugThrowableReachesSlf4jAtDebugLevel() {
        logger.debug(MESSAGE, cause);

        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        verify(slf4jLogger).debug(eq(MESSAGE), throwable.capture());
        Assert.assertSame(throwable.getValue(), cause);
    }

    @Test
    public void testErrorMessageReachesSlf4jAtErrorLevel() {
        logger.error(MESSAGE);

        verify(slf4jLogger).error(MESSAGE);
    }

    @Test
    public void testErrorFormatAndArgumentsReachSlf4jUnformatted() {
        logger.error(FORMAT, FIRST_ARGUMENT, SECOND_ARGUMENT);

        verify(slf4jLogger).error(FORMAT, ARGUMENTS);
    }

    @Test
    public void testErrorThrowableReachesSlf4jAtErrorLevel() {
        logger.error(MESSAGE, cause);

        verify(slf4jLogger).error(MESSAGE, cause);
    }

    @Test
    public void testInfoMessageReachesSlf4jAtInfoLevel() {
        logger.info(MESSAGE);

        verify(slf4jLogger).info(MESSAGE);
    }

    @Test
    public void testInfoFormatAndArgumentsReachSlf4jUnformatted() {
        logger.info(FORMAT, FIRST_ARGUMENT, SECOND_ARGUMENT);

        verify(slf4jLogger).info(FORMAT, ARGUMENTS);
    }

    @Test
    public void testInfoThrowableReachesSlf4jAtInfoLevel() {
        logger.info(MESSAGE, cause);

        verify(slf4jLogger).info(MESSAGE, cause);
    }

    @Test
    public void testTraceMessageReachesSlf4jAtTraceLevel() {
        logger.trace(MESSAGE);

        verify(slf4jLogger).trace(MESSAGE);
    }

    @Test
    public void testTraceFormatAndArgumentsReachSlf4jUnformatted() {
        logger.trace(FORMAT, FIRST_ARGUMENT, SECOND_ARGUMENT);

        verify(slf4jLogger).trace(FORMAT, ARGUMENTS);
    }

    @Test
    public void testTraceThrowableReachesSlf4jAtTraceLevel() {
        logger.trace(MESSAGE, cause);

        verify(slf4jLogger).trace(MESSAGE, cause);
    }

    @Test
    public void testWarnMessageReachesSlf4jAtWarnLevel() {
        logger.warn(MESSAGE);

        verify(slf4jLogger).warn(MESSAGE);
    }

    @Test
    public void testWarnFormatAndArgumentsReachSlf4jUnformatted() {
        logger.warn(FORMAT, FIRST_ARGUMENT, SECOND_ARGUMENT);

        verify(slf4jLogger).warn(FORMAT, ARGUMENTS);
    }

    @Test
    public void testWarnThrowableReachesSlf4jAtWarnLevel() {
        logger.warn(MESSAGE, cause);

        verify(slf4jLogger).warn(MESSAGE, cause);
    }

    @Test
    public void testSingleFormatArgumentIsForwardedAsAOneElementArray() {
        logger.warn(FORMAT, FIRST_ARGUMENT);

        verify(slf4jLogger).warn(FORMAT, new Object[] {FIRST_ARGUMENT});
    }
}
