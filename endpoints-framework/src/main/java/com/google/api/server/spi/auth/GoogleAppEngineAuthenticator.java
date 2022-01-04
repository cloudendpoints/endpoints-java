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

import com.google.api.server.spi.EnvUtil;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.Singleton;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.oauth.OAuthService;
import com.google.appengine.api.oauth.OAuthServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import com.google.common.flogger.FluentLogger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Authenticator for Google App Engine. It validates incoming OAuth2 credentials or cookies and
 * obtains an {@code com.google.appengine.api.users.User} object.
 */
@Singleton
class GoogleAppEngineAuthenticator implements Authenticator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final OAuthService oauthService;
  private final UserService userService;

  public GoogleAppEngineAuthenticator() {
    this(OAuthServiceFactory.getOAuthService(), UserServiceFactory.getUserService());
  }

  public GoogleAppEngineAuthenticator(OAuthService oauthService, UserService userService) {
    this.oauthService = oauthService;
    this.userService = userService;
  }

  @VisibleForTesting
  String getOAuth2ClientIdDev(String token) throws ServiceUnavailableException {
    GoogleAuth.TokenInfo tokenInfo = GoogleAuth.getTokenInfoRemote(token);
    return tokenInfo != null ? tokenInfo.clientId : null;
  }

  @VisibleForTesting
  boolean shouldTryCookieAuth(ApiMethodConfig config) {
    return config.getApiClassConfig().getApiConfig().getAuthConfig().getAllowCookieAuth();
  }

  @VisibleForTesting
  com.google.appengine.api.users.User getOAuth2User(HttpServletRequest request,
      ApiMethodConfig config) throws ServiceUnavailableException {
    String token = GoogleAuth.getAuthToken(request);
    if (!GoogleAuth.isOAuth2Token(token)) {
      return null;
    }
    AuthScopeExpression scopeExpression = config.getScopeExpression();
    String[] allScopes = scopeExpression.getAllScopes();
    String clientId = null;
    if (EnvUtil.isRunningOnAppEngineProd()) {
      try {
        String[] authorizedScopes = oauthService.getAuthorizedScopes(allScopes);
        boolean authorized = false;
        if (authorizedScopes != null) {
          // Authorize against the scopes based on the scope expression.
          authorized = scopeExpression.isAuthorized(ImmutableSet.copyOf(authorizedScopes));
        }
        if (!authorized) {
          logger.atWarning().log(
              "Access token does not contain sufficient scopes from: %s", scopeExpression);
          return null;
        }
        clientId = oauthService.getClientId(allScopes);
      } catch (OAuthRequestException e) {
        logger.atWarning().withCause(e).log("Failed to get client id for '%s'", scopeExpression);
        return null;
      }
    } else { // Dev env.
      clientId = getOAuth2ClientIdDev(token);
    }
    // Check client id.
    if ((Attribute.from(request).isEnabled(Attribute.ENABLE_CLIENT_ID_WHITELIST)
        && !GoogleAuth.checkClientId(clientId, config.getClientIds(), true))) {
      logger.atWarning().log("ClientId is not allowed: %s", clientId);
      return null;
    }

    try {
      com.google.appengine.api.users.User appEngineUser = oauthService.getCurrentUser(allScopes);
      return appEngineUser;
    } catch (OAuthRequestException e) {
      logger.atWarning().withCause(e).log("Failed to get user for '%s'", scopeExpression);
    }
    return null;
  }

  @Override
  public User authenticate(HttpServletRequest request) throws ServiceUnavailableException {
    Attribute attr = Attribute.from(request);
    if (!EnvUtil.isRunningOnAppEngine()) {
      return null;
    }

    com.google.appengine.api.users.User appEngineUser = null;
    ApiMethodConfig config = attr.get(Attribute.API_METHOD_CONFIG);
    if (!attr.isEnabled(Attribute.SKIP_TOKEN_AUTH)) {
      appEngineUser = getOAuth2User(request, config);
    }
    if (appEngineUser == null && shouldTryCookieAuth(config)) {
      appEngineUser = userService.getCurrentUser();
    }
    if (appEngineUser == null) {
      return null;
    }
    User user = new User(appEngineUser.getEmail());
    if (attr.isEnabled(Attribute.REQUIRE_APPENGINE_USER)) {
      attr.set(Attribute.AUTHENTICATED_APPENGINE_USER, appEngineUser);
    }
    return user;
  }
}
