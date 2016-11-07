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

import com.google.api.server.spi.config.ApiConfigException;
import com.google.appengine.tools.util.Option;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.io.IOException;

/**
 * Command to print out detailed usage for another action.
 */
public class HelpAction extends EndpointsToolAction {
  
  // name of this command
  public static final String NAME = "help";
  
  public HelpAction() {
    super(NAME);
    setOptions(ImmutableList.<Option>of());
    setShortDescription("Print out detailed usage for a command.");
    setHelpDisplayNeeded(true);
  }

  @Override
  public boolean execute()
      throws ClassNotFoundException,
      IOException,
      ApiConfigException {
    if (getArgs().isEmpty()) {
      // Print out general usage for Endpoints command-line tool.
      EndpointsTool tool = new EndpointsTool();
      tool.printUsage();
      return true;
    } else if (getArgs().size() > 1) {
      return false;
    }
    
    String commandName = getArgs().get(0);
    switch (commandName) {
      case "get-client-lib":
        printUsage(new GetClientLibAction());
        return true;
      case "get-discovery-doc":
        printUsage(new GetDiscoveryDocAction());
        return true;
      case "gen-api-config":
        printUsage(new GenApiConfigAction());
        return true;
      case "gen-client-lib":
        printUsage(new GenClientLibAction());
        return true;
      default:
        return false;
    }
  }
  
  /**
   * Print out detailed action for Endpoints tool action.
   * @param action Endpoints Tool Action, help lines of which needs to be printed out.
   */
  @VisibleForTesting
  void printUsage(EndpointsToolAction action) {
    System.out.println(Joiner.on('\n').join(action.getHelpLines()));
  }

  @Override
  public String getUsageString() {
    return NAME + " <command name>";
  }
}
