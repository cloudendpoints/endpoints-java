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
 * Internal server exception that is mapped to a 500 response.
 */
public class InternalServerErrorException extends ServiceException {

  private static final int CODE = 500;

  public InternalServerErrorException(String message) {
    super(CODE, message);
  }

  public InternalServerErrorException(Throwable cause) {
    super(CODE, cause);
  }

  public InternalServerErrorException(String message, Throwable cause) {
    super(CODE, message, cause);
  }

  public InternalServerErrorException(String statusMessage, String reason) {
    super(CODE, statusMessage, reason);
  }
  
  public InternalServerErrorException(String statusMessage, String reason, Throwable cause) {
    super(CODE, statusMessage, reason, cause);
  }

  public InternalServerErrorException(String statusMessage, String reason, String domain) {
    super(CODE, statusMessage, reason, domain);
  }
  
  public InternalServerErrorException(String statusMessage, String reason, String domain, 
      Throwable cause) {
    super(CODE, statusMessage, reason, domain, cause);
  }
}
