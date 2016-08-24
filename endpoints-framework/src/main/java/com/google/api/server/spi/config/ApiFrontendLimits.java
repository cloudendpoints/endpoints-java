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
 * Annotation for configuration of API quota frontend limits.
 * These are the limits applied to unregistered users of your API.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiFrontendLimits {

  /**
   * Sets the number of queries per second an unregistered user can make
   * against your API. The default is unlimited.  If unauthenticated, each IP
   * address is treated as a distinct user.
   */
  int unregisteredUserQps() default Api.UNSPECIFIED_INT;

  /**
   * Sets the maximum number of queries per second across all unregistered
   * access to your API. The default is unlimited.
   */
  int unregisteredQps() default Api.UNSPECIFIED_INT;

  /**
   * Sets the maximum number of requests per day for unregistered access to your
   * API. The default is unlimited.
   */
  int unregisteredDaily() default Api.UNSPECIFIED_INT;

  /**
   * Sets custom rules for unregistered traffic to your API. See
   * {@link ApiFrontendLimitRule} for details.
   */
  ApiFrontendLimitRule[] rules() default {};
}
