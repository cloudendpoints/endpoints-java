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
import com.google.api.server.spi.config.ApiCacheControl;
import com.google.api.server.spi.config.ApiFrontendLimits;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiReference;

import javax.inject.Named;

/**
 * Test endpoint that references {@link SubclassedEndpoint} and overrides multiple annotation
 * values.  Extends {@link SubclassedOverridingEndpoint} to test that API and class configuration is
 * not loaded through inheritance.  Method configuration still comes through inheritance.
 *
 * @author Eric Orth
 */
@ApiReference(SubclassedEndpoint.class)
@Api(version = "v3",
     description = "more overridden description",
     frontendLimits = @ApiFrontendLimits(unregisteredQps = 4),
     cacheControl = @ApiCacheControl(maxAge = 2),
     scopes = {"ss0b", "ss1b"},
     audiences = {"aa0b", "aa1b"},
     clientIds = {"cc0b", "cc1b"},
     transformers = { DumbSerializer2.class })
public class ReferenceOverridingEndpoint extends SubclassedOverridingEndpoint {
  @ApiMethod(
      name = "foos.get3",
      path = "foos/{id}",
      httpMethod = HttpMethod.GET
  )
  @Override
  public Foo getFoo(@Named("id") String id) {
    return null;
  }
}
