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

import com.google.common.base.CharMatcher;

import java.util.List;

/**
 * Common Utilities.
 */
public class Strings {
  public static boolean isEmptyOrWhitespace(String string) {
    return string == null || string.trim().isEmpty();
  }

  public static boolean isEmptyOrNull(List<String> list) {
    return list == null || list.isEmpty();
  }

  public static boolean isWhitelisted(String item, List<String> whitelist) {
    if (isEmptyOrNull(whitelist) || isEmptyOrWhitespace(item)) {
      return false;
    }
    return whitelist.contains(item);
  }

  /**
   * Strips the lead slash from a string if it has a lead slash; otherwise return the
   * string unchanged.
   */
  public static String stripLeadingSlash(String s) {
    return s == null ? null : CharMatcher.is('/').trimLeadingFrom(s);
  }

  /**
   * Strips the trailing slash from a string if it has a trailing slash; otherwise return the
   * string unchanged.
   */
  public static String stripTrailingSlash(String s) {
    return s == null ? null : CharMatcher.is('/').trimTrailingFrom(s);
  }

  /**
   * Strips the leading and trailing slash from a string if it has a trailing slash; otherwise
   * return the string unchanged.
   */
  public static String stripSlash(String s) {
    return s == null ? null : CharMatcher.is('/').trimFrom(s);
  }
}
