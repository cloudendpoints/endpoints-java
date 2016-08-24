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
package com.google.api.server.spi.request;

import com.google.api.server.spi.ServletInitializationParameters;
import com.google.api.server.spi.Strings;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.common.annotations.VisibleForTesting;

import javax.servlet.http.HttpServletRequest;

/**
 * Defines attribute constants passed in Request.
 */
public class Attribute {
  public static final String AUTHENTICATED_APPENGINE_USER =
      "endpoints:Authenticated-AppEngine-User";
  public static final String API_METHOD_CONFIG = "endpoints:Api-Method-Config";
  public static final String ENABLE_CLIENT_ID_WHITELIST =
      "endpoints:Enable-Client-Id-Whitelist";
  public static final String RESTRICT_SERVLET = "endpoints:Restrict-Servlet";
  public static final String REQUIRE_APPENGINE_USER = "endpoints:Require-AppEngine-User";
  public static final String SKIP_TOKEN_AUTH = "endpoints:Skip-Token-Auth";
  public static final String AUTH_TOKEN = "endpoints:Auth-Token";

  private final HttpServletRequest request;

  @VisibleForTesting
  Attribute(HttpServletRequest request) {
    this.request = request;
  }

  public static Attribute from(HttpServletRequest request) {
    return new Attribute(request);
  }

  public Object get(String attr) {
    return request.getAttribute(attr);
  }

  public void set(String attr, Object value) {
    request.setAttribute(attr, value);
  }

  public void remove(String attr) {
    request.removeAttribute(attr);
  }

  public boolean isEnabled(String attr) {
    return request.getAttribute(attr) == null ? false : (Boolean) request.getAttribute(attr);
  }

  public static Attribute bindStandardRequestAttributes(HttpServletRequest request,
      ApiMethodConfig methodConfig,
      ServletInitializationParameters initParameters) {
    Attribute attr = Attribute.from(request);
    attr.set(Attribute.RESTRICT_SERVLET, initParameters.isServletRestricted());
    attr.set(Attribute.ENABLE_CLIENT_ID_WHITELIST, initParameters.isClientIdWhitelistEnabled());
    attr.set(Attribute.API_METHOD_CONFIG, methodConfig);
    // No clientId is allowed. Producer is not interested in Jwt/OAuth2 authentication.
    if (initParameters.isClientIdWhitelistEnabled()
        && Strings.isEmptyOrNull(methodConfig.getClientIds())) {
      attr.set(Attribute.SKIP_TOKEN_AUTH, true);
    }
    return attr;
  }
}
