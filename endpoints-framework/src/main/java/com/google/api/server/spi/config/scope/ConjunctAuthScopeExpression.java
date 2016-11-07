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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * An {@code AuthScopeExpression} that determines if a {@code User} is authorized for all contained
 * {@code AuthScopeExpression} instances.
 */
class ConjunctAuthScopeExpression extends AbstractAuthScopeExpression {

  private final ImmutableList<SingleAuthScopeExpression> innerExpressions;

  ConjunctAuthScopeExpression(List<SingleAuthScopeExpression> innerExpressions) {
    this.innerExpressions = ImmutableList.copyOf(innerExpressions);
  }

  @Override
  public String[] getAllScopes() {
    Set<String> allScopes = Sets.newHashSet();
    for (AuthScopeExpression innerExpression : innerExpressions) {
      allScopes.addAll(Arrays.asList(innerExpression.getAllScopes()));
    }
    return allScopes.toArray(new String[allScopes.size()]);
  }

  @Override
  public boolean isAuthorized(Set<String> userScopes) {
    if (innerExpressions.isEmpty()) {
      return false;
    }
    for (AuthScopeExpression innerExpression : innerExpressions) {
      if (!innerExpression.isAuthorized(userScopes)) {
        return false;
      }
    }
    return true;
  }

  @Override
  List<String> encode() {
    return ImmutableList.of(doEncode());
  }

  @Override
  List<String> encodeMutable() {
    return Lists.newArrayList(doEncode());
  }

  @Override
  public String toLoggingForm() {
    return "("
        + Joiner.on(" && ")
            .join(Iterables.transform(innerExpressions,
                new Function<AuthScopeExpression, String>() {
                  @Override
                  public String apply(AuthScopeExpression scopeExpression) {
                    return scopeExpression.toLoggingForm();
                  }
                }))
        + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof ConjunctAuthScopeExpression)) {
      return false;
    }
    ConjunctAuthScopeExpression that = (ConjunctAuthScopeExpression) obj;
    return innerExpressions.equals(that.innerExpressions);
  }

  @Override
  public int hashCode() {
    return innerExpressions.hashCode();
  }

  private String doEncode() {
    List<String> scopes = Lists.newArrayList();
    for (SingleAuthScopeExpression innerExpression : innerExpressions) {
      scopes.add(innerExpression.getScope());
    }
    return AuthScopeExpressions.CONJUNCT_SCOPE_JOINER.join(scopes);
  }
}
