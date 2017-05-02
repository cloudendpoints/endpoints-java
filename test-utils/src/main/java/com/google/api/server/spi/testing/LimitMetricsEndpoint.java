/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiLimitMetric;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMetricCost;

/**
 * Testing for API methods that have absolute paths.
 */
@Api(
    name = "limits",
    version = "v1",
    limitDefinitions = {
        @ApiLimitMetric(
            name = "read",
            displayName = "Read requests",
            limit = 100),
        @ApiLimitMetric(
            name = "write",
            limit = 10),
    })
public class LimitMetricsEndpoint {
  @ApiMethod(
      name = "create",
      path = "create",
      metricCosts = {@ApiMetricCost(name = "write", cost = 5)})
  public Foo createFoo() {
    return null;
  }

  @ApiMethod(
      name = "custom",
      metricCosts = {
          @ApiMetricCost(name = "read", cost = 1),
          @ApiMetricCost(name = "write", cost = 2)
      })
  public void customFoo() { }
}
