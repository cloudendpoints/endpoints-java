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
package com.google.api.server.spi.config;

import javax.servlet.http.HttpServletRequest;

/**
 * Peer authenticators aim to verify the peer and run before {@code Authenticator}. It returns false
 * if authentication failed and stops handling the rest of the request; true if authentication
 * succeeds and continue to execute rest of peer authenticators.
 *
 * <p>
 * If no peer authenticator is set, {@code EndpointsPeerAuthenticator} will be the default to verify
 * the request is from Google. If you supply your own peer authenticator, make sure you also put
 * {@code EndpointsPeerAuthenticator} to the head of peerAuthenticators list to verify the request
 * is from Google.
 */
public interface PeerAuthenticator {
  boolean authenticate(HttpServletRequest request);
}
