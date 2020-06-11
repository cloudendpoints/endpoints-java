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

import static com.google.api.server.spi.config.model.EndpointsFlag.MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA;
import static com.google.api.server.spi.config.model.EndpointsFlag.MAP_SCHEMA_IGNORE_UNSUPPORTED_KEY_TYPES;
import static com.google.api.server.spi.config.model.EndpointsFlag.MAP_SCHEMA_SUPPORT_ARRAYS_VALUES;
import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.swagger.SwaggerGenerator.SwaggerContext;
import com.google.api.server.spi.testing.AbsoluteCommonPathEndpoint;
import com.google.api.server.spi.testing.AbsolutePathEndpoint;
import com.google.api.server.spi.testing.ArrayEndpoint;
import com.google.api.server.spi.testing.EnumEndpoint;
import com.google.api.server.spi.testing.FooCommonParamsEndpoint;
import com.google.api.server.spi.testing.FooDescriptionEndpoint;
import com.google.api.server.spi.testing.FooEndpoint;
import com.google.api.server.spi.testing.LimitMetricsEndpoint;
import com.google.api.server.spi.testing.MapEndpoint;
import com.google.api.server.spi.testing.MapEndpointInvalid;
import com.google.api.server.spi.testing.MultiResourceEndpoint.NoResourceEndpoint;
import com.google.api.server.spi.testing.MultiResourceEndpoint.Resource1Endpoint;
import com.google.api.server.spi.testing.MultiResourceEndpoint.Resource2Endpoint;
import com.google.api.server.spi.testing.MultiVersionEndpoint.Version1Endpoint;
import com.google.api.server.spi.testing.MultiVersionEndpoint.Version2Endpoint;
import com.google.api.server.spi.testing.RequiredPropertiesEndpoint;
import com.google.api.server.spi.testing.SpecialCharsEndpoint;
import com.google.api.server.spi.testing.ResponseStatusEndpoint;
import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
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
      .setScheme("https")
      .setHostname("swagger-test.appspot.com")
      .setBasePath("/api");
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
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context);
    Swagger expected = readExpectedAsSwagger("foo_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpointCustomTemplates() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), FooEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), new SwaggerContext()
        .setTagTemplate("${ApiName}${ApiVersion}")
        .setOperationIdTemplate("${apiName}-${apiVersion}-${method}")
    );
    Swagger expected = readExpectedAsSwagger("foo_endpoint_custom_templates.swagger");
    checkSwagger(expected, swagger);
  }
  
  @Test
  public void testWriteSwagger_FooEndpointParameterCombineParamSamePath() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), 
        FooCommonParamsEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context
      .setCombineCommonParametersInSamePath(true));
    Swagger expected = readExpectedAsSwagger("foo_endpoint_combine_params_same_path.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpointParameterExtractParamRef() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), 
        FooCommonParamsEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context
        .setExtractCommonParametersAsRefs(true));
    Swagger expected = readExpectedAsSwagger("foo_endpoint_extract_param_refs.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpointParameterCombineAllParam() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), 
        FooCommonParamsEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context
        .setExtractCommonParametersAsRefs(true)
        .setCombineCommonParametersInSamePath(true));
    Swagger expected = readExpectedAsSwagger("foo_endpoint_combine_all_params.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpointDefaultContext() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), FooEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), new SwaggerContext());
    Swagger expected = readExpectedAsSwagger("foo_endpoint_default_context.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpointWithApiName() throws Exception {
    Swagger swagger = getSwagger(
        FooEndpoint.class, new SwaggerContext().setApiName("customApiName"));
    Swagger expected = readExpectedAsSwagger("foo_endpoint_api_name.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_EnumEndpoint() throws Exception {
    Swagger swagger = getSwagger(EnumEndpoint.class, new SwaggerContext());
    Swagger expected = readExpectedAsSwagger("enum_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_ArrayEndpoint() throws Exception {
    Swagger swagger = getSwagger(ArrayEndpoint.class, new SwaggerContext());
    Swagger expected = readExpectedAsSwagger("array_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_MapEndpoint() throws Exception {
    Swagger swagger = getSwagger(MapEndpoint.class, new SwaggerContext());
    Swagger expected = readExpectedAsSwagger("map_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_MapEndpoint_Legacy() throws Exception {
    System.setProperty(MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA.systemPropertyName, "");
    try {
      Swagger swagger = getSwagger(MapEndpoint.class, new SwaggerContext());
      Swagger expected = readExpectedAsSwagger("map_endpoint_legacy.swagger");
      checkSwagger(expected, swagger);
    } finally {
      System.clearProperty(MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA.systemPropertyName);
    }
  }

  @Test
  public void testWriteDiscovery_MapEndpoint_InvalidKeyType() throws Exception {
    try {
      getSwagger(MapEndpointInvalid.class, new SwaggerContext());
      Assert.fail("Should have failed to generate schema for invalid key type");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testWriteDiscovery_MapEndpoint_InvalidKeyType_ignore() throws Exception {
    System.setProperty(MAP_SCHEMA_IGNORE_UNSUPPORTED_KEY_TYPES.systemPropertyName, "true");
    try {
      getSwagger(MapEndpointInvalid.class, new SwaggerContext());
    } finally {
      System.clearProperty(MAP_SCHEMA_IGNORE_UNSUPPORTED_KEY_TYPES.systemPropertyName);
    }
  }

  @Test
  public void testWriteSwagger_MapEndpoint_WithArrayValue() throws Exception {
    System.setProperty(MAP_SCHEMA_SUPPORT_ARRAYS_VALUES.systemPropertyName, "TRUE");
    try {
      Swagger swagger = getSwagger(MapEndpoint.class, new SwaggerContext());
      Swagger expected = readExpectedAsSwagger("map_endpoint_with_array.swagger");
      checkSwagger(expected, swagger);
    } finally {
      System.clearProperty(MAP_SCHEMA_SUPPORT_ARRAYS_VALUES.systemPropertyName);
    }
  }

  @Test
  public void testWriteSwagger_ThirdPartyAuthEndpoint() throws Exception {
    ApiConfig config =
        configLoader.loadConfiguration(ServiceContext.create(), ThirdPartyAuthEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context);
    Swagger expected = readExpectedAsSwagger("third_party_auth.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_GoogleAuthEndpoint() throws Exception {
    ApiConfig config =
        configLoader.loadConfiguration(ServiceContext.create(), GoogleAuthEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context);
    Swagger expected = readExpectedAsSwagger("google_auth.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_MultipleScopes() throws Exception {
    ApiConfig config =
        configLoader.loadConfiguration(ServiceContext.create(), MultipleScopesEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context);
    Swagger expected = readExpectedAsSwagger("multiple_scopes.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_ApiKeys() throws Exception {
    ApiConfig config =
        configLoader.loadConfiguration(ServiceContext.create(), ApiKeysEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context);
    Swagger expected = readExpectedAsSwagger("api_keys.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_AbsolutePathEndpoint() throws Exception {
    Swagger swagger = getSwagger(AbsolutePathEndpoint.class, new SwaggerContext());
    Swagger expected = readExpectedAsSwagger("absolute_path_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_AbsoluteCommonPathEndpoint() throws Exception {
    Swagger swagger = getSwagger(AbsoluteCommonPathEndpoint.class, new SwaggerContext());
    Swagger expected = readExpectedAsSwagger("absolute_common_path_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_LimitMetricsEndpoint() throws Exception {
    Swagger swagger = getSwagger(LimitMetricsEndpoint.class, new SwaggerContext());
    Swagger expected = readExpectedAsSwagger("limit_metrics_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_FooEndpointWithDescription() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), FooDescriptionEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context);
    Swagger expected = readExpectedAsSwagger("foo_with_description_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_RequiredPropertiesEndpoint() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), RequiredPropertiesEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context);
    Swagger expected = readExpectedAsSwagger("required_parameters_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_MultiResourceEndpoint() throws Exception {
    ServiceContext serviceContext = ServiceContext.create();
    ImmutableList<ApiConfig> configs = ImmutableList.of(
        configLoader.loadConfiguration(serviceContext, NoResourceEndpoint.class), 
        configLoader.loadConfiguration(serviceContext, Resource1Endpoint.class), 
        configLoader.loadConfiguration(serviceContext, Resource2Endpoint.class));
    Swagger swagger = generator.writeSwagger(configs, context);
    Swagger expected = readExpectedAsSwagger("multi_resource_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_MultiVersionEndpoint() throws Exception {
    ServiceContext serviceContext = ServiceContext.create();
    ImmutableList<ApiConfig> configs = ImmutableList.of(
        configLoader.loadConfiguration(serviceContext, Version1Endpoint.class),
        configLoader.loadConfiguration(serviceContext, Version2Endpoint.class));
    Swagger swagger = generator.writeSwagger(configs, context);
    Swagger expected = readExpectedAsSwagger("multi_version_endpoint.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_ErrorAsDefaultResponse() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), ExceptionEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context
        .setAddGoogleJsonErrorAsDefaultResponse(true));
    Swagger expected = readExpectedAsSwagger("error_codes_default_response.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_ServiceExceptionErrorCodes() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), ExceptionEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context
        .setAddErrorCodesForServiceExceptions(true));
    Swagger expected = readExpectedAsSwagger("error_codes_service_exceptions.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_ResponseStatus() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), ResponseStatusEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context);
    Swagger expected = readExpectedAsSwagger("response_status.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testWriteSwagger_AllErrors() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), ExceptionEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context
        .setAddGoogleJsonErrorAsDefaultResponse(true)
        .setAddErrorCodesForServiceExceptions(true));
    Swagger expected = readExpectedAsSwagger("error_codes_all.swagger");
    checkSwagger(expected, swagger);
  }

  @Test
  public void testEquivalentPathsNotAccepted() {
    try {
      ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), EquivalentPathsEndpoint.class);
      generator.writeSwagger(ImmutableList.of(config), context);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e).hasMessageThat().contains("Equivalent paths found");
    }
  }

  @Test
  public void testWriteSwagger_SpecialChars() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), SpecialCharsEndpoint.class);
    Swagger swagger = generator.writeSwagger(ImmutableList.of(config), context
        .setExtractCommonParametersAsRefs(true));
    Swagger expected = readExpectedAsSwagger("special_chars.swagger");
    checkSwagger(expected, swagger);
  }

  private Swagger getSwagger(Class<?> serviceClass, SwaggerContext context)
      throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), serviceClass);
    return generator.writeSwagger(ImmutableList.of(config), context);
  }

  private Swagger readExpectedAsSwagger(String file) throws Exception {
    String expectedString = IoUtil.readResourceFile(SwaggerGeneratorTest.class, file);
    return mapper.readValue(expectedString, Swagger.class);
  }

  private void checkSwagger(Swagger expected, Swagger actual) {
    SwaggerSubject.assertThat(actual).isValid();
    SwaggerSubject.assertThat(actual).isSameAs(expected);
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
            @ApiIssuerAudience(name = Constant.GOOGLE_ID_TOKEN_ALT, audiences = "googleaud")
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

  @Api(name = "multipleScopes",
      version = "v1",
      audiences = {"audience"},
      scopes = "https://mail.google.com/")
  private static class MultipleScopesEndpoint {
    @ApiMethod
    public void noOverride() { }
    @ApiMethod(scopes = Constant.API_EMAIL_SCOPE)
    public void scopeOverride() { }
    @ApiMethod(scopes = "unknownScope")
    public void unknownScope() { }
    @ApiMethod(audiences = {"audience2"})
    public void overrideAudience() { }
  }

  @Api(name = "exceptions", version = "v1")
  private static class ExceptionEndpoint {
    @ApiMethod
    public void doesNotThrow() { }

    @ApiMethod
    public void throwsServiceException() throws ServiceException { }

    @ApiMethod
    public void throwsNotFoundException() throws NotFoundException { }

    @ApiMethod
    public void throwsMultipleExceptions() throws BadRequestException, ConflictException { }

    @ApiMethod
    public void throwsUnknownException() throws IllegalStateException { }
  }
  
  @Api(name = "equivalentPaths", version = "v1")
  private static class EquivalentPathsEndpoint {
    @ApiMethod(path = "foo/{id}", httpMethod = HttpMethod.GET)
    public void path1(@Named("id") String id) { }

    @ApiMethod(path = "foo/{fooId}", httpMethod = HttpMethod.POST)
    public void path2(@Named("fooId") String fooId) { }
  }
  
}
