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
import com.google.api.server.spi.EndpointsContext;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.types.SimpleDate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
  private ApiConfig apiConfig;
  private ApiMethodConfig methodConfig;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    endpointMethod = EndpointMethod.create(TestApi.class,
        TestApi.class.getMethod("test", Long.TYPE, List.class, SimpleDate.class,
            TestResource.class));
    request = new MockHttpServletRequest();
    ServiceContext serviceContext = ServiceContext.create();
    serializationConfig = new ApiSerializationConfig();
    TypeLoader typeLoader = new TypeLoader();
    apiConfig = new ApiConfig.Factory().create(serviceContext, typeLoader, TestApi.class);
    ApiConfigAnnotationReader annotationReader = new ApiConfigAnnotationReader();
    annotationReader.loadEndpointClass(serviceContext, TestApi.class, apiConfig);
    annotationReader.loadEndpointMethods(serviceContext, TestApi.class,
        apiConfig.getApiClassConfig().getMethods());
    methodConfig = apiConfig.getApiClassConfig().getMethods().get(endpointMethod);
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
  public void parseIntegerError() throws ServiceException {
    request.setContent("{\"objInt\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid number");
    reader.read();
  }

  @Test
  public void parseIntError() throws ServiceException {
    request.setContent("{\"simpleInt\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid number value \"invalid\"");
    reader.read();
  }

  @Test
  public void parseLongError() throws ServiceException {
    request.setContent("{\"objLong\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid number");
    reader.read();
  }

  @Test
  public void parsePrimitiveLongError() throws ServiceException {
    request.setContent("{\"simpleLong\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid number value \"invalid\"");
    reader.read();
  }

  @Test
  public void parseFloatError() throws ServiceException {
    request.setContent("{\"objFloat\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid number");
    reader.read();
  }

  @Test
  public void parsePrimitiveFloatError() throws ServiceException {
    request.setContent("{\"simpleFloat\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid number value \"invalid\"");
    reader.read();
  }

  @Test
  public void parseDoubleError() throws ServiceException {
    request.setContent("{\"objDouble\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid number");
    reader.read();
  }

  @Test
  public void parsePrimitiveDoubleError() throws ServiceException {
    request.setContent("{\"simpleDouble\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid number value \"invalid\"");
    reader.read();
  }

  @Test
  public void parseBooleanError() throws ServiceException {
    request.setContent("{\"objBoolean\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid boolean value");
    reader.read();
  }

  @Test
  public void parsePrimitiveBooleanError() throws ServiceException {
    request.setContent("{\"simpleBoolean\":\"invalid\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid boolean value \"invalid\"");
    reader.read();
  }

  @Test
  public void parseEnumError() throws ServiceException {
    request.setContent("{\"simpleEnum\":\"invalidEnum\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid enum value \"invalidEnum\". Valid values are [One, Two, Three]");
    reader.read();
  }
  
  @Test
  public void parseDateError() throws ServiceException {
    request.setContent("{\"objDate\":\"invalidDate\"}".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Invalid date value \"invalidDate\".");
    reader.read();
  }
  
  @Test
  public void parseError() throws ServiceException {
    request.setContent("{\"field\": \"this is an invalid json".getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("path", "1234"));
  
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("Parse error");
    reader.read();
  
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

  @Test
  public void arrayPathParam() throws Exception {
    endpointMethod = EndpointMethod.create(TestApi.class,
        TestApi.class.getMethod("testArrayPathParam", ArrayList.class));
    methodConfig = apiConfig.getApiClassConfig().getMethods().get(endpointMethod);
    RestServletRequestParamReader reader = createReader(ImmutableMap.of("values", "4,3,2,1"));

    Object[] params = reader.read();

    assertThat(params).hasLength(endpointMethod.getParameterClasses().length);
    assertThat(params).asList()
        .containsExactly(ImmutableList.of("4", "3", "2", "1"));
  }

  @Test
  public void multipartFormData() throws Exception {
    endpointMethod = EndpointMethod.create(TestApi.class,
        TestApi.class.getMethod("testFormData", String.class, Integer.class));
    methodConfig = apiConfig.getApiClassConfig().getMethods().get(endpointMethod);
    request.setContentType("multipart/form-data; boundary=----test");
    request.setMethod("POST");
    String requestContent =
        "------test\r\n" +
        "Content-Disposition: form-data; name=\"foo\"\r\n\r\n" +
        "test\r\n" +
        "------test\r\n" +
        "Content-Disposition: form-data; name=\"bar\"\r\n\r\n" +
        "1234\r\n" +
        "------test--\r\n";
    request.setContent(requestContent.getBytes(StandardCharsets.UTF_8));
    RestServletRequestParamReader reader = createReader(ImmutableMap.<String, String>of());

    Object[] params = reader.read();

    assertThat(params).hasLength(endpointMethod.getParameterClasses().length);
    assertThat(params).asList()
        .containsExactly("test", 1234);
  }

  private RestServletRequestParamReader createReader(Map<String, String> rawPathParameters) {
    EndpointsContext endpointsContext =
        new EndpointsContext("GET", "/", request, new MockHttpServletResponse(), true);
    endpointsContext.setRawPathParameters(rawPathParameters);
    return new RestServletRequestParamReader(endpointMethod, endpointsContext, null,
        serializationConfig, methodConfig);
  }

  public enum TestEnum {
    One, Two, Three
  }
  
  public static class TestResource {
    public SimpleDate query;
    
    public int simpleInt;
    public long simpleLong;
    public float simpleFloat;
    public double simpleDouble;
    public boolean simpleBoolean;
    
    public Integer objInt;
    public Long objLong;
    public Float objFloat;
    public Double objDouble;
    public Boolean objBoolean;
    
    public Date objDate;
    
    public TestEnum simpleEnum;
    
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
    public void test(
        @Nullable @Named("path") long path,
        @Nullable @Named("dates") List<SimpleDate> dates,
        @Named("defaultvalue") @DefaultValue("2015-01-01") SimpleDate defaultValue,
        TestResource resource) {
    }

    @ApiMethod(
        name = "testArrayPathParam",
        httpMethod = HttpMethod.GET,
        path = "testArrayPathParam/{values}")
    public void testArrayPathParam(@Named("values") ArrayList<String> values) {
    }

    @ApiMethod(
        name = "testFormData",
        httpMethod = HttpMethod.POST,
        path = "testFormData")
    public void testFormData(
        @Nullable @Named("foo") String foo,
        @Nullable @Named("bar") Integer bar) {
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
