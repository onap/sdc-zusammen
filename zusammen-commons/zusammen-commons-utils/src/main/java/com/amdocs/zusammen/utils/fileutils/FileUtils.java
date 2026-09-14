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

package com.amdocs.zusammen.utils.fileutils;


import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {

  public static InputStream getFileInputStream(String fileName) {
    InputStream is;
    URL urlFile = FileUtils.class.getClassLoader().getResource(fileName);
    try {

      if(urlFile!=null){
        is = urlFile.openStream();
      }
      else{
        is = new FileInputStream(new File(fileName));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return is;
  }

  public static List<InputStream> getFileInputStreams(String fileName) {
    Enumeration<URL> urlFiles;
    List<InputStream> streams = new ArrayList<>();
    InputStream is;
    URL url;
    try {
      urlFiles = FileUtils.class.getClassLoader().getResources(fileName);
      while (urlFiles.hasMoreElements()) {
        url = urlFiles.nextElement();
        is = url.openStream();
        streams.add(is);
      }


    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return streams;
  }


  public enum FileExtension {

    JSON("json"),
    YAML("yaml"),
    YML("yml"),
    OTHER("other");

    private String displayName;

    public String getDisplayName() {
      return displayName;
    }

    FileExtension(String displayName) {
      this.displayName = displayName;
    }
  }
/*

  public static byte[] convertToBytes(Object object, FileExtension extension) {
    if (object != null) {
      if (extension.equals(FileExtension.YAML) || extension.equals(FileExtension.YML)) {
        return new YamlUtil().objectToYaml(object).getBytes();
      } else {
        return JsonUtil.object2Json(object).getBytes();
      }
    } else {
      return new byte[]{};
    }
  }

  public static InputStream convertToInputStream(Object object, FileExtension extension) {
    if (object != null) {

      byte[] content;

      if (extension.equals(FileExtension.YAML) || extension.equals(FileExtension.YML)) {
        content = new YamlUtil().objectToYaml(object).getBytes();
      } else {
        content = JsonUtil.object2Json(object).getBytes();

      }
      return new ByteArrayInputStream(content);
    } else {
      return null;
    }
  }*/


    /*public static String loadFileToString(String fileName) {
        if(fileName== null) {
            return null;
        }
        byte[] fileBytes = loadFileToByte(fileName);
        if(fileBytes ==null || fileBytes.length==0) {
            return null;
        }
        return new String(fileBytes);
    }*/


  public static InputStream loadFileToInputStream(String fileName) {
    URL urlFile = FileUtils.class.getClassLoader().getResource(fileName);
    try {
      Enumeration<URL> en = FileUtils.class.getClassLoader().getResources(fileName);
      while (en.hasMoreElements()) {
        urlFile = en.nextElement();
      }
    } catch (IOException | NullPointerException e) {
      throw new RuntimeException(e);
    }
    try {
      if (urlFile != null) {
        return urlFile.openStream();
      } else {
        throw new RuntimeException();
      }
    } catch (IOException | NullPointerException e) {
      throw new RuntimeException(e);
    }

  }


  public static byte[] toByteArray(InputStream input) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      copy(input, output);
      byte[] returnValue = output.toByteArray();
      return returnValue;
    } catch (IOException e) {
      throw new RuntimeException(
          "error will converting input stream to byte array:" + e.getMessage());
    }finally {
      try {
        output.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static InputStream toInputStream(byte[] bytes) {
    if (bytes == null) {
      return new ByteArrayInputStream(new byte[]{});
    }
    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    return is;
  }

  public static int copy(InputStream input, OutputStream output) throws IOException {
    long count = copyLarge(input, output);
    return count > 2147483647L ? -1 : (int) count;
  }


  private static long copyLarge(InputStream input, OutputStream output) throws IOException {
    return copyLarge(input, output, new byte[4096]);
  }

  private static long copyLarge(InputStream input, OutputStream output, byte[] buffer)
      throws IOException {
    long count = 0L;

    int n1;
    if (input == null) {
      return count;
    }
    for (; -1 != (n1 = input.read(buffer)); count += (long) n1) {
      output.write(buffer, 0, n1);
    }

    return count;


  }

  public static String getFileWithoutExtention(String fileName) {
    if (!fileName.contains(".")) {
      return fileName;
    }
    return fileName.substring(0, fileName.lastIndexOf("."));
  }

  public static FileContentHandler getFileContentMapFromZip(byte[] zipData) throws IOException {
    ZipEntry zipEntry;
    FileContentHandler mapFileContent = new FileContentHandler();
    try {
      ZipInputStream inputZipStream;

      byte[] fileByteContent;
      String currentEntryName;
      inputZipStream = new ZipInputStream(new ByteArrayInputStream(zipData));

      while ((zipEntry = inputZipStream.getNextEntry()) != null) {
        currentEntryName = zipEntry.getName();
        fileByteContent = FileUtils.toByteArray(inputZipStream);
        mapFileContent.addFile(currentEntryName, fileByteContent);
      }

    } catch (RuntimeException e) {
      throw new IOException(e);
    }
    return mapFileContent;
  }

  public static List<File> getFiles(String path) {
    File file = new File(path);
    if (!file.exists()) {
      throw new RuntimeException("file[" + file.getAbsolutePath() + "] does not " +
          "exist");
    }
    List<File> fileList = new ArrayList<>();

    getFiles(file, fileList);

    return fileList;
  }

  private static void getFiles(File file, List<File> fileList) {
    if (file.exists()) {
      if (file.isDirectory()) {
        File[] files = file.listFiles();
        for (File subFile : files) {
          getFiles(subFile, fileList);
        }
      } else {
        fileList.add(file);
      }
    }
  }

  /**
   * Deletes a given file. If the file is a directory, then all contained
   * files are recursively deleted.
   *
   * @param file the file to delete.
   * @return <code>true</code> if the file was deleted or <code>false</code> otherwise.
   */
  public static boolean delete(File file) {
    if (file.exists()) {
      if (file.isDirectory()) {
        File[] files = file.listFiles();
        if (files != null) {
          for (File subFile : files) {
            boolean isDeleted = delete(subFile);
            if (!isDeleted) {
              return false;
            }
          }
        }
      }
      return file.delete();
    }
    return false;
  }

  public static File writeFile(String path, String fileName, Object fileData) {
    File file = new File(path + File.separator + fileName);
    String fileByte = JsonUtil.object2Json(fileData);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      fos.write(fileByte.getBytes());

    } catch (IOException e) {
      throw new RuntimeException(e);
    }finally {
      if(fos!= null)
        try {
          fos.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
    }
    return file;
  }

  public static Optional<InputStream> readFile(String path, String fileName){
    File file = new File(path+File.separator+fileName);
    if(!file.exists()) return Optional.empty();

    try {
      FileInputStream fis = new FileInputStream(file);
      return Optional.of(fis);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static File writeFileFromInputStream(String path, String fileName, InputStream fileData){
    OutputStream fileDataOut = null;
    File file = new File(path+File.separator+fileName);
    try {
      fileDataOut = new FileOutputStream(file);
      copy(fileData,fileDataOut);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }finally {
      if(fileDataOut!= null)
        try {
          fileDataOut.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
    }

    return file;

  }

  public static boolean exists(String path) {
    File file = new File(path);
    return file.exists();
  }

  public static File getFile(String path) {
    File file = new File(path);
    file.getParentFile().mkdirs();
    return file;
  }

  public static String trimPath(String path){

    int lastIndex = path.lastIndexOf(File.separator);
    return path.substring(0,lastIndex-1);

  }

}
