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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests for {@link ExplorerHandler}.
 */
public class ExplorerHandlerTest {
  @Test
  public void testHandle() throws Exception {
    testHandle("http", 8080, "https://developers.google.com/apis-explorer/"
        + "?base=http://localhost:8080/_ah/api&root=http://localhost:8080/_ah/api");
  }

  @Test
  public void testHandle_explicitHttpPort() throws Exception {
    testHandle("http", 80, "https://developers.google.com/apis-explorer/"
        + "?base=http://localhost/_ah/api&root=http://localhost/_ah/api");
  }

  @Test
  public void testHandle_explicitHttpsPort() throws Exception {
    testHandle("https", 443, "https://developers.google.com/apis-explorer/"
        + "?base=https://localhost/_ah/api&root=https://localhost/_ah/api");
  }

  private void testHandle(String scheme, int port, String expectedLocation) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setScheme(scheme);
    request.setServerName("localhost");
    request.setServerPort(port);
    request.setRequestURI("/_ah/api/explorer/");
    MockHttpServletResponse response = new MockHttpServletResponse();
    ExplorerHandler handler = new ExplorerHandler();
    EndpointsContext context = new EndpointsContext("GET", "explorer", request, response, true);
    handler.handle(context);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
    assertThat(response.getHeader("Location")).isEqualTo(expectedLocation);
  }
}
