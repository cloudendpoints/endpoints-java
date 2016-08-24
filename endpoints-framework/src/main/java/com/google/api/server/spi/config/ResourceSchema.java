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

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Defines the schema for a resource, which will be exposed in discovery.
 */
public class ResourceSchema {

  private final String name;
  private final Type type;
  private final ImmutableMap<String, ResourcePropertySchema> properties;

  private ResourceSchema(Builder builder) {
    this.name = builder.name;
    this.properties = builder.properties.build();
    this.type = builder.type;
  }

  /**
   * Returns the custom name of the resource as it should appear in the schema. Returns null if no
   * custom name was specified.
   */
  @Nullable
  public String getName() {
    return name;
  }

  /**
   * Gets the type the current schema represents.
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns map of property name to property schema.
   */
  public Map<String, ResourcePropertySchema> getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ResourceSchema)) {
      return false;
    }

    ResourceSchema that = (ResourceSchema) obj;
    return Objects.equals(this.name, that.name)
        && Objects.equals(this.properties, that.properties)
        && Objects.equals(this.type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, properties, type);
  }

  @Override
  public String toString() {
    ToStringHelper helper = MoreObjects.toStringHelper(this.getClass())
        .add("_name", name)
        .add("_type", type);
    for (Entry<String, ResourcePropertySchema> property : properties.entrySet()) {
      helper.add(property.getKey(), property.getValue());
    }
    return helper.toString();
  }

  /**
   * Constructs a new builder for {@code ResourceSchema}s.
   *
   * @param type the resource type
   */
  public static Builder builderForType(Type type) {
    return new Builder(type);
  }

  /**
   * Constructs a new builder populated with an existing {@code ResourceSchema}.
   *
   * @param schema a resource schema
   */
  public static Builder builderWithSchema(ResourceSchema schema) {
    return new Builder(schema);
  }

  /**
   * A builder for {@code ResourceSchema}.
   */
  public static class Builder {
    private String name;
    private Type type;
    private ImmutableMap.Builder<String, ResourcePropertySchema> properties =
        ImmutableMap.builder();

    private Builder(Type type) {
      Preconditions.checkNotNull(type);
      this.type = type;
      this.name = null;
    }

    private Builder(ResourceSchema schema) {
      Preconditions.checkNotNull(schema);
      this.name = schema.name;
      this.type = schema.type;
      this.properties.putAll(schema.properties);
    }

    /**
     * Sets the resource name. If null is specified, the default type will be used.
     *
     * @return this builder for method chaining
     */
    public Builder setName(@Nullable String name) {
      this.name = name;
      return this;
    }

    /**
     * Adds a property to the resource.
     *
     * @param name the property name
     * @param propertySchema the property schema
     * @return this builder for method chaining
     */
    public Builder addProperty(String name, ResourcePropertySchema propertySchema) {
      properties.put(name, propertySchema);
      return this;
    }

    /**
     * Builds a new {@code ResourceSchema}.
     */
    public ResourceSchema build() {
      return new ResourceSchema(this);
    }
  }
}
