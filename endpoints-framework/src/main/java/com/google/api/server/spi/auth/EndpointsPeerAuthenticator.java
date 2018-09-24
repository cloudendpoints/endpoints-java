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
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.server.spi.Client;
import com.google.api.server.spi.EnvUtil;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * The default peer authenticator. It verify the request is from Google Cloud Endpoints frontend. It
 * is different from EndpointsAuthenticator, which authenticates the end user.
 */
public class EndpointsPeerAuthenticator implements PeerAuthenticator {
  @VisibleForTesting
  static final String ISSUER = "https://www.cloudendpointsapis.com";
  @VisibleForTesting
  static final String SIGNER = "cloud-endpoints-signer@system.gserviceaccount.com";
  @VisibleForTesting
  static final String HEADER_APPENGINE_PEER = "X-Appengine-Peer";
  @VisibleForTesting
  static final String APPENGINE_PEER = "apiserving";
  @VisibleForTesting
  static final String HEADER_PEER_AUTHORIZATION = "Peer-Authorization";

  private static final String PUBLIC_CERT_URL =
      "https://www.googleapis.com/service_accounts/v1/metadata/x509/" + SIGNER;
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final ImmutableSet<String> localHostAddresses = getLocalHostAddresses();

  private final GoogleJwtAuthenticator jwtAuthenticator;

  private static ImmutableSet<String> getLocalHostAddresses() {
    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
    try {
      builder.add(InetAddress.getLocalHost().getHostAddress());
    } catch (IOException e) {
      // try next.
    }
    try {
      builder.add(InetAddress.getByName(null).getHostAddress());
    } catch (IOException e) {
      // try next.
    }
    try {
      for (InetAddress inetAddress : InetAddress.getAllByName("localhost")) {
        builder.add(inetAddress.getHostAddress());
      }
    } catch (IOException e) {
      // check at the end.
    }
    ImmutableSet<String> localHostSet = builder.build();
    if (localHostSet.isEmpty()) {
      logger.atWarning().log("Unable to lookup local addresses.");
    }
    return localHostSet;
  }

  public EndpointsPeerAuthenticator() {
    Client client = Client.getInstance();
    GooglePublicKeysManager keyManager = new GooglePublicKeysManager.Builder(
        client.getHttpTransport(), client.getJsonFactory()).setPublicCertsEncodedUrl(
        PUBLIC_CERT_URL).build();
    GoogleIdTokenVerifier verifier =
        new GoogleIdTokenVerifier.Builder(keyManager).setIssuer(ISSUER).build();
    jwtAuthenticator = new GoogleJwtAuthenticator(verifier);
  }

  @VisibleForTesting
  public EndpointsPeerAuthenticator(GoogleJwtAuthenticator jwtAuthenticator) {
    this.jwtAuthenticator = jwtAuthenticator;
  }

  @Override
  public boolean authenticate(HttpServletRequest request) {
    // Preserve current check for App Engine Env.
    if (EnvUtil.isRunningOnAppEngine()) {
      return APPENGINE_PEER.equals(request.getHeader(HEADER_APPENGINE_PEER));
    }

    // Skip peer verification for localhost request.
    if (localHostAddresses.contains(request.getRemoteAddr())) {
      logger.atFine().log("Skip endpoints peer verication from localhost.");
      return true;
    }
    // Verify peer token, signer and audience.
    GoogleIdToken idToken =
        jwtAuthenticator.verifyToken(request.getHeader(HEADER_PEER_AUTHORIZATION));
    if (idToken == null || !SIGNER.equals(idToken.getPayload().getEmail())
        || !matchHostAndPort(idToken, request)) {
      return false;
    }
    return true;
  }

  private boolean matchHostAndPort(GoogleIdToken idToken, HttpServletRequest request) {
    URL urlFromIdToken;
    URL urlFromRequest;
    try {
      urlFromIdToken = new URL((String) idToken.getPayload().getAudience());
      urlFromRequest = new URL(request.getRequestURL().toString());
      return urlFromIdToken.getHost().equals(urlFromRequest.getHost())
          && getPort(urlFromIdToken) == getPort(urlFromRequest);
    } catch (MalformedURLException e) {
      logger.atWarning().log("Invalid URL from request");
      return false;
    }
  }

  private int getPort(URL url) {
    int port = url.getPort();
    return port == -1 ? url.getDefaultPort() : port;
  }
}
