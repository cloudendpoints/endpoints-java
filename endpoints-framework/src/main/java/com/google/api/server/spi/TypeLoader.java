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

import com.google.common.reflect.TypeToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for dealing with {@link Type} objects in endpoint config generation.
 *
 * @author Eric Orth
 */
public final class TypeLoader {
  private final Map<String, Class<?>> classTypes;

  /**
   * Mapping from Java types to parameter types in config (which are mapped to JSON schema
   * types and formats by Discovery Service).
   */
  private final Map<Class<?>, String> parameterTypes;

  /**
   * Mapping from Java types to JSON schema types.
   */
  private final Map<Class<?>, String> schemaTypes;
  private final Map<Class<?>, String> schemaFormats;

  private final Map<String, Class<? extends Annotation>> annotationTypes;
  private final Set<Class<?>> injectedClassTypes;

  public TypeLoader() throws ClassNotFoundException {
    this(TypeLoader.class.getClassLoader());
  }

  public TypeLoader(ClassLoader classLoader) throws ClassNotFoundException {
    classTypes = createClassTypes(classLoader);
    parameterTypes = createParameterTypes(classLoader);
    schemaTypes = createSchemaTypes(classLoader);
    schemaFormats = createSchemaFormats(classLoader);
    annotationTypes = createAnnotationTypes(classLoader);
    injectedClassTypes = createInjectedClassTypes(classLoader);
  }

  private static Map<String, Class<?>> createClassTypes(ClassLoader classLoader)
      throws ClassNotFoundException {
    Map<String, Class<?>> classTypes = new HashMap<String, Class<?>>();
    classTypes.put("HttpServletRequest",
        classLoader.loadClass("javax.servlet.http.HttpServletRequest"));
    classTypes.put("Collection", classLoader.loadClass("java.util.Collection"));
    classTypes.put("Map", classLoader.loadClass("java.util.Map"));
    classTypes.put("CollectionResponses",
        classLoader.loadClass("com.google.api.server.spi.response.CollectionResponse"));
    return Collections.unmodifiableMap(classTypes);
  }

  private static Map<Class<?>, String> createParameterTypes(ClassLoader classLoader)
      throws ClassNotFoundException {
    Map<Class<?>, String> parameterTypes = new HashMap<Class<?>, String>();
    parameterTypes.put(classLoader.loadClass("java.lang.String"), "string");
    parameterTypes.put(classLoader.loadClass("java.lang.Boolean"), "boolean");
    parameterTypes.put(Boolean.TYPE, "boolean");
    parameterTypes.put(classLoader.loadClass("java.lang.Integer"), "int32");
    parameterTypes.put(Integer.TYPE, "int32");
    parameterTypes.put(classLoader.loadClass("java.lang.Long"), "int64");
    parameterTypes.put(Long.TYPE, "int64");
    parameterTypes.put(classLoader.loadClass("java.lang.Float"), "float");
    parameterTypes.put(Float.TYPE, "float");
    parameterTypes.put(classLoader.loadClass("java.lang.Double"), "double");
    parameterTypes.put(Double.TYPE, "double");
    parameterTypes.put(byte[].class, "string");
    /*
     * TODO: Adding support for JSR 310 http://jcp.org/en/jsr/detail?id=310
     * LocalDate --> date, LocalDateTime --> datetime
     */
    parameterTypes.put(classLoader.loadClass("java.util.Date"), "datetime");
    parameterTypes.put(
        classLoader.loadClass("com.google.api.server.spi.types.DateAndTime"), "datetime");
    parameterTypes.put(
        classLoader.loadClass("com.google.api.server.spi.types.SimpleDate"), "date");
    try {
      parameterTypes.put(
          classLoader.loadClass("com.google.appengine.api.datastore.Blob"), "string");
    } catch (ClassNotFoundException e) {
      // Do nothing; if we're on Flex, this class won't exist.
    }
    return Collections.unmodifiableMap(parameterTypes);
  }

  private static Map<Class<?>, String> createSchemaTypes(ClassLoader classLoader)
      throws ClassNotFoundException {
    Map<Class<?>, String> schemaTypes = new HashMap<Class<?>, String>();
    schemaTypes.put(classLoader.loadClass("java.lang.String"), "string");
    schemaTypes.put(classLoader.loadClass("java.lang.Short"), "integer");
    schemaTypes.put(Short.TYPE, "integer");
    schemaTypes.put(classLoader.loadClass("java.lang.Byte"), "integer");
    schemaTypes.put(Byte.TYPE, "integer");
    schemaTypes.put(classLoader.loadClass("java.lang.Character"), "string");
    schemaTypes.put(Character.TYPE, "string");
    schemaTypes.put(classLoader.loadClass("java.lang.Integer"), "integer");
    schemaTypes.put(Integer.TYPE, "integer");
    schemaTypes.put(classLoader.loadClass("java.lang.Long"), "string");
    schemaTypes.put(Long.TYPE, "string");
    schemaTypes.put(classLoader.loadClass("java.lang.Float"), "number");
    schemaTypes.put(Float.TYPE, "number");
    schemaTypes.put(classLoader.loadClass("java.lang.Double"), "number");
    schemaTypes.put(Double.TYPE, "number");
    schemaTypes.put(classLoader.loadClass("java.lang.Boolean"), "boolean");
    schemaTypes.put(Boolean.TYPE, "boolean");
    schemaTypes.put(classLoader.loadClass("java.util.Date"), "string");
    schemaTypes.put(classLoader.loadClass("com.google.api.server.spi.types.DateAndTime"), "string");
    schemaTypes.put(classLoader.loadClass("com.google.api.server.spi.types.SimpleDate"), "string");
    schemaTypes.put(byte[].class, "string");
    try {
      schemaTypes.put(classLoader.loadClass("com.google.appengine.api.datastore.Blob"), "string");
    } catch (ClassNotFoundException e) {
      // Do nothing; if we're on Flex, this class won't exist.
    }
    return Collections.unmodifiableMap(schemaTypes);
  }

  private Map<Class<?>, String> createSchemaFormats(ClassLoader classLoader)
      throws ClassNotFoundException {
    Map<Class<?>, String> schemaFormats = new HashMap<Class<?>, String>();
    schemaFormats.put(classLoader.loadClass("java.lang.Long"), "int64");
    schemaFormats.put(Long.TYPE, "int64");
    schemaFormats.put(classLoader.loadClass("java.lang.Float"), "float");
    schemaFormats.put(Float.TYPE, "float");
    schemaFormats.put(classLoader.loadClass("java.util.Date"), "date-time");
    schemaFormats.put(
        classLoader.loadClass("com.google.api.server.spi.types.DateAndTime"), "date-time");
    schemaFormats.put(
        classLoader.loadClass("com.google.api.server.spi.types.SimpleDate"), "date");
    schemaFormats.put(byte[].class, "byte");
    try {
      schemaFormats.put(classLoader.loadClass("com.google.appengine.api.datastore.Blob"), "byte");
    } catch (ClassNotFoundException e) {
      // Do nothing; if we're on Flex, this class won't exist.
    }
    return Collections.unmodifiableMap(schemaFormats);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Class<? extends Annotation>> createAnnotationTypes(
      ClassLoader classLoader) throws ClassNotFoundException {
    Map<String, Class<? extends Annotation>> annotationTypes =
        new HashMap<String, Class<? extends Annotation>>();
    annotationTypes.put("Api", loadAnnotation(classLoader, "com.google.api.server.spi.config.Api"));
    annotationTypes.put("ApiReference",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.ApiReference"));
    annotationTypes.put("ApiClass",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.ApiClass"));
    annotationTypes.put("ApiMethod",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.ApiMethod"));
    annotationTypes.put("ApiAuth",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.ApiAuth"));
    annotationTypes.put("ApiFrontendLimits",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.ApiFrontendLimits"));
    annotationTypes.put("ApiFrontendLimitRule",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.ApiFrontendLimitRule"));
    annotationTypes.put("ApiCacheControl",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.ApiCacheControl"));
    annotationTypes.put("ApiNamespace",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.ApiNamespace"));
    annotationTypes.put("ApiTransformer",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.ApiTransformer"));
    annotationTypes.put("DefaultValue",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.DefaultValue"));
    annotationTypes.put("Description",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.Description"));
    annotationTypes.put("Named",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.Named"));
    annotationTypes.put("Nullable",
        loadAnnotation(classLoader, "com.google.api.server.spi.config.Nullable"));
    return Collections.unmodifiableMap(annotationTypes);
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends Annotation> loadAnnotation(ClassLoader classLoader,
      String fullClassName) throws ClassNotFoundException {
    return (Class<? extends Annotation>) classLoader.loadClass(fullClassName);
  }

  private Set<Class<?>> createInjectedClassTypes(ClassLoader classLoader)
      throws ClassNotFoundException {
    Set<Class<?>> injectedClassTypes = new HashSet<Class<?>>();
    try {
      injectedClassTypes.add(classLoader.loadClass("com.google.appengine.api.users.User"));
    } catch (ClassNotFoundException e) {
      // Do nothing; if we're on Flex, this class won't exist.
    }
    injectedClassTypes.add(classLoader.loadClass("javax.servlet.http.HttpServletRequest"));
    injectedClassTypes.add(classLoader.loadClass("javax.servlet.ServletContext"));
    injectedClassTypes.add(classLoader.loadClass("com.google.api.server.spi.auth.common.User"));
    return Collections.unmodifiableSet(injectedClassTypes);
  }

  public Map<String, Class<?>> getClassTypes() {
    return classTypes;
  }

  public Map<Class<?>, String> getParameterTypes() {
    return parameterTypes;
  }

  public Map<Class<?>, String> getSchemaTypes() {
    return schemaTypes;
  }

  public Map<Class<?>, String> getSchemaFormats() {
    return schemaFormats;
  }

  public Map<String, Class<? extends Annotation>> getAnnotationTypes() {
    return annotationTypes;
  }

  public boolean isInjectedType(TypeToken<?> type) {
    return injectedClassTypes.contains(type.getRawType());
  }

  public String getSchemaType(TypeToken<?> type) {
    return schemaTypes.get(type.getRawType());
  }

  public boolean isSchemaType(TypeToken<?> type) {
    return getSchemaType(type) != null;
  }

  public String getSchemaFormat(TypeToken<?> type) {
    return schemaFormats.get(type.getRawType());
  }

  public boolean isParameterType(TypeToken<?> type) {
    return parameterTypes.containsKey(type.getRawType());
  }
}
