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
package com.google.api.server.spi.config.annotationreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.api.server.spi.config.ApiTransformer;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.ResourceTransformer;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.testing.DefaultValueSerializer;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

/**
 * Tests for {@link ApiAnnotationIntrospector}. These tests actually test serialization using the
 * introspector, rather than the introspector methods themselves, which aren't that interesting.
 */
@RunWith(JUnit4.class)
public class ApiAnnotationIntrospectorTest {
  private ObjectMapper objectMapper = null;

  @Before
  public void setUp() throws Exception {
    objectMapper = ObjectMapperUtil.createStandardObjectMapper();
  }

  @Test
  public void testSerializeApiNameOnGetter() throws Exception {
    testSerializeApiName(TestApiNameOnGetter.class);
  }

  @Test
  public void testDeserializeApiNameOnGetter() throws Exception {
    testDeserializeApiName(TestApiNameOnGetter.class);
  }

  @Test
  public void testSerializeApiNameOnSetter() throws Exception {
    testSerializeApiName(TestApiNameOnSetter.class);
  }

  @Test
  public void testDeserializeApiNameOnSetter() throws Exception {
    testDeserializeApiName(TestApiNameOnSetter.class);
  }

  @Test
  public void testSerializeApiIgnoreOnGetter() throws Exception {
    testSerializeApiIgnore(TestApiIgnoreOnGetter.class);
  }

  @Test
  public void testDeserializeApiIgnoreOnGetter() throws Exception {
    testDeserializeApiIgnore(TestApiIgnoreOnGetter.class);
  }

  @Test
  public void testSerializeApiIgnoreOnSetter() throws Exception {
    testSerializeApiIgnore(TestApiIgnoreOnSetter.class);
  }

  @Test
  public void testDeserializeApiIgnoreOnSetter() throws Exception {
    testDeserializeApiIgnore(TestApiIgnoreOnSetter.class);
  }

  @Test
  public void testSerializeCustomResourceSerializer() throws Exception {
    TestResourceWithCustomSerializer obj = new TestResourceWithCustomSerializer();
    obj.point = "7,8";
    obj.nested = new TestSerializePublicField();
    obj.nested.foo = "bar";
    String json = objectMapper.writeValueAsString(obj);
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertEquals(7, value.path("x").asInt());
    assertEquals(8, value.path("y").asInt());
    assertEquals("bar", value.path("nestedResource").path("foo").asText());
  }

  @Test
  public void testDeserializeCustomResourceSerializer() throws Exception {
    String json = "{\"nestedResource\": {\"foo\": \"baz\"}, \"y\": 42, \"notAField\": 123, "
        + "\"alsoNotAField\": {\"y\": 43}}";
    TestResourceWithCustomSerializer value =
        objectMapper.readValue(json, TestResourceWithCustomSerializer.class);
    assertEquals("0,42", value.point);
    assertNotNull(value.nested);
    assertEquals("baz", value.nested.foo);
  }

  @Test
  public void testSerializeCustomResourceSerializerInheritance() throws Exception {
    TestChildResourceWithCustomSerializer obj = new TestChildResourceWithCustomSerializer();
    obj.point = "1,2";
    obj.nested = new TestSerializePublicField();
    obj.nested.foo = null;
    obj.number = 123;
    String json = objectMapper.writeValueAsString(obj);
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertEquals(1, value.path("x").asInt());
    assertEquals(2, value.path("y").asInt());
    assertTrue(value.path("nestedResource").path("foo").isNull());
    assertEquals(123, value.path("aShort").asInt());
  }

  @Test
  public void testDeserializeCustomResourceSerializerInheritance() throws Exception {
    String json = "{\"nestedResource\": {}, \"aShort\": 99}";
    TestChildResourceWithCustomSerializer value =
        objectMapper.readValue(json, TestChildResourceWithCustomSerializer.class);
    assertEquals("0,0", value.point);
    assertNotNull(value.nested);
    assertEquals("test", value.nested.foo);
    assertEquals(new Short((short) 99), value.number);
  }

  @Test
  public void testSerializePublicField() throws Exception {
    String json = objectMapper.writeValueAsString(new TestSerializePublicField());
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertEquals("test", value.path("foo").asText());
  }

  @Test
  public void testDeserializePublicField() throws Exception {
    String json = "{\"foo\": \"bar\"}";
    TestSerializePublicField value = objectMapper.readValue(json, TestSerializePublicField.class);
    assertEquals("bar", value.foo);
  }

  @Test
  public void testSerializePrivateField() throws Exception {
    String json = objectMapper.writeValueAsString(new TestSerializePrivateField());
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertTrue(value.path("foo").isMissingNode());
  }

  @Test
  public void testDeserializePrivateField() throws Exception {
    String json = "{\"foo\": \"bar\"}";
    TestSerializePrivateField value = objectMapper.readValue(json, TestSerializePrivateField.class);
    assertEquals("test", value.foo);
  }

  @Test
  public void testSerializeAnnotatedPrivateField() throws Exception {
    String json = objectMapper.writeValueAsString(new TestSerializeAnnotatedPrivateField());
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertEquals("test", value.path("foo").asText());
  }

  @Test
  public void testDeserializeAnnotatedPrivateField() throws Exception {
    String json = "{\"foo\": \"bar\"}";
    TestSerializeAnnotatedPrivateField value =
        objectMapper.readValue(json, TestSerializeAnnotatedPrivateField.class);
    assertEquals("bar", value.foo);
  }

  @Test
  public void testSerializePublicIgnoredField() throws Exception {
    String json = objectMapper.writeValueAsString(new TestSerializePrivateField());
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertTrue(value.path("foo").isMissingNode());
  }

  @Test
  public void testDeserializePublicIgnoredField() throws Exception {
    String json = "{\"foo\": \"bar\"}";
    TestSerializePrivateField value = objectMapper.readValue(json, TestSerializePrivateField.class);
    assertEquals("test", value.foo);
  }

  @Test
  public void testApiSerializationByConfig() throws Exception {
    ApiSerializationConfig config = new ApiSerializationConfig();
    config.addSerializationConfig(TestApiSerializationByConfigConverter.class);
    objectMapper.setAnnotationIntrospector(new ApiAnnotationIntrospector(config));
    String json = objectMapper.writeValueAsString(new TestApiSerializationByConfig());
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertEquals("testserialized", value.asText());
  }

  @Test
  public void testApiDeserializationByConfig() throws Exception {
    ApiSerializationConfig config = new ApiSerializationConfig();
    config.addSerializationConfig(TestApiSerializationByConfigConverter.class);
    objectMapper.setAnnotationIntrospector(new ApiAnnotationIntrospector(config));
    TestApiSerializationByConfig value =
        objectMapper.readValue("\"test2serialized\"", TestApiSerializationByConfig.class);
    assertEquals("test2", value.getFoo());
  }

  @Test
  public void testApiSerializationByParameterizedConfig() throws Exception {
    ApiSerializationConfig config = new ApiSerializationConfig();
    config.addSerializationConfig(TestApiSerializationByParameterizedConfigConverter.class);
    objectMapper.setAnnotationIntrospector(new ApiAnnotationIntrospector(config));
    String json = objectMapper.writeValueAsString(
        new TestApiSerializationByParameterizedConfig<String>("paramtest"));
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertEquals("paramtestserialized", value.asText());
  }

  @Test
  public void testApiDeserializationByParameterizedConfig() throws Exception {
    ApiSerializationConfig config = new ApiSerializationConfig();
    config.addSerializationConfig(TestApiSerializationByParameterizedConfigConverter.class);
    objectMapper.setAnnotationIntrospector(new ApiAnnotationIntrospector(config));
    TestApiSerializationByParameterizedConfig value = objectMapper.readValue(
        "\"paramtest2serialized\"", TestApiSerializationByParameterizedConfig.class);
    assertEquals("paramtest2", value.getBar());
  }

  @Test
  public void testApiSerializationByAnnotation() throws Exception {
    String json = objectMapper.writeValueAsString(new TestApiSerializationByAnnotation());
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertEquals("testserialized", value.asText());
  }

  @Test
  public void testApiDeserializationByAnnotation() throws Exception {
    TestApiSerializationByAnnotation value =
        objectMapper.readValue("\"test2serialized\"", TestApiSerializationByAnnotation.class);
    assertEquals("test2", value.getFoo());
  }

  @Test
  public void testApiSerializationToComplex() throws Exception {
    String json = objectMapper.writeValueAsString(new TestApiSerializationToComplex());
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertEquals("test", value.path("bar").asText());
  }

  @Test
  public void testApiDeserializationToComplex() throws Exception {
    TestApiSerializationToComplex value =
        objectMapper.readValue("{\"bar\": \"test2\"}", TestApiSerializationToComplex.class);
    assertEquals("test2", value.getFoo());
  }

  @Test
  public void testGetSerializerTypeWithIncompatibleSourceType() throws Exception {
    try {
      ApiConfig config = new ApiConfig.Factory().create(
          ServiceContext.create(), new TypeLoader(), TestEndpoint.class);
      ApiAnnotationIntrospector.getSchemaType(TypeToken.of(BadSerializerBean.class), config);
      fail("no exception thrown when a bad serializer was specified");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  private void testSerializeApiName(Class<? extends Foo> testApiNameClass) throws Exception {
    String json = objectMapper.writeValueAsString(testApiNameClass.newInstance());
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertEquals("test", value.path("bar").asText());
  }

  private void testDeserializeApiName(Class<? extends Foo> testApiNameClass) throws Exception {
    String json = "{\"bar\": \"foo\"}";
    Foo value = objectMapper.readValue(json, testApiNameClass);
    assertEquals("foo", value.getFoo());
  }

  private void testSerializeApiIgnore(Class<? extends Foo> testApiIgnoreClass) throws Exception {
    String json = objectMapper.writeValueAsString(testApiIgnoreClass.newInstance());
    JsonNode value = objectMapper.readValue(json, JsonNode.class);
    assertTrue(value.path("foo").isMissingNode());
  }

  private void testDeserializeApiIgnore(Class<? extends Foo> testApiIgnoreClass) throws Exception {
    String json = "{\"foo\": \"foo\"}";
    Foo value = objectMapper.readValue(json, testApiIgnoreClass);
    assertEquals("test", value.getFoo());
  }

  public interface Foo {
    String getFoo();
    void setFoo(String foo);
  }

  /**
   * Parameterized interface used to test transformer operation on ParameterizedType.
   */
  public interface Bar<T> {
    T getBar();
    void setBar(T bar);
  }

  public static class TestApiNameOnGetter implements Foo {
    private String foo = "test";

    @Override
    @ApiResourceProperty(name = "bar")
    public String getFoo() {
      return foo;
    }

    @Override
    public void setFoo(String foo) {
      this.foo = foo;
    }
  }

  public static class TestApiNameOnSetter implements Foo {
    private String foo = "test";

    @Override
    public String getFoo() {
      return foo;
    }

    @Override
    @ApiResourceProperty(name = "bar")
    public void setFoo(String foo) {
      this.foo = foo;
    }
  }

  public static class TestApiIgnoreOnGetter implements Foo {
    private String foo = "test";

    @Override
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public String getFoo() {
      return foo;
    }

    @Override
    public void setFoo(String foo) {
      this.foo = foo;
    }

    public String getBar() {
      return null;
    }
  }

  public static class TestApiIgnoreOnSetter implements Foo {
    private String foo = "test";

    @Override
    public String getFoo() {
      return foo;
    }

    @Override
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public void setFoo(String foo) {
      this.foo = foo;
    }

    public String getBar() {
      return null;
    }
  }

  public static class TestApiSerializationByConfig implements Foo {
    private String foo = "test";

    @Override
    public String getFoo() {
      return foo;
    }

    @Override
    public void setFoo(String foo) {
      this.foo = foo;
    }
  }

  public static class TestApiSerializationByConfigConverter
      implements Transformer<TestApiSerializationByConfig, String> {

    @Override
    public String transformTo(TestApiSerializationByConfig in) {
      return in.getFoo() + "serialized";
    }

    @Override
    public TestApiSerializationByConfig transformFrom(String in) {
      TestApiSerializationByConfig obj = new TestApiSerializationByConfig();
      obj.setFoo(in.replace("serialized", ""));
      return obj;
    }
  }

  /**
   * An implementation of {@code Bar} for parameterized testing.
   */
  public static class TestApiSerializationByParameterizedConfig<T> implements Bar<T> {
    private T bar;

    TestApiSerializationByParameterizedConfig(T bar) {
      setBar(bar);
    }

    @Override
    public T getBar() {
      return bar;
    }

    @Override
    public void setBar(T bar) {
      this.bar = bar;
    }
  }

  /**
   * A (non-parameterized) Transformer operating on a parameterized type.
   */
  public static class TestApiSerializationByParameterizedConfigConverter
      implements Transformer<TestApiSerializationByParameterizedConfig, String> {

    @Override
    public String transformTo(TestApiSerializationByParameterizedConfig in) {
      return in.getBar().toString() + "serialized";
    }

    @Override
    public TestApiSerializationByParameterizedConfig transformFrom(String in) {
      return new TestApiSerializationByParameterizedConfig<String>(in.replace("serialized", ""));
    }
  }

  @ApiTransformer(TestApiSerializationByAnnotationConverter.class)
  public static class TestApiSerializationByAnnotation implements Foo {
    private String foo = "test";

    @Override
    public String getFoo() {
      return foo;
    }

    @Override
    public void setFoo(String foo) {
      this.foo = foo;
    }
  }

  public static class TestApiSerializationByAnnotationConverter
      implements Transformer<TestApiSerializationByAnnotation, String> {

    @Override
    public String transformTo(TestApiSerializationByAnnotation in) {
      return in.getFoo() + "serialized";
    }

    @Override
    public TestApiSerializationByAnnotation transformFrom(String in) {
      TestApiSerializationByAnnotation obj = new TestApiSerializationByAnnotation();
      obj.setFoo(in.replace("serialized", ""));
      return obj;
    }
  }

  public static class Complex {
    private String bar;

    public Complex() {
    }

    public Complex(String bar) {
      this.bar = bar;
    }

    public String getBar() {
      return bar;
    }

    public void setBar(String bar) {
      this.bar = bar;
    }
  }

  @ApiTransformer(TestApiSerializationToComplexConverter.class)
  public static class TestApiSerializationToComplex implements Foo {
    private String foo = "test";

    @Override
    public String getFoo() {
      return foo;
    }

    @Override
    public void setFoo(String foo) {
      this.foo = foo;
    }
  }

  public static class TestApiSerializationToComplexConverter
      implements Transformer<TestApiSerializationToComplex, Complex> {

    @Override
    public Complex transformTo(TestApiSerializationToComplex in) {
      return new Complex(in.getFoo());
    }

    @Override
    public TestApiSerializationToComplex transformFrom(Complex in) {
      TestApiSerializationToComplex obj = new TestApiSerializationToComplex();
      obj.setFoo(in.getBar());
      return obj;
    }
  }

  public static class SuperSerializer extends DefaultValueSerializer<String, Integer> {
  }

  @ApiTransformer(BadSerializer.class)
  public static class BadSerializerBean {
  }

  public static class BadSerializer extends DefaultValueSerializer<String, Integer> {
  }

  @ApiTransformer(CustomResourceSerializer.class)
  public static class TestResourceWithCustomSerializer {
    public String point = "0,0";
    public TestSerializePublicField nested;
  }

  public static class TestChildResourceWithCustomSerializer
      extends TestResourceWithCustomSerializer {
    public Short number = 25;
  }

  // A resource serializer with inheritance.
  public static class CustomResourceSerializer
      implements ResourceTransformer<TestResourceWithCustomSerializer> {

    private Class<? extends TestResourceWithCustomSerializer> clazz;

    public CustomResourceSerializer(Class<? extends TestResourceWithCustomSerializer> clazz) {
      this.clazz = clazz;
    }

    @Override
    public Map<String, Object> transformTo(TestResourceWithCustomSerializer in) {
      ImmutableMap.Builder<String, Object> resultBuilder = ImmutableMap.builder();
      resultBuilder.put("x", new Integer(in.point.split(",")[0]));
      resultBuilder.put("y", new Integer(in.point.split(",")[1]));
      resultBuilder.put("nestedResource", in.nested);
      if (TestChildResourceWithCustomSerializer.class.isAssignableFrom(clazz)) {
        resultBuilder.put("aShort", ((TestChildResourceWithCustomSerializer) in).number);
      }
      return resultBuilder.build();
    }

    @Override
    public TestResourceWithCustomSerializer transformFrom(Map<String, Object> in) {
      TestResourceWithCustomSerializer obj = null;
      try {
        obj = clazz.newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      obj.point = String.format("%d,%d",
          MoreObjects.firstNonNull(in.get("x"), 0),
          MoreObjects.firstNonNull(in.get("y"), 0));
      obj.nested = (TestSerializePublicField) in.get("nestedResource");
      if (TestChildResourceWithCustomSerializer.class.isAssignableFrom(clazz)) {
        ((TestChildResourceWithCustomSerializer) obj).number = (Short) in.get("aShort");
      }
      return obj;
    }

    @Override
    public ResourceSchema getResourceSchema() {
      ResourceSchema.Builder builder = ResourceSchema.builderForType(clazz)
          .addProperty("x", ResourcePropertySchema.of(TypeToken.of(Integer.class)))
          .addProperty("y", ResourcePropertySchema.of(TypeToken.of(Integer.class)))
          .addProperty("nestedResource",
              ResourcePropertySchema.of(TypeToken.of(TestSerializePublicField.class)));
      if (TestChildResourceWithCustomSerializer.class.isAssignableFrom(clazz)) {
        builder.addProperty("aShort", ResourcePropertySchema.of(TypeToken.of(Short.class)));
      }
      return builder.build();
    }
  }

  public static class TestSerializePublicField {
    public String foo = "test";
  }

  public static class TestSerializePrivateField {
    private String foo = "test";
    public String bar = "test";
  }

  public static class TestSerializeAnnotatedPrivateField {
    @ApiResourceProperty
    private String foo = "test";
  }

  public static class TestSerializePublicIgnoredField {
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public String foo = "test";
    public String bar = "test";
  }
}
