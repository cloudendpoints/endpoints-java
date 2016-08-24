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
package com.google.api.server.spi;

import com.google.api.server.spi.auth.EndpointsPeerAuthenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.Singleton;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.request.Attribute;
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
 * Utilities used to do peer authorization.
 */
public class PeerAuth {
  private static final Logger logger = Logger.getLogger(PeerAuth.class.getName());

  private static volatile
      Map<Class<? extends PeerAuthenticator>, PeerAuthenticator> peerAuthenticatorInstances =
          new HashMap<Class<? extends PeerAuthenticator>, PeerAuthenticator>();

  private static final PeerAuthenticator DEFAULT_PEER_AUTHENTICATOR =
      new EndpointsPeerAuthenticator();

  private static final
      Function<Class<? extends PeerAuthenticator>, PeerAuthenticator>
      INSTANTIATE_PEER_AUTHENTICATOR =
        new Function<Class<? extends PeerAuthenticator>, PeerAuthenticator>() {
        @Override
        public PeerAuthenticator apply(Class<? extends PeerAuthenticator> clazz) {
          try {
            if (clazz.getAnnotation(Singleton.class) != null) {
              if (!peerAuthenticatorInstances.containsKey(clazz)) {
                peerAuthenticatorInstances.put(clazz, clazz.newInstance());
              }
              return peerAuthenticatorInstances.get(clazz);
            } else {
              return clazz.newInstance();
            }
          } catch (IllegalAccessException | InstantiationException e) {
            logger.log(Level.WARNING,
                "Could not instantiate peer authenticator: " + clazz.getName());
            return null;
          }
        }
      };

  private final HttpServletRequest request;
  private final Attribute attr;
  private final ApiMethodConfig config;

  @VisibleForTesting
  PeerAuth(HttpServletRequest request) {
    this.request = request;
    attr = Attribute.from(request);
    config = (ApiMethodConfig) attr.get(Attribute.API_METHOD_CONFIG);
  }

  static PeerAuth from(HttpServletRequest request) {
    return new PeerAuth(request);
  }

  @VisibleForTesting
  Iterable<PeerAuthenticator> getPeerAuthenticatorInstances() {
    List<Class<? extends PeerAuthenticator>> classes = config.getPeerAuthenticators();
    return classes == null ? ImmutableList.of(DEFAULT_PEER_AUTHENTICATOR)
        : Iterables.filter(Iterables.transform(classes, INSTANTIATE_PEER_AUTHENTICATOR),
            Predicates.notNull());
  }

  boolean authorizePeer() {
    if (!attr.isEnabled(Attribute.RESTRICT_SERVLET)) {
      return true;
    }
    Iterable<PeerAuthenticator> peerAuthenticators = getPeerAuthenticatorInstances();
    if (peerAuthenticators != null) {
      for (PeerAuthenticator peerAuthenticator : peerAuthenticators) {
        if (!peerAuthenticator.authenticate(request)) {
          return false;
        }
      }
    }
    return true;
  }
}
