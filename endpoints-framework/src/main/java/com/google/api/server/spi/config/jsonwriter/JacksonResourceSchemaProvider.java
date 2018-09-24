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

import com.google.api.client.util.ClassInfo;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.Types;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.reflect.TypeToken;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Provider for a resource schema using Jacksons serialization configuration.
 */
public class JacksonResourceSchemaProvider extends AbstractResourceSchemaProvider {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

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
    ResourceSchema.Builder schemaBuilder = ResourceSchema.builderForType(type.getRawType());
    Set<String> genericDataFieldNames = getGenericDataFieldNames(type);
    for (BeanPropertyDefinition definition : beanDescription.findProperties()) {
      TypeToken<?> propertyType = getPropertyType(type, toMethod(definition.getGetter()),
          toMethod(definition.getSetter()), definition.getField(), config);
      String name = definition.getName();
      if (genericDataFieldNames == null || genericDataFieldNames.contains(name)) {
        if (hasUnresolvedType(propertyType)) {
          logger.atWarning().log("skipping field '%s' of type '%s' because it is unresolved.", name,
              propertyType);
          continue;
        }
        if (propertyType != null) {
          ResourcePropertySchema propertySchema = ResourcePropertySchema.of(propertyType);
          propertySchema.setDescription(definition.getMetadata().getDescription());
          schemaBuilder.addProperty(name, propertySchema);
        } else {
          logger.atWarning().log("No type found for property '%s' on class '%s'.", name, type);
        }
      } else {
        logger.atFine()
            .log("skipping field '%s' because it's not a Java client model field.", name);
      }
    }
    return schemaBuilder.build();
  }

  private static Set<String> getGenericDataFieldNames(TypeToken<?> type) {
    if (!Types.isJavaClientEntity(type)) {
      return null;
    }
    return ImmutableSet.copyOf(ClassInfo.of(type.getRawType(), false /* ignoreCase */).getNames());
  }

  private static boolean hasUnresolvedType(TypeToken<?> type) {
    Type javaType = type.getType();
    if (javaType instanceof ParameterizedType) {
      ParameterizedType p = (ParameterizedType) javaType;
      for (Type t : p.getActualTypeArguments()) {
        if (Types.isWildcardType(type.resolveType(t))) {
          logger.atWarning().log("skipping field of type '%s' because it is unresolved", type);
          return true;
        }
      }
    }
    return false;
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
