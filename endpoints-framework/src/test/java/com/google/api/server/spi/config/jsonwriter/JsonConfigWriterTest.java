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
package com.google.api.server.spi.config.jsonwriter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.config.model.StandardParameters;
import com.google.api.server.spi.config.validation.ApiConfigValidator;
import com.google.api.server.spi.config.validation.InvalidReturnTypeException;
import com.google.api.server.spi.config.validation.PropertyParameterNameConflictException;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.testing.Bar;
import com.google.api.server.spi.testing.Baz;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;

/**
 * Tests for {@link JsonConfigWriter}.
 */
@RunWith(JUnit4.class)
public class JsonConfigWriterTest {
  private JsonConfigWriter writer;
  private ApiConfig.Factory configFactory;
  private ServiceContext serviceContext;
  private ApiConfig apiConfig;
  private ObjectMapper objectMapper;

  @Before
  public void init() throws Exception {
    writer = new JsonConfigWriter();
    configFactory = new ApiConfig.Factory();
    serviceContext = ServiceContext.create();
    apiConfig = configFactory.create(serviceContext, new TypeLoader(), TestEndpoint.class);
    objectMapper = ObjectMapperUtil.createStandardObjectMapper();
  }

  private enum Outcome {
    WON, LOST, TIE
  }

  static class Foo {}

  static class Qux<T> {}

  @Api
  static class TestEndpoint {
    @SuppressWarnings("unused")
    public Foo getItem(
        @Named("required") String required, @Nullable @Named("optional") String optional) {
      return null;
    }

    public List<Bar> list() {
      return null;
    }

    public CollectionResponse<Baz> listWithPagination() {
      return null;
    }

    private static class MyCollectionResponse<T> extends CollectionResponse<T> {
      protected MyCollectionResponse(Collection<T> items, String nextPageToken) {
        super(items, nextPageToken);
      }
    }

    public MyCollectionResponse<Foo> listWithMyCollectionResponse() {
      return null;
    }
  }

  @SuppressWarnings("rawtypes")
  static class ParameterizedTypeTransformer implements Transformer<Qux, String> {
    @Override
    public String transformTo(Qux in) {
      return "qux";
    }
    @Override
    public Qux transformFrom(String in) {
      return new Qux<String>();
    }
  }

  @Test
  public void testEnumType() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode schemasConfig = mapper.createObjectNode();
    writer.addTypeToSchema(schemasConfig, TypeToken.of(Outcome.class), apiConfig, null);

    JsonNode outcome = schemasConfig.path("Outcome");
    assertEquals("Outcome", outcome.path("id").asText());
    assertEquals("string", outcome.path("type").asText());
    JsonNode enumConfig = outcome.path("enum");
    assertTrue(enumConfig.isArray());
    assertEquals(3, enumConfig.size());
    assertEquals(Outcome.WON.toString(), enumConfig.get(0).asText());
    assertEquals(Outcome.LOST.toString(), enumConfig.get(1).asText());
    assertEquals(Outcome.TIE.toString(), enumConfig.get(2).asText());
  }

  @SuppressWarnings("unused")
  private static class Bean {
    public Date getDate() {
      return null;
    }

    public void setDate(Date date) {}
  }

  @SuppressWarnings("unused")
  private static class ParameterizedBean {
    public Qux<String> getQux() {
      return null;
    }

    public void setQux(Qux<String> qux) {}
  }

  @Test
  public void testBeanPropertyDateType() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode schemasConfig = mapper.createObjectNode();
    writer.addTypeToSchema(schemasConfig, TypeToken.of(Bean.class), apiConfig, null);

    JsonNode beanConfig = schemasConfig.path("Bean");
    assertEquals("Bean", beanConfig.path("id").asText());
    assertEquals("object", beanConfig.path("type").asText());
    assertEquals("string", beanConfig.path("properties").path("date").path("type").asText());
    assertEquals("date-time", beanConfig.path("properties").path("date").path("format").asText());
  }

  @Test
  public void writeConfigWithBasicConfig() throws Exception {
    String jsonString = Iterables.getOnlyElement(
        writer.writeConfig(Collections.singleton(apiConfig)).values());
    assertNotNull(jsonString);

    JsonNode root = objectMapper.readValue(jsonString, JsonNode.class);
    assertEquals("myapi", root.path("name").asText());
    assertEquals("v1", root.path("version").asText());
  }

  @Test
  public void apiCollation() throws Exception {
    ApiConfig config2 = configFactory.copy(apiConfig);
    ApiConfig config3 = configFactory.copy(apiConfig);
    ApiConfig config4 = configFactory.copy(apiConfig);
    ApiConfig config5 = configFactory.copy(apiConfig);
    ApiConfig config6 = configFactory.copy(apiConfig);

    config4.setVersion("v2");
    config5.setVersion("v2");
    config6.setName("differentApi");

    Map<ApiKey, String> jsonConfigs = writer.writeConfig(
        ImmutableList.of(apiConfig, config2, config3, config4, config5, config6));

    assertEquals(3, jsonConfigs.size());
    assertThat(jsonConfigs.keySet())
        .containsExactly(new ApiKey("myapi", "v1", "https://myapp.appspot.com/_ah/api"),
            new ApiKey("myapi", "v2", "https://myapp.appspot.com/_ah/api"),
            new ApiKey("differentApi", "v1", "https://myapp.appspot.com/_ah/api"));
  }

  @Test
  public void bodyFieldConflictsWithParameter() throws Exception {
    final class Endpoint {
      @SuppressWarnings("unused")
      public void set(@Named("date") String id, Bean resource) {}
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(serviceContext, Endpoint.class,
        apiConfig.getApiClassConfig().getMethods());
    try {
      writer.writeConfig(Collections.singleton(apiConfig));
      fail();
    } catch (PropertyParameterNameConflictException e) {
      // Expected.
    }
  }

  static final class IdBean {
    @SuppressWarnings("unused")
    public String getId() {
      return null;
    }

    @SuppressWarnings("unused")
    public void setId(String id) {}
  }

  @Test
  public void idIsASpecialCaseForPropertyConflicts() throws Exception {
    final class Endpoint {
      @SuppressWarnings("unused")
      public void set(@Named("id") String id, IdBean resource) {}

      @SuppressWarnings("unused")
      public IdBean get(@Named("id") String id) {
        return null;
      }

      @SuppressWarnings("unused")
      public IdBean update(@Named("id") String id) {
        return null;
      }

      @SuppressWarnings("unused")
      public void remove(@Named("id") String id) {}
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(serviceContext, Endpoint.class,
        apiConfig.getApiClassConfig().getMethods());
    writer.writeConfig(Collections.singleton(apiConfig));
  }

  @Test
  public void responseFieldDoesNotConflictWithParameter() throws Exception {
    final class Endpoint {
      @SuppressWarnings("unused")
      public Bean get(@Named("date") String id) {
        return null;
      }
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(serviceContext, Endpoint.class,
        apiConfig.getApiClassConfig().getMethods());
    writer.writeConfig(Collections.singleton(apiConfig));
  }

  private enum TestEnum {
    A,
    B,
  }

  @Test
  public void enumResponseIsBlocked() throws Exception {
    final class Endpoint {
      @SuppressWarnings("unused")
      public TestEnum get() {
        return TestEnum.A;
      }
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(serviceContext, Endpoint.class,
        apiConfig.getApiClassConfig().getMethods());
    try {
      writer.writeConfig(Collections.singleton(apiConfig));
      fail();
    } catch (InvalidReturnTypeException e) {
      // Expected.
    }
  }

  @Test
  public void primitiveResponseIsBlocked() throws Exception {
    final class Endpoint {
      @SuppressWarnings("unused")
      public int get() {
        return 4;
      }
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(serviceContext, Endpoint.class,
        apiConfig.getApiClassConfig().getMethods());
    try {
      writer.writeConfig(Collections.singleton(apiConfig));
      fail();
    } catch (InvalidReturnTypeException e) {
      // Expected.
    }
  }

  @Test
  public void objectResponseIsAllowed() throws Exception {
    final class Endpoint {
      @SuppressWarnings("unused")
      public Bean get() {
        return null;
      }
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(serviceContext, Endpoint.class,
        apiConfig.getApiClassConfig().getMethods());
    writer.writeConfig(Collections.singleton(apiConfig));
  }

  @Test
  public void writeConfigOrderIsPreserved() throws Exception {
    @Api(name = "onetoday", description = "OneToday API")
    final class OneToday {
    }
    @Api(name = "onetodayadmin", description = "One Today Admin API")
    final class OneTodayAdmin {
    }
    ApiConfigValidator validator = Mockito.mock(ApiConfigValidator.class);
    TypeLoader typeLoader = new TypeLoader();
    JsonConfigWriter writer =
        new JsonConfigWriter(JsonConfigWriter.class.getClassLoader(), validator);
    ApiConfig oneToday = new ApiConfig.Factory().create(serviceContext, typeLoader, OneToday.class);
    ApiConfig oneTodayAdmin =
        new ApiConfig.Factory().create(serviceContext, typeLoader, OneTodayAdmin.class);
    oneToday.setName("onetoday");
    oneToday.setVersion("v1");
    oneTodayAdmin.setName("onetodayadmin");
    oneTodayAdmin.setVersion("v1");
    Map<ApiKey, String> configs = writer.writeConfig(Lists.newArrayList(oneToday, oneTodayAdmin));
    Iterator<ApiKey> iterator = configs.keySet().iterator();
    assertEquals(new ApiKey("onetoday", "v1", "https://myapp.appspot.com/_ah/api"),
        iterator.next());
    assertEquals(new ApiKey("onetodayadmin", "v1", "https://myapp.appspot.com/_ah/api"),
        iterator.next());
  }

  @Test
  public void writeConfigOrderIsPreservedMulticlass() throws Exception {
    @Api(name = "onetoday", description = "OneToday API")
    final class OneToday {
    }
    @Api(name = "onetoday", description = "OneToday API")
    final class OneToday2 {
    }
    @Api(name = "onetodayadmin", description = "One Today Admin API")
    final class OneTodayAdmin {
    }
    @Api(name = "onetodayadmin", description = "One Today Admin API")
    final class OneTodayAdmin2 {
    }
    ApiConfigValidator validator = Mockito.mock(ApiConfigValidator.class);
    TypeLoader typeLoader = new TypeLoader();
    JsonConfigWriter writer =
        new JsonConfigWriter(JsonConfigWriter.class.getClassLoader(), validator);
    ApiConfig oneToday = new ApiConfig.Factory().create(serviceContext, typeLoader, OneToday.class);
    ApiConfig oneToday2 =
        new ApiConfig.Factory().create(serviceContext, typeLoader, OneToday2.class);
    ApiConfig oneTodayAdmin =
        new ApiConfig.Factory().create(serviceContext, typeLoader, OneTodayAdmin.class);
    ApiConfig oneTodayAdmin2 =
        new ApiConfig.Factory().create(serviceContext, typeLoader, OneTodayAdmin2.class);
    oneToday.setName("onetoday");
    oneToday.setVersion("v1");
    oneToday2.setName("onetoday");
    oneToday2.setVersion("v1");
    oneTodayAdmin.setName("onetodayadmin");
    oneTodayAdmin.setVersion("v1");
    oneTodayAdmin2.setName("onetodayadmin");
    oneTodayAdmin2.setVersion("v1");
    Map<ApiKey, String> configs =
        writer.writeConfig(Lists.newArrayList(oneToday, oneTodayAdmin, oneTodayAdmin2, oneToday2));
    Iterator<ApiKey> iterator = configs.keySet().iterator();
    assertEquals(new ApiKey("onetoday", "v1", "https://myapp.appspot.com/_ah/api"),
        iterator.next());
    assertEquals(new ApiKey("onetodayadmin", "v1", "https://myapp.appspot.com/_ah/api"),
        iterator.next());
  }

  /**
   * This tests writeConfig with a parameterized type which is transformed to {@code String}.
   * When the transformer is present, writeConfig should not throw an exception.
   */
  @Test
  public void writeConfigWithParameterizedTypeTransformerConfig() throws Exception {
    ApiConfig transformedApiConfig = configFactory.copy(apiConfig);
    transformedApiConfig.getSerializationConfig()
        .addSerializationConfig(ParameterizedTypeTransformer.class);

    final class Endpoint {
      @SuppressWarnings("unused")
      public ParameterizedBean get(@Named("date") String id) {
        return null;
      }
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(
        serviceContext, Endpoint.class, transformedApiConfig.getApiClassConfig().getMethods());
    writer.writeConfig(Collections.singleton(transformedApiConfig));
  }

  /**
   * This tests writeConfig with a parameterized type, which should be supported by adding a schema
   * named Qux_String.
   */
  @Test
  public void writeConfigWithParameterizedTypeNoTransformerConfig() throws Exception {
    final class Endpoint {
      @SuppressWarnings("unused")
      public ParameterizedBean get(@Named("date") String id) {
        return null;
      }
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(
        serviceContext, Endpoint.class, apiConfig.getApiClassConfig().getMethods());

    Map<ApiKey, String> apiKeyStringMap = writer.writeConfig(Collections.singleton(apiConfig));
    String configString = Iterables.getFirst(apiKeyStringMap.values(), null);
    assertThat(configString).contains("Qux_String");
  }

  /**
   * Tests that if a method has multiple resource parameters, a helpful message is thrown.
   */
  @Test
  public void multipleResourceParametersErrorMessage() throws Exception {
    final class Endpoint {
      @SuppressWarnings("unused")
      public void get(Bean bean1, Bean bean2) {
      }
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(
        serviceContext, Endpoint.class, apiConfig.getApiClassConfig().getMethods());
    try {
      writer.writeConfig(Collections.singleton(apiConfig));
      fail("Method with multiple resources should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("cannot have multiple resource parameters");
    }
  }

  /**
   * Tests that ignored methods aren't written to config.
   */
  @Test
  public void ignoredMethod() throws Exception {
    final class Endpoint {
      @ApiMethod(ignored = AnnotationBoolean.TRUE)
      public void thisShouldBeIgnored() {
      }
    }

    new ApiConfigAnnotationReader().loadEndpointMethods(
        serviceContext, Endpoint.class, apiConfig.getApiClassConfig().getMethods());
    for (String config : writer.writeConfig(Collections.singleton(apiConfig)).values()) {
      assertThat(config).doesNotContain("thisShouldBeIgnored");
    }
  }

  /**
   * Tests that standard parameter names aren't written as per-method parameters.
   */
  @Test
  public void standardParameters() throws Exception {
    final class Endpoint {
      public void standardParameters(@Named("alt") String alt, @Named("fields") String fields,
          @Named("key") String key, @Named("oauth_token") String oauthToken,
          @Named("prettyPrint") boolean prettyPrint, @Named("quotaUser") String quotaUser,
          @Named("userIp") String userIp) {
      }
    }
    new ApiConfigAnnotationReader().loadEndpointMethods(
        serviceContext, Endpoint.class, apiConfig.getApiClassConfig().getMethods());
    for (String config : writer.writeConfig(Collections.singleton(apiConfig)).values()) {
      for (String param : StandardParameters.STANDARD_PARAM_NAMES) {
        // Make sure they don't appear as path parameters. Standard parameters are added by
        // discovery, so this means the parameters should not appear at all in the config.
        assertThat(config).doesNotContain(param);
      }
    }
  }
}
