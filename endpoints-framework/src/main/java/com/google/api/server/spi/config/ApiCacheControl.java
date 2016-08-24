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
 * Annotation for API cache control configuration. Note that the API
 * frontend itself may act as a caching proxy.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiCacheControl {

  /** Constants of cache control types */
  class Type {
    /** Allows clients and proxies to cache responses */
    public static final String PUBLIC = "public";

    /** Allows only clients to cache responses */
    public static final String PRIVATE = "private";

    /** Allows none to cache responses */
    public static final String NO_CACHE = "no-cache";
  }

  /** The cache control type. Defaults to no-cache. */
  String type() default "";

  /** The maximum age that results may be cached.  If unspecified, defaults to 0. */
  int maxAge() default Api.UNSPECIFIED_INT;
}
