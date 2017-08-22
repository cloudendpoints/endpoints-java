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
package com.google.api.server.spi.dispatcher;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Possible HTTP methods for use in the dispatcher.
 */
enum HttpMethod {
  GET,
  POST,
  PUT,
  DELETE,
  PATCH;

  private static final ImmutableMap<String, HttpMethod> STRING_TO_ENUM =
      ImmutableMap.<String, HttpMethod>builder()
          .put("GET", GET)
          .put("POST", POST)
          .put("PUT", PUT)
          .put("DELETE", DELETE)
          .put("PATCH", PATCH)
          .build();

  /**
   * Returns an {@link HttpMethod} corresponding to a string value, or null if it doesn't exist.
   */
  public static HttpMethod fromString(String method) {
    Preconditions.checkNotNull(method, "method");
    return STRING_TO_ENUM.get(method.toUpperCase());
  }
}
