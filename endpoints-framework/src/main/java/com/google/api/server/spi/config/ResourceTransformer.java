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

import java.util.Map;

/**
 * A specialized transformer for handling Resource types. Resources get serialized to a map of
 * property name to property value. In addition, resource transformer must expose schema. This
 * allows proper discovery documentation to be generated.
 *
 * @param <TFrom> The resource being transformed
 */
public interface ResourceTransformer<TFrom> extends Transformer<TFrom, Map<String, Object>> {

  /**
   * Gets the schema for this resource, which documents how this resource should be exposed in
   * discovery.
   */
  ResourceSchema getResourceSchema();
}
