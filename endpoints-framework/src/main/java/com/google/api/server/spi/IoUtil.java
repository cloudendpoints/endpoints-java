/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.google.api.server.spi;

import com.google.common.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;

/**
 * Utililty for I/O operations.
 */
public final class IoUtil {
  private static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
  private static final String GZIP_ENCODING = "gzip";

  private IoUtil() {}

  /**
   * Loads a text resource file for a class.
   * @param c Class to load resource for.
   * @param fileName Path to resource file.
   * @return Content of the text resource file.
   * @throws IOException
   */
  public static String readResourceFile(Class<?> c, String fileName) throws IOException {
    URL url = c.getResource(fileName);
    StringBuilder sb = new StringBuilder();
    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
    for (String line = in.readLine(); line != null; line = in.readLine()) {
      sb.append(line);
    }
    return sb.toString();
  }

  /**
   * Copies an input stream into a file.
   * @param in Input stream to copy.
   * @param dest File to copy to.
   * @throws IOException
   */
  public static void copy(InputStream in, File dest) throws IOException {
    OutputStream out = new FileOutputStream(dest);
    byte[] buf = new byte[1024];
    for (int len = in.read(buf); len > 0; len = in.read(buf)) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  /**
   * Reads the full content of a file.
   */
  public static String readFile(File file) throws FileNotFoundException, IOException {
    byte[] buffer = new byte[(int) file.length()];
    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
    try {
      randomAccessFile.readFully(buffer);
      return new String(buffer, "UTF-8");
    } finally {
      randomAccessFile.close();
    }
  }

  /**
   * Reads and returns the content of all files under a directory that satisfy a filter.
   * @param directory Directory under which to read files
   * @param filter File filter used to pick which files to read
   * @return List of strings, each being the content of a file
   */
  public static List<String> readFiles(File directory, FileFilter filter)
      throws FileNotFoundException, IOException {
    List<String> contents = new ArrayList<String>();
    for (File file : directory.listFiles(filter)) {
      contents.add(IoUtil.readFile(file));
    }
    return contents;
  }

  @VisibleForTesting
  static final int BUFFER_SIZE = 1024;

  /**
   * Reads the full content of a stream and returns it in a string.
   */
  public static String readStream(InputStream is) throws IOException {
    return readStream(is, BUFFER_SIZE);
  }

  @VisibleForTesting
  static String readStream(InputStream is, int bufferSize) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      byte[] buffer = new byte[bufferSize];
      for (int n = is.read(buffer); n > 0; n = is.read(buffer)) {
        os.write(buffer, 0, n);
      }
      return os.toString("UTF-8");
    } finally {
      is.close();
      os.close();
    }
  }

  /**
   * Gets an uncompressed {@link InputStream} for the request. If the request specifies gzip
   * encoding, tries to wrap the request input stream in a {@link GZIPInputStream}. If the input
   * stream does not start with a gzip header, then a stream representing a plaintext request is
   * returned.
   */
  public static InputStream getRequestInputStream(HttpServletRequest request) throws IOException {
    InputStream bodyStream = request.getInputStream();
    if (bodyStream != null && GZIP_ENCODING.equals(request.getHeader(HEADER_CONTENT_ENCODING))) {
      PushbackInputStream pushbackStream = new PushbackInputStream(bodyStream, 2);
      byte[] header = new byte[2];
      int len = pushbackStream.read(header);
      if (len > 0) {
        pushbackStream.unread(header, 0, len);
      }
      return isGzipHeader(header) ? new GZIPInputStream(pushbackStream) : pushbackStream;
    }
    return bodyStream;
  }

  /**
   * Reads a possibly compressed request body.
   */
  public static String readRequestBody(HttpServletRequest request) throws IOException {
    InputStream inputStream = getRequestInputStream(request);
    return inputStream != null ? IoUtil.readStream(inputStream) : null;
  }

  private static boolean isGzipHeader(byte[] header) {
    // GZIP_MAGIC represents the 16-bit header that identify all gzipped content, as defined in
    // section 2.3.1 of https://tools.ietf.org/html/rfc1952.
    return header.length >= 2
        && (header[0] | ((header[1] & 0xff) << 8)) == GZIPInputStream.GZIP_MAGIC;
  }
}
