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
package com.google.api.server.spi.config;

import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Loader for entire configurations for swarm endpoints.  The current implementation hardcodes two
 * configuration sources.  First annotation configuration is read, then if requested by the config
 * so far, datastore configuration is read, overriding the annotation configurations.
 *
 * @author Eric Orth
 */
public class ApiConfigLoader {

  /**
   * The common API name to use for all internal-mechanism endpoint classes loaded through
   * {@code loadInternalConfiguration}.
   */
  public static final String INTERNAL_API_NAME = "_GoogleCloudEndpointsInternal";

  private final ApiConfig.Factory configFactory;
  private final TypeLoader typeLoader;

  private final ApiConfigAnnotationReader annotationSource;
  // Configuration sources.
  private final ImmutableList<ApiConfigSource> apiConfigSources;

  /**
   * Creates a {@link ApiConfigLoader}.
   *
   * @param configFactory Factory class in charge of creating {@link ApiConfig}s.
   * @param typeLoader Utility class for dealing with types in endpoint config generation.
   * @param annotationSource Config source which reads annotations in endpoint classes.
   * @param apiConfigSources Additional config sources, if any, to operate on endpoints.
   * @throws IllegalArgumentException if {@code apiConfigSources} includes another instance
   *         of ApiConfigAnnotationReader.
   */
  public ApiConfigLoader(ApiConfig.Factory configFactory, TypeLoader typeLoader,
      ApiConfigAnnotationReader annotationSource, ApiConfigSource... apiConfigSources) {
    this.configFactory = Preconditions.checkNotNull(configFactory);
    this.typeLoader = Preconditions.checkNotNull(typeLoader);
    this.annotationSource = Preconditions.checkNotNull(annotationSource);
    this.apiConfigSources = ImmutableList.copyOf(apiConfigSources);
    Preconditions.checkArgument(
        !Iterables.any(
            this.apiConfigSources, Predicates.instanceOf(ApiConfigAnnotationReader.class)),
        "Multiple instances of the of ApiConfigAnnotationReader were passed in");
  }

  /**
   * Constructor with basic defaults suitable for unit tests.
   */
  public ApiConfigLoader() throws ClassNotFoundException {
    this(new ApiConfig.Factory(),
        new TypeLoader(ApiConfigLoader.class.getClassLoader()),
        new ApiConfigAnnotationReader());
  }

  public ApiConfig loadConfiguration(ServiceContext serviceContext, Class<?> endpointClass)
      throws ApiConfigException {
    ApiConfig config = configFactory.create(serviceContext, typeLoader, endpointClass);

    annotationSource.loadEndpointClass(serviceContext, endpointClass, config);
    for (ApiConfigSource apiConfigSource : apiConfigSources) {
      apiConfigSource.loadEndpointClass(serviceContext, endpointClass, config);
    }

    annotationSource.loadEndpointMethods(
        serviceContext, endpointClass, config.getApiClassConfig().getMethods());
    for (ApiConfigSource apiConfigSource : apiConfigSources) {
      apiConfigSource.loadEndpointMethods(
          serviceContext, endpointClass, config.getApiClassConfig().getMethods());
    }

    return config;
  }

  /**
   * Only used for internal-mechanism Apis.  Loads default values for class-wide configuration.
   * Most validation is skipped/ignored as these Apis are not configurable and often follow
   * different rules anyway.
   */
  public ApiConfig loadInternalConfiguration(ServiceContext serviceContext,
      Class<?> endpointClass) throws ApiConfigException {
    ApiConfig config = configFactory.create(serviceContext, typeLoader, endpointClass);
    config.setName(INTERNAL_API_NAME);
    annotationSource.loadEndpointMethods(
        serviceContext, endpointClass, config.getApiClassConfig().getMethods());
    return config;
  }

  public boolean isStaticConfig(ApiConfig config) {
    for (ApiConfigSource apiConfigSource : apiConfigSources) {
      if (!apiConfigSource.isStaticConfig(config)) {
        return false;
      }
    }
    // ApiConfigAnnotationReader is static.
    return true;
  }

  public ApiConfig reloadConfiguration(ServiceContext serviceContext, Class<?> endpointClass,
      ApiConfig oldConfig) throws ApiConfigException {
    ApiConfig config = configFactory.copy(oldConfig);

    // ApiConfigAnnotationReader is static, so ignore.
    List<ApiConfigSource> apiMethodConfigSources = Lists.newArrayList();
    for (ApiConfigSource apiConfigSource : apiConfigSources) {
      if (!apiConfigSource.isStaticConfig(config)) {
        apiConfigSource.loadEndpointClass(serviceContext, endpointClass, config);
        apiMethodConfigSources.add(apiConfigSource);
      }
    }

    for (ApiConfigSource apiConfigSource : apiMethodConfigSources) {
      apiConfigSource.loadEndpointMethods(
          serviceContext, endpointClass, config.getApiClassConfig().getMethods());
    }

    return config;
  }
}
