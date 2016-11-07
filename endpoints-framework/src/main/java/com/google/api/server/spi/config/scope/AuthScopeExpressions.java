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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

/**
 * Interpreter for a method's {@code AuthScopeExpression} in its annotation-provided configuration
 * form.
 */
public class AuthScopeExpressions {
  static final Joiner CONJUNCT_SCOPE_JOINER = Joiner.on(" ");
  private static final Splitter CONJUNCT_SCOPE_SPLITTER = Splitter.on(" ").omitEmptyStrings();

  /**
   * Blocks construction.
   */
  private AuthScopeExpressions() {
  }

  /**
   * Interprets the annotation-provided scope strings into an {@code AuthScopeExpression};
   */
  public static AuthScopeExpression interpret(String... scopes) {
    return interpret(Arrays.asList(scopes));
  }

  /**
   * Interprets the list of annotation-provided scope strings into an {@code AuthScopeExpression};
   */
  public static AuthScopeExpression interpret(List<String> scopes) {
    List<AbstractAuthScopeExpression> innerAuthScopeExpressions = Lists.newArrayList();
    for (String scope : scopes) {
      AbstractAuthScopeExpression innerAuthScopeExpression = interpretConjunct(scope);
      if (!innerAuthScopeExpressions.contains(innerAuthScopeExpression)) {
        innerAuthScopeExpressions.add(innerAuthScopeExpression);
      }
    }
    if (innerAuthScopeExpressions.size() == 1) {
      return innerAuthScopeExpressions.get(0);
    } else {
      return new DisjunctAuthScopeExpression(innerAuthScopeExpressions);
    }
  }

  /**
   * Encodes an {@code AuthScopeExpression} back into its String List form.
   */
  public static List<String> encode(AuthScopeExpression authScopeExpression) {
    try {
      return ((AbstractAuthScopeExpression) authScopeExpression).encode();
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(
          "Expressions of type " + authScopeExpression.getClass() + " are not supported.");
    }
  }

  /**
   * Encodes an {@code AuthScopeExpression} back into its mutable String List form.
   */
  public static List<String> encodeMutable(AuthScopeExpression authScopeExpression) {
    try {
      return ((AbstractAuthScopeExpression) authScopeExpression).encodeMutable();
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(
          "Expressions of type " + authScopeExpression.getClass() + " are not supported.");
    }
  }

  private static AbstractAuthScopeExpression interpretConjunct(String conjunctScopeExpression) {
    List<String> scopes = CONJUNCT_SCOPE_SPLITTER.splitToList(conjunctScopeExpression);
    if (scopes.size() == 1) {
      return new SingleAuthScopeExpression(scopes.get(0));
    } else {
      List<SingleAuthScopeExpression> innerAuthScopeExpressions = Lists.newArrayList();
      for (String scope : scopes) {
        SingleAuthScopeExpression innerAuthScopeExpression = new SingleAuthScopeExpression(scope);
        if (!innerAuthScopeExpressions.contains(innerAuthScopeExpression)) {
          innerAuthScopeExpressions.add(innerAuthScopeExpression);
        }
      }
      return new ConjunctAuthScopeExpression(innerAuthScopeExpressions);
    }
  }
}
