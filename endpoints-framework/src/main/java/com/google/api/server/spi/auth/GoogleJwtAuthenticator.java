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

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.server.spi.Client;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.Singleton;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.request.Attribute;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Authenticator for Google issued JSON Web Token, currently specific for Google Id Token.
 */
@Singleton
public class GoogleJwtAuthenticator implements Authenticator {
  private static final Logger logger = Logger.getLogger(GoogleJwtAuthenticator.class.getName());
  private final GoogleIdTokenVerifier verifier;

  public GoogleJwtAuthenticator() {
    this(new GoogleIdTokenVerifier.Builder(Client.getInstance().getHttpTransport(),
        Client.getInstance().getJsonFactory()).build());
  }

  public GoogleJwtAuthenticator(GoogleIdTokenVerifier verifier) {
    this.verifier = verifier;
  }

  @VisibleForTesting
  GoogleIdToken verifyToken(String token) {
    if (token == null) {
      return null;
    }
    try {
      return verifier.verify(token);
    } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
      logger.warning(e.getMessage());
      return null;
    }
  }

  @Override
  public User authenticate(HttpServletRequest request) {
    Attribute attr = Attribute.from(request);
    if (attr.isEnabled(Attribute.SKIP_TOKEN_AUTH)) {
      return null;
    }

    String token = GoogleAuth.getAuthToken(request);
    if (!GoogleAuth.isJwt(token)) {
      return null;
    }

    GoogleIdToken idToken = verifyToken(token);
    if (idToken == null) {
      return null;
    }

    attr.set(Attribute.ID_TOKEN, idToken);

    String clientId = idToken.getPayload().getAuthorizedParty();
    String audience = (String) idToken.getPayload().getAudience();

    ApiMethodConfig config = attr.get(Attribute.API_METHOD_CONFIG);

    // Check client id.
    if ((attr.isEnabled(Attribute.ENABLE_CLIENT_ID_WHITELIST)
        && !GoogleAuth.checkClientId(clientId, config.getClientIds(), false))) {
      logger.warning("ClientId is not allowed: " + clientId);
      return null;
    }
    // Check audience.
    if (!GoogleAuth.checkAudience(audience, config.getAudiences(), clientId)) {
      logger.warning("Audience is not allowed: " + audience);
      return null;
    }

    String userId = idToken.getPayload().getSubject();
    String email = idToken.getPayload().getEmail();
    User user = (userId == null && email == null) ? null : new User(userId, email);
    if (attr.isEnabled(Attribute.REQUIRE_APPENGINE_USER)) {
      com.google.appengine.api.users.User appEngineUser =
          (email == null) ? null : new com.google.appengine.api.users.User(email, "");
      attr.set(Attribute.AUTHENTICATED_APPENGINE_USER, appEngineUser);
      logger.log(Level.FINE, "appEngineUser = {0}", appEngineUser);
    } else {
      logger.log(Level.FINE, "user = {0}", user);
    }
    return user;
  }
}
