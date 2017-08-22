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

import com.google.api.server.spi.EndpointsContext;
import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.dispatcher.DispatcherHandler;

import java.io.IOException;

/**
 * Serves the API proxy source with the correct API path.
 */
public class ApiProxyHandler implements DispatcherHandler<EndpointsContext> {
  private volatile String cachedProxyHtml;

  @Override
  public void handle(EndpointsContext context) throws IOException {
    if (cachedProxyHtml == null) {
      cachedProxyHtml = IoUtil.readResourceFile(ApiProxyHandler.class, "proxy.html");
    }
    context.getResponse().setContentType("text/html");
    // This is a nonstandard value, but it seems sometimes X-Frame-Options can be injected by
    // a proxy. We set this explicitly in hopes that the proxy won't override a set value.
    context.getResponse().addHeader("X-Frame-Options", "ALLOWALL");
    context.getResponse().getWriter().write(cachedProxyHtml);
  }
}
