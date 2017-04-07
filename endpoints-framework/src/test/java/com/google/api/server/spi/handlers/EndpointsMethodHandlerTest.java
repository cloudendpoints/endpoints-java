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

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.EndpointsContext;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.ServletInitializationParameters;
import com.google.api.server.spi.SystemService;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.request.FakeParamReader;
import com.google.api.server.spi.request.ParamReader;
import com.google.api.server.spi.response.ErrorResultWriter;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.ResultWriter;
import com.google.api.server.spi.response.SuccessResultWriter;
import com.google.api.server.spi.testing.ArrayEndpoint;
import com.google.common.annotations.VisibleForTesting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests for {@link EndpointsMethodHandler}.
 */
@RunWith(JUnit4.class)
public class EndpointsMethodHandlerTest {
  private static final TestResource RESOURCE = new TestResource(1234);

  private ClassLoader classLoader;
  private SystemService systemService;
  private ApiConfig apiConfig;
  private TypeLoader typeLoader;
  private EndpointsContext context;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @Before
  public void setUp() throws Exception {
    classLoader = EndpointsMethodHandlerTest.class.getClassLoader();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    context = new EndpointsContext("", "", request, response, true);
    systemService = SystemService.builder()
        .withDefaults(classLoader)
        .addService(TestEndpoint.class, new TestEndpoint())
        .build();
    typeLoader = new TypeLoader(classLoader);
    apiConfig = new ApiConfig.Factory()
        .create(ServiceContext.create(), typeLoader, TestEndpoint.class);
  }

  @Test
  public void simple() throws Exception {
    TestMethodHandler handler = createTestHandler("simple", RESOURCE, RESOURCE);
    handler.getRestHandler().handle(context);
  }

  @Test
  public void simple_withCors() throws Exception {
    TestMethodHandler handler = createTestHandler("simple", RESOURCE, RESOURCE);
    request.addHeader("Origin", "http://test.com");
    handler.getRestHandler().handle(context);
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://test.com");
    assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
  }

  @Test
  public void fail() throws Exception {
    TestMethodHandler handler = createTestHandler("fail", 404);
    handler.getRestHandler().handle(context);
  }

  @Test
  public void fail_findService() throws Exception {
    EndpointMethod method = systemService.resolveService("TestEndpoint", "simple");
    ApiMethodConfig methodConfig = new ApiMethodConfig(method, typeLoader,
        apiConfig.getApiClassConfig());
    systemService = SystemService.builder()
        .withDefaults(classLoader)
        .addService(ArrayEndpoint.class, new ArrayEndpoint())
        .build();
    TestMethodHandler handler = new TestMethodHandler(
        ServletInitializationParameters.builder().build(), method,
        apiConfig, methodConfig, systemService, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        RESOURCE);
    handler.getRestHandler().handle(context);
  }

  @Test
  public void rootMethodHandler() throws Exception {
    EndpointMethod method = systemService.resolveService("TestEndpoint", "root");
    ApiMethodConfig methodConfig = new ApiMethodConfig(method, typeLoader,
        apiConfig.getApiClassConfig());
    methodConfig.setPath("/root");
    TestMethodHandler handler = new TestMethodHandler(
        ServletInitializationParameters.builder().build(), method, apiConfig, methodConfig,
        systemService, 200);
    assertThat(handler.getRestPath()).isEqualTo("root");
  }

  private TestMethodHandler createTestHandler(String methodName, Object expectedResponse,
      Object... params) throws Exception {
    EndpointMethod method = systemService.resolveService("TestEndpoint", methodName);
    ApiMethodConfig methodConfig = new ApiMethodConfig(method, typeLoader,
        apiConfig.getApiClassConfig());
    return new TestMethodHandler(ServletInitializationParameters.builder().build(), method,
        apiConfig, methodConfig, systemService, expectedResponse, params);
  }

  private static class TestMethodHandler extends EndpointsMethodHandler {
    private final Object[] params;
    private final Object expectedResult;
    public TestMethodHandler(
        ServletInitializationParameters initParameters,
        EndpointMethod endpointMethod,
        ApiConfig apiConfig,
        ApiMethodConfig methodConfig,
        SystemService systemService,
        Object expectedResult,
        Object... params) {
      super(initParameters, null /* servletContext */, endpointMethod, apiConfig, methodConfig,
          systemService);
      this.params = params;
      this.expectedResult = expectedResult;
    }

    @Override
    @VisibleForTesting
    protected ParamReader createRestParamReader(EndpointsContext context,
        ApiSerializationConfig serializationConfig) {
      return new FakeParamReader(params);
    }

    @Override
    @VisibleForTesting
    protected ResultWriter createResultWriter(EndpointsContext context,
        ApiSerializationConfig serializationConfig) {
      if (expectedResult instanceof Integer) {
        return new ErrorResultWriter((Integer) expectedResult);
      }
      return new SuccessResultWriter(expectedResult);
    }
  }

  private static class TestResource {
    public int x;

    TestResource(int x) {
      this.x = x;
    }
  }

  @Api
  public static class TestEndpoint {
    public TestResource simple(TestResource resource) {
      return resource;
    }

    public TestResource fail() throws NotFoundException {
      throw new NotFoundException("");
    }

    @ApiMethod(path = "/root")
    public void root() { }
  }
}
