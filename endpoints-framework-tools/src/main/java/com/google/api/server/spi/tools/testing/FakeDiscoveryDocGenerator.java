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
import com.google.api.server.spi.tools.DiscoveryDocGenerator;

import java.io.IOException;
import java.util.List;

/**
 * Generates a Discovery document for any API configuration by loading a static JSON file.
 */
public class FakeDiscoveryDocGenerator implements DiscoveryDocGenerator {

  @Override
  public String generateDiscoveryDoc(String apiConfigJson, Format format) {
    try {
      return IoUtil.readResourceFile(getClass(),
          "fake-discovery-doc-" + format.toString().toLowerCase() + ".json");
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  /* This function is just here to implement DiscvoeryDocGenerator interface
   * and is currently not used. And the fake-api-directory.json does not yet exist
   */
  public String generateApiDirectory(List<String> apiConfigsJson) {
    try {
      return IoUtil.readResourceFile(getClass(),
          "fake-api-directory.json");
    } catch (IOException e) {
      return null;
    }
  }
}
