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
import com.google.api.server.spi.RequestUtil;
import com.google.api.server.spi.Strings;
import com.google.api.server.spi.dispatcher.DispatcherHandler;

import java.io.IOException;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

/**
 * A handler which sends a redirect to the API Explorer.
 */
public class ExplorerHandler implements DispatcherHandler<EndpointsContext> {
  
  private static final String DEFAULT_TEMPLATE 
      = "https://apis-explorer.appspot.com/apis-explorer/?base=${apiBase}";
  
  private final String urlTemplate;

  public ExplorerHandler(String urlTemplate) {
    this.urlTemplate = Optional.ofNullable(urlTemplate).orElse(DEFAULT_TEMPLATE);
  }

  @Override
  public void handle(EndpointsContext context) throws IOException {
    context.getResponse()
        .sendRedirect(getExplorerUrl(context.getRequest(), context.getPath()));
  }

  private String getExplorerUrl(HttpServletRequest req, String path) {
    String requestUrl = RequestUtil.getOriginalRequestUrl(req);
    requestUrl = Strings.stripTrailingSlash(requestUrl);
    // This will convert http://localhost:8080/_ah/api/explorer to
    // ${EXPLORER_URL}?base=http://localhost:8080/_ah/api
    String apiBase = requestUrl.substring(0, requestUrl.length() - path.length() - 1);
    return urlTemplate.replace("${apiBase}", apiBase);
  }
  
}
