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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;

/**
 * Tests for the {@link DisjunctAuthScopeExpression} class.
 */
@RunWith(JUnit4.class)
public class DisjunctAuthScopeExpressionTest {

  @Test
  public void testIsAuthorizedWithNoInnerExpressions() {
    DisjunctAuthScopeExpression expression =
        new DisjunctAuthScopeExpression(ImmutableList.<AbstractAuthScopeExpression>of());

    assertFalse(expression.isAuthorized(ImmutableSet.of("scope0")));
  }

  @Test
  public void testIsAuthorizedWithSuccess() {
    Set<String> scopes = ImmutableSet.of("scope0", "scope1", "scope2", "scope3");
    AbstractAuthScopeExpression expression1 = mock(AbstractAuthScopeExpression.class);
    AbstractAuthScopeExpression expression2 = mock(AbstractAuthScopeExpression.class);
    when(expression2.isAuthorized(scopes)).thenReturn(true);
    AbstractAuthScopeExpression expression3 = mock(AbstractAuthScopeExpression.class);
    when(expression3.isAuthorized(scopes)).thenReturn(true);

    DisjunctAuthScopeExpression expression =
        new DisjunctAuthScopeExpression(
            ImmutableList.<AbstractAuthScopeExpression>of(expression1, expression2, expression3));

    assertTrue(expression.isAuthorized(scopes));
  }

  @Test
  public void testIsAuthorizedWithFailure() {
    Set<String> scopes = ImmutableSet.of("scope0", "scope1", "scope2", "scope3");
    AbstractAuthScopeExpression expression1 = mock(AbstractAuthScopeExpression.class);
    AbstractAuthScopeExpression expression2 = mock(AbstractAuthScopeExpression.class);
    AbstractAuthScopeExpression expression3 = mock(AbstractAuthScopeExpression.class);

    DisjunctAuthScopeExpression expression =
        new DisjunctAuthScopeExpression(
            ImmutableList.<AbstractAuthScopeExpression>of(expression1, expression2, expression3));

    assertFalse(expression.isAuthorized(scopes));
  }

  @Test
  public void testGetAllScopes() {
    AbstractAuthScopeExpression expression1 = mock(AbstractAuthScopeExpression.class);
    when(expression1.getAllScopes()).thenReturn(new String[] { "scope0" });
    AbstractAuthScopeExpression expression2 = mock(AbstractAuthScopeExpression.class);
    when(expression2.getAllScopes()).thenReturn(new String[] { "scope1" });
    AbstractAuthScopeExpression expression3 = mock(AbstractAuthScopeExpression.class);
    when(expression3.getAllScopes()).thenReturn(new String[] { "scope0", "scope2" });

    DisjunctAuthScopeExpression expression =
        new DisjunctAuthScopeExpression(
            ImmutableList.<AbstractAuthScopeExpression>of(expression1, expression2, expression3));

    assertThat(ImmutableList.copyOf(expression.getAllScopes()))
        .containsExactly("scope0", "scope1", "scope2");
  }

  @Test
  public void testToLoggingForm() {
    AbstractAuthScopeExpression expression1 = new SingleAuthScopeExpression("scope1");
    AbstractAuthScopeExpression expression2 = new SingleAuthScopeExpression("scope2");
    AbstractAuthScopeExpression expression3 = new SingleAuthScopeExpression("scope3");

    AuthScopeExpression expression =
        new DisjunctAuthScopeExpression(ImmutableList.of(expression1, expression2, expression3));
    assertThat(expression.toLoggingForm()).isEqualTo("(scope1 || scope2 || scope3)");
  }

  @Test
  public void testToString() {
    AbstractAuthScopeExpression expression1 = new SingleAuthScopeExpression("scope1");
    AbstractAuthScopeExpression expression2 = new DisjunctAuthScopeExpression(
        ImmutableList.<AbstractAuthScopeExpression>of(
            new SingleAuthScopeExpression("scope2"), new SingleAuthScopeExpression("scope3")));
    AbstractAuthScopeExpression expression3 = new ConjunctAuthScopeExpression(ImmutableList.of(
        new SingleAuthScopeExpression("scope4"), new SingleAuthScopeExpression("scope5")));

    AuthScopeExpression expression =
        new DisjunctAuthScopeExpression(ImmutableList.of(expression1, expression2, expression3));
    assertThat(expression.toString())
        .isEqualTo("(scope1 || (scope2 || scope3) || (scope4 && scope5))");
  }
}
