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
import com.google.api.server.spi.types.DateAndTime;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Test service used for testing map schemas.
 */
@Api(transformers = StringValueTransformer.class)
public class MapEndpoint {

  public static class MapSubclass extends HashMap<Boolean, Integer> { }

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

  public Map<String, Map<String, Foo>> getFooMapMap() {
    return null;
  }

  public Map<String, StringValue> getStringValueMap() {
    return null;
  }

  public MapSubclass getMapSubclass() {
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

  //Keys that can be converted from / to String generate schema with additionalProperties

  public Map<TestEnum, String> getEnumKeyMap() {
    return null;
  }

  public Map<Boolean, String> getBooleanKeyMap() {
    return null;
  }

  public Map<Integer, String> getIntKeyMap() {
    return null;
  }

  public Map<Long, String> getLongKeyMap() {
    return null;
  }

  public Map<Float, String> getFloatKeyMap() {
    return null;
  }

  public Map<Date, String> getDateKeyMap() {
    return null;
  }

  @ApiMethod(path = "getDateTimeKeyMap")
  public Map<DateAndTime, String> getDateTimeKeyMap() {
    return null;
  }

  //Maps with array-like values generate a JsonMap schema (not supported by API client generator)
  //unless activated with MapSchemaFlag.SUPPORT_ARRAYS_VALUES

  public Map<String, String[]> getStringArrayMap() {
    return null;
  }

  @ApiMethod(path = "getStringCollectionMap")
  public Map<String, Collection<String>> getStringCollectionMap() {
    return null;
  }

}
