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

import java.util.Map;
import java.util.logging.Level;

/**
 * Generic service exception that, in addition to a status message, has a status code, and
 * optionally, response headers to return.
 */
public class ServiceException extends Exception {

  protected final int statusCode;
  protected final String reason;
  protected final String domain;
  protected Level logLevel;

  public ServiceException(int statusCode, String statusMessage) {
    super(statusMessage);

    this.statusCode = statusCode;
    this.reason = null;
    this.domain = null;
  }

  public ServiceException(int statusCode, Throwable cause) {
    super(cause);

    this.statusCode = statusCode;
    this.reason = null;
    this.domain = null;
  }

  public ServiceException(int statusCode, String statusMessage, Throwable cause) {
    super(statusMessage, cause);

    this.statusCode = statusCode;
    this.reason = null;
    this.domain = null;
  }

  public ServiceException(int statusCode, String statusMessage, String reason) {
    super(statusMessage);

    this.statusCode = statusCode;
    this.reason = reason;
    this.domain = null;
  }
  
  public ServiceException(int statusCode, String statusMessage, String reason, Throwable cause) {
    super(statusMessage, cause);
    
    this.statusCode = statusCode;
    this.reason = reason;
    this.domain = null;
  }

  public ServiceException(int statusCode, String statusMessage, String reason, String domain) {
    super(statusMessage);

    this.statusCode = statusCode;
    this.reason = reason;
    this.domain = domain;
  }
  
  public ServiceException(int statusCode, String statusMessage, String reason, String domain, 
      Throwable cause) {
    super(statusMessage, cause);
    
    this.statusCode = statusCode;
    this.reason = reason;
    this.domain = domain;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getReason() {
    return reason;
  }

  public String getDomain() {
    return domain;
  }

  public Map<String, String> getHeaders() {
    return null;
  }

  public Level getLogLevel() {
    return logLevel == null ? getDefaultLoggingLevel(statusCode) : logLevel;
  }

  private static Level getDefaultLoggingLevel(int statusCode) {
    return statusCode >= 500 ? Level.SEVERE : Level.INFO;
  }

  public static <T extends ServiceException> T withLogLevel(T exception, Level level) {
    exception.logLevel = level;
    return exception;
  }
}
