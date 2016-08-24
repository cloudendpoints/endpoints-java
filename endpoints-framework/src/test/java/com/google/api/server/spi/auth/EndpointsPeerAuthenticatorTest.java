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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.server.spi.EnvUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetAddress;

/**
 * Tests for {@code EndpointsPeerAuthenticator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EndpointsPeerAuthenticatorTest {
  private static final String FAKE_TOKEN = "fakeToken";

  @Mock private GoogleJwtAuthenticator jwtAuthenticator;
  @Mock private GoogleIdToken token;

  private final Payload payload = new Payload();
  private MockHttpServletRequest request;
  private EndpointsPeerAuthenticator authenticator;

  @Before
  public void setUp() throws Exception {
    System.clearProperty(EnvUtil.ENV_APPENGINE_RUNTIME);
    authenticator = new EndpointsPeerAuthenticator(jwtAuthenticator);
    request = new MockHttpServletRequest();
    request.setRemoteAddr("8.8.8.8");
  }

  @After
  public void tearDown() throws Exception {
    EnvUtil.recoverAppEngineRuntime();
  }

  @Test
  public void testAuthenticate_localHost() throws Exception {
    request.setRemoteAddr(InetAddress.getLocalHost().getHostAddress());
    assertTrue(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_localHostIp() {
    request.setRemoteAddr("127.0.0.1");
    assertTrue(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_appEngineRunTimeNoXAppengineHeader() {
    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "Production");
    assertFalse(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_appEngineRunTimeUnmatchedXAppengineHeader() {
    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "Production");
    request.addHeader(EndpointsPeerAuthenticator.APPENGINE_PEER, "invalid");
    assertFalse(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_appEngineRunTimeSuccess() {
    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "Production");
    request.addHeader(EndpointsPeerAuthenticator.HEADER_APPENGINE_PEER,
        EndpointsPeerAuthenticator.APPENGINE_PEER);
    assertTrue(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_noPeerAuthorizationHeader() {
    assertFalse(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_invalidHeader() {
    request.addHeader(EndpointsPeerAuthenticator.HEADER_PEER_AUTHORIZATION, FAKE_TOKEN);
    when(jwtAuthenticator.verifyToken(FAKE_TOKEN)).thenReturn(null);
    assertFalse(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_invalidEmail() {
    payload.setEmail("invalid@gmail.com");
    request.addHeader(EndpointsPeerAuthenticator.HEADER_PEER_AUTHORIZATION, FAKE_TOKEN);
    when(jwtAuthenticator.verifyToken(FAKE_TOKEN)).thenReturn(token);
    when(token.getPayload()).thenReturn(payload);
    assertFalse(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_unmatchedHost() {
    payload.setEmail(EndpointsPeerAuthenticator.SIGNER);
    payload.setAudience("http://otherhost.com/api");
    request.addHeader(EndpointsPeerAuthenticator.HEADER_PEER_AUTHORIZATION, FAKE_TOKEN);
    request.addHeader("Host", "myhost.com");
    when(jwtAuthenticator.verifyToken(FAKE_TOKEN)).thenReturn(token);
    when(token.getPayload()).thenReturn(payload);
    assertFalse(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_unmatchedPortDefault() {
    payload.setEmail(EndpointsPeerAuthenticator.SIGNER);
    payload.setAudience("https://myhost.com/api");
    request.addHeader(EndpointsPeerAuthenticator.HEADER_PEER_AUTHORIZATION, FAKE_TOKEN);
    request.addHeader("Host", "myhost.com");
    when(jwtAuthenticator.verifyToken(FAKE_TOKEN)).thenReturn(token);
    when(token.getPayload()).thenReturn(payload);
    assertFalse(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_unmatchedPort() {
    request = createRequest("myhost.com", 456, "", "", "");
    request.setRemoteAddr("8.8.8.8");
    payload.setEmail(EndpointsPeerAuthenticator.SIGNER);
    payload.setAudience("http://otherhost.com:789/api");
    request.addHeader(EndpointsPeerAuthenticator.HEADER_PEER_AUTHORIZATION, FAKE_TOKEN);
    when(jwtAuthenticator.verifyToken(FAKE_TOKEN)).thenReturn(token);
    when(token.getPayload()).thenReturn(payload);
    assertFalse(authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_success() {
    request = createRequest("myhost.com", 456, "", "", "");
    request.setRemoteAddr("8.8.8.8");
    payload.setEmail(EndpointsPeerAuthenticator.SIGNER);
    payload.setAudience("http://myhost.com:456/api");
    request.addHeader(EndpointsPeerAuthenticator.HEADER_PEER_AUTHORIZATION, FAKE_TOKEN);
    when(jwtAuthenticator.verifyToken(FAKE_TOKEN)).thenReturn(token);
    when(token.getPayload()).thenReturn(payload);
    assertTrue(authenticator.authenticate(request));
  }

  @Test
  public void testNewInstance() {
    try {
      authenticator = EndpointsPeerAuthenticator.class.newInstance();
    } catch (Exception e) {
      fail("newInstance on EndpointsPeerAuthenticator.class failed");
    }
  }

  private static MockHttpServletRequest createRequest(
      String host, int port, String servletPath, String contextPath, String queryString) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Host", host);
    request.setServerName(host);
    request.setServerPort(port);
    request.setServletPath(servletPath);
    request.setQueryString(queryString);
    request.setContextPath(contextPath);
    return request;
  }
}
