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

import java.util.Collection;

/**
 * Exception for parameter types with multiple serializers available from its superclass or
 * interfaces.
 *
 * @author Eric Orth
 */
public class MultipleTransformersException extends ApiParameterConfigInvalidException {
  public MultipleTransformersException(ApiParameterConfig config,
      Collection<Class<? extends Transformer<?, ?>>> serializers) {
    super(config, getErrorMessage(serializers));
  }

  private static String getErrorMessage(
      Collection<Class<? extends Transformer<?, ?>>> serializers) {
    return String.format("Found multiple transformers for parameter type. Only one superclass or "
        + "implemented interface may be annotated. Transformed found: %s.", serializers);
  }
}
