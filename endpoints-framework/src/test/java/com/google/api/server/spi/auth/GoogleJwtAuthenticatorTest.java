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
package com.google.api.server.spi.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.request.Attribute;
import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Tests for GoogleJwtAuthenticator.
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleJwtAuthenticatorTest {
  private static final String TOKEN = "abcdefjh.abcdefjh.abcdefjh";
  private static final String EMAIL = "dummy@gmail.com";
  private static final String CLIENT_ID = "clientId1";
  private static final String AUDIENCE = "audience1";
  private static final String USER_ID = "1234567";

  private Payload payload;
  private GoogleJwtAuthenticator authenticator;
  private MockHttpServletRequest request;
  private Attribute attr;

  @Mock private GoogleIdTokenVerifier verifier;
  @Mock private GoogleIdToken token;
  @Mock protected ApiMethodConfig config;

  @Before
  public void setUp() throws Exception {
    payload = new Payload();
    payload.setAuthorizedParty(CLIENT_ID);
    payload.setAudience(AUDIENCE);
    payload.setEmail(EMAIL);
    payload.setSubject(USER_ID);
    authenticator = new GoogleJwtAuthenticator(verifier);
    request = new MockHttpServletRequest();
    attr = Attribute.from(request);
    attr.set(Attribute.API_METHOD_CONFIG, config);
    attr.set(Attribute.ENABLE_CLIENT_ID_WHITELIST, true);
    request.addHeader(GoogleAuth.AUTHORIZATION_HEADER, "Bearer " + TOKEN);
    when(token.getPayload()).thenReturn(payload);
  }

  @Test
  public void testVerifyToken() throws Exception {
        when(verifier.verify(TOKEN))
        .thenReturn(token)
        .thenThrow(new GeneralSecurityException())
        .thenThrow(new IOException())
        .thenThrow(new IllegalArgumentException());
    assertEquals(token, authenticator.verifyToken(TOKEN));
    for (int i = 0; i < 3; i++) {
      assertNull(authenticator.verifyToken(TOKEN));
    }
  }

  @Test
  public void testAuthenticate_skipTokenAuth() {
    attr.set(Attribute.SKIP_TOKEN_AUTH, true);
    assertNull(authenticator.authenticate(request));
    assertNull(attr.get(Attribute.ID_TOKEN));
  }

  @Test
  public void testAuthenticate_notJwt() {
    request.addHeader(GoogleAuth.AUTHORIZATION_HEADER, "Bearer abc.abc");
    assertNull(authenticator.authenticate(request));
    assertNull(attr.get(Attribute.ID_TOKEN));
  }

  @Test
  public void testAuthenticate_invalidToken() throws Exception {
    when(verifier.verify(TOKEN)).thenReturn(null);
    assertNull(authenticator.authenticate(request));
    assertNull(attr.get(Attribute.ID_TOKEN));
  }

  @Test
  public void testAuthenticate_clientIdNotAllowed() throws Exception {
    when(verifier.verify(TOKEN)).thenReturn(token);
    when(config.getClientIds()).thenReturn(ImmutableList.of("clientId2"));
    assertNull(authenticator.authenticate(request));
    assertNotNull(attr.get(Attribute.ID_TOKEN));
  }

  @Test
  public void testAuthenticate_audienceNotAllowed() throws Exception {
    when(verifier.verify(TOKEN)).thenReturn(token);
    when(config.getClientIds()).thenReturn(ImmutableList.of(CLIENT_ID));
    when(config.getAudiences()).thenReturn(ImmutableList.of("audience2"));
    assertNull(authenticator.authenticate(request));
    assertNotNull(attr.get(Attribute.ID_TOKEN));
  }

  @Test
  public void testAuthenticate_skipClientIdCheck() throws Exception {
    request.removeAttribute(Attribute.ENABLE_CLIENT_ID_WHITELIST);
    when(verifier.verify(TOKEN)).thenReturn(token);
    when(config.getAudiences()).thenReturn(ImmutableList.of(AUDIENCE));
    User user = authenticator.authenticate(request);
    assertEquals(EMAIL, user.getEmail());
    assertEquals(USER_ID, user.getId());
    assertNotNull(attr.get(Attribute.ID_TOKEN));
  }

  @Test
  public void testAuthenticate() throws Exception {
    when(verifier.verify(TOKEN)).thenReturn(token);
    when(config.getClientIds()).thenReturn(ImmutableList.of(CLIENT_ID));
    when(config.getAudiences()).thenReturn(ImmutableList.of(AUDIENCE));
    User user = authenticator.authenticate(request);
    assertEquals(EMAIL, user.getEmail());
    assertEquals(USER_ID, user.getId());
    GoogleIdToken idToken = attr.get(Attribute.ID_TOKEN);
    assertNotNull(idToken);
    assertEquals(EMAIL, idToken.getPayload().getEmail());
    assertEquals(USER_ID, idToken.getPayload().getSubject());
  }

  @Test
  public void testAuthenticate_appEngineUser() throws GeneralSecurityException, IOException {
    attr.set(Attribute.REQUIRE_APPENGINE_USER, true);
    when(verifier.verify(TOKEN)).thenReturn(token);
    when(config.getClientIds()).thenReturn(ImmutableList.of(CLIENT_ID));
    when(config.getAudiences()).thenReturn(ImmutableList.of(AUDIENCE));
    User user = authenticator.authenticate(request);
    assertEquals(EMAIL, user.getEmail());
    assertEquals(USER_ID, user.getId());
    com.google.appengine.api.users.User appEngineuser =
        attr.get(Attribute.AUTHENTICATED_APPENGINE_USER);
    assertEquals(EMAIL, appEngineuser.getEmail());
    assertNull(appEngineuser.getUserId());
  }
}
