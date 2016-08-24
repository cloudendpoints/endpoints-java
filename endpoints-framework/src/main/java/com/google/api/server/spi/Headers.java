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
package com.google.api.server.spi;

/**
 * Common locations for HTTP header constants.
 */
public class Headers {
  /**
   * The origin of a request, e.g. http://example.com.
   */
  public static final String ORIGIN = "Origin";

  /**
   * The HTTP method for which an OPTIONS request is asking for permission to send.
   */
  public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

  /**
   * A comma separated list of header names for which an OPTIONS request is asking for permissions
   * to send.
   */
  public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

  /**
   * An origin in the same format as {@link #ORIGIN}, which is permitted to send a request for a
   * resource, specified in response to an OPTIONS request.
   */
  public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  /**
   * Whether or not the request after the OPTIONS request is planning to send credentials (e.g.
   * cookies) with the request.
   */
  public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

  /**
   * How long the results of an OPTIONS request can be cached, specified in response to an
   * OPTIONS request.
   */
  public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

  /**
   * A comma-separated list of HTTP methods which are permitted, specified in response to an
   * OPTIONS request.
   */
  public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

  /**
   * A comma-separated list of header names which are permitted, specified in response to an
   * OPTIONS request.
   */
  public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
}
