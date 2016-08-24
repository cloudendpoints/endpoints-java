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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.types.SimpleDate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

/**
 * Tests for {@link RestServletRequestParamReader}.
 */
@RunWith(JUnit4.class)
public class RestServletRequestParamReaderTest {
  public static final SimpleDate JAN_1 = new SimpleDate(2015, 1, 1);
  public static final SimpleDate NOV_2 = new SimpleDate(2015, 11, 2);
  public static final SimpleDate NOV_1 = new SimpleDate(2015, 11, 1);

  private EndpointMethod endpointMethod;
  private MockHttpServletRequest request;
  private ApiSerializationConfig serializationConfig;
  private ApiMethodConfig methodConfig;

  @Before
  public void setUp() throws Exception {
    endpointMethod = EndpointMethod.create(TestApi.class,
        TestApi.class.getMethod("test", Long.TYPE, List.class, SimpleDate.class,
            TestResource.class));
    request = new MockHttpServletRequest();
    ServiceContext serviceContext = ServiceContext.create();
    serializationConfig = new ApiSerializationConfig();
    TypeLoader typeLoader = new TypeLoader();
    ApiConfig config = (new ApiConfig.Factory()).create(serviceContext, typeLoader,
        TestApi.class);
    ApiConfigAnnotationReader annotationReader = new ApiConfigAnnotationReader();
    annotationReader.loadEndpointClass(serviceContext, TestApi.class, config);
    annotationReader.loadEndpointMethods(serviceContext, TestApi.class,
        config.getApiClassConfig().getMethods());
    methodConfig = config.getApiClassConfig().getMethods().get(endpointMethod);
  }

  @Test
  public void repeatedQueryParameter() throws Exception {
    request.addParameter("dates", NOV_1.toString());
    request.addParameter("dates", NOV_2.toString());
    request.addParameter("defaultvalue", NOV_2.toString());
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));

    Object[] params = reader.read();

    assertThat(params).hasLength(endpointMethod.getParameterClasses().length);
    assertThat(params).asList()
        .containsExactly(
            1234L,
            ImmutableList.of(NOV_1, NOV_2),
            NOV_2,
            new TestResource())
        .inOrder();
  }

  @Test
  public void defaultValue() throws Exception {
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));

    Object[] params = reader.read();

    assertThat(params).hasLength(endpointMethod.getParameterClasses().length);
    assertThat(params).asList()
        .containsExactly(
            1234L,
            null,
            JAN_1,
            new TestResource());
  }

  @Test
  public void resourceOverridesQuery() throws Exception {
    request.addParameter("query", NOV_1.toString());
    request.addParameter("defaultvalue", NOV_2.toString());
    request.setContent(
        String.format("{\"query\": \"%s\"}", NOV_2).getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));

    Object[] params = reader.read();

    assertThat(params).hasLength(endpointMethod.getParameterClasses().length);
    assertThat(params).asList()
        .containsExactly(
            1234L,
            null,
            NOV_2,
            new TestResource(NOV_2))
        .inOrder();
  }

  @Test
  public void queryOverridesPath() throws Exception {
    request.addParameter("path", "4321");
    request.addParameter("defaultvalue", NOV_2.toString());
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));

    Object[] params = reader.read();

    assertThat(params).hasLength(endpointMethod.getParameterClasses().length);
    assertThat(params).asList()
        .containsExactly(
            4321L,
            null,
            NOV_2,
            new TestResource())
        .inOrder();
  }

  @Test
  public void nonObjectRequest() throws Exception {
    try {
      request.setContent("\"a string\"".getBytes(StandardCharsets.UTF_8));
      RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));

      reader.read();
      fail("expected BadRequestException");
    } catch (BadRequestException e) {
      // expected
    }
  }

  @Test
  public void gzippedRequest() throws Exception {
    request.addParameter("path", "1234");
    request.setContent(compress(
        String.format("{\"query\": \"%s\"}", NOV_2).getBytes(StandardCharsets.UTF_8)));
    request.addHeader("Content-Encoding", "gzip");
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));

    Object[] params = reader.read();

    assertThat(params).hasLength(endpointMethod.getParameterClasses().length);
    assertThat(params).asList()
        .containsExactly(
            1234L,
            null,
            JAN_1,
            new TestResource(NOV_2))
        .inOrder();
  }

  private RestServletRequestParamReader createReader(Map<String, String> rawPathParameters) {
    return new RestServletRequestParamReader(endpointMethod, request, null,
        serializationConfig, methodConfig, rawPathParameters);
  }

  public static class TestResource {
    public SimpleDate query;

    public TestResource() {}

    public TestResource(SimpleDate query) {
      this.query = query;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof TestResource && Objects.equals(((TestResource) o).query, query);
    }
  }

  @Api
  public static class TestApi {
    @ApiMethod(name = "test", httpMethod = HttpMethod.GET, path = "test/{path}")
    public void test(@Named("path") long path, @Named("dates") List<SimpleDate> dates,
        @Named("defaultvalue") @DefaultValue("2015-01-01") SimpleDate defaultValue,
        TestResource resource) {
    }
  }

  private static byte[] compress(byte[] bytes) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gos = new GZIPOutputStream(baos);
      gos.write(bytes, 0, bytes.length);
      gos.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
