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
package com.google.api.server.spi.response;

import com.google.api.server.spi.ServiceException;
import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unauthorized exception that is mapped to a 401 response.
 */
public class UnauthorizedException extends ServiceException {

  public static final String AUTH_SCHEME_BEARER = "Bearer";
  private static final Map<String, String> GOOGLE_REALM =
      ImmutableMap.of("realm", "\"https://accounts.google.com/\"");
  private static final int CODE = 401;

  private final String authScheme;
  private final Map<String, String> params;

  public UnauthorizedException(String message) {
    this(message, AUTH_SCHEME_BEARER, GOOGLE_REALM);
  }

  public UnauthorizedException(Throwable cause) {
    super(CODE, cause);

    this.authScheme = AUTH_SCHEME_BEARER;
    this.params = GOOGLE_REALM;
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(CODE, message, cause);

    this.authScheme = AUTH_SCHEME_BEARER;
    this.params = null;
  }

  public UnauthorizedException(String statusMessage, String reason) {
    super(CODE, statusMessage, reason);

    this.authScheme = AUTH_SCHEME_BEARER;
    this.params = null;
  }

  public UnauthorizedException(String statusMessage, String reason, Throwable cause) {
    super(CODE, statusMessage, reason, cause);

    this.authScheme = AUTH_SCHEME_BEARER;
    this.params = null;
  }

  public UnauthorizedException(String statusMessage, String reason, String domain) {
    super(CODE, statusMessage, reason, domain);

    this.authScheme = AUTH_SCHEME_BEARER;
    this.params = null;
  }

  public UnauthorizedException(String statusMessage, String reason, String domain, 
      Throwable cause) {
    super(CODE, statusMessage, reason, domain, cause);

    this.authScheme = AUTH_SCHEME_BEARER;
    this.params = null;
  }

  /**
   * Creates an UnauthorizedException.
   *
   * @param message Error message to return
   * @param authScheme Auth scheme in the WWW-Authenticate header
   * @param params Parameters in the WWW-Authenticate header
   */
  public UnauthorizedException(String message, String authScheme, Map<String, String> params) {
    super(CODE, message);

    this.authScheme = authScheme;
    this.params = params;
  }

  public UnauthorizedException(String message, String reason, String domain, String authScheme, 
      Map<String, String> params) {
    super(CODE, message, reason, domain, null);

    this.authScheme = authScheme;
    this.params = params;
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> headers = new LinkedHashMap<String, String>();
    StringBuilder value = new StringBuilder(authScheme);
    if (params != null) {
      boolean first = true;
      for (Map.Entry<String, String> entry : params.entrySet()) {
        if (first) {
          first = false;
        } else {
          value.append(',');
        }
        value.append(' ');
        value.append(entry.getKey());
        value.append('=');
        value.append(entry.getValue());
      }
    }
    headers.put("WWW-Authenticate", value.toString());
    return headers;
  }
}
