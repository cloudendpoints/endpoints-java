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

import java.io.IOException;

/**
 * Writes a result or error.
 */
public interface ResultWriter {

  /**
   * Writes a result JSON object.
   * @throws IOException
   */
  void write(Object result, int status) throws IOException;

  /**
   * Writes an error response with the HTTP status code and JSON body of {"message":
   * "&lt;exception's message&gt;"}.
   */
  void writeError(ServiceException e) throws IOException;
}
