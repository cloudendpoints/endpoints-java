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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

/**
 * An {@code AuthScopeExpression} that determines if a {@code User} has a particular OAuth scope.
 */
class SingleAuthScopeExpression extends AbstractAuthScopeExpression {

  private final String scope;

  SingleAuthScopeExpression(String scope) {
    this.scope = Preconditions.checkNotNull(scope);
  }

  @Override
  public String[] getAllScopes() {
    return new String[] {scope};
  }

  @Override
  public boolean isAuthorized(Set<String> userScopes) {
    return userScopes.contains(scope);
  }

  String getScope() {
    return scope;
  }

  @Override
  List<String> encode() {
    return ImmutableList.of(scope);
  }

  @Override
  List<String> encodeMutable() {
    return Lists.newArrayList(scope);
  }

  @Override
  public String toLoggingForm() {
    return scope;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof SingleAuthScopeExpression)) {
      return false;
    }
    SingleAuthScopeExpression that = (SingleAuthScopeExpression) obj;
    return scope.equals(that.scope);
  }

  @Override
  public int hashCode() {
    return scope.hashCode();
  }
}
