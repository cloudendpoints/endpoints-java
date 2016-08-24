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

import java.util.Set;

/**
 * Representation of the set of OAuth scopes that are required to invoke a method.
 */
public interface AuthScopeExpression {
  /**
   * Gets all OAuth scopes that are involved with this expression.
   */
  String[] getAllScopes();

  /**
   * Determines whether a set of scopes that a user has is sufficient for the expression.
   */
  boolean isAuthorized(Set<String> userScopes);

  /**
   * Gets a stringified form of the expression that can be used for logging.
   */
  String toLoggingForm();
}
