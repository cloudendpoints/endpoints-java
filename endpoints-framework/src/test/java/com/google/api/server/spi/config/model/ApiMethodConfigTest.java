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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

/**
 * Tests for {@link ApiMethodConfig}.
 * @author Eric Orth
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiMethodConfigTest {
  private ApiMethodConfig methodConfig;

  @Mock ApiConfig apiConfig;
  @Mock ApiClassConfig apiClassConfig;
  @Mock EndpointMethod method;

  private static final AuthScopeExpression defaultScopeExpression =
      AuthScopeExpressions.interpret("s1", "s2");
  private static final List<String> defaultAudiences = Lists.newArrayList("a1", "a2");
  private static final List<String> defaultClientIds = Lists.newArrayList("c1", "c2");

  private static final AuthScopeExpression defaultScopeExpression2 =
      AuthScopeExpressions.interpret("s1", "s2", "s3");
  private static final List<String> defaultAudiences2 = Lists.newArrayList("a1");
  private static final List<String> defaultClientIds2 = Lists.newArrayList("c1", "c2", "c3");

  private static final TypeToken voidReturnType = TypeToken.of(Void.class);
  private static final TypeToken stringReturnType = TypeToken.of(String.class);

  @Before
  public void setUp() throws Exception {
    Mockito.when(apiConfig.getName()).thenReturn("testapi");
    Mockito.when(apiConfig.getVersion()).thenReturn("v1");
    Mockito.when(apiClassConfig.getResource()).thenReturn("resource");
    Mockito.when(apiClassConfig.getAuthLevel()).thenReturn(AuthLevel.NONE);
    Mockito.when(apiClassConfig.getScopeExpression()).thenReturn(defaultScopeExpression);
    Mockito.when(apiClassConfig.getAudiences()).thenReturn(defaultAudiences);
    Mockito.when(apiClassConfig.getClientIds()).thenReturn(defaultClientIds);
    Mockito.when(apiClassConfig.getApiClassJavaSimpleName()).thenReturn("className");
    Mockito.when(apiClassConfig.getApiConfig()).thenReturn(apiConfig);

    Mockito.when(method.getMethod()).thenReturn(TestEndpoint.class.getMethod("getResultNoParams"));

    methodConfig = new ApiMethodConfig(method, new TypeLoader(), apiClassConfig);
  }

  @Test
  public void testAuthDefaults() {
    assertEquals(AuthLevel.NONE, methodConfig.getAuthLevel());
    assertEquals(defaultScopeExpression, methodConfig.getScopeExpression());
    assertEquals(defaultAudiences, methodConfig.getAudiences());
    assertEquals(defaultClientIds, methodConfig.getClientIds());
  }

  @Test
  public void testAuthDefaultsChanged() {
    Mockito.when(apiClassConfig.getAuthLevel()).thenReturn(AuthLevel.REQUIRED);
    Mockito.when(apiClassConfig.getScopeExpression()).thenReturn(defaultScopeExpression2);
    Mockito.when(apiClassConfig.getAudiences()).thenReturn(defaultAudiences2);
    Mockito.when(apiClassConfig.getClientIds()).thenReturn(defaultClientIds2);

    assertEquals(AuthLevel.REQUIRED, methodConfig.getAuthLevel());
    assertEquals(defaultScopeExpression2, methodConfig.getScopeExpression());
    assertEquals(defaultAudiences2, methodConfig.getAudiences());
    assertEquals(defaultClientIds2, methodConfig.getClientIds());
  }

  @Test
  public void testMethodNameWithResource() {
    assertEquals("resource.getResultNoParams", methodConfig.getName());
  }

  @Test
  public void testMethodNameNoResource() {
    Mockito.when(apiClassConfig.getResource()).thenReturn(null);
    assertEquals("className.getResultNoParams", methodConfig.getName());
  }

  @Test
  public void testMethodResponseStatusEffectiveStatus_returnValue_statusOK() throws Exception {
    Mockito.when(method.getReturnType()).thenReturn(stringReturnType);
    methodConfig = new ApiMethodConfig(method, new TypeLoader(), apiClassConfig);

    assertEquals(200, methodConfig.getEffectiveResponseStatus());
  }

  @Test
  public void testMethodReponseStatusEffectiveStatus_returnVoid_statusNO_CONTENT() throws Exception {
    Mockito.when(method.getReturnType()).thenReturn(voidReturnType);
    methodConfig = new ApiMethodConfig(method, new TypeLoader(), apiClassConfig);

    assertEquals(204, methodConfig.getEffectiveResponseStatus());
  }

  @Test
  public void addInjectedParameter_notInPath() {
    methodConfig.addParameter("alt", null, false, null, TypeToken.of(String.class));
    assertThat(methodConfig.getPath()).doesNotContain("{alt}");
  }

  @Test
  public void addPathParameter_appendsToCanonicalPath() {
    methodConfig.addParameter("test", null, false, null, TypeToken.of(String.class));
    assertThat(methodConfig.getCanonicalPath()).contains("{test}");
  }

  @Test
  public void addPathParameter_doesNotAppendIfInPathAlready() {
    methodConfig.setPath("test/{test}");
    methodConfig.addParameter("test", null, false, null, TypeToken.of(String.class));
    assertThat(methodConfig.getPath()).isEqualTo("test/{test}");
  }
}
