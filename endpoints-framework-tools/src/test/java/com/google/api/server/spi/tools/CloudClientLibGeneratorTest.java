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
package com.google.api.server.spi.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tests for {@link CloudClientLibGenerator}.
 */
@RunWith(JUnit4.class)
public class CloudClientLibGeneratorTest {

  private static final String CLOUD_GENERATOR_URL = "http://client/lib/generate";
  private static final String DISCOVERY_DOC = "{\"discovery\":\"doc\"}";
  private static final byte[] JAR = new byte[]{99};

  private void testGenerateClientLib(final String lv) throws IOException {
    ClientLibGenerator generator = new CloudClientLibGenerator(CLOUD_GENERATOR_URL) {

      @Override
      InputStream postRequest(String url, String boundary, String content) {
        assertEquals(CLOUD_GENERATOR_URL, url);
        assertTrue(boundary.startsWith(CloudClientLibGenerator.BOUNDARY_PREFIX));
        assertTrue(content.indexOf(DISCOVERY_DOC) > 0);
        assertTrue(content.indexOf("java\n") > 0);
        if (lv == null) {
          assertFalse(content.indexOf("name=\"lv\"") > 0);
        } else {
          assertTrue(content.indexOf("name=\"lv\"") > 0);
        }
        return new ByteArrayInputStream(JAR);
      }

      @Override
      void copyJar(InputStream input, File output) throws IOException {
        byte[] buffer = new byte[2];
        int size = input.read(buffer);
        assertEquals(1, size);
        assertEquals(99, buffer[0]);
      }
    };
    File file = Mockito.mock(File.class);
    generator.generateClientLib(DISCOVERY_DOC, "java", lv, null, file);
  }

  @Test
  public void testGenerateClientLib() throws IOException {
    testGenerateClientLib(null);
    testGenerateClientLib("lv");
  }
}
