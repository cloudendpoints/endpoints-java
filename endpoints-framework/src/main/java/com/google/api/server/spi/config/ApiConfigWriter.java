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
package com.google.api.server.spi.config;

import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;

import java.util.Map;

/**
 * Generator of wire-formatted configuration strings to send to the API frontend.
 *
 * @author Eric Orth
 */
public interface ApiConfigWriter {
  /**
   * Generate wire-formatted configuration strings for the given configs.
   *
   * @return A map from {@link ApiKey}s to wire-formatted configuration strings.
   */
  Map<ApiKey, String> writeConfig(Iterable<? extends ApiConfig> configs) throws ApiConfigException;

  /**
   * The file extension associated with this writer.
   */
  String getFileExtension();
}
