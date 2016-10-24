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
package com.google.api.server.spi.config.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.testing.FloatToStringSerializer;
import com.google.api.server.spi.testing.IntegerToStringSerializer;
import com.google.api.server.spi.testing.LongToStringSerializer;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Tests for {@link Types}.
 */
public class TypesTest {
  private static final TypeToken<?> INT = TypeToken.of(int.class);
  private static final TypeToken<?> STRING = TypeToken.of(String.class);
  private static final TypeToken<?> STRING_COLLECTION = new TypeToken<Collection<String>>() { };

  private ApiConfig apiConfig;

  @Before
  public void setUp() throws Exception {
    ServiceContext serviceContext = ServiceContext.create();
    TypeLoader typeLoader = new TypeLoader();
    apiConfig = new ApiConfig.Factory().create(serviceContext, typeLoader, TestEndpoint.class);
    apiConfig.getSerializationConfig().addSerializationConfig(IntegerToStringSerializer.class);
    apiConfig.getSerializationConfig().addSerializationConfig(LongToStringSerializer.class);
    apiConfig.getSerializationConfig().addSerializationConfig(FloatToStringSerializer.class);
  }

  @Test
  public void isArrayType() {
    assertThat(Types.isArrayType(TypeToken.of(Double[].class))).isTrue();
    assertThat(Types.isArrayType(TypeToken.of(Double.class))).isFalse();
  }

  @Test
  public void isArrayType_ByteArray() {
    assertThat(Types.isArrayType(TypeToken.of(byte[].class))).isFalse();
  }

  @Test
  public void isEnumType() {
    assertThat(Types.isEnumType(TypeToken.of(Enum.class))).isTrue();
    assertThat(Types.isEnumType(TypeToken.of(String.class))).isFalse();
  }

  @Test
  public void isMapType() {
    assertThat(Types.isMapType(TypeToken.of(Map.class))).isTrue();
    assertThat(Types.isMapType(TypeToken.of(Collection.class))).isFalse();
  }

  @Test
  public void isTypeVariable() throws Exception {
    final class Foo<T> {
      @SuppressWarnings("unused")
      void foo(T t) {}
    }

    assertThat(Types.isTypeVariable(
        TypeToken.of(
            Foo.class.getDeclaredMethod("foo", Object.class).getGenericParameterTypes()[0])))
        .isTrue();
  }

  @Test
  public void isTypeVariable_notGeneric() {
    assertThat(Types.isTypeVariable(TypeToken.of(Integer.class))).isFalse();
    assertThat(Types.isTypeVariable(TypeToken.of(String.class))).isFalse();
    assertThat(Types.isTypeVariable(TypeToken.of(Object.class))).isFalse();
    assertThat(Types.isTypeVariable(TypeToken.of(String[].class))).isFalse();
    assertThat(Types.isTypeVariable(TypeToken.of(Collection.class))).isFalse();
  }

  @Test
  public void isTypeVariable_parameterized() {
    class Foo extends ArrayList<String> {}

    assertThat(Types.isTypeVariable(TypeToken.of(Foo.class.getGenericSuperclass()))).isFalse();
  }

  @Test
  public void isCollectionResponseType() {
    final class MyResponse extends CollectionResponse {
      protected MyResponse(Collection items, String nextPageToken) {
        super(items, nextPageToken);
      }
    }
    assertThat(Types.isCollectionResponseType(TypeToken.of(CollectionResponse.class))).isTrue();
    assertThat(Types.isCollectionResponseType(TypeToken.of(MyResponse.class))).isTrue();
    assertThat(Types.isCollectionResponseType(TypeToken.of(Collection.class))).isFalse();
  }

  @Test
  public void isWildcardType() {
    assertThat(Types.isWildcardType(STRING)).isFalse();
    TypeToken<?> t = new TypeToken<Collection<?>>() {};
    assertThat(Types.isWildcardType(
        t.resolveType(((ParameterizedType) t.getType()).getActualTypeArguments()[0]))).isTrue();
    t = new TypeToken<Collection<? extends String>>() {};
    assertThat(Types.isWildcardType(
        t.resolveType(((ParameterizedType) t.getType()).getActualTypeArguments()[0]))).isTrue();
  }

  @Test
  public void isObject() {
    assertThat(Types.isObject(TypeToken.of(Object.class))).isTrue();
    assertThat(Types.isObject(STRING)).isFalse();
  }

  @Test
  public void getSimpleName_null() {
    assertThat(Types.getSimpleName(null, apiConfig.getSerializationConfig())).isNull();
  }

  @Test
  public void getSimpleName_array() {
    assertThat(getSimpleName(String[].class)).isEqualTo("StringCollection");
  }

  @Test
  public void getSimpleName_basic() {
    assertThat(getSimpleName(String.class)).isEqualTo("String");
  }

  @Test
  public void getSimpleName_withSerializer() {
    assertThat(getSimpleName(Integer.class)).isEqualTo("Integer");
  }

  @Test
  public void getSimpleName_withResourceSerializerNameOverride() {
    assertThat(getSimpleName(Long.class)).isEqualTo("Number");
  }

  @Test
  public void getSimpleName_withResourceSerializerNoNameOverride() {
    assertThat(getSimpleName(Float.class)).isEqualTo("Float");
  }

  @Test
  public void getSimpleName_parameterized() {
    class Foo extends Base2<Integer, Base<String>> {
    }

    ParameterizedType type = (ParameterizedType) Foo.class.getGenericSuperclass();
    assertThat(getSimpleName(type)).isEqualTo("Base2_Integer_Base_String");
  }

  private String getSimpleName(Type type) {
    return Types.getSimpleName(TypeToken.of(type), apiConfig.getSerializationConfig());
  }

  @Test
  public void getArrayItemType_basic() {
    assertThat(Types.getArrayItemType(TypeToken.of(String[].class))).isEqualTo(STRING);
    assertThat(Types.getArrayItemType(TypeToken.of(int[].class))).isEqualTo(INT);
  }

  @Test
  public void getArrayItemType_genericArray() {
    class Foo extends Base<String[]> {}

    ParameterizedType type = (ParameterizedType) Foo.class.getGenericSuperclass();
    assertThat(Types.getArrayItemType(TypeToken.of(type.getActualTypeArguments()[0])))
        .isEqualTo(STRING);
  }

  @Test
  public void getArrayItemType_genericCollection() {
    class Foo extends Base<Collection<String>> {}

    ParameterizedType type = (ParameterizedType) Foo.class.getGenericSuperclass();
    assertThat(Types.getArrayItemType(TypeToken.of(type.getActualTypeArguments()[0])))
        .isEqualTo(STRING);
  }

  @Test
  public void getArrayItemType_nonArray() {
    assertThat(Types.getArrayItemType(STRING)).isNull();
  }

  @Test
  public void getTypeParameter() {
    assertThat(Types.getTypeParameter(STRING_COLLECTION, 0)).isEqualTo(STRING);
  }

  @Test
  public void getTypeParameter_notParameterized() {
    try {
      Types.getTypeParameter(STRING, 0);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test
  public void getTypeParameter_notEnoughParameters() {
    try {
      Types.getTypeParameter(STRING_COLLECTION, 1);
      fail("expected IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
      // expected
    }
  }

  private enum Enum {
    A,
    B,
  }

  private static class Base<T> {
  }

  private static class Base2<T1, T2> {
  }
}