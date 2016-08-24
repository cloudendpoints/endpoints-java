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
package com.google.api.server.spi.config.model;

import com.google.api.server.spi.config.ApiConfigInconsistency;
import com.google.common.collect.Iterables;

import java.util.Objects;

/**
 * Flattened cache control configuration for a swarm endpoint.  Data generally originates from
 * {@link com.google.api.server.spi.config.ApiCacheControl} annotations.
 *
 * @author Eric Orth
 */
public class ApiCacheControlConfig {
  private String type;
  private int maxAge;

  public ApiCacheControlConfig() {
    setDefaults();
  }

  public ApiCacheControlConfig(ApiCacheControlConfig original) {
    this.type = original.type;
    this.maxAge = original.maxAge;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof ApiCacheControlConfig) {
      ApiCacheControlConfig config = (ApiCacheControlConfig) o;
      return Iterables.isEmpty(getConfigurationInconsistencies(config));
    } else {
      return false;
    }
  }

  public Iterable<ApiConfigInconsistency<Object>> getConfigurationInconsistencies(
      ApiCacheControlConfig config) {
    return ApiConfigInconsistency.listBuilder()
        .addIfInconsistent("cacheControl.type", type, config.type)
        .addIfInconsistent("cacheControl.maxAge", maxAge, config.maxAge)
        .build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, maxAge);
  }

  /**
   * Sets all fields to their default value to be used if not set otherwise.  Override to change the
   * default configuration.
   */
  protected void setDefaults() {
    type = "no-cache";
    maxAge = 0;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public void setMaxAge(int maxAge) {
    this.maxAge = maxAge;
  }

  public int getMaxAge() {
    return maxAge;
  }
}
