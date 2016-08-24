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
package com.google.api.server.spi.handlers;

import com.google.api.server.spi.Headers;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedSet;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A permissible CORS implementation, which allows all domains and headers.
 */
public class CorsHandler {
  private static final Set<String> ALLOWED_METHODS = ImmutableSortedSet
      .of("HEAD", "DELETE", "GET", "PATCH", "POST", "PUT");
  public static final String ALLOWED_METHODS_STRING = Joiner.on(',').join(ALLOWED_METHODS);
  private static final String NULL_ORIGIN = "null";

  public void handle(HttpServletRequest request, HttpServletResponse response) {
    if (isValidMethod(request)) {
      allowOrigin(request, response);
      allowMethods(response);
      allowHeaders(request, response);
      setMaxAge(response);
      setAccessControlAllowCredentials(response);
    }
  }

  // This logic is also used to allow simple CORS requests.
  public static void allowOrigin(HttpServletRequest request, HttpServletResponse response) {
    String origin = request.getHeader(Headers.ORIGIN);
    // The Origin spec (http://tools.ietf.org/html/draft-abarth-origin-09) allows for the Origin
    // http header value to be "null". This is for cases where a request doesn't have a valid
    // origin; for example, issuing a CORS request from a local file:// rather than a website. In
    // these cases, we'd like to enable CORS to facilitate testing; the mechanism for doing so is
    // to set the Access-Control-Allow-Origin header to '*'.
    origin = NULL_ORIGIN.equals(origin) ? "*" : origin;
    response.setHeader(Headers.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
  }

  public static void setAccessControlAllowCredentials(HttpServletResponse response) {
    response.setHeader(Headers.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
  }

  private void setMaxAge(HttpServletResponse response) {
    response.setHeader(Headers.ACCESS_CONTROL_MAX_AGE, "3600");
  }

  private void allowHeaders(HttpServletRequest request, HttpServletResponse response) {
    response.setHeader(
        Headers.ACCESS_CONTROL_ALLOW_HEADERS,
        request.getHeader(Headers.ACCESS_CONTROL_REQUEST_HEADERS));
  }

  private void allowMethods(HttpServletResponse response) {
    response.setHeader(Headers.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS_STRING);
  }

  private boolean isValidMethod(HttpServletRequest request) {
    return ALLOWED_METHODS.contains(request.getHeader(Headers.ACCESS_CONTROL_REQUEST_METHOD));
  }
}
