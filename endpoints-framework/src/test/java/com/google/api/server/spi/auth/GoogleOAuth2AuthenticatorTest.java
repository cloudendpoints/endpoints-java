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

import com.google.api.server.spi.auth.GoogleAuth.TokenInfo;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Test for GoogleOAuth2Authenticator.
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleOAuth2AuthenticatorTest {
  private static final String TOKEN = "ya29.abcdefg";
  private static final String EMAIL = "dummy@gmail.com";
  private static final String CLIENT_ID = "clientId1";
  private static final String SCOPES = "scope1 scope2";
  private static final String USER_ID = "1234567";

  private GoogleOAuth2Authenticator authenticator;
  private MockHttpServletRequest request;
  private Attribute attr;

  @Mock protected ApiMethodConfig config;

  @Before
  public void setUp() throws Exception {
    initializeRequest("Bearer " + TOKEN);
  }

  private void initializeRequest(String bearerString) {
    request = new MockHttpServletRequest();
    attr = Attribute.from(request);
    attr.set(Attribute.API_METHOD_CONFIG, config);
    attr.set(Attribute.ENABLE_CLIENT_ID_WHITELIST, true);
    request.addHeader(GoogleAuth.AUTHORIZATION_HEADER, bearerString);
    authenticator = createAuthenticator(EMAIL, CLIENT_ID, SCOPES, USER_ID);
  }

  @Test
  public void testAuthenticate_skipTokenAuth() throws ServiceUnavailableException {
    attr.set(Attribute.SKIP_TOKEN_AUTH, true);
    assertNull(authenticator.authenticate(request));
    assertNull(attr.get(Attribute.TOKEN_INFO));
  }

  @Test
  public void testAuthenticate_notOAuth2() throws ServiceUnavailableException {
    initializeRequest("Bearer badToken");
    assertNull(authenticator.authenticate(request));
    assertNull(attr.get(Attribute.TOKEN_INFO));
  }

  @Test
  public void testAuthenticate_nullTokenInfo() throws ServiceUnavailableException {
    authenticator = createAuthenticator(null, null, null, null);
    assertNull(authenticator.authenticate(request));
    assertNull(attr.get(Attribute.TOKEN_INFO));
  }

  @Test
  public void testAuthenticate_scopeNotAllowed() throws ServiceUnavailableException {
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret("scope3"));
    assertNull(authenticator.authenticate(request));
    assertNotNull(attr.get(Attribute.TOKEN_INFO));
  }

  @Test
  public void testAuthenticate_clientIdNotAllowed() throws ServiceUnavailableException {
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret("scope1"));
    when(config.getClientIds()).thenReturn(ImmutableList.of("clientId2"));
    assertNull(authenticator.authenticate(request));
    assertNotNull(attr.get(Attribute.TOKEN_INFO));
  }

  @Test
  public void testAuthenticate_skipClientIdCheck() throws ServiceUnavailableException {
    request.removeAttribute(Attribute.ENABLE_CLIENT_ID_WHITELIST);
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret("scope1"));
    User user = authenticator.authenticate(request);
    assertEquals(EMAIL, user.getEmail());
    assertEquals(USER_ID, user.getId());
    assertNotNull(attr.get(Attribute.TOKEN_INFO));
  }

  @Test
  public void testAuthenticate() throws ServiceUnavailableException {
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret("scope1"));
    when(config.getClientIds()).thenReturn(ImmutableList.of(CLIENT_ID));
    User user = authenticator.authenticate(request);
    assertEquals(EMAIL, user.getEmail());
    assertEquals(USER_ID, user.getId());
    final TokenInfo tokenInfo = attr.get(Attribute.TOKEN_INFO);
    assertNotNull(tokenInfo);
    assertEquals(EMAIL, tokenInfo.email);
    assertEquals(USER_ID, tokenInfo.userId);
  }

  @Test
  public void testAuthenticate_appEngineUser() throws ServiceUnavailableException {
    attr.set(Attribute.REQUIRE_APPENGINE_USER, true);
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret("scope1"));
    when(config.getClientIds()).thenReturn(ImmutableList.of(CLIENT_ID));
    User user = authenticator.authenticate(request);
    assertEquals(EMAIL, user.getEmail());
    assertEquals(USER_ID, user.getId());
    com.google.appengine.api.users.User appEngineuser =
        (com.google.appengine.api.users.User) attr.get(Attribute.AUTHENTICATED_APPENGINE_USER);
    assertEquals(EMAIL, appEngineuser.getEmail());
    assertNull(appEngineuser.getUserId());
  }

  private GoogleOAuth2Authenticator createAuthenticator(final String email, final String clientId,
      final String scopes, final String userId) {
    return new GoogleOAuth2Authenticator() {
      @Override
      TokenInfo getTokenInfoRemote(String token) {
        if (email == null) {
          return null;
        }
        TokenInfo info = new TokenInfo();
        info.email = email;
        info.clientId = clientId;
        info.scopes = scopes;
        info.userId = userId;
        return info;
      }
    };
  }
}
