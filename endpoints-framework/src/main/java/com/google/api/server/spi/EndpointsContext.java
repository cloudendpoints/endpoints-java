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

import com.google.api.server.spi.dispatcher.DispatcherContext;
import com.google.common.base.Preconditions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Context for dispatching Endpoints methods.
 */
public class EndpointsContext extends DispatcherContext {
  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final boolean prettyPrint;

  public EndpointsContext(String httpMethod, String path, HttpServletRequest request,
      HttpServletResponse response, boolean prettyPrint) {
    super(httpMethod, path);
    this.request = Preconditions.checkNotNull(request, "request");
    this.response = Preconditions.checkNotNull(response, "response");
    this.prettyPrint = prettyPrint;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  public HttpServletResponse getResponse() {
    return response;
  }

  public boolean isPrettyPrintEnabled() {
    return prettyPrint;
  }
}
