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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.EnvUtil;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.oauth.OAuthService;
import com.google.appengine.api.users.UserService;
import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests for GoogleAppEngineAuthenticator.
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleAppEngineAuthenticatorTest {
  private static final String TOKEN = "ya29.abcdefg";
  private static final String EMAIL = "dummy@gmail.com";
  private static final String CLIENT_ID = "clientId1";
  private static final String[] SCOPES = {"scope1", "scope2"};
  private static final String USER_ID = "1234567";
  private static final User USER = new User(EMAIL);
  private static final com.google.appengine.api.users.User APP_ENGINE_USER =
      new com.google.appengine.api.users.User(EMAIL, "", USER_ID);

  @Mock private OAuthService oauthService;
  @Mock private UserService userService;
  @Mock protected ApiMethodConfig config;

  private GoogleAppEngineAuthenticator authenticator;
  private MockHttpServletRequest request;
  private Attribute attr;

  @Before
  public void setUp() throws Exception {
    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "Production");
    authenticator = new GoogleAppEngineAuthenticator(oauthService, userService) {
      @Override
      String getOAuth2ClientIdDev(String token) {
        return CLIENT_ID;
      }

      @Override
      boolean shouldTryCookieAuth(ApiMethodConfig config) {
        return false;
      }
    };
    initializeRequest("Bearer " + TOKEN);
  }

  private void initializeRequest(String bearerString) {
    request = new MockHttpServletRequest();
    attr = Attribute.from(request);
    attr.set(Attribute.API_METHOD_CONFIG, config);
    attr.set(Attribute.ENABLE_CLIENT_ID_WHITELIST, true);
    request.addHeader(GoogleAuth.AUTHORIZATION_HEADER, bearerString);
    attr.set(Attribute.REQUIRE_APPENGINE_USER, true);
  }

  @Test
  public void testGetOAuth2UserNonOAuth2() throws ServiceUnavailableException {
    initializeRequest("Bearer badToken");
    assertNull(authenticator.getOAuth2User(request, config));

    initializeRequest("Bearer");
    assertNull(authenticator.getOAuth2User(request, config));

    initializeRequest("Random abc");
    assertNull(authenticator.getOAuth2User(request, config));
  }

  @Test
  public void testGetOAuth2UserScopeNotAllowed() throws Exception {
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret(SCOPES));
    when(oauthService.getAuthorizedScopes(SCOPES)).thenThrow(new OAuthRequestException("any"))
        .thenReturn(null).thenReturn(new String[] {"scope3"});
    for (int i = 0; i < 3; i++) {
      assertNull(authenticator.getOAuth2User(request, config));
    }
  }

  @Test
  public void testGetOAuth2UserAppEngineProdClientIdNotAllowed() throws Exception {
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret(SCOPES));
    when(oauthService.getAuthorizedScopes(SCOPES)).thenReturn(SCOPES);
    when(oauthService.getClientId(SCOPES)).thenThrow(new OAuthRequestException("any"))
        .thenReturn(null).thenReturn(CLIENT_ID);
    when(config.getClientIds()).thenReturn(ImmutableList.of("clientId2"));
    for (int i = 0; i < 3; i++) {
      assertNull(authenticator.getOAuth2User(request, config));
    }
  }

  @Test
  public void testGetOAuth2UserUserFailure() throws Exception {
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret(SCOPES));
    when(oauthService.getAuthorizedScopes(SCOPES)).thenReturn(SCOPES);
    when(oauthService.getClientId(SCOPES)).thenReturn(CLIENT_ID);
    when(config.getClientIds()).thenReturn(ImmutableList.of(CLIENT_ID));
    when(oauthService.getCurrentUser(SCOPES)).thenReturn(null);
    assertNull(authenticator.getOAuth2User(request, config));
  }

  @Test
  public void testGetOAuth2User() throws Exception {
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret(SCOPES));
    when(oauthService.getAuthorizedScopes(SCOPES)).thenReturn(SCOPES);
    when(oauthService.getClientId(SCOPES)).thenReturn(CLIENT_ID);
    when(config.getClientIds()).thenReturn(ImmutableList.of(CLIENT_ID));
    when(oauthService.getCurrentUser(SCOPES)).thenReturn(APP_ENGINE_USER);
    assertEquals(APP_ENGINE_USER, authenticator.getOAuth2User(request, config));
  }

  @Test
  public void testGetOAuth2UserSkipClientIdCheck() throws Exception {
    request.removeAttribute(Attribute.ENABLE_CLIENT_ID_WHITELIST);
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret(SCOPES));
    when(oauthService.getAuthorizedScopes(SCOPES)).thenReturn(SCOPES);
    when(oauthService.getClientId(SCOPES)).thenReturn(CLIENT_ID);
    when(oauthService.getCurrentUser(SCOPES)).thenReturn(APP_ENGINE_USER);
    assertEquals(APP_ENGINE_USER, authenticator.getOAuth2User(request, config));
  }

  @Test
  public void testGetOAuth2UserAppEngineDevClientIdNotAllowed() throws ServiceUnavailableException {
    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "Developement");
    when(config.getScopeExpression()).thenReturn(AuthScopeExpressions.interpret(SCOPES));
    when(config.getClientIds()).thenReturn(ImmutableList.of("clientId2"));
    assertNull(authenticator.getOAuth2User(request, config));
  }

  @Test
  public void testAuthenticateNonAppEngine() throws ServiceUnavailableException {
    System.clearProperty(EnvUtil.ENV_APPENGINE_RUNTIME);
    assertNull(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticateSkipTokenAuth() throws ServiceUnavailableException {
    attr.set(Attribute.SKIP_TOKEN_AUTH, true);
    assertNull(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticateOAuth2Fail() throws ServiceUnavailableException {
    authenticator = new GoogleAppEngineAuthenticator(oauthService, userService) {
      @Override
      com.google.appengine.api.users.User getOAuth2User(HttpServletRequest request,
          ApiMethodConfig config) {
        return null;
      }

      @Override
      boolean shouldTryCookieAuth(ApiMethodConfig config) {
        return false;
      }
    };
    assertNull(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticateOAuth2() throws ServiceUnavailableException {
    authenticator = new GoogleAppEngineAuthenticator(oauthService, userService) {
      @Override
      com.google.appengine.api.users.User getOAuth2User(HttpServletRequest request,
          ApiMethodConfig config) {
        return APP_ENGINE_USER;
      }
    };
    assertEquals(USER, authenticator.authenticate(request));
    assertEquals(APP_ENGINE_USER, attr.get(Attribute.AUTHENTICATED_APPENGINE_USER));
  }

  @Test
  public void testAuthenticateSkipTokenAuthCookieAuthFail() throws ServiceUnavailableException {
    attr.set(Attribute.SKIP_TOKEN_AUTH, true);
    authenticator = new GoogleAppEngineAuthenticator(oauthService, userService) {
      @Override
      boolean shouldTryCookieAuth(ApiMethodConfig config) {
        return true;
      }
    };
    when(userService.getCurrentUser()).thenReturn(null);
    assertNull(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticateSkipTokenAuthCookieAuth() throws ServiceUnavailableException {
    attr.set(Attribute.SKIP_TOKEN_AUTH, true);
    authenticator = new GoogleAppEngineAuthenticator(oauthService, userService) {
      @Override
      boolean shouldTryCookieAuth(ApiMethodConfig config) {
        return true;
      }
    };
    when(userService.getCurrentUser()).thenReturn(APP_ENGINE_USER);
    assertEquals(USER, authenticator.authenticate(request));
    assertEquals(APP_ENGINE_USER, attr.get(Attribute.AUTHENTICATED_APPENGINE_USER));
  }

  @Test
  public void testAuthenticateOAuth2CookieAuthBothFail() throws ServiceUnavailableException {
    authenticator = new GoogleAppEngineAuthenticator(oauthService, userService) {
      @Override
      com.google.appengine.api.users.User getOAuth2User(HttpServletRequest request,
          ApiMethodConfig config) {
        return null;
      }

      @Override
      boolean shouldTryCookieAuth(ApiMethodConfig config) {
        return true;
      }
    };
    when(userService.getCurrentUser()).thenReturn(null);
    assertNull(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticateOAuth2FailCookieAuth() throws ServiceUnavailableException {
    authenticator = new GoogleAppEngineAuthenticator(oauthService, userService) {
      @Override
      com.google.appengine.api.users.User getOAuth2User(HttpServletRequest request,
          ApiMethodConfig config) {
        return null;
      }

      @Override
      boolean shouldTryCookieAuth(ApiMethodConfig config) {
        return true;
      }
    };
    when(userService.getCurrentUser()).thenReturn(APP_ENGINE_USER);
    assertEquals(USER, authenticator.authenticate(request));
    assertEquals(APP_ENGINE_USER, attr.get(Attribute.AUTHENTICATED_APPENGINE_USER));
  }
}
