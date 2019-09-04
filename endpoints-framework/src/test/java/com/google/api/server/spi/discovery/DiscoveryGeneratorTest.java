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
package com.google.api.server.spi.discovery;

import static com.google.api.server.spi.config.model.EndpointsFlag.MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA;
import static com.google.api.server.spi.config.model.EndpointsFlag.MAP_SCHEMA_IGNORE_UNSUPPORTED_KEY_TYPES;
import static com.google.api.server.spi.config.model.EndpointsFlag.MAP_SCHEMA_SUPPORT_ARRAYS_VALUES;
import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.discovery.DiscoveryGenerator.DiscoveryContext;
import com.google.api.server.spi.testing.AbsoluteCommonPathEndpoint;
import com.google.api.server.spi.testing.AbsolutePathEndpoint;
import com.google.api.server.spi.testing.ArrayEndpoint;
import com.google.api.server.spi.testing.CustomScopesEndpoint;
import com.google.api.server.spi.testing.EnumEndpoint;
import com.google.api.server.spi.testing.EnumEndpointV2;
import com.google.api.server.spi.testing.FooDescriptionEndpoint;
import com.google.api.server.spi.testing.FooEndpoint;
import com.google.api.server.spi.testing.MapEndpoint;
import com.google.api.server.spi.testing.MapEndpointInvalid;
import com.google.api.server.spi.testing.MultipleParameterEndpoint;
import com.google.api.server.spi.testing.NamespaceEndpoint;
import com.google.api.server.spi.testing.NonDiscoverableEndpoint;
import com.google.api.server.spi.testing.PrimitiveEndpoint;
import com.google.api.server.spi.testing.RequiredPropertiesEndpoint;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.RestDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link DiscoveryGenerator}.
 */
@RunWith(JUnit4.class)
public class DiscoveryGeneratorTest {
  private final DiscoveryContext context = new DiscoveryContext()
      .setApiRoot("https://discovery-test.appspot.com/api");
  private final ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
  private DiscoveryGenerator generator;
  private ApiConfigLoader configLoader;
  private SchemaRepository schemaRepository;

  @Before
  public void setUp() throws Exception {
    TypeLoader typeLoader = new TypeLoader(getClass().getClassLoader());
    ApiConfigAnnotationReader annotationReader =
        new ApiConfigAnnotationReader(typeLoader.getAnnotationTypes());
    this.configLoader = new ApiConfigLoader(new ApiConfig.Factory(), typeLoader,
        annotationReader);
    this.generator = new DiscoveryGenerator(typeLoader);
    this.schemaRepository = new SchemaRepository(typeLoader);
  }

  @Test
  public void testWriteDiscovery_FooEndpoint() throws Exception {
    RestDescription doc = getDiscovery(context, FooEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("foo_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_FooEndpointDefaultContext() throws Exception {
    RestDescription doc = getDiscovery(new DiscoveryContext(), FooEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("foo_endpoint_default_context.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_FooEndpointLocalhost() throws Exception {
    RestDescription doc = getDiscovery(
        new DiscoveryContext().setApiRoot("http://localhost:8080/api"), FooEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("foo_endpoint_localhost.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_EnumEndpoint() throws Exception {
    RestDescription doc = getDiscovery(new DiscoveryContext(), EnumEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("enum_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_ArrayEndpoint() throws Exception {
    RestDescription doc = getDiscovery(new DiscoveryContext(), ArrayEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("array_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_MapEndpoint() throws Exception {
    RestDescription doc = getDiscovery(new DiscoveryContext(), MapEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("map_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_MapEndpoint_Legacy() throws Exception {
    System.setProperty(MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA.systemPropertyName, "");
    try {
      RestDescription doc = getDiscovery(new DiscoveryContext(), MapEndpoint.class);
      RestDescription expected = readExpectedAsDiscovery("map_endpoint_legacy.json");
      compareDiscovery(expected, doc);
    } finally {
      System.clearProperty(MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA.systemPropertyName);
    }
  }

  @Test
  public void testWriteDiscovery_MapEndpoint_InvalidKeyType() throws Exception {
    try {
      getDiscovery(new DiscoveryContext(), MapEndpointInvalid.class);
      Assert.fail("Should have failed to generate schema for invalid key type");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testWriteDiscovery_MapEndpoint_InvalidKeyType_ignore() throws Exception {
    System.setProperty(MAP_SCHEMA_IGNORE_UNSUPPORTED_KEY_TYPES.systemPropertyName, "true");
    try {
      getDiscovery(new DiscoveryContext(), MapEndpointInvalid.class);
    } finally {
      System.clearProperty(MAP_SCHEMA_IGNORE_UNSUPPORTED_KEY_TYPES.systemPropertyName);
    }
  }

  @Test
  public void testWriteDiscovery_MapEndpoint_WithArrayValue() throws Exception {
    System.setProperty(MAP_SCHEMA_SUPPORT_ARRAYS_VALUES.systemPropertyName, "yes");
    try {
      RestDescription doc = getDiscovery(new DiscoveryContext(), MapEndpoint.class);
      RestDescription expected = readExpectedAsDiscovery("map_endpoint_with_array.json");
      compareDiscovery(expected, doc);
    } finally {
      System.clearProperty(MAP_SCHEMA_SUPPORT_ARRAYS_VALUES.systemPropertyName);
    }
  }

  @Test
  public void testWriteDiscovery_namespace() throws Exception {
    RestDescription doc = getDiscovery(new DiscoveryContext(), NamespaceEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("namespace_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_parameterOrder() throws Exception {
    RestDescription doc = getDiscovery(new DiscoveryContext(), MultipleParameterEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("multiple_parameter_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_newHostname() throws Exception {
    RestDescription doc = getDiscovery(
        new DiscoveryContext().setApiRoot("http://notlocalhost/api").setHostname("localhost:8080"),
        FooEndpoint.class
    );
    RestDescription expected = readExpectedAsDiscovery("foo_endpoint_localhost.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_PrimitiveEndpoint() throws Exception {
    RestDescription doc = getDiscovery(context, PrimitiveEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("primitive_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_AbsolutePathEndpoint() throws Exception {
    RestDescription doc = getDiscovery(context, AbsolutePathEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("absolute_path_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_AbsoluteCommonPathEndpoint() throws Exception {
    RestDescription doc = getDiscovery(context, AbsoluteCommonPathEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("absolute_common_path_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_FooEndpointWithDescription() throws Exception {
    RestDescription doc = getDiscovery(context, FooDescriptionEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("foo_with_description_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_RequiredPropertiesEndpoint() throws Exception {
    RestDescription doc = getDiscovery(context, RequiredPropertiesEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("required_parameters_endpoint.json");
    compareDiscovery(expected, doc);
  }
  
  @Test
  public void testWriteDiscovery_multipleApisWithSharedSchema() throws Exception {
    // Read in an API that uses a resource with fields that have their own schema, then read in
    // another API that uses the same resource. The discovery for the second API should contain
    // both schema, rather than just the first one.
    getDiscovery(new DiscoveryContext(), EnumEndpointV2.class);
    RestDescription doc = getDiscovery(new DiscoveryContext(), EnumEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("enum_endpoint.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_scopeDescriptions() throws Exception {
    RestDescription doc = getDiscovery(context, CustomScopesEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("custom_scopes.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_directory() throws Exception {
    DiscoveryGenerator.Result result =
        generator.writeDiscovery(
            ImmutableList.of(
                configLoader.loadConfiguration(ServiceContext.create(), ArrayEndpoint.class),
                configLoader.loadConfiguration(ServiceContext.create(), EnumEndpoint.class)),
            new DiscoveryContext());
    assertThat(result.directory()).isEqualTo(readExpectedAsDirectory("directory.json"));
  }

  @Test
  public void testWriteDiscovery_nonDiscoverableEndpointButGenerateAll() throws Exception {
    getDiscovery(context, NonDiscoverableEndpoint.class);
    RestDescription doc = getDiscovery(new DiscoveryContext(), NonDiscoverableEndpoint.class);
    RestDescription expected = readExpectedAsDiscovery("foo_endpoint_default_context.json");
    compareDiscovery(expected, doc);
  }

  @Test
  public void testWriteDiscovery_nonDiscoverableEndpoint() throws Exception {
    DiscoveryGenerator.Result result = generator.writeDiscovery(
        ImmutableList.of(
            configLoader.loadConfiguration(ServiceContext.create(), NonDiscoverableEndpoint.class)),
        new DiscoveryContext().setGenerateAll(false));
    assertThat(result.discoveryDocs()).isEmpty();
    assertThat(result.directory().getItems()).isEmpty();
  }

  @Test
  public void testDirectoryIsCloneable() throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), FooEndpoint.class);
    DiscoveryGenerator.Result result = generator.writeDiscovery(ImmutableList.of(config), context);
    result.directory().clone();
  }

  private RestDescription getDiscovery(DiscoveryContext context, Class<?> serviceClass)
      throws Exception {
    ApiConfig config = configLoader.loadConfiguration(ServiceContext.create(), serviceClass);
    // If the clone call fails, the generated discovery is invalid.
    return Iterables.getFirst(
        generator.writeDiscovery(
            ImmutableList.of(config), context, schemaRepository).discoveryDocs().values(),
        null).clone();
  }

  private RestDescription readExpectedAsDiscovery(String file) throws Exception {
    String expectedString = IoUtil.readResourceFile(DiscoveryGeneratorTest.class, file);
    return JacksonFactory.getDefaultInstance().createJsonParser(expectedString)
        .parse(RestDescription.class);
  }

  private DirectoryList readExpectedAsDirectory(String file) throws Exception {
    String expectedString = IoUtil.readResourceFile(DiscoveryGeneratorTest.class, file);
    return JacksonFactory.getDefaultInstance().createJsonParser(expectedString)
        .parse(DirectoryList.class);
  }

  private void compareDiscovery(RestDescription expected, RestDescription actual) throws Exception {
    DiscoverySubject.assertThat(actual).isSameAs(expected);
  }
}
