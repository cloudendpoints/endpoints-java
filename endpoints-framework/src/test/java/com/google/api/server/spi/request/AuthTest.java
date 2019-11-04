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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.EnvUtil;
import com.google.api.server.spi.auth.EndpointsAuthenticator;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.testing.AppEngineAuthenticator;
import com.google.api.server.spi.testing.FailAuthenticator;
import com.google.api.server.spi.testing.PassAuthenticator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

/**
 * Test for Auth.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthTest {
  @Mock protected ApiMethodConfig config;

  private MockHttpServletRequest request;
  private Auth auth;
  private Attribute attr;

  @Before
  public void setUp() throws Exception {
    request = new MockHttpServletRequest();
    attr = Attribute.from(request);
    attr.set(Attribute.API_METHOD_CONFIG, config);
    auth = Auth.from(request);
    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "Production");
  }

  @After
  public void tearDown() {
    System.clearProperty(EnvUtil.ENV_APPENGINE_RUNTIME);
  }

  @Test
  public void testGetAuthenticatorInstances_default() throws Exception {
    when(config.getAuthenticators()).thenReturn(null);
    List<Authenticator> authenticators = Lists.newArrayList(auth.getAuthenticatorInstances());
    assertEquals(1, authenticators.size());
    assertTrue(authenticators.get(0) instanceof EndpointsAuthenticator);
  }

  @Test
  public void testGetAuthenticatorInstances() throws Exception {
    when(config.getAuthenticators()).thenReturn(
        ImmutableList.of(PassAuthenticator.class, FailAuthenticator.class));
    List<Authenticator> authenticators = Lists.newArrayList(auth.getAuthenticatorInstances());
    assertEquals(2, authenticators.size());
    assertTrue(authenticators.get(0) instanceof PassAuthenticator);
    assertTrue(authenticators.get(1) instanceof FailAuthenticator);
  }

  @Test
  public void testGetAuthenticatorInstances_singleton() throws Exception {
    when(config.getAuthenticators()).thenReturn(ImmutableList.of(PassAuthenticator.class,
        FailAuthenticator.class, PassAuthenticator.class));
    List<Authenticator> authenticators = Lists.newArrayList(auth.getAuthenticatorInstances());
    assertEquals(3, authenticators.size());
    assertTrue(authenticators.get(0) instanceof PassAuthenticator);
    assertTrue(authenticators.get(1) instanceof FailAuthenticator);
    assertTrue(authenticators.get(2) instanceof PassAuthenticator);
    assertSame(authenticators.get(0), authenticators.get(2));
  }

  @Test
  public void testGetAuthenticatorInstances_nonSingleton() throws Exception {
    when(config.getAuthenticators()).thenReturn(ImmutableList.of(PassAuthenticator.class,
        FailAuthenticator.class, FailAuthenticator.class));
    List<Authenticator> authenticators = Lists.newArrayList(auth.getAuthenticatorInstances());
    assertEquals(3, authenticators.size());
    assertTrue(authenticators.get(0) instanceof PassAuthenticator);
    assertTrue(authenticators.get(1) instanceof FailAuthenticator);
    assertTrue(authenticators.get(2) instanceof FailAuthenticator);
    assertNotSame(authenticators.get(1), authenticators.get(2));
  }

  @Test
  public void testAuthenticate_pass() throws Exception {
    when(config.getAuthenticators()).thenReturn(
        ImmutableList.<Class<? extends Authenticator>>of(PassAuthenticator.class));
    assertEquals(PassAuthenticator.USER, auth.authenticate());
  }

  @Test
  public void testAuthenticate_fail() throws Exception {
    when(config.getAuthenticators()).thenReturn(
        ImmutableList.<Class<? extends Authenticator>>of(FailAuthenticator.class));
    assertNull(auth.authenticate());
  }

  @Test
  public void testAuthenticate_passThenFail() throws Exception {
    when(config.getAuthenticators()).thenReturn(
        ImmutableList.of(PassAuthenticator.class, FailAuthenticator.class));
    assertEquals(PassAuthenticator.USER, auth.authenticate());
  }

  @Test
  public void testAuthenticate_failThenPass() throws Exception {
    when(config.getAuthenticators()).thenReturn(
        ImmutableList.of(FailAuthenticator.class, PassAuthenticator.class));
    assertEquals(PassAuthenticator.USER, auth.authenticate());
  }

  @Test
  public void testAuthenticate_appEngine() throws Exception {
    when(config.getAuthenticators()).thenReturn(
        ImmutableList.of(FailAuthenticator.class, AppEngineAuthenticator.class));
    assertEquals(AppEngineAuthenticator.USER, auth.authenticate());
    assertEquals(AppEngineAuthenticator.APP_ENGINE_USER,
        attr.get(Attribute.AUTHENTICATED_APPENGINE_USER));
    assertEquals(AppEngineAuthenticator.APP_ENGINE_USER, auth.authenticateAppEngineUser());
  }

  @Test
  public void testAuthenticate_appEngineUser_CustomAuth() throws Exception {
    when(config.getAuthenticators())
        .thenReturn(ImmutableList.of(FailAuthenticator.class, PassAuthenticator.class));
    assertEquals(PassAuthenticator.USER, auth.authenticate());
    assertNull(attr.get(Attribute.AUTHENTICATED_APPENGINE_USER));
    assertEquals(AppEngineAuthenticator.APP_ENGINE_USER, auth.authenticateAppEngineUser());
  }
}
