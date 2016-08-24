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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the {@link SingleAuthScopeExpression} class.
 */
@RunWith(JUnit4.class)
public class SingleAuthScopeExpressionTest {

  @Test
  public void testIsAuthorizedWithSuccess() {
    String scope = "some_scope";
    SingleAuthScopeExpression expression = new SingleAuthScopeExpression(scope);

    assertTrue(expression.isAuthorized(ImmutableSet.of("scope0", scope)));
  }

  @Test
  public void testIsAuthorizedWithFailure() {
    String scope = "some_scope";
    SingleAuthScopeExpression expression = new SingleAuthScopeExpression(scope);

    assertFalse(expression.isAuthorized(ImmutableSet.of("scope0")));
  }

  @Test
  public void testGetAllScopes() {
    SingleAuthScopeExpression expression = new SingleAuthScopeExpression("scope0");

    assertThat(ImmutableList.copyOf(expression.getAllScopes())).containsExactly("scope0");
  }

  @Test
  public void testToLoggingForm() {
    AuthScopeExpression expression = new SingleAuthScopeExpression("scopeValue");

    assertThat(expression.toLoggingForm()).isEqualTo("scopeValue");
  }

  @Test
  public void testToString() {
    AuthScopeExpression expression = new SingleAuthScopeExpression("scopeValue");

    assertThat(expression.toString()).isEqualTo("scopeValue");
  }
}
