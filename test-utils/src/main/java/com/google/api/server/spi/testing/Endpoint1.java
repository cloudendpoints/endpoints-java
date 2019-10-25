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
import com.google.api.server.spi.config.ApiAuth;
import com.google.api.server.spi.config.ApiCacheControl;
import com.google.api.server.spi.config.ApiFrontendLimitRule;
import com.google.api.server.spi.config.ApiFrontendLimits;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiMethodCacheControl;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Test endpoint with custom configuration and one resource collection.
 */
@Api(
    version = "v1",
    description = "API for testing",
    auth = @ApiAuth(
        allowCookieAuth = AnnotationBoolean.TRUE,
        blockedRegions = {"CU"}
    ),
    frontendLimits = @ApiFrontendLimits(
        unregisteredUserQps = 1,
        unregisteredQps = 2,
        unregisteredDaily = 3,
        rules = {
            @ApiFrontendLimitRule(
                match = "match0",
                qps = 1,
                userQps = 2,
                daily = 3,
                analyticsId = "analyticsId0"
            ),
            @ApiFrontendLimitRule(
                match = "match10",
                qps = 11,
                userQps = 12,
                daily = 13,
                analyticsId = "analyticsId10"
            )
        }
    ),
    cacheControl = @ApiCacheControl(
        type = ApiCacheControl.Type.PUBLIC,
        maxAge = 1
    ),
    scopes = {"ss0", "ss1 ss2"},
    audiences = {"aa0", "aa1"},
    clientIds = {"cc0", "cc1"},
    authenticators = { PassAuthenticator.class },
    defaultVersion = AnnotationBoolean.TRUE,
    transformers = { DumbSerializer1.class },
    useDatastoreForAdditionalConfig = AnnotationBoolean.TRUE
)
public class Endpoint1 {

  @ApiMethod(
      name = "foos.list",
      path = "foos",
      httpMethod = HttpMethod.GET,
      cacheControl = @ApiMethodCacheControl(
          noCache = true,
          maxAge = 1
      ),
      scopes = {"s0", "s1 s2"},
      audiences = {"a0", "a1"},
      clientIds = {"c0", "c1"},
      authenticators = { FailAuthenticator.class }
  )
  public List<Foo> listFoos() {
    return null;
  }

  @ApiMethod(
      name = "foos.get",
      path = "foos/{id}",
      httpMethod = HttpMethod.GET,
      cacheControl = @ApiMethodCacheControl(
          noCache = false,
          maxAge = 2
      )
  )
  public Foo getFoo(@Named("id") String id) {
    return null;
  }

  @ApiMethod(
      name = "foos.insert",
      path = "foos",
      httpMethod = HttpMethod.POST,
      cacheControl = @ApiMethodCacheControl(
          noCache = false,
          maxAge = 3
      )
  )
  public Foo insertFoo(Foo r) {
    return null;
  }

  @ApiMethod(
      name = "foos.update",
      path = "foos/{id}",
      httpMethod = HttpMethod.PUT,
      cacheControl = @ApiMethodCacheControl(
          noCache = false,
          maxAge = 4
      )
  )
  public Foo updateFoo(@Named("id") String id, Foo r) {
    return null;
  }

  @ApiMethod(
      name = "foos.remove",
      path = "foos/{id}",
      httpMethod = HttpMethod.DELETE,
      cacheControl = @ApiMethodCacheControl(
          noCache = false,
          maxAge = 5
      )
  )
  public void removeFoo(@Named("id") String id) {
  }

  @ApiMethod(
      name = "foos.execute0",
      path = "execute0",
      httpMethod = "POST"
  )
  public Object execute0(@Named("id") String id, @Named("i0") int i0,
      @Named("i1") @Nullable Integer i1, @Named("long0") long long0,
      @Nullable @Named("long1") Long long1, @Named("b0") boolean b0,
      @Nullable @Named("b1") Boolean b1, @Named("f") float f,
      @Nullable @Named("d") Double d) {
    return null;
  }

  public Map<String, Object> execute1(Foo r) {
    return null;
  }

  @ApiMethod(name = "foos.execute2")
  public void execute2(@Named("serialized") SimpleBean b) {
  }
}
