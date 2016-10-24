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

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.api.server.spi.config.ApiTransformer;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.ResourceTransformer;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.testing.DefaultValueSerializer;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

/**
 * Tests for {@link ResourceSchemaProvider}.
 */
public abstract class ResourceSchemaProviderTest {
  private ResourceSchemaProvider provider;
  private ApiConfig config;

  @Before
  public void setUp() throws Exception {
    provider = getResourceSchemaProvider();
    config = new ApiConfig.Factory().create(
        ServiceContext.create(), new TypeLoader(), TestEndpoint.class);
  }

  @Test
  public void testNoClassPropertyReturned() {
    ResourceSchema schema = getResourceSchema(SinglePropertyBean.class);
    assertThat(schema.getProperties().keySet()).containsExactly("foo");
  }

  @Test
  public void testIgnoredPropertyNotReturned() {
    ResourceSchema schema = getResourceSchema(IgnoredPropertyBean.class);
    assertThat(schema.getProperties().keySet()).containsExactly("foo");
  }

  @Test
  public void testRenamedProperty() {
    ResourceSchema schema = getResourceSchema(RenamedPropertyBean.class);
    assertThat(schema.getProperties().keySet()).containsExactly("bar");
  }

  @Test
  public void testMissingPropertyType() {
    ResourceSchema schema = getResourceSchema(MissingPropertyTypeBean.class);
    assertThat(schema.getProperties().keySet()).containsExactly("bar");
  }

  @Test
  public void testCustomSerializedPropertyReturns() {
    ResourceSchema schema = getResourceSchema(CustomSerializerParentBean.class);
    assertThat(schema.getProperties().keySet()).containsExactly("foo");
    assertEquals(String.class, schema.getProperties().get("foo").getJavaType());
  }

  @Test
  public void testAnnotationCustomResourceSerializedPropertyReturnsSchema() {
    ResourceSchema schema = getResourceSchema(CustomResourceSerializerBean.class);
    assertEquals("CustomizedName", schema.getName());
    assertThat(schema.getProperties().keySet()).containsExactly("baz", "qux");
    assertEquals(Integer.class, schema.getProperties().get("baz").getJavaType());
    assertEquals(Boolean.class, schema.getProperties().get("qux").getJavaType());
  }

  @Test
  public void testConfigCustomResourceSerializedPropertyReturnsSchema() {
    config.getSerializationConfig().addSerializationConfig(SinglePropertyResourceSerializer.class);
    ResourceSchema schema = getResourceSchema(SinglePropertyBean.class);
    String name = SinglePropertyBean.class.getSimpleName();
    assertThat(schema.getProperties().keySet()).containsExactly(name);
    assertEquals(Long.class, schema.getProperties().get(name).getJavaType());
  }

  @Test
  public void testConfigCustomResourceSerializedInheritedPropertyReturnsSchema() {
    config.getSerializationConfig().addSerializationConfig(SinglePropertyResourceSerializer.class);
    ResourceSchema schema = getResourceSchema(SinglePropertyBeanChild.class);
    String name = SinglePropertyBeanChild.class.getSimpleName();
    assertThat(schema.getProperties().keySet()).containsExactly(name);
    assertEquals(Long.class, schema.getProperties().get(name).getJavaType());
  }

  @Test
  public void testBeanPropertyWithGetterAndSetter() throws Exception {
    ResourceSchema schema = getResourceSchema(Bean.class);
    assertThat(schema.getProperties().keySet()).containsExactly("date");
    assertEquals(Date.class, schema.getProperties().get("date").getJavaType());
  }

  @Test
  public void testPrivateBeanPropertyWithAnnotation() throws Exception {
    ResourceSchema schema = getResourceSchema(PrivatePropertyBean.class);
    assertEquals(1, schema.getProperties().size());
    assertEquals(String.class, schema.getProperties().get("foo").getJavaType());
  }

  @Test
  public void testBeanPropertyWithSetterOnly() throws Exception {
    ResourceSchema schema = getResourceSchema(BeanWithSetterOnlyProperty.class);
    assertThat(schema.getProperties().keySet()).containsExactly("a");
    assertEquals(String.class, schema.getProperties().get("a").getJavaType());
  }

  @Test
  public void testGenericJsonEntity() throws Exception {
    ResourceSchema schema = getResourceSchema(GenericJsonEntity.class);
    assertThat(schema.getProperties().keySet()).containsExactly("foo");
    assertThat(schema.getProperties().get("foo").getJavaType()).isEqualTo(String.class);
  }

  private static class BeanWithSetterOnlyProperty {
    @SuppressWarnings("unused")
    public void setA(String a) {}
  }

  @SuppressWarnings("unused")
  private static class Bean {
    public Date getDate() {
      return null;
    }

    public void setDate(Date date) {}
  }

  private static class SinglePropertyBean {
    @SuppressWarnings("unused")
    public String getFoo() {
      return null;
    }
  }

  private static class SinglePropertyBeanChild extends SinglePropertyBean {
  }

  private static class IgnoredPropertyBean {
    @SuppressWarnings("unused")
    public String getFoo() {
      return null;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public String getBar() {
      return null;
    }
  }

  private static class RenamedPropertyBean {
    @ApiResourceProperty(name = "bar")
    public String getFoo() {
      return null;
    }
  }

  /**
   * A JavaBean that has a JsonProperty, but no supporting JavaBean property to access it.
   */
  private static class MissingPropertyTypeBean {
    private long foo;

    @SuppressWarnings("unused")
    public String getBar() {
      return null;
    }
  }

  private static class CustomSerializerParentBean {
    @SuppressWarnings("unused")
    public CustomSerializerChildBean getFoo() {
      return null;
    }
  }

  @ApiTransformer(value = CustomSerializer.class)
  private static class CustomSerializerChildBean {
  }

  static class CustomSerializer extends DefaultValueSerializer<CustomSerializerChildBean, String> {
  }

  @ApiTransformer(value = CustomResourceSerializer.class)
  static class CustomResourceSerializerBean {

    public String getFoo() {
      return null;
    }

    public Long getBar() {
      return null;
    }
  }

  private static class GenericJsonEntity extends GenericJson {
    @Key private String foo;

    public String getFoo() {
      return foo;
    }

    public void setFoo(String foo) {
      this.foo = foo;
    }
  }

  public static class CustomResourceSerializer
      extends DefaultValueSerializer<CustomResourceSerializerBean, Map<String, Object>>
      implements ResourceTransformer<CustomResourceSerializerBean> {

    @Override
    public ResourceSchema getResourceSchema() {
      return ResourceSchema.builderForType(CustomResourceSerializer.class)
          .setName("CustomizedName")
          .addProperty("baz", ResourcePropertySchema.of(TypeToken.of(Integer.class)))
          .addProperty("qux", ResourcePropertySchema.of(TypeToken.of(Boolean.class)))
          .build();
    }
  }

  public static class SinglePropertyResourceSerializer
      extends DefaultValueSerializer<SinglePropertyBean, Map<String, Object>>
      implements ResourceTransformer<SinglePropertyBean> {
    private Class<? extends SinglePropertyBean> clazz;

    public SinglePropertyResourceSerializer(Class<? extends SinglePropertyBean> clazz) {
      this.clazz = clazz;
    }

    @Override
    public ResourceSchema getResourceSchema() {
      return ResourceSchema.builderForType(clazz)
          .addProperty(clazz.getSimpleName(), ResourcePropertySchema.of(TypeToken.of(Long.class)))
          .build();
    }
  }

  @SuppressWarnings("unused")
  public static class PrivatePropertyBean {
    @ApiResourceProperty(name = "foo") private String test;
    private String test2;
  }

  private ResourceSchema getResourceSchema(Type type) {
    return provider.getResourceSchema(TypeToken.of(type), config);
  }

  public abstract ResourceSchemaProvider getResourceSchemaProvider();
}
