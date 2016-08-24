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

import com.google.common.base.Splitter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests for {@link CorsHandler}.
 */
@RunWith(JUnit4.class)
public class CorsHandlerTest {
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();
  public static final String ORIGIN = "http://test.com";
  public static final String TEST_HEADER = "Test-Header";
  public static final String VALID_METHOD = "DELETE";
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private CorsHandler handler = new CorsHandler();

  @Before
  public void setUp() {
    request = new MockHttpServletRequest("OPTIONS", "http://test.com/some/path");
    response = new MockHttpServletResponse();
  }

  @Test
  public void handle() {
    initializeValidRequest(request);

    handler.handle(request, response);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
    assertThat(response.getHeader("Access-Control-Allow-Headers")).isEqualTo(TEST_HEADER);
    assertThat(response.getHeader("Access-Control-Max-Age")).isEqualTo("3600");
    assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    assertThat(COMMA_SPLITTER.split(response.getHeader("Access-Control-Allow-Methods")))
        .containsExactly("HEAD", "DELETE", "GET", "PATCH", "POST", "PUT");
  }

  @Test
  public void handle_invalidMethod() {
    request.addHeader("Access-Control-Request-Method", "DEELEEETE");
    request.addHeader("Access-Control-Request-Headers", TEST_HEADER);
    request.addHeader("Origin", ORIGIN);

    handler.handle(request, response);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    assertThat(response.getHeader("Access-Control-Allow-Headers")).isNull();
    assertThat(response.getHeader("Access-Control-Max-Age")).isNull();
    assertThat(response.getHeader("Access-Control-Allow-Credentials")).isNull();
    assertThat(response.getHeader("Access-Control-Allow-Methods")).isNull();
  }

  @Test
  public void allowOrigin() {
    initializeValidRequest(request);
    CorsHandler.allowOrigin(request, response);
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
  }

  @Test
  public void setAccessControlAllowCredentials() {
    CorsHandler.setAccessControlAllowCredentials(response);
    assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
  }

  private static void initializeValidRequest(MockHttpServletRequest request) {
    request.addHeader("Access-Control-Request-Method", VALID_METHOD);
    request.addHeader("Access-Control-Request-Headers", TEST_HEADER);
    request.addHeader("Origin", ORIGIN);
  }
}
