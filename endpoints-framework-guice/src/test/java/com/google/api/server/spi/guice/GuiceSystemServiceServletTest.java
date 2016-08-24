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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.testing.Foo;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * Tests for {@link GuiceSystemServiceServlet}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GuiceSystemServiceServletTest {
  @Mock protected ServletConfig servletConfig;
  @Mock protected ServletContext servletContext;
  @Mock protected ApiMethodConfig methodConfig;

  public static final String URL_PATTERN = "/api/service/*";

  @Test
  public void testCanCreateInjectedService() throws Exception {
    Injector injector = Guice.createInjector(new TestServletModule(new InterceptorModule()));
    GuiceSystemServiceServlet servlet = injector.getInstance(GuiceSystemServiceServlet.class);
    when(servletConfig.getInitParameter("restricted")).thenReturn("false");
    when(servletConfig.getInitParameter("services")).thenReturn(InjectedEndpoint.class.getName());
    when(servletConfig.getInitParameter("clientIdWhitelistEnabled")).thenReturn(null);
    when(servletConfig.getServletContext()).thenReturn(servletContext);
    servlet.init(servletConfig);
    assertEquals("testing", servlet.createService(InjectedEndpoint.class).getValue().getName());
  }

  @Api
  private static class InjectedEndpoint {
    @Inject
    Foo foo;

    @SuppressWarnings("unused")
    public Foo getValue() {
      return foo;
    }
  }

  /**
   * Test servlet module that binds the services as expected.
   */
  private static class TestServletModule extends ServletModule {

    private final Module module;

    public TestServletModule(Module module) {
      this.module = module;
    }

    @Override
    protected void configureServlets() {
      bind(Foo.class).toInstance(new Foo() {
        @Override
        public String getName() {
          return "testing";
        }
      });
      install(module);
      install(new SystemServiceModule() { // Bind services.
        @Override
        protected Set<Class<?>> getServiceClasses() {
          return ImmutableSet.<Class<?>>of(InjectedEndpoint.class);
        }
      });
      serve(URL_PATTERN).with(GuiceSystemServiceServlet.class);
    }
  }
}
