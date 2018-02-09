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
package com.google.api.server.spi.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for configuring bean properties for API resources.
 * <p>
 * This annotation can be used on all parts of the definition of a property: the field, the
 * accessor (getter), or the mutator (setter). However, it should only be used on one of the three;
 * behavior for multiple annotations on one property is not defined.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiResourceProperty {
  /**
   * The name that the property represented by the annotated getter, setter, or field should appear
   * as in the API.
   */
  String name() default "";

  /**
   * The description that the property represented by the annotated getter, setter, or field should appear
   * as in the API.
   */
  String description() default "";

  /**
   * Whether or not the property represented by the annotated getter, setter or field should be
   * ignored for the API.
   */
  AnnotationBoolean ignored() default AnnotationBoolean.UNSPECIFIED;
}
