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
import com.google.api.server.spi.config.ApiNamespace;
import com.google.common.collect.Iterables;

import java.util.Objects;

/**
 * Flattened Api namespace configuration originating from {@link ApiNamespace}.
 */
public class ApiNamespaceConfig {
  private String ownerDomain;
  private String ownerName;
  private String packagePath;

  public ApiNamespaceConfig() {
    setDefaults();
  }

  public ApiNamespaceConfig(ApiNamespaceConfig original) {
    this.ownerDomain = original.ownerDomain;
    this.ownerName = original.ownerName;
    this.packagePath = original.packagePath;
  }

  /**
   * Sets all fields to their default value to be used if not set otherwise.
   * Override to change the default configuration.
   */
  protected void setDefaults() {
    ownerDomain = "";
    ownerName = "";
    packagePath = "";
  }

  public void setOwnerDomain(String ownerDomain) {
    this.ownerDomain = ownerDomain;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public void setPackagePath(String packagePath) {
    this.packagePath = packagePath;
  }

  public String getOwnerDomain() {
    return ownerDomain;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public String getPackagePath() {
    return packagePath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ApiNamespaceConfig)) {
      return false;
    }
    ApiNamespaceConfig that = (ApiNamespaceConfig) o;
    return Iterables.isEmpty(getConfigurationInconsistencies(that));
  }

  public Iterable<ApiConfigInconsistency<String>> getConfigurationInconsistencies(
      ApiNamespaceConfig config) {
    return ApiConfigInconsistency.<String>listBuilder()
        .addIfInconsistent("namespace.ownerDomain", ownerDomain, config.ownerDomain)
        .addIfInconsistent("namespace.ownerName", ownerName, config.ownerName)
        .addIfInconsistent("namespace.packagePath", packagePath, config.packagePath)
        .build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(ownerDomain, ownerName, packagePath);
  }
}