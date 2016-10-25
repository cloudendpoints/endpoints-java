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
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.CollectionResponse;

@Api(name = "foo", version = "v1", audiences = {"audience"})
public class FooEndpoint {
  @ApiMethod(name = "foo.create", description = "create desc", path = "foos/{id}",
      httpMethod = HttpMethod.PUT)
  public Foo createFoo(@Named("id") String id, Foo foo) {
    return null;
  }
  @ApiMethod(name = "foo.get", description = "get desc", path = "foos/{id}",
      httpMethod = HttpMethod.GET)
  public Foo getFoo(@Named("id") String id) {
    return null;
  }
  @ApiMethod(name = "foo.update", description = "update desc", path = "foos/{id}",
      httpMethod = HttpMethod.POST)
  public Foo updateFoo(@Named("id") String id, Foo foo) {
    return null;
  }
  @ApiMethod(name = "foo.delete", description = "delete desc", path = "foos/{id}",
      httpMethod = HttpMethod.DELETE)
  public Foo deleteFoo(@Named("id") String id) {
    return null;
  }
  @ApiMethod(name = "foo.list", description = "list desc", path = "foos",
      httpMethod = HttpMethod.GET)
  public CollectionResponse<Foo> listFoos(@Named("n") Integer n) {
    return null;
  }
  @ApiMethod(name = "toplevel", path = "foos", httpMethod = HttpMethod.POST)
  public CollectionResponse<Foo> toplevel() {
    return null;
  }
}