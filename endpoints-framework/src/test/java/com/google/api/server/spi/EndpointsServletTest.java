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

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.common.base.Splitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for {@link EndpointsServlet}.
 */
@RunWith(JUnit4.class)
public class EndpointsServletTest {
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();
  private static final String API_SERVER_NAME = "localhost";
  private static final int API_PORT = 9090;
  private static final String API_HOST = API_SERVER_NAME + ":" + API_PORT;
  private static final String API_ROOT = "http://" + API_HOST + "/_ah/api";
  MockHttpServletRequest req;
  MockHttpServletResponse resp;
  EndpointsServlet servlet;

  @Before
  public void setUp() throws ServletException {
    req = new MockHttpServletRequest();
    req.setServletPath("/_ah/api");
    req.addHeader("Host", API_SERVER_NAME);
    req.setServerName(API_SERVER_NAME);
    req.setServerPort(API_PORT);
    resp = new MockHttpServletResponse();
    servlet = new EndpointsServlet();
    MockServletConfig config = new MockServletConfig();
    config.addInitParameter("services", TestApi.class.getName());
    servlet.init(config);
  }

  @Test
  public void explorer() throws IOException {
    req.setRequestURI("/_ah/api/explorer/");
    req.setMethod("GET");

    servlet.service(req, resp);

    assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
    assertThat(resp.getHeader("Location")).isEqualTo(
        "https://developers.google.com/apis-explorer/?base=" + API_ROOT + "&root=" + API_ROOT);
  }

  @Test
  public void notFound() throws IOException {
    req.setRequestURI("/_ah/api/notfound");
    req.setMethod("GET");

    servlet.service(req, resp);

    assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void empty() throws IOException {
    req.setRequestURI("/_ah/api/test/v2/empty");
    req.setMethod("GET");

    servlet.service(req, resp);

    assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
  }

  @Test
  public void echo() throws IOException {
    req.setRequestURI("/_ah/api/test/v2/echo");
    req.setMethod("POST");
    req.setParameter("x", "1");

    servlet.service(req, resp);

    assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    ObjectNode actual = mapper.readValue(resp.getContentAsString(), ObjectNode.class);
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get("x").asInt()).isEqualTo(1);
  }

  @Test
  public void contentLengthHeaderNull() throws IOException {
    req.setRequestURI("/_ah/api/test/v2/echo");
    req.setMethod("POST");
    req.setParameter("x", "1");

    servlet.service(req, resp);

    assertThat(resp.getHeader("Content-Length")).isNull();
  }

  @Test
  public void contentLengthHeaderPresent() throws IOException, ServletException {
    MockServletConfig config = new MockServletConfig();
    config.addInitParameter("services", TestApi.class.getName());
    config.addInitParameter("addContentLength", "true");
    servlet.init(config);

    req.setRequestURI("/_ah/api/test/v2/echo");
    req.setMethod("POST");
    req.setParameter("x", "1");

    servlet.service(req, resp);

    assertThat(resp.getHeader("Content-Length")).isNotNull();
  }

  @Test
  public void methodOverride() throws IOException {
    req.setRequestURI("/_ah/api/test/v2/increment");
    req.setMethod("POST");
    req.addHeader("X-HTTP-Method-Override", "PATCH");
    req.setParameter("x", "1");

    servlet.service(req, resp);

    assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    ObjectNode actual = mapper.readValue(resp.getContentAsString(), ObjectNode.class);
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get("x").asInt()).isEqualTo(2);
  }

  @Test
  public void proxy() throws IOException {
    req.setRequestURI("/_ah/api/static/proxy.html");
    req.setMethod("GET");

    servlet.service(req, resp);

    assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(resp.getContentAsString()).contains("googleapis.server.init()");
  }

  @Test
  public void cors() throws IOException {
    req.setRequestURI("/does/not/matter");
    req.setMethod("OPTIONS");
    req.addHeader("Access-Control-Request-Method", "DELETE");
    req.addHeader("Access-Control-Request-Headers", "Test-Header");
    req.addHeader("Origin", "http://test.com");

    servlet.service(req, resp);

    assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(resp.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://test.com");
    assertThat(resp.getHeader("Access-Control-Allow-Headers")).isEqualTo("Test-Header");
    assertThat(resp.getHeader("Access-Control-Max-Age")).isEqualTo("3600");
    assertThat(resp.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    assertThat(COMMA_SPLITTER.split(resp.getHeader("Access-Control-Allow-Methods")))
        .containsExactly("HEAD", "DELETE", "GET", "PATCH", "POST", "PUT");
  }

  public static class TestResource {
    public int x;
  }

  @Api(name = "test", version = "v2")
  public static class TestApi {
    @ApiMethod(httpMethod = HttpMethod.GET)
    public void empty() {}

    @ApiMethod(httpMethod = HttpMethod.POST)
    public TestResource echo(TestResource r) {
      return r;
    }

    @ApiMethod(httpMethod = "PATCH")
    public TestResource increment(TestResource r) {
      r.x = r.x + 1;
      return r;
    }
  }
}
