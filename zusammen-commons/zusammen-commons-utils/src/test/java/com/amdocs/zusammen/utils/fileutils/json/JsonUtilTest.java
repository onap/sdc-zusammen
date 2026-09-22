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

package com.amdocs.zusammen.utils.fileutils.json;

import com.amdocs.zusammen.utils.fileutils.FileUtils;
import com.google.gson.reflect.TypeToken;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JsonUtilTest {

    public static class Payload {
        private String name;
        private int count;
        private boolean enabled;
        private List<String> tags;

        Payload() {
        }

        Payload(String name, int count, boolean enabled, List<String> tags) {
            this.name = name;
            this.count = count;
            this.enabled = enabled;
            this.tags = tags;
        }
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testObject2JsonIsPrettyPrinted() {
        String json = JsonUtil.object2Json(new Payload("alpha", 7, true, Arrays.asList("x", "y")));

        Assert.assertTrue(json.contains("\n"), json);
        Assert.assertTrue(json.contains("\"name\": \"alpha\""), json);
        Assert.assertTrue(json.contains("\"count\": 7"), json);
    }

    @Test
    public void testObject2JsonOfNull() {
        Assert.assertEquals(JsonUtil.object2Json(null), "null");
    }

    @Test
    public void testJson2ObjectFromStringKeepsEveryField() {
        Payload source = new Payload("alpha", 7, true, Arrays.asList("x", "y"));

        Payload restored = JsonUtil.json2Object(JsonUtil.object2Json(source), Payload.class);

        Assert.assertEquals(restored.name, "alpha");
        Assert.assertEquals(restored.count, 7);
        Assert.assertTrue(restored.enabled);
        Assert.assertEquals(restored.tags, Arrays.asList("x", "y"));
    }

    @Test
    public void testJson2ObjectFromStringWithGenericType() {
        Map<String, String> map = JsonUtil.json2Object("{\"a\":\"1\",\"b\":\"2\"}",
                new TypeToken<Map<String, String>>() {
                }.getType());

        Assert.assertEquals(map.size(), 2);
        Assert.assertEquals(map.get("a"), "1");
        Assert.assertEquals(map.get("b"), "2");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testJson2ObjectFromStringFailsOnMalformedJson() {
        JsonUtil.json2Object("{\"name\": ", Payload.class);
    }

    @Test
    public void testJson2ObjectFromInputStreamKeepsEveryField() {
        Payload source = new Payload("beta", -3, false, Arrays.asList("only"));

        Payload restored =
                JsonUtil.json2Object(stream(JsonUtil.object2Json(source)), Payload.class);

        Assert.assertEquals(restored.name, "beta");
        Assert.assertEquals(restored.count, -3);
        Assert.assertFalse(restored.enabled);
        Assert.assertEquals(restored.tags, Arrays.asList("only"));
    }

    @Test
    public void testJson2ObjectFromInputStreamWithGenericType() {
        Map<String, String> map = JsonUtil.json2Object(stream("{\"a\":\"1\"}"),
                new TypeToken<Map<String, String>>() {
                }.getType());

        Assert.assertEquals(map.get("a"), "1");
    }

    @Test
    public void testJson2ObjectClosesTheInputStream() throws IOException {
        final boolean[] closed = new boolean[1];
        InputStream is = new ByteArrayInputStream("{\"a\":\"1\"}".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };

        JsonUtil.json2Object(is, Map.class);

        Assert.assertTrue(closed[0]);
    }

    @Test
    public void testJson2ObjectReadsAClasspathResource() {
        Map<String, String> map = JsonUtil.json2Object(
                FileUtils.getFileInputStream("zusammen-commons-utils-test-fixture.json"),
                new TypeToken<Map<String, String>>() {
                }.getType());

        Assert.assertEquals(map.get("alpha"), "1");
        Assert.assertEquals(map.get("beta"), "2");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testJson2ObjectFromInputStreamFailsOnNullStream() {
        JsonUtil.json2Object((InputStream) null, Map.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testJson2ObjectFromInputStreamFailsOnMalformedJson() {
        JsonUtil.json2Object(stream("{\"name\": "), Payload.class);
    }

    @Test
    public void testInputStream2JsonNormalisesToPrettyPrintedJson() {
        String json = JsonUtil.inputStream2Json(stream("{\"a\":\"1\"}"));

        Assert.assertEquals(json.replaceAll("\\s", ""), "{\"a\":\"1\"}");
        Assert.assertTrue(json.contains("\n"), json);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testInputStream2JsonFailsOnNullInput() {
        JsonUtil.inputStream2Json(null);
    }

    @Test
    public void testIsValidJsonAcceptsObject() {
        Assert.assertTrue(JsonUtil.isValidJson("{\"a\":\"1\"}"));
    }

    @Test
    public void testIsValidJsonRejectsArray() {
        Assert.assertFalse(JsonUtil.isValidJson("[1,2]"));
    }

    @Test
    public void testIsValidJsonRejectsMalformedJson() {
        Assert.assertFalse(JsonUtil.isValidJson("{\"a\":"));
    }
}
