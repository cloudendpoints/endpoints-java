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
 * Fields populated in Discovery in order to proper namespace generated clients.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiNamespace {
  /**
   * This is the domain name commonly associated with owner.
   * If, for instance, this is an API owned by NASA, then "nasa.gov" would be a reasonable choice
   * for domain name.
   * It does not necessarily need to be the same as the serving domain, though the latter will be
   * picked as a default if {@code ownerDomain} is not set.
   *
   * Required for a specified namespace.
   */
  String ownerDomain();

  /**
   * This is a canonical company name obeying the same rules as the canonical API name.
   * It obeys the exact capitalization a company would like to use to represent themselves.
   * If their name is really two dwords, they are separated by spaces.
   * E.g. "YouTube", "NASA" , "3Com", "Fox News".
   *
   * Required for a specified namespace.
   */
  String ownerName();

  /**
   * This is an optional way to further scope a generated client library.
   * It follows the same name rules for ownerName, with the restriction that words must be
   * alphanumeric.
   * This feature allows an enterprise to group one or more of their APIs from
   * different Endpoints instances together in a logical space more fine-grained than just their
   * domain name. If the package includes multiple words, each word will, when possible, be used as
   * an additional level in the scope of the package.
   */
  String packagePath() default "";
}