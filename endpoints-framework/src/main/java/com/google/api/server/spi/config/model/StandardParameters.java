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
package com.google.api.server.spi.config.model;

import com.google.api.server.spi.EndpointsContext;
import com.google.common.collect.ImmutableSet;

import javax.servlet.http.HttpServletRequest;

public final class StandardParameters {
  public static final String ALT = "alt";
  public static final String FIELDS = "fields";
  public static final String KEY = "key";
  public static final String OAUTH_TOKEN = "oauth_token";
  public static final String PRETTY_PRINT = "prettyPrint";
  public static final String QUOTA_USER = "quotaUser";
  public static final String USER_IP = "userIp";

  public static final ImmutableSet<String> STANDARD_PARAM_NAMES =
      new ImmutableSet.Builder<String>()
          .add(ALT)
          .add(FIELDS)
          .add(KEY)
          .add(OAUTH_TOKEN)
          .add(PRETTY_PRINT)
          .add(QUOTA_USER)
          .add(USER_IP)
          .build();

  private StandardParameters() {}

  public static boolean isStandardParamName(String paramName) {
    return STANDARD_PARAM_NAMES.contains(paramName);
  }

  public static boolean shouldPrettyPrint(EndpointsContext context) {
    HttpServletRequest request = context.getRequest();
    String prettyPrintStr = request.getParameter("prettyPrint");
    if (prettyPrintStr == null) {
      return context.isPrettyPrintEnabled();
    }
    return "true".equals(prettyPrintStr.toLowerCase());
  }
}
