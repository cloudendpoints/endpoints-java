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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Flattened auth configuration for a swarm endpoint.  Data generally originates from
 * {@link com.google.api.server.spi.config.ApiAuth} annotations.
 *
 * @author Eric Orth
 */
public class ApiAuthConfig {
  private boolean allowCookieAuth;
  private List<String> blockedRegions;

  public ApiAuthConfig() {
    setDefaults();
  }

  public ApiAuthConfig(ApiAuthConfig original) {
    this.allowCookieAuth = original.allowCookieAuth;
    this.blockedRegions = new ArrayList<String>(original.blockedRegions);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof ApiAuthConfig) {
      ApiAuthConfig config = (ApiAuthConfig) o;
      return Iterables.isEmpty(getConfigurationInconsistencies(config));
    } else {
      return false;
    }
  }

  public Iterable<ApiConfigInconsistency<Object>> getConfigurationInconsistencies(
      ApiAuthConfig config) {
    return ApiConfigInconsistency.listBuilder()
        .addIfInconsistent("auth.allowCookieAuth", allowCookieAuth, config.allowCookieAuth)
        .addIfInconsistent("auth.blockedRegions", blockedRegions, config.blockedRegions)
        .build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowCookieAuth, blockedRegions);
  }

  /**
   * Sets all fields to their default value to be used if not set otherwise.  Override to change the
   * default configuration.
   */
  protected void setDefaults() {
    allowCookieAuth = false;
    blockedRegions = Collections.emptyList();
  }

  public void setAllowCookieAuth(boolean allowCookieAuth) {
    this.allowCookieAuth = allowCookieAuth;
  }

  public boolean getAllowCookieAuth() {
    return allowCookieAuth;
  }

  public void setBlockedRegions(List<String> blockedRegions) {
    this.blockedRegions = blockedRegions;
  }

  public List<String> getBlockedRegions() {
    return blockedRegions;
  }
}
