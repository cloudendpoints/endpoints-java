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
package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.ApiReference;

/**
 * Test endpoint that references {@link Endpoint1} without overriding any configuration values.
 * Extends {@link Endpoint4} to test that API and class configuration is
 * not loaded through inheritance.  Method configuration still comes through inheritance.
 *
 * @author Eric Orth
 */
@ApiReference(Endpoint1.class)
public class SimpleReferenceEndpoint extends Endpoint4 {
  // No methods.
}
