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

import com.google.appengine.tools.util.Action;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link EndpointsTool}.
 */
@RunWith(JUnit4.class)
public class EndpointsToolTest {

  protected boolean usagePrinted;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  protected final EndpointsTool tool = new EndpointsTool() {
    @Override
    public void printUsage() {
      super.printUsage();
      usagePrinted = true;
    }

    @Override
    public void printUsage(Action action) {
      usagePrinted = true;
    }

    @Override
    protected Map<String, EndpointsToolAction> getActions() {
      Map<String, EndpointsToolAction> actions = new HashMap<String, EndpointsToolAction>();
      addTestAction(actions);
      return actions;
    }
  };

  protected void addTestAction(Map<String, EndpointsToolAction> commands) {
  }

  protected void assertStringsEqual(List<String> strings0, List<String> strings1) {
    assertEquals(strings0.size(), strings1.size());
    for (int i = 0; i < strings0.size(); i++) {
      assertEquals(strings0.get(i), strings1.get(i));
    }
  }

  @Before
  public void setUp() throws Exception {
    System.setOut(new PrintStream(outContent));

    usagePrinted = false;
  }

  @After
  public void tearDown() {
      System.setOut(null);
  }

  @Test
  public void testPrintUsage() {
    tool.printUsage();
    String usageMsg = outContent.toString();
    assertTrue(usageMsg.contains("usage: <endpoints-tool> <command> <options> [args]"));

    // Make sure only get-client-lib, get-discovery-doc and help command are displayed in usage.
    assertTrue(usageMsg.contains("get-client-lib"));
    assertTrue(usageMsg.contains("get-discovery-doc"));
    assertTrue(usageMsg.contains("help"));
    assertFalse(usageMsg.contains("gen-api-config"));
    assertFalse(usageMsg.contains("gen-discovery-doc"));
    assertFalse(usageMsg.contains("gen-client-lib"));
  }

  @Test
  public void testNoArguments() throws Exception {
    tool.execute(new String[]{});
    assertTrue(usagePrinted);
  }

  @Test
  public void testMissingCommand() throws Exception {
    tool.execute(new String[]{option(GetClientLibAction.OPTION_CLASS_PATH_SHORT), "classPath",
        option(GetClientLibAction.OPTION_OUTPUT_DIR_SHORT), "outputDir", "MyService"});
    assertTrue(usagePrinted);
  }

  @Test
  public void testUnknownCommand() throws Exception {
    tool.execute(new String[]{"no-such-command"});
    assertTrue(usagePrinted);
  }

  protected String option(String option) {
    return option(option, true);
  }

  protected String option(String option, boolean isShort) {
    if (isShort) {
      return "-" + option;
    }
    return "--" + option;
  }
}
