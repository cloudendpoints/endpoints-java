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


import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.model.ApiAuthConfig;

import java.util.Arrays;

/**
 * Flattened auth configuration for a swarm endpoint.  Data generally originates from
 * {@link com.google.api.server.spi.config.ApiAuth} annotations.
 *
 * @author Eric Orth
 */
class ApiAuthAnnotationConfig {
  private ApiAuthConfig config;

  public ApiAuthAnnotationConfig(ApiAuthConfig config) {
    this.config = config;
  }

  public ApiAuthConfig getConfig() {
    return config;
  }

  public void setAllowCookieAuthIfSpecified(AnnotationBoolean allowCookieAuth) {
    if (allowCookieAuth == AnnotationBoolean.TRUE) {
      config.setAllowCookieAuth(true);
    } else if (allowCookieAuth == AnnotationBoolean.FALSE) {
      config.setAllowCookieAuth(false);
    }
  }

  public void setBlockedRegionsIfNotEmpty(String[] blockedRegions) {
    if (blockedRegions != null && blockedRegions.length > 0) {
      config.setBlockedRegions(Arrays.asList(blockedRegions));
    }
  }
}
