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
  private List<String> serviceClassNames;
  private String buildSystem;
  private boolean debugOutput;
  private String hostname;
  private String basePath;
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
    serviceClassNames = null;
    hostname = null;
    basePath = null;
    testAction = new GetClientLibAction() {

      @Override
      public Object getClientLib(
          URL[] c, String l, String o, List<String> s, String bs, String h, String bp, boolean d) {
        classPath = c;
        language = l;
        outputDirPath = o;
        serviceClassNames = s;
        buildSystem = bs;
        hostname = h;
        basePath = bp;
        debugOutput = d;
        return null;
      }
    };
  }

  @Test
  public void testMissingOption() throws Exception {
    tool.execute(new String[]{GetClientLibAction.NAME,
        option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT), "classPath",
        option(EndpointsToolAction.OPTION_HOSTNAME_SHORT), "foo.com",
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
    tool.execute(new String[]{GetClientLibAction.NAME,
        option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT), "classPath",
        option(EndpointsToolAction.OPTION_LANGUAGE_SHORT), "java",
        option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        option(EndpointsToolAction.OPTION_HOSTNAME_SHORT), "foo.com",
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
    assertStringsEqual(Arrays.asList("MyService", "MyService2"), serviceClassNames);
    assertEquals(null, buildSystem);
    assertFalse(debugOutput);
    assertThat(basePath).isEqualTo("/_ah/api");
  }

  @Test
  public void testGetClientLibWithBuildSystem() throws Exception {
    tool.execute(new String[]{GetClientLibAction.NAME,
        option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT), "classPath",
        option(EndpointsToolAction.OPTION_LANGUAGE_SHORT), "java",
        option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        option(EndpointsToolAction.OPTION_BUILD_SYSTEM_SHORT), "maven",
        option(EndpointsToolAction.OPTION_HOSTNAME_SHORT), "foo.com",
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
    assertStringsEqual(Arrays.asList("MyService", "MyService2"), serviceClassNames);
    assertEquals("maven", buildSystem);
    assertFalse(debugOutput);
  }

  @Test
  public void testGetClientLibWithDebugOutput() throws Exception {
    tool.execute(new String[]{GetClientLibAction.NAME,
        option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT), "classPath",
        option(EndpointsToolAction.OPTION_LANGUAGE_SHORT), "java",
        option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        option(EndpointsToolAction.OPTION_DEBUG, false),
        option(EndpointsToolAction.OPTION_HOSTNAME_SHORT), "foo.com",
        option(EndpointsToolAction.OPTION_BASE_PATH_SHORT), "/api",
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
    assertStringsEqual(Arrays.asList("MyService", "MyService2"), serviceClassNames);
    assertEquals(null, buildSystem);
    assertTrue(debugOutput);
    assertThat(hostname).isEqualTo("foo.com");
    assertThat(basePath).isEqualTo("/api");
  }
}
