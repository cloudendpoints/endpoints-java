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
package com.google.api.server.spi.config.annotationreader;

import com.google.api.server.spi.config.model.ApiNamespaceConfig;
import com.google.common.base.Preconditions;

/**
 * Middle layer which translates {@link com.google.api.server.spi.config.ApiNamespace} annotations
 * into annotation agnostic {@link ApiNamespaceConfig}.
 */
public class ApiNamespaceAnnotationConfig {
  private final ApiNamespaceConfig config;

  public ApiNamespaceAnnotationConfig(ApiNamespaceConfig config) {
    this.config = Preconditions.checkNotNull(config, "config");
  }

  public ApiNamespaceConfig getConfig() {
    return config;
  }

  public void setOwnerDomainIfNotEmpty(String ownerDomain) {
    if (!Preconditions.checkNotNull(ownerDomain, "ownerDomain").isEmpty()) {
      config.setOwnerDomain(ownerDomain);
    }
  }

  public void setOwnerNameIfNotEmpty(String ownerName) {
    if (!Preconditions.checkNotNull(ownerName, "ownerName").isEmpty()) {
      config.setOwnerName(ownerName);
    }
  }

  public void setPackagePathIfNotEmpty(String packagePath) {
    if (!Preconditions.checkNotNull(packagePath, "packagePath").isEmpty()) {
      config.setPackagePath(packagePath);
    }
  }
}