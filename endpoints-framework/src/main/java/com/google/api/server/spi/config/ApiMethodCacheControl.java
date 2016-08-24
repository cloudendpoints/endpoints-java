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
 * Annotation for configuration of API method cache control.
 * @deprecated ApiMethodCacheControl is deprecated and will be removed in a future version of
 * Cloud Endpoints.
 */
// TODO: Delete this after a sufficient deprecation period.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Deprecated
public @interface ApiMethodCacheControl {

  /**
   * Disables caching of this method. The default value is true, so merely
   * adding this annotation to your method config will disable caching for that
   * method, unless you set this field to {@code false}.
   */
  boolean noCache() default true;

  /**
   * Overrides the maximum age to cache responses from this method.
   */
  int maxAge() default 0;
}
