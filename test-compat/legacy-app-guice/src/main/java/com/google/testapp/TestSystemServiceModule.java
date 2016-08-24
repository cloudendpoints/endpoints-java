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
package com.google.testapp;

import com.google.api.server.spi.guice.GuiceSystemServiceServletModule;
import com.google.common.collect.ImmutableList;
import com.google.inject.Scopes;
import com.google.testapi.TestEndpoint;
import com.google.waxapi.InMemoryWaxDataStore;
import com.google.waxapi.WaxEndpoint;

/**
 * A Guice-configured test servlet module.
 */
public class TestSystemServiceModule extends GuiceSystemServiceServletModule {
  @Override
  public void configureServlets() {
    bind(WaxEndpoint.class).toInstance(new WaxEndpoint(new InMemoryWaxDataStore()));
    bind(TestEndpoint.class).in(Scopes.SINGLETON);
    serveGuiceSystemServiceServlet("/_ah/spi/*",
        ImmutableList.of(WaxEndpoint.class, TestEndpoint.class));
  }
}
