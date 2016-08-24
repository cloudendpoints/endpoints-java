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
package com.google.api.server.spi.tools.testing;

import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.tools.ApiConfigGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Generates any API configuration by loading a static JSON file.
 */
public class FakeApiConfigGenerator implements ApiConfigGenerator {

  @Override
  public Map<String, String> generateConfig(Class<?>... serviceClasses) {
    return generateConfig(null, serviceClasses);
  }

  @Override
  public Map<String, String> generateConfig(@Nullable ServiceContext serviceContext,
      Class<?>... serviceClasses) {
    try {
      Map<String, String> apiConfigs = new HashMap<String, String>();
      apiConfigs.put("guestbook-v1.api",
          IoUtil.readResourceFile(getClass(), "fake-api-config.json"));
      return apiConfigs;
    } catch (IOException e) {
      return null;
    }
  }
}
