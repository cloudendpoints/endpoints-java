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

import com.google.appengine.tools.util.Action;
import com.google.appengine.tools.util.Parser;
import com.google.appengine.tools.util.Parser.ParseResult;
import com.google.common.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Command tool for App Engine Web APIs to generate the client library for an API.
 * Called by $SDK/bin/endpoints.sh.<br>
 * <br>
 * Usage:<br>
 *   get-client-lib -cp &lt;classPath&gt; -l &lt;language&gt; -o &lt;outputDir&gt;
 *     &lt;serviceClass&gt;...<br>
 * <br>
 * &lt;classPath&gt;: Class path used to load service classes and their dependencies<br>
 * &lt;language&gt;: Language of client library<br>
 * &lt;outputDir&gt;: Directory to write the client library zip file into<br>
 * &lt;serviceClass&gt;...: Service classes of the API<br>
 * <br>
 * A typical run:<br>
 * <pre>
 * $SDK/bin/endpoints.sh get-client-lib -cp $SDK/lib/shared/servlet-api.jar:war/WEB-INF/classes \
 *     -l java -o /tmp guestbook.spi.GreetingServiceV1 guestbook.spi.GreetingServiceV2
 * </pre>
 * <br>
 */
public class EndpointsTool {
  private final Map<String, EndpointsToolAction> actions;

  public EndpointsTool() {
    actions = new LinkedHashMap<String, EndpointsToolAction>();
    actions.put(GetDiscoveryDocAction.NAME, new GetDiscoveryDocAction());
    actions.put(GetClientLibAction.NAME, new GetClientLibAction());
    actions.put(GenApiConfigAction.NAME, new GenApiConfigAction());
    actions.put(GenClientLibAction.NAME, new GenClientLibAction());
    actions.put(GetSwaggerDocAction.NAME, new GetSwaggerDocAction());
    actions.put(HelpAction.NAME, new HelpAction());
  }

  @VisibleForTesting
  Map<String, EndpointsToolAction> getActions() {
    return actions;
  }

  /**
   * Executes the tool with command line arguments.
   */
  public void execute(String[] args) throws Exception {
    if (args.length < 1) {
      printUsage();
      return;
    }
    EndpointsToolAction action = getActions().get(args[0]);
    if (action == null) {
      printUsage();
      return;
    }
    Parser parser = new Parser();
    ParseResult r =
        parser.parseArgs(action, action.getOptions(), Arrays.copyOfRange(args, 1, args.length));
    r.applyArgs();
    if (!action.execute()) {
      printUsage(action);
    }
  }

  /**
   * Prints the usage information of all commands to stdout.
   */
  public void printUsage() {
    System.out.println("Command line tool for Google Cloud Endpoints.");
    System.out.println("");
    System.out.println("usage: <endpoints-tool> <command> <options> [args]");
    System.out.println();
    System.out.println("<command> must be one of:\n");
    for (EndpointsToolAction action : actions.values()) {
      if (action.isHelpDisplayNeeded()) {
        System.out.println(action.getNameString());
      }
    }
    System.out.println();
    System.out.println("Use 'help <command>' for a detailed description of a command.");
  }

  /**
   * Prints the usage information of the given command to stdout.
   */
  public void printUsage(Action action) {
    System.out.println(action.getHelpString());
  }

  public static void main(String[] args) {
    try {
      new EndpointsTool().execute(args);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
    }
  }
}
