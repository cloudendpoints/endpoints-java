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
 * Tests for {@link GetDiscoveryDocAction}.
 */
@RunWith(JUnit4.class)
public class GetDiscoveryDocActionTest extends EndpointsToolTest {

  private URL[] classPath;
  private String outputDirPath;
  private String warPath;
  private List<String> serviceClassNames;
  private GetDiscoveryDocAction testAction;
  private boolean debugOutput;

  @Override
  protected void addTestAction(Map<String, EndpointsToolAction> commands) {
    commands.put(GetDiscoveryDocAction.NAME, testAction);
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();

    usagePrinted = false;
    classPath = null;
    outputDirPath = null;
    warPath = null;
    serviceClassNames = null;
    testAction = new GetDiscoveryDocAction() {

      @Override
      public Map<String, String> getDiscoveryDoc(
          URL[] c, String o, String w, List<String> s, boolean d) {
        classPath = c;
        outputDirPath = o;
        warPath = w;
        serviceClassNames = s;
        debugOutput = d;
        return null;
      }
    };
  }

  @Test
  public void testMissingOption() throws Exception {
    tool.execute(new String[]{
        GetDiscoveryDocAction.NAME, option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT),
        "classPath", "MyService"});
    assertFalse(usagePrinted);
    assertThat(Lists.newArrayList(classPath))
        .containsExactly(new File("classPath").toURI().toURL(),
            new File(new File(EndpointsToolAction.DEFAULT_WAR_PATH).getAbsoluteFile(),
                "/WEB-INF/classes")
                .toURI()
                .toURL());
    assertEquals(EndpointsToolAction.DEFAULT_OUTPUT_PATH, outputDirPath);
    assertEquals(EndpointsToolAction.DEFAULT_WAR_PATH, warPath);
    assertStringsEqual(Arrays.asList("MyService"), serviceClassNames);
  }

  @Test
  public void testMissingArgument() throws Exception {
    tool.execute(new String[]{
        GetDiscoveryDocAction.NAME, option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT),
        "classPath", option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir"});
    assertTrue(usagePrinted);
  }

  @Test
  public void testGetDiscoveryDoc() throws Exception {
    tool.execute(new String[]{GetDiscoveryDocAction.NAME,
        option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT), "classPath",
        option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir",
        "MyService", "MyService2"});
    assertFalse(usagePrinted);
    assertThat(Lists.newArrayList(classPath))
        .containsExactly(new File("classPath").toURI().toURL(),
            new File(new File(EndpointsToolAction.DEFAULT_WAR_PATH).getAbsoluteFile(),
                "/WEB-INF/classes")
                .toURI()
                .toURL());
    assertEquals("outputDir", outputDirPath);
    assertEquals(EndpointsToolAction.DEFAULT_WAR_PATH, warPath);
    assertStringsEqual(Arrays.asList("MyService", "MyService2"), serviceClassNames);
    assertFalse(debugOutput);
  }
}
