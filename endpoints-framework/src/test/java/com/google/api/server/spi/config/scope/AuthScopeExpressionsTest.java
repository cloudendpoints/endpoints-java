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

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the {@link AuthScopeExpressions} class.
 */
@RunWith(JUnit4.class)
public class AuthScopeExpressionsTest {
  public void testInterpretSingleScope() {
    assertEquals(new SingleAuthScopeExpression("scope_1"),
        AuthScopeExpressions.interpret("scope_1"));
  }

  @Test
  public void testEncodeSingleScope() {
    assertEquals(ImmutableList.of("scope_1"),
        AuthScopeExpressions.encode(new SingleAuthScopeExpression("scope_1")));
  }

  @Test
  public void testInterpretDisjunctScope() {
    assertEquals(
        new DisjunctAuthScopeExpression(ImmutableList.<AbstractAuthScopeExpression>of(
            new SingleAuthScopeExpression("scope_1"),
            new SingleAuthScopeExpression("scope_2"),
            new SingleAuthScopeExpression("scope_3"))),
        AuthScopeExpressions.interpret("scope_1", "scope_2", "scope_3"));
  }

  @Test
  public void testEncodeDisjunctScope() {
    assertEquals(ImmutableList.of("scope_1", "scope_2", "scope_3"),
        AuthScopeExpressions.encode(
            new DisjunctAuthScopeExpression(ImmutableList.<AbstractAuthScopeExpression>of(
                new SingleAuthScopeExpression("scope_1"),
                new SingleAuthScopeExpression("scope_2"),
                new SingleAuthScopeExpression("scope_3")))));
  }

  @Test
  public void testInterpretConjunctScope() {
    assertEquals(
        new ConjunctAuthScopeExpression(ImmutableList.<SingleAuthScopeExpression>of(
            new SingleAuthScopeExpression("scope_1"),
            new SingleAuthScopeExpression("scope_2"),
            new SingleAuthScopeExpression("scope_3"))),
        AuthScopeExpressions.interpret("scope_1 scope_2 scope_3"));
  }

  @Test
  public void testEncodeConjunctScope() {
    assertEquals(ImmutableList.of("scope_1 scope_2 scope_3"),
        AuthScopeExpressions.encode(
            new ConjunctAuthScopeExpression(ImmutableList.<SingleAuthScopeExpression>of(
                new SingleAuthScopeExpression("scope_1"),
                new SingleAuthScopeExpression("scope_2"),
                new SingleAuthScopeExpression("scope_3")))));
  }

  @Test
  public void testInterpretComplexScope() {
    assertEquals(
        new DisjunctAuthScopeExpression(ImmutableList.<AbstractAuthScopeExpression>of(
            new ConjunctAuthScopeExpression(ImmutableList.<SingleAuthScopeExpression>of(
                new SingleAuthScopeExpression("scope_1"),
                new SingleAuthScopeExpression("scope_2"))),
            new ConjunctAuthScopeExpression(ImmutableList.<SingleAuthScopeExpression>of(
                new SingleAuthScopeExpression("scope_1"),
                new SingleAuthScopeExpression("scope_3"))))),
        AuthScopeExpressions.interpret(
            "scope_1 scope_2",
            "scope_1 scope_3"));
  }

  @Test
  public void testEncodeComplexScope() {
    assertEquals(ImmutableList.of("scope_1 scope_2", "scope_1 scope_3"),
        AuthScopeExpressions.encode(
            new DisjunctAuthScopeExpression(ImmutableList.<AbstractAuthScopeExpression>of(
                new ConjunctAuthScopeExpression(ImmutableList.<SingleAuthScopeExpression>of(
                    new SingleAuthScopeExpression("scope_1"),
                    new SingleAuthScopeExpression("scope_2"))),
                new ConjunctAuthScopeExpression(ImmutableList.<SingleAuthScopeExpression>of(
                    new SingleAuthScopeExpression("scope_1"),
                    new SingleAuthScopeExpression("scope_3")))))));
  }
}
