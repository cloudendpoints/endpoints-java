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


import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.model.ApiFrontendLimitsConfig;

/**
 * Flattened frontend limits configuration for a swarm endpoint.  Data generally originates from
 * {@link com.google.api.server.spi.config.ApiFrontendLimits} annotations.
 *
 * @author Eric Orth
 */
class ApiFrontendLimitsAnnotationConfig {
  private ApiFrontendLimitsConfig config;

  public ApiFrontendLimitsAnnotationConfig(ApiFrontendLimitsConfig config) {
    this.config = config;
  }

  public ApiFrontendLimitsConfig getConfig() {
    return config;
  }

  public void setUnregisteredUserQpsIfSpecified(int unregisteredUserQps) {
    if (unregisteredUserQps != Api.UNSPECIFIED_INT) {
      config.setUnregisteredUserQps(unregisteredUserQps);
    }
  }

  public void setUnregisteredQpsIfSpecified(int unregisteredQps) {
    if (unregisteredQps != Api.UNSPECIFIED_INT) {
      config.setUnregisteredQps(unregisteredQps);
    }
  }

  public void setUnregisteredDailyIfSpecified(int unregisteredDaily) {
    if (unregisteredDaily != Api.UNSPECIFIED_INT) {
      config.setUnregisteredDaily(unregisteredDaily);
    }
  }
}
