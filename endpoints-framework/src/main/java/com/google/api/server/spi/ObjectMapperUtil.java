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

import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.model.ApiSerializationConfig;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;

import com.google.api.server.spi.config.model.EndpointsFlag;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * Utilities for {@link ObjectMapper}.
 */
public class ObjectMapperUtil {

  /**
   * Creates an Endpoints standard object mapper that allows unquoted field names and unknown
   * properties.
   *
   * Note on unknown properties: When Apiary FE supports a strict mode where properties
   * are checked against the schema, BE can just ignore unknown properties.  This way, FE does
   * not need to filter out everything that the BE doesn't understand.  Before that's done,
   * a property name with a typo in it, for example, will just be ignored by the BE.
   */
  public static ObjectMapper createStandardObjectMapper() {
    return createStandardObjectMapper(null);
  }

  /**
   * Creates an Endpoints standard object mapper that allows unquoted field names and unknown
   * properties.
   *
   * Note on unknown properties: When Apiary FE supports a strict mode where properties
   * are checked against the schema, BE can just ignore unknown properties.  This way, FE does
   * not need to filter out everything that the BE doesn't understand.  Before that's done,
   * a property name with a typo in it, for example, will just be ignored by the BE.
   */
  public static ObjectMapper createStandardObjectMapper(ApiSerializationConfig config) {
    ObjectMapper objectMapper = new ObjectMapper()
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setBase64Variant(Base64Variants.MODIFIED_FOR_URL)
        .setSerializerFactory(
            BeanSerializerFactory.instance.withSerializerModifier(new DeepEmptyCheckingModifier()));
    AnnotationIntrospector pair = EndpointsFlag.JSON_USE_JACKSON_ANNOTATIONS.isEnabled()
        ? AnnotationIntrospector.pair(
            new ApiAnnotationIntrospector(config),
            new JacksonAnnotationIntrospector())
        : new ApiAnnotationIntrospector(config);
    objectMapper.setAnnotationIntrospector(pair);
    return objectMapper;
  }

  /**
   * A {@link BeanSerializerModifier} which modifies output to omit deeply empty collections.
   * A collection is considered empty if it has zero elements, or all of its elements are also
   * deeply empty, recursively.
   */
  private static class DeepEmptyCheckingModifier extends BeanSerializerModifier {
    @Override
    public JsonSerializer<?> modifyArraySerializer(SerializationConfig config, ArrayType valueType,
        BeanDescription beanDesc, JsonSerializer<?> serializer) {
      return new DeepEmptyCheckingSerializer<>(serializer);
    }

    @Override
    public JsonSerializer<?> modifyCollectionSerializer(SerializationConfig config,
        CollectionType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
      return new DeepEmptyCheckingSerializer<>(serializer);
    }

    @Override
    public JsonSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType,
        BeanDescription beanDesc, JsonSerializer<?> serializer) {
      if (serializer instanceof MapSerializer) {
        // TODO: We should probably be propagating the NON_EMPTY inclusion here, but it's breaking
        // discovery.
        return new DeepEmptyCheckingSerializer<>(serializer);
      }
      return serializer;
    }
  }

  /**
   * A {@link JsonSerializer} whose {@link #isEmpty(SerializerProvider, Object)} method checks for
   * "deep" emptiness, rather than simply calling the container's empty method. In this case, a
   * container is considered empty if all of its values are null or are containers that are deeply
   * empty.
   */
  private static class DeepEmptyCheckingSerializer<T> extends JsonSerializer<T> implements
      ContextualSerializer {
    private final JsonSerializer<T> delegate;

    DeepEmptyCheckingSerializer(JsonSerializer<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      delegate.serialize(value, gen, serializers);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Object value) {
      return ObjectMapperUtil.isEmpty(value);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
        throws JsonMappingException {
      if (delegate instanceof ContextualSerializer) {
        return new DeepEmptyCheckingSerializer<>(
            ((ContextualSerializer) delegate).createContextual(provider, property));
      }
      return this;
    }
  }

  private static boolean isEmpty(Object value) {
    Class<?> clazz = value.getClass();
    if (clazz.isArray()) {
      int len = Array.getLength(value);
      for (int i = 0; i < len; i++) {
        Object element = Array.get(value, i);
        if (element != null && !isEmpty(element)) {
          return false;
        }
      }
      return true;
    } else if (Collection.class.isAssignableFrom(clazz)) {
      Collection<?> c = (Collection<?>) value;
      for (Object element : c) {
        if (element != null && !isEmpty(element)) {
          return false;
        }
      }
      return true;
    } else if (Map.class.isAssignableFrom(clazz)) {
      Map<?, ?> m = (Map<?, ?>) value;
      for (Object entryValue : m.values()) {
        if (entryValue != null && !isEmpty(entryValue)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}
