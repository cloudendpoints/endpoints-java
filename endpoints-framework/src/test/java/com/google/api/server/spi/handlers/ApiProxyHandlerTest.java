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
package com.google.api.server.spi.handlers;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.EndpointsContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for {@link ApiProxyHandler}.
 */
@RunWith(JUnit4.class)
public class ApiProxyHandlerTest {
  @Test
  public void handle() throws Exception {
    testWithServletPath("/_ah/api");
  }

  @Test
  public void handleNonDefaultPath() throws Exception {
    testWithServletPath("/api");
  }

  private void testWithServletPath(String servletPath) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServerName("localhost");
    request.setServerPort(8080);
    request.setServletPath(servletPath);
    MockHttpServletResponse response = new MockHttpServletResponse();
    ApiProxyHandler handler = new ApiProxyHandler();
    EndpointsContext context = new EndpointsContext("GET", "static/proxy.html", request, response);

    handler.handle(context);

    assertThat(response.getContentAsString()).contains("googleapis.server.init()");
  }
}
