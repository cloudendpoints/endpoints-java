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
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.api.server.spi.config.Description;
import com.google.api.server.spi.response.CollectionResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Test service used for testing array schemas.
 */
@Api(transformers = StringValueTransformer.class)
public class MapEndpoint {

  public MapEndpoint getMapService() {
    return null;
  }

  public Map<String, String> getStringMap() {
    return null;
  }

  public Map<String, Integer> getIntMap() {
    return null;
  }

  public Map<String, Foo> getFooMap() {
    return null;
  }

  public Map<String, Baz> getBazMap() {
    return null;
  }

  public Map<String, Map<String, Foo>> getFooMap2() {
    return null;
  }

  public Map<StringValue, String> getStringValueKeyMap() {
    return null;
  }

  @ApiMethod(path = "getMapOfStrings")
  public MapContainer getMapOfStrings() {
    return null;
  }

  public static class MapContainer {
    @ApiResourceProperty(description = "A map of string values")
    public Map<String, StringValue> stringMap;
  }

  //Map types below are still generating JsonMap schema for now

  public Map<TestEnum, String> getEnumyMap() {
    return null;
  }

  public Map<String, String[]> getStringArrayMap() {
    return null;
  }

  @ApiMethod(path = "getStringCollectionMap")
  public Map<String, Collection<String>> getStringCollectionMap() {
    return null;
  }

}
