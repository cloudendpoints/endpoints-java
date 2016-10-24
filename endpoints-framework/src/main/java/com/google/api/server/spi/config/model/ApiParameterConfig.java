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

import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.config.ResourceTransformer;
import com.google.api.server.spi.config.Transformer;
import com.google.common.reflect.TypeToken;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for a single parameter of a Swarm Endpoint method.
 */
public class ApiParameterConfig {
  private final ApiMethodConfig apiMethodConfig;
  private final String name;
  private final String description;
  private final boolean nullable;
  private final String defaultValue;
  private final TypeToken<?> type;

  private Class<? extends Transformer<?, ?>> serializer;
  private Class<? extends Transformer<?, ?>> repeatedItemSerializer;

  private final TypeLoader typeLoader;

  /**
   * Classification of how the parameter is used within Endpoints method calls.
   */
  public enum Classification {
    /** Parameter that will be automatically injected by the SPI framework.*/
    INJECTED,
    /** Parameter that will be filled by the value of an API parameter.  Generally used for simple
     *  values like an ID. */
    API_PARAMETER,
    /** Parameter that will be filled with an API entity.  Generally used for the resource object
     *  in REST-style APIs. */
    RESOURCE,
    /** Parameter that could not be classified because its type is unknown.*/
    UNKNOWN
  }

  public ApiParameterConfig(ApiMethodConfig apiMethodConfig, String name, String description,
      boolean nullable, String defaultValue, TypeToken<?> type, TypeLoader typeLoader) {
    this.apiMethodConfig = apiMethodConfig;
    this.name = name;
    this.description = description;
    this.nullable = nullable;
    this.defaultValue = defaultValue;
    this.type = type;
    this.serializer = null;
    this.repeatedItemSerializer = null;
    this.typeLoader = typeLoader;
  }

  public ApiParameterConfig(ApiParameterConfig original, ApiMethodConfig apiMethodConfig) {
    this.apiMethodConfig = apiMethodConfig;
    this.name = original.name;
    this.description = original.description;
    this.nullable = original.nullable;
    this.defaultValue = original.defaultValue;
    this.type = original.type;
    this.serializer = original.serializer;
    this.repeatedItemSerializer = original.repeatedItemSerializer;
    this.typeLoader = original.typeLoader;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof ApiParameterConfig) {
      ApiParameterConfig parameter = (ApiParameterConfig) o;
      return Objects.equals(name, parameter.name)
          && nullable == parameter.nullable
          && Objects.equals(defaultValue, parameter.defaultValue)
          && Objects.equals(type, parameter.type)
          && Objects.equals(serializer, parameter.serializer)
          && Objects.equals(repeatedItemSerializer, parameter.repeatedItemSerializer)
          && Objects.equals(typeLoader, parameter.typeLoader);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, nullable, defaultValue, type, serializer, repeatedItemSerializer,
        typeLoader);
  }

  public ApiMethodConfig getApiMethodConfig() {
    return apiMethodConfig;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean getNullable() {
    return nullable;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public TypeToken<?> getType() {
    return type;
  }

  /**
   * If the serialized type of the parameter is a repeated type, returns the individual item type.
   * Otherwise returns {@code null}.
   */
  public TypeToken<?> getRepeatedItemType() {
    return Types.getArrayItemType(getSchemaBaseType());
  }

  /**
   * @return {@code true} iff the serialized type of the parameter is a repeated type.
   */
  public boolean isRepeated() {
    return !typeLoader.isInjectedType(getType()) && Types.isArrayType(getSchemaBaseType());
  }

  /**
   * @return {@code true} iff the serialized type (or serialized item type) of the parameter is an
   * enum type.
   */
  public boolean isEnum() {
    if (typeLoader.isInjectedType(getType())) {
      return false;
    }

    TypeToken<?> type;
    if (isRepeated()) {
      type = getRepeatedItemSerializedType();
    } else {
      type = getSchemaBaseType();
    }

    return Types.isEnumType(type);
  }

  private List<Class<? extends Transformer<?, ?>>> tryFindDefaultSerializers(
      @Nullable TypeToken<?> type) {
    ApiSerializationConfig serializerConfig =
        apiMethodConfig.getApiClassConfig().getApiConfig().getSerializationConfig();
    return Serializers.getSerializerClasses(type, serializerConfig);
  }

  /**
   * @return The serializer to be used on the parameter.
   */
  public List<Class<? extends Transformer<?, ?>>> getSerializers() {
    if (serializer != null) {
      return Collections.<Class<? extends Transformer<?, ?>>>singletonList(serializer);
    } else {
      return tryFindDefaultSerializers(getType());
    }
  }

  public void setSerializer(Class<? extends Transformer<?, ?>> serializer) {
    this.serializer = serializer;
  }

  /**
   * If the serialized type of the parameter is a repeated type, returns the serializer to be used
   * on each individual item.  Otherwise returns {@code null}.
   */
  public List<Class<? extends Transformer<?, ?>>> getRepeatedItemSerializers() {
    if (repeatedItemSerializer != null) {
      return Collections.<Class<? extends Transformer<?, ?>>>singletonList(repeatedItemSerializer);
    } else {
      return tryFindDefaultSerializers(Types.getArrayItemType(getSchemaBaseType()));
    }
  }

  public void setRepeatedItemSerializer(Class<? extends Transformer<?, ?>> repeatedItemSerializer) {
    this.repeatedItemSerializer = repeatedItemSerializer;
  }

  /**
   * Gets the type that acts as the source for schema generation. In the case of resource
   * serialization, the schema is based on the type being serialized. Simple serializers just
   * convert from one type to another, in which case the schema would be derived from the target
   * type instead.
   */
  public TypeToken<?> getSchemaBaseType() {
    List<Class<? extends Transformer<?, ?>>> serializers = getSerializers();
    if (serializers.isEmpty()) {
      return getType();
    } else if (ResourceTransformer.class.isAssignableFrom(serializers.get(0))) {
      return getType();
    } else {
      return Serializers.getTargetType(serializers.get(0));
    }
  }

  /**
   * If the serialized type of the parameter is a repeated type, returns the serialized individual
   * item type.  Otherwise returns {@code null}.
   */
  public TypeToken<?> getRepeatedItemSerializedType() {
    List<Class<? extends Transformer<?, ?>>> serializers = getRepeatedItemSerializers();
    if (serializers.isEmpty()) {
      return getRepeatedItemType();
    } else {
      return Serializers.getTargetType(serializers.get(0));
    }
  }

  /**
   * Generates an API parameter classifcation based on the type.
   */
  public Classification getClassification() {
    if (typeLoader.isInjectedType(type) || StandardParameters.isStandardParamName(name)) {
      return Classification.INJECTED;
    }

    TypeToken<?> type;
    if (isRepeated()) {
      type = getRepeatedItemSerializedType();
    } else {
      type = getSchemaBaseType();
    }

    if (typeLoader.isParameterType(type) || Types.isEnumType(type)) {
      return Classification.API_PARAMETER;
    } else if (Types.isTypeVariable(type)) {
      return Classification.UNKNOWN;
    } else {
      return Classification.RESOURCE;
    }
  }
}
