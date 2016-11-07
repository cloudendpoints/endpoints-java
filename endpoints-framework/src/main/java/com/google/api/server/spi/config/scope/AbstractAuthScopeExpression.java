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
package com.google.api.server.spi.config.scope;

import java.util.List;

/**
 * A base class for all AuthScopeExpression implementations, allowing for package visible
 * capabilities.
 */
abstract class AbstractAuthScopeExpression implements AuthScopeExpression {
  /**
   * Encodes the expression to match what the {@link AuthScopeExpressions#interpret(List)} would
   * accept.
   */
  abstract List<String> encode();

  /**
   * Encodes the expression to match what {@link AuthScopeExpressions#interpret(List)}
   * accepts. The returned list is mutable.
   */
  abstract List<String> encodeMutable();

  @Override
  public String toString() {
    return toLoggingForm();
  }
}
