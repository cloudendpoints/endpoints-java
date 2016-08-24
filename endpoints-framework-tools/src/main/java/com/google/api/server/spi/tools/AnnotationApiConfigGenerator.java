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

import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.ApiConfigWriter;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.jsonwriter.JsonConfigWriter;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Generates configuration files from annotations on endpoint classes.
 */
public class AnnotationApiConfigGenerator implements ApiConfigGenerator {
  private final ApiConfigWriter configWriter;
  private final ApiConfigLoader configLoader;

  public AnnotationApiConfigGenerator() throws ClassNotFoundException {
    this(new JsonConfigWriter(), AnnotationApiConfigGenerator.class.getClassLoader(),
        new ApiConfig.Factory());
  }

  public AnnotationApiConfigGenerator(ApiConfigWriter apiConfigWriter, ClassLoader classLoader,
      ApiConfig.Factory configFactory) throws ClassNotFoundException {
    this.configWriter = apiConfigWriter;
    this.configLoader = createConfigLoader(configFactory, new TypeLoader(classLoader));
  }

  private static ApiConfigLoader createConfigLoader(ApiConfig.Factory configFactory,
      TypeLoader typeLoader) {
    return new ApiConfigLoader(
        configFactory, typeLoader, new ApiConfigAnnotationReader(typeLoader.getAnnotationTypes()));
  }

  @Override
  public Map<String, String> generateConfig(Class<?>... serviceClasses)
      throws ApiConfigException {
    return generateConfig(ServiceContext.create(), serviceClasses);
  }

  @Override
  public Map<String, String> generateConfig(
      ServiceContext serviceContext, Class<?>... serviceClasses)
      throws ApiConfigException {

    List<ApiConfig> apiConfigs = Lists.newArrayListWithCapacity(serviceClasses.length);
    for (Class<?> serviceClass : serviceClasses) {
      apiConfigs.add(configLoader.loadConfiguration(serviceContext, serviceClass));
    }

    Map<ApiKey, String> configByApiKey = configWriter.writeConfig(apiConfigs);
    // This *must* retain the order of configByApiKey so the lily_java_api BUILD rule has
    // predictable output order.
    Map<String, String> configByFileName = Maps.newLinkedHashMap();
    for (Map.Entry<ApiKey, String> entry : configByApiKey.entrySet()) {
      configByFileName.put(
          entry.getKey().getApiString() + "." + configWriter.getFileExtension(), entry.getValue());
    }

    return configByFileName;
  }
}
