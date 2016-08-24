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
package com.google.api.server.spi.tools;

import java.io.IOException;
import java.util.List;

/**
 * Discovery document generator.
 */
public interface DiscoveryDocGenerator {

  /**
   * Style of API Discovery document is for.
   */
  enum Format {
    /**
     * Discovery document for REST style of API.
     */
    REST,

    /**
     * Discovery document for RPC style of API.
     */
    RPC
  }

  /**
   * Generates the Discovery document of an API.
   *
   * @param apiConfigJson API configuration in JSON format, which is expected to have two
   *     properties: "config" and "descriptor". Output from ApiConfigGenerator#generateFor() is
   *     a suitable input here.
   * @param format Whether to generate the discovery doc for REST or RPC style of the API.
   * @return API's Discovery document in the desired format
   * @throws IOException
   */
  public String generateDiscoveryDoc(String apiConfigJson, Format format) throws IOException;

  /**
   * Generates the directory document for a set of APIs.
   *
   * @param apiConfigsJson A list of API configurations in JSON format.
   * @return The API directory
   * @throws IOException
   */
  public String generateApiDirectory(List<String> apiConfigsJson) throws IOException;

}
