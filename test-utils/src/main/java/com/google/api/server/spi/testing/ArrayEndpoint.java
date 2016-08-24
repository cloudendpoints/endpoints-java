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
import com.google.api.server.spi.response.CollectionResponse;

import java.util.Collection;

/**
 * Test service used for testing array schemas.
 */
@Api
public class ArrayEndpoint {

  public ArrayEndpoint getArrayService() {
    return null;
  }

  public Collection<Foo> getFoos() {
    return null;
  }

  public Baz getBaz() {
    return null;
  }

  @ApiMethod(path = "getAllFoos")
  public Collection<Collection<Foo>> getAllFoos() {
    return null;
  }

  @ApiMethod(path = "getArrayedFoos")
  public Foo[] getArrayedFoos() {
    return null;
  }

  public Foo[][] getAllArrayedFoos() {
    return null;
  }

  public CollectionResponse<Foo> getFoosResponse() {
    return null;
  }

  @ApiMethod(path = "getAllFoosResponse")
  public CollectionResponse<Collection<Foo>> getAllFoosResponse() {
    return null;
  }

  @ApiMethod(path = "getAllNestedFoosResponse")
  public CollectionResponse<Collection<Collection<Foo>>> getAllNestedFoosResponse() {
    return null;
  }

  public int[] getIntegers() {
    return null;
  }

  public Integer[] getObjectIntegers() {
    return null;
  }

  @ApiMethod(path = "getIntegersResponse")
  public CollectionResponse<Integer> getIntegersResponse() {
    return null;
  }
}
