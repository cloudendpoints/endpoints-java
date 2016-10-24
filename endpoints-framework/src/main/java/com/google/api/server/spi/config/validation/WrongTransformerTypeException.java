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

import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiParameterConfig;
import com.google.common.reflect.TypeToken;


/**
 * Exception for requesting that a {@link Transformer} transform a parameter of a type that the
 * Transformer does not support.
 *
 * @author Eric Orth
 */
public class WrongTransformerTypeException extends ApiParameterConfigInvalidException {
  public WrongTransformerTypeException(ApiParameterConfig config,
      Class<? extends Transformer<?, ?>> transformer, TypeToken<?> parameterType,
      TypeToken<?> sourceType) {
    super(config, getErrorMessage(transformer, parameterType, sourceType));
  }

  private static String getErrorMessage(Class<? extends Transformer<?, ?>> transformer,
      TypeToken<?> parameterType, TypeToken<?> sourceType) {
    return String.format("Bad transformer (%s). Specified for %s, but only transforms %s.",
        transformer, parameterType, sourceType);
  }
}
