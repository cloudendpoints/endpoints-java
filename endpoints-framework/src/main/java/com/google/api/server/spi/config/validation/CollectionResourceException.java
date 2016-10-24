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

import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.common.reflect.TypeToken;

/**
 * Exception for arrays or collections of entity (resource) type parameters.
 *
 * @author Eric Orth
 */
public class CollectionResourceException extends ApiParameterConfigInvalidException {
  public CollectionResourceException(
      ApiParameterConfig config, TypeToken<?> repeatedItemType, TypeToken<?> type) {
    super(config, getErrorMessage(repeatedItemType, type));
  }

  private static String getErrorMessage(TypeToken<?> repeatedItemType, TypeToken<?> type) {
    return String.format(
        "Illegal parameter type ('%s' in collection type '%s').  Arrays or collections of entity "
            + "types are not allowed.",
        repeatedItemType, type);
  }
}
