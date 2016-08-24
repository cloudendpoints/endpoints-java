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

import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.request.ParamReader;
import com.google.api.server.spi.request.ServletRequestParamReader;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.ResultWriter;
import com.google.api.server.spi.response.ServletResponseResultWriter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * SPI servlet that turns a request on an SPI endpoint into an invocation of a method on the
 * {@link SystemService}.
 *
 *         POST /_ah/spi/{Service}.{method}
 */
public class SystemServiceServlet extends HttpServlet {
  private static final Pattern PATH_PATTERN = Pattern.compile("/([^/]+)\\.([^/]+)");
  private static final Logger logger = Logger.getLogger(SystemServiceServlet.class.getName());
  private volatile SystemService systemService;
  private volatile ServletInitializationParameters initParameters;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    ClassLoader classLoader = getUserClassLoader(config);
    this.initParameters = ServletInitializationParameters.fromServletConfig(config, classLoader);
    logger.log(Level.INFO, "SPI restricted: {0}", this.initParameters.isServletRestricted());
    this.systemService = createSystemService(classLoader, this.initParameters);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String path = request.getPathInfo();
    String[] pathParams = getPathParams(path);
    if (pathParams != null) {
      try {
        String serviceName = pathParams[0];
        String methodName = pathParams[1];
        execute(request, response, serviceName, methodName);
      } catch (ServiceException e) {
        getErrorResponseWriter(response).writeError(e);
      }
    } else {
      getErrorResponseWriter(response).writeError(
          new BadRequestException("missing /{Service}.{method}"));
    }
  }

  private ParamReader getParamReader(EndpointMethod method,
      ApiSerializationConfig serializationConfig, HttpServletRequest request) {
    return new ServletRequestParamReader(method, request, getServletContext(), serializationConfig);
  }

  private ResultWriter getResponseWriter(ApiSerializationConfig serializationConfig,
      HttpServletResponse response) {
    return new ServletResponseResultWriter(response, serializationConfig);
  }

  private ResultWriter getErrorResponseWriter(HttpServletResponse response) {
    return getResponseWriter(null, response);
  }

  protected void execute(HttpServletRequest request, HttpServletResponse response,
      String serviceName, String methodName) throws IOException, ServiceException {
    logger.log(Level.FINE, "serviceName={0} methodName={1}",
        new Object[] {serviceName, methodName});

    EndpointMethod serviceMethod = systemService.resolveService(serviceName, methodName);
    ApiSerializationConfig serializationConfig = systemService.getSerializationConfig(serviceName);
    ResultWriter responseWriter = getResponseWriter(serializationConfig, response);
    logger.log(Level.FINE, "serviceMethod={0}", serviceMethod);
    ApiMethodConfig methodConfig =
        systemService.resolveAndUpdateServiceConfig(serviceName, methodName);
    Attribute.bindStandardRequestAttributes(request, methodConfig, initParameters);
    if (!PeerAuth.from(request).authorizePeer()) {
      logger.info("SPI restricted and request denied");
      getErrorResponseWriter(response).writeError(new NotFoundException("Not found"));
      return;
    }
    ParamReader requestReader = getParamReader(serviceMethod, serializationConfig, request);
    systemService.invokeServiceMethod(systemService.findService(serviceName),
        serviceMethod.getMethod(), requestReader, responseWriter);
  }

  private String[] getPathParams(String path) {
    Matcher matcher = PATH_PATTERN.matcher(path);
    if (matcher.matches()) {
      if (matcher.groupCount() == 2) {
        return new String[] {matcher.group(1), matcher.group(2)};
      }
    }
    return null;
  }

  private SystemService createSystemService(ClassLoader classLoader,
      ServletInitializationParameters initParameters) throws ServletException {
    try {
      SystemService.Builder builder = SystemService.builder()
          .withDefaults(classLoader)
          .setIllegalArgumentIsBackendError(initParameters.isIllegalArgumentBackendError());
      for (Class<?> serviceClass : initParameters.getServiceClasses()) {
        builder.addService(serviceClass, createService(serviceClass));
      }
      return builder.build();
    } catch (ApiConfigException | ClassNotFoundException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Creates a new instance of the specified service class.
   *
   * @param serviceClass the class of the service to create
   */
  protected <T> T createService(Class<T> serviceClass) {
    try {
      return serviceClass.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException(
          String.format("Cannot instantiate service class: %s", serviceClass.getName()), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(
          String.format("Cannot access service class: %s", serviceClass.getName()), e);
    }
  }

  private ClassLoader getUserClassLoader(ServletConfig config) throws ServletException {
    try {
      // system space
      Class<?> contextClass = Class.forName(
          "com.google.apphosting.utils.jetty.AppEngineWebAppContext$AppEngineServletContext");
      Method method = contextClass.getMethod("getClassLoader");
      return (ClassLoader) method.invoke(config.getServletContext());
    } catch (ClassNotFoundException e) {
      // user space
      return getClass().getClassLoader();
    } catch (NoSuchMethodException e) {
      throw new ServletException(e);
    } catch (IllegalAccessException e) {
      throw new ServletException(e);
    } catch (InvocationTargetException e) {
      throw new ServletException(e);
    }
  }
}
