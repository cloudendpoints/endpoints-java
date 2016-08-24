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
import com.google.api.server.spi.config.ApiCacheControl;
import com.google.api.server.spi.config.ApiFrontendLimits;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethodCacheControl;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

/**
 * Test endpoint that subclasses {@link SubclassedEndpoint} and overrides multiple annotation
 * values.
 *
 * @author Eric Orth
 */
@Api(version = "v2",
     description = "overridden description",
     frontendLimits = @ApiFrontendLimits(unregisteredQps = 4),
     cacheControl = @ApiCacheControl(maxAge = 2),
     scopes = {"ss0a", "ss1a"},
     audiences = {"aa0a", "aa1a"},
     clientIds = {"cc0a", "cc1a"},
     defaultVersion = AnnotationBoolean.FALSE,
     transformers = { DumbSerializer2.class },
     useDatastoreForAdditionalConfig = AnnotationBoolean.FALSE,
     discoverable = AnnotationBoolean.FALSE)
public class SubclassedOverridingEndpoint extends SubclassedEndpoint {
  // Override an implementation but not the config.
  @Override
  public List<Foo> listFoos() {
    return Collections.singletonList(new Foo());
  }

  // Override a config.
  @ApiMethod(
      name = "foos.get2",
      cacheControl = @ApiMethodCacheControl(maxAge = 4)
  )
  @Override
  public Foo getFoo(@Named("id") String id) {
    return new Foo();
  }
}
