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
import com.google.api.server.spi.config.Description;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.CollectionResponse;

@Api(
    name = "foo",
    version = "v1",
    audiences = {"audience"},
    title = "The Foo API",
    description = "Just Foo Things")
public class FooDescriptionEndpoint {
  @ApiMethod(name = "foo.create", description = "create desc", path = "foos/{id}",
      httpMethod = HttpMethod.PUT)
  public FooDescription createFoo(@Named("id") @Description("id desc") String id, FooDescription foo) {
    return null;
  }
  @ApiMethod(name = "foo.get", description = "get desc", path = "foos/{id}",
      httpMethod = HttpMethod.GET)
  public FooDescription getFoo(@Named("id") @Description("id desc") String id) {
    return null;
  }
  @ApiMethod(name = "foo.update", description = "update desc", path = "foos/{id}",
      httpMethod = HttpMethod.POST)
  public FooDescription updateFoo(@Named("id") @Description("id desc") String id, FooDescription foo) {
    return null;
  }
  @ApiMethod(name = "foo.delete", description = "delete desc", path = "foos/{id}",
      httpMethod = HttpMethod.DELETE)
  public FooDescription deleteFoo(@Named("id") @Description("id desc") String id) {
    return null;
  }
  @ApiMethod(name = "foo.list", description = "list desc", path = "foos",
      httpMethod = HttpMethod.GET)
  public CollectionResponse<FooDescription> listFoos(@Named("n") Integer n) {
    return null;
  }
  @ApiMethod(name = "toplevel", path = "foos", httpMethod = HttpMethod.POST)
  public CollectionResponse<FooDescription> toplevel() {
    return null;
  }
}
