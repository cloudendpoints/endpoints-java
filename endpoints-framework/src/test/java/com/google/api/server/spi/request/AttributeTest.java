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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.EndpointsServlet;
import com.google.api.server.spi.ServletInitializationParameters;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Tests for {@link Attribute}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AttributeTest {
  @Mock protected ServletConfig servletConfig;
  @Mock protected ServletContext servletContext;
  @Mock protected ApiMethodConfig methodConfig;

  private MockHttpServletRequest request;

  @Before
  public void setUp() {
    request = new MockHttpServletRequest();
  }

  @Test
  public void bindStandardRequestAttributes_restricted() throws Exception {
    when(methodConfig.getClientIds()).thenReturn(null);

    ServletInitializationParameters initParams =
        createInitParams(true /* restricted */, true /* clientIdWhitelistEnabled */);
    Attribute attr = Attribute.bindStandardRequestAttributes(request, methodConfig, initParams);
    assertTrue(attr.isEnabled(Attribute.RESTRICT_SERVLET));

    initParams = createInitParams(false /* restricted */, true /* clientIdWhitelistEnabled */);
    attr = Attribute.bindStandardRequestAttributes(request, methodConfig, initParams);
    assertFalse(attr.isEnabled(Attribute.RESTRICT_SERVLET));
  }

  @Test
  public void bindStandardRequestAttributes_clientIdWhitelist() throws Exception {
    when(methodConfig.getClientIds()).thenReturn(null);

    ServletInitializationParameters initParams =
        createInitParams(true /* restricted */, true /* clientIdWhitelistEnabled */);
    Attribute attr = Attribute.bindStandardRequestAttributes(request, methodConfig, initParams);
    assertTrue(attr.isEnabled(Attribute.ENABLE_CLIENT_ID_WHITELIST));

    attr.remove(Attribute.ENABLE_CLIENT_ID_WHITELIST);
    initParams = createInitParams(true /* restricted */, false /* clientIdWhitelistEnabled */);
    attr = Attribute.bindStandardRequestAttributes(request, methodConfig, initParams);
    assertFalse(attr.isEnabled(Attribute.ENABLE_CLIENT_ID_WHITELIST));
  }

  @Test
  public void bindStandardRequestAttributes_skipTokenAuth() throws Exception {
    ServletInitializationParameters initParams =
        createInitParams(true /* restricted */, true /* clientIdWhitelistEnabled */);
    when(methodConfig.getClientIds()).thenReturn(null);
    Attribute attr = Attribute.bindStandardRequestAttributes(request, methodConfig, initParams);
    assertTrue(attr.isEnabled(Attribute.SKIP_TOKEN_AUTH));

    attr.remove(Attribute.SKIP_TOKEN_AUTH);
    initParams = createInitParams(true /* restricted */, true /* clientIdWhitelistEnabled */);
    when(methodConfig.getClientIds()).thenReturn(ImmutableList.of("clientId"));
    attr = Attribute.bindStandardRequestAttributes(request, methodConfig, initParams);
    assertFalse(attr.isEnabled(Attribute.SKIP_TOKEN_AUTH));

    attr.remove(Attribute.SKIP_TOKEN_AUTH);
    initParams = createInitParams(true /* restricted */, false /* clientIdWhitelistEnabled */);
    attr = Attribute.bindStandardRequestAttributes(request, methodConfig, initParams);
    assertFalse(attr.isEnabled(Attribute.SKIP_TOKEN_AUTH));
  }

  @Test
  public void bindStandardRequestAttributes_apiMethodConfig() throws Exception {
    when(methodConfig.getClientIds()).thenReturn(null);
    ServletInitializationParameters initParams =
        createInitParams(true /* restricted */, true /* clientIdWhitelistEnabled */);
    Attribute attr = Attribute.bindStandardRequestAttributes(request, methodConfig, initParams);
    assertEquals(attr.get(Attribute.API_METHOD_CONFIG), methodConfig);
  }

  /**
   * Sets up some expectations for {@link #servletConfig} when it is passed to
   * {@link EndpointsServlet#init(ServletConfig)}.
   *
   * @throws ServletException
   */
  protected ServletInitializationParameters createInitParams(boolean restricted,
      boolean clientIdWhitelistEnabled) throws Exception {
    return ServletInitializationParameters.builder()
        .setRestricted(restricted)
        .setClientIdWhitelistEnabled(clientIdWhitelistEnabled)
        .build();
  }
}
