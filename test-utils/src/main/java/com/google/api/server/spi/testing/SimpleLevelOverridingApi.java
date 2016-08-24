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

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiClass;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.AuthLevel;

/**
 * Test API class making use of level overriding.
 */
@Api(
    authLevel = AuthLevel.NONE,
    scopes = {"s0", "s1"},
    audiences = {"a0", "a1"},
    clientIds = {"c0", "c1"},
    resource = "resource",
    useDatastoreForAdditionalConfig = AnnotationBoolean.TRUE
)
@ApiClass(
    authLevel = AuthLevel.REQUIRED,
    scopes = {"s0a", "s1a"},
    audiences = {"a0a", "a1a"},
    clientIds = {"c0a", "c1a"},
    resource = "resource1",
    useDatastoreForAdditionalConfig = AnnotationBoolean.FALSE
)
public class SimpleLevelOverridingApi {
  public void noOverrides() {
  }

  @ApiMethod(
      authLevel = AuthLevel.OPTIONAL,
      path = "overridden",
      scopes = {"s0b", "s1b"},
      audiences = {"a0b", "a1b"},
      clientIds = {"c0b", "c1b"}
  )
  public void overrides() {
  }
}
