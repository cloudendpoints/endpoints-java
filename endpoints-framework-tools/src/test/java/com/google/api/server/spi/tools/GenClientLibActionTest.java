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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

/**
 * Tests for {@link GenClientLibAction}.
 */
@RunWith(JUnit4.class)
public class GenClientLibActionTest extends EndpointsToolTest {

  private String language;
  private String outputDirPath;
  private String discoveryDocPath;
  private String buildSystem;

  @Override
  protected void addTestAction(Map<String, EndpointsToolAction> commands) {
    commands.put(GenClientLibAction.NAME, new GenClientLibAction() {

      @Override
      public Object genClientLibFromFile(String l, String o, String d, String b) {
        language = l;
        outputDirPath = o;
        discoveryDocPath = d;
        buildSystem = b;
        return null;
      }
    });
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();

    usagePrinted = false;
    language = null;
    outputDirPath = null;
    discoveryDocPath = null;
  }

  @Test
  public void testGenClientLib() throws Exception {
    tool.execute(
        new String[]{GenClientLibAction.NAME, option(EndpointsToolAction.OPTION_LANGUAGE_SHORT),
        "java", option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        option(EndpointsToolAction.OPTION_BUILD_SYSTEM_SHORT), "maven", "discoveryDocPath"});
    assertFalse(usagePrinted);
    assertEquals("java", language);
    assertEquals("outputDir", outputDirPath);
    assertEquals("discoveryDocPath", discoveryDocPath);
    assertEquals("maven", buildSystem);
  }

  @Test
  public void testGenClientLibWithoutBuildSystem() throws Exception {
    tool.execute(
        new String[]{GenClientLibAction.NAME, option(EndpointsToolAction.OPTION_LANGUAGE_SHORT),
        "java", option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        "discoveryDocPath"});
    assertFalse(usagePrinted);
    assertEquals("java", language);
    assertEquals("outputDir", outputDirPath);
    assertEquals("discoveryDocPath", discoveryDocPath);
    assertEquals(null, buildSystem);
  }
}
