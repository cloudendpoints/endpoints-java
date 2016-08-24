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
package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.Singleton;

import javax.servlet.http.HttpServletRequest;

/**
 * Simple dumb peer authenticator for tests that always pass.
 */
@Singleton
public class PassPeerAuthenticator implements PeerAuthenticator {
  @Override
  public boolean authenticate(HttpServletRequest request) {
    return true;
  }

  public static Class<? extends PeerAuthenticator>[] testArray = makeTestArray();

  // Unchecked cast needed to get a generic array type.
  @SuppressWarnings("unchecked")
  private static Class<? extends PeerAuthenticator>[] makeTestArray() {
    Class<?>[] peerAuthenticators = {PassPeerAuthenticator.class};
    return (Class<? extends PeerAuthenticator>[]) peerAuthenticators;
  }
}
