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
import static org.junit.Assert.assertTrue;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.auth.EndpointsAuthenticator;
import com.google.api.server.spi.auth.EndpointsPeerAuthenticator;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.testing.PassAuthenticator;
import com.google.api.server.spi.testing.PassPeerAuthenticator;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link ApiMethodConfig}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiMethodAnnotationConfigTest {
  private ApiMethodConfig config;
  private ApiMethodAnnotationConfig annotationConfig;
  private ApiSerializationConfig serializationConfig;

  @Mock private ApiConfig apiConfig;
  @Mock private ApiClassConfig apiClassConfig;

  private static final AuthScopeExpression defaultScopeExpression = toScopeExpression("s1", "s2");
  private static final List<String> defaultAudiences = Lists.newArrayList("a1", "a2");
  private static final List<String> defaultClientIds = Lists.newArrayList("c1", "c2");
  private static final List<Class<? extends Authenticator>> defaultAuthenticators =
      ImmutableList.<Class<? extends Authenticator>>of(EndpointsAuthenticator.class);
  private static final List<Class<? extends PeerAuthenticator>> defaultPeerAuthenticators =
      ImmutableList.<Class<? extends PeerAuthenticator>>of(EndpointsPeerAuthenticator.class);

  @Before
  public void setUp() throws Exception {
    serializationConfig = new ApiSerializationConfig();

    Mockito.when(apiClassConfig.getApiConfig()).thenReturn(apiConfig);
    Mockito.when(apiClassConfig.getAuthLevel()).thenReturn(AuthLevel.NONE);
    Mockito.when(apiClassConfig.getScopeExpression()).thenReturn(defaultScopeExpression);
    Mockito.when(apiClassConfig.getAudiences()).thenReturn(defaultAudiences);
    Mockito.when(apiClassConfig.getClientIds()).thenReturn(defaultClientIds);
    Mockito.<List<Class<? extends Authenticator>>>when(apiClassConfig.getAuthenticators())
        .thenReturn(defaultAuthenticators);
    Mockito.<List<Class<? extends PeerAuthenticator>>>when(apiClassConfig.getPeerAuthenticators())
        .thenReturn(defaultPeerAuthenticators);
    Mockito.when(apiClassConfig.getApiClassJavaSimpleName()).thenReturn(
        TestEndpoint.class.getSimpleName());
    Mockito.when(apiConfig.getSerializationConfig()).thenReturn(serializationConfig);

    EndpointMethod method = EndpointMethod.create(TestEndpoint.class,
        TestEndpoint.class.getMethod("overrideMethod1"));
    config = new ApiMethodConfig(method, new TypeLoader(), apiClassConfig);
    annotationConfig = new ApiMethodAnnotationConfig(config);
  }

  @Test
  public void testDefaults() {
    assertEquals("TestEndpoint.overrideMethod1", config.getName());
    assertEquals(null, config.getDescription());
    assertEquals("overrideMethod1", config.getPath());
    assertEquals("POST", config.getHttpMethod());
    assertEquals(AuthLevel.NONE, config.getAuthLevel());
    assertEquals(defaultScopeExpression, config.getScopeExpression());
    assertEquals(defaultAudiences, config.getAudiences());
    assertEquals(defaultClientIds, config.getClientIds());
    assertEquals(defaultAuthenticators, config.getAuthenticators());
    assertEquals(defaultPeerAuthenticators, config.getPeerAuthenticators());
  }

  @Test
  public void testAddParameter() {
    assertEquals(0, config.getParameterConfigs().size());

    config.addParameter("bleh", "desc", false, null, TypeToken.of(String.class));

    assertEquals(1, config.getParameterConfigs().size());
    assertEquals("bleh", config.getParameterConfigs().get(0).getName());
    assertEquals("desc", config.getParameterConfigs().get(0).getDescription());
    assertEquals(TypeToken.of(String.class), config.getParameterConfigs().get(0).getType());
    assertTrue(config.getParameterConfigs().get(0).getSerializers().isEmpty());
    assertEquals(
        TypeToken.of(String.class), config.getParameterConfigs().get(0).getSchemaBaseType());
    assertEquals("overrideMethod1/{bleh}", config.getPath());
  }

  @Test
  public void testAddParameter_nullableOrDefault() {
    assertEquals(0, config.getParameterConfigs().size());

    config.addParameter("bleh", null, true, null, TypeToken.of(String.class));
    assertEquals(1, config.getParameterConfigs().size());
    assertEquals("bleh", config.getParameterConfigs().get(0).getName());
    assertEquals("overrideMethod1", config.getPath());

    config.addParameter("foo", null, false, "42", TypeToken.of(String.class));
    assertEquals(2, config.getParameterConfigs().size());
    assertEquals("foo", config.getParameterConfigs().get(1).getName());
    assertEquals("overrideMethod1", config.getPath());
  }

  @Test
  public void testSetNameIfNotEmpty() {
    annotationConfig.setNameIfNotEmpty("bleh");
    assertEquals("bleh", config.getName());
  }

  @Test
  public void testSetNameIfNotEmpty_empty() {
    annotationConfig.setNameIfNotEmpty("");
    testDefaults();

    annotationConfig.setNameIfNotEmpty("foo");
    annotationConfig.setNameIfNotEmpty("");
    assertEquals("foo", config.getName());
  }

  @Test
  public void testSetDescriptionIfNotEmpty() {
    annotationConfig.setDescriptionIfNotEmpty("bleh");
    assertEquals("bleh", config.getDescription());
  }

  @Test
  public void testSetDescriptionIfNotEmpty_empty() {
    annotationConfig.setDescriptionIfNotEmpty("");
    testDefaults();

    annotationConfig.setDescriptionIfNotEmpty("foo");
    annotationConfig.setDescriptionIfNotEmpty("");
    assertEquals("foo", config.getDescription());
  }

  @Test
  public void testSetPathIfNotEmpty() {
    annotationConfig.setPathIfNotEmpty("bleh");
    assertEquals("bleh", config.getPath());
  }

  @Test
  public void testSetPathIfNotEmpty_empty() {
    annotationConfig.setPathIfNotEmpty("");
    testDefaults();

    annotationConfig.setPathIfNotEmpty("foo");
    annotationConfig.setPathIfNotEmpty("");
    assertEquals("foo", config.getPath());
  }

  @Test
  public void testSetHttpMethodIfNotEmpty() {
    annotationConfig.setHttpMethodIfNotEmpty("bleh");
    assertEquals("bleh", config.getHttpMethod());
  }

  @Test
  public void testSetHttpMethodIfNotEmpty_empty() {
    annotationConfig.setHttpMethodIfNotEmpty("");
    testDefaults();

    annotationConfig.setHttpMethodIfNotEmpty("foo");
    annotationConfig.setHttpMethodIfNotEmpty("");
    assertEquals("foo", config.getHttpMethod());
  }

  @Test
  public void testSetAuthLevelIfSpecified() throws Exception {
    for (AuthLevel authLevel : AuthLevel.values()) {
      if (authLevel == AuthLevel.UNSPECIFIED) { // next test.
        continue;
      }
      annotationConfig.setAuthLevelIfSpecified(authLevel);
      assertEquals(authLevel, config.getAuthLevel());
    }
  }

  @Test
  public void testSetAuthLevelIfSpecified_unspecified() throws Exception {
    annotationConfig.setAuthLevelIfSpecified(AuthLevel.UNSPECIFIED);
    assertEquals(AuthLevel.NONE, config.getAuthLevel());

    Mockito.when(apiClassConfig.getAuthLevel()).thenReturn(AuthLevel.REQUIRED);
    assertEquals(AuthLevel.REQUIRED, config.getAuthLevel());

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
  }

  @Test
  public void testSetScopesIfSpecified_empty() throws Exception {
    String[] empty = {};
    annotationConfig.setScopesIfSpecified(empty);
    assertEquals(toScopeExpression(), config.getScopeExpression());

    String[] scopes = { "bleh", "more bleh" };
    annotationConfig.setScopesIfSpecified(scopes);
    annotationConfig.setScopesIfSpecified(empty);
    assertEquals(toScopeExpression(), config.getScopeExpression());
  }

  @Test
  public void testSetScopesIfSpecified_unspecified() throws Exception {
    testDefaults();

    annotationConfig.setScopesIfSpecified(null);
    testDefaults();

    String[] unspecified = {Api.UNSPECIFIED_STRING_FOR_LIST};
    annotationConfig.setScopesIfSpecified(unspecified);
    testDefaults();

    String[] scopes = {"bleh", "more bleh"};
    annotationConfig.setScopesIfSpecified(scopes);
    annotationConfig.setScopesIfSpecified(unspecified);
    assertEquals(toScopeExpression(scopes), config.getScopeExpression());
  }

  @Test
  public void testSetAudiencesIfSpecified() throws Exception {
    String[] audiences = { "foo", "bar" };
    annotationConfig.setAudiencesIfSpecified(audiences);

    assertEquals(Arrays.asList(audiences), config.getAudiences());
  }

  @Test
  public void testSetAudiencesIfSpecified_empty() throws Exception {
    String[] empty = {};
    annotationConfig.setAudiencesIfSpecified(empty);
    assertEquals(Collections.emptyList(), config.getAudiences());

    String[] audiences = {"bleh", "more bleh"};
    annotationConfig.setAudiencesIfSpecified(audiences);
    annotationConfig.setAudiencesIfSpecified(empty);
    assertEquals(Collections.emptyList(), config.getAudiences());
  }

  @Test
  public void testSetAudiencesIfSpecified_unspecified() throws Exception {
    testDefaults();

    annotationConfig.setAudiencesIfSpecified(null);
    testDefaults();

    String[] unspecified = {Api.UNSPECIFIED_STRING_FOR_LIST};
    annotationConfig.setAudiencesIfSpecified(unspecified);
    testDefaults();

    String[] audiences = {"bleh", "more bleh"};
    annotationConfig.setAudiencesIfSpecified(audiences);
    annotationConfig.setAudiencesIfSpecified(unspecified);
    assertEquals(Arrays.asList(audiences), config.getAudiences());
  }

  @Test
  public void testSetClientIdsIfSpecified() throws Exception {
    String[] clientIds = {"foo", "bar"};
    annotationConfig.setClientIdsIfSpecified(clientIds);

    assertEquals(Arrays.asList(clientIds), config.getClientIds());
  }

  @Test
  public void testSetClientIdsIfSpecified_empty() throws Exception {
    String[] empty = {};
    annotationConfig.setClientIdsIfSpecified(empty);
    assertEquals(Collections.emptyList(), config.getClientIds());

    String[] clientIds = {"bleh", "more bleh"};
    annotationConfig.setClientIdsIfSpecified(clientIds);
    annotationConfig.setClientIdsIfSpecified(empty);
    assertEquals(Collections.emptyList(), config.getClientIds());
  }

  @Test
  public void testSetClientIdsIfSpecified_unspecified() throws Exception {
    testDefaults();

    annotationConfig.setClientIdsIfSpecified(null);
    testDefaults();

    String[] unspecified = {Api.UNSPECIFIED_STRING_FOR_LIST};
    annotationConfig.setClientIdsIfSpecified(unspecified);
    testDefaults();

    String[] clientIds = {"bleh", "more bleh"};
    annotationConfig.setClientIdsIfSpecified(clientIds);
    annotationConfig.setClientIdsIfSpecified(unspecified);
    assertEquals(Arrays.asList(clientIds), config.getClientIds());
  }

  @Test
  public void testSetAuthenticatorIfSpecified() throws Exception  {
    annotationConfig.setAuthenticatorsIfSpecified(PassAuthenticator.testArray);
    assertEquals(Arrays.asList(PassAuthenticator.testArray), config.getAuthenticators());
  }

  //Unchecked cast needed to get a generic array type.
  @SuppressWarnings("unchecked")
  public void testSetAuthenticatorIfSpecified_unspecified() throws Exception {
    Class<?>[] unspecified = {Authenticator.class};
    Class<? extends Authenticator>[] unspecifiedConverted =
        (Class<? extends Authenticator>[]) unspecified;

    testDefaults();

    annotationConfig.setAuthenticatorsIfSpecified(unspecifiedConverted);
    testDefaults();

    annotationConfig.setAuthenticatorsIfSpecified(PassAuthenticator.testArray);
    annotationConfig.setAuthenticatorsIfSpecified(unspecifiedConverted);
    assertEquals(Arrays.asList(PassAuthenticator.testArray), config.getAuthenticators());
  }

  @Test
  public void testPeerSetAuthenticatorIfSpecified() throws Exception {
    annotationConfig.setPeerAuthenticatorsIfSpecified(PassPeerAuthenticator.testArray);
    assertEquals(Arrays.asList(PassPeerAuthenticator.testArray), config.getPeerAuthenticators());
  }

  // Unchecked cast needed to get a generic array type.
  @SuppressWarnings("unchecked")
  public void testSetPeerAuthenticatorIfSpecified_unspecified() throws Exception {
    Class<?>[] unspecified = {PeerAuthenticator.class};
    Class<? extends PeerAuthenticator>[] unspecifiedConverted =
        (Class<? extends PeerAuthenticator>[]) unspecified;

    testDefaults();

    annotationConfig.setPeerAuthenticatorsIfSpecified(unspecifiedConverted);
    testDefaults();

    annotationConfig.setPeerAuthenticatorsIfSpecified(PassPeerAuthenticator.testArray);
    annotationConfig.setPeerAuthenticatorsIfSpecified(unspecifiedConverted);
    assertEquals(Arrays.asList(PassPeerAuthenticator.testArray), config.getPeerAuthenticators());
  }

  private static AuthScopeExpression toScopeExpression(String... scopes) {
    return AuthScopeExpressions.interpret(scopes);
  }
}
