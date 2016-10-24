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

import com.google.api.server.spi.config.ApiConfigInconsistency;
import com.google.api.server.spi.config.Transformer;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flattened serialization configuration for a swarm endpoint.  Data generally originates from
 * {@link com.google.api.server.spi.config.ApiTransformer} annotations.
 */
public class ApiSerializationConfig {

  private final Map<TypeToken<?>, SerializerConfig> configs;

  public ApiSerializationConfig() {
    this.configs = new LinkedHashMap<>();
  }

  public ApiSerializationConfig(ApiSerializationConfig original) {
    this.configs = new LinkedHashMap<>(original.configs);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof ApiSerializationConfig) {
      ApiSerializationConfig config = (ApiSerializationConfig) o;
      return Iterables.isEmpty(getConfigurationInconsistencies(config));
    } else {
      return false;
    }
  }

  public Iterable<ApiConfigInconsistency<Map<TypeToken<?>, SerializerConfig>>>
      getConfigurationInconsistencies(ApiSerializationConfig config) {
    return ApiConfigInconsistency.<Map<TypeToken<?>, SerializerConfig>>listBuilder()
        .addIfInconsistent("serialization.configs", configs, config.configs)
        .build();
  }

  @Override
  public int hashCode() {
    return configs.hashCode();
  }

  public void addSerializationConfig(Class<? extends Transformer<?, ?>> serializer) {
    TypeToken<?> sourceType = Serializers.getSourceType(serializer);
    configs.put(sourceType, new SerializerConfig(sourceType, serializer));
  }

  public List<SerializerConfig> getSerializerConfigs() {
    return new ArrayList<>(configs.values());
  }

  public SerializerConfig getSerializerConfig(TypeToken<?> type) {
    return configs.get(type);
  }

  /**
   * A single serialization rule.
   */
  public static class SerializerConfig {
    private final TypeToken<?> sourceType;
    private final Class<? extends Transformer<?, ?>> serializer;

    public SerializerConfig(
        TypeToken<?> sourceType, Class<? extends Transformer<?, ?>> serializer) {
      this.sourceType = sourceType;
      this.serializer = serializer;
    }

    public TypeToken<?> getSourceType() {
      return sourceType;
    }

    public Class<? extends Transformer<?, ?>> getSerializer() {
      return serializer;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SerializerConfig) {
        SerializerConfig other = (SerializerConfig) obj;
        return sourceType.equals(other.sourceType) && serializer.equals(other.serializer);
      }
      return false;
    }
  }
}
