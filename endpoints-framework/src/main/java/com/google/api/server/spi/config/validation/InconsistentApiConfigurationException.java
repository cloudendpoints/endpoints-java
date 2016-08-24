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
package com.google.api.server.spi.config.validation;

import com.google.api.server.spi.config.ApiConfigInconsistency;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

/**
 * Exception for when two API classes are in the same API, but provide inconsistent API-wide
 * configuration.
 *
 * @author Eric Orth
 */
public class InconsistentApiConfigurationException extends ApiConfigInvalidException {
  public InconsistentApiConfigurationException(ApiConfig config, ApiConfig otherConfig,
      Iterable<ApiConfigInconsistency<Object>> inconsistencies) {
    super(config, getErrorMessage(config, otherConfig, inconsistencies));
  }

  private static String getErrorMessage(ApiConfig config, ApiConfig otherConfig,
      Iterable<ApiConfigInconsistency<Object>> inconsistencies) {
    Preconditions.checkArgument(!Iterables.isEmpty(inconsistencies));
    ApiConfigInconsistency<?> firstInconsistency = Iterables.getFirst(inconsistencies, null);
    return String.format("API-wide configuration does not match between the classes %s and %s. All "
        + "API classes with the same API name and version must have the exact same API-wide "
        + "configuration. Differing property: %s (%s vs %s).",
        config.getApiClassConfig().getApiClassJavaName(),
        otherConfig.getApiClassConfig().getApiClassJavaName(),
        firstInconsistency.getPropertyName(),
        firstInconsistency.getValue1(),
        firstInconsistency.getValue2());
  }
}
