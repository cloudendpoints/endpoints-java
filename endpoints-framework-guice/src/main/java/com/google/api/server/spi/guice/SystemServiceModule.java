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

import com.google.inject.AbstractModule;

import java.util.Set;

/**
 * Configures bindings needed for system service integration.
 *
 * @author Joe Catalano
 */
@Deprecated
public abstract class SystemServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ServiceMap.class).toInstance(ServiceMap.create(binder(), getServiceClasses()));
  }

  /**
   * Gets the service classes of the system services.
   */
  protected abstract Set<Class<?>> getServiceClasses();
}
