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

import com.google.api.client.util.GenericData;
import com.google.api.client.util.Preconditions;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.ResourceTransformer;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

/**
 * Utilities for dealing with type information.
 */
public abstract class Types {
  /**
   * Returns whether or not this type should be treated as a JSON array type. This includes all
   * array and {@link Collection} types, except for byte arrays, which are treated as base64
   * encoded strings.
   */
  public static boolean isArrayType(TypeToken<?> type) {
    return getArrayItemType(type) != null && !type.isSubtypeOf(byte[].class);
  }

  /**
   * Returns whether or not this type is an {@link Enum}.
   */
  public static boolean isEnumType(TypeToken<?> type) {
    return type.isSubtypeOf(Enum.class);
  }

  /**
   * Returns whether or not this type is a {@link Map}. This excludes {@link GenericData}, which is
   * used by the Google Java client library as a supertype of resource types with concrete fields.
   */
  public static boolean isMapType(TypeToken<?> type) {
    return type.isSubtypeOf(Map.class) && !isJavaClientEntity(type);
  }

  /**
   * Returns whether or not this type is a Google Java client library entity.
   */
  public static boolean isJavaClientEntity(TypeToken<?> type) {
    return type.isSubtypeOf(GenericData.class);
  }

  /**
   * Returns whether or not this type is an unresolved type variable.
   */
  public static boolean isTypeVariable(TypeToken<?> type) {
    return type.getType() instanceof TypeVariable;
  }

  public static boolean isCollectionResponseType(TypeToken<?> type) {
    return type.isSubtypeOf(CollectionResponse.class);
  }

  public static boolean isWildcardType(TypeToken<?> type) {
    Type javaType = type.getType();
    return javaType instanceof WildcardType
        || (javaType instanceof TypeVariable
            && ((TypeVariable) javaType).getName().startsWith("capture"));
  }

  /**
   * Returns whether or not this type is {@link Object}, excluding subtypes.
   */
  public static boolean isObject(TypeToken<?> type) {
    return type.getType() == Object.class;
  }


  /**
   * Gets a simple name for a type that's suitable for use as a schema name. This will resolve any
   * transformations on the type, which may affect the type name.
   */
  public static String getSimpleName(TypeToken<?> type, ApiSerializationConfig config) {
    if (type == null) {
      return null;
    }
    TypeToken<?> itemType = getArrayItemType(type);
    if (itemType != null) {
      return getSimpleName(itemType, config) + "Collection";
    } else if (type.getType() instanceof ParameterizedType) {
      Class<?> clazz = type.getRawType();
      StringBuilder builder = new StringBuilder();
      builder.append(clazz.getSimpleName());
      for (Type typeArg : clazz.getTypeParameters()) {
        builder.append('_');
        builder.append(getSimpleName(type.resolveType(typeArg), config));
      }
      return builder.toString();
    } else {
      Class<? extends Transformer<?, ?>> serializerClass = Iterables.getOnlyElement(
          Serializers.getSerializerClasses(type, config), null);
      if (serializerClass != null && ResourceTransformer.class.isAssignableFrom(serializerClass)) {
        @SuppressWarnings("unchecked")
        ResourceTransformer<?> resourceSerializer =
            (ResourceTransformer<?>) Serializers.instantiate(serializerClass, type);
        ResourceSchema resourceSchema = resourceSerializer.getResourceSchema();
        if (resourceSchema != null && resourceSchema.getName() != null) {
          return resourceSchema.getName();
        }
      }
      String collectionName = FieldType.fromType(type).getCollectionName();
      return collectionName != null ? collectionName : type.getRawType().getSimpleName();
    }
  }

  /**
   * Gets the element type of a type we want to treat as an array. Actual arrays or subtypes of
   * {@link java.util.Collection} can be treated as arrays. Returns null if the type cannot be
   * treated as an array.
   */
  public static TypeToken<?> getArrayItemType(TypeToken<?> type) {
    if (type.isSubtypeOf(Collection.class)) {
      return type.resolveType(Collection.class.getTypeParameters()[0]);
    } else if (type.isArray()) {
      return type.getComponentType();
    }
    return null;
  }

  /**
   * Returns the type parameter at a specified index.
   *
   * @throws IllegalArgumentException if the type is not parameterized
   * @throws IndexOutOfBoundsException if the type doesn't have enough type parameters
   */
  public static TypeToken<?> getTypeParameter(TypeToken<?> type, int index) {
    Preconditions.checkArgument(
        type.getType() instanceof ParameterizedType, "type is not parameterized");
    Type[] typeArgs = ((ParameterizedType) type.getType()).getActualTypeArguments();
    if (typeArgs.length <= index) {
      throw new IndexOutOfBoundsException(
          String.format("type '%s' has %d <= %d type arguments", type, typeArgs.length, index));
    }
    return type.resolveType(typeArgs[index]);
  }
}
