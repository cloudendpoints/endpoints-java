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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.EnvUtil;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.response.ServiceUnavailableException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Test for {@code EndpointsAuthenticator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EndpointsAuthenticatorTest {
  private static final User USER = new User("123", "dummy@gmail.com");

  @Mock private GoogleJwtAuthenticator jwtAuthenticator;
  @Mock private GoogleAppEngineAuthenticator appEngineAuthenticator;
  @Mock private GoogleOAuth2Authenticator oauth2Authenticator;

  private EndpointsAuthenticator authenticator;
  private MockHttpServletRequest request;
  private Attribute attr;

  @Before
  public void setUp() throws Exception {
    authenticator =
        new EndpointsAuthenticator(jwtAuthenticator, appEngineAuthenticator, oauth2Authenticator);
    request = new MockHttpServletRequest();
    attr = Attribute.from(request);
  }

  @Test
  public void testAuthenticate_jwt() throws ServiceUnavailableException {
    when(jwtAuthenticator.authenticate(request)).thenReturn(USER);
    assertEquals(USER, authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_appEngine() throws ServiceUnavailableException {
    when(jwtAuthenticator.authenticate(request)).thenReturn(null);
    when(appEngineAuthenticator.authenticate(request)).thenReturn(USER);

    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "Production");
    attr.set(Attribute.REQUIRE_APPENGINE_USER, true);

    assertEquals(USER, authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_oauth2NonAppEngine() throws ServiceUnavailableException {
    when(jwtAuthenticator.authenticate(request)).thenReturn(null);
    when(oauth2Authenticator.authenticate(request)).thenReturn(USER);

    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "");
    attr.set(Attribute.REQUIRE_APPENGINE_USER, true);

    assertEquals(USER, authenticator.authenticate(request));
  }

  @Test
  public void testAuthenticate_oAuth2NotRequireAppEngineUser() throws ServiceUnavailableException {
    when(jwtAuthenticator.authenticate(request)).thenReturn(null);
    when(oauth2Authenticator.authenticate(request)).thenReturn(USER);

    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "Production");
    attr.set(Attribute.REQUIRE_APPENGINE_USER, false);

    assertEquals(USER, authenticator.authenticate(request));
  }
}
