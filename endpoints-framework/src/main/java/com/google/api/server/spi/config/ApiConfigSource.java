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
import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.model.ApiConfig;


/**
 * Interface for any class that can read some swarm endpoint configuration from some source.  May
 * be capable of loading a complete configuration or may only support a subset of fields, in which
 * case it may need to be used in conjunction with another source to get the rest of the
 * configuration.
 *
 * @author Eric Orth
 */
public interface ApiConfigSource {
  /**
   * Loads all configuration data for the endpoint class from the configuration source into
   * {@code config}.  Does not include configuration data for the endpoint methods.
   *
   * @param serviceContext Provides context regarding GAE application settings.
   * @param endpointClass Class object whose configuration data is being loaded.
   * @param config Configuration as loaded so far with defaults and any already-read configurations.
   *        The ApiConfigSource will overwrite any values with those set by its config source.
   */
  void loadEndpointClass(ServiceContext serviceContext, Class<?> endpointClass,
      ApiConfig config) throws ApiConfigException;

  /**
   * Loads all configuration data for the methods of the endpoint class from the configuration
   * source into {@code methodConfigMap}.
   *
   * @param serviceContext Provides context regarding GAE application settings.
   * @param endpointClass Class object whose configuration data is being loaded.
   * @param methodConfigMap A map of endpoint method to its configuration data. This data will
   *        can be read and/or overwritten by this method.
   */
  void loadEndpointMethods(ServiceContext serviceContext, Class<?> endpointClass,
      ApiClassConfig.MethodConfigMap methodConfigMap) throws ApiConfigException;

  /**
   * Returns {@code true} iff configuration from this source is static and cannot change between
   * calls to this source.  This would be true, for example, when the config is loaded from
   * annotations on the java classes.
   *
   * @param config Configuration as loaded so far with defaults and any already-read configurations.
   */
  boolean isStaticConfig(ApiConfig config);
}
