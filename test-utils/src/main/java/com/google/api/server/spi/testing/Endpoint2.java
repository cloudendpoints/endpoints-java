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
package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;

/**
 * Test endpoint with no public methods.
 */
@Api
public class Endpoint2 {

  @ApiMethod(
      name = "api2.foos.invisible0",
      path = "invisible0",
      httpMethod = HttpMethod.POST
  )
  protected String invisible0() {
    return null;
  }

  @SuppressWarnings("unused")
  @ApiMethod(
      name = "api2.foos.invisible1",
      path = "invisible1",
      httpMethod = HttpMethod.POST
  )
  private String invisible1() {
    return null;
  }
}
