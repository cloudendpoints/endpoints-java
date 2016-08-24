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
package com.google.api.server.spi.response;

import com.google.api.server.spi.Constant;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for writing responses.
 */
public final class ResponseUtil {

  private ResponseUtil() {}

  /**
   * Wraps a collection in an "items" property.
   */
  static Object wrapCollection(Object value) {
    if (isCollection(value)) {
      Map<String, Object> wrapped = new HashMap<String, Object>();
      wrapped.put(Constant.ITEMS, value);
      return wrapped;
    } else {
      return value;
    }
  }

  /**
   * Returns {@code true} if {@code value} is either a collection or an array; {@code false}
   * otherwise.
   */
  private static boolean isCollection(Object value) {
    return value == null ? false : value instanceof Collection<?> || value.getClass().isArray();
  }
}
