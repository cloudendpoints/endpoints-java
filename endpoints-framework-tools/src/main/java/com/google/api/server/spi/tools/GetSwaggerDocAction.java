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

import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.response.EndpointsPrettyPrinter;
import com.google.api.server.spi.swagger.SwaggerGenerator;
import com.google.api.server.spi.swagger.SwaggerGenerator.SwaggerContext;
import com.google.appengine.tools.util.Option;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import io.swagger.models.Swagger;
import io.swagger.util.Json;

/**
 * Command to generate a Swagger document from annotated service classes.
 */
public class GetSwaggerDocAction extends EndpointsToolAction {
  public static final String NAME = "get-swagger-doc";

  private Option classPathOption = makeClassPathOption();
  private Option outputOption = makeSwaggerOutputOption();
  private Option warOption = makeWarOption();
  private Option hostnameOption = makeHostnameOption();
  private Option basePathOption = makeBasePathOption();

  public GetSwaggerDocAction() {
    super(NAME);
    setOptions(
        Arrays.asList(classPathOption, outputOption, warOption, hostnameOption, basePathOption));
    setShortDescription("Generates a Swagger document");
    setExampleString("<Endpoints tool> get-swagger-doc "
        + "com.google.devrel.samples.ttt.spi.BoardV1 com.google.devrel.samples.ttt.spi.ScoresV1");
    setHelpDisplayNeeded(true);
  }

  @Override
  public String getUsageString() {
    return NAME + " <options> <service class>...";
  }

  @Override
  public boolean execute() throws ClassNotFoundException, IOException, ApiConfigException {
    if (getArgs().isEmpty()) {
      return false;
    }
    String warPath = getWarPath(warOption);
    genSwaggerDoc(computeClassPath(warPath, getClassPath(classPathOption)),
        getSwaggerOutputPath(outputOption), getHostname(hostnameOption, warPath),
        getBasePath(basePathOption), getArgs(), true);
    return true;
  }

  /**
   * Generates Swagger document for an array of service classes.
   *
   * @param classPath Class path to load service classes and their dependencies
   * @param outputFilePath File to store the Swagger document in
   * @param hostname The hostname to use for the Swagger document
   * @param basePath The base path to use for the Swagger document, e.g. /_ah/api
   * @param serviceClassNames Array of service class names of the API
   * @param outputToDisk Iff {@code true}, outputs a swagger.json to disk.
   * @return a single Swagger document representing all service classes.
   */
  public String genSwaggerDoc(
      URL[] classPath, String outputFilePath, String hostname, String basePath,
      List<String> serviceClassNames, boolean outputToDisk)
      throws ClassNotFoundException, IOException, ApiConfigException {
    File outputDir = new File(outputFilePath).getParentFile();
    if (!outputDir.isDirectory()) {
      throw new IllegalArgumentException(outputFilePath + " is not a file");
    }

    ClassLoader classLoader = new URLClassLoader(classPath, getClass().getClassLoader());
    ApiConfig.Factory configFactory = new ApiConfig.Factory();
    Class<?>[] serviceClasses = loadClasses(classLoader, serviceClassNames);
    List<ApiConfig> apiConfigs = Lists.newArrayListWithCapacity(serviceClasses.length);
    TypeLoader typeLoader = new TypeLoader(classLoader);
    ApiConfigLoader configLoader = new ApiConfigLoader(configFactory, typeLoader,
        new ApiConfigAnnotationReader(typeLoader.getAnnotationTypes()));
    ServiceContext serviceContext = ServiceContext.create();
    for (Class<?> serviceClass : serviceClasses) {
      apiConfigs.add(configLoader.loadConfiguration(serviceContext, serviceClass));
    }
    SwaggerGenerator generator = new SwaggerGenerator();
    SwaggerContext swaggerContext = new SwaggerContext()
        .setHostname(hostname)
        .setBasePath(basePath);
    Swagger swagger = generator.writeSwagger(apiConfigs, true, swaggerContext);
    String swaggerStr = Json.mapper().writer(new EndpointsPrettyPrinter())
        .writeValueAsString(swagger);
    if (outputToDisk) {
      Files.write(swaggerStr, new File(outputFilePath), UTF_8);
      System.out.println("Swagger document written to " + outputFilePath);
    }

    return swaggerStr;
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
