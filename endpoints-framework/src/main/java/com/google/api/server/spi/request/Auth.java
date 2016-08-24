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
import com.google.api.server.spi.auth.EndpointsAuthenticator;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.Singleton;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Utilities for end user authentication.
 */
public class Auth {
  private static final Logger logger = Logger.getLogger(Auth.class.getName());

  private static volatile Map<Class<? extends Authenticator>, Authenticator>
      authenticatorInstances = new HashMap<Class<? extends Authenticator>, Authenticator>();

  private static final Authenticator DEFAULT_AUTHENTICATOR = new EndpointsAuthenticator();

  private static final Function<Class<? extends Authenticator>, Authenticator>
      INSTANTIATE_AUTHENTICATOR = new Function<Class<? extends Authenticator>, Authenticator>() {
        @Override
        public Authenticator apply(Class<? extends Authenticator> clazz) {
          try {
            if (clazz.getAnnotation(Singleton.class) != null) {
              if (!authenticatorInstances.containsKey(clazz)) {
                authenticatorInstances.put(clazz, clazz.newInstance());
              }
              return authenticatorInstances.get(clazz);
            } else {
              return clazz.newInstance();
            }
          } catch (IllegalAccessException | InstantiationException e) {
            logger.log(Level.WARNING, "Could not instantiate  authenticator: " + clazz.getName());
            return null;
          }
        }
      };

  private final HttpServletRequest request;
  private final Attribute attr;
  private final ApiMethodConfig config;

  @VisibleForTesting
  Auth(HttpServletRequest request) {
    this.request = request;
    attr = Attribute.from(request);
    config = (ApiMethodConfig) attr.get(Attribute.API_METHOD_CONFIG);
  }

  static Auth from(HttpServletRequest request) {
    return new Auth(request);
  }

  @VisibleForTesting
  Iterable<Authenticator> getAuthenticatorInstances() {
    List<Class<? extends Authenticator>> classes = config.getAuthenticators();
    return classes == null ? ImmutableList.of(DEFAULT_AUTHENTICATOR)
        : Iterables.filter(Iterables.transform(classes, INSTANTIATE_AUTHENTICATOR),
            Predicates.notNull());
  }

  /**
   * Authenticate the request and retrieve a {@code User}. Should only run once per request.
   */
  User authenticate() {
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
  com.google.appengine.api.users.User authenticateAppEngineUser() {
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
        (com.google.appengine.api.users.User) attr.get(Attribute.AUTHENTICATED_APPENGINE_USER);
    if (appEngineUser != null) {
      return appEngineUser;
    } else {
      return user.getEmail() == null
          ? null : new com.google.appengine.api.users.User(user.getEmail(), "", user.getId());
    }
  }
}
