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
import com.google.api.server.spi.config.ApiConfigException;

import java.io.IOException;
import java.util.Map;

/**
 * API configuration generator.
 */
public interface ApiConfigGenerator {

  /**
   * Generates API configuration file as a string for a set of service classes.
   *
   * @param serviceClasses Service classes to generate API configuration for.
   * @return A map of API configuration JSON strings with &lt;apiString&gt;.&lt;ext&gt; as the key.
   */
  public Map<String, String> generateConfig(Class<?>... serviceClasses)
      throws IOException, ApiConfigException;

  /**
   * Generates API configuration file as a string for a set of service classes given a default
   * API name.
   *
   * @param serviceContext Service context used to retrieve information about the service.
   * @param serviceClasses Service classes to generate API configuration for.
   * @return A map of API configuration JSON strings with &lt;apiString&gt;.&lt;ext&gt; as the key.
   */
  public Map<String, String> generateConfig(ServiceContext serviceContext,
      Class<?>... serviceClasses) throws IOException, ApiConfigException;

}
