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

import com.google.api.server.spi.tools.DiscoveryDocGenerator.Format;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

/**
 * Tests for {@link GenDiscoveryDocAction}.
 */
@RunWith(JUnit4.class)
public class GenDiscoveryDocActionTest extends EndpointsToolTest {

  private Format format;
  private String outputDirPath;
  private String apiConfigFilePath;
  private boolean outputToDisk;

  @Override
  protected void addTestAction(Map<String, EndpointsToolAction> commands) {
    commands.put(GenDiscoveryDocAction.NAME, new GenDiscoveryDocAction() {

      @Override
      public String genDiscoveryDocFromFile(Format f, String o, String c, boolean d) {
        format = f;
        outputDirPath = o;
        apiConfigFilePath = c;
        outputToDisk = d;
        return null;
      }
    });
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();

    usagePrinted = false;
    format = null;
    outputDirPath = null;
    apiConfigFilePath = null;
  }

  @Test
  public void testGenDiscoveryDoc() throws Exception {
    testGenDiscoveryDoc("rest");
    testGenDiscoveryDoc("rpc");
  }

  private void testGenDiscoveryDoc(String format2) throws Exception {
    tool.execute(
        new String[]{GenDiscoveryDocAction.NAME, option(GenDiscoveryDocAction.OPTION_FORMAT_SHORT),
        format2, option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        "apiConfigFilePath"});
    assertFalse(usagePrinted);
    assertEquals(Format.valueOf(format2.toUpperCase()), format);
    assertEquals("outputDir", outputDirPath);
    assertEquals("apiConfigFilePath", apiConfigFilePath);
    assertTrue(outputToDisk);
  }
}
