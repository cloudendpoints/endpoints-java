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

import com.google.api.server.spi.config.Transformer;

import javax.annotation.Nullable;

/**
 * A default serializer implementation for testing.
 *
 * @param <TFrom> the serializer source type
 * @param <TTo> the serializer target type
 */
public class DefaultValueSerializer<TFrom, TTo> implements Transformer<TFrom, TTo> {
  private final TTo toValue;
  private final TFrom fromValue;

  /**
   * Constructs a serializer that always serializes and deserializes to {@code null}.
   */
  public DefaultValueSerializer() {
    this(null, null);
  }

  /**
   * Constructs a serializer that always serializes and deserializes to a default set of values.
   *
   * @param toValue the default serializer return
   * @param fromValue the default deserializer return
   */
  public DefaultValueSerializer(@Nullable TTo toValue, @Nullable TFrom fromValue) {
    this.toValue = toValue;
    this.fromValue = fromValue;
  }

  @Nullable
  @Override
  public TTo transformTo(TFrom in) {
    return toValue;
  }

  @Nullable
  @Override
  public TFrom transformFrom(TTo in) {
    return fromValue;
  }
}
