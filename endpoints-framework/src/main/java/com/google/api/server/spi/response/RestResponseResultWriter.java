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
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * A {@link ResultWriter} that writes JSON-REST errors, for use with {@link
 * com.google.api.server.spi.EndpointsServlet}.
 */
public class RestResponseResultWriter extends ServletResponseResultWriter {
  private final boolean enableExceptionCompatibility;

  public RestResponseResultWriter(
      HttpServletResponse servletResponse, ApiSerializationConfig serializationConfig,
      boolean prettyPrint, boolean enableExceptionCompatibility) {
    super(servletResponse, serializationConfig, prettyPrint);
    this.enableExceptionCompatibility = enableExceptionCompatibility;
  }

  /**
   * Writes an error in the expected form for JSON-REST:
   *
   * {
   *   "error": {
   *     "errors: [
   *       {
   *         "domain: "global",
   *         "reason": "backendError",
   *         "message: "..."
   *       }
   *     ],
   *     "code": 503,
   *     "message": "..."
   *   }
   * }
   */
  @Override
  public void writeError(ServiceException e) throws IOException {
    ErrorMap errorMap = new ErrorMap(enableExceptionCompatibility);
    int code = errorMap.getHttpStatus(e.getStatusCode());
    String reason = !Strings.isNullOrEmpty(e.getReason()) ?
        e.getReason() : errorMap.getReason(e.getStatusCode());
    String domain = !Strings.isNullOrEmpty(e.getDomain()) ?
        e.getDomain() : errorMap.getDomain(e.getStatusCode());
    write(code, e.getHeaders(),
        writeValueAsString(createError(code, reason, domain, e.getMessage())));
  }

  private Object createError(int code, String reason, String domain, String message) {
    return ImmutableMap.of(
        "error", ImmutableMap.of(
            "errors", ImmutableList.of(ImmutableMap.of(
                "domain", domain,
                "reason", reason,
                "message", message
            )),
            "code", code,
            "message", message
        ));
  }
}
