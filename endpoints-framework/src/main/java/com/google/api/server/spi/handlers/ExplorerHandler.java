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
import com.google.api.server.spi.Strings;
import com.google.api.server.spi.dispatcher.DispatcherHandler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

/**
 * A handler which sends a redirect to the API Explorer.
 */
public class ExplorerHandler implements DispatcherHandler<EndpointsContext> {
  private static final String EXPLORER_URL = "http://apis-explorer.appspot.com/apis-explorer/";

  @Override
  public void handle(EndpointsContext context) throws IOException {
    context.getResponse().sendRedirect(getExplorerUrl(context.getRequest(), context.getPath()));
  }

  private String getExplorerUrl(HttpServletRequest req, String path) {
    String url = stripRedundantPorts(Strings.stripTrailingSlash(req.getRequestURL().toString()));
    // This will convert http://localhost:8080/_ah/api/explorer to
    // http://apis-explorer.appspot.com/apis-explorer/?base=http://localhost:8080/_ah/api&
    //   root=http://localhost:8080/_ah/api
    // The root parameter is necessary for the non-default module case and the case where the
    // host is manually specified. This will override the root, which API explorer now respects
    // by default.
    String apiRoot = url.substring(0, url.length() - path.length() - 1);
    return EXPLORER_URL + "?base=" + apiRoot + "&root=" + apiRoot;
  }

  private static String stripRedundantPorts(String url) {
    if (url == null) {
      return null;
    } else if (url.startsWith("http:") && url.contains(":80/")) {
      return url.replace(":80/", "/");
    } else if (url.startsWith("https:") && url.contains(":443/")) {
      return url.replace(":443/", "/");
    }
    return url;
  }
}
