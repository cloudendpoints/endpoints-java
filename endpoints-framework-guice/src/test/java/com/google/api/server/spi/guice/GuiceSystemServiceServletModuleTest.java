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
import static org.junit.Assert.assertNotNull;

import com.google.api.server.spi.ServletInitializationParameters;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.servlet.InstanceFilterBinding;
import com.google.inject.servlet.InstanceServletBinding;
import com.google.inject.servlet.LinkedFilterBinding;
import com.google.inject.servlet.LinkedServletBinding;
import com.google.inject.servlet.ServletModuleTargetVisitor;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.Elements;
import com.google.inject.spi.ProviderInstanceBinding;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link GuiceSystemServiceServletModule}.
 */
@RunWith(JUnit4.class)
public class GuiceSystemServiceServletModuleTest {
  private static final String URL_PATTERN = "/my/path/";
  private static final Set<? extends Class<?>> SERVICES = ImmutableSet.of(TestEndpoint.class);
  private static final ServletInitializationParameters INIT_PARAMETERS =
      ServletInitializationParameters.builder()
      .addServiceClasses(SERVICES)
      .setRestricted(false)
      .build();
  private GuiceSystemServiceServletModule module;

  @Before
  public void setUp() throws Exception {
    module = new GuiceSystemServiceServletModule() {
      @Override
      protected void configureServlets() {
        super.configureServlets();
        serveGuiceSystemServiceServlet(URL_PATTERN, INIT_PARAMETERS);
      }
    };
    Elements.getElements(module);
  }

  /**
   * Test method for
   * {@link GuiceSystemServiceServletModule#serveGuiceSystemServiceServlet(String, ServletInitializationParameters)}.
   */
  @Test
  public void testServeGuiceSystemServiceServlet_withInterceptor() {
    Injector injector = Guice.createInjector(module, new InterceptorModule());

    Visitor visitor = new Visitor();
    for (Binding<?> binding : injector.getAllBindings().values()) {
      binding.acceptTargetVisitor(visitor);
    }

    assertEquals("Servlet not bound.", 1, visitor.linkedServlets.size());
    LinkedServletBinding servletBinding = visitor.linkedServlets.get(0);
    assertEquals("URL pattern does not match", URL_PATTERN, servletBinding.getPattern());
    assertEquals("Wrong initialization parameter provided", "false",
        servletBinding.getInitParams().get("restricted"));
    assertNotNull("SystemService named provider not found.", visitor.systemServiceProvider);

    ServiceMap serviceMap = (ServiceMap) visitor.systemServiceProvider.getProvider().get();
    Collection<Object> services = serviceMap.getServices();
    assertEquals("Incorrect number of services provided", 1, services.size());
    assertEquals("Service not enhanced correctly.", SERVICES.toArray()[0],
        ((Class<?>) services.toArray()[0].getClass()).getSuperclass());
  }

  /**
   * Test method for
   * {@link GuiceSystemServiceServletModule#serveGuiceSystemServiceServlet(String, ServletInitializationParameters)}.
   */
  @Test
  public void testServeGuiceSystemServiceServlet_withoutInterceptor() {
    Injector injector = Guice.createInjector(module, new DummyModule());

    Visitor visitor = new Visitor();
    for (Binding<?> binding : injector.getAllBindings().values()) {
      binding.acceptTargetVisitor(visitor);
    }

    assertEquals("Servlet not bound.", 1, visitor.linkedServlets.size());
    LinkedServletBinding servletBinding = visitor.linkedServlets.get(0);
    assertEquals("URL pattern does not match", URL_PATTERN, servletBinding.getPattern());
    assertEquals("Wrong initialization parameter provided", "false",
        servletBinding.getInitParams().get("restricted"));
    assertNotNull("SystemService named provider not found.", visitor.systemServiceProvider);

    ServiceMap serviceMap = (ServiceMap) visitor.systemServiceProvider.getProvider().get();
    Collection<Object> services = serviceMap.getServices();
    assertEquals("Incorrect number of services provided", 1, services.size());
    assertEquals("Service not provided correctly.", SERVICES.toArray()[0],
        services.toArray()[0].getClass());
  }

  /**
   * Test method for
   * {@link GuiceSystemServiceServletModule#serveGuiceSystemServiceServlet(String, Iterable)}.
   */
  @Test
  public void testServeGuiceSystemServiceServlet_defaultInitParameters() {
    module = new GuiceSystemServiceServletModule() {
      @Override
      protected void configureServlets() {
        super.configureServlets();
        serveGuiceSystemServiceServlet(URL_PATTERN, SERVICES);
      }
    };
    Injector injector = Guice.createInjector(module, new DummyModule());

    Visitor visitor = new Visitor();
    for (Binding<?> binding : injector.getAllBindings().values()) {
      binding.acceptTargetVisitor(visitor);
    }

    assertEquals("Servlet not bound.", 1, visitor.linkedServlets.size());
    LinkedServletBinding servletBinding = visitor.linkedServlets.get(0);
    assertEquals("URL pattern does not match", URL_PATTERN, servletBinding.getPattern());
    assertEquals("Wrong initialization parameter provided", "true",
        servletBinding.getInitParams().get("restricted"));
    assertNotNull("SystemService named provider not found.", visitor.systemServiceProvider);

    ServiceMap serviceMap = (ServiceMap) visitor.systemServiceProvider.getProvider().get();
    Collection<Object> services = serviceMap.getServices();
    assertEquals("Incorrect number of services provided", 1, services.size());
    assertEquals("Service not provided correctly.", SERVICES.toArray()[0],
        services.toArray()[0].getClass());
  }

  /**
   * Tests that services are injected.
   */
  @Test
  public void testInjected_withoutInterceptors() {
    Injector injector = Guice.createInjector(module, new DummyModule());
    MockServlet mockServlet = new MockServlet();
    injector.injectMembers(mockServlet);
    assertNotNull(mockServlet.services);
    assertEquals(SERVICES.size(), mockServlet.services.getServices().size());
  }

  /**
   * Tests that services are injected.
   */
  @Test
  public void testInjected_withInterceptors() {
    Injector injector = Guice.createInjector(module, new InterceptorModule());
    MockServlet mockServlet = new MockServlet();
    injector.injectMembers(mockServlet);
    assertNotNull(mockServlet.services);
    assertEquals(SERVICES.size(), mockServlet.services.getServices().size());
  }

  private static class Visitor extends DefaultBindingTargetVisitor<Object, Void>
      implements ServletModuleTargetVisitor<Object, Void> {
    List<LinkedFilterBinding> linkedFilters = Lists.newArrayList();
    List<LinkedServletBinding> linkedServlets = Lists.newArrayList();
    List<InstanceFilterBinding> instanceFilters = Lists.newArrayList();
    List<InstanceServletBinding> instanceServlets = Lists.newArrayList();
    Binding<?> systemServiceProvider = null;

    @Override
    public Void visit(LinkedFilterBinding binding) {
      linkedFilters.add(binding);
      return null;
    }

    @Override
    public Void visit(InstanceFilterBinding binding) {
      instanceFilters.add(binding);
      return null;
    }

    @Override
    public Void visit(LinkedServletBinding binding) {
      linkedServlets.add(binding);
      return null;
    }

    @Override
    public Void visit(InstanceServletBinding binding) {
      instanceServlets.add(binding);
      return null;
    }

    @Override
    public Void visit(ProviderInstanceBinding<?> providerInstanceBinding) {
      extractSystemServiceBinding(providerInstanceBinding);
      return null;
    }

    @Override
    protected Void visitOther(Binding<?> binding) {
      extractSystemServiceBinding(binding);
      return null;
    }

    private void extractSystemServiceBinding(Binding<?> binding) {
      Class<?> clazz = binding.getKey().getTypeLiteral().getRawType();
      if (ServiceMap.class.equals(clazz)) {
        systemServiceProvider = binding;
      }
    }
  }

  private static class MockServlet {
    @Inject
    public ServiceMap services;
  }
}
