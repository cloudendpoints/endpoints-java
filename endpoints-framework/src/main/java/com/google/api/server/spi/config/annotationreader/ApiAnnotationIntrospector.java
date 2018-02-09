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

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceTransformer;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.config.model.Serializers;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A Jackson annotation introspector used to configure serialization.
 */
public class ApiAnnotationIntrospector extends NopAnnotationIntrospector {

  private final ApiSerializationConfig config;

  public ApiAnnotationIntrospector() {
    this(new ApiSerializationConfig());
  }

  public ApiAnnotationIntrospector(ApiSerializationConfig config) {
    this.config = config;
  }

  @Override
  public boolean hasIgnoreMarker(AnnotatedMember member) {
    ApiResourceProperty apiProperty = member.getAnnotation(ApiResourceProperty.class);
    return apiProperty != null && apiProperty.ignored() == AnnotationBoolean.TRUE;
  }

  @Override
  public PropertyName findNameForSerialization(Annotated a) {
    ApiResourceProperty apiName = a.getAnnotation(ApiResourceProperty.class);
    if (apiName != null && apiName.ignored() != AnnotationBoolean.TRUE) {
      return PropertyName.construct(apiName.name());
    }
    return null;
  }

  @Override
  public PropertyName findNameForDeserialization(Annotated a) {
    ApiResourceProperty annotation = findAnnotation(a);
    return annotation != null ? PropertyName.construct(annotation.name()) : null;
  }

  @Override
  public String findPropertyDescription(Annotated a) {
    ApiResourceProperty annotation = findAnnotation(a);
    return annotation != null ? annotation.description() : null;
  }

  private ApiResourceProperty findAnnotation(Annotated a) {
    ApiResourceProperty annotation = a.getAnnotation(ApiResourceProperty.class);
    return annotation != null && annotation.ignored() != AnnotationBoolean.TRUE ? annotation : null;
  }

  @Override
  public JsonSerializer<?> findSerializer(Annotated method) {
    return getJsonSerializer(findSerializerInstance(method));
  }

  @Override
  public String findEnumValue(Enum<?> value) {
    return value.name();
  }

  @Nullable
  private static <TFrom, TTo> JsonSerializer<TFrom> getJsonSerializer(
      @Nullable final Transformer<TFrom, TTo> serializer) {
    if (serializer == null) {
      return null;
    }
    return new JsonSerializer<TFrom>() {
      @Override
      public void serialize(TFrom value, JsonGenerator jgen, SerializerProvider provider)
          throws IOException {
        jgen.writeObject(serializer.transformTo(value));
      }
    };
  }

  @Override
  public JsonDeserializer<?> findDeserializer(Annotated a) {
    return getJsonDeserializer(findSerializerInstance(a));
  }

  @Override
  public VisibilityChecker<?> findAutoDetectVisibility(
      AnnotatedClass ac, VisibilityChecker<?> checker) {
    return checker.withSetterVisibility(Visibility.PUBLIC_ONLY);
  }

  private static <TFrom, TTo> JsonDeserializer<TFrom> getJsonDeserializer(
      @Nullable final Transformer<TFrom, TTo> serializer) {
    if (serializer == null) {
      return null;
    }
    final TypeReference<TTo> serializedType = typeReferenceOf(serializer);
    if (serializer instanceof ResourceTransformer) {
      @SuppressWarnings("unchecked")
      final ResourceTransformer<TFrom> resourceSerializer = (ResourceTransformer<TFrom>) serializer;
      return new ResourceDeserializer<>(resourceSerializer);
    } else {
      return new JsonDeserializer<TFrom>() {
        @Override
        public TFrom deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
          TTo deserialized = jp.readValueAs(serializedType);
          return serializer.transformFrom(deserialized);
        }
      };
    }
  }

  /**
   * Gets the schema type for a type. The schema type is identical to the original type if
   * there is no matching {@link com.google.api.server.spi.config.ApiTransformer} annotation for
   * the type. If there is a {@link com.google.api.server.spi.config.ResourceTransformer} installed,
   * the source type determines schema, not the output map.
   */
  public static TypeToken<?> getSchemaType(TypeToken<?> type, ApiConfig config) {
    Type rawType = type.getType();
    if (rawType instanceof Class || rawType instanceof ParameterizedType) {
      List<Class<? extends Transformer<?, ?>>> serializers =
          Serializers.getSerializerClasses(type, config.getSerializationConfig());

      if (!serializers.isEmpty() &&
          !(ResourceTransformer.class.isAssignableFrom(serializers.get(0)))) {
        TypeToken<?> sourceType = Serializers.getSourceType(serializers.get(0));
        TypeToken<?> serializedType = Serializers.getTargetType(serializers.get(0));

        Preconditions.checkArgument(
            sourceType.isSupertypeOf(type),
            "Serializer specified for %s, but only serializes for %s: %s",
            type,
            sourceType,
            serializers.get(0));
        Preconditions.checkArgument(
            serializedType != null,
            "Couldn't find Serializer interface in serializer for %s: %s",
            type,
            serializers.get(0));
        return serializedType;
      }
    }
    return type;
  }

  @Nullable
  private Transformer<?, ?> findSerializerInstance(Annotated a) {
    if (a instanceof AnnotatedClass) {
      AnnotatedClass clazz = (AnnotatedClass) a;
      List<Class<? extends Transformer<?, ?>>> serializerClasses =
          Serializers.getSerializerClasses(clazz.getRawType(), config);
      if (!serializerClasses.isEmpty()) {
        return Serializers.instantiate(serializerClasses.get(0), TypeToken.of(a.getRawType()));
      }
    }
    return null;
  }

  private static <T> TypeReference<T> typeReferenceOf(Transformer<?, T> serializer) {
    @SuppressWarnings("unchecked")
    Class<? extends Transformer<?, T>> serializerClass =
        (Class<? extends Transformer<?, T>>) serializer.getClass();
    final TypeToken<?> type = Serializers.getTargetType(serializerClass);
    return new TypeReference<T> (){
      @Override
      public Type getType() {
        return type.getType();
      }
    };
  }

  private static class ResourceDeserializer<TFrom> extends JsonDeserializer<TFrom> {
    private final ResourceTransformer<TFrom> resourceSerializer;

    private ResourceDeserializer(ResourceTransformer<TFrom> resourceSerializer) {
      this.resourceSerializer = resourceSerializer;
    }

    @Override
    public TFrom deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      Map<String, ResourcePropertySchema> properties =
          resourceSerializer.getResourceSchema().getProperties();
      ImmutableMap.Builder<String, Object> values = ImmutableMap.builder();
      while (jp.nextValue() != JsonToken.END_OBJECT) {
        String property = jp.getCurrentName();
        final ResourcePropertySchema schemaType = properties.get(property);
        if (schemaType != null) {
          TypeReference<Object> jacksonTypeRef = new TypeReference<Object> (){
            @Override
            public Type getType() {
              return schemaType.getJavaType();
            }
          };
          values.put(property, jp.readValueAs(jacksonTypeRef));
        }
        jp.skipChildren();
      }
      return resourceSerializer.transformFrom(values.build());
    }
  }
}
