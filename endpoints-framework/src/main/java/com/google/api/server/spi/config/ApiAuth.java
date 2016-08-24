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
 * Annotation for API authentication configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiAuth {

  /**
   * Allows cookie authentication to be used for the API. By default, API
   * methods do not allow cookie authentication, and require the use of
   * OAuth2. Setting this field to {@code true} will allow cookies to be used
   * to access the API, with potentially dangerous results.  Please be very
   * cautious in enabling this setting, and make sure to require appropriate
   * XSRF tokens to protect your API.
   */
  // TODO: Rename to "allowCookies" (do the same in .api)
  AnnotationBoolean allowCookieAuth() default AnnotationBoolean.UNSPECIFIED;

  /**
   * A list of ISO region codes to block. By default, APIs allow all regions.
   */
  String[] blockedRegions() default {};
}
