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

/**
 * An interface used to transform between a bean type and another type during JSON serialization.
 * <p>
 * A serializer is constructed using one of the following constructor signatures (sorted by
 * precedence):
 * <ol>
 * <li>A constructor taking in a single {@code java.lang.reflect.Type}. This is the type being
 * serialized.</li>
 * <li>A constructor taking in a single {@code java.lang.Class}. This is the class being serialized.
 * </li>
 * <li>A no-arg constructor</li>
 * </ol>
 *
 * @param <TFrom> The type being transformed
 * @param <TTo> The type being transformed to
 */
public interface Transformer<TFrom, TTo> {
  /**
   * Converts the source object into the destination type.
   */
  TTo transformTo(TFrom in);

  /**
   * Converts an object from the destination type into the source type.
   */
  TFrom transformFrom(TTo in);
}
