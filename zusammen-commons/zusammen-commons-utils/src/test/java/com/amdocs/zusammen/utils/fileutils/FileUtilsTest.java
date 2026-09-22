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
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtilsTest {

    private static final String TEXT_FIXTURE = "zusammen-commons-utils-test-fixture.txt";
    private static final String TEXT_FIXTURE_CONTENT = "fixture-content";

    private File tempDir;

    @BeforeMethod
    public void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("zusammen-fileutils-test").toFile();
    }

    @AfterMethod
    public void removeTempDir() {
        FileUtils.delete(tempDir);
    }

    private File writeTempFile(String name, String content) throws IOException {
        File file = new File(tempDir, name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String read(InputStream is) {
        return new String(FileUtils.toByteArray(is), StandardCharsets.UTF_8);
    }

    @Test
    public void testGetFileInputStreamFromClasspath() {
        Assert.assertEquals(read(FileUtils.getFileInputStream(TEXT_FIXTURE)), TEXT_FIXTURE_CONTENT);
    }

    @Test
    public void testGetFileInputStreamFallsBackToFileSystem() throws IOException {
        File file = writeTempFile("plain.txt", "from-disk");

        Assert.assertEquals(read(FileUtils.getFileInputStream(file.getAbsolutePath())), "from-disk");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetFileInputStreamFailsWhenNeitherResourceNorFileExists() {
        FileUtils.getFileInputStream(new File(tempDir, "absent.txt").getAbsolutePath());
    }

    @Test
    public void testGetFileInputStreamsFindsClasspathResource() {
        List<InputStream> streams = FileUtils.getFileInputStreams(TEXT_FIXTURE);

        Assert.assertEquals(streams.size(), 1);
        Assert.assertEquals(read(streams.get(0)), TEXT_FIXTURE_CONTENT);
    }

    @Test
    public void testGetFileInputStreamsOfUnknownResourceIsEmpty() {
        Assert.assertTrue(FileUtils.getFileInputStreams("no-such-resource-anywhere.txt").isEmpty());
    }

    @Test
    public void testLoadFileToInputStreamFromClasspath() {
        Assert.assertEquals(read(FileUtils.loadFileToInputStream(TEXT_FIXTURE)), TEXT_FIXTURE_CONTENT);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testLoadFileToInputStreamFailsForUnknownResource() {
        FileUtils.loadFileToInputStream("no-such-resource-anywhere.txt");
    }

    @Test
    public void testToByteArrayAndToInputStreamRoundTrip() {
        byte[] bytes = "payload".getBytes(StandardCharsets.UTF_8);

        Assert.assertEquals(FileUtils.toByteArray(FileUtils.toInputStream(bytes)), bytes);
    }

    @Test
    public void testToInputStreamOfNullIsEmptyStream() {
        Assert.assertEquals(FileUtils.toByteArray(FileUtils.toInputStream(null)), new byte[0]);
    }

    @Test
    public void testToByteArrayOfNullStreamIsEmpty() {
        Assert.assertEquals(FileUtils.toByteArray(null), new byte[0]);
    }

    @Test
    public void testCopyReturnsNumberOfBytesCopied() throws IOException {
        byte[] bytes = new byte[10000];
        Arrays.fill(bytes, (byte) 'x');
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int copied = FileUtils.copy(new ByteArrayInputStream(bytes), out);

        Assert.assertEquals(copied, 10000);
        Assert.assertEquals(out.toByteArray(), bytes);
    }

    @Test
    public void testCopyOfNullInputCopiesNothing() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Assert.assertEquals(FileUtils.copy(null, out), 0);
        Assert.assertEquals(out.size(), 0);
    }

    @Test
    public void testGetFileWithoutExtention() {
        Assert.assertEquals(FileUtils.getFileWithoutExtention("schema.json"), "schema");
        Assert.assertEquals(FileUtils.getFileWithoutExtention("archive.tar.gz"), "archive.tar");
        Assert.assertEquals(FileUtils.getFileWithoutExtention("noextension"), "noextension");
    }

    @Test
    public void testGetFileContentMapFromZip() throws IOException {
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(zipped);
        zos.putNextEntry(new ZipEntry("dir/one.txt"));
        zos.write("one".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("two.txt"));
        zos.write("two".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        zos.close();

        FileContentHandler handler = FileUtils.getFileContentMapFromZip(zipped.toByteArray());

        Assert.assertEquals(handler.getFileList(),
                new HashSet<>(Arrays.asList("dir/one.txt", "two.txt")));
        Assert.assertEquals(read(handler.getFileContent("dir/one.txt")), "one");
        Assert.assertEquals(read(handler.getFileContent("two.txt")), "two");
    }

    @Test
    public void testGetFileContentMapFromZipOfNonZipDataIsEmpty() throws IOException {
        Assert.assertTrue(
                FileUtils.getFileContentMapFromZip("not a zip".getBytes(StandardCharsets.UTF_8))
                        .getFileList().isEmpty());
    }

    @Test(expectedExceptions = IOException.class)
    public void testGetFileContentMapFromZipFailsWhenEntryDataIsTruncated() throws IOException {
        byte[] incompressible = new byte[8192];
        new Random(42).nextBytes(incompressible);
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(zipped);
        zos.putNextEntry(new ZipEntry("big.bin"));
        zos.write(incompressible);
        zos.closeEntry();
        zos.close();

        FileUtils.getFileContentMapFromZip(Arrays.copyOf(zipped.toByteArray(), 200));
    }

    @Test
    public void testGetFilesWalksSubdirectories() throws IOException {
        writeTempFile("top.txt", "t");
        File sub = new File(tempDir, "sub");
        Assert.assertTrue(sub.mkdir());
        Files.write(new File(sub, "nested.txt").toPath(), "n".getBytes(StandardCharsets.UTF_8));

        List<File> files = FileUtils.getFiles(tempDir.getAbsolutePath());

        Set<String> names = new HashSet<>();
        for (File file : files) {
            names.add(file.getName());
        }
        Assert.assertEquals(names, new HashSet<>(Arrays.asList("top.txt", "nested.txt")));
    }

    @Test
    public void testGetFilesOfSingleFile() throws IOException {
        File file = writeTempFile("single.txt", "s");

        List<File> files = FileUtils.getFiles(file.getAbsolutePath());

        Assert.assertEquals(files.size(), 1);
        Assert.assertEquals(files.get(0).getName(), "single.txt");
    }

    @Test
    public void testGetFilesOfEmptyDirectory() {
        Assert.assertTrue(FileUtils.getFiles(tempDir.getAbsolutePath()).isEmpty());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetFilesFailsForMissingPath() {
        FileUtils.getFiles(new File(tempDir, "absent").getAbsolutePath());
    }

    @Test
    public void testDeleteFile() throws IOException {
        File file = writeTempFile("doomed.txt", "d");

        Assert.assertTrue(FileUtils.delete(file));
        Assert.assertFalse(file.exists());
    }

    @Test
    public void testDeleteDirectoryTree() throws IOException {
        File sub = new File(tempDir, "sub");
        Assert.assertTrue(sub.mkdir());
        Files.write(new File(sub, "nested.txt").toPath(), "n".getBytes(StandardCharsets.UTF_8));

        Assert.assertTrue(FileUtils.delete(sub));
        Assert.assertFalse(sub.exists());
    }

    @Test
    public void testDeleteReportsFailureWhenANestedFileCannotBeRemoved() throws IOException {
        File sub = new File(tempDir, "sub");
        Assert.assertTrue(sub.mkdir());
        Files.write(new File(sub, "nested.txt").toPath(), "n".getBytes(StandardCharsets.UTF_8));
        Assert.assertTrue(sub.setWritable(false));
        try {
            if (Files.isWritable(sub.toPath())) {
                throw new SkipException("directory permissions are not enforced for this user");
            }

            Assert.assertFalse(FileUtils.delete(sub));
            Assert.assertTrue(sub.exists());
        } finally {
            sub.setWritable(true);
        }
    }

    @Test
    public void testDeleteMissingFileReturnsFalse() {
        Assert.assertFalse(FileUtils.delete(new File(tempDir, "absent")));
    }

    @Test
    public void testWriteFileSerialisesDataAsJson() {
        File written = FileUtils.writeFile(tempDir.getAbsolutePath(), "data.json",
                Collections.singletonMap("key", "value"));

        Assert.assertTrue(written.exists());
        Assert.assertEquals(read(FileUtils.getFileInputStream(written.getAbsolutePath())).replaceAll(
                "\\s", ""), "{\"key\":\"value\"}");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testWriteFileFailsWhenDirectoryDoesNotExist() {
        FileUtils.writeFile(new File(tempDir, "absent").getAbsolutePath(), "data.json", "x");
    }

    @Test
    public void testReadFileReturnsContentOfExistingFile() throws IOException {
        writeTempFile("present.txt", "here");

        Optional<InputStream> read = FileUtils.readFile(tempDir.getAbsolutePath(), "present.txt");

        Assert.assertTrue(read.isPresent());
        Assert.assertEquals(read(read.get()), "here");
    }

    @Test
    public void testReadFileOfMissingFileIsEmpty() {
        Assert.assertFalse(
                FileUtils.readFile(tempDir.getAbsolutePath(), "absent.txt").isPresent());
    }

    @Test
    public void testWriteFileFromInputStream() {
        File written = FileUtils.writeFileFromInputStream(tempDir.getAbsolutePath(), "stream.bin",
                new ByteArrayInputStream("streamed".getBytes(StandardCharsets.UTF_8)));

        Assert.assertTrue(written.exists());
        Assert.assertEquals(read(FileUtils.getFileInputStream(written.getAbsolutePath())), "streamed");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testWriteFileFromInputStreamFailsWhenDirectoryDoesNotExist() {
        FileUtils.writeFileFromInputStream(new File(tempDir, "absent").getAbsolutePath(), "stream.bin",
                new ByteArrayInputStream(new byte[]{1}));
    }

    @Test
    public void testExists() throws IOException {
        File file = writeTempFile("here.txt", "h");

        Assert.assertTrue(FileUtils.exists(file.getAbsolutePath()));
        Assert.assertFalse(FileUtils.exists(new File(tempDir, "absent.txt").getAbsolutePath()));
    }

    @Test
    public void testGetFileCreatesParentDirectoriesButNotTheFile() {
        File target = new File(new File(tempDir, "a/b"), "c.txt");

        File file = FileUtils.getFile(target.getAbsolutePath());

        Assert.assertTrue(file.getParentFile().isDirectory());
        Assert.assertFalse(file.exists());
    }

    @Test
    public void testFileExtensionDisplayNames() {
        Assert.assertEquals(FileUtils.FileExtension.JSON.getDisplayName(), "json");
        Assert.assertEquals(FileUtils.FileExtension.YAML.getDisplayName(), "yaml");
        Assert.assertEquals(FileUtils.FileExtension.YML.getDisplayName(), "yml");
        Assert.assertEquals(FileUtils.FileExtension.OTHER.getDisplayName(), "other");
        Assert.assertEquals(FileUtils.FileExtension.values().length, 4);
        Assert.assertEquals(FileUtils.FileExtension.valueOf("JSON"), FileUtils.FileExtension.JSON);
    }
}
