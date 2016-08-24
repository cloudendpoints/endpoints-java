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

import com.google.api.server.spi.config.model.ApiMethodConfig;

/**
 * Exception for custom authenticators/peer authenticators without a nullary constructor.
 *
 * @author Eric Orth
 */
public class InvalidConstructorException extends ApiMethodConfigInvalidException {
  public InvalidConstructorException(Class<?> clazz, ApiMethodConfig config, String description) {
    super(config, getErrorMessage(clazz, description));
  }

  private static String getErrorMessage(Class<?> clazz, String description) {
    return String.format("Invalid %s %s. It must have a public nullary constructor.", description,
        clazz.getName());
  }
}
