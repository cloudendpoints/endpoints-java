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

import com.google.api.server.spi.config.ApiTransformer;
import com.google.api.server.spi.config.Transformer;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Common utility functions for {@link Transformer}s.
 */
public final class Serializers {

  private enum SerializerConstructor {
    TYPE {
      @Override
      <S extends Transformer<?, ?>> S construct(Class<S> serializerClass, Type sourceType)
          throws Exception {
        return serializerClass.getDeclaredConstructor(Type.class).newInstance(sourceType);
      }

      @Override
      ImmutableList<Type> getArgs() {
        return ImmutableList.<Type>of(Type.class);
      }
    },
    CLASS {
      @Override
      <S extends Transformer<?, ?>> S construct(Class<S> serializerClass, Type sourceType)
          throws Exception {
        Class<?> sourceClass = TypeToken.of(sourceType).getRawType();
        return serializerClass.getDeclaredConstructor(Class.class).newInstance(sourceClass);
      }

      @Override
      ImmutableList<Type> getArgs() {
        return ImmutableList.<Type>of(Class.class);
      }
    },
    DEFAULT {
      @Override
      <S extends Transformer<?, ?>> S construct(Class<S> serializerClass, Type sourceType)
          throws Exception {
        return serializerClass.getDeclaredConstructor().newInstance();
      }

      @Override
      ImmutableList<Type> getArgs() {
        return ImmutableList.<Type>of();
      }
    };

    abstract <S extends Transformer<?, ?>> S construct(Class<S> serializerClass, Type sourceType)
          throws Exception;

    abstract ImmutableList<Type> getArgs();

    @Override
    public String toString() {
      return String.format("(%s)", Joiner.on(", ").join(getArgs()));
    }
  }

  private Serializers() { }

  /**
   * Instantiates a new serializer instance.
   *
   * @param serializerClass a serializer class
   * @param sourceType the type being serialized
   * @return an instance of the serializer
   * @throws IllegalStateException if instantiation failed, or the class does not implement
   *     Serializer
   */
  @SuppressWarnings({"unchecked"})
  public static <S extends Transformer<?, ?>> S instantiate(final Class<S> serializerClass,
      TypeToken<?> sourceType) {
    if (!getSourceType(serializerClass).isSupertypeOf(sourceType)) {
      throw new IllegalArgumentException(String.format(
          "Can not instantiate %s, the serializer source %s is not assignable from %s",
          serializerClass, getSourceType(serializerClass), sourceType));
    }
    for (SerializerConstructor constructor : SerializerConstructor.values()) {
      try {
        return constructor.construct(serializerClass, sourceType.getType());
      } catch (NoSuchMethodException e) {
        continue;
      } catch (Exception e) {
        String errorMessage = String.format(
            "Failed to instantiate custom serializer constructor %s%s with source type: %s",
            serializerClass.getName(),
            constructor,
            sourceType);
        throw new IllegalStateException(errorMessage, e);
      }
    }
    String message = String.format(
        "Failed to instantiate custom serializer %s, constructors not found: %s",
        serializerClass.getName(),
        Arrays.toString(SerializerConstructor.values()));
    throw new IllegalStateException(message);
  }

  /**
   * Gets the {@link Transformer} class for a particular data type. This first checks to see if the
   * type or any of its parents is annotated with {@link ApiTransformer}. If that fails, a lookup
   * is made in the serialization config.
   *
   * @param type a type to find a serializer for
   * @param config a serialization config
   * @return All matching serializers. Could be &gt; 1 if multiple interfaces provide serializers.
   */
  public static List<Class<? extends Transformer<?, ?>>> getSerializerClasses(
      @Nullable Type type, @Nullable final ApiSerializationConfig config) {
    if (type == null) {
      return Collections.emptyList();
    }
    return getSerializerClasses(TypeToken.of(type), config);
  }

  public static List<Class<? extends Transformer<?, ?>>> getSerializerClasses(
      TypeToken<?> type, @Nullable final ApiSerializationConfig config) {
    if (type == null) {
      return Collections.emptyList();
    }

    List<Class<? extends Transformer<?, ?>>> allParentSerializers = Lists.newArrayList();
    List<TypeToken<?>> serializedTypes = Lists.newArrayList();
    for (TypeToken<?> typeToken : type.getTypes()) {
      ApiTransformer apiSerialization = typeToken.getRawType().getAnnotation(ApiTransformer.class);
      if (isSupertypeOf(typeToken, serializedTypes)) {
        continue;
      }
      if (apiSerialization != null) {
        allParentSerializers.add(apiSerialization.value());
        serializedTypes.add(typeToken);
      } else if (config != null) {
        ApiSerializationConfig.SerializerConfig serializerConfig =
            config.getSerializerConfig(typeToken);
        if (serializerConfig != null) {
          allParentSerializers.add(serializerConfig.getSerializer());
          serializedTypes.add(typeToken);
        }
      }
    }

    return allParentSerializers;
  }

  /**
   * Gets the {@code Serializer} source type for a class. This resolves placeholders in generics.
   *
   * @param clazz a class, possibly implementing {@code Transformer}
   * @return the resolved source type, null if clazz is not a serializer
   */
  @Nullable
  public static TypeToken<?> getSourceType(Class<? extends Transformer<?, ?>> clazz) {
    try {
      TypeToken<?> token = TypeToken.of(clazz);
      return token.resolveType(
          Transformer.class.getMethod("transformFrom", Object.class).getGenericReturnType());
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  /**
   * Gets the {@code Serializer} target type for a class. This resolves placeholders in generics.
   *
   * @param clazz a class, possibly implementing {@code Transformer}
   * @return the resolved target type, null if clazz is not a serializer
   */
  @Nullable
  public static TypeToken<?> getTargetType(Class<? extends Transformer<?, ?>> clazz) {
    try {
      TypeToken<?> token = TypeToken.of(clazz);
      return token.resolveType(
          Transformer.class.getMethod("transformTo", Object.class).getGenericReturnType());
    } catch (NoSuchMethodException e) {
      return null;
    }
  }


  @Nullable
  private static ParameterizedType getResolvedType(
      @Nullable Class<? extends Transformer<?, ?>> clazz) {
    if (clazz == null || !Transformer.class.isAssignableFrom(clazz)) {
        return null;
    }
    for (TypeToken<?> token : TypeToken.of(clazz).getTypes().interfaces()) {
      if (token.getRawType().equals(Transformer.class)) {
        Type tokenType = token.getType();
        return tokenType instanceof ParameterizedType ? (ParameterizedType) tokenType : null;
      }
    }
    return null;
  }

  private static boolean isSupertypeOf(TypeToken<?> typeToken, List<TypeToken<?>> subtypes) {
    for (TypeToken<?> subType : subtypes) {
      if (typeToken.isSupertypeOf(subType)) {
        return true;
      }
    }
    return false;
  }
}
