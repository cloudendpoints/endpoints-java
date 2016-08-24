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

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Java SPI errors to errors to error details seen in production.
 */
public class ErrorMap {
  private static Map<Integer, Error> errors = createErrorMap();

  private final boolean enableExceptionCompatibility;

  public ErrorMap() {
    this(true);
  }

  public ErrorMap(boolean enableExceptionCompatibility) {
    this.enableExceptionCompatibility = enableExceptionCompatibility;
  }

  private static class Error {
    private final int httpStatus;
    private final int rpcStatus;
    private final String compatibilityReason;
    private final String reason;
    private final String domain;

    Error(int httpStatus, int rpcStatus, String reason, String domain) {
      this(httpStatus, rpcStatus, reason, reason, domain);
    }

    Error(int httpStatus, int rpcStatus, String compatibilityReason, String reason, String domain) {
      this.httpStatus = httpStatus;
      this.rpcStatus = rpcStatus;
      this.compatibilityReason = compatibilityReason;
      this.reason = reason;
      this.domain = domain;
    }
  }

  public int getHttpStatus(int lilyStatus) {
    if (!enableExceptionCompatibility) {
      return lilyStatus;
    }
    if (lilyStatus >= 500) {
      return 503;
    }
    Error error = errors.get(lilyStatus);

    if (error == null) {
      return 404;
    }
    return error.httpStatus;
  }

  public int getRpcStatus(int lilyStatus) {
    if (lilyStatus >= 500) {
      return -32099;
    }
    Error error = errors.get(lilyStatus);

    if (error == null) {
      return 404;
    }
    return error.rpcStatus;
  }

  public String getReason(int lilyStatus) {
    if (lilyStatus >= 500) {
      return "backendError";
    }
    Error error = errors.get(lilyStatus);

    if (error == null) {
      return "unsupportedProtocol";
    }
    return enableExceptionCompatibility ? error.compatibilityReason : error.reason;
  }

  public String getDomain(int lilyStatus) {
    if (lilyStatus >= 500) {
      return "global";
    }
    Error error = errors.get(lilyStatus);

    if (error == null) {
      return "global";
    }
    return error.domain;
  }

  private static Map<Integer, Error> createErrorMap() {
    Map<Integer, Error> errors = new HashMap<>();

    errors.put(400, new Error(400, 400, "badRequest", "global"));
    errors.put(401, new Error(401, 401, "required", "global"));
    errors.put(402, new Error(404, 404, "unsupportedProtocol", "paymentRequired", "global"));
    errors.put(403, new Error(403, 403, "forbidden", "global"));
    errors.put(404, new Error(404, 404, "notFound", "global"));
    errors.put(405, new Error(501, 501, "unsupportedMethod", "global"));
    errors.put(406, new Error(404, 404, "unsupportedProtocol", "notAcceptable", "global"));
    errors.put(407, new Error(404, 404, "unsupportedProtocol", "proxyAuthentication", "global"));
    errors.put(408, new Error(503, -32099, "backendError", "requestTimeout", "global"));
    errors.put(409, new Error(409, 409, "conflict", "global"));
    errors.put(410, new Error(410, 410, "deleted", "global"));
    errors.put(411, new Error(404, 404, "unsupportedProtocol", "lengthRequired", "global"));
    errors.put(412, new Error(412, 412, "conditionNotMet", "global"));
    errors.put(413, new Error(413, 413, "uploadTooLarge", "global"));
    errors.put(414, new Error(404, 404, "unsupportedProtocol", "uriTooLong", "global"));
    errors.put(415, new Error(404, 404, "unsupportedProtocol", "unsupportedMediaType", "global"));
    errors.put(416, new Error(404, 404, "unsupportedProtocol", "rangeNotSatisfiable", "global"));
    errors.put(417, new Error(404, 404, "unsupportedProtocol", "expectationFailed", "global"));
    return errors;
  }
}
