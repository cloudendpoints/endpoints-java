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
package com.google.api.server.spi.discovery;

import com.google.common.base.Preconditions;

/**
 * Iteratively tracks the common path prefix of a list of paths. The common prefix returned will
 * only end after a slash and won't include any path parameters.
 */
class CommonPathPrefixBuilder {
  private String commonPrefix = null;

  void addPath(String path) {
    Preconditions.checkNotNull(path, "path");
    String prefix = getLongestPossiblePrefix(path);
    if (commonPrefix == null) {
      commonPrefix = prefix.substring(0, prefix.lastIndexOf('/') + 1);
      return;
    }
    for (int i = 0; i < commonPrefix.length(); i++) {
      if (i >= prefix.length() || commonPrefix.charAt(i) != prefix.charAt(i)) {
        commonPrefix = prefix.substring(0, i);
        commonPrefix = commonPrefix.substring(0, commonPrefix.lastIndexOf('/') + 1);
        return;
      }
    }
  }

  /**
   * Gets the common prefix of all paths that have been input so far.
   */
  String getCommonPrefix() {
    return commonPrefix;
  }

  private String getLongestPossiblePrefix(String path) {
    int paramIndex = path.indexOf('{');
    if (paramIndex == -1) {
      return path;
    }
    return path.substring(0, paramIndex);
  }
}
