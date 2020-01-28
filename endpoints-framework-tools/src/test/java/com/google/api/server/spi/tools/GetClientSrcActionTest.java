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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.singletonList;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.collect.Lists;

/**
 * Tests for {@link GetClientSrcAction}.
 */
@RunWith(JUnit4.class)
public class GetClientSrcActionTest extends EndpointsToolTest {

  private URL[] classPath;
  private String language;
  private String outputDirPath;
  private List<String> serviceClassNames;
  private boolean debugOutput;
  private String hostname;
  private String basePath;
  private GetClientSrcAction testAction;

  @Override
  protected void addTestAction(Map<String, EndpointsToolAction> commands) {
    commands.put(GetClientSrcAction.NAME, testAction);
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
    testAction = new GetClientSrcAction() {

      @Override
      public Object getClientSrc(
          URL[] c, String l, String o, List<String> s, String h, String bp, boolean d) {
        classPath = c;
        language = l;
        outputDirPath = o;
        serviceClassNames = s;
        hostname = h;
        basePath = bp;
        debugOutput = d;
        return null;
      }
    };
  }

  @Test
  public void testMissingOption() throws Exception {
    tool.execute(new String[]{GetClientSrcAction.NAME,
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
    assertStringsEqual(singletonList("MyService"), serviceClassNames);
  }

  @Test
  public void testMissingArgument() throws Exception {
    tool.execute(new String[]{
        GetClientSrcAction.NAME, option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT),
        "classPath", option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir"});
    assertTrue(usagePrinted);
  }

  @Test
  public void testGetClientSrc() throws Exception {
    tool.execute(new String[]{GetClientSrcAction.NAME,
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
    assertFalse(debugOutput);
    assertThat(basePath).isEqualTo("/_ah/api");
  }

  @Test
  public void testGetClientSrcWithDebugOutput() throws Exception {
    tool.execute(new String[]{GetClientSrcAction.NAME,
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
    assertTrue(debugOutput);
    assertThat(hostname).isEqualTo("foo.com");
    assertThat(basePath).isEqualTo("/api");
  }
}
