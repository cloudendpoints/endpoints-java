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
import com.google.api.server.spi.tools.DiscoveryDocGenerator.Format;
import com.google.appengine.tools.util.Option;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Command that combines 2 other ones and generates a discovery doc from service classes.
 */
public class GetDiscoveryDocAction extends EndpointsToolAction {

  // name of this command
  public static final String NAME = "get-discovery-doc";

  private Option classPathOption = makeClassPathOption();

  private Option outputOption = makeOutputOption();

  private Option warOption = makeWarOption();

  private Option debugOption = makeDebugOption();

  private Option formatOption = makeFormatOption();

  public GetDiscoveryDocAction() {
    super(NAME);
    setOptions(Arrays.asList(classPathOption, formatOption, outputOption, warOption, debugOption));
    setShortDescription("Generates discovery documents");
    setExampleString("<Endpoints tool> get-discovery-doc --format=rpc "
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
    Format format = Format.valueOf(getFormat(formatOption).toUpperCase());
    getDiscoveryDoc(format, computeClassPath(warPath, getClassPath(classPathOption)),
        getOutputPath(outputOption), warPath, serviceClassNames, getDebug(debugOption));
    return true;
  }

  /**
   * Generates a Java client library for an API.  Combines the steps of generating API
   * configuration, generating Discovery doc and generating client library into one.
   * @param format the discovery doc format to generate
   * @param classPath Class path to load service classes and their dependencies
   * @param outputDirPath Directory to write output files into
   * @param warPath Directory or file containing a WAR layout
   * @param serviceClassNames Array of service class names of the API
   * @param debug Whether or not to output intermediate output files
   */
  public Iterable<String> getDiscoveryDoc(Format format, URL[] classPath, String outputDirPath,
      String warPath, List<String> serviceClassNames, boolean debug)
          throws ClassNotFoundException, IOException, ApiConfigException {
    Iterable<String> apiConfigs = new GenApiConfigAction().genApiConfig(classPath, outputDirPath,
        warPath, serviceClassNames, debug);
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (String apiConfig : apiConfigs) {
      builder.add(generateDiscoveryDocs(format, outputDirPath, apiConfig));
    }
    return builder.build();
  }

  // Generates and returns a REST discovery document for an API version.
  @VisibleForTesting
  String generateDiscoveryDocs(Format format, String outputDirPath, String apiConfig)
      throws IOException {
    return generateDiscoveryDoc(format, outputDirPath, apiConfig);
  }

  @VisibleForTesting
  String generateDiscoveryDoc(Format format, String outputDirPath, String apiConfig)
      throws IOException {
    return new GenDiscoveryDocAction().genDiscoveryDoc(format, outputDirPath, apiConfig, true);
  }

  @Override
  public String getUsageString() {
    return NAME + " <options> <service class>...";
  }
}
