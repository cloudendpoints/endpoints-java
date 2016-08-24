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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.auth.EndpointsPeerAuthenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.testing.FailPeerAuthenticator;
import com.google.api.server.spi.testing.PassPeerAuthenticator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

/**
 * Test for PeerAuth.
 */
@RunWith(MockitoJUnitRunner.class)
public class PeerAuthTest {
  @Mock private ApiMethodConfig config;

  private MockHttpServletRequest request;
  private PeerAuth peerAuth;
  private Attribute attr;

  @Before
  public void setUp() throws Exception {
    request = new MockHttpServletRequest();
    attr = Attribute.from(request);
    attr.set(Attribute.RESTRICT_SERVLET, true);
    attr.set(Attribute.API_METHOD_CONFIG, config);
    peerAuth = PeerAuth.from(request);
  }

  @Test
  public void testGetPeerAuthenticatorInstances_default() throws Exception {
    when(config.getPeerAuthenticators()).thenReturn(null);
    List<PeerAuthenticator> peerAuthenticators =
        Lists.newArrayList(peerAuth.getPeerAuthenticatorInstances());
    assertEquals(1, peerAuthenticators.size());
    assertTrue(peerAuthenticators.get(0) instanceof EndpointsPeerAuthenticator);
  }

  @Test
  public void testGetPeerAuthenticatorInstances() throws Exception {
    when(config.getPeerAuthenticators()).thenReturn(
        ImmutableList.of(PassPeerAuthenticator.class, FailPeerAuthenticator.class));
    List<PeerAuthenticator> peerAuthenticators =
        Lists.newArrayList(peerAuth.getPeerAuthenticatorInstances());
    assertEquals(2, peerAuthenticators.size());
    assertTrue(peerAuthenticators.get(0) instanceof PassPeerAuthenticator);
    assertTrue(peerAuthenticators.get(1) instanceof FailPeerAuthenticator);
  }

  @Test
  public void testGetPeerAuthenticatorInstances_singleton() throws Exception {
    when(config.getPeerAuthenticators()).thenReturn(ImmutableList.of(PassPeerAuthenticator.class,
        FailPeerAuthenticator.class, PassPeerAuthenticator.class));
    List<PeerAuthenticator> peerAuthenticators =
        Lists.newArrayList(peerAuth.getPeerAuthenticatorInstances());
    assertEquals(3, peerAuthenticators.size());
    assertTrue(peerAuthenticators.get(0) instanceof PassPeerAuthenticator);
    assertTrue(peerAuthenticators.get(1) instanceof FailPeerAuthenticator);
    assertTrue(peerAuthenticators.get(2) instanceof PassPeerAuthenticator);
    assertSame(peerAuthenticators.get(0), peerAuthenticators.get(2));
  }

  @Test
  public void testGetPeerAuthenticatorInstances_nonSingleton() throws Exception {
    when(config.getPeerAuthenticators()).thenReturn(ImmutableList.of(PassPeerAuthenticator.class,
        FailPeerAuthenticator.class, FailPeerAuthenticator.class));
    List<PeerAuthenticator> peerAuthenticators =
        Lists.newArrayList(peerAuth.getPeerAuthenticatorInstances());
    assertEquals(3, peerAuthenticators.size());
    assertTrue(peerAuthenticators.get(0) instanceof PassPeerAuthenticator);
    assertTrue(peerAuthenticators.get(1) instanceof FailPeerAuthenticator);
    assertTrue(peerAuthenticators.get(2) instanceof FailPeerAuthenticator);
    assertNotSame(peerAuthenticators.get(1), peerAuthenticators.get(2));
  }

  @Test
  public void testPeerAuthorize_nonRestricted() throws Exception {
    attr.set(Attribute.RESTRICT_SERVLET, false);
    assertTrue(peerAuth.authorizePeer());
  }

  @Test
  public void testPeerAuthorize_pass() throws Exception {
    when(config.getPeerAuthenticators()).thenReturn(
        ImmutableList.<Class<? extends PeerAuthenticator>>of(PassPeerAuthenticator.class));
    assertTrue(peerAuth.authorizePeer());
  }

  @Test
  public void testPeerAuthorize_fail() throws Exception {
    when(config.getPeerAuthenticators()).thenReturn(
        ImmutableList.<Class<? extends PeerAuthenticator>>of(FailPeerAuthenticator.class));
    assertFalse(peerAuth.authorizePeer());
  }

  @Test
  public void testPeerAuthorize_passThenFail() throws Exception {
    when(config.getPeerAuthenticators()).thenReturn(
        ImmutableList.of(PassPeerAuthenticator.class, FailPeerAuthenticator.class));
    assertFalse(peerAuth.authorizePeer());
  }

  @Test
  public void testPeerAuthorize_failThenPass() throws Exception {
    when(config.getPeerAuthenticators()).thenReturn(
        ImmutableList.of(FailPeerAuthenticator.class, PassPeerAuthenticator.class));
    assertFalse(peerAuth.authorizePeer());
  }
}
