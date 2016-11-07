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

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

/**
 * Tests for {@link HelpAction}.
 */
@RunWith(JUnit4.class)
public class HelpActionTest extends EndpointsToolTest {
  private HelpAction testAction;
  private EndpointsToolAction action;

  @Override
  protected void addTestAction(Map<String, EndpointsToolAction> commands) {
    commands.put(HelpAction.NAME, testAction);
  }
  
  @Before
  public void setUp() throws Exception {
    super.setUp();
    action = null;
    testAction = new HelpAction() {

      @Override
      void printUsage(EndpointsToolAction a) {
        action = a;
      }
    };
  }

  @Test
  public void testHelp() throws Exception {
    tool.execute(new String[]{HelpAction.NAME, "get-client-lib"});
    assertTrue(action instanceof GetClientLibAction);
    tool.execute(new String[]{HelpAction.NAME, "get-discovery-doc"});
    assertTrue(action instanceof GetDiscoveryDocAction);
  }

  @Test
  public void testHelpUnknownAction() throws Exception {
    tool.execute(new String[]{HelpAction.NAME, "unknown"});
    assertTrue(usagePrinted);
  }

  @Test
  public void testHelpInvisibleAction() throws Exception {
    tool.execute(new String[]{HelpAction.NAME, "gen-api-config"});
    assertTrue(action instanceof GenApiConfigAction);
    tool.execute(new String[]{HelpAction.NAME, "gen-client-lib"});
    assertTrue(action instanceof GenClientLibAction);
  }
}
