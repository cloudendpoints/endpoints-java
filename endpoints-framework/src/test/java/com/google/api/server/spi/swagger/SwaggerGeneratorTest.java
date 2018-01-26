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
package com.google.api.server.spi.swagger;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.swagger.SwaggerGenerator.SwaggerContext;
import com.google.api.server.spi.testing.AbsoluteCommonPathEndpoint;
import com.google.api.server.spi.testing.AbsolutePathEndpoint;
import com.google.api.server.spi.testing.ArrayEndpoint;
import com.google.api.server.spi.testing.EnumEndpoint;
import com.google.api.server.spi.testing.FooDescriptionEndpoint;
import com.google.api.server.spi.testing.FooEndpoint;
import com.google.api.server.spi.testing.LimitMetricsEndpoint;
import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.swagger.models.Swagger;
import io.swagger.util.Json;

/**
 * Tests for {@link SwaggerGenerator}.
 */
@RunWith(JUnit4.class)
public class SwaggerGeneratorTest {
  private final SwaggerGenerator generator = new SwaggerGenerator();
  private final SwaggerContext context = new SwaggerContext()
      .setApiRoot("https://swagger-test.appspot.com/api");
  private final ObjectMapper mapper = Json.mapper();
  private ApiConfigLoader configLoader;

  @Before
  public void setUp() throws Exception {
    TypeLoader typeLoader = new TypeLoader(getClass().getClassLoader());
    ApiConfigAnnotationReader annotationReader =
        new ApiConfigAnnotationReader(typeLoader.getAnnotationTypes());
    this.configLoader = new ApiConfigLoader(new ApiConfig.Factory(), typeLoader,
        annotationReader);
  }

  @Test
  public void testWriteSwagger_FooEndpoint() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), FooEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), false, context);
    Swagger expected = readExpectedAsSwagger("foo_endpoint.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpointDefaultContext() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), FooEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), false, new SwaggerContext());
    Swagger expected = readExpectedAsSwagger("foo_endpoint_default_context.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpointLocalhost() throws Exception {
    Swagger swagger = getSwagger(
        FooEndpoint.class, new SwaggerContext().setApiRoot("http://localhost:8080/api"), false);
    Swagger expected = readExpectedAsSwagger("foo_endpoint_localhost.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_EnumEndpoint() throws Exception {
    Swagger swagger = getSwagger(EnumEndpoint.class, new SwaggerContext(), true);
    Swagger expected = readExpectedAsSwagger("enum_endpoint.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_ArrayEndpoint() throws Exception {
    Swagger swagger = getSwagger(ArrayEndpoint.class, new SwaggerContext(), true);
    Swagger expected = readExpectedAsSwagger("array_endpoint.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpoint_internal() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), FooEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), true, context);
    Swagger expected = readExpectedAsSwagger("foo_endpoint_internal.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_ThirdPartyAuthEndpoint() throws Exception {
    ApiConfig config =
        configLoader.loadConfiguration(ServiceContext.create(), ThirdPartyAuthEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), true, context);
    Swagger expected = readExpectedAsSwagger("third_party_auth.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_GoogleAuthEndpoint() throws Exception {
    ApiConfig config =
        configLoader.loadConfiguration(ServiceContext.create(), GoogleAuthEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), true, context);
    Swagger expected = readExpectedAsSwagger("google_auth.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_ApiKeys() throws Exception {
    ApiConfig config =
        configLoader.loadConfiguration(ServiceContext.create(), ApiKeysEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), true, context);
    Swagger expected = readExpectedAsSwagger("api_keys.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_AbsolutePathEndpoint() throws Exception {
    Swagger swagger = getSwagger(AbsolutePathEndpoint.class, new SwaggerContext(), true);
    Swagger expected = readExpectedAsSwagger("absolute_path_endpoint.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_AbsoluteCommonPathEndpoint() throws Exception {
    Swagger swagger = getSwagger(AbsoluteCommonPathEndpoint.class, new SwaggerContext(), true);
    Swagger expected = readExpectedAsSwagger("absolute_common_path_endpoint.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_LimitMetricsEndpoint() throws Exception {
    Swagger swagger = getSwagger(LimitMetricsEndpoint.class, new SwaggerContext(), true);
    Swagger expected = readExpectedAsSwagger("limit_metrics_endpoint.swagger");
    compareSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpointWithDescription() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), FooDescriptionEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), false, context);
    Swagger expected = readExpectedAsSwagger("foo_with_description_endpoint.swagger");
    compareSwagger(expected, swagger);
  }

  private Swagger getSwagger(Class<?> serviceClass, SwaggerContext context, boolean internal)
      throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), serviceClass);
    return generator.writeSwagger(ImmutableList.of(config), internal, context);
  }

  private Swagger readExpectedAsSwagger(String file) throws Exception {
    String expectedString = IoUtil.readResourceFile(SwaggerGeneratorTest.class, file);
    return mapper.readValue(expectedString, Swagger.class);
  }

  private void compareSwagger(Swagger expected, Swagger actual) throws Exception {
    System.out.println("Actual: " + mapper.writeValueAsString(actual));
    System.out.println("Expected: " + mapper.writeValueAsString(expected));
    assertThat(actual).isEqualTo(expected);
    // TODO: Remove once Swagger models check this in equals
    assertThat(actual.getVendorExtensions()).isEqualTo(expected.getVendorExtensions());
  }

  @Api(name = "thirdparty", version = "v1",
      issuers = {
          @ApiIssuer(name = "auth0", issuer = "https://test.auth0.com/authorize",
              jwksUri = "https://test.auth0.com/.wellknown/jwks.json"),
          @ApiIssuer(name = "nojwks", issuer = "https://nojwks.com")
      },
      issuerAudiences = {
          @ApiIssuerAudience(name = "auth0", audiences = "auth0audapi")
      })
  private static class ThirdPartyAuthEndpoint {
    @ApiMethod(
        issuerAudiences = {
            @ApiIssuerAudience(name = "auth0", audiences = "auth0audmethod")
        }
    )
    public void authOverride() { }

    public void noOverride() { }
  }

  private static class GoogleAuthEndpoint extends ThirdPartyAuthEndpoint {
    @ApiMethod(
        issuerAudiences = {
            @ApiIssuerAudience(name = Constant.GOOGLE_ID_TOKEN_NAME, audiences = "googleaud")
        }
    )
    public void googleAuth() { }
  }

  @Api(name = "apikeys", version = "v1",
      issuers = {
          @ApiIssuer(name = "auth0", issuer = "https://test.auth0.com/authorize",
              jwksUri = "https://test.auth0.com/.wellknown/jwks.json")
      },
      apiKeyRequired = AnnotationBoolean.TRUE)
  private static class ApiKeysEndpoint {
    @ApiMethod(apiKeyRequired = AnnotationBoolean.FALSE)
    public void overrideApiKeySetting() { }

    @ApiMethod
    public void inheritApiKeySetting() { }

    @ApiMethod(
        issuerAudiences = {
            @ApiIssuerAudience(name = "auth0", audiences = "auth0audmethod")
        })
    public void apiKeyWithAuth() { }
  }
}
