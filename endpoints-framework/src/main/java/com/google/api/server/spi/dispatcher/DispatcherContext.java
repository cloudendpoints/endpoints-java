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
package com.google.api.server.spi.dispatcher;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Base context for {@link PathDispatcher}, which contains string values of path parameters.
 */
public class DispatcherContext {
  private final String httpMethod;
  private final String path;
  private ImmutableMap<String, String> rawPathParameters = ImmutableMap.of();

  public DispatcherContext(String httpMethod, String path) {
    this.httpMethod = Preconditions.checkNotNull(httpMethod, "httpMethod").toUpperCase();
    this.path = Preconditions.checkNotNull(path);
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public String getPath() {
    return path;
  }

  /**
   * Gets the URL-decoded string values of the path parameters that were specified in the current
   * request.
   */
  public ImmutableMap<String, String> getRawPathParameters() {
    return rawPathParameters;
  }

  public void setRawPathParameters(Map<String, String> rawPathParameters) {
    this.rawPathParameters = ImmutableMap.copyOf(rawPathParameters);
  }
}
