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
package com.google.api.server.spi.request;

import com.google.api.server.spi.EnvUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.EndpointsAuthenticator;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.Singleton;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.common.annotations.VisibleForTesting;

import javax.servlet.http.HttpServletRequest;

/**
 * Utilities for end user authentication.
 */
public class Auth {
  private static final Singleton.Instantiator<Authenticator> INSTANTIATOR
      = new Singleton.Instantiator<Authenticator>(new EndpointsAuthenticator());

  /**
   * Must be used to instantiate new {@link Authenticator}s to honor
   * {@link com.google.api.server.spi.config.Singleton} contract.
   *
   *  @return a new instance of clazz, or an existing one if clazz is annotated with @{@link
   * com.google.api.server.spi.config.Singleton}
   */
  public static Authenticator instantiateAuthenticator(Class<? extends Authenticator> clazz) {
    return INSTANTIATOR.getInstanceOrDefault(clazz);
  }

  private final HttpServletRequest request;
  private final Attribute attr;
  private final ApiMethodConfig config;

  @VisibleForTesting
  Auth(HttpServletRequest request) {
    this.request = request;
    attr = Attribute.from(request);
    config = attr.get(Attribute.API_METHOD_CONFIG);
  }

  static Auth from(HttpServletRequest request) {
    return new Auth(request);
  }

  @VisibleForTesting
  Iterable<Authenticator> getAuthenticatorInstances() {
    return INSTANTIATOR.getInstancesOrDefault(config.getAuthenticators());
  }

  /**
   * Authenticate the request and retrieve a {@code User}. Should only run once per request.
   */
  User authenticate() throws ServiceException {
    Iterable<Authenticator> authenticators = getAuthenticatorInstances();
    User user = null;
    if (authenticators != null) {
      for (Authenticator authenticator : authenticators) {
        user = authenticator.authenticate(request);
        if (user != null) {
          break;
        }
      }
    }
    return user;
  }

  /**
   * Authenticate the request and retrieve an {@code com.google.appengine.api.users.User}. Should
   * only run once per request.
   */
  com.google.appengine.api.users.User authenticateAppEngineUser() throws ServiceException {
    if (!EnvUtil.isRunningOnAppEngine()) {
      return null;
    }
    attr.set(Attribute.REQUIRE_APPENGINE_USER, true);
    User user = authenticate();
    attr.set(Attribute.REQUIRE_APPENGINE_USER, false);
    if (user == null) {
      return null;
    }
    com.google.appengine.api.users.User appEngineUser =
        attr.get(Attribute.AUTHENTICATED_APPENGINE_USER);
    if (appEngineUser != null) {
      return appEngineUser;
    } else {
      return user.getEmail() == null
          ? null : new com.google.appengine.api.users.User(user.getEmail(), "", user.getId());
    }
  }
}
