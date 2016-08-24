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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify that the annotated class should use the same {@link Api} configuration
 * information as the referenced class. This referenced configuration is used instead of any
 * configuration inherited from {@link Api} annotations or other {@code ApiReference} annotations
 * on super classes.
 *
 * If a single class is annotated with both {@link Api} and {@link ApiReference} annotations,
 * configuration is first retrieved using the {@link ApiReference} annotation and is then overridden
 * with configuration from the {@link Api} annotation.
 *
 * @author Eric Orth
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiReference {
  /**
   * The class to which this ApiReference annotation will act as a reference. The referenced class
   * or an inherited superclass of the referenced class must have API configuration provided
   * from an {@link Api} annotation or another {@link ApiReference} annotation.
   */
  Class<?> value();
}
