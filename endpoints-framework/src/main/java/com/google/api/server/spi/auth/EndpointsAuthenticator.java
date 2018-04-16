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
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.common.annotations.VisibleForTesting;

import javax.servlet.http.HttpServletRequest;

/**
 * Standard Endpoints authenticator for Google users.
 *
 * @author Eric Orth
 */
@Singleton
public class EndpointsAuthenticator implements Authenticator {
  private final GoogleJwtAuthenticator jwtAuthenticator;
  private final GoogleAppEngineAuthenticator appEngineAuthenticator;
  private final GoogleOAuth2Authenticator oauth2Authenticator;

  public EndpointsAuthenticator() {
    this.jwtAuthenticator = new GoogleJwtAuthenticator();
    this.appEngineAuthenticator = new GoogleAppEngineAuthenticator();
    this.oauth2Authenticator = new GoogleOAuth2Authenticator();
  }

  @VisibleForTesting
  public EndpointsAuthenticator(GoogleJwtAuthenticator jwtAuthenticator,
      GoogleAppEngineAuthenticator appEngineAuthenticator,
      GoogleOAuth2Authenticator oauth2Authenticator) {
    this.jwtAuthenticator = jwtAuthenticator;
    this.appEngineAuthenticator = appEngineAuthenticator;
    this.oauth2Authenticator = oauth2Authenticator;
  }

  @Override
  public User authenticate(HttpServletRequest request) throws ServiceUnavailableException {
    Attribute attr = Attribute.from(request);
    User user = jwtAuthenticator.authenticate(request);
    if (user == null) {
      if (EnvUtil.isRunningOnAppEngine() && attr.isEnabled(Attribute.REQUIRE_APPENGINE_USER)) {
        user = appEngineAuthenticator.authenticate(request);
      } else {
        user = oauth2Authenticator.authenticate(request);
      }
    }
    return user;
  }
}
