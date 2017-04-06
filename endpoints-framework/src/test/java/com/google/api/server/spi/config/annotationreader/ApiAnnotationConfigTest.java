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
package com.google.api.server.spi.config.annotationreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.testing.PassAuthenticator;
import com.google.api.server.spi.testing.PassPeerAuthenticator;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

/**
 * Tests for {@link ApiConfig}.
 */
@RunWith(JUnit4.class)
public class ApiAnnotationConfigTest {
  private ServiceContext serviceContext;
  private ApiConfig config;
  private ApiAnnotationConfig annotationConfig;

  @Before
  public void setUp() throws Exception {
    serviceContext = Mockito.mock(ServiceContext.class);
    Mockito.when(serviceContext.getDefaultApiName()).thenReturn("api");
    Mockito.when(serviceContext.getAppHostname()).thenReturn("appHostName.com");
    Mockito.when(serviceContext.getTransferProtocol()).thenReturn("https");

    config = new ApiConfig.Factory().create(serviceContext, new TypeLoader(), TestEndpoint.class);
    annotationConfig = new ApiAnnotationConfig(config);
  }

  @Test
  public void testDefaults() {
    assertEquals("https://appHostName.com/_ah/api", config.getRoot());
    assertEquals("api", config.getName());
    assertEquals("v1", config.getVersion());
    assertEquals(null, config.getDescription());
    assertEquals("https://appHostName.com/_ah/spi", config.getBackendRoot());
    assertEquals(false, config.getIsAbstract());
    assertEquals(true, config.getIsDefaultVersion());
    assertEquals(true, config.getIsDiscoverable());
    assertEquals(false, config.getUseDatastore());
  }

  @Test
  public void testSetRootIfNotEmpty() {
    annotationConfig.setRootIfNotEmpty("foo");
    assertEquals("foo", config.getRoot());
  }

  @Test
  public void testSetRootIfNotEmpty_empty() {
    annotationConfig.setRootIfNotEmpty("");
    testDefaults();

    annotationConfig.setRootIfNotEmpty("bleh");
    annotationConfig.setRootIfNotEmpty("");
    assertEquals("bleh", config.getRoot());
  }

  @Test
  public void testSetNameIfNotEmpty() {
    annotationConfig.setNameIfNotEmpty("foo");
    assertEquals("foo", config.getName());
  }

  @Test
  public void testSetNameIfNotEmpty_empty() {
    annotationConfig.setNameIfNotEmpty("");
    testDefaults();

    annotationConfig.setNameIfNotEmpty("bleh");
    annotationConfig.setNameIfNotEmpty("");
    assertEquals("bleh", config.getName());
  }

  @Test
  public void testSetVersionIfNotEmpty() {
    annotationConfig.setVersionIfNotEmpty("foo");
    assertEquals("foo", config.getVersion());
  }

  @Test
  public void testSetVersionIfNotEmpty_empty() {
    annotationConfig.setVersionIfNotEmpty("");
    testDefaults();

    annotationConfig.setVersionIfNotEmpty("bleh");
    annotationConfig.setVersionIfNotEmpty("");
    assertEquals("bleh", config.getVersion());
  }

  @Test
  public void testSetDescriptionIfNotEmpty() {
    annotationConfig.setDescriptionIfNotEmpty("foo");
    assertEquals("foo", config.getDescription());
  }

  @Test
  public void testSetDescriptionIfNotEmpty_empty() {
    annotationConfig.setDescriptionIfNotEmpty("");
    testDefaults();

    annotationConfig.setDescriptionIfNotEmpty("bleh");
    annotationConfig.setDescriptionIfNotEmpty("");
    assertEquals("bleh", config.getDescription());
  }

  @Test
  public void testSetBackendRootIfNotEmpty() {
    annotationConfig.setBackendRootIfNotEmpty("foo");
    assertEquals("foo", config.getBackendRoot());
  }

  @Test
  public void testSetBackendRootIfNotEmpty_empty() {
    annotationConfig.setBackendRootIfNotEmpty("");
    testDefaults();

    annotationConfig.setBackendRootIfNotEmpty("bleh");
    annotationConfig.setBackendRootIfNotEmpty("");
    assertEquals("bleh", config.getBackendRoot());
  }

  @Test
  public void testSetIsAbstractIfSpecified() {
    annotationConfig.setIsAbstractIfSpecified(AnnotationBoolean.TRUE);
    assertEquals(true, config.getIsAbstract());
  }

  @Test
  public void testSetIsAbstractIfSpecified_unspecified() {
    annotationConfig.setIsAbstractIfSpecified(AnnotationBoolean.UNSPECIFIED);
    testDefaults();

    annotationConfig.setIsAbstractIfSpecified(AnnotationBoolean.TRUE);
    annotationConfig.setIsAbstractIfSpecified(AnnotationBoolean.UNSPECIFIED);
    assertEquals(true, config.getIsAbstract());
  }

  @Test
  public void testSetIsDefaultVersionIfSpecified() {
    annotationConfig.setIsDefaultVersionIfSpecified(AnnotationBoolean.TRUE);
    assertEquals(true, config.getIsDefaultVersion());
  }

  @Test
  public void testSetIsDefaultVersionIfSpecified_unspecified() {
    annotationConfig.setIsDefaultVersionIfSpecified(AnnotationBoolean.UNSPECIFIED);
    testDefaults();

    annotationConfig.setIsDefaultVersionIfSpecified(AnnotationBoolean.TRUE);
    annotationConfig.setIsDefaultVersionIfSpecified(AnnotationBoolean.UNSPECIFIED);
    assertEquals(true, config.getIsDefaultVersion());
  }

  @Test
  public void testSetIsDiscoverableIfSpecified() {
    annotationConfig.setIsDiscoverableIfSpecified(AnnotationBoolean.FALSE);
    assertEquals(false, config.getIsDiscoverable());
  }

  @Test
  public void testSetIsDiscoverableIfSpecified_unspecified() {
    annotationConfig.setIsDiscoverableIfSpecified(AnnotationBoolean.UNSPECIFIED);
    testDefaults();

    annotationConfig.setIsDiscoverableIfSpecified(AnnotationBoolean.FALSE);
    annotationConfig.setIsDiscoverableIfSpecified(AnnotationBoolean.UNSPECIFIED);
    assertEquals(false, config.getIsDiscoverable());
  }

  @Test
  public void testSetAuthLevelIfSpecified() throws Exception {
    for (AuthLevel authLevel : AuthLevel.values()) {
      if (authLevel == AuthLevel.UNSPECIFIED) { // next test.
        continue;
      }
      annotationConfig.setAuthLevelIfSpecified(authLevel);
      assertEquals(authLevel, config.getAuthLevel());

      ApiMethodConfig methodConfig =
          config.getApiClassConfig().getMethods().getOrCreate(getResultNoParamsMethod());
      assertEquals(authLevel, methodConfig.getAuthLevel());
    }
  }

  @Test
  public void testSetAuthLevelIfSpecified_unspecified() throws Exception {
    EndpointMethod method = getResultNoParamsMethod();
    annotationConfig.setAuthLevelIfSpecified(AuthLevel.UNSPECIFIED);
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().getOrCreate(method);
    assertEquals(AuthLevel.NONE, methodConfig.getAuthLevel());

    annotationConfig.setAuthLevelIfSpecified(AuthLevel.REQUIRED);
    assertEquals(AuthLevel.REQUIRED, config.getAuthLevel());
    annotationConfig.setAuthLevelIfSpecified(AuthLevel.UNSPECIFIED);
    assertEquals(AuthLevel.REQUIRED, config.getAuthLevel());
  }

  @Test
  public void testSetScopesIfSpecified() throws Exception {
    String[] scopes = { "foo", "bar" };
    annotationConfig.setScopesIfSpecified(scopes);
    assertEquals(toScopeExpression(scopes), config.getScopeExpression());

    ApiMethodConfig methodConfig =
        config.getApiClassConfig().getMethods().getOrCreate(getResultNoParamsMethod());
    assertEquals(toScopeExpression(scopes), methodConfig.getScopeExpression());
  }

  @Test
  public void testSetScopesIfSpecified_empty() throws Exception {
    String[] empty = {};
    annotationConfig.setScopesIfSpecified(empty);
    EndpointMethod method = getResultNoParamsMethod();
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().getOrCreate(method);
    assertEquals(toScopeExpression(), methodConfig.getScopeExpression());

    String[] scopes = {"bleh", "more bleh"};
    annotationConfig.setScopesIfSpecified(scopes);
    annotationConfig.setScopesIfSpecified(empty);
    assertEquals(toScopeExpression(), config.getScopeExpression());
  }

  @Test
  public void testSetScopesIfSpecified_unspecified() throws Exception {
    String[] unspecified = {Api.UNSPECIFIED_STRING_FOR_LIST};

    EndpointMethod method = getResultNoParamsMethod();
    annotationConfig.setScopesIfSpecified(unspecified);
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().getOrCreate(method);
    assertEquals(toScopeExpression(Constant.API_EMAIL_SCOPE), methodConfig.getScopeExpression());

    String[] scopes = {"bleh", "more bleh"};
    annotationConfig.setScopesIfSpecified(scopes);
    annotationConfig.setScopesIfSpecified(null);
    assertEquals(toScopeExpression(scopes), config.getScopeExpression());

    annotationConfig.setScopesIfSpecified(scopes);
    annotationConfig.setScopesIfSpecified(unspecified);
    assertEquals(toScopeExpression(scopes), config.getScopeExpression());
  }

  @Test
  public void testSetAudiencesIfSpecified() throws Exception {
    String[] audiences = { "foo", "bar" };
    annotationConfig.setAudiencesIfSpecified(audiences);
    assertEquals(Arrays.asList(audiences), config.getAudiences());

    ApiMethodConfig methodConfig =
        config.getApiClassConfig().getMethods().getOrCreate(getResultNoParamsMethod());
    assertEquals(Arrays.asList(audiences), methodConfig.getAudiences());
  }

  @Test
  public void testSetAudiencesIfSpecified_empty() throws Exception {
    String[] empty = {};
    annotationConfig.setAudiencesIfSpecified(empty);
    EndpointMethod method = getResultNoParamsMethod();
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().getOrCreate(method);
    assertEquals(Collections.emptyList(), methodConfig.getAudiences());

    String[] audiences = { "bleh", "more bleh" };
    annotationConfig.setAudiencesIfSpecified(audiences);
    annotationConfig.setAudiencesIfSpecified(empty);
    assertEquals(Collections.emptyList(), config.getAudiences());
  }

  @Test
  public void testSetAudiencesIfSpecified_unspecified() throws Exception {
    String[] unspecified = {Api.UNSPECIFIED_STRING_FOR_LIST};

    EndpointMethod method = getResultNoParamsMethod();
    annotationConfig.setScopesIfSpecified(unspecified);
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().getOrCreate(method);
    assertEquals(Collections.emptyList(), methodConfig.getAudiences());

    String[] audiences = {"bleh", "more bleh"};
    annotationConfig.setAudiencesIfSpecified(audiences);
    annotationConfig.setAudiencesIfSpecified(null);
    assertEquals(Arrays.asList(audiences), config.getAudiences());

    annotationConfig.setAudiencesIfSpecified(audiences);
    annotationConfig.setAudiencesIfSpecified(unspecified);
    assertEquals(Arrays.asList(audiences), config.getAudiences());
  }

  @Test
  public void testSetClientIdsIfSpecified() throws Exception {
    String[] clientIds = { "foo", "bar" };
    annotationConfig.setClientIdsIfSpecified(clientIds);
    assertEquals(Arrays.asList(clientIds), config.getClientIds());

    ApiMethodConfig methodConfig =
        config.getApiClassConfig().getMethods().getOrCreate(getResultNoParamsMethod());
    assertEquals(Arrays.asList(clientIds), methodConfig.getClientIds());
  }

  @Test
  public void testSetClientIdsIfSpecified_empty() throws Exception {
    String[] empty = {};
    annotationConfig.setClientIdsIfSpecified(empty);
    EndpointMethod method = getResultNoParamsMethod();
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().getOrCreate(method);
    assertEquals(Collections.emptyList(), methodConfig.getClientIds());

    String[] clientIds = { "bleh", "more bleh" };
    annotationConfig.setClientIdsIfSpecified(clientIds);
    annotationConfig.setClientIdsIfSpecified(empty);
    assertEquals(Collections.emptyList(), config.getClientIds());
  }

  @Test
  public void testSetClientIdsIfSpecified_unspecified() throws Exception {
    String[] unspecified = {Api.UNSPECIFIED_STRING_FOR_LIST};

    EndpointMethod method = getResultNoParamsMethod();
    annotationConfig.setScopesIfSpecified(unspecified);
    ApiMethodConfig methodConfig = config.getApiClassConfig().getMethods().getOrCreate(method);
    assertEquals(ImmutableList.of(Constant.API_EXPLORER_CLIENT_ID), methodConfig.getClientIds());

    String[] clientIds = {"bleh", "more bleh"};
    annotationConfig.setClientIdsIfSpecified(clientIds);
    annotationConfig.setClientIdsIfSpecified(null);
    assertEquals(Arrays.asList(clientIds), config.getClientIds());

    annotationConfig.setClientIdsIfSpecified(clientIds);
    annotationConfig.setClientIdsIfSpecified(unspecified);
    assertEquals(Arrays.asList(clientIds), config.getClientIds());
  }

  @Test
  public void testSetAuthenticatorIfSpecified() throws Exception {
    annotationConfig.setAuthenticatorsIfSpecified(PassAuthenticator.testArray);
    assertEquals(Arrays.asList(PassAuthenticator.testArray), config.getAuthenticators());
  }

  //Unchecked cast needed to get a generic array type.
  @SuppressWarnings("unchecked")
  public void testSetAuthenticatorIfSpecified_unspecified() throws Exception {
    Class<?>[] authenticators = {Authenticator.class};
    annotationConfig.setAuthenticatorsIfSpecified(
        (Class<? extends Authenticator>[]) authenticators);
    assertNull(config.getAuthenticators());
  }

  @Test
  public void testSetPeerAuthenticatorIfSpecified() throws Exception {
    annotationConfig.setPeerAuthenticatorsIfSpecified(PassPeerAuthenticator.testArray);
    assertEquals(Arrays.asList(PassPeerAuthenticator.testArray), config.getPeerAuthenticators());
  }

  // Unchecked cast needed to get a generic array type.
  @SuppressWarnings("unchecked")
  public void testSetPeerAuthenticatorIfSpecified_unspecified() throws Exception {
    Class<?>[] peerAuthenticators = {PeerAuthenticator.class};
    annotationConfig.setPeerAuthenticatorsIfSpecified(
        (Class<? extends PeerAuthenticator>[]) peerAuthenticators);
    assertNull(config.getPeerAuthenticators());
  }

  private EndpointMethod getResultNoParamsMethod() throws NoSuchMethodException, SecurityException {
    return getSimpleEndpointMethod(TestEndpoint.class.getMethod("getResultNoParams"));
  }

  private EndpointMethod getSimpleEndpointMethod(Method method) {
    return EndpointMethod.create(method.getDeclaringClass(), method);
  }

  private static AuthScopeExpression toScopeExpression(String... scopes) {
    return AuthScopeExpressions.interpret(scopes);
  }
}
