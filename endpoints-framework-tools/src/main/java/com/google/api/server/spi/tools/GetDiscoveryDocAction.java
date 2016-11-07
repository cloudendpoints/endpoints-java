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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.discovery.DiscoveryGenerator;
import com.google.api.server.spi.discovery.DiscoveryGenerator.DiscoveryContext;
import com.google.api.server.spi.response.EndpointsPrettyPrinter;
import com.google.api.services.discovery.model.RestDescription;
import com.google.appengine.tools.util.Option;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

  public GetDiscoveryDocAction() {
    super(NAME);
    setOptions(Arrays.asList(classPathOption, outputOption, warOption, debugOption));
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
    getDiscoveryDoc(computeClassPath(warPath, getClassPath(classPathOption)),
        getOutputPath(outputOption), warPath, serviceClassNames, getDebug(debugOption));
    return true;
  }

  /**
   * Generates a Java client library for an API.  Combines the steps of generating API
   * configuration, generating Discovery doc and generating client library into one.
   * @param classPath Class path to load service classes and their dependencies
   * @param outputDirPath Directory to write output files into
   * @param warPath Directory or file containing a WAR layout
   * @param serviceClassNames Array of service class names of the API
   * @param debug Whether or not to output intermediate output files
   */
  public Map<String, String> getDiscoveryDoc(URL[] classPath, String outputDirPath,
      String warPath, List<String> serviceClassNames, boolean debug)
          throws ClassNotFoundException, IOException, ApiConfigException {
    File outputDir = new File(outputDirPath);
    if (!outputDir.isDirectory()) {
      throw new IllegalArgumentException(outputDirPath + " is not a directory");
    }

    ClassLoader classLoader = new URLClassLoader(classPath, getClass().getClassLoader());
    ApiConfig.Factory configFactory = new ApiConfig.Factory();
    TypeLoader typeLoader = new TypeLoader(classLoader);
    DiscoveryGenerator discoveryGenerator = new DiscoveryGenerator(typeLoader);
    List<ApiConfig> apiConfigs = Lists.newArrayListWithCapacity(serviceClassNames.size());
    ApiConfigLoader configLoader = new ApiConfigLoader(
        configFactory, typeLoader, new ApiConfigAnnotationReader(typeLoader.getAnnotationTypes()));
    ServiceContext serviceContext = ServiceContext.create(
        AppEngineUtil.getApplicationId(warPath), ServiceContext.DEFAULT_API_NAME);
    for (Class<?> serviceClass : loadClasses(classLoader, serviceClassNames)) {
      apiConfigs.add(configLoader.loadConfiguration(serviceContext, serviceClass));
    }
    DiscoveryGenerator.Result result = discoveryGenerator.writeDiscovery(
        apiConfigs, new DiscoveryContext().setHostname(serviceContext.getAppHostName()));
    ObjectWriter writer =
        ObjectMapperUtil.createStandardObjectMapper().writer(new EndpointsPrettyPrinter());
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (Map.Entry<ApiKey, RestDescription> entry : result.discoveryDocs().entrySet()) {
      ApiKey key = entry.getKey();
      String discoveryDocFilePath =
          outputDir + "/" + key.getName() + "-" + key.getVersion() + "-rest.discovery";
      String docString = writer.writeValueAsString(entry.getValue());
      Files.write(docString, new File(discoveryDocFilePath), UTF_8);
      builder.put(discoveryDocFilePath, docString);
      System.out.println("API Discovery Document written to " + discoveryDocFilePath);
    }
    return builder.build();
  }

  @Override
  public String getUsageString() {
    return NAME + " <options> <service class>...";
  }

  private static Class<?>[] loadClasses(ClassLoader classLoader, List<String> classNames)
      throws ClassNotFoundException {
    Class<?>[] classes = new Class<?>[classNames.size()];
    for (int i = 0; i < classNames.size(); i++) {
      classes[i] = classLoader.loadClass(classNames.get(i));
    }
    return classes;
  }
}
