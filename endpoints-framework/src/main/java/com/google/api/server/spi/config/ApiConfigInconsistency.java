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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Data object to represent a value inconsistency for the same property in two different
 * configurations.
 * 
 * The two inconsistent values of the property can be thought of as 'expected' and 'actual'
 * except those names imply that the inconsistency is an error.  This class makes no assumptions on
 * the desirability of the inconsistency.  Thus the two values are simply called 'value1' and
 * 'value2'.
 *
 * @param <T> The data type of the inconsistent property.
 *
 * @author Eric Orth
 */
public class ApiConfigInconsistency<T> {
  private String propertyName;
  private T value1;
  private T value2;

  public ApiConfigInconsistency(String propertyName, T value1, T value2) {
    this.propertyName = propertyName;
    this.value1 = value1;
    this.value2 = value2;
  }

  @Override
  public String toString() {
    return String.format("Inconsistency: %s (%s vs %s)", propertyName, value1, value2);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof ApiConfigInconsistency) {
      ApiConfigInconsistency<?> inconsistency = (ApiConfigInconsistency<?>) other;
      return propertyName.equals(inconsistency.propertyName) && value1.equals(inconsistency.value1)
          && value2.equals(inconsistency.value2);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertyName, value1, value2);
  }

  public String getPropertyName()  {
    return propertyName;
  }

  public T getValue1()  {
    return value1;
  }

  public T getValue2() {
    return value2;
  }

  public static <T> ListBuilder<T> listBuilder() {
    return new ListBuilder<T>();
  }

  /**
   * Builder to build a list of inconsistency objects.
   */
  public static class ListBuilder<T> {
    @Nullable private ImmutableList.Builder<ApiConfigInconsistency<T>> builder;

    private ListBuilder() {}

    private void createBuilderIfNecessary() {
      if (builder == null) {
        builder = ImmutableList.builder();
      }
    }

    /**
     * Creates and adds an inconsistency object to the list iff the two given values are not equal.
     */
    public <T1 extends T> ListBuilder<T> addIfInconsistent(String propertyName, T1 value1,
        T1 value2) {
      if (!Objects.equals(value1, value2)) {
        createBuilderIfNecessary();
        builder.add(new ApiConfigInconsistency<T>(propertyName, value1, value2));
      }
      return this;
    }

    // Safe to cast Iterable<ApiConfigInconsistency<T1>> to Iterable<ApiConfigInconsistency<T>>
    // because T1 extends T.
    @SuppressWarnings("unchecked")
    public <T1 extends T> ListBuilder<T> addAll(
        Iterable<ApiConfigInconsistency<T1>> inconsistencies) {
      if (!Iterables.isEmpty(inconsistencies)) {
        createBuilderIfNecessary();
        builder.addAll((Iterable<ApiConfigInconsistency<T>>) (Iterable<?>) inconsistencies);
      }
      return this;
    }

    public ImmutableList<ApiConfigInconsistency<T>> build() {
      if (builder == null) {
        return ImmutableList.of();
      } else {
        return builder.build();
      }
    }
  }
}
