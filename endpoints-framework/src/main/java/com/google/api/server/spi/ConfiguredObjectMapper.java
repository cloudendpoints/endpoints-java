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
package com.google.api.server.spi;

import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.google.common.flogger.FluentLogger;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * A wrapper around an {@link ObjectMapper} with a frozen configuration. This exposes a subset of
 * {@link ObjectMapper} methods, it doesn't allow modifications to configuration.
 * <p>
 * The created ObjectMapper also automatically installs some modules that are common to SPI code.
 */
public class ConfiguredObjectMapper {

  @VisibleForTesting
  final ObjectMapper delegate;

  // Constructs a {@link ConfiguredObjectMapper} that delegates calls to a {@link ObjectMapper}. It
  // assumes {@code delegate} will never change configuration.
  private ConfiguredObjectMapper(ObjectMapper delegate) {
    this.delegate = delegate;
  }

  /**
   * Returns an ObjectReader, this has an immutable configuration.
   *
   * @return a reader
   */
  public ObjectReader reader() {
    return delegate.reader();
  }

  /**
   * Returns an ObjectWriter, this has an immutable configuration.
   *
   * @return a writer
   */
  public ObjectWriter writer() {
    return delegate.writer();
  }

  /**
   * Constructs a new {@link Builder} for creating {@link ConfiguredObjectMapper} instances.
   *
   * @return a builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for constructing {@link ConfiguredObjectMapper} instances.
   */
  public static class Builder {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private int maxCacheSize;
    private ApiSerializationConfig config;
    private ImmutableSet.Builder<Module> modules = ImmutableSet.builder();


    // Global Cache
    private static final Map<CacheKey, ConfiguredObjectMapper> globalCache =
        Maps.newConcurrentMap();

    // Instance Cache
    // TODO: Consider replacing this with commons.cache, which is currently not included /
    // repackaged in the SDK.
    private final Map<CacheKey, ConfiguredObjectMapper> cache;

    private Builder() {
      this(globalCache, 1000);
    }

    @VisibleForTesting
    Builder(Map<CacheKey, ConfiguredObjectMapper> mutableCache, int maxCacheSize) {
      Preconditions.checkArgument(maxCacheSize > 0, "cache should be positive");
      this.cache = Preconditions.checkNotNull(mutableCache, "cache should not be null");
      this.maxCacheSize = maxCacheSize;
    }

    /**
     * Sets the {@link ApiSerializationConfig} that should be used to construct the backing {@link
     * ObjectMapper}. This replaces the previous configuration, {@code null} can be used to clear
     * the previous value.
     *
     * @param config an Api serialization config
     * @return the builder
     */
    public Builder apiSerializationConfig(@Nullable ApiSerializationConfig config) {
      this.config = config == null ? null : new ApiSerializationConfig(config);
      return this;
    }

    /**
     * Adds {@code modules} that will be registered in the backing {@link ObjectMapper}.
     *
     * @param modules modules to register
     * @return the builder
     */
    public Builder addRegisteredModules(Iterable<? extends Module> modules) {
      this.modules.addAll(modules);
      return this;
    }

    /**
     * Builds a {@link ConfiguredObjectMapper} using the configuration specified in this builder.
     *
     * @return the constructed object
     */
    public ConfiguredObjectMapper build() {
      CacheKey key = new CacheKey(config, modules.build());
      ConfiguredObjectMapper instance = cache.get(key);
      if (instance == null) {
        ObjectMapper mapper =
            ObjectMapperUtil.createStandardObjectMapper(key.apiSerializationConfig);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        for (Module module : key.modulesSet) {
          mapper.registerModule(module);
        }
        instance = new ConfiguredObjectMapper(mapper);

        // Evict all entries if the cache grows beyond a certain size.
        if (maxCacheSize <= cache.size()) {
          cache.clear();
        }

        cache.put(key, instance);
        logger.atFine().log("Cache miss, created ObjectMapper");
      } else {
        logger.atFine().log("Cache hit, reusing ObjectMapper");
      }
      return instance;
    }
  }

  // A key that uniquely identify a cached object mapper.
  @VisibleForTesting
  static class CacheKey {
    private final ApiSerializationConfig apiSerializationConfig;
    private final ImmutableSet<Module> modulesSet;

    private CacheKey(@Nullable ApiSerializationConfig config, ImmutableSet<Module> modules) {
      this.apiSerializationConfig = config;
      this.modulesSet = modules;
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.apiSerializationConfig, this.modulesSet);
    }

    @Override
    public boolean equals(@Nullable Object object) {
      if (this == object) {
        return true;
      }
      if (object instanceof CacheKey) {
        CacheKey that = (CacheKey) object;
        return Objects.equals(this.apiSerializationConfig, that.apiSerializationConfig)
            && this.modulesSet.equals(that.modulesSet);
      }
      return false;
    }
  }
}
