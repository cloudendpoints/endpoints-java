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

/**
 * Service Unavailable response for the API, mapped to a HTTP 503 response.
 */
public class ServiceUnavailableException extends ServiceException {

  private static final int CODE = 503;

  public ServiceUnavailableException(String message) {
    super(CODE, message);
  }

  public ServiceUnavailableException(Throwable cause) {
    super(CODE, cause);
  }

  public ServiceUnavailableException(String message, Throwable cause) {
    super(CODE, message, cause);
  }

  public ServiceUnavailableException(String statusMessage, String reason) {
    super(CODE, statusMessage, reason);
  }

  public ServiceUnavailableException(String statusMessage, String reason, Throwable cause) {
    super(CODE, statusMessage, reason, cause);
  }

  public ServiceUnavailableException(String statusMessage, String reason, String domain) {
    super(CODE, statusMessage, reason, domain);
  }
  
  public ServiceUnavailableException(String statusMessage, String reason, String domain, 
      Throwable cause) {
    super(CODE, statusMessage, reason, domain, cause);
  }
}
