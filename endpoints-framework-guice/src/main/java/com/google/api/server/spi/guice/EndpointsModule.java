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
package com.google.api.server.spi.guice;

import com.google.api.server.spi.ServletInitializationParameters;
import com.google.inject.servlet.ServletModule;

/**
 * A base Guice module which provides helpers for configuring Endpoints. Consumers should extend
 * this class and call one of the helpers in {@link #configureServlets()}.
 */
public class EndpointsModule extends ServletModule {
  /**
   * Configure Endpoints given a list of service classes using {@link GuiceEndpointsServlet}.
   *
   * @param urlPattern the URL pattern to configure the servlet on. For the legacy servlet, use
   * "/_ah/spi/*". For the new servlet, use "/_ah/api/*" if backwards compatibility is desired, or
   * any other pattern if compatibility is not an issue.
   * @param serviceClasses the list of backend classes to be included
   */
  protected void configureEndpoints(
      String urlPattern, Iterable<? extends Class<?>> serviceClasses) {
    configureEndpoints(urlPattern, serviceClasses, false);
  }

  /**
   * Configure Endpoints given a list of service classes.
   *
   * @param urlPattern the URL pattern to configure the servlet on. For the legacy servlet, use
   * "/_ah/spi/*". For the new servlet, use "/_ah/api/*" if backwards compatibility is desired, or
   * any other pattern if compatibility is not an issue.
   * @param serviceClasses the list of backend classes to be included
   * @param useLegacyServlet whether or not to use the old style servlet
   */
  protected void configureEndpoints(
      String urlPattern, Iterable<? extends Class<?>> serviceClasses, boolean useLegacyServlet) {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .addServiceClasses(serviceClasses)
        .build();
    configureEndpoints(urlPattern, initParameters, useLegacyServlet);
  }

  /**
   * Configure Endpoints given {@link ServletInitializationParameters} using
   * {@link GuiceEndpointsServlet}.
   *
   * @param urlPattern the URL pattern to configure the servlet on. For the legacy servlet, use
   * "/_ah/spi/*". For the new servlet, use "/_ah/api/*" if backwards compatibility is desired, or
   * any other pattern if compatibility is not an issue
   * @param initParameters the initialization parameters. Must include service classes to be useful
   */
  protected void configureEndpoints(
      String urlPattern, ServletInitializationParameters initParameters) {
    configureEndpoints(urlPattern, initParameters, false);
  }

  /**
   * Configure Endpoints given {@link ServletInitializationParameters}.
   *
   * @param urlPattern the URL pattern to configure the servlet on. For the legacy servlet, use
   * "/_ah/spi/*". For the new servlet, use "/_ah/api/*" if backwards compatibility is desired, or
   * any other pattern if compatibility is not an issue
   * @param initParameters the initialization parameters. Must include service classes to be useful
   * @param useLegacyServlet whether or not to use the old style servlet
   */
  protected void configureEndpoints(
      String urlPattern, ServletInitializationParameters initParameters, boolean useLegacyServlet) {
    bind(ServiceMap.class)
        .toInstance(ServiceMap.create(binder(), initParameters.getServiceClasses()));
    if (useLegacyServlet) {
      super.serve(urlPattern).with(GuiceSystemServiceServlet.class, initParameters.asMap());
    } else {
      super.serve(urlPattern).with(GuiceEndpointsServlet.class, initParameters.asMap());
    }
  }
}
