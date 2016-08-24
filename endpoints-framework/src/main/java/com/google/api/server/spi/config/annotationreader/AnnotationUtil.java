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

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

/**
 * Utilities for accessing annotation values.
 */
public final class AnnotationUtil {

  private AnnotationUtil() {}

  /**
   * Returns an annotation of the ith parameter; or null if none.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Annotation> T getParameterAnnotation(
      Method method, int i, Class<T> annotationType) {
    for (Annotation annotation : method.getParameterAnnotations()[i]) {
      if (annotation.annotationType() == annotationType) {
        return (T) annotation;
      }
    }
    return null;
  }

  /**
   * Returns an annotation with the given name from the ith parameter; or null if none.
   *
   * @param method The method which will have its parameters searched for the annotation.
   * @param i The index specifying which parameter to search on the given method.
   * @param annotationName Fully qualified name of the java class for the target annotation type.
   * @param valueType If non-null, an annotation will only be considered a match if it has a
   *                  property "value" with the given type.
   */
  public static Annotation getParameterAnnotationWithName(Method method, int i,
      String annotationName, @Nullable Class<?> valueType) {
    for (Annotation annotation : method.getParameterAnnotations()[i]) {
      if (annotation.annotationType().getName().equals(annotationName)) {
        try {
          if (valueType == null
              || annotation.annotationType().getMethod("value").getReturnType().equals(valueType)) {
            return annotation;
          }
        } catch (NoSuchMethodException e) {
          // There was no expected "value" method.  Not a match, so move on.
        }
      }
    }
    return null;
  }

  /**
   * Returns the {@code @Named} annotation of the ith parameter.  Prefers the given internal type,
   * but if not present, returns the javax {@code @Named}.
   *
   * @param method The method which will have its parameters searched for the annotation.
   * @param i The index specifying which parameter to search on the given method.
   * @param internalNamedType The type to use for the SPI-owned version of {@code @Named}.
   */
  public static Annotation getNamedParameter(Method method, int i,
      Class<? extends Annotation> internalNamedType) {
    Annotation annotation = AnnotationUtil.getParameterAnnotation(method, i, internalNamedType);
    if (annotation == null) {
      annotation = AnnotationUtil.getParameterAnnotationWithName(method, i, "javax.inject.Named",
          String.class);
    }

    return annotation;
  }

  /**
   * Returns the {@code @Nullable} annotation of the ith parameter.  Prefers the given internal
   * type, but if not present, returns the javax {@code @Nullable}.
   *
   * @param method The method which will have its parameters searched for the annotation.
   * @param i The index specifying which parameter to search on the given method.
   * @param internalNullableType The type to use for the SPI-owned version of {@code @Nullable}.
   */
  public static Annotation getNullableParameter(Method method, int i,
      Class<? extends Annotation> internalNullableType) {
    Annotation annotation = AnnotationUtil.getParameterAnnotation(method, i, internalNullableType);
    if (annotation == null) {
      annotation = AnnotationUtil.getParameterAnnotationWithName(method, i,
          "javax.annotation.Nullable", null);
    }

    return annotation;
  }

  public static boolean isUnspecified(String[] values) {
    return values == null
        || (values.length == 1 && values[0].equals(Api.UNSPECIFIED_STRING_FOR_LIST));
  }

  public static boolean isUnspecified(Class<? extends Authenticator>[] values) {
    return values == null
        || (values.length == 1 && values[0].equals(Authenticator.class));
  }

  public static boolean isUnspecifiedPeerAuthenticators(
      Class<? extends PeerAuthenticator>[] values) {
    return values == null || (values.length == 1 && values[0].equals(PeerAuthenticator.class));
  }
}
