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

import com.google.common.base.Preconditions;
import com.google.inject.Binder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

/**
 * A class which introduces heterogenous bindings for service providers.
 * Called from {@link EndpointsModule} and injected into {@link GuiceEndpointsServlet}.
 */
public class ServiceMap {

  /**
   * Build method for a {@link ServiceMap}.
   * 
   * @param binder A module's binder which has bindings for all service classes
   * @param classes List of service class.
   * @return A {@link ServiceMap} object.
   */
  public static ServiceMap create(Binder binder, Iterable<Class<?>> classes) {
    Map<Class<?>, Provider<?>> services = new HashMap<Class<?>, Provider<?>>();
    for (Class<?> clazz : classes) {
      Provider<?> object = binder.getProvider(clazz);
      if (object != null) {
        services.put(clazz, object);
      }
    }
    return new ServiceMap(services);
  }

  private final Map<Class<?>, Provider<?>> services;

  private ServiceMap(Map<Class<?>, Provider<?>> services) {
    this.services = Preconditions.checkNotNull(services, "services");
  }

  /**
   * Gets the object associated with the given service class.
   * 
   * @param clazz The service class
   * @return The service object.
   */
  public <T> T get(Class<T> clazz) {
    Preconditions.checkNotNull(clazz, "clazz");
    @SuppressWarnings("unchecked")
    Provider<T> provider = 
        (Provider<T>) Preconditions.checkNotNull(services.get(clazz), "provider of " + clazz);
    return provider.get();
  }
  
  /**
   * Return the service classes in the map.
   */
  Iterable<Class<?>> getClasses() {
    return services.keySet();
  }

  /**
   * Return the service objects in the amp.
   */
  Collection<Object> getServices() {
    return new ArrayList<Object>() {{
      for (Provider<?> provider : services.values()) {
        add(provider.get());
      }
    }};
  }
}