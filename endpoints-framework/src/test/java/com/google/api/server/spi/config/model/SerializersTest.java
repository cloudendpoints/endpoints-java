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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.testing.DefaultValueSerializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tests for {@link Serializers}.
 */
@SuppressWarnings("RedundantCast")
@RunWith(JUnit4.class)
public class SerializersTest {

  @Test
  public void testGetSerializerSourceAndTarget_simple() {
    abstract class TestSerializer implements Transformer<Long, Double> {
    }
    assertEquals(TypeToken.of(Long.class), Serializers.getSourceType(TestSerializer.class));
    assertEquals(TypeToken.of(Double.class), Serializers.getTargetType(TestSerializer.class));
  }

  @Test
  public void testGetSerializerSourceAndTarget_parameterized() {
    abstract class TestSerializer implements Transformer<List<String>, Map<String, Boolean>> {
    }
    assertEquals(new TypeToken<List<String>>() {},
        Serializers.getSourceType(TestSerializer.class));
    assertEquals(new TypeToken<Map<String, Boolean>>() {},
        Serializers.getTargetType(TestSerializer.class));
  }

  @Test
  public void testGetSerializerSourceAndTarget_classInheritance() {
    abstract class ParentTestSerializer<P, Q> implements Transformer<List<P>, Q> {
    }
    abstract class TestSerializer extends ParentTestSerializer<Float, Map<String, Boolean>> {
    }
    assertEquals(new TypeToken<List<Float>>() {},
        Serializers.getSourceType(TestSerializer.class));
    assertEquals(new TypeToken<Map<String, Boolean>>() {},
        Serializers.getTargetType(TestSerializer.class));
  }

  @Test
  public void testGetSerializerSourceAndTarget_interfaceInheritance() {
    abstract class TestSerializer implements ChildSerializerInterface<Float, List<Double>> {
    }
    assertEquals(new TypeToken<List<Float>>() {},
        Serializers.getSourceType(TestSerializer.class));
    assertEquals(new TypeToken<List<Double>>() {},
        Serializers.getTargetType(TestSerializer.class));
  }

  @SuppressWarnings("unchecked")
  public void testGetSerializerSourceAndTarget_notSerializers() {
    assertNull(Serializers.getSourceType(null));
    assertNull(Serializers.getTargetType(null));
    assertNull(Serializers.getSourceType(
        (Class<? extends Transformer<?, ?>>) (Class<?>) List.class));
    assertNull(Serializers.getTargetType(
        (Class<? extends Transformer<?, ?>>) (Class<?>) List.class));
  }

  @SuppressWarnings("unchecked")
  public void testInstantiate_valid() {
    ToMagicNumberSerializer serializer = Serializers.instantiate(
        ToMagicNumberSerializer.class, TypeToken.of(String.class));
    assertEquals(42, serializer.transformTo("").intValue());
  }

  @Test
  public void testInstantiate_invalid() {
    try {
      Serializers.instantiate(PrivateConstructorSerializer.class, TypeToken.of(String.class));
      fail("Instatiation should fail for non-serializers.");
    } catch (RuntimeException expected) {
      // expected
    }
  }

  @Test
  public void testInstantiate_typeConstructor() {
    Transformer<List<?>, Type> serializer;
    serializer =
        Serializers.instantiate(ListToTypeSerializer.class, TypeToken.of(ImmutableList.class));
    assertEquals(ImmutableList.class, serializer.transformTo(ImmutableList.of()));
    Type typeWithGeneric = new TypeToken<List<?>>() {}.getType();
    serializer = Serializers.instantiate(ListToTypeSerializer.class, TypeToken.of(typeWithGeneric));
    assertEquals(typeWithGeneric, serializer.transformTo(ImmutableList.of()));
    assertEquals(ImmutableList.of(typeWithGeneric), serializer.transformFrom(List.class));
    try {
      Serializers.instantiate(ListToTypeSerializer.class, TypeToken.of(Collection.class));
      fail("Shouldn't be able to instantiate with Collection");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testInstantiate_classConstructor() {
    Transformer<Set<?>, Type> serializer;
    serializer =
        Serializers.instantiate(SetToTypeSerializer.class, TypeToken.of(ImmutableSet.class));
    TypeToken<?> typeWithGeneric = new TypeToken<TreeSet<?>>() {};
    serializer = Serializers.instantiate(SetToTypeSerializer.class, typeWithGeneric);
    assertEquals(TreeSet.class, serializer.transformTo(ImmutableSet.of()));
    try {
      Serializers.instantiate(SetToTypeSerializer.class, TypeToken.of(Collection.class));
      fail("Shouldn't be able to instantiate with Collection");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testInstantiate_noConstructor() {
    try {
      Serializers.instantiate(NoValidConstructorSerializer.class, TypeToken.of(String.class));
      fail("Shouldn't be able to instantiate with Collection");
    } catch (IllegalStateException e) {
      String expected = "Failed to instantiate custom serializer "
          + NoValidConstructorSerializer.class.getName() + ", constructors not found: "
          + "[(interface java.lang.reflect.Type), (class java.lang.Class), ()]";
      assertEquals(expected, e.getMessage());
    }
  }

  private static class PrivateConstructorSerializer
      extends DefaultValueSerializer<String, Integer> {
    private PrivateConstructorSerializer() {}
  }

  static class ToMagicNumberSerializer extends DefaultValueSerializer<String, Integer> {
    public ToMagicNumberSerializer() {
      super(42, null);
    }
  }

  private interface ChildSerializerInterface<P, Q> extends Transformer<List<P>, Q> {
  }

  static class ListToTypeSerializer extends DefaultValueSerializer<List<?>, Type> {
    public ListToTypeSerializer(Type type) {
      super(type, ImmutableList.of(type));
    }

    public ListToTypeSerializer(Class<?> clazz) {
      throw new UnsupportedOperationException("Constructor should not have been called");
    }

    public ListToTypeSerializer() {
      throw new UnsupportedOperationException("Constructor should not have been called");
    }
  }

  static class SetToTypeSerializer extends DefaultValueSerializer<Set<?>, Type> {
    SetToTypeSerializer(Class<?> clazz) {
      super(clazz, null);
    }

    public SetToTypeSerializer() {
      throw new UnsupportedOperationException("Constructor should not have been called");
    }
  }

  static class NoValidConstructorSerializer extends DefaultValueSerializer<String, Integer> {
    public NoValidConstructorSerializer(String str) {
      super(42, null);
    }
  }
}
