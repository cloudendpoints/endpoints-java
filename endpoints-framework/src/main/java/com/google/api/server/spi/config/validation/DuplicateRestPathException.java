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
package com.google.api.server.spi.config.validation;

import com.google.api.server.spi.config.model.ApiClassConfig;

/**
 * Exception for API configurations that would result in ambiguous REST paths.
 *
 * @author Eric Orth
 */
public class DuplicateRestPathException extends ApiClassConfigInvalidException {
  public DuplicateRestPathException(ApiClassConfig config, String restSignature, String method1,
      String method2) {
    super(config, getErrorMessage(restSignature, method1, method2));
  }

  private static String getErrorMessage(String restSignature, String method1, String method2) {
    return String.format("Multiple methods with same rest path \"%s\": \"%s\" and \"%s\"",
        restSignature, method1, method2);
  }
}
