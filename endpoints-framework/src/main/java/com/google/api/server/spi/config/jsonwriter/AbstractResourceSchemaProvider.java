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


import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.ResourceTransformer;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.Serializers;
import com.google.common.reflect.TypeToken;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Provider for a resource schema that returns schema if a resource serializer is provided.
 */
abstract class AbstractResourceSchemaProvider implements ResourceSchemaProvider {

  @Nullable
  @Override
  public ResourceSchema getResourceSchema(TypeToken<?> type, ApiConfig config) {
    return getResourceSchemaImpl(type, config);
  }

  @Nullable
  private <T> ResourceSchema getResourceSchemaImpl(TypeToken<T> type, ApiConfig config) {
    Class<? super T> clazz = type.getRawType();
    List<Class<? extends Transformer<?, ?>>> serializerClasses =
        Serializers.getSerializerClasses(type, config.getSerializationConfig());
    if (!serializerClasses.isEmpty() &&
        ResourceTransformer.class.isAssignableFrom(serializerClasses.get(0))) {
      @SuppressWarnings("unchecked")
      ResourceTransformer<T> resourceSerializer =
          (ResourceTransformer<T>) Serializers.instantiate(serializerClasses.get(0), type);
      return resourceSerializer.getResourceSchema();
    }
    return null;
  }
}
