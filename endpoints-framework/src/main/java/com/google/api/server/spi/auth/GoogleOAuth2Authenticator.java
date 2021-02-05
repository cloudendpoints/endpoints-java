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

import com.google.api.server.spi.Strings;
import com.google.api.server.spi.auth.GoogleAuth.TokenInfo;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.Singleton;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import com.google.common.flogger.FluentLogger;

import javax.servlet.http.HttpServletRequest;

/**
 * Authenticator for Google OAuth2 credentials. It calls public Google OAuth2 API to validate the
 * token and is thus platform-independent.
 */
@Singleton
public class GoogleOAuth2Authenticator implements Authenticator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public User authenticate(HttpServletRequest request) throws ServiceUnavailableException {
    Attribute attr = Attribute.from(request);
    if (attr.isEnabled(Attribute.SKIP_TOKEN_AUTH)) {
      return null;
    }

    String token = GoogleAuth.getAuthToken(request);
    if (!GoogleAuth.isOAuth2Token(token)) {
      return null;
    }

    GoogleAuth.TokenInfo tokenInfo = getTokenInfoRemote(token);
    if (tokenInfo == null) {
      return null;
    }

    attr.set(Attribute.TOKEN_INFO, tokenInfo);

    ApiMethodConfig config = (ApiMethodConfig) request.getAttribute(Attribute.API_METHOD_CONFIG);

    // Check scopes.
    if (Strings.isEmptyOrWhitespace(tokenInfo.scopes)) {
      logger.atWarning().log("Access token does not contain a valid scope");
      return null;
    }
    String[] authorizedScopes = tokenInfo.scopes.split("\\s+");
    if (!config.getScopeExpression().isAuthorized(ImmutableSet.copyOf(authorizedScopes))) {
      logger.atWarning().log(
          "Access token does not contain sufficient scopes from: %s", config.getScopeExpression());
      return null;
    }

    // Check clientId.
    if (attr.isEnabled(Attribute.ENABLE_CLIENT_ID_WHITELIST)
        && !GoogleAuth.checkClientId(tokenInfo.clientId, config.getClientIds(), true)) {
      logger.atWarning().log("ClientId is not allowed: %s", tokenInfo.clientId);
      return null;
    }

    User user = new User(tokenInfo.userId, tokenInfo.email);
    if (attr.isEnabled(Attribute.REQUIRE_APPENGINE_USER)) {
      com.google.appengine.api.users.User appEngineUser =
          new com.google.appengine.api.users.User(tokenInfo.email, "");
      request.setAttribute(Attribute.AUTHENTICATED_APPENGINE_USER, appEngineUser);
    }
    return user;
  }

  @VisibleForTesting
  TokenInfo getTokenInfoRemote(String token) throws ServiceUnavailableException {
    return GoogleAuth.getTokenInfoRemote(token);
  }
}
