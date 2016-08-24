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

import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.model.ApiParameterConfig;

/**
 * Exception for any invalid configuration specific to an API parameter.
 *
 * @author Eric Orth
 */
public class ApiParameterConfigInvalidException extends ApiConfigException {
  public ApiParameterConfigInvalidException(ApiParameterConfig config, String message) {
    super(getErrorMessage(config, message));
  }

  private static String getErrorMessage(ApiParameterConfig config, String message) {
    String apiName = config.getApiMethodConfig().getApiClassConfig().getApiConfig().getName();
    String className = config.getApiMethodConfig().getApiClassConfig().getApiClassJavaName();
    String methodName = config.getApiMethodConfig().getName();
    // Use the parameter name if it has one, otherwise identify the parameter by its type.
    String parameterName = config.getName() == null ? String.format("(type %s)", config.getType()) :
                                                      config.getName();

    return String.format("%s.%s.%s parameter %s: %s", apiName, className, methodName, parameterName,
        message);
  }
}
