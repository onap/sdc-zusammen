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

package com.amdocs.zusammen.utils.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommonMethodsTest {

    private static final String FIXTURE = "zusammen-commons-utils-test-fixture.txt";

    @Test
    public void testSerializeDeserializeRoundTrip() {
        ArrayList<String> original = new ArrayList<>(Arrays.asList("alpha", "beta"));

        byte[] bytes = CommonMethods.serializeObject(original);
        Serializable restored = CommonMethods.deserializeObject(bytes);

        Assert.assertEquals(restored, original);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testSerializeObjectFailsOnNonSerializableContent() {
        ArrayList<Object> holder = new ArrayList<>();
        holder.add(new Object());

        CommonMethods.serializeObject(holder);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDeserializeObjectFailsOnCorruptBytes() {
        CommonMethods.deserializeObject(new byte[]{1, 2, 3, 4});
    }

    @Test
    public void testIsEmptyObject() {
        Assert.assertTrue(CommonMethods.isEmpty((Object) null));
        Assert.assertFalse(CommonMethods.isEmpty(new Object()));
    }

    @Test
    public void testIsEmptyByteArray() {
        Assert.assertTrue(CommonMethods.isEmpty((byte[]) null));
        Assert.assertTrue(CommonMethods.isEmpty(new byte[0]));
        Assert.assertFalse(CommonMethods.isEmpty(new byte[]{7}));
    }

    @Test
    public void testIsEmptyString() {
        Assert.assertTrue(CommonMethods.isEmpty((String) null));
        Assert.assertTrue(CommonMethods.isEmpty(""));
        Assert.assertFalse(CommonMethods.isEmpty(" "));
    }

    @Test
    public void testIsEmptyObjectArray() {
        Assert.assertTrue(CommonMethods.isEmpty((Object[]) null));
        Assert.assertTrue(CommonMethods.isEmpty(new String[0]));
        Assert.assertFalse(CommonMethods.isEmpty(new String[]{"a"}));
    }

    @Test
    public void testIsEmptyCollection() {
        Assert.assertTrue(CommonMethods.isEmpty((Collection<?>) null));
        Assert.assertTrue(CommonMethods.isEmpty(new ArrayList<String>()));
        Assert.assertFalse(CommonMethods.isEmpty(Arrays.asList("a")));
    }

    @Test
    public void testIsEmptyMap() {
        Assert.assertTrue(CommonMethods.isEmpty((Map<?, ?>) null));
        Assert.assertTrue(CommonMethods.isEmpty(new HashMap<String, String>()));
        Assert.assertFalse(CommonMethods.isEmpty(Collections.singletonMap("k", "v")));
    }

    @Test
    public void testToPrimitiveOfNull() {
        Assert.assertNull(CommonMethods.toPrimitive(null));
    }

    @Test
    public void testToPrimitiveTreatsNullElementAsZero() {
        long[] result = CommonMethods.toPrimitive(new Long[]{5L, null, -3L});

        Assert.assertEquals(result, new long[]{5L, 0L, -3L});
    }

    @Test
    public void testToPrimitiveOfEmptyArray() {
        Assert.assertEquals(CommonMethods.toPrimitive(new Long[0]), new long[0]);
    }

    @Test
    public void testToArrayOfCollection() {
        String[] result = CommonMethods.toArray(Arrays.asList("a", "b"), String.class);

        Assert.assertEquals(result, new String[]{"a", "b"});
    }

    @Test
    public void testToArrayOfNullCollection() {
        String[] result = CommonMethods.toArray((Collection<String>) null, String.class);

        Assert.assertEquals(result.length, 0);
        Assert.assertEquals(result.getClass(), String[].class);
    }

    @Test
    public void testToArrayOfEmptyCollection() {
        Assert.assertEquals(CommonMethods.toArray(new ArrayList<String>(), String.class).length, 0);
    }

    @Test
    public void testNextUuidIsThirtyTwoHexDigits() {
        String uuid = CommonMethods.nextUUID();

        Assert.assertEquals(uuid.length(), 32);
        Assert.assertTrue(uuid.matches("[0-9A-F]{32}"), "not upper case hex: " + uuid);
    }

    @Test
    public void testNextUuidIsUnique() {
        Assert.assertNotEquals(CommonMethods.nextUUID(), CommonMethods.nextUUID());
    }

    @Test
    public void testConcatOfTwoPopulatedArrays() {
        Integer[] result = CommonMethods.concat(new Integer[]{1, 2}, new Integer[]{3});

        Assert.assertEquals(result, new Integer[]{1, 2, 3});
    }

    @Test
    public void testConcatReturnsRightWhenLeftIsEmpty() {
        Integer[] right = new Integer[]{3};

        Assert.assertSame(CommonMethods.concat(new Integer[0], right), right);
        Assert.assertSame(CommonMethods.concat(null, right), right);
    }

    @Test
    public void testConcatReturnsLeftWhenRightIsEmpty() {
        Integer[] left = new Integer[]{1};

        Assert.assertSame(CommonMethods.concat(left, new Integer[0]), left);
        Assert.assertSame(CommonMethods.concat(left, null), left);
    }

    @Test
    public void testConcatOfTwoNullArrays() {
        Assert.assertNull(CommonMethods.concat((Integer[]) null, (Integer[]) null));
    }

    @Test
    public void testCastToCompatibleType() {
        Object value = "text";

        Assert.assertEquals(CommonMethods.cast(value, String.class), "text");
    }

    @Test
    public void testCastOfNullReturnsNull() {
        Assert.assertNull(CommonMethods.cast(null, String.class));
    }

    @Test
    public void testCastToIncompatibleTypeNamesBothTypes() {
        try {
            CommonMethods.cast("text", Integer.class);
            Assert.fail("expected ClassCastException");
        } catch (ClassCastException e) {
            Assert.assertEquals(e.getMessage(),
                    "Failed to cast from 'java.lang.String' to 'java.lang.Integer'");
        }
    }

    @Test
    public void testNewInstanceByClassName() {
        Object instance = CommonMethods.newInstance("java.util.ArrayList");

        Assert.assertEquals(instance.getClass(), ArrayList.class);
    }

    @Test
    public void testNewInstanceByClassNameAndType() {
        List<?> instance = CommonMethods.newInstance("java.util.ArrayList", List.class);

        Assert.assertTrue(instance.isEmpty());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNewInstanceFailsOnEmptyClassName() {
        CommonMethods.newInstance("", Object.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNewInstanceFailsOnNullClassName() {
        CommonMethods.newInstance(null, Object.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNewInstanceFailsOnNullType() {
        CommonMethods.newInstance("java.util.ArrayList", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNewInstanceFailsOnUnknownClassName() {
        CommonMethods.newInstance("com.amdocs.zusammen.NoSuchClass", Object.class);
    }

    @Test
    public void testNewInstanceFailsWhenClassIsNotOfRequestedType() {
        try {
            CommonMethods.newInstance("java.lang.String", Number.class);
            Assert.fail("expected ClassCastException");
        } catch (ClassCastException e) {
            Assert.assertEquals(e.getMessage(),
                    "Failed to cast from 'java.lang.String' to 'java.lang.Number'");
        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testNewInstanceFailsOnInterface() {
        CommonMethods.newInstance(Runnable.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testNewInstanceFailsOnInaccessibleConstructor() {
        CommonMethods.newInstance(Collections.class);
    }

    @Test
    public void testGetResourcesPathReturnsDirectoryHoldingTheResource() {
        String path = CommonMethods.getResourcesPath(FIXTURE);

        Assert.assertTrue(path.endsWith("/"), path);
        Assert.assertTrue(new File(path, FIXTURE).exists(), path);
    }

    @Test
    public void testGetStackTraceOfNull() {
        Assert.assertEquals(CommonMethods.getStackTrace(null), "");
    }

    @Test
    public void testGetStackTraceContainsTypeMessageAndCaller() {
        String trace = CommonMethods.getStackTrace(new IllegalStateException("boom"));

        Assert.assertTrue(trace.startsWith("java.lang.IllegalStateException: boom"), trace);
        Assert.assertTrue(trace.contains("testGetStackTraceContainsTypeMessageAndCaller"), trace);
    }

    @Test
    public void testPrintStackTraceContainsCallingMethod() {
        String trace = CommonMethods.printStackTrace();

        Assert.assertTrue(trace.contains("CommonMethodsTest.testPrintStackTraceContainsCallingMethod"),
                trace);
        Assert.assertTrue(trace.endsWith(System.lineSeparator()), trace);
    }

    @Test
    public void testIsEqualObject() {
        Assert.assertTrue(CommonMethods.isEqualObject(null, null));
        Assert.assertTrue(CommonMethods.isEqualObject("a", "a"));
        Assert.assertFalse(CommonMethods.isEqualObject(null, "a"));
        Assert.assertFalse(CommonMethods.isEqualObject("a", null));
        Assert.assertFalse(CommonMethods.isEqualObject("a", "b"));
    }

    @Test
    public void testArrayToCommaSeparatedString() {
        Assert.assertEquals(CommonMethods.arrayToCommaSeparatedString(new String[]{"a", "b", "c"}),
                "a,b,c");
    }

    @Test
    public void testArrayToCommaSeparatedStringOfSingleElement() {
        Assert.assertEquals(CommonMethods.arrayToCommaSeparatedString(new String[]{"a"}), "a");
    }

    @Test
    public void testArrayToCommaSeparatedStringOfEmptyArray() {
        Assert.assertEquals(CommonMethods.arrayToCommaSeparatedString(new String[0]), "");
    }

    @Test
    public void testArrayToSeparatedStringWithCustomSeparator() {
        Assert.assertEquals(CommonMethods.arrayToSeparatedString(new String[]{"a", "b"}, '|'), "a|b");
    }

    @Test
    public void testCollectionToCommaSeparatedStringKeepsIterationOrder() {
        Set<String> values = new LinkedHashSet<>(Arrays.asList("first", "second"));

        Assert.assertEquals(CommonMethods.collectionToCommaSeparatedString(values), "first,second");
    }

    @Test
    public void testCollectionToCommaSeparatedStringOfEmptyCollection() {
        Assert.assertEquals(
                CommonMethods.collectionToCommaSeparatedString(new ArrayList<String>()), "");
    }

    @Test
    public void testListToSeparatedStringOfNullList() {
        Assert.assertNull(CommonMethods.listToSeparatedString(null, ','));
    }

    @Test
    public void testListToSeparatedStringOfEmptyList() {
        Assert.assertEquals(CommonMethods.listToSeparatedString(new ArrayList<String>(), ','), "");
    }

    @Test
    public void testListToSeparatedStringKeepsNullElementsAsText() {
        Assert.assertEquals(CommonMethods.listToSeparatedString(Arrays.asList("a", null), ';'),
                "a;null");
    }

    @Test
    public void testDuplicateStringWithDelimiter() {
        Assert.assertEquals(CommonMethods.duplicateStringWithDelimiter("?", ',', 3), "?,?,?");
        Assert.assertEquals(CommonMethods.duplicateStringWithDelimiter("?", ',', 1), "?");
        Assert.assertEquals(CommonMethods.duplicateStringWithDelimiter("?", ',', 0), "");
    }

    @Test
    public void testBytesToHex() {
        Assert.assertEquals(CommonMethods.bytesToHex(new byte[]{0x00, 0x0f, (byte) 0xff, 0x1a}),
                "000FFF1A");
    }

    @Test
    public void testBytesToHexOfEmptyArray() {
        Assert.assertEquals(CommonMethods.bytesToHex(new byte[0]), "");
    }

    @Test
    public void testToSingleElementSet() {
        Set<String> set = CommonMethods.toSingleElementSet("only");

        Assert.assertEquals(set.size(), 1);
        Assert.assertTrue(set.contains("only"));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testToSingleElementSetIsImmutable() {
        CommonMethods.toSingleElementSet("only").add("another");
    }

    @Test
    public void testIteratorToList() {
        Assert.assertEquals(CommonMethods.iteratorToList(Arrays.asList("a", "b")),
                Arrays.asList("a", "b"));
    }

    @Test
    public void testIteratorToListOfEmptyIterable() {
        Assert.assertTrue(CommonMethods.iteratorToList(new ArrayList<String>()).isEmpty());
    }
}
