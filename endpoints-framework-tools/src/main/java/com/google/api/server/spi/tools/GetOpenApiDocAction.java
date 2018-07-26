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
 * Command to generate an OpenAPI document from annotated service classes.
 */
public class GetOpenApiDocAction extends EndpointsToolAction {
  public static final String NAME = "get-openapi-doc";
  public static final String LEGACY_NAME = "get-swagger-doc";

  private Option classPathOption = makeClassPathOption();
  private Option outputOption = makeOpenApiOutputOption();
  private Option warOption = makeWarOption();
  private Option hostnameOption = makeHostnameOption();
  private Option basePathOption = makeBasePathOption();

  public GetOpenApiDocAction() {
    this(NAME, true);
  }

  protected GetOpenApiDocAction(String alias, boolean displayHelp) {
    super(alias);
    setOptions(
        Arrays.asList(classPathOption, outputOption, warOption, hostnameOption, basePathOption));
    setShortDescription("Generates an OpenAPI document");
    setExampleString("<Endpoints tool> " + getNames()[0]
        + " com.google.devrel.samples.ttt.spi.BoardV1 com.google.devrel.samples.ttt.spi.ScoresV1");
    setHelpDisplayNeeded(displayHelp);
  }

  @Override
  public String getUsageString() {
    return getNames()[0] + " <options> <service class>...";
  }

  @Override
  public boolean execute() throws ClassNotFoundException, IOException, ApiConfigException {
    String warPath = getWarPath(warOption);
    List<String> serviceClassNames = getServiceClassNames(warPath);
    if (serviceClassNames.isEmpty()) {
      return false;
    }
    genOpenApiDoc(computeClassPath(warPath, getClassPath(classPathOption)),
        getOpenApiOutputPath(outputOption), getHostname(hostnameOption, warPath),
        getBasePath(basePathOption), serviceClassNames, true);
    return true;
  }

  /**
   * Generates an OpenAPI document for an array of service classes.
   *
   * @param classPath Class path to load service classes and their dependencies
   * @param outputFilePath File to store the OpenAPI document in
   * @param hostname The hostname to use for the OpenAPI document
   * @param basePath The base path to use for the OpenAPI document, e.g. /_ah/api
   * @param serviceClassNames Array of service class names of the API
   * @param outputToDisk Iff {@code true}, outputs a openapi.json to disk.
   * @return a single OpenAPI document representing all service classes.
   */
  public String genOpenApiDoc(
      URL[] classPath, String outputFilePath, String hostname, String basePath,
      List<String> serviceClassNames, boolean outputToDisk)
      throws ClassNotFoundException, IOException, ApiConfigException {
    File outputFile = new File(outputFilePath);
    File outputDir = outputFile.getParentFile();
    if (!outputDir.isDirectory() || outputFile.isDirectory()) {
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
      Files.write(swaggerStr, outputFile, UTF_8);
      System.out.println("OpenAPI document written to " + outputFilePath);
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
