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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation for customizing API output by simple type conversion.
 * <p>
 * This can be used in two ways. The first is to annotate a class. This specifies that the
 * transformer should be used to convert any objects of this type to the target type. This applies
 * to all APIs in the current App Engine application.
 * <p>
 * The second is to use {@link Api#transformers()}. This can be used to customize serialization of
 * third-party classes, but is not applied globally and is instead done per-API.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiTransformer {
  /**
   * The serializer class to do the type conversion. It must be possible to create this class using
   * {@link Class#newInstance()}.
   */
  Class<? extends Transformer<?, ?>> value();
}
