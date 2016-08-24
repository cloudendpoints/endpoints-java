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
 * Annotation for configuration of API quota frontend limit rules.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiFrontendLimitRule {

  /**
   * The matching rule that defines this segment. You can define the match in
   * terms of headers or query parameters.
   * For example "Referer=.*\.abc\.com" will define a match for traffic from
   * the abc.com site. The first matching rule will be used to define the limits
   * for a particular request.
   */
  String match() default "";

  /**
   * The total queries per second to allow for this rule. The default is unlimited.
   */
  int qps() default -1;

  /**
   * The per-user queries per second to allow for this rule. The default is unlimited.
   */
  int userQps() default -1;

  /**
   * The total queries per day to allow for this rule. The default is unlimited.
   */
  int daily() default -1;

  /**
   * Logs traffic for this rule under a specific analytics project id.
   */
  String analyticsId() default "";
}
