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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Command that combines 3 other ones and generates client libraries from service classes.
 */
public class GetClientLibAction extends EndpointsToolAction {

  // name of this command
  public static final String NAME = "get-client-lib";

  private Option classPathOption = makeClassPathOption();

  private Option languageOption = makeLanguageOption();

  private Option outputOption = makeOutputOption();

  private Option warOption = makeWarOption();

  private Option buildSystemOption = makeBuildSystemOption();

  private Option debugOption = makeDebugOption();

  public GetClientLibAction() {
    super(NAME);
    setOptions(Arrays.asList(classPathOption, languageOption, outputOption, warOption,
        buildSystemOption, debugOption));
    setShortDescription("Generates a client library");
    setExampleString("<Endpoints tool> get-client-lib --language=java --build_system=maven "
        + "com.google.devrel.samples.ttt.spi.BoardV1 com.google.devrel.samples.ttt.spi.ScoresV1");
    setHelpDisplayNeeded(true);
  }

  @Override
  public boolean execute() throws ClassNotFoundException, IOException, ApiConfigException {
    String warPath = getWarPath(warOption);
    List<String> serviceClassNames = getServiceClassNames(warPath);
    if (serviceClassNames.isEmpty()) {
      return false;
    }
    getClientLib(computeClassPath(warPath, getClassPath(classPathOption)),
        getLanguage(languageOption), getOutputPath(outputOption), warPath, serviceClassNames,
        getBuildSystem(buildSystemOption), getDebug(debugOption));
    return true;
  }

  /**
   * Generates a Java client library for an API.  Combines the steps of generating API
   * configuration, generating Discovery doc and generating client library into one.
   * @param classPath Class path to load service classes and their dependencies
   * @param language Language of the client library.  Only "java" is supported right now
   * @param outputDirPath Directory to write output files into
   * @param warPath Directory or file containing a WAR layout
   * @param serviceClassNames Array of service class names of the API
   * @param buildSystem The build system to use for the client library
   * @param debug Whether or not to output intermediate output files
   */
  public Object getClientLib(URL[] classPath, String language, String outputDirPath,
      String warPath, List<String> serviceClassNames, String buildSystem, boolean debug)
          throws ClassNotFoundException, IOException, ApiConfigException {
    Iterable<String> apiConfigs = new GenApiConfigAction().genApiConfig(classPath, outputDirPath,
        warPath, serviceClassNames, debug);
    Map<String, String> discoveryDocs = new GetDiscoveryDocAction().getDiscoveryDoc(
        classPath, outputDirPath, warPath, serviceClassNames, debug);
    for (Map.Entry<String, String> entry : discoveryDocs.entrySet()) {
      String restDiscoveryDocPath = entry.getKey();
      new GenClientLibAction().genClientLib(language, outputDirPath, restDiscoveryDocPath,
          buildSystem);
    }
    return null;
  }

  @Override
  public String getUsageString() {
    return NAME + " <options> <service class>...";
  }
}
