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

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.testing.PassAuthenticator;
import com.google.api.server.spi.testing.PassPeerAuthenticator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

/**
 * Tests for {@link ApiClassAnnotationConfig}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiClassAnnotationConfigTest {
  private ApiClassAnnotationConfig annotationConfig;

  @Mock private ApiClassConfig config;

  @Before
  public void setUp() throws Exception {
    annotationConfig = new ApiClassAnnotationConfig(config);
  }

  @Test
  public void testSetResourceIfNotEmpty() {
    annotationConfig.setResourceIfNotEmpty("foo");
    Mockito.verify(config).setResource("foo");
  }

  @Test
  public void testSetResourceIfNotEmpty_empty() {
    annotationConfig.setResourceIfNotEmpty("");
    Mockito.verifyZeroInteractions(config);

    annotationConfig.setResourceIfNotEmpty("bleh");
    annotationConfig.setResourceIfNotEmpty("");
    Mockito.verify(config).setResource("bleh");
    Mockito.verifyNoMoreInteractions(config);
  }

  @Test
  public void testSetAuthLevelIfSpecified() throws Exception {
    for (AuthLevel authLevel : AuthLevel.values()) {
      if (authLevel == AuthLevel.UNSPECIFIED) { // next test.
        continue;
      }
      annotationConfig.setAuthLevelIfSpecified(authLevel);
      Mockito.verify(config).setAuthLevel(authLevel);
    }
  }

  @Test
  public void testSetAuthLevelIfSpecified_unspecified() throws Exception {
    annotationConfig.setAuthLevelIfSpecified(AuthLevel.UNSPECIFIED);
    Mockito.verifyZeroInteractions(config);
  }

  @Test
  public void testSetScopesIfSpecified() throws Exception {
    String[] scopes = { "foo", "bar" };
    annotationConfig.setScopesIfSpecified(scopes);
    Mockito.verify(config).setScopeExpression(toScopeExpression(scopes));
  }

  @Test
  public void testSetScopesIfSpecified_empty() throws Exception {
    String[] empty = {};
    annotationConfig.setScopesIfSpecified(empty);
    Mockito.verify(config).setScopeExpression(toScopeExpression());
  }

  @Test
  public void testSetScopesIfSpecified_unspecified() throws Exception {
    annotationConfig.setScopesIfSpecified(null);
    Mockito.verifyZeroInteractions(config);

    String[] unspecified = {Api.UNSPECIFIED_STRING_FOR_LIST};
    annotationConfig.setScopesIfSpecified(unspecified);
    Mockito.verifyZeroInteractions(config);

    String[] scopes = { "bleh", "more bleh" };
    annotationConfig.setScopesIfSpecified(scopes);
    annotationConfig.setScopesIfSpecified(unspecified);
    Mockito.verify(config).setScopeExpression(toScopeExpression(scopes));
    Mockito.verifyNoMoreInteractions(config);
  }

  @Test
  public void testSetAudiencesIfSpecified() throws Exception {
    String[] audiences = {"foo", "bar"};
    annotationConfig.setAudiencesIfSpecified(audiences);
    Mockito.verify(config).setAudiences(Arrays.asList(audiences));
  }

  @Test
  public void testSetAudiencesIfSpecified_empty() throws Exception {
    String[] empty = {};
    annotationConfig.setAudiencesIfSpecified(empty);
    Mockito.verify(config).setAudiences(Arrays.asList(empty));
  }

  @Test
  public void testSetAudiencesIfSpecified_unspecified() throws Exception {
    annotationConfig.setAudiencesIfSpecified(null);
    Mockito.verifyZeroInteractions(config);

    String[] unspecified = {Api.UNSPECIFIED_STRING_FOR_LIST};
    annotationConfig.setAudiencesIfSpecified(unspecified);
    Mockito.verifyZeroInteractions(config);

    String[] audiences = {"bleh", "more bleh"};
    annotationConfig.setAudiencesIfSpecified(audiences);
    annotationConfig.setAudiencesIfSpecified(unspecified);
    Mockito.verify(config).setAudiences(Arrays.asList(audiences));
    Mockito.verifyNoMoreInteractions(config);
  }

  @Test
  public void testSetClientIdsIfSpecified() throws Exception {
    String[] clientIds = {"foo", "bar"};
    annotationConfig.setClientIdsIfSpecified(clientIds);
    Mockito.verify(config).setClientIds(Arrays.asList(clientIds));
  }

  @Test
  public void testSetClientIdsIfSpecified_empty() throws Exception {
    String[] empty = {};
    annotationConfig.setClientIdsIfSpecified(empty);
    Mockito.verify(config).setClientIds(Arrays.asList(empty));
  }

  @Test
  public void testSetClientIdsIfSpecified_unspecified() throws Exception {
    annotationConfig.setClientIdsIfSpecified(null);
    Mockito.verifyZeroInteractions(config);

    String[] unspecified = {Api.UNSPECIFIED_STRING_FOR_LIST};
    annotationConfig.setClientIdsIfSpecified(unspecified);
    Mockito.verifyZeroInteractions(config);

    String[] clientIds = {"bleh", "more bleh"};
    annotationConfig.setClientIdsIfSpecified(clientIds);
    annotationConfig.setClientIdsIfSpecified(unspecified);
    Mockito.verify(config).setClientIds(Arrays.asList(clientIds));
    Mockito.verifyNoMoreInteractions(config);
  }

  @Test
  public void testSetAuthenticatorIfSpecified() throws Exception {
    annotationConfig.setAuthenticatorsIfSpecified(PassAuthenticator.testArray);
    Mockito.verify(config).setAuthenticators(Arrays.asList(PassAuthenticator.testArray));
  }

  //Unchecked cast needed to get a generic array type.
  @SuppressWarnings("unchecked")
  public void testSetAuthenticatorIfSpecified_unspecified() throws Exception {
    Class<?>[] authenticators = {Authenticator.class};
    annotationConfig.setAuthenticatorsIfSpecified(
        (Class<? extends Authenticator>[]) authenticators);
    Mockito.verifyZeroInteractions(config);
  }

  @Test
  public void testSetPeerAuthenticatorIfSpecified() throws Exception {
    annotationConfig.setPeerAuthenticatorsIfSpecified(PassPeerAuthenticator.testArray);
    Mockito.verify(config).setPeerAuthenticators(Arrays.asList(PassPeerAuthenticator.testArray));
  }

  // Unchecked cast needed to get a generic array type.
  @SuppressWarnings("unchecked")
  public void testSetPeerAuthenticatorIfSpecified_unspecified() throws Exception {
    Class<?>[] peerAuthenticators = {PeerAuthenticator.class};
    annotationConfig.setPeerAuthenticatorsIfSpecified(
        (Class<? extends PeerAuthenticator>[]) peerAuthenticators);
    Mockito.verifyZeroInteractions(config);
  }

  @Test
  public void testSetUseDatastoreIfSpecified() throws Exception {
    annotationConfig.setUseDatastoreIfSpecified(AnnotationBoolean.TRUE);
    Mockito.verify(config).setUseDatastore(true);

    annotationConfig.setUseDatastoreIfSpecified(AnnotationBoolean.FALSE);
    Mockito.verify(config).setUseDatastore(false);

    Mockito.verifyNoMoreInteractions(config);
  }

  private static AuthScopeExpression toScopeExpression(String... scopes) {
    return AuthScopeExpressions.interpret(scopes);
  }
}
