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

import java.io.File;
import java.io.IOException;

/**
 * Client library generator.
 */
public interface ClientLibGenerator {

  /**
   * Generates the client library for an API and saves it in a file.
   *
   * @param discoveryDoc Discovery document of the API
   * @param language Language of the client library, valid options can be found at
   *        https://developers.google.com/resources/api-libraries/endpoints/genlib
   * @param languageVersion Version of language.  {@code null} for default.
   * @param layout of the client bundle. {@code null} for default.
   * @param file Zip/jar file to save the generated source into
   */
  public void generateClientLib(String discoveryDoc, String language, String languageVersion,
      String layout, File file) throws IOException;
}
