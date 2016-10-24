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

import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.common.reflect.TypeToken;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Provider for a resource schema using Jacksons serialization configuration.
 */
public class JacksonResourceSchemaProvider extends AbstractResourceSchemaProvider {

  private static final Logger logger =
      Logger.getLogger(JacksonResourceSchemaProvider.class.getName());

  @Override
  public ResourceSchema getResourceSchema(TypeToken<?> type, ApiConfig config) {
    ResourceSchema schema = super.getResourceSchema(type, config);
    if (schema != null) {
      return schema;
    }
    ObjectMapper objectMapper =
        ObjectMapperUtil.createStandardObjectMapper(config.getSerializationConfig());
    JavaType javaType = objectMapper.getTypeFactory().constructType(type.getRawType());
    BeanDescription beanDescription = objectMapper.getSerializationConfig().introspect(javaType);
    List<BeanPropertyDefinition> definitions = beanDescription.findProperties();
    ResourceSchema.Builder schemaBuilder = ResourceSchema.builderForType(type.getRawType());
    for (BeanPropertyDefinition definition : definitions) {
      TypeToken<?> propertyType = getPropertyType(type, toMethod(definition.getGetter()),
          toMethod(definition.getSetter()), definition.getField(), config);
      if (propertyType != null) {
        schemaBuilder.addProperty(definition.getName(), ResourcePropertySchema.of(propertyType));
      } else {
        logger.warning(
            "No type found for property " + definition.getName() + " on class " + type);
      }
    }
    return schemaBuilder.build();
  }

  private static Method toMethod(AnnotatedMethod am) {
    if (am != null) {
      return am.getAnnotated();
    }
    return null;
  }

  @Nullable
  private TypeToken<?> getPropertyType(TypeToken<?> beanType, Method readMethod, Method writeMethod,
      AnnotatedField field, ApiConfig config) {
    if (readMethod != null) {
      // read method's return type is the property type
      return ApiAnnotationIntrospector.getSchemaType(
          beanType.resolveType(readMethod.getGenericReturnType()), config);
    } else if (writeMethod != null) {
      Type[] paramTypes = writeMethod.getGenericParameterTypes();
      if (paramTypes.length == 1) {
        // write method's first parameter type is the property type
        return ApiAnnotationIntrospector.getSchemaType(
            beanType.resolveType(paramTypes[0]), config);
      }
    } else if (field != null) {
      return ApiAnnotationIntrospector.getSchemaType(
          beanType.resolveType(field.getGenericType()), config);
    }
    return null;
  }
}
