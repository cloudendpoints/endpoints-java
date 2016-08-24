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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tests for {@link IoUtil}.
 */
@RunWith(JUnit4.class)
public class IoUtilTest {

  @Test
  public void testReadStreamEmptyInput() throws Exception {
    testReadStream("");
  }

  @Test
  public void testReadStream_inputSmallerThanBuffer() throws Exception {
    testReadStream("abc123");
  }

  @Test
  public void testReadStream_inputBiggerThanBuffer() throws Exception {
    testReadStream("abc123", 3);
  }

  @Test
  public void testGetRequestInputStream() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(compress("test".getBytes(StandardCharsets.UTF_8)));
    request.addHeader("Content-Encoding", "gzip");
    InputStream stream = IoUtil.getRequestInputStream(request);
    assertThat(stream).isInstanceOf(GZIPInputStream.class);
    assertThat(IoUtil.readStream(stream)).isEqualTo("test");
  }

  @Test
  public void testGetRequestInputStream_emptyStream() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(new byte[0]);
    request.addHeader("Content-Encoding", "gzip");
    InputStream stream = IoUtil.getRequestInputStream(request);
    assertThat(stream).isNotInstanceOf(GZIPInputStream.class);
    assertThat(IoUtil.readStream(stream)).isEqualTo("");
  }

  @Test
  public void testGetRequestInputStream_headerWithPlaintext() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent("test".getBytes(StandardCharsets.UTF_8));
    request.addHeader("Content-Encoding", "gzip");
    InputStream stream = IoUtil.getRequestInputStream(request);
    assertThat(stream).isNotInstanceOf(GZIPInputStream.class);
    assertThat(IoUtil.readStream(stream)).isEqualTo("test");
  }

  @Test
  public void testReadRequestBody() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(compress("test".getBytes(StandardCharsets.UTF_8)));
    request.addHeader("Content-Encoding", "gzip");
    assertThat(IoUtil.readRequestBody(request)).isEqualTo("test");
  }

  private void testReadStream(String input) throws Exception {
    testReadStream(input, IoUtil.BUFFER_SIZE);
  }

  private void testReadStream(String input, int bufferSize) throws Exception {
    InputStream is = new ByteArrayInputStream(input.getBytes("UTF-8"));
    assertEquals(input, IoUtil.readStream(is, bufferSize));
  }

  private static byte[] compress(byte[] bytes) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gos = new GZIPOutputStream(baos);
      gos.write(bytes, 0, bytes.length);
      gos.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
