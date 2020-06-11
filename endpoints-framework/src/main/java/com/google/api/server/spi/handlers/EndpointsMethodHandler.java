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

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.EndpointsContext;
import com.google.api.server.spi.Headers;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.ServletInitializationParameters;
import com.google.api.server.spi.SystemService;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.config.model.StandardParameters;
import com.google.api.server.spi.dispatcher.DispatcherHandler;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.request.ParamReader;
import com.google.api.server.spi.request.RestServletRequestParamReader;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.RestResponseResultWriter;
import com.google.api.server.spi.response.ResultWriter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A wrapper class for an Endpoints method which provides helpers for doing JSON-REST and
 * (eventually) JSON-RPC dispatching.
 */
public class EndpointsMethodHandler {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final ServletInitializationParameters initParameters;
  private final ServletContext servletContext;
  private final EndpointMethod endpointMethod;
  private final ApiMethodConfig methodConfig;
  private final SystemService systemService;
  private final RestHandler restHandler;
  private final String restPath;

  public EndpointsMethodHandler(ServletInitializationParameters initParameters,
      ServletContext servletContext, EndpointMethod endpointMethod, ApiMethodConfig methodConfig,
      SystemService systemService) {
    this.initParameters = initParameters;
    this.servletContext = servletContext;
    this.endpointMethod = endpointMethod;
    this.methodConfig = methodConfig;
    this.systemService = systemService;
    this.restHandler = new RestHandler();
    this.restPath = createRestPath(methodConfig);
  }

  public String getRestMethod() {
    return methodConfig.getHttpMethod();
  }

  public String getRestPath() {
    return restPath;
  }

  public DispatcherHandler<EndpointsContext> getRestHandler() {
    return restHandler;
  }

  @VisibleForTesting
  protected ParamReader createRestParamReader(EndpointsContext context,
      ApiSerializationConfig serializationConfig) {
    return new RestServletRequestParamReader(endpointMethod, context,
        servletContext, serializationConfig, methodConfig);
  }

  /**
   * Override to customize the serialization of the response body
   *
   * @return a result writer
   * @throws ServiceException if the result writer customization fails
   */
  protected ResultWriter createResultWriter(EndpointsContext context,
      ApiSerializationConfig serializationConfig) throws ServiceException {
    return _createResultWriter(context, serializationConfig);
  }

  private void writeError(EndpointsContext context, ServiceException error) throws IOException {
      _createResultWriter(context, null).writeError(error);
  }

  private ResultWriter _createResultWriter(EndpointsContext context,
      ApiSerializationConfig serializationConfig) {
    return new RestResponseResultWriter(context.getResponse(), serializationConfig,
        StandardParameters.shouldPrettyPrint(context),
        initParameters.isAddContentLength(),
        initParameters.isExceptionCompatibilityEnabled());
  }

  private class RestHandler implements DispatcherHandler<EndpointsContext> {
    @Override
    public void handle(EndpointsContext context) throws IOException {
      try {
        HttpServletRequest request = context.getRequest();
        Attribute.bindStandardRequestAttributes(request, methodConfig, initParameters);
        String serviceName = endpointMethod.getEndpointClass().getName();
        Object service = systemService.findService(serviceName);
        ApiSerializationConfig serializationConfig = systemService.getSerializationConfig(
            serviceName);
        ParamReader reader = createRestParamReader(context, serializationConfig);
        ResultWriter writer = createResultWriter(context, serializationConfig);
        if (request.getHeader(Headers.ORIGIN) != null) {
          HttpServletResponse response = context.getResponse();
          CorsHandler.allowOrigin(request, response);
          CorsHandler.setAccessControlAllowCredentials(response);
        }
        systemService.invokeServiceMethod(service, endpointMethod.getMethod(), methodConfig.getEffectiveResponseStatus(), reader, writer);
      } catch (ServiceException e) {
        writeError(context, e);
      } catch (Exception e) {
        // All exceptions here are unexpected, including the ServiceException that may be thrown by
        // the findService call. We return an internal server error and leave the details in the
        // backend log.
        logger.atWarning().withCause(e).log("exception occurred while invoking backend method");
        writeError(context, new InternalServerErrorException("backend error"));
      }
    }
  }

  private static String createRestPath(ApiMethodConfig methodConfig) {
    // Don't include the api name or version if the path starts with a slash.
    if (methodConfig.getPath().startsWith("/")) {
      return methodConfig.getPath().substring(1);
    }
    ApiConfig apiConfig = methodConfig.getApiConfig();
    return String.format(
        "%s/%s/%s", apiConfig.getName(), apiConfig.getVersion(), methodConfig.getPath());
  }
}
