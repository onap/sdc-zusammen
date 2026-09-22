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

package com.amdocs.zusammen.utils.fileutils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class FileContentHandlerTest {

    private static byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private static String contentOf(FileContentHandler handler, String fileName) {
        return new String(FileUtils.toByteArray(handler.getFileContent(fileName)),
                StandardCharsets.UTF_8);
    }

    @Test
    public void testAddFileFromByteArray() {
        FileContentHandler handler = new FileContentHandler();

        handler.addFile("a.txt", bytes("alpha"));

        Assert.assertEquals(contentOf(handler, "a.txt"), "alpha");
    }

    @Test
    public void testAddFileFromInputStream() {
        FileContentHandler handler = new FileContentHandler();

        handler.addFile("a.txt", new ByteArrayInputStream(bytes("alpha")));

        Assert.assertEquals(contentOf(handler, "a.txt"), "alpha");
    }

    @Test
    public void testGetFileContentOfUnknownFileIsNull() {
        Assert.assertNull(new FileContentHandler().getFileContent("absent.txt"));
    }

    @Test
    public void testGetFileContentOfEmptyFileIsNull() {
        FileContentHandler handler = new FileContentHandler();
        handler.addFile("empty.txt", new byte[0]);

        Assert.assertNull(handler.getFileContent("empty.txt"));
        Assert.assertTrue(handler.containsFile("empty.txt"));
    }

    @Test
    public void testContainsFile() {
        FileContentHandler handler = new FileContentHandler();
        handler.addFile("a.txt", bytes("alpha"));

        Assert.assertTrue(handler.containsFile("a.txt"));
        Assert.assertFalse(handler.containsFile("b.txt"));
    }

    @Test
    public void testRemove() {
        FileContentHandler handler = new FileContentHandler();
        handler.addFile("a.txt", bytes("alpha"));

        handler.remove("a.txt");

        Assert.assertFalse(handler.containsFile("a.txt"));
        Assert.assertTrue(handler.getFileList().isEmpty());
    }

    @Test
    public void testGetFileList() {
        FileContentHandler handler = new FileContentHandler();
        handler.addFile("a.txt", bytes("alpha"));
        handler.addFile("b.txt", bytes("beta"));

        Assert.assertEquals(handler.getFileList(), new HashSet<>(Arrays.asList("a.txt", "b.txt")));
    }

    @Test
    public void testSetFilesCopiesContentFromAnotherHandler() {
        FileContentHandler source = new FileContentHandler();
        source.addFile("a.txt", bytes("alpha"));
        source.addFile("b.txt", bytes("beta"));
        FileContentHandler target = new FileContentHandler();

        target.setFiles(source);

        Assert.assertEquals(target.getFileList(), source.getFileList());
        Assert.assertEquals(contentOf(target, "a.txt"), "alpha");
        Assert.assertEquals(contentOf(target, "b.txt"), "beta");
    }

    @Test
    public void testSetFilesTurnsEmptyContentIntoEmptyByteArray() {
        FileContentHandler source = new FileContentHandler();
        source.addFile("empty.txt", new byte[0]);
        FileContentHandler target = new FileContentHandler();

        target.setFiles(source);

        Assert.assertTrue(target.containsFile("empty.txt"));
        Assert.assertNull(target.getFileContent("empty.txt"));
    }

    @Test
    public void testAddAllMergesAndOverwrites() {
        FileContentHandler target = new FileContentHandler();
        target.addFile("a.txt", bytes("alpha"));
        target.addFile("shared.txt", bytes("mine"));
        FileContentHandler other = new FileContentHandler();
        other.addFile("b.txt", bytes("beta"));
        other.addFile("shared.txt", bytes("theirs"));

        target.addAll(other);

        Assert.assertEquals(target.getFileList(),
                new HashSet<>(Arrays.asList("a.txt", "b.txt", "shared.txt")));
        Assert.assertEquals(contentOf(target, "shared.txt"), "theirs");
    }

    @Test
    public void testPutAllMakesTheGivenContentAvailable() {
        FileContentHandler handler = new FileContentHandler();
        Map<String, byte[]> files = new HashMap<>();
        files.put("new.txt", bytes("new"));

        handler.putAll(files);

        Assert.assertEquals(contentOf(handler, "new.txt"), "new");
    }
}
