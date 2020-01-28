/*
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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.api.server.spi.config.ApiConfigException;
import com.google.appengine.tools.util.Option;

/**
 * Command that combines 3 other ones and generates client API source code from service classes.
 */
public class GetClientSrcAction extends EndpointsToolAction {

  // name of this command
  public static final String NAME = "get-client-src";

  private Option classPathOption = makeClassPathOption();
  private Option languageOption = makeLanguageOption();
  private Option outputOption = makeOutputOption();
  private Option warOption = makeWarOption();
  private Option debugOption = makeDebugOption();
  private Option hostnameOption = makeHostnameOption();
  private Option basePathOption = makeBasePathOption();

  public GetClientSrcAction() {
    super(NAME);
    setOptions(Arrays.asList(classPathOption, languageOption, outputOption, warOption,
        debugOption, hostnameOption, basePathOption));
    setShortDescription("Generates client source code");
    setExampleString("<Endpoints tool> get-client-src --language=java "
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
    getClientSrc(computeClassPath(warPath, getClassPath(classPathOption)),
        getLanguage(languageOption), getOutputPath(outputOption), serviceClassNames,
        getHostname(hostnameOption, warPath), getBasePath(basePathOption),
            getDebug(debugOption)
    );
    return true;
  }

  /**
   * Generates a Java client API source code for an API.  Combines the steps of generating API
   * configuration, generating Discovery doc and generating client source code into one.
   * @param classPath Class path to load service classes and their dependencies
   * @param language Language of the client library.  Only "java" is supported right now
   * @param outputDirPath Directory to write output files into
   * @param serviceClassNames Array of service class names of the API
   * @param hostname The hostname to use
   * @param basePath The base path to use
   * @param debug Whether or not to output intermediate output files
   */
  public Object getClientSrc(URL[] classPath, String language, String outputDirPath,
      List<String> serviceClassNames, String hostname, String basePath,
      boolean debug) throws ClassNotFoundException, IOException, ApiConfigException {
    Map<String, String> discoveryDocs = new GetDiscoveryDocAction().getDiscoveryDoc(
        classPath, outputDirPath, serviceClassNames, hostname, basePath, debug /* outputToDisk */);
    for (Map.Entry<String, String> entry : discoveryDocs.entrySet()) {
      new GenClientSrcAction().genClientSrcFromFile(language, outputDirPath, entry.getValue());
    }
    return null;
  }

  @Override
  public String getUsageString() {
    return NAME + " <options> <service class>...";
  }
}
