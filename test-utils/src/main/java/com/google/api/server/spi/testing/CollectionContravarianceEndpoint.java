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

import java.util.Collection;
import java.util.List;

import javax.inject.Named;

class BaseCollectionContravarianceEndpoint {
  @ApiMethod(name = "api.foos.base", path = "base", httpMethod = HttpMethod.GET)
  public Foo foo(List<String> id) {
    return null;
  }
}

/**
 * Tests contravariance of parameter type. Not supported in Java, should fail.
 */
@Api
public class CollectionContravarianceEndpoint extends BaseCollectionContravarianceEndpoint {
  @ApiMethod(name = "api.foos.fn", path = "fn", httpMethod = HttpMethod.GET)
  public Foo foo(@Named("id") Collection<String> id) {
    return null;
  }

  /**
   * Overloading with {@code foo(java.util.List<Object> id)} will not compile.
   * This is not allowed due to type erasure: a non-override (different parameterized types),
   * but same runtime types. Errors at compile time.
   */
}
