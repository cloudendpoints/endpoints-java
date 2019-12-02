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
import com.google.api.server.spi.config.jsonwriter.JsonConfigWriter;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.config.validation.ApiConfigValidator;
import com.google.appengine.tools.util.Option;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Command to generate API configuration from annotated service classes.
 */
public class GenApiConfigAction extends EndpointsToolAction {

  // name of this command
  public static final String NAME = "gen-api-config";

  private Option classPathOption = makeClassPathOption();

  private Option outputOption = makeWarOutputOption();

  private Option warOption = makeWarOption();

  public GenApiConfigAction() {
    super(NAME);
    setOptions(Arrays.asList(classPathOption, outputOption, warOption));
    setShortDescription("Generates API configuration files from service classes");
    setHelpDisplayNeeded(false);
  }

  @Override
  public boolean execute() throws ClassNotFoundException, IOException, ApiConfigException {
    String warPath = getWarPath(warOption);
    List<String> serviceClassNames = getServiceClassNames(warPath);
    if (serviceClassNames.isEmpty()) {
      return false;
    }
    genApiConfig(computeClassPath(warPath, getClassPath(classPathOption)),
        getWarOutputPath(outputOption, warPath), warPath, serviceClassNames, true);
    return true;
  }

  /**
   * Generates API configuration for an array of service classes.
   * @param classPath Class path to load service classes and their dependencies
   * @param outputDirPath Directory to write API configuration files into
   * @param serviceClassNames Array of service class names of the API
   * @param warPath Directory or file containing a WAR layout
   * @param outputToDisk Iff {@code true}, outputs a *.api file to disk for each API.
   * @return JSON-formatted configurations for each API.
   */
  public Iterable<String> genApiConfig(
      URL[] classPath, String outputDirPath, String warPath, List<String> serviceClassNames,
      boolean outputToDisk)
      throws ClassNotFoundException, IOException, ApiConfigException {
    File outputDir = new File(outputDirPath);
    if (!outputDir.isDirectory()) {
      throw new IllegalArgumentException(outputDirPath + " is not a directory");
    }

    ClassLoader classLoader = new URLClassLoader(classPath, getClass().getClassLoader());
    ApiConfig.Factory configFactory = new ApiConfig.Factory();
    TypeLoader typeLoader = new TypeLoader(classLoader);
    SchemaRepository schemaRepository = new SchemaRepository(typeLoader);
    ApiConfigValidator validator = new ApiConfigValidator(typeLoader, schemaRepository);
    JsonConfigWriter jsonConfigWriter = new JsonConfigWriter(typeLoader, validator);
    ApiConfigGenerator generator =
        new AnnotationApiConfigGenerator(jsonConfigWriter, classLoader, configFactory);
    Map<String, String> apiConfigs = generator.generateConfig(
        ServiceContext.create(
            AppEngineUtil.getApplicationId(warPath), ServiceContext.DEFAULT_API_NAME),
        loadClasses(classLoader, serviceClassNames));

    if (outputToDisk) {
      for (Map.Entry<String, String> entry : apiConfigs.entrySet()) {
        String apiConfigFileName = entry.getKey();
        String apiConfigFileContent = entry.getValue();
        String apiConfigFilePath = outputDir + "/" + apiConfigFileName;
        Files.asCharSink(new File(apiConfigFilePath), UTF_8).write(apiConfigFileContent);
        System.out.println("API configuration written to " + apiConfigFilePath);
      }
    }

    return apiConfigs.values();
  }

  private static Class<?>[] loadClasses(ClassLoader classLoader, List<String> classNames)
      throws ClassNotFoundException {
    Class<?>[] classes = new Class<?>[classNames.size()];
    for (int i = 0; i < classNames.size(); i++) {
      classes[i] = classLoader.loadClass(classNames.get(i));
    }
    return classes;
  }

  @Override
  public String getUsageString() {
    return NAME + " <options> <service class>...";
  }
}
