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
 * Test base service in order to make sure inherited methods filter through.
 */
class Base {
  public Object fn1() {
    return null;
  }

  @ApiMethod(
      name = "api6.foos.fn2",
      path = "fn2",
      httpMethod = HttpMethod.GET
  )
  public Object fn2() {
    return null;
  }
}

/**
 * Test service which overrides a method with covariant return type,
 * resulting in bridge methods
 */
@Api
public class BridgeInheritanceEndpoint extends Base {
  @Override
  @ApiMethod(
      name = "api6.foos.fn1",
      path = "fn1",
      httpMethod = HttpMethod.GET
  )
  public Object fn1() {
    return null;
  }
}
