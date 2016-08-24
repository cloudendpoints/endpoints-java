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

import java.util.Set;

/**
 * Extension of Guice servlet module which provides
 * {@link #serveGuiceSystemServiceServlet(String, Iterable)} as a
 * means to bind the {@link GuiceSystemServiceServlet} to the provided
 * URL pattern and the {@code serviceClasses}.
 * <p>
 * Your application's {@link ServletModule} should extend this one and
 * should call {@link #serveGuiceSystemServiceServlet(String, Iterable)} to
 * serve the proxied system services.
 *
 * @author Joe Catalano
 */
@Deprecated
public abstract class GuiceSystemServiceServletModule extends ServletModule {

  /**
   * Serves the {@link GuiceSystemServiceServlet} to the {@code urlPattern} for
   * the {@code serviceClasses} services.  Defaults for all init parameters are used.
   *
   * @param urlPattern the URL pattern to serve (i.e. /my/services/*)
   * @param serviceClasses the annotated service classes to serve
   */
  protected void serveGuiceSystemServiceServlet(
      String urlPattern, Iterable<? extends Class<?>> serviceClasses) {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .addServiceClasses(serviceClasses)
        .build();
    serveGuiceSystemServiceServlet(urlPattern, initParameters);
  }

  /**
   * Serves the {@link GuiceSystemServiceServlet} to the {@code urlPattern} for the
   * services specified in {@code initParameters}.  The specified initialization
   * parameters are used instead of the defaults.
   *
   * @param urlPattern the URL pattern to serve (i.e. /my/services/*)
   * @param initParameters the initialization parameters to pass to the servlet
   */
  protected void serveGuiceSystemServiceServlet(
      String urlPattern, final ServletInitializationParameters initParameters) {
    // Install system service module.
    install(new SystemServiceModule() {
      @Override
      protected Set<Class<?>> getServiceClasses() {
        return initParameters.getServiceClasses();
      }
    });

    super.serve(urlPattern).with(GuiceSystemServiceServlet.class, initParameters.asMap());
  }
}
