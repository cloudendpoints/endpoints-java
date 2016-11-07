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
package com.google.api.server.spi.discovery;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.RestDescription;
import com.google.api.services.discovery.model.RpcDescription;
import com.google.common.annotations.VisibleForTesting;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * A discovery API which acts as a proxy (of sorts) to the Endpoints v1.0 discovery service. Uses
 * the existing service to generate documents and caches them to make subsequent requests faster.
 * This will eventually be replaced by full discovery emulation that is independent of the service.
 */
@Api(
    name = "discovery",
    version = "v1",
    title = "APIs Discovery Service",
    description = "Lets you discover information about other Google APIs, such as what APIs are "
        + "available, the resource and method details for each API"
)
public class ProxyingDiscoveryService {
  private static final Logger logger = Logger.getLogger(ProxyingDiscoveryService.class.getName());

  private DiscoveryProvider discoveryProvider;
  private boolean initialized = false;

  @ApiMethod(ignored = AnnotationBoolean.TRUE)
  public synchronized void initialize(DiscoveryProvider discoveryProvider) {
    if (!initialized) {
      this.discoveryProvider = discoveryProvider;
      initialized = true;
    }
  }

  @ApiMethod(
      name = "apis.getRest",
      path = "apis/{api}/{version}/rest"
  )
  public RestDescription getRestDocument(HttpServletRequest request, @Named("api") String name,
      @Named("version") String version) throws NotFoundException, InternalServerErrorException {
    checkIsInitialized();
    return discoveryProvider.getRestDocument(getActualRoot(request), name, version);
  }

  @ApiMethod(
      name = "apis.getRpc",
      path = "apis/{api}/{version}/rpc"
  )
  public RpcDescription getRpcDocument(HttpServletRequest request, @Named("api") String name,
      @Named("version") String version) throws NotFoundException, InternalServerErrorException {
    checkIsInitialized();
    return discoveryProvider.getRpcDocument(getActualRoot(request), name, version);
  }

  @ApiMethod(
      name = "apis.list",
      path = "apis"
  )
  public DirectoryList getApiList(HttpServletRequest request) throws InternalServerErrorException {
    checkIsInitialized();
    return discoveryProvider.getDirectory(getActualRoot(request));
  }

  private void checkIsInitialized() throws InternalServerErrorException {
    if (!initialized) {
      logger.warning("Tried to call discovery before initialization!");
      throw new InternalServerErrorException("Internal Server Error");
    }
  }

  @VisibleForTesting
  static String getActualRoot(HttpServletRequest request)
      throws InternalServerErrorException {
    String uri = request.getRequestURI();
    int index = uri.indexOf("discovery/v1/apis");
    if (index == -1) {
      logger.severe("Could not compute discovery root from url: " + request.getRequestURI());
      throw new InternalServerErrorException("Internal Server Error");
    }
    StringBuffer url = request.getRequestURL();
    return url.substring(0, url.length() - (uri.length() - index));
  }
}
