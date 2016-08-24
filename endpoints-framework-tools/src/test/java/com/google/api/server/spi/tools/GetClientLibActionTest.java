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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link GetClientLibAction}.
 */
@RunWith(JUnit4.class)
public class GetClientLibActionTest extends EndpointsToolTest {

  private URL[] classPath;
  private String language;
  private String outputDirPath;
  private String warPath;
  private List<String> serviceClassNames;
  private String buildSystem;
  private boolean debugOutput;
  private GetClientLibAction testAction;

  @Override
  protected void addTestAction(Map<String, EndpointsToolAction> commands) {
    commands.put(GetClientLibAction.NAME, testAction);
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();

    usagePrinted = false;
    classPath = null;
    language = null;
    outputDirPath = null;
    warPath = null;
    serviceClassNames = null;
    testAction = new GetClientLibAction() {

      @Override
      public Object getClientLib(
          URL[] c, String l, String o, String w, List<String> s, String b, boolean d) {
        classPath = c;
        language = l;
        outputDirPath = o;
        warPath = w;
        serviceClassNames = s;
        buildSystem = b;
        debugOutput = d;
        return null;
      }
    };
  }

  @Test
  public void testMissingOption() throws Exception {
    tool.execute(new String[]{
        GetClientLibAction.NAME, option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT), "classPath",
        "MyService"});
    assertFalse(usagePrinted);
    assertThat(Lists.newArrayList(classPath))
        .containsExactly(new File("classPath").toURI().toURL(),
            new File(new File(EndpointsToolAction.DEFAULT_WAR_PATH).getAbsoluteFile(),
                "/WEB-INF/classes")
                .toURI()
                .toURL());
    assertEquals(EndpointsToolAction.DEFAULT_LANGUAGE, language);
    assertEquals(EndpointsToolAction.DEFAULT_OUTPUT_PATH, outputDirPath);
    assertEquals(EndpointsToolAction.DEFAULT_WAR_PATH, warPath);
    assertStringsEqual(Arrays.asList("MyService"), serviceClassNames);
  }

  @Test
  public void testMissingArgument() throws Exception {
    tool.execute(new String[]{
        GetClientLibAction.NAME, option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT),
        "classPath", option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir"});
    assertTrue(usagePrinted);
  }

  @Test
  public void testGetClientLib() throws Exception {
    tool.execute(
        new String[]{GetClientLibAction.NAME, option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT),
        "classPath", option(EndpointsToolAction.OPTION_LANGUAGE_SHORT), "java",
        option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        "MyService", "MyService2"});
    assertFalse(usagePrinted);
    assertThat(Lists.newArrayList(classPath))
        .containsExactly(new File("classPath").toURI().toURL(),
            new File(new File(EndpointsToolAction.DEFAULT_WAR_PATH).getAbsoluteFile(),
                "/WEB-INF/classes")
                .toURI()
                .toURL());
    assertEquals("java", language);
    assertEquals("outputDir", outputDirPath);
    assertEquals(EndpointsToolAction.DEFAULT_WAR_PATH, warPath);
    assertStringsEqual(Arrays.asList("MyService", "MyService2"), serviceClassNames);
    assertEquals(null, buildSystem);
    assertFalse(debugOutput);
  }

  @Test
  public void testGetClientLibWithBuildSystem() throws Exception {
    tool.execute(
        new String[]{GetClientLibAction.NAME, option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT),
        "classPath", option(EndpointsToolAction.OPTION_LANGUAGE_SHORT), "java",
        option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        option(EndpointsToolAction.OPTION_BUILD_SYSTEM_SHORT), "maven",
        "MyService", "MyService2"});
    assertFalse(usagePrinted);
    assertThat(Lists.newArrayList(classPath))
        .containsExactly(new File("classPath").toURI().toURL(),
            new File(new File(EndpointsToolAction.DEFAULT_WAR_PATH).getAbsoluteFile(),
                "/WEB-INF/classes")
                .toURI()
                .toURL());
    assertEquals("java", language);
    assertEquals("outputDir", outputDirPath);
    assertEquals(EndpointsToolAction.DEFAULT_WAR_PATH, warPath);
    assertStringsEqual(Arrays.asList("MyService", "MyService2"), serviceClassNames);
    assertEquals("maven", buildSystem);
    assertFalse(debugOutput);
  }

  @Test
  public void testGetClientLibWithDebugOutput() throws Exception {
    tool.execute(
        new String[]{GetClientLibAction.NAME, option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT),
        "classPath", option(EndpointsToolAction.OPTION_LANGUAGE_SHORT), "java",
        option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        option(EndpointsToolAction.OPTION_DEBUG, false), "MyService", "MyService2"});
    assertFalse(usagePrinted);
    assertThat(Lists.newArrayList(classPath))
        .containsExactly(new File("classPath").toURI().toURL(),
            new File(new File(EndpointsToolAction.DEFAULT_WAR_PATH).getAbsoluteFile(),
                "/WEB-INF/classes")
                .toURI()
                .toURL());
    assertEquals("java", language);
    assertEquals("outputDir", outputDirPath);
    assertEquals(EndpointsToolAction.DEFAULT_WAR_PATH, warPath);
    assertStringsEqual(Arrays.asList("MyService", "MyService2"), serviceClassNames);
    assertEquals(null, buildSystem);
    assertTrue(debugOutput);
  }
}
