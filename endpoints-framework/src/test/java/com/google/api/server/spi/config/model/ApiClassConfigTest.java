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
package com.google.api.server.spi.config.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link ApiClassConfig}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiClassConfigTest {
  private ApiClassConfig config;

  @Mock
  ApiConfig apiConfig;

  private static final AuthScopeExpression defaultScopeExpression =
      AuthScopeExpressions.interpret("s1", "s2");
  private static final List<String> defaultAudiences = Lists.newArrayList("a1", "a2");
  private static final List<String> defaultClientIds = Lists.newArrayList("c1", "c2");

  private static final AuthScopeExpression defaultScopeExpression2 =
      AuthScopeExpressions.interpret("s1", "s2", "s3");
  private static final List<String> defaultAudiences2 = Lists.newArrayList("a1");
  private static final List<String> defaultClientIds2 = Lists.newArrayList("c1", "c2", "c3");

  @Before
  public void setUp()  throws Exception {
    Mockito.when(apiConfig.getResource()).thenReturn("resource");
    Mockito.when(apiConfig.getAuthLevel()).thenReturn(AuthLevel.NONE);
    Mockito.when(apiConfig.getScopeExpression()).thenReturn(defaultScopeExpression);
    Mockito.when(apiConfig.getAudiences()).thenReturn(defaultAudiences);
    Mockito.when(apiConfig.getClientIds()).thenReturn(defaultClientIds);
    Mockito.when(apiConfig.getUseDatastore()).thenReturn(false);

    config = new ApiClassConfig(apiConfig, new TypeLoader(), TestEndpoint.class);
  }

  @Test
  public void testDefaults() {
    assertEquals(TestEndpoint.class.getName(), config.getApiClassJavaName());
    assertEquals("resource", config.getResource());
    assertEquals(AuthLevel.NONE, config.getAuthLevel());
    assertEquals(defaultScopeExpression, config.getScopeExpression());
    assertEquals(defaultAudiences, config.getAudiences());
    assertEquals(defaultClientIds, config.getClientIds());
    assertFalse(config.getUseDatastore());
  }

  @Test
  public void testChangedDefaults() {
    Mockito.when(apiConfig.getResource()).thenReturn("resource2");
    Mockito.when(apiConfig.getAuthLevel()).thenReturn(AuthLevel.REQUIRED);
    Mockito.when(apiConfig.getScopeExpression()).thenReturn(defaultScopeExpression2);
    Mockito.when(apiConfig.getAudiences()).thenReturn(defaultAudiences2);
    Mockito.when(apiConfig.getClientIds()).thenReturn(defaultClientIds2);
    Mockito.when(apiConfig.getUseDatastore()).thenReturn(true);

    assertEquals("resource2", config.getResource());
    assertEquals(AuthLevel.REQUIRED, config.getAuthLevel());
    assertEquals(defaultScopeExpression2, config.getScopeExpression());
    assertEquals(defaultAudiences2, config.getAudiences());
    assertEquals(defaultClientIds2, config.getClientIds());
    assertTrue(config.getUseDatastore());
  }

  @Test
  public void testDefaultsOverriddenWithLocal() {
    config.setResource("bleh");
    config.setAuthLevel(AuthLevel.REQUIRED);
    AuthScopeExpression scopes = AuthScopeExpressions.interpret("scope1", "scope2");
    config.setScopeExpression(scopes);
    List<String> audiences = Lists.newArrayList("audience1", "audience2");
    config.setAudiences(audiences);
    List<String> clientIds = Lists.newArrayList("ci1", "ci2");
    config.setClientIds(clientIds);
    config.setUseDatastore(true);

    assertEquals("bleh", config.getResource());
    assertEquals(AuthLevel.REQUIRED, config.getAuthLevel());
    assertEquals(scopes, config.getScopeExpression());
    assertEquals(audiences, config.getAudiences());
    assertEquals(clientIds, config.getClientIds());
    assertTrue(config.getUseDatastore());
  }

  @Test
  public void testAddMethod() throws Exception {
    assertEquals(0, config.getMethods().size());

    EndpointMethod method = getResultNoParamsMethod();
    ApiMethodConfig methodConfig = config.getMethods().getOrCreate(method);

    Map<EndpointMethod, ApiMethodConfig> methodConfigs = config.getMethods();
    assertEquals(1, methodConfigs.size());
    assertEquals(methodConfig, methodConfigs.get(method));
  }

  @Test
  public void testAddMethodDuplicates() throws Exception {
    assertEquals(0, config.getMethods().size());

    EndpointMethod method1 = getGetDateMethod();
    ApiMethodConfig methodConfig1 = config.getMethods().getOrCreate(method1);
    ApiMethodConfig methodConfig2 = config.getMethods().getOrCreate(method1);
    assertEquals(1, config.getMethods().size());
    assertEquals(methodConfig1, methodConfig2);

    EndpointMethod method2 = getResultNoParamsMethod();
    methodConfig1 = config.getMethods().getOrCreate(method2);
    methodConfig2 = config.getMethods().getOrCreate(method2);
    assertEquals(2, config.getMethods().size());
    assertEquals(methodConfig1, methodConfig2);
  }

  private EndpointMethod getResultNoParamsMethod() throws NoSuchMethodException, SecurityException {
    return getSimpleEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams"));
  }

  private EndpointMethod getGetDateMethod() throws NoSuchMethodException, SecurityException {
    return getSimpleEndpointMethod(TestEndpoint.class.getMethod("getDate", Date.class));
  }

  private EndpointMethod getSimpleEndpointMethod(Method method) {
    return EndpointMethod.create(method.getDeclaringClass(), method);
  }
}
