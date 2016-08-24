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
package com.google.api.server.spi;

import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.model.ApiSerializationConfig;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * Utilities for {@link ObjectMapper}.
 */
public class ObjectMapperUtil {

  /**
   * Creates an Endpoints standard object mapper that allows unquoted field names and unknown
   * properties.
   *
   * Note on unknown properties: When Apiary FE supports a strict mode where properties
   * are checked against the schema, BE can just ignore unknown properties.  This way, FE does
   * not need to filter out everything that the BE doesn't understand.  Before that's done,
   * a property name with a typo in it, for example, will just be ignored by the BE.
   */
  public static ObjectMapper createStandardObjectMapper() {
    return createStandardObjectMapper(null);
  }

  /**
   * Creates an Endpoints standard object mapper that allows unquoted field names and unknown
   * properties.
   *
   * Note on unknown properties: When Apiary FE supports a strict mode where properties
   * are checked against the schema, BE can just ignore unknown properties.  This way, FE does
   * not need to filter out everything that the BE doesn't understand.  Before that's done,
   * a property name with a typo in it, for example, will just be ignored by the BE.
   */
  public static ObjectMapper createStandardObjectMapper(ApiSerializationConfig config) {
    ObjectMapper objectMapper = new ObjectMapper()
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    AnnotationIntrospector pair = AnnotationIntrospector.pair(
        new ApiAnnotationIntrospector(config), new JacksonAnnotationIntrospector());
    objectMapper.setAnnotationIntrospector(pair);
    return objectMapper;
  }
}
