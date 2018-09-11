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

import javax.servlet.http.HttpServletRequest;

/**
 * Utilities used to do peer authorization.
 */
public class PeerAuth {
  private static final Singleton.Instantiator<PeerAuthenticator> INSTANTIATOR
      = new Singleton.Instantiator<PeerAuthenticator>(new EndpointsPeerAuthenticator());

  /**
   * Must be used to instantiate new {@link PeerAuthenticator}s to honor
   * {@link com.google.api.server.spi.config.Singleton} contract.
   *
   * @return a new instance of clazz, or an existing one if clazz is annotated with @{@link
   * com.google.api.server.spi.config.Singleton}
   */
  public static PeerAuthenticator instantiatePeerAuthenticator(Class<? extends PeerAuthenticator> clazz) {
    return INSTANTIATOR.getInstanceOrDefault(clazz);
  }

  private final HttpServletRequest request;
  private final Attribute attr;
  private final ApiMethodConfig config;

  @VisibleForTesting
  PeerAuth(HttpServletRequest request) {
    this.request = request;
    attr = Attribute.from(request);
    config = attr.get(Attribute.API_METHOD_CONFIG);
  }

  static PeerAuth from(HttpServletRequest request) {
    return new PeerAuth(request);
  }

  @VisibleForTesting
  Iterable<PeerAuthenticator> getPeerAuthenticatorInstances() {
    return INSTANTIATOR.getInstancesOrDefault(config.getPeerAuthenticators());
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
