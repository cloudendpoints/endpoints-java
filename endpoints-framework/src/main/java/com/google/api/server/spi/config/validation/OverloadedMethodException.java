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
 * Exception for overloaded methods in an API class.
 *
 * @author Eric Orth
 */
public class OverloadedMethodException extends ApiClassConfigInvalidException {
  public OverloadedMethodException(ApiClassConfig config, String className, String method1,
      String method2) {
    super(config, getErrorMessage(className, method1, method2));
  }

  private static String getErrorMessage(String className, String method1, String method2) {
    return String.format(
        "Overloaded methods are not supported. %s has at least one overload: %s and %s",
        className, method1, method2);
  }
}
