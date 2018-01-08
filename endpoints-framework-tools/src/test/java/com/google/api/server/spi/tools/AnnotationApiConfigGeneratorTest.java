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
package com.google.api.server.spi.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiCacheControl;
import com.google.api.server.spi.config.ApiClass;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.api.server.spi.config.ApiTransformer;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.ResourceTransformer;
import com.google.api.server.spi.config.jsonwriter.JsonConfigWriter;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.validation.CollectionResourceException;
import com.google.api.server.spi.config.validation.DuplicateParameterNameException;
import com.google.api.server.spi.config.validation.DuplicateRestPathException;
import com.google.api.server.spi.config.validation.GenericTypeException;
import com.google.api.server.spi.config.validation.InconsistentApiConfigurationException;
import com.google.api.server.spi.config.validation.InvalidNamespaceException;
import com.google.api.server.spi.config.validation.InvalidParameterAnnotationsException;
import com.google.api.server.spi.config.validation.InvalidReturnTypeException;
import com.google.api.server.spi.config.validation.MissingParameterNameException;
import com.google.api.server.spi.config.validation.NestedCollectionException;
import com.google.api.server.spi.config.validation.OverloadedMethodException;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.testing.ArrayEndpoint;
import com.google.api.server.spi.testing.Bar;
import com.google.api.server.spi.testing.Baz;
import com.google.api.server.spi.testing.BoundedGenericEndpoint;
import com.google.api.server.spi.testing.BridgeInheritanceEndpoint;
import com.google.api.server.spi.testing.CollectionContravarianceEndpoint;
import com.google.api.server.spi.testing.CollectionCovarianceEndpoint;
import com.google.api.server.spi.testing.DeepGenericHierarchyFailEndpoint;
import com.google.api.server.spi.testing.DeepGenericHierarchySuccessEndpoint;
import com.google.api.server.spi.testing.DefaultValueSerializer;
import com.google.api.server.spi.testing.DuplicateMethodEndpoint;
import com.google.api.server.spi.testing.Endpoint0;
import com.google.api.server.spi.testing.Endpoint1;
import com.google.api.server.spi.testing.Endpoint2;
import com.google.api.server.spi.testing.Endpoint3;
import com.google.api.server.spi.testing.Endpoint4;
import com.google.api.server.spi.testing.Endpoint5;
import com.google.api.server.spi.testing.ParentChildEndpoint;
import com.google.api.server.spi.testing.PrimitiveEndpoint;
import com.google.api.server.spi.testing.RecursiveEndpoint;
import com.google.api.server.spi.testing.RestfulResourceEndpointBase;
import com.google.api.server.spi.testing.SimpleContravarianceEndpoint;
import com.google.api.server.spi.testing.SimpleCovarianceEndpoint;
import com.google.api.server.spi.testing.SimpleLevelOverridingApi;
import com.google.api.server.spi.testing.SimpleOverloadEndpoint;
import com.google.api.server.spi.testing.SimpleOverrideEndpoint;
import com.google.api.server.spi.testing.SubclassedEndpoint;
import com.google.api.server.spi.testing.SubclassedOverridingEndpoint;
import com.google.appengine.api.users.User;
import com.google.common.reflect.TypeToken;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Tests for {@link AnnotationApiConfigGenerator}.
 */
@RunWith(JUnit4.class)
public class AnnotationApiConfigGeneratorTest {

  private final ObjectMapper objectMapper = ObjectMapperUtil.createStandardObjectMapper();

  protected ApiConfigGenerator g;

  @Before
  public void setUp() throws Exception {
    g = createApiConfigGenerator();
  }

  protected ApiConfigGenerator createApiConfigGenerator() throws Exception {
    return new AnnotationApiConfigGenerator();
  }

  @Test
  public void testEndpointWithOnlyDefaultConfiguration() throws Exception {
    String apiConfigSource = g.generateConfig(Endpoint0.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);

    verifyApi(root, "thirdParty.api", "https://myapp.appspot.com/_ah/api",
        "myapi", "v1", "", "https://myapp.appspot.com/_ah/spi", false, true);

    JsonNode methodGetFoo = root.path("methods").path("myapi.endpoint0.getFoo");
    verifyMethod(methodGetFoo, "foo/{id}", HttpMethod.GET, Endpoint0.class.getName() + ".getFoo",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(methodGetFoo.path("request"), "empty", 1);
    verifyMethodRequestParameter(methodGetFoo.path("request"), "id", "string", true, false);

    verifyEndpoint0Schema(root, Endpoint0.class.getName());
  }

  @SuppressWarnings("unused")
  @Api
  private static class MultipleEndpoint1 {
    @ApiMethod(name = "add", path = "item", httpMethod = HttpMethod.POST)
    public void add(@Named("id") String id) {}
  }

  @SuppressWarnings("unused")
  @Api
  private static class MultipleEndpoint2 {
    @ApiMethod(name = "delete", path = "item/{id}", httpMethod = HttpMethod.DELETE)
    public void delete(@Named("id") String id) {}
  }

  @Test
  public void testMultipleServiceClasses() throws Exception {
    String apiConfigSource =
        g.generateConfig(MultipleEndpoint1.class, MultipleEndpoint2.class).get("myapi-v1.api");
    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);

    assertEquals("item", root.path("methods").path("myapi.add").path("path").asText());
    assertEquals("item/{id}", root.path("methods").path("myapi.delete").path("path").asText());
  }

  @Test
  public void testFullyConfiguredEndpoint() throws Exception {
    String apiConfigSource = g.generateConfig(Endpoint1.class).get("myapi-v1.api");
    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    verifyEndpoint1(Endpoint1.class, root);
    verifyEndpoint1Schema(Endpoint1.class, root);
  }

  @Test
  public void testEmptyRequestBodies() throws Exception {
    String apiConfigSource = g.generateConfig(Endpoint1.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methods = root.path("methods");

    // myapi.foos.get
    JsonNode methodGetFoo = methods.path("myapi.foos.get");
    assertTrue(methodGetFoo.path("request").path("body").asText().equals("empty"));
    assertFalse(root.path("descriptor").path("methods")
        .path(methodGetFoo.path("rosyMethod").asText()).has("request"));

    // myapi.foos.insert
    JsonNode methodInsertFoo = methods.path("myapi.foos.insert");
    assertFalse(methodInsertFoo.path("request").path("body").asText().equals("empty"));
    assertTrue(root.path("descriptor").path("methods")
        .path(methodInsertFoo.path("rosyMethod").asText()).has("request"));

    // myapi.foos.execute0
    JsonNode methodExecute0 = methods.path("myapi.foos.execute0");
    assertTrue(methodExecute0.path("request").path("body").asText().equals("empty"));
    assertFalse(root.path("descriptor").path("methods")
        .path(methodExecute0.path("rosyMethod").asText()).has("request"));
  }

  @Test
  public void testEmptyParameterBodies() throws Exception {
    String apiConfigSource = g.generateConfig(Endpoint1.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methods = root.path("methods");

    // foos.list
    JsonNode methodListFoo = methods.path("myapi.foos.list");
    assertFalse(methodListFoo.path("request").has("parameters"));

    // foos.get
    JsonNode methodGetFoo = methods.path("myapi.foos.get");
    assertTrue(methodGetFoo.path("request").has("parameters"));
    assertFalse(0 == methodGetFoo.path("request").path("parameters").size());

    //foos.insert
    JsonNode methodInsertFoo = methods.path("myapi.foos.insert");
    assertFalse(methodInsertFoo.path("request").has("parameters"));
  }

  @Test
  public void testEndpointWithNoPublicMethods() throws Exception {
    String apiConfigSource = g.generateConfig(Endpoint2.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);

    JsonNode methods = root.path("methods");
    assertNull(methods.findValue("api2.foos.invisible0"));
    assertNull(methods.findValue("api2.foos.invisible1"));
  }

  @Test
  public void testEndpointWithInheritance() throws Exception {
    String apiConfigSource = g.generateConfig(Endpoint3.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    verifyEndpoint1(Endpoint3.class, root);
    verifyEndpoint3(root);
    verifyEndpoint1Schema(Endpoint3.class, root);
    verifyEndpoint3Schema(root);
  }

  @Test
  public void testEndpointWithBridgeMethods() throws Exception {
    String apiConfigSource = g.generateConfig(BridgeInheritanceEndpoint.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methods = root.path("methods");

    assertEquals(2, methods.size());
    assertNull(methods.findValue("myapi.fn1"));
    assertNotNull(methods.findValue("myapi.api6.foos.fn1"));
    assertNotNull(methods.findValue("myapi.api6.foos.fn2"));
  }

  @Test
  public void testDuplicateMethodEndpoint() throws Exception {
    try {
      g.generateConfig(DuplicateMethodEndpoint.class);
      fail("Config generation for endpoint with overloaded method should have failed.");
    } catch (OverloadedMethodException expected) {
      // expected
    }
  }

  @Test
  public void testSimpleOverrideEndpoint() throws Exception {
    String apiConfigSource = g.generateConfig(SimpleOverrideEndpoint.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methods = root.path("methods");
    assertEquals(1, methods.size());
  }

  @Test
  public void testSimpleOverloadEndpoint() throws Exception {
    try {
      g.generateConfig(SimpleOverloadEndpoint.class);
      fail("Config generation for endpoint with overloaded inherited method should have failed");
    } catch (OverloadedMethodException expected) {
      // expected
    }
  }

  @Test
  public void testSimpleCovarianceEndpoint() throws Exception {
    try {
      g.generateConfig(SimpleCovarianceEndpoint.class);
      fail("Config generation for endpoint with covariant inherited method should have failed");
    } catch (OverloadedMethodException expected) {
      // expected
    }
  }

  @Test
  public void testSimpleContravarianceEndpoint() throws Exception {
    try {
      g.generateConfig(SimpleContravarianceEndpoint.class);
      fail("Config generation for endpoint with contravariant inherited method should have failed");
    } catch (OverloadedMethodException expected) {
      // expected
    }
  }

  @Test
  public void testCollectionCovarianceEndpoint() throws Exception {
    try {
      g.generateConfig(CollectionCovarianceEndpoint.class);
      fail("Config generation for endpoint with covariant inherited method should have failed");
    } catch (OverloadedMethodException expected) {
      // expected
    }
  }

  @Test
  public void testCollectionContravarianceEndpoint() throws Exception {
    try {
      g.generateConfig(CollectionContravarianceEndpoint.class);
      fail("Config generation for endpoint with contravariant inherited method should have failed");
    } catch (OverloadedMethodException expected) {
      // expected
    }
  }

  @Test
  public void testFullySpecializedDateEndpoint() throws Exception {
    String apiConfigSource = g.generateConfig(
        RestfulResourceEndpointBase.FullySpecializedEndpoint.class).get("fullApi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methods = root.path("methods");
    assertEquals(6, methods.size());
  }

  @Test
  public void testGenericBasePartiallySpecializedEndpoint() throws Exception {
    String apiConfigSource = g.generateConfig(
        RestfulResourceEndpointBase.PartiallySpecializedEndpoint.class).get("partialApi-v1.api");
    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methods = root.path("methods");
    assertEquals(6, methods.size());

    JsonNode rootPartial = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methodsPartial = rootPartial.path("descriptor").path("methods");
    JsonNode rootFull = objectMapper.readValue(
        g.generateConfig(RestfulResourceEndpointBase.FullySpecializedEndpoint.class)
        .get("fullApi-v1.api"), JsonNode.class);
    JsonNode methodsFull = rootFull.path("descriptor").path("methods");

    String partialPrefix = RestfulResourceEndpointBase.PartiallySpecializedEndpoint.class.getName();
    String fullPrefix = RestfulResourceEndpointBase.FullySpecializedEndpoint.class.getName();
    // list
    JsonNode methodListPartial = methodsFull.path(partialPrefix + ".list");
    JsonNode methodListFull = methodsFull.path(fullPrefix + ".list");
    assertNotNull(methodListPartial);
    assertEquals(
        methodListPartial.path("request").asText(), methodListFull.path("request").asText());
    // misc
    JsonNode methodMiscPartial = methodsFull.path("GenericRestBasePartiallySpecialized.misc");
    JsonNode methodMiscFull = methodsFull.path("GenericRestBaseFullySpecialized.misc");
    assertEquals(
        methodMiscPartial.path("request").asText(), methodMiscFull.path("request").asText());
  }

  @Test
  public void testDeepGenericHierarchySuccessEndpoint() throws Exception {
    String apiConfigSource =
        g.generateConfig(DeepGenericHierarchySuccessEndpoint.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methods = root.path("methods");
    assertEquals(1, methods.size());
  }

  @Test
  public void testBoundedGenericEndpoint() throws Exception {
    String apiConfigSource = g.generateConfig(BoundedGenericEndpoint.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methods = root.path("methods");
    assertEquals(1, methods.size());
  }

  @Test
  public void testWildcardParameterTypes() throws Exception {
    @Api
    final class WildcardEndpoint {
      @SuppressWarnings("unused")
      public void foo(Map<String, ? extends Integer> map) {}
    }
    try {
      g.generateConfig(WildcardEndpoint.class);
      fail("Config generation for service class with wildcard parameter type should have failed");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testDeepGenericHierarchyFailEndpoint() throws Exception {
    try {
      g.generateConfig(DeepGenericHierarchyFailEndpoint.class);
      fail("Config generation for endpoint with contravariant inherited method should have failed");
    } catch (OverloadedMethodException expected) {
      // expected
    }
  }

  @Test
  public void testServiceWithMergedInheritance() throws Exception {
    String apiConfigSource = g.generateConfig(SubclassedEndpoint.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    verifyEndpoint1(SubclassedEndpoint.class, root);
    verifyEndpoint1Schema(SubclassedEndpoint.class, root);
  }

  @Test
  public void testServiceWithOverridingInheritance() throws Exception {
    String apiConfigSource =
        g.generateConfig(SubclassedOverridingEndpoint.class).get("myapi-v2.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    verifySubclassedOverridingEndpoint(SubclassedOverridingEndpoint.class, root);
    verifySubclassedOverridingEndpointSchema(SubclassedOverridingEndpoint.class, root);
  }

  @Test
  public void testServiceWithApiNameOverride() throws Exception {
    String apiConfigSource = g.generateConfig(
        ServiceContext.create("abc", "xyz"), Endpoint4.class).get("api4-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    assertEquals("api4", root.path("name").asText());

    JsonNode methods = root.path("methods");

    verifyEndpoint0Schema(root, Endpoint4.class.getName());
  }

  @Test
  public void testDefaultEndpointOnGooglePlex() throws Exception {
    String apiConfigSource = g.generateConfig(
        ServiceContext.create("google.com:myapp", "myapi"), Endpoint0.class)
        .get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    assertEquals("myapi", root.path("name").asText());
    assertEquals("https://myapp.googleplex.com/_ah/api", root.path("root").asText());
    assertEquals("https://myapp.googleplex.com/_ah/spi", root.path("adapter").path("bns").asText());
  }

  @Test
  public void testEndpointWithNestedBeans() throws Exception {
    String apiConfigSource = g.generateConfig(Endpoint5.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    verifyEndpoint5Schema(root);
  }

  @Test
  public void testPrimitiveSchemas() throws Exception {
    String apiConfigSource = g.generateConfig(PrimitiveEndpoint.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    verifyPrimitiveSchemas(root);
  }

  @Test
  public void testEndpointNoApiAnnotation() throws Exception {
    try {
      g.generateConfig(Object.class);
      fail("Config generation for service class with no @Api annotation should have failed");
    } catch (ApiConfigException e) {
      // expected
    }
  }

  @Api
  static class TestUnnamedParameterType {
    public String getFoo(String id) {
      return null;
    }
  }

  @Test
  public void testEndpointWithUnnamedParameterTypes() throws Exception {
    try {
      g.generateConfig(TestUnnamedParameterType.class);
      fail("Config generation for service class with unnamed parameter type should have failed");
    } catch (MissingParameterNameException e) {
      // expected
    }
  }

  @Test
  public void testSimpleLevelOverriding() throws Exception {
    String apiConfigSource = g.generateConfig(SimpleLevelOverridingApi.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    JsonNode methods = root.path("methods");

    // myapi.resource1.noOverrides
    JsonNode noOverrides = methods.path("myapi.resource1.noOverrides");
    assertFalse(noOverrides.isMissingNode());
    verifyStrings(objectMapper.convertValue(noOverrides.path("scopes"), String[].class),
        new String[] {"s0a", "s1a"});
    verifyStrings(objectMapper.convertValue(noOverrides.path("audiences"), String[].class),
        new String[] {"a0a", "a1a"});
    verifyStrings(objectMapper.convertValue(noOverrides.path("clientIds"), String[].class),
        new String[] {"c0a", "c1a"});
    assertEquals("resource1", objectMapper.convertValue(noOverrides.path("path"), String.class));
    assertEquals(AuthLevel.REQUIRED,
        objectMapper.convertValue(noOverrides.path("authLevel"), AuthLevel.class));

    // myapi.resource1.overrides
    JsonNode overrides = methods.path("myapi.resource1.overrides");
    assertFalse(overrides.isMissingNode());
    verifyStrings(objectMapper.convertValue(overrides.path("scopes"), String[].class),
        new String[] {"s0b", "s1b"});
    verifyStrings(objectMapper.convertValue(overrides.path("audiences"), String[].class),
        new String[] {"a0b", "a1b"});
    verifyStrings(objectMapper.convertValue(overrides.path("clientIds"), String[].class),
        new String[] {"c0b", "c1b"});
    assertEquals("overridden", objectMapper.convertValue(overrides.path("path"), String.class));
    assertEquals(AuthLevel.OPTIONAL,
        objectMapper.convertValue(overrides.path("authLevel"), AuthLevel.class));
  }

  private void verifyPrimitiveSchemas(JsonNode root) {
    JsonNode schemas = root.path("descriptor").path("schemas");

    JsonNode primitive = schemas.path("PrimitiveBean");
    verifyPrimitiveSchema(primitive);

    JsonNode primitiveCollection = schemas.path("PrimitiveBeanCollection");
    verifyArraySchemaRef(primitiveCollection.path("properties").path("items"), "PrimitiveBean");

    String className = PrimitiveEndpoint.class.getName();
    verifyEmptyMethodRequest(root, className + ".getPrimitive");
    verifyMethodResponseRef(root, className + ".getPrimitive", "PrimitiveBean");
    verifyEmptyMethodRequest(root, className + ".getPrimitives");
    verifyMethodResponseRef(root, className + ".getPrimitives", "PrimitiveBeanCollection");
  }

  private void verifyPrimitiveSchema(JsonNode primitive) {
    verifyObjectSchema(primitive, "PrimitiveBean", "object");
    verifyObjectPropertySchema(primitive, "bool", "boolean");
    verifyObjectPropertySchema(primitive, "byte", "integer");
    verifyObjectPropertySchema(primitive, "char", "string");
    verifyObjectPropertySchema(primitive, "double", "number");
    verifyObjectPropertySchema(primitive, "float", "number", "float");
    verifyObjectPropertySchema(primitive, "int", "integer");
    verifyObjectPropertySchema(primitive, "long", "string", "int64");
    verifyObjectPropertySchema(primitive, "short", "integer");
    verifyObjectPropertySchema(primitive, "str", "string");
  }

  @Test
  public void testRecursiveSchemas() throws Exception {
    String apiConfigSource = g.generateConfig(RecursiveEndpoint.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    verifyRecursiveSchemas(root);
  }

  private void verifyRecursiveSchemas(JsonNode root) {
    JsonNode schemas = root.path("descriptor").path("schemas");

    JsonNode recursive = schemas.path("RecursiveBean");
    verifyObjectSchema(recursive, "RecursiveBean", "object");
    verifyObjectPropertySchema(recursive, "name", "string");
    verifyObjectPropertyRef(recursive, "child", "RecursiveBean");
    verifyObjectPropertyRef(recursive, "primitive", "PrimitiveBean");

    JsonNode primitive = schemas.path("PrimitiveBean");
    verifyPrimitiveSchema(primitive);

    String className = RecursiveEndpoint.class.getName();
    verifyEmptyMethodRequest(root, className + ".getRecursive");
    verifyMethodResponseRef(root, className + ".getRecursive", "RecursiveBean");
    verifyMethodRequestRef(root, className + ".updateRecursive", "RecursiveBean");
    verifyMethodResponseRef(root, className + ".updateRecursive", "RecursiveBean");
  }

  @Test
  public void testParentChildSchemas() throws Exception {
    String apiConfigSource = g.generateConfig(ParentChildEndpoint.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    verifyParentChildSchemas(root);
  }

  private void verifyParentChildSchemas(JsonNode root) {
    JsonNode schemas = root.path("descriptor").path("schemas");

    JsonNode parent = schemas.path("ParentBean");
    verifyObjectSchema(parent, "ParentBean", "object");
    verifyObjectPropertySchema(parent, "name", "string");
    verifyArraySchemaRef(parent.path("properties").path("children"), "ChildBean");

    JsonNode child = schemas.path("ChildBean");
    verifyObjectSchema(child, "ChildBean", "object");
    verifyObjectPropertySchema(child, "id", "integer");
    verifyObjectPropertyRef(child, "parent", "ParentBean");
    verifyArraySchema(child.path("properties").path("names"), "string");

    JsonNode children = schemas.path("ChildBeanCollection");
    verifyArraySchemaRef(children.path("properties").path("items"), "ChildBean");

    verifyEmptyMethodRequest(root, ParentChildEndpoint.class.getName() + ".getParent");
    verifyMethodResponseRef(root, ParentChildEndpoint.class.getName() + ".getParent", "ParentBean");
    verifyEmptyMethodRequest(root, ParentChildEndpoint.class.getName() + ".listChildren");
    verifyMethodResponseRef(
        root, ParentChildEndpoint.class.getName() + ".listChildren", "ChildBeanCollection");
  }

  @Test
  public void testEndpointWithMultidimensionalArrays() throws Exception {
    String apiConfigSource = g.generateConfig(ArrayEndpoint.class).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, JsonNode.class);
    verifyArrayEndpointSchema(root);
  }

  private void verifyArrayEndpointSchema(JsonNode root) {
    JsonNode schemas = root.path("descriptor").path("schemas");

    JsonNode fooCollection = schemas.path("FooCollection");
    verifyArraySchemaRef(fooCollection.path("properties").path("items"), "Foo");

    JsonNode fooCollectionCollection = schemas.path("FooCollectionCollection");
    verifyArraySchemaRef(fooCollectionCollection.path("properties").path("items"), "Foo", 2);

    JsonNode integerCollection = schemas.path("IntegerCollection");
    verifyArraySchema(integerCollection.path("properties").path("items"), "integer");

    JsonNode baz = schemas.path("Baz");
    verifyObjectSchema(baz, "Baz", "object");
    verifyObjectPropertyRef(baz, "foo", "Foo");
    verifyArraySchemaRef(baz.path("properties").path("foos"), "Foo");

    String backendName = ArrayEndpoint.class.getName();
    verifyEmptyMethodRequest(root, backendName + ".getFoos");
    verifyMethodResponseRef(root, backendName + ".getFoos", "FooCollection");
    verifyEmptyMethodRequest(root, backendName + ".getAllFoos");
    verifyMethodResponseRef(root, backendName + ".getAllFoos", "FooCollectionCollection");
    verifyEmptyMethodRequest(root, backendName + ".getArrayedFoos");
    verifyMethodResponseRef(root, backendName + ".getArrayedFoos", "FooCollection");
    verifyEmptyMethodRequest(root, backendName + ".getAllArrayedFoosFoos");
    verifyMethodResponseRef(root, backendName + ".getAllArrayedFoos", "FooCollectionCollection");
    verifyEmptyMethodRequest(root, backendName + ".getFoosResponse");
    verifyMethodResponseRef(root, backendName + ".getFoosResponse", "CollectionResponse_Foo");
    verifyEmptyMethodRequest(root, backendName + ".getAllFoosResponse");
    verifyMethodResponseRef(
        root, backendName + ".getAllFoosResponse", "CollectionResponse_FooCollection");
    verifyEmptyMethodRequest(root, backendName + ".getAllNestedFoosResponse");
    verifyMethodResponseRef(root, backendName + ".getAllNestedFoosResponse",
        "CollectionResponse_FooCollectionCollection");
    verifyEmptyMethodRequest(root, backendName + ".getIntegers");
    verifyMethodResponseRef(root, backendName + ".getIntegers", "IntegerCollection");
    verifyEmptyMethodRequest(root, backendName + ".getObjectIntegers");
    verifyMethodResponseRef(root, backendName + ".getObjectIntegers", "IntegerCollection");
    verifyEmptyMethodRequest(root, backendName + ".getIntegersResponse");
    verifyMethodResponseRef(
        root, backendName + ".getIntegersResponse", "CollectionResponse_Integer");
    JsonNode arrayEndpoint = schemas.path("ArrayEndpoint");
    verifyArraySchemaRef(arrayEndpoint.path("properties").path("foos"), "Foo");
    verifyArraySchemaRef(arrayEndpoint.path("properties").path("allFoos"), "Foo", 2);
    verifyArraySchemaRef(arrayEndpoint.path("properties").path("arrayedFoos"), "Foo");
    verifyArraySchemaRef(arrayEndpoint.path("properties").path("allArrayedFoos"), "Foo", 2);
    verifyArraySchema(arrayEndpoint.path("properties").path("integers"), "integer");
    verifyArraySchema(arrayEndpoint.path("properties").path("objectIntegers"), "integer");
  }

  private void verifyApi(
      JsonNode root, String superConfig, String apiRoot, String apiName, String apiVersion,
      String description, String backendRoot, boolean allowCookieAuth, boolean defaultVersion) {
    verifyApi(root, superConfig, apiRoot, apiName, apiVersion, description,  backendRoot,
        allowCookieAuth, defaultVersion, null);
  }

  private void verifyApi(JsonNode root, String superConfig, String apiRoot, String apiName,
      String apiVersion, String description, String backendRoot, boolean allowCookieAuth,
      boolean defaultVersion, String[] blockedRegions) {
    assertEquals(superConfig, root.path("extends").asText());
    assertEquals(false, root.path("abstract").asBoolean());
    assertEquals(defaultVersion, root.path("defaultVersion").asBoolean());
    assertEquals(apiRoot, root.path("root").asText());
    assertEquals(apiName, root.path("name").asText());
    assertEquals(apiVersion, root.path("version").asText());
    assertEquals(description, root.path("description").asText());

    JsonNode adapter = root.path("adapter");
    assertEquals(backendRoot, adapter.path("bns").asText());

    JsonNode auth = root.path("auth");
    assertEquals(allowCookieAuth, auth.path("allowCookieAuth").asBoolean());

    if (blockedRegions != null) {
      ArrayNode blockedRegionsNode = (ArrayNode) auth.path("blockedRegions");
      for (int i = 0; i < blockedRegions.length; i++) {
        assertEquals(blockedRegions[i], blockedRegionsNode.get(i).asText());
      }
    }
  }

  private void verifyEndpoint3(JsonNode root) {
    JsonNode methods = root.path("methods");

    // myapi.listBars
    JsonNode methodListBars = methods.path("myapi.endpoint3.listBars");
    verifyMethod(methodListBars, "bar", HttpMethod.GET, Endpoint3.class.getName() + ".listBars",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(methodListBars.path("request"), "empty", 0);

    // myapi.getBar
    JsonNode methodGetBar = methods.path("myapi.endpoint3.getBar");
    verifyMethod(methodGetBar, "bar/{id}", HttpMethod.GET, Endpoint3.class.getName() + ".getBar",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(methodGetBar.path("request"), "empty", 1);
    verifyMethodRequestParameter(methodGetBar.path("request"), "id", "string", true, false);

    // myapi.insertBar
    JsonNode methodInsertBar = methods.path("myapi.endpoint3.insertBar");
    verifyMethod(methodInsertBar, "bar", HttpMethod.POST, Endpoint3.class.getName() + ".insertBar",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(
        methodInsertBar.path("request"), "autoTemplate(backendRequest)", 0);

    // myapi.updateBar
    JsonNode methodUpdateBar = methods.path("myapi.endpoint3.updateBar");
    verifyMethod(methodUpdateBar, "bar/{id}", HttpMethod.PUT,
        Endpoint3.class.getName() + ".updateBar", "autoTemplate(backendResponse)");
    verifyMethodRequest(
        methodUpdateBar.path("request"), "autoTemplate(backendRequest)", 1);
    verifyMethodRequestParameter(methodUpdateBar.path("request"), "id", "string", true, false);

    // myapi.removeBar
    JsonNode methodRemoveBar = methods.path("myapi.endpoint3.removeBar");
    verifyMethod(methodRemoveBar, "bar/{id}", HttpMethod.DELETE,
        Endpoint3.class.getName() + ".removeBar", "empty");
    verifyMethodRequest(methodRemoveBar.path("request"), "empty", 1);
    verifyMethodRequestParameter(methodRemoveBar.path("request"), "id", "string", true, false);
  }

  private void verifyEndpoint1(Class<? extends Endpoint1> serviceClass, JsonNode root) {
    verifyApi(root, "thirdParty.api", "https://myapp.appspot.com/_ah/api",
        "myapi", "v1", "API for testing", "https://myapp.appspot.com/_ah/spi", true, true,
        new String[]{"CU"});

    verifyFrontendLimits(root.path("frontendLimits"), 1, 2, 3);

    ArrayNode rules = (ArrayNode) root.path("frontendLimits").path("rules");
    verifyFrontendLimitRule(rules.get(0), "match0", 1, 2, 3, "analyticsId0");
    verifyFrontendLimitRule(rules.get(1), "match10", 11, 12, 13, "analyticsId10");

    verifyCacheControl(root.path("cacheControl"), ApiCacheControl.Type.PUBLIC, 1);

    JsonNode methods = root.path("methods");
    String serviceClassName = serviceClass.getName();

    // myapi.foos.listFoos
    JsonNode methodListFoos = methods.path("myapi.foos.list");
    verifyMethod(methodListFoos, "foos", HttpMethod.GET, serviceClassName + ".listFoos",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(methodListFoos.path("request"), "empty", 0);
    verifyStrings(objectMapper.convertValue(methodListFoos.path("scopes"), String[].class),
        new String[] {"s0", "s1 s2"});
    verifyStrings(objectMapper.convertValue(methodListFoos.path("audiences"), String[].class),
        new String[] {"a0", "a1"});
    verifyStrings(objectMapper.convertValue(methodListFoos.path("clientIds"), String[].class),
        new String[] {"c0", "c1"});

    // myapi.foos.get
    JsonNode methodGetFoo = methods.path("myapi.foos.get");
    verifyMethod(methodGetFoo, "foos/{id}", HttpMethod.GET, serviceClassName + ".getFoo",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(methodGetFoo.path("request"), "empty", 1);
    verifyMethodRequestParameter(methodGetFoo.path("request"), "id", "string", true, false);
    verifyStrings(objectMapper.convertValue(methodGetFoo.path("scopes"), String[].class),
        new String[] {"ss0", "ss1 ss2"});
    verifyStrings(objectMapper.convertValue(methodGetFoo.path("audiences"), String[].class),
        new String[] {"aa0", "aa1"});
    verifyStrings(objectMapper.convertValue(methodGetFoo.path("clientIds"), String[].class),
        new String[] {"cc0", "cc1"});

    // myapi.foos.insert
    JsonNode methodInsertFoo = methods.path("myapi.foos.insert");
    verifyMethod(methodInsertFoo, "foos", HttpMethod.POST, serviceClassName + ".insertFoo",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(
        methodInsertFoo.path("request"), "autoTemplate(backendRequest)", 0);

    // myapi.foos.update
    JsonNode methodUpdateFoo = methods.path("myapi.foos.update");
    verifyMethod(methodUpdateFoo, "foos/{id}", HttpMethod.PUT, serviceClassName + ".updateFoo",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(
        methodUpdateFoo.path("request"), "autoTemplate(backendRequest)", 1);
    verifyMethodRequestParameter(methodUpdateFoo.path("request"), "id", "string", true, false);

    // myapi.foos.remove
    JsonNode methodRemoveFoo = methods.path("myapi.foos.remove");
    verifyMethod(
        methodRemoveFoo, "foos/{id}", HttpMethod.DELETE, serviceClassName + ".removeFoo", "empty");
    verifyMethodRequest(methodRemoveFoo.path("request"), "empty", 1);
    verifyMethodRequestParameter(methodRemoveFoo.path("request"), "id", "string", true, false);

    // myapi.foos.execute0
    JsonNode methodExecute0 = methods.path("myapi.foos.execute0");
    verifyMethod(methodExecute0, "execute0", HttpMethod.POST, serviceClassName + ".execute0",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(methodExecute0.path("request"), "empty", 9);

    JsonNode methodExecute0Request = methodExecute0.path("request");
    verifyMethodRequestParameter(methodExecute0Request, "id", "string", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "i0", "int32", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "i1", "int32", false, false);
    verifyMethodRequestParameter(methodExecute0Request, "long0", "int64", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "long1", "int64", false, false);
    verifyMethodRequestParameter(methodExecute0Request, "b0", "boolean", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "b1", "boolean", false, false);
    verifyMethodRequestParameter(methodExecute0Request, "f", "float", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "d", "double", false, false);

    // myapi.foos.execute2
    JsonNode methodExecute2 = methods.path("myapi.foos.execute2");
    verifyMethod(methodExecute2, "execute2/{serialized}", HttpMethod.POST,
        serviceClassName + ".execute2", "empty");
    JsonNode methodExecute2Request = methodExecute2.path("request");
    verifyMethodRequestParameter(methodExecute2Request, "serialized", "string", true, false);
  }

  private void verifySubclassedOverridingEndpoint(
      Class<? extends SubclassedOverridingEndpoint> serviceClass, JsonNode root) {
    verifyApi(root, "thirdParty.api", "https://myapp.appspot.com/_ah/api",
        "myapi", "v2", "overridden description", "https://myapp.appspot.com/_ah/spi", true,
        false);

    verifyFrontendLimits(root.path("frontendLimits"), 1, 4, 3);

    ArrayNode rules = (ArrayNode) root.path("frontendLimits").path("rules");
    verifyFrontendLimitRule(rules.get(0), "match0", 1, 2, 3, "analyticsId0");
    verifyFrontendLimitRule(rules.get(1), "match10", 11, 12, 13, "analyticsId10");

    verifyCacheControl(root.path("cacheControl"), ApiCacheControl.Type.PUBLIC, 2);

    JsonNode methods = root.path("methods");
    String serviceClassName = serviceClass.getName();
    String servicePrefix = "myapi." + serviceClass.getSimpleName();

    // myapi.foos.listFoos
    JsonNode methodListFoos = methods.path("myapi.foos.list");
    verifyMethod(methodListFoos, "foos", HttpMethod.GET, serviceClassName + ".listFoos",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(methodListFoos.path("request"), "empty", 0);
    verifyStrings(objectMapper.convertValue(methodListFoos.path("scopes"), String[].class),
        new String[] {"s0", "s1 s2"});
    verifyStrings(objectMapper.convertValue(methodListFoos.path("audiences"), String[].class),
        new String[] {"a0", "a1"});
    verifyStrings(objectMapper.convertValue(methodListFoos.path("clientIds"), String[].class),
        new String[] {"c0", "c1"});

    // myapi.foos.get
    assertTrue(methods.path(servicePrefix + ".foos.get").isMissingNode());

    // myapi.foos.get2
    JsonNode methodGetFoo = methods.path("myapi.foos.get2");
    verifyMethod(methodGetFoo, "foos/{id}", HttpMethod.GET, serviceClassName + ".getFoo",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(methodGetFoo.path("request"), "empty", 1);
    verifyMethodRequestParameter(methodGetFoo.path("request"), "id", "string", true, false);
    verifyStrings(objectMapper.convertValue(methodGetFoo.path("scopes"), String[].class),
        new String[] {"ss0a", "ss1a"});
    verifyStrings(objectMapper.convertValue(methodGetFoo.path("audiences"), String[].class),
        new String[] {"aa0a", "aa1a"});
    verifyStrings(objectMapper.convertValue(methodGetFoo.path("clientIds"), String[].class),
        new String[] {"cc0a", "cc1a"});

    // myapi.foos.insert
    JsonNode methodInsertFoo = methods.path("myapi.foos.insert");
    verifyMethod(methodInsertFoo, "foos", HttpMethod.POST, serviceClassName + ".insertFoo",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(
        methodInsertFoo.path("request"), "autoTemplate(backendRequest)", 0);

    // myapi.foos.update
    JsonNode methodUpdateFoo = methods.path("myapi.foos.update");
    verifyMethod(methodUpdateFoo, "foos/{id}", HttpMethod.PUT, serviceClassName + ".updateFoo",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(
        methodUpdateFoo.path("request"), "autoTemplate(backendRequest)", 1);
    verifyMethodRequestParameter(methodUpdateFoo.path("request"), "id", "string", true, false);

    // myapi.foos.remove
    JsonNode methodRemoveFoo = methods.path("myapi.foos.remove");
    verifyMethod(methodRemoveFoo, "foos/{id}", HttpMethod.DELETE, serviceClassName + ".removeFoo",
        "empty");
    verifyMethodRequest(methodRemoveFoo.path("request"), "empty", 1);
    verifyMethodRequestParameter(methodRemoveFoo.path("request"), "id", "string", true, false);

    // myapi.foos.execute0
    JsonNode methodExecute0 = methods.path("myapi.foos.execute0");
    verifyMethod(methodExecute0, "execute0", HttpMethod.POST, serviceClassName + ".execute0",
        "autoTemplate(backendResponse)");
    verifyMethodRequest(methodExecute0.path("request"), "empty", 9);
    JsonNode methodExecute0Request = methodExecute0.path("request");
    verifyMethodRequestParameter(methodExecute0Request, "id", "string", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "i0", "int32", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "i1", "int32", false, false);
    verifyMethodRequestParameter(methodExecute0Request, "long0", "int64", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "long1", "int64", false, false);
    verifyMethodRequestParameter(methodExecute0Request, "b0", "boolean", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "b1", "boolean", false, false);
    verifyMethodRequestParameter(methodExecute0Request, "f", "float", true, false);
    verifyMethodRequestParameter(methodExecute0Request, "d", "double", false, false);

    // myapi.foos.execute2
    JsonNode methodExecute2 = methods.path("myapi.foos.execute2");
    verifyMethod(methodExecute2, "execute2/{serialized}", HttpMethod.POST,
        serviceClassName + ".execute2", "empty");
    JsonNode methodExecute2Request = methodExecute2.path("request");
    verifyMethodRequestParameter(methodExecute2Request, "serialized", "int32", true, false);
  }

  private void verifyFrontendLimits(JsonNode frontendLimits, int unregisteredUserQps,
      int unregisteredQps, int unregisteredDaily) {
    assertEquals(unregisteredUserQps, frontendLimits.path("unregisteredUserQps").asInt());
    assertEquals(unregisteredQps, frontendLimits.path("unregisteredQps").asInt());
    assertEquals(unregisteredDaily, frontendLimits.path("unregisteredDaily").asInt());
  }

  private void verifyFrontendLimitRule(
      JsonNode rule, String match, int qps, int userQps, int daily, String analyticsId) {
    assertEquals(match, rule.path("match").asText());
    assertEquals(qps, rule.path("qps").asInt());
    assertEquals(userQps, rule.path("userQps").asInt());
    assertEquals(daily, rule.path("daily").asInt());
    assertEquals(analyticsId, rule.path("analyticsId").asText());
  }

  private void verifyCacheControl(JsonNode cacheControl, String type, int maxAge) {
    assertEquals(type, cacheControl.path("type").asText());
    assertEquals(maxAge, cacheControl.path("maxAge").asInt());
  }

  private void verifyMethod(JsonNode method, String restPath, String httpMethod,
      String backendMethod, String responseBody) {
    assertFalse(method.isMissingNode());

    assertEquals(restPath, method.path("path").asText());
    assertEquals(httpMethod, method.path("httpMethod").asText());
    assertEquals(backendMethod, method.path("rosyMethod").asText());

    JsonNode response = method.path("response");
    assertEquals(responseBody, response.path("body").asText());
  }

  private void verifyMethodRequest(JsonNode request, String requestBody, int parameterCount) {
    assertEquals(requestBody, request.path("body").asText());
    if (!requestBody.equals("empty")) {
      assertEquals("resource", request.path("bodyName").asText());
    }
    JsonNode parameters = request.path("parameters");
    assertEquals(parameterCount, parameters.size());
  }

  private void verifyMethodRequestParameter(JsonNode request, String parameterName, String type,
      boolean required, boolean repeated, String... enumValues) {
    assertFalse(request.isMissingNode());

    JsonNode parameters = request.path("parameters");
    JsonNode parameter = parameters.path(parameterName);
    assertEquals(required, parameter.path("required").asBoolean());
    assertEquals(repeated, parameter.path("repeated").asBoolean());
    assertEquals(type, parameter.path("type").asText());
    if (enumValues.length > 0) {
      JsonNode enumValueNodes = parameter.path("enum");
      assertFalse(enumValueNodes.isMissingNode());
      assertEquals(enumValues.length, enumValueNodes.size());
      for (String enumValue : enumValues) {
        assertNotNull(enumValueNodes.get(enumValue));
      }
    }
  }

  private void verifyObjectSchema(JsonNode schema, String id, String type) {
    assertEquals(id, schema.path("id").asText());
    assertEquals(type, schema.path("type").asText());
  }

  private void verifyObjectPropertySchema(JsonNode schema, String name, String type) {
    assertEquals(type, schema.path("properties").path(name).path("type").asText());
  }

  private void verifyObjectPropertySchema(
      JsonNode schema, String name, String type, String format) {
    assertEquals(type, schema.path("properties").path(name).path("type").asText());
    assertEquals(format, schema.path("properties").path(name).path("format").asText());
  }

  private void verifyObjectPropertyRef(JsonNode schema, String name, String ref) {
    assertEquals(ref, schema.path("properties").path(name).path("$ref").asText());
  }

  private void verifyArraySchema(JsonNode array, String itemType) {
    verifyArraySchema(array, itemType, 1);
  }

  private void verifyArraySchema(JsonNode array, String itemType, int dimensions) {
    for (int i = 0; i < dimensions; i++) {
      assertEquals("array", array.path("type").asText());
      array = array.path("items");
    }
    assertEquals(itemType, array.path("type").asText());
  }

  private void verifyArraySchemaRef(JsonNode array, String itemType) {
    verifyArraySchemaRef(array, itemType, 1);
  }

  private void verifyArraySchemaRef(JsonNode array, String itemType, int dimensions) {
    for (int i = 0; i < dimensions; i++) {
      assertEquals("array", array.path("type").asText());
      array = array.path("items");
    }
    assertEquals(itemType, array.path("$ref").asText());
  }

  private void verifyEmptyMethodRequest(JsonNode root, String backendMethodName) {
    verifyMethodRequest(root, backendMethodName, "", "");
    verifyMethodRequestRef(root, backendMethodName, "");
  }

  private void verifyMethodRequest(
      JsonNode root, String backendMethodName, String requestType, String requestFormat) {
    JsonNode request =
        root.path("descriptor").path("methods").path(backendMethodName).path("request");
    assertEquals(requestType, request.path("type").asText());
    assertEquals(requestFormat, request.path("format").asText());
  }

  private void verifyMethodRequestRef(JsonNode root, String backendMethodName, String requestType) {
    JsonNode request =
        root.path("descriptor").path("methods").path(backendMethodName).path("request");
    assertEquals(requestType, request.path("$ref").asText());
  }

  private void verifyEmptyMethodResponse(JsonNode root, String backendMethodName) {
    verifyMethodResponse(root, backendMethodName, "", "");
    verifyMethodResponseRef(root, backendMethodName, "");
  }

  private void verifyMethodResponse(
      JsonNode root, String backendMethodName, String responseType, String responseFormat) {
    JsonNode response =
        root.path("descriptor").path("methods").path(backendMethodName).path("response");
    assertEquals(responseType, response.path("type").asText());
    assertEquals(responseFormat, response.path("format").asText());
  }

  private void verifyMethodResponseRef(
      JsonNode root, String backendMethodName, String responseType) {
    JsonNode response =
        root.path("descriptor").path("methods").path(backendMethodName).path("response");
    assertEquals(responseType, response.path("$ref").asText());
  }

  private void verifyEndpoint0Schema(JsonNode root, String className) {
    JsonNode schemas = root.path("descriptor").path("schemas");
    JsonNode foo = schemas.path("Foo");
    verifyObjectSchema(foo, "Foo", "object");
    verifyObjectPropertySchema(foo, "name", "string");
    verifyObjectPropertySchema(foo, "value", "integer");

    verifyEmptyMethodRequest(root, className + ".getFoo");
    verifyMethodResponseRef(root, className + ".getFoo", "Foo");
  }

  private void verifyEndpoint1Schema(Class<? extends Endpoint1> serviceClass, JsonNode root) {
    JsonNode schemas = root.path("descriptor").path("schemas");
    JsonNode foo = schemas.path("Foo");
    verifyObjectSchema(foo, "Foo", "object");
    verifyObjectPropertySchema(foo, "name", "string");
    verifyObjectPropertySchema(foo, "value", "integer");
    JsonNode fooCollection = schemas.path("FooCollection");
    verifyArraySchemaRef(fooCollection.path("properties").path("items"), "Foo");

    String serviceClassName = serviceClass.getName();
    verifyEmptyMethodRequest(root, serviceClassName + ".listFoos");
    verifyMethodResponseRef(root, serviceClassName + ".listFoos", "FooCollection");
    verifyEmptyMethodRequest(root, serviceClassName + ".getFoo");
    verifyMethodResponseRef(root, serviceClassName + ".getFoo", "Foo");
    verifyMethodRequestRef(root, serviceClassName + ".insertFoo", "Foo");
    verifyMethodResponseRef(root, serviceClassName + ".insertFoo", "Foo");
    verifyMethodRequestRef(root, serviceClassName + ".updateFoo", "Foo");
    verifyMethodResponseRef(root, serviceClassName + ".updateFoo", "Foo");
    verifyEmptyMethodRequest(root, serviceClassName + ".removeFoo");
    verifyEmptyMethodResponse(root, serviceClassName + ".removeFoo");
    verifyEmptyMethodRequest(root, serviceClassName + ".execute0");
    verifyMethodResponseRef(
        root, serviceClassName + ".execute0", JsonConfigWriter.ANY_SCHEMA_NAME);
    verifyMethodRequestRef(root, serviceClassName + ".execute1", "Foo");
    verifyMethodResponseRef(
        root, serviceClassName + ".execute1", JsonConfigWriter.MAP_SCHEMA_NAME);
  }

  private void verifyEndpoint3Schema(JsonNode root) {
    JsonNode schemas = root.path("descriptor").path("schemas");
    JsonNode foo = schemas.path("Bar");
    verifyObjectSchema(foo, "Bar", "object");
    verifyObjectPropertySchema(foo, "name", "string");
    verifyObjectPropertySchema(foo, "value", "integer");
    JsonNode foos = schemas.path("BarCollection");
    verifyArraySchemaRef(foos.path("properties").path("items"), "Bar");

    verifyEmptyMethodRequest(root, Endpoint3.class.getName() + ".listBars");
    verifyMethodResponseRef(root, Endpoint3.class.getName() + ".listBars", "BarCollection");
    verifyEmptyMethodRequest(root, Endpoint3.class.getName() + ".getBar");
    verifyMethodResponseRef(root, Endpoint3.class.getName() + ".getBar", "Bar");
    verifyMethodRequestRef(root, Endpoint3.class.getName() + ".insertBar", "Bar");
    verifyMethodResponseRef(root, Endpoint3.class.getName() + ".insertBar", "Bar");
    verifyMethodRequestRef(root, Endpoint3.class.getName() + ".updateBar", "Bar");
    verifyMethodResponseRef(root, Endpoint3.class.getName() + ".updateBar", "Bar");
    verifyEmptyMethodRequest(root, Endpoint3.class.getName() + ".removeBar");
    verifyEmptyMethodResponse(root, Endpoint3.class.getName() + ".removeBar");
  }

  private void verifyEndpoint5Schema(JsonNode root) {
    JsonNode schemas = root.path("descriptor").path("schemas");
    JsonNode foo = schemas.path("Foo");
    verifyObjectSchema(foo, "Foo", "object");
    verifyObjectPropertySchema(foo, "name", "string");
    verifyObjectPropertySchema(foo, "value", "integer");

    JsonNode baz = schemas.path("Baz");
    verifyObjectSchema(baz, "Baz", "object");
    verifyObjectPropertyRef(baz, "foo", "Foo");
    verifyArraySchemaRef(baz.path("properties").path("foos"), "Foo");

    verifyEmptyMethodRequest(root, Endpoint5.class.getName() + ".getBaz");
    verifyMethodResponseRef(root, Endpoint5.class.getName() + ".getBaz", "Baz");
  }

  private void verifySubclassedOverridingEndpointSchema(
      Class<? extends SubclassedOverridingEndpoint> serviceClass, JsonNode root) {
    JsonNode schemas = root.path("descriptor").path("schemas");
    JsonNode foo = schemas.path("Foo");
    verifyObjectSchema(foo, "Foo", "object");
    verifyObjectPropertySchema(foo, "name", "string");
    verifyObjectPropertySchema(foo, "value", "integer");
    JsonNode fooCollection = schemas.path("FooCollection");
    verifyArraySchemaRef(fooCollection.path("properties").path("items"), "Foo");

    String serviceClassName = serviceClass.getName();
    verifyEmptyMethodRequest(root, serviceClassName + ".listFoos");
    verifyMethodResponseRef(root, serviceClassName + ".listFoos", "FooCollection");
    verifyEmptyMethodRequest(root, serviceClassName + ".getFoo");
    verifyMethodResponseRef(root, serviceClassName + ".getFoo", "Foo");
    verifyMethodRequestRef(root, serviceClassName + ".insertFoo", "Foo");
    verifyMethodResponseRef(root, serviceClassName + ".insertFoo", "Foo");
    verifyMethodRequestRef(root, serviceClassName + ".updateFoo", "Foo");
    verifyMethodResponseRef(root, serviceClassName + ".updateFoo", "Foo");
    verifyEmptyMethodRequest(root, serviceClassName + ".removeFoo");
    verifyEmptyMethodResponse(root, serviceClassName + ".removeFoo");
    verifyEmptyMethodRequest(root, serviceClassName + ".execute0");
    verifyMethodResponseRef(
        root, serviceClassName + ".execute0", JsonConfigWriter.ANY_SCHEMA_NAME);
    verifyMethodRequestRef(root, serviceClassName + ".execute1", "Foo");
    verifyMethodResponseRef(
        root, serviceClassName + ".execute1", JsonConfigWriter.MAP_SCHEMA_NAME);
  }

  private enum Outcome {
    WON, LOST, TIE
  }

  @SuppressWarnings("unused")
  private static class Bean {
    public Date getDate() {
      return null;
    }

    public void setDate(Date date) {}
  }

  @Test
  public void testValidDateInParameter() throws Exception {
    @Api
    class DateParameter {
      @SuppressWarnings("unused")
      public void foo(@Named("date") Date date) { }
    }
    String apiConfigSource = g.generateConfig(DateParameter.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    JsonNode request = root.path("methods").path("myapi.dateParameter.foo").path("request");
    verifyMethodRequestParameter(request, "date", "datetime", true, false);
    assertTrue(root.path("descriptor").path("schemas").path("Outcome").isMissingNode());
    verifyEmptyMethodRequest(root, DateParameter.class.getName() + ".pick");
  }

  @Test
  public void testDateCollection() throws Exception {
    @Api
    class DateParameters {
      @SuppressWarnings("unused")
      public void foo(@Named("date") Date date, @Named("dates1") Collection<Date> dates1,
          @Named("dates2") Date[] dates2) { }
    }
    String apiConfigSource = g.generateConfig(DateParameters.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    JsonNode request = root.path("methods").path("myapi.dateParameters.foo").path("request");
    verifyMethodRequestParameter(request, "date", "datetime", true, false);
    verifyMethodRequestParameter(request, "dates1", "datetime", true, true);
    verifyMethodRequestParameter(request, "dates2", "datetime", true, true);
    verifyEmptyMethodRequest(root, DateParameters.class.getName() + ".foo");
  }

  @Test
  public void testInvalidDateInEndpointRequest() throws Exception {
    @Api
    class DateParameter {
      @SuppressWarnings("unused")
      public void foo(Date date) {}
    }

    try {
      g.generateConfig(DateParameter.class).get("myapi-v1.api");
      fail("Dates should not be treated as resources");
    } catch (MissingParameterNameException expected) {
      // expected
    }
  }

  private static class EnumBean {
    @SuppressWarnings("unused")
    public Outcome getOutcome() {
      return null;
    }
  }

  @Test
  public void testValidEnumInParameter() throws Exception {
    @Api
    class EnumParameter {
      @SuppressWarnings("unused")
      public void pick(@Named("outcome") Outcome outcome) { }
    }
    String apiConfigSource = g.generateConfig(EnumParameter.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    JsonNode request = root.path("methods").path("myapi.enumParameter.pick").path("request");
    verifyMethodRequestParameter(request, "outcome", "string", true, false, "WON", "LOST", "TIE");
    assertTrue(root.path("descriptor").path("schemas").path("Outcome").isMissingNode());
    verifyEmptyMethodRequest(root, EnumParameter.class.getName() + ".pick");
  }

  @Test
  public void testValidEnumInParameterAndResource() throws Exception {
    @Api
    class EnumParameter {
      @SuppressWarnings("unused")
      public void pick(@Named("outcomeParam") Outcome outcome, EnumBean date) { }
    }

    String apiConfigSource = g.generateConfig(EnumParameter.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    JsonNode outcomeSchema = root.path("descriptor").path("schemas").path("Outcome");
    assertEquals("Outcome", outcomeSchema.path("id").asText());
    assertEquals("string", outcomeSchema.path("type").asText());
    JsonNode enumConfig = outcomeSchema.path("enum");
    assertTrue(enumConfig.isArray());
    assertEquals(3, enumConfig.size());
    assertEquals(Outcome.WON.toString(), enumConfig.get(0).asText());
    assertEquals(Outcome.LOST.toString(), enumConfig.get(1).asText());
    assertEquals(Outcome.TIE.toString(), enumConfig.get(2).asText());
    JsonNode enumSchema = root.path("descriptor").path("schemas").path("EnumBean");
    assertEquals("EnumBean", enumSchema.path("id").asText());
    assertEquals("object", enumSchema.path("type").asText());
    assertNotNull(enumSchema.get("properties"));
    assertNotNull(enumSchema.path("properties").path("outcome").get("$ref"));
    assertEquals("Outcome", enumSchema.path("properties").path("outcome").path("$ref").asText());

    JsonNode methodSchema = root.path("descriptor").path("methods").path(
        EnumParameter.class.getName() + ".pick");
    assertNotNull(methodSchema.get("request"));
    assertNotNull(methodSchema.path("request").get("$ref"));
    assertEquals("EnumBean", methodSchema.path("request").get("$ref").asText());
  }

  /**
   * A {@code Bar} resource serializer with a property of type {@code Baz}.
   */
  protected static class BarResourceSerializer
      extends DefaultValueSerializer<Bar, Map<String, Object>> implements ResourceTransformer<Bar> {
    public BarResourceSerializer() {
    }
    @Override
    public ResourceSchema getResourceSchema() {
      return ResourceSchema.builderForType(Bar.class)
          .addProperty("someBaz", ResourcePropertySchema.of(TypeToken.of(Baz.class)))
          .build();
    }
  }

  @Test
  public void testSerializedPropertyInResourceSchema() throws Exception {
    class BazToDateSerializer extends DefaultValueSerializer<Baz, Date> {
    }
    @Api(transformers = {BazToDateSerializer.class, BarResourceSerializer.class})
    class BarEndpoint {
      @SuppressWarnings("unused")
      public Bar getBar() {
        return null;
      }
    }
    String apiConfigSource = g.generateConfig(BarEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode bar = root.path("descriptor").path("schemas").path("Bar");
    verifyObjectPropertySchema(bar, "someBaz", "string", "date-time");
  }

  @Test
  public void testChainedSerializer() throws Exception {
    class BarToBazSerializer extends DefaultValueSerializer<Bar, Baz> {
    }
    class BazToDateSerializer extends DefaultValueSerializer<Baz, Date> {
    }
    class Qux {
      @SuppressWarnings("unused")
      public Bar getSomeBar() {
        return null;
      }
    }
    @Api(transformers = {BazToDateSerializer.class, BarToBazSerializer.class})
    class QuxEndpoint {
      @SuppressWarnings("unused")
      public Qux getQux() {
        return null;
      }
    }
    String apiConfigSource = g.generateConfig(QuxEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode qux = root.path("descriptor").path("schemas").path("Qux");
    verifyObjectPropertySchema(qux, "someBar", "string", "date-time");
  }

  @Test
  public void testSerializedEnum() throws Exception {
    class OutcomeToIntegerSerializer extends DefaultValueSerializer<Outcome, Integer> {
    }
    @Api(transformers = {OutcomeToIntegerSerializer.class})
    class EnumParameter {
      @SuppressWarnings("unused")
      public void foo(@Named("outcome") Outcome outcome) { }
    }
    String apiConfigSource = g.generateConfig(EnumParameter.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    JsonNode request = root.path("methods").path("myapi.enumParameter.foo").path("request");
    verifyMethodRequestParameter(request, "outcome", "int32", true, false);
  }

  @Test
  public void testNonSerializedEnumShouldAlwaysBeString() throws Exception {
    class StringToIntegerSerializer extends DefaultValueSerializer<String, Integer> {
    }
    @Api(transformers = {StringToIntegerSerializer.class})
    class EnumParameter {
      @SuppressWarnings("unused")
      public void foo(@Named("outcome") Outcome outcome) { }
    }
    String apiConfigSource = g.generateConfig(EnumParameter.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    JsonNode request = root.path("methods").path("myapi.enumParameter.foo").path("request");
    verifyMethodRequestParameter(request, "outcome", "string", true, false, "WON", "LOST", "TIE");
    assertTrue(root.path("descriptor").path("schemas").path("Outcome").isMissingNode());
    verifyEmptyMethodRequest(root, EnumParameter.class.getName() + ".pick");
  }

  @Test
  public void testEnumCollection() throws Exception {
    @Api
    class EnumParameters {
      @SuppressWarnings("unused")
      public void foo(@Named("outcome") Outcome outcome,
          @Named("outcomes1") Collection<Outcome> outcomes1,
          @Named("outcomes2") Outcome[] outcomes2) { }
    }
    String apiConfigSource = g.generateConfig(EnumParameters.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    JsonNode request = root.path("methods").path("myapi.enumParameters.foo").path("request");
    verifyMethodRequestParameter(request, "outcome", "string", true, false, "WON", "LOST", "TIE");
    verifyMethodRequestParameter(request, "outcomes1", "string", true, true, "WON", "LOST", "TIE");
    verifyMethodRequestParameter(request, "outcomes2", "string", true, true, "WON", "LOST", "TIE");
    assertTrue(root.path("descriptor").path("schemas").path("Outcome").isMissingNode());
    verifyEmptyMethodRequest(root, EnumParameters.class.getName() + ".foo");
  }

  @Test
  public void testInvalidEnumInEndpointRequest() throws Exception {
    @Api
    class EnumParameter {
      @SuppressWarnings("unused")
      public void pick(Outcome outcome) {}
    }

    try {
      g.generateConfig(EnumParameter.class).get("myapi-v1.api");
      fail("Enums should not be treated as resources");
    } catch (MissingParameterNameException expected) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  @Api
  private static class Endpoint {
    public void remove(@Named("id") String id) {}

    public void delete(@Named("id") String id) {}
  }

  @Test
  public void testBareRemoveAndDelete() throws Exception {
    String apiConfigSource = g.generateConfig(Endpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    // if we have no other clue, we should just use "remove" or "delete" as the REST path
    assertEquals(
        "remove/{id}", root.path("methods").path("myapi.endpoint.remove").path("path").asText());
    assertEquals(
        "delete/{id}", root.path("methods").path("myapi.endpoint.delete").path("path").asText());
  }

  @Api
  static class TestEndpoint {
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

  @Test
  public void testOptionalParameters() throws Exception {
    String apiConfigSource = g.generateConfig(TestEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    assertEquals("foo/{required}",
        root.path("methods").path("myapi.testEndpoint.getItem").path("path").asText());
  }

  private void verifyStrings(String[] a0, String[] a1) {
    assertEquals(a0.length, a1.length);
    for (int i = 0; i < a0.length; i++) {
      assertEquals(a0[i], a1[i]);
    }
  }

  @Test
  public void testCollectionResponse() throws Exception {
    String apiConfigSource = g.generateConfig(TestEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    // regular collection response
    assertEquals("bar", root.path("methods").path("myapi.testEndpoint.list").path("path").asText());
    assertNotNull(root.path("descriptor").path("schemas").path("Bar"));
    assertNotNull(root.path("descriptor").path("schemas").path("BarCollection"));
    assertEquals("array", root.path("descriptor").path("schemas").path("BarCollection")
        .path("properties").path("items").path("type").asText());
    assertEquals("Bar", root.path("descriptor").path("schemas").path("BarCollection")
        .path("properties").path("items").path("items").path("$ref").asText());

    String backendName = TestEndpoint.class.getName();
    // specific CollectionResponse
    assertEquals("baz", root.path("methods").path("myapi.testEndpoint.listWithPagination")
        .path("path").asText());
    assertEquals("CollectionResponse_Baz", root.path("descriptor").path("methods")
        .path(backendName + ".listWithPagination").path("response").path("$ref").asText());
    assertNotNull(root.path("descriptor").path("schemas").path("Baz"));
    assertNotNull(root.path("descriptor").path("schemas").path("CollectionResponse_Baz"));
    assertEquals("array", root.path("descriptor").path("schemas").path("CollectionResponse_Baz")
        .path("properties").path("items").path("type").asText());
    assertEquals("Baz", root.path("descriptor").path("schemas").path("CollectionResponse_Baz")
        .path("properties").path("items").path("items").path("$ref").asText());
    assertEquals("string", root.path("descriptor").path("schemas").path("CollectionResponse_Baz")
        .path("properties").path("nextPageToken").path("type").asText());
    assertEquals("CollectionResponse_Baz", root.path("descriptor").path("methods")
        .path(backendName + ".listWithPagination").path("response").path("$ref").asText());

    // subclass of CollectionResponse
    assertEquals("foo", root.path("methods").path("myapi.testEndpoint.listWithMyCollectionResponse")
        .path("path").asText());
    assertEquals("MyCollectionResponse_Foo", root.path("descriptor").path("methods")
        .path(backendName + ".listWithMyCollectionResponse").path("response")
        .path("$ref").asText());
    assertNotNull(root.path("descriptor").path("schemas").path("Foo"));
    assertNotNull(root.path("descriptor").path("schemas").path("MyCollectionResponse_Foo"));
    assertEquals("array", root.path("descriptor").path("schemas").path("MyCollectionResponse_Foo")
        .path("properties").path("items").path("type").asText());
    assertEquals("Foo", root.path("descriptor").path("schemas").path("MyCollectionResponse_Foo")
        .path("properties").path("items").path("items").path("$ref").asText());
    assertEquals("string", root.path("descriptor").path("schemas").path("MyCollectionResponse_Foo")
        .path("properties").path("nextPageToken").path("type").asText());
    assertEquals("MyCollectionResponse_Foo", root.path("descriptor").path("methods")
        .path(backendName + ".listWithMyCollectionResponse").path("response")
        .path("$ref").asText());
  }

  @Test
  public void testAbstract() throws Exception {
    @Api(isAbstract = AnnotationBoolean.TRUE)
    class Test {}

    String apiConfigSource = g.generateConfig(Test.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    assertTrue(root.get("abstract").asBoolean());
  }

  @Test
  public void testRepeatedParameterName() throws Exception {
    @Api
    class Test {
      @ApiMethod(path = "path")
      public void foo(@Named("id") String id1, @Named("id") Long id2) {}
    }

    try {
      g.generateConfig(Test.class).get("myapi-v1.api");
      fail("Config generation for endpoint with two parameters named the same should have failed.");
    } catch (DuplicateParameterNameException expected) {
      // expected
    }
  }

  @Test
  public void testNullablePathParameter() throws Exception {
    @Api
    class Test {
      @ApiMethod(path = "path/{id}")
      public void foo(@Named("id") @Nullable String id) {}
    }

    try {
      g.generateConfig(Test.class);
      fail("Config generation for endpoint with nullable path parameter should have failed.");
    } catch (InvalidParameterAnnotationsException expected) {
      // expected
    }
  }

  @Api
  private static class DefaultValuedPathParameterEndpoint<T> {
    @ApiMethod(path = "path/{id}")
    public void foo(@Named("id") @DefaultValue("bar") T id) {}
  }

  @Test
  public void testDefaultValuedPathParameterBoolean() throws Exception {
    final class Test extends DefaultValuedPathParameterEndpoint<Boolean> {}

    try {
      g.generateConfig(Test.class);
      fail("Config generation for endpoint with default-valued path parameter should have failed.");
    } catch (InvalidParameterAnnotationsException expected) {
      // expected
    }
  }

  @Test
  public void testDefaultValuedPathParameterInteger() throws Exception {
    final class Test extends DefaultValuedPathParameterEndpoint<Integer> {}

    try {
      g.generateConfig(Test.class);
      fail("Config generation for endpoint with default-valued path parameter should have failed.");
    } catch (InvalidParameterAnnotationsException expected) {
      // expected
    }
  }

  @Test
  public void testDefaultValuedPathParameterLong() throws Exception {
    final class Test extends DefaultValuedPathParameterEndpoint<Long> {}

    try {
      g.generateConfig(Test.class);
      fail("Config generation for endpoint with default-valued path parameter should have failed.");
    } catch (InvalidParameterAnnotationsException expected) {
      // expected
    }
  }

  @Test
  public void testDefaultValuedPathParameterString() throws Exception {
    final class Test extends DefaultValuedPathParameterEndpoint<String> {}

    try {
      g.generateConfig(Test.class);
      fail("Config generation for endpoint with default-valued path parameter should have failed.");
    } catch (InvalidParameterAnnotationsException expected) {
      // expected
    }
  }

  @Api
  private static class DefaultValuedEndpoint<T> {
    @SuppressWarnings("unused")
    public void foo(T id) {}
  }

  @Test
  public void testValidDefaultValuedParameterBoolean() throws Exception {
    final class Test extends DefaultValuedEndpoint<Boolean> {
      @Override public void foo(@Named("id") @DefaultValue("true") Boolean id) {}
    }
    assertEquals(true, implValidTestDefaultValuedParameter(Test.class).asBoolean());
  }

  @Test
  public void testValidDefaultValuedParameterInteger() throws Exception {
    final class Test extends DefaultValuedEndpoint<Integer> {
      @Override public void foo(@Named("id") @DefaultValue("2718") Integer id) {}
    }
    assertEquals(2718, implValidTestDefaultValuedParameter(Test.class).asInt());
  }

  @Test
  public void testValidDefaultValuedParameterLong() throws Exception {
    final class Test extends DefaultValuedEndpoint<Long> {
      @Override public void foo(@Named("id") @DefaultValue("3141") Long id) {}
    }
    assertEquals(3141L, implValidTestDefaultValuedParameter(Test.class).asLong());
  }

  @Test
  public void testValidDefaultValuedParameterString() throws Exception {
    final class Test extends DefaultValuedEndpoint<String> {
      @Override public void foo(@Named("id") @DefaultValue("bar") String id) {}
    }
    assertEquals("bar", implValidTestDefaultValuedParameter(Test.class).asText());
  }

  private <T> JsonNode implValidTestDefaultValuedParameter(
      Class<? extends DefaultValuedEndpoint<T>> clazz) throws Exception {
    String apiConfigSource = g.generateConfig(clazz).get("myapi-v1.api");

    JsonNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    JsonNode methodFoo = root.path("methods").path("myapi.test.foo");
    JsonNode parameters = methodFoo.path("request").path("parameters");
    JsonNode parameter = parameters.path("id");
    JsonNode defaultValue = parameter.path("default");

    assertFalse(parameter.path("required").isMissingNode());
    assertEquals(false, parameter.path("required").asBoolean());
    assertFalse(defaultValue.isMissingNode());

    return defaultValue;
  }

  @Test
  public void testInvalidDefaultValuedParameterBoolean() throws Exception {
    implInvalidTestDefaultValuedParameter(new DefaultValuedEndpoint<Boolean>() {
      @Override public void foo(@Named("id") @DefaultValue("bar") Boolean id) {}
    }.getClass());
  }

  @Test
  public void testInvalidDefaultValuedParameterInteger() throws Exception {
    implInvalidTestDefaultValuedParameter(new DefaultValuedEndpoint<Integer>() {
      @Override public void foo(@Named("id") @DefaultValue("bar") Integer id) {}
    }.getClass());
  }

  @Test
  public void testInvalidDefaultValuedParameterLong() throws Exception {
    implInvalidTestDefaultValuedParameter(new DefaultValuedEndpoint<Long>() {
      @Override public void foo(@Named("id") @DefaultValue("bar") Long id) {}
    }.getClass());
  }

  private <T> void implInvalidTestDefaultValuedParameter(
      Class<? extends DefaultValuedEndpoint<T>> clazz) throws Exception {
    try {
      g.generateConfig(clazz);
      fail("Config generation for endpoint with bad default for given type should have failed.");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Api
  class SameRestPathDifferentType<T1, T2> {
    @SuppressWarnings("unused")
    public List<Bar> list(@Named("id") T1 id) { return null; }
    @SuppressWarnings("unused")
    public Bar get(@Named("id") T2 id) { return null; }
  }

  @Test
  public void testSameRestPathStringString() throws Exception {
    implSameRestPathDifferentTypeTest(
        new SameRestPathDifferentType<String, String>() {}.getClass());
  }

  @Test
  public void testSameRestPathIntegerLong() throws Exception {
    implSameRestPathDifferentTypeTest(
        new SameRestPathDifferentType<Long, String>() {}.getClass());
  }

  @Test
  public void testSameRestPathIntegerString() throws Exception {
    implSameRestPathDifferentTypeTest(
        new SameRestPathDifferentType<Integer, String>() {}.getClass());
  }

  @Test
  public void testSameRestPathIntegerBoolean() throws Exception {
    implSameRestPathDifferentTypeTest(
        new SameRestPathDifferentType<Integer, Boolean>() {}.getClass());
  }

  private <T1, T2> void implSameRestPathDifferentTypeTest(
      Class<? extends SameRestPathDifferentType<T1, T2>> clazz) throws Exception {
    try {
      g.generateConfig(clazz).get("myapi-v1.api");
      fail("Multiple methods with same RESTful signature");
    } catch (DuplicateRestPathException expected) {
      // expected
    }
  }

  /**
   * Test if the generated API has the expected method paths. First try this out without any
   * explicit paths.
   */
  public void testMethodPaths_repeatRestPath() throws Exception {
    @SuppressWarnings("unused")
    @Api
    class Dates {
      @ApiMethod(httpMethod = "GET", path = "dates/{id1}/{id2}")
      public Date get(@Named("id1") String id1, @Named("id2") String id2) { return null; }

      @ApiMethod(httpMethod = "GET", path = "dates/{x}/{y}")
      public List<Date> listFromXY(@Named("x") String x, @Named("y") String y) { return null; }
    }
    try {
      g.generateConfig(Dates.class).get("myapi-v1.api");
      fail("Multiple methods with same RESTful signature");
    } catch (DuplicateRestPathException expected) {
      // expected
    }
  }

  @Test
  public void testMethodPaths_apiAtCustomPath() throws Exception {
    @SuppressWarnings("unused")
    @Api(resource = "foos")
    class Foos {
      public List<Foo> list() { return null; }
      public Foo get(@Named("id") Long id) { return null; }
    }
    String apiConfigSource = g.generateConfig(Foos.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    verifyMethodPathAndHttpMethod(root, "myapi.foos.list", "foos", "GET");
    verifyMethodPathAndHttpMethod(root, "myapi.foos.get", "foos/{id}", "GET");
  }

  @Test
  public void testMethodPaths_apiAndMethodsAtCustomPaths() throws Exception {
    @Api(resource = "foo")
    class AcmeCo {
      @ApiMethod(path = "foos")
      public List<Foo> list() { return null; }
      @SuppressWarnings("unused")
      public List<Foo> listAllTheThings() { return null; }
      @ApiMethod(path = "give")
      public Foo giveMeOne() { return null; }
    }
    String apiConfigSource = g.generateConfig(AcmeCo.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    verifyMethodPathAndHttpMethod(root, "myapi.foo.list", "foos", "GET");
    verifyMethodPathAndHttpMethod(root, "myapi.foo.listAllTheThings", "foo", "GET");
    verifyMethodPathAndHttpMethod(root, "myapi.foo.giveMeOne", "give", "POST");
  }

  private static void verifyMethodPathAndHttpMethod(
      JsonNode root, String methodName, String expectedPath, String expectedHttpMethod) {
    JsonNode methodNode = root.path("methods").path(methodName);
    assertEquals(expectedPath, methodNode.path("path").asText());
    assertEquals(expectedHttpMethod, methodNode.path("httpMethod").asText());
  }

  /**
   * A class for testing inheritance in Endpoints.
   */
  abstract static class GenFoos<T> {
    public List<T> list() {
      return null;
    }
    public T get(@Named("id") long id) {
      return null;
    }
    public T insert(T object) {
      return null;
    }
    public T update(T updated) {
      return null;
    }
    public void remove(@Named("id") long id) {
      // empty
    }

    private static void verifyMethodPathsAndHttpMethods(
        Class<? extends GenFoos<?>> serviceClass, String path, JsonNode root, String resource) {
      String classPart = resource != null ? resource : serviceClass.getSimpleName();
      String servicePrefix = ApiMethodConfig.methodNameFormatter("myapi." + classPart);
      verifyMethodPathAndHttpMethod(root, servicePrefix + ".insert", path, "POST");
      verifyMethodPathAndHttpMethod(root, servicePrefix + ".update", path, "PUT");
      verifyMethodPathAndHttpMethod(root, servicePrefix + ".list", path, "GET");
      verifyMethodPathAndHttpMethod(root, servicePrefix + ".remove", path + "/{id}", "DELETE");
      verifyMethodPathAndHttpMethod(root, servicePrefix + ".get", path + "/{id}", "GET");
    }
  }

  /**
   * A class for testing inheritance in Endpoints with resource specified.
   */
  @Api(resource = "bars")
  abstract static class GenBars<T> extends GenFoos<T> {
    private static void verifyMethodPathsAndHttpMethods(
        Class<? extends GenBars<?>> serviceClass, JsonNode root) {
      GenFoos.verifyMethodPathsAndHttpMethods(serviceClass, "bars", root, "bars");
    }
  }

  static class Foo {}

  @Test
  public void testMethodPaths_inheritanceResourceSpecified() throws Exception {
    @Api(resource = "foos")
    class Foos extends GenFoos<Foo> {}
    String apiConfigSource = g.generateConfig(Foos.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    GenFoos.verifyMethodPathsAndHttpMethods(Foos.class, "foos", root, "foos");
  }

  @Test
  public void testMethodPaths_inheritanceResourceInheritedAndSpecified() throws Exception {
    @Api(resource = "foos")
    class Foos extends GenBars<Foo> {}
    String apiConfigSource = g.generateConfig(Foos.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    GenFoos.verifyMethodPathsAndHttpMethods(Foos.class, "foos", root, "foos");
  }

  @Test
  public void testMethodPaths_inheritanceResourceUnspecified() throws Exception {
    @Api
    class Foos extends GenFoos<Foo> {}
    String apiConfigSource = g.generateConfig(Foos.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    verifyMethodPathAndHttpMethod(root, "myapi.foos.insert", "foo", "POST");
    verifyMethodPathAndHttpMethod(root, "myapi.foos.update", "foo", "PUT");
    verifyMethodPathAndHttpMethod(root, "myapi.foos.list", "foo", "GET");
    verifyMethodPathAndHttpMethod(root, "myapi.foos.remove", "remove/{id}", "DELETE");
    verifyMethodPathAndHttpMethod(root, "myapi.foos.get", "foo/{id}", "GET");
  }

  @Test
  public void testMethodPaths_inheritanceResourceInheritedAndUnspecified() throws Exception {
    @Api
    class Foos extends GenBars<Foos> {}
    String apiConfigSource = g.generateConfig(Foos.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    GenBars.verifyMethodPathsAndHttpMethods(Foos.class, root);
  }

  @Test
  public void testMultipleGenericClasses() throws Exception {
    @Api
    class GenFoo extends GenBars<Foo> {}
    @Api
    @ApiClass(resource = "resource2")
    class GenBar extends GenBars<Bar> {}
    String apiConfigSource = g.generateConfig(GenFoo.class, GenBar.class).get("myapi-v1.api");
  }

  @Api
  private static class SimpleFoo<R> {
    @SuppressWarnings("unused")
    public void foo(R param) {}
  }

  @Test
  public void testRequestDoesContainMap() throws Exception {
    checkRequestIsNotEmpty(new SimpleFoo<Map<ServletContext, User>>() {}.getClass());
  }

  @Test
  public void testRequestDoesNotContainUser() throws Exception {
    checkRequestIsEmpty(new SimpleFoo<User>() {}.getClass());
  }

  @Test
  public void testRequestDoesNotContainHttpServletRequest() throws Exception {
    checkRequestIsEmpty(new SimpleFoo<HttpServletRequest>() {}.getClass());
  }

  @Test
  public void testRequestDoesNotContainServletContext() throws Exception {
    checkRequestIsEmpty(new SimpleFoo<ServletContext>() {}.getClass());
  }

  private void checkRequestIsEmpty(Class<? extends SimpleFoo<?>> clazz) throws Exception {
    checkRequest(clazz, true);
  }

  private void checkRequestIsNotEmpty(Class<? extends SimpleFoo<?>> clazz) throws Exception {
    checkRequest(clazz, false);
  }

  private void checkRequest(Class<? extends SimpleFoo<?>> clazz, boolean isEmpty) throws Exception {
    String apiConfigSource = g.generateConfig(clazz).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    String test = clazz.getName() + ".foo";
    JsonNode methodFooRequest =
        root.path("descriptor").path("methods").path(clazz.getName() + ".foo").path("request");
    assertEquals(isEmpty, methodFooRequest.isMissingNode());
  }

  @Api(transformers = {SerializerTestConvertedBeanFromApiSerializer.class})
  private static class SerializerTestEndpoint {
    @SuppressWarnings("unused")
    public SerializerTestBean getFoo() {
      return null;
    }

    @SuppressWarnings("unused")
    public SerializerTestOverridingPropertyBean getBar() {
      return null;
    }

    @SuppressWarnings("unused")
    public SerializerTestConvertedBeanFromApi getBaz() {
      return null;
    }

    @SuppressWarnings("unused")
    public void testMethodRequestBody(SerializerTestConvertedBean body) {
    }
  }

  @Api
  private static class BadMethodRequestEndpoint {
    @SuppressWarnings("unused")
    public void testBadMethodRequestBody(StringBean body) {
    }
  }

  private static class SerializerTestBean {
    @ApiResourceProperty(name = "baz")
    public String getFoo() {
      return null;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public String getBar() {
      return null;
    }

    @SuppressWarnings("unused")
    public SerializerTestConvertedBean getConverted() {
      return null;
    }
  }

  @ApiTransformer(SerializerTestConvertedBeanSerializer.class)
  private static class SerializerTestConvertedBean {
    @SuppressWarnings("unused")
    public Integer getFoo() {
      return null;
    }
  }

  private static class SerializerTestConvertedBeanFromApi {
    @SuppressWarnings("unused")
    public Integer getFoo() {
      return null;
    }
  }

  private static class SerializerTestConvertedToBean {
    @SuppressWarnings("unused")
    public String getFoo() {
      return null;
    }
  }

  private static class SerializerTestConvertedBeanSerializer
      extends DefaultValueSerializer<SerializerTestConvertedBean, SerializerTestConvertedToBean> {
  }

  private static class SerializerTestConvertedBeanFromApiSerializer extends
      DefaultValueSerializer<SerializerTestConvertedBeanFromApi, SerializerTestConvertedToBean> {
  }

  private static class SerializerTestOverridingPropertyBean {
    @ApiResourceProperty(name = "bar")
    public String getFoo() {
      return null;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Integer getBar() {
      return null;
    }
  }

  @ApiTransformer(StringBeanSerializer.class)
  private static class StringBean {
    @SuppressWarnings("unused")
    public String getFoo() {
      return null;
    }
  }

  private static class StringBeanSerializer extends DefaultValueSerializer<StringBean, String> {
  }

  @Test
  public void testCustomSerialization_renamedProperty() throws Exception {
    String apiConfigSource = g.generateConfig(SerializerTestEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode schemas = root.path("descriptor").path("schemas");
    verifyObjectPropertySchema(schemas.path("SerializerTestBean"), "baz", "string");
  }

  @Test
  public void testCustomSerialization_ignoredProperty() throws Exception {
    String apiConfigSource = g.generateConfig(SerializerTestEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode schemas = root.path("descriptor").path("schemas");
    assertTrue(schemas.path("SerializerTestBean").path("properties").path("bar").isMissingNode());
  }

  @Test
  public void testCustomSerialization_customizedProperty() throws Exception {
    String apiConfigSource = g.generateConfig(SerializerTestEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode schemas = root.path("descriptor").path("schemas");
    verifyObjectPropertyRef(
        schemas.path("SerializerTestBean"), "converted", "SerializerTestConvertedToBean");
  }

  @Test
  public void testCustomSerialization_customizedPropertyFromApi() throws Exception {
    String apiConfigSource = g.generateConfig(SerializerTestEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    verifyMethodResponseRef(
        root, SerializerTestEndpoint.class.getName() + ".getBaz", "SerializerTestConvertedToBean");
  }

  @Test
  public void testCustomSerialization_overridenProperty() throws Exception {
    String apiConfigSource = g.generateConfig(SerializerTestEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode schemas = root.path("descriptor").path("schemas");
    verifyObjectPropertySchema(
        schemas.path("SerializerTestOverridingPropertyBean"), "bar", "string");
  }

  @Test
  public void testCustomSerialization_requestBody() throws Exception {
    Class<?> clazz = SerializerTestEndpoint.class;
    String apiConfigSource = g.generateConfig(clazz).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    verifyMethodRequestRef(
        root, clazz.getName() + ".testMethodRequestBody", "SerializerTestConvertedToBean");
  }

  @Test
  public void testCustomSerialization_badRequestBodyAfterSerialization() throws Exception {
    try {
      g.generateConfig(BadMethodRequestEndpoint.class).get("myapi-v1.api");
      fail("Type serialized to string should not be usable as a method request body");
    } catch (MissingParameterNameException e) {
      // expected
    }
  }

  @Api
  private static class EmptyRequestEndpoint {
    @SuppressWarnings("unused")
    public void foo() {
    }
    @SuppressWarnings("unused")
    public Void bar() {
      return null;
    }
  }

  @Test
  public void testEmptyRequestBody_void() throws Exception {
    testEmpty("foo", "request");
  }

  @Test
  public void testEmptyResponseBody_void() throws Exception {
    testEmpty("foo", "response");
  }

  @Test
  public void testEmptyRequestBody_Void() throws Exception {
    testEmpty("bar", "request");
  }

  @Test
  public void testEmptyResponseBody_Void() throws Exception {
    testEmpty("bar", "response");
  }

  private void testEmpty(String methodName, String requestOrResponse) throws Exception {
    String apiConfigSource = g.generateConfig(EmptyRequestEndpoint.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode fooNode = root.path("descriptor").path("methods").path(
        EmptyRequestEndpoint.class.getName() + "." + methodName);
    assertFalse(fooNode.isMissingNode());
    assertTrue(fooNode.path(requestOrResponse).isMissingNode());
  }

  @Api
  private static class StringRequestEndpoint {
    @SuppressWarnings("unused")
    public void foo(String body) {
    }
  }

  @Api
  private static class StringResponseEndpoint {
    @SuppressWarnings("unused")
    public String foo() {
      return null;
    }
  }

  @Test
  public void testStringRequestThrowsException() throws Exception {
    try {
      String apiConfigSource = g.generateConfig(StringRequestEndpoint.class).get("myapi-v1.api");
      fail("String request body didn't fail in config generation");
    } catch (MissingParameterNameException e) {
      // expected
    }
  }

  @Test
  public void testStringResponseThrowsException() throws Exception {
    try {
      String apiConfigSource = g.generateConfig(StringResponseEndpoint.class).get("myapi-v1.api");
      fail("String response body didn't fail in config generation");
    } catch (InvalidReturnTypeException e) {
      // expected
    }
  }

  @Test
  public void testNamespaceParameters() throws Exception {
    @Api
    class DefaultApi {}
    doTestNamespaceParameters(DefaultApi.class, null, null, null);

    @Api(namespace = @ApiNamespace(ownerDomain = "ownerdomain.com.br", ownerName = ""))
    class OwnedApi {}
    try {
      doTestNamespaceParameters(OwnedApi.class, "ownerdomain.com.br", null, null);
      fail("Forgot to specify required owner name");
    } catch (InvalidNamespaceException expected) {
      // expected
    }

    @Api(namespace = @ApiNamespace(ownerName = "Owner Name", ownerDomain = ""))
    class NamedApi {}
    try {
      doTestNamespaceParameters(NamedApi.class, null, "Owner Name", null);
      fail("Forgot to specify required owner domain");
    } catch (InvalidNamespaceException expected) {
      // expected
    }

    @Api(namespace = @ApiNamespace(packagePath = "Package path", ownerDomain = "", ownerName = ""))
    class PackagedApi {}
    try {
      doTestNamespaceParameters(PackagedApi.class, null, null, "Package path");
      fail("Forgot to specify both owner domain and owner name");
    } catch (InvalidNamespaceException expected) {
      // expected
    }

    @Api(namespace = @ApiNamespace(ownerDomain = "ownerdomain.com.br", ownerName = "Owner Name"))
    class PartiallyNamespacedApi {}
    doTestNamespaceParameters(
        PartiallyNamespacedApi.class, "ownerdomain.com.br", "Owner Name", null);

    @Api(namespace = @ApiNamespace(ownerDomain = "ownerdomain.com.br", ownerName = "Owner Name",
        packagePath = "Package path"))
    class FullyNamespacedApi {}
    doTestNamespaceParameters(
        FullyNamespacedApi.class, "ownerdomain.com.br", "Owner Name", "Package path");
  }

  private void doTestNamespaceParameters(
      Class<?> clazz, String ownerDomain, String ownerName, String packagePath) throws Exception {
    String apiConfigSource = g.generateConfig(clazz).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);

    if (ownerDomain != null) {
      assertFalse(root.path("ownerDomain").isMissingNode());
      assertEquals(ownerDomain, root.get("ownerDomain").asText());
    }
    if (ownerName != null) {
      assertFalse(root.path("ownerName").isMissingNode());
      assertEquals(ownerName, root.get("ownerName").asText());
    }
    if (packagePath != null) {
      assertFalse(root.path("packagePath").isMissingNode());
      assertEquals(packagePath, root.get("packagePath").asText());
    }
  }

  @Test
  public void testArrayRequest() throws Exception {
    @Api class StringArray {
      @SuppressWarnings("unused")
      public void foo(@Named("collection") String[] s) {}
    }
    String apiConfigSource = g.generateConfig(StringArray.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode methodNode = root.path("methods").path("myapi.stringArray.foo");
    assertFalse(methodNode.isMissingNode());
    verifyMethodRequestParameter(methodNode.get("request"), "collection", "string", true, true);
  }

  @Test
  public void testGenericArrayRequest() throws Exception {
    @Api abstract class GenericArray<T> {
      @SuppressWarnings("unused")
      public void foo(@Named("collection") T[] t) {}
    }
    // TODO: remove with JDK8, dummy to force inclusion of GenericArray to InnerClass attribute
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=2210448
    GenericArray<Integer> dummy = new GenericArray<Integer>() {};
    class Int32Array extends GenericArray<Integer> {}
    String apiConfigSource = g.generateConfig(Int32Array.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode methodNode = root.path("methods").path("myapi.int32Array.foo");
    assertFalse(methodNode.isMissingNode());
    verifyMethodRequestParameter(methodNode.get("request"), "collection", "int32", true, true);
  }

  @Test
  public void testCollectionRequest() throws Exception {
    @Api class BooleanCollection {
      @SuppressWarnings("unused")
      public void foo(@Named("collection") Collection<Boolean> b) {}
    }
    String apiConfigSource = g.generateConfig(BooleanCollection.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode methodNode = root.path("methods").path("myapi.booleanCollection.foo");
    assertFalse(methodNode.isMissingNode());
    verifyMethodRequestParameter(methodNode.get("request"), "collection", "boolean", true, true);
  }

  @Test
  public void testGenericCollectionRequest() throws Exception {
    @Api
    abstract class GenericCollection<T> {
      @SuppressWarnings("unused")
      public void foo(@Named("collection") Collection<T> t) {}
    }
    // TODO: remove with JDK8, dummy to force inclusion of GenericArray to InnerClass attribute
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=2210448
    GenericCollection<Long> dummy = new GenericCollection<Long>() {};
    class Int64Collection extends GenericCollection<Long> {}
    String apiConfigSource = g.generateConfig(Int64Collection.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode methodNode = root.path("methods").path("myapi.int64Collection.foo");
    assertFalse(methodNode.isMissingNode());
    verifyMethodRequestParameter(methodNode.get("request"), "collection", "int64", true, true);
  }

  @Test
  public void testComplexTypeArrayRequest() throws Exception {
    @Api class ComplexArray {
      @SuppressWarnings("unused")
      public void foo(@Named("collection") Bean[] beans) {}
    }
    try {
      g.generateConfig(ComplexArray.class).get("myapi-v1.api");
      fail("Non-primitive array should have failed");
    } catch (CollectionResourceException expected) {
      // expected
    }
  }

  @Test
  public void testComplexTypeCollectionRequest() throws Exception {
    @Api class ComplexCollection {
      @SuppressWarnings("unused")
      public void foo(@Named("collection") Collection<Object> dates){}
    }
    try {
      g.generateConfig(ComplexCollection.class).get("myapi-v1.api");
      fail("Non-primitive array should have failed");
    } catch (CollectionResourceException expected) {
      // expected
    }
  }

  @Test
  public void testUnnamedTypeArrayRequest() throws Exception {
    @Api class UnnamedArray {
      @SuppressWarnings("unused")
      public void foo(Long[] dates) {}
    }
    try {
      g.generateConfig(UnnamedArray.class).get("myapi-v1.api");
      fail("Non-primitive array should have failed");
    } catch (MissingParameterNameException expected) {
      // expected
    }
  }

  @Test
  public void testUnnamedTypeCollectionRequest() throws Exception {
    @Api
    class UnnamedCollection {
      @SuppressWarnings("unused")
      public void foo(Collection<String> dates){}
    }
    try {
      g.generateConfig(UnnamedCollection.class).get("myapi-v1.api");
      fail("Non-primitive array should have failed");
    } catch (MissingParameterNameException expected) {
      // expected
    }
  }

  @Test
  public void testMultipleCollectionRequests() throws Exception {
    @Api
    class MultipleCollections {
      @SuppressWarnings("unused")
      public void foo(@Named("ids") Long[] ids, @Named("authors") List<String> authors) {}
    }
    String apiConfigSource = g.generateConfig(MultipleCollections.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode methodNode = root.path("methods").path("myapi.multipleCollections.foo");
    assertFalse(methodNode.isMissingNode());
    verifyMethodRequestParameter(methodNode.get("request"), "ids", "int64", true, true);
    verifyMethodRequestParameter(methodNode.get("request"), "authors", "string", true, true);
  }

  @Test
  public void testResourceAndCollection() throws Exception {
    @Api
    class ResourceAndCollection {
      @SuppressWarnings("unused")
      public void foo(Bean resource, @Named("authors") List<String> authors) {}
    }

    String apiConfigSource = g.generateConfig(ResourceAndCollection.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode methodNode = root.path("methods").path("myapi.resourceAndCollection.foo");
    assertFalse(methodNode.isMissingNode());
    verifyMethodRequestParameter(methodNode.get("request"), "authors", "string", true, true);
    assertEquals(1, methodNode.path("request").path("parameters").size());
    verifyMethodRequestRef(root, ResourceAndCollection.class.getName() + ".foo", "Bean");
  }

  @Test
  public void testCollectionOfArrays() throws Exception {
    @Api
    class NestedCollections {
      @SuppressWarnings("unused")
      public void foo(@Named("authors") List<String[]> authors) {}
    }
    try {
      g.generateConfig(NestedCollections.class).get("myapi-v1.api");
      fail("Nested collections should fail");
    } catch (NestedCollectionException expected) {
      // expected
    }
  }

  @Test
  public void testArraysOfCollections() throws Exception {
    @Api
    class NestedCollections {
      @SuppressWarnings("unused")
      public void foo(@Named("authors") List<String>[] authors) {}
    }
    try {
      g.generateConfig(NestedCollections.class).get("myapi-v1.api");
      fail("Nested collections should fail");
    } catch (NestedCollectionException expected) {
      // expected
    }
  }

  @Test
  public void testCollectionOfCollections() throws Exception {
    @Api
    class NestedCollections {
      @SuppressWarnings("unused")
      public void foo(@Named("authors") List<List<String>> authors) {}
    }
    try {
      g.generateConfig(NestedCollections.class).get("myapi-v1.api");
      fail("Nested collections should fail");
    } catch (NestedCollectionException expected) {
      // expected
    }
  }

  @Test
  public void testArrayOfArrays() throws Exception {
    @Api
    class NestedCollections {
      @SuppressWarnings("unused")
      public void foo(@Named("authors") String[][] authors) {}
    }
    try {
      g.generateConfig(NestedCollections.class).get("myapi-v1.api");
      fail("Nested collections should fail");
    } catch (NestedCollectionException expected) {
      // expected
    }
  }

  @Test
  public void testArrayOfResources() throws Exception {
    @Api
    class ResourceCollection {
      @SuppressWarnings("unused")
      public void foo(List<Bean> beans) {}
    }
    try {
      g.generateConfig(ResourceCollection.class).get("myapi-v1.api");
      fail("Array of resources should fail");
    } catch (CollectionResourceException expected) {
      // expected
    }
  }

  @Test
  public void testArrayOfSerializedResources() throws Exception {
    @Api
    class ResourceCollection {
      @SuppressWarnings("unused")
      public void foo(@Named("beans") List<StringBean> beans) {}
    }
    String apiConfigSource = g.generateConfig(ResourceCollection.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    JsonNode methodNode = root.path("methods").path("myapi.resourceCollection.foo");
    assertFalse(methodNode.isMissingNode());
    verifyMethodRequestParameter(methodNode.get("request"), "beans", "string", true, true);
  }

  @Test
  public void testCanonicalName() throws Exception {
    @Api(name = "myapi", canonicalName = "My API")
    class CanonicalApi {
    }
    String apiConfigSource = g.generateConfig(CanonicalApi.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    assertEquals("myapi", root.path("name").asText());
    assertEquals("My API", root.path("canonicalName").asText());
  }

  @Test
  public void testTitle() throws Exception {
    @Api(name = "myapi", title = "My API Title")
    class ApiWithTitle {
    }
    String apiConfigSource = g.generateConfig(ApiWithTitle.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    assertEquals("myapi", root.path("name").asText());
    assertEquals("My API Title", root.path("title").asText());
  }

  @Test
  public void testDocumentationLink() throws Exception {
    @Api(name = "myapi", documentationLink = "http://go/documentation")
    class ApiWithDocs {
    }
    String apiConfigSource = g.generateConfig(ApiWithDocs.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    assertEquals("myapi", root.path("name").asText());
    assertEquals("http://go/documentation", root.path("documentation").asText());
  }

  @Test
  public void testIcons() throws Exception {
    @Api(name = "myapi", iconX16 = "http://go/iconX16", iconX32 = "http://go/iconX32")
    class ApiWithIcons {
    }
    String apiConfigSource = g.generateConfig(ApiWithIcons.class).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    assertEquals("myapi", root.path("name").asText());
    assertEquals("http://go/iconX16", root.path("icons").path("x16").asText());
    assertEquals("http://go/iconX32", root.path("icons").path("x32").asText());
  }

  @Test
  public void testGenericParameterTypes() throws Exception {
    @Api
    final class Test <T> {
      @SuppressWarnings("unused")
      public void setT(T t) {}
    }

    try {
      g.generateConfig(Test.class).get("myapi-v1.api");
      fail();
    } catch (GenericTypeException e) {
      // Expected.
    }
  }

  @Test
  public void testGenericParameterTypeThroughMethodCall() throws Exception {
    this.<Integer>genericParameterTypeTestImpl();
  }

  @Test
  public void testConsistentApiWideConfig() throws Exception {
    @Api(scopes = { "scopes" })
    @ApiClass(scopes = { "foo" })
    final class Test1 {}

    @Api(scopes = { "scopes" })
    @ApiClass(scopes = { "bar" })
    final class Test2 {}

    g.generateConfig(Test1.class, Test2.class);
  }

  @Test
  public void testInconsistentApiWideConfig() throws Exception {
    @Api(scopes = { "foo" })
    @ApiClass(scopes = { "scopes" })
    final class Test1 {}

    @Api(scopes = { "bar" })
    @ApiClass(scopes = { "scopes" })
    final class Test2 {}

    try {
      g.generateConfig(Test1.class, Test2.class);
      fail();
    } catch (InconsistentApiConfigurationException e) {
      // Expected exception.
    }
  }

  private <T> void genericParameterTypeTestImpl() throws Exception {
    @Api
    class Bar <T1> {
      @SuppressWarnings("unused")
      public void bar(T1 t1) {}
    }
    class Foo extends Bar<T> {}

    try {
      g.generateConfig(Foo.class).get("myapi-v1.api");
      fail();
    } catch (GenericTypeException e) {
      // Expected.
    }
  }

  @Test
  public void testAnonymousClass() throws Exception {
    @Api() 
    class Test {
      @SuppressWarnings("unused")
      public void test() {
      }
    }
    String apiConfigSource = g.generateConfig(new Test() {}.getClass()).get("myapi-v1.api");
    ObjectNode root = objectMapper.readValue(apiConfigSource, ObjectNode.class);
    assertEquals("test", root.path("methods").path("myapi.test").path("path").asText());
  }

  @Test
  public void testGenerateConfigRetainsOrder() throws Exception {
    @Api(name = "onetoday", description = "OneToday API")
    final class OneToday {
    }
    @Api(name = "onetodayadmin", description = "One Today Admin API")
    final class OneTodayAdmin {
    }
    Map<String, String> configs = g.generateConfig(OneToday.class, OneTodayAdmin.class);
    Iterator<String> iterator = configs.keySet().iterator();
    assertEquals("onetoday-v1.api", iterator.next());
    assertEquals("onetodayadmin-v1.api", iterator.next());
  }
}
