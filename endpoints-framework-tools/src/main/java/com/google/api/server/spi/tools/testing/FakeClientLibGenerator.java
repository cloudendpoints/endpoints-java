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
import com.google.api.server.spi.tools.ClientLibGenerator;

import java.io.File;
import java.io.IOException;

/**
 * Generates a client library for any discovery document by loading a static jar file.
 */
public class FakeClientLibGenerator implements ClientLibGenerator {

  @Override
  public void generateClientLib(String discoveryDoc, String language, String languageVersion,
      String layout, File file) throws IOException {
    IoUtil.copy(getClass().getResourceAsStream("fake-api-client-lib.jar"), file);
  }
}
