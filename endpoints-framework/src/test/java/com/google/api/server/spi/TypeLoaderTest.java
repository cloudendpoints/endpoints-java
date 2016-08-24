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
package com.google.api.server.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Tests for {@link TypeLoader}.
 * @author Eric Orth
 */
@RunWith(JUnit4.class)
public class TypeLoaderTest {
  private TypeLoader typeLoader;

  private static class Base<T> {}
  private enum Enum {
    A,
    B,
  }

  @Before
  public void setUp() throws Exception {
    typeLoader = new TypeLoader();
  }

  @Test
  public void testGetArrayItemType_basic() {
    assertEquals(String.class, TypeLoader.getArrayItemType(String[].class));
    assertEquals(int.class, TypeLoader.getArrayItemType(int[].class));
  }

  @Test
  public void testGetArrayItemType_genericArray() {
    class Foo extends Base<String[]> {}

    ParameterizedType type = (ParameterizedType) Foo.class.getGenericSuperclass();
    assertEquals(String.class, TypeLoader.getArrayItemType(type.getActualTypeArguments()[0]));
  }

  @Test
  public void testGetArrayItemType_genericCollection() {
    class Foo extends Base<Collection<String>> {}

    ParameterizedType type = (ParameterizedType) Foo.class.getGenericSuperclass();
    assertEquals(String.class, TypeLoader.getArrayItemType(type.getActualTypeArguments()[0]));
  }

  @Test
  public void testGetArrayItemType_nonArray() {
    assertNull(TypeLoader.getArrayItemType(String.class));
  }

  @Test
  public void testIsInjectedType() {
    assertTrue(typeLoader.isInjectedType(javax.servlet.http.HttpServletRequest.class));
    assertFalse(typeLoader.isInjectedType(String.class));
  }

  @Test
  public void testIsSchemaType() {
    assertTrue(typeLoader.isSchemaType(Boolean.class));
    assertFalse(typeLoader.isSchemaType(java.util.HashMap.class));
  }

  @Test
  public void testIsParameterType() {
    assertTrue(typeLoader.isParameterType(Integer.class));
    assertFalse(typeLoader.isParameterType(Short.class));
  }

  @Test
  public void testIsArrayType() {
    assertTrue(TypeLoader.isArrayType(Double[].class));
    assertFalse(TypeLoader.isArrayType(Double.class));
  }

  @Test
  public void testIsArrayType_ByteArray() {
    assertFalse(TypeLoader.isArrayType(byte[].class));
  }

  @Test
  public void testIsEnumType() {
    assertTrue(TypeLoader.isEnumType(Enum.class));
    assertFalse(TypeLoader.isEnumType(String.class));
  }

  @Test
  public void testIsMapType() {
    assertTrue(typeLoader.isMapType(java.util.Map.class));
    assertTrue(typeLoader.isMapType(java.util.HashMap.class));
    assertFalse(typeLoader.isMapType(java.util.Collection.class));
  }

  @Test
  public void testIsMapType_parameterized() {
    class Foo extends HashMap<String, String> {}

    assertTrue(typeLoader.isMapType(Foo.class));
    ParameterizedType type = (ParameterizedType) Foo.class.getGenericSuperclass();
    assertTrue(typeLoader.isMapType(type));
  }

  @Test
  public void testIsGenericType() {
    final class Foo<T> {
      @SuppressWarnings("unused")
      void foo(T t) {}
    }

    assertTrue(TypeLoader.isGenericType(
        Foo.class.getDeclaredMethods()[0].getGenericParameterTypes()[0]));
  }

  @Test
  public void testIsGenericType_notGeneric() {
    assertFalse(TypeLoader.isGenericType(Integer.class));
    assertFalse(TypeLoader.isGenericType(String.class));
    assertFalse(TypeLoader.isGenericType(Object.class));
    assertFalse(TypeLoader.isGenericType(String[].class));
    assertFalse(TypeLoader.isGenericType(Collection.class));
  }

  @Test
  public void testIsGenericType_parameterized() {
    class Foo extends ArrayList<String> {}

    assertFalse(TypeLoader.isGenericType(Foo.class.getGenericSuperclass()));
  }
}
