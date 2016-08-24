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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.Module.SetupContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

/** Unit tests for {@link ConfiguredObjectMapper}. */
@RunWith(MockitoJUnitRunner.class)
public class ConfiguredObjectMapperTest {

  private Map<ConfiguredObjectMapper.CacheKey, ConfiguredObjectMapper> cache;
  private ConfiguredObjectMapper.Builder builder;

  @Mock Module moduleA;
  @Mock Module moduleB;
  @Mock Module moduleC;
  @Mock ObjectMapperUtil util;
  private ApiSerializationConfig fooConfig;
  private ApiSerializationConfig barConfig;

  private abstract class FooSerializer implements Transformer<String, Long> {
  }

  private abstract class BarSerializer implements Transformer<String, Long> {
  }

  @Before
  public void setUp() throws Exception {
    fooConfig = new ApiSerializationConfig();
    fooConfig.addSerializationConfig(FooSerializer.class);
    barConfig = new ApiSerializationConfig();
    barConfig.addSerializationConfig(BarSerializer.class);
    cache = Maps.newLinkedHashMap();
    builder = new ConfiguredObjectMapper.Builder(cache, 100);
  }

  private static void doModuleSetup(Module module, String name) {
    when(module.getModuleName()).thenReturn(name);
    when(module.version()).thenReturn(Version.unknownVersion());
  }

  @Test
  public void testConstructor_nullCache() {
    try {
      new ConfiguredObjectMapper.Builder(null, 100);
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void testConstructor_badCacheSize() {
    try {
      new ConfiguredObjectMapper.Builder(cache, 0);
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testBuildDefault() {
    ConfiguredObjectMapper result1 = builder.build();
    assertEquals(1, cache.size());
    ConfiguredObjectMapper result2 = builder.build();
    assertEquals(1, cache.size());
    assertSame(result1, result2);
  }

  @Test
  public void testBuildWithModules_nullModules() {
    try {
      builder.addRegisteredModules(null).build();
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void testBuildWithModules_oneWithAll() {
    doModuleSetup(moduleA, "moduleA");
    doModuleSetup(moduleB, "moduleB");
    doModuleSetup(moduleC, "moduleC");
    builder.addRegisteredModules(ImmutableList.of(moduleA, moduleB, moduleC)).build();
    Mockito.verify(moduleA, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.verify(moduleB, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.verify(moduleC, atLeastOnce()).setupModule(any(SetupContext.class));
    assertEquals(1, cache.size());
  }

  @Test
  public void testBuildWithModules_manyWithEmpty() {
    ConfiguredObjectMapper result1 =
        builder.addRegisteredModules(ImmutableList.<Module>of()).build();
    assertEquals(1, cache.size());
    ConfiguredObjectMapper result2 =
        builder.addRegisteredModules(ImmutableList.<Module>of()).build();
    assertEquals(1, cache.size());
    assertSame(result1, result2);
  }

  @Test
  public void testBuildWithModules_manyWithAll() {
    doModuleSetup(moduleA, "moduleA");
    doModuleSetup(moduleB, "moduleB");
    doModuleSetup(moduleC, "moduleC");
    ConfiguredObjectMapper result1 = builder
        .addRegisteredModules(ImmutableList.of(moduleA, moduleB))
        .addRegisteredModules(ImmutableList.of(moduleC))
        .build();
    Mockito.verify(moduleA, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.verify(moduleB, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.verify(moduleC, atLeastOnce()).setupModule(any(SetupContext.class));
    assertEquals(1, cache.size());
    ConfiguredObjectMapper result2 = builder
        .addRegisteredModules(ImmutableList.of(moduleA, moduleB, moduleC))
        .build();
    assertEquals(1, cache.size());
    assertSame(result1, result2);
  }

  @Test
  public void testBuildWithConfig_misses() {
    builder.apiSerializationConfig(null).build();
    assertEquals(1, cache.size());
    builder.apiSerializationConfig(fooConfig).build();
    assertEquals(2, cache.size());
    builder.apiSerializationConfig(barConfig).build();
    assertEquals(3, cache.size());
  }

  @Test
  public void testBuildWithConfig_hits() {
    ConfiguredObjectMapper resultDefault1 = builder.apiSerializationConfig(null).build();
    ConfiguredObjectMapper resultDefault2 = builder.build();
    assertEquals(1, cache.size());
    assertSame(resultDefault1, resultDefault2);
    ConfiguredObjectMapper resultFoo1 = builder.apiSerializationConfig(fooConfig).build();
    ConfiguredObjectMapper resultFoo2 = builder
        .apiSerializationConfig(barConfig)
        .apiSerializationConfig(new ApiSerializationConfig(fooConfig))
        .build();
    assertEquals(2, cache.size());
    assertSame(resultFoo1, resultFoo2);
  }

  @Test
  public void testEviction() {
    doModuleSetup(moduleA, "moduleA");
    builder = new ConfiguredObjectMapper.Builder(cache, 1);
    builder.addRegisteredModules(ImmutableList.of(moduleA)).build();
    assertEquals(1, cache.size());
    Mockito.verify(moduleA, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.reset();

    cache = Maps.newLinkedHashMap();
    doModuleSetup(moduleB, "moduleB");
    doModuleSetup(moduleA, "moduleA");

    // Evict the other entries
    new ConfiguredObjectMapper.Builder(cache, 1)
        .addRegisteredModules(ImmutableList.of(moduleB))
        .build();
    // Now this is a miss
    new ConfiguredObjectMapper.Builder(cache, 1)
        .addRegisteredModules(ImmutableList.of(moduleA))
        .build();
    assertEquals(1, cache.size());
    Mockito.verify(moduleA, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.verify(moduleB, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.reset();
  }

  @Test
  public void testBuildComplex() {
    // Cache miss
    doModuleSetup(moduleA, "moduleA");
    doModuleSetup(moduleB, "moduleB");
    builder = new ConfiguredObjectMapper.Builder(cache, 100);
    ConfiguredObjectMapper firstResultAB = builder
        .addRegisteredModules(ImmutableList.of(moduleA, moduleB))
        .build();
    Mockito.verify(moduleA, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.verify(moduleB, atLeastOnce()).setupModule(any(SetupContext.class));
    assertEquals(1, cache.size());
    Mockito.reset();

    // Cache miss
    doModuleSetup(moduleA, "moduleA");
    builder = new ConfiguredObjectMapper.Builder(cache, 100);
    ConfiguredObjectMapper firstResultA = builder
        .addRegisteredModules(ImmutableList.of(moduleA))
        .apiSerializationConfig(fooConfig)
        .build();
    assertEquals(2, cache.size());
    Mockito.verify(moduleA, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.reset();

    // Cache miss
    doModuleSetup(moduleB, "moduleB");
    builder = new ConfiguredObjectMapper.Builder(cache, 100);
    ConfiguredObjectMapper firstResultB =
        builder.addRegisteredModules(ImmutableList.of(moduleB)).build();
    assertEquals(3, cache.size());

    // Cache hit
    builder = new ConfiguredObjectMapper.Builder(cache, 100);
    ConfiguredObjectMapper secondResultAB = builder
        .addRegisteredModules(ImmutableList.of(moduleB, moduleA))
        .build();
    assertEquals(3, cache.size());
    assertSame(firstResultAB, secondResultAB);
    Mockito.verify(moduleB, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.reset();

    // Cache hit, with config
    builder = new ConfiguredObjectMapper.Builder(cache, 100);
    ConfiguredObjectMapper secondResultA = builder
        .apiSerializationConfig(fooConfig)
        .addRegisteredModules(ImmutableList.of(moduleA))
        .build();
    assertEquals(3, cache.size());
    assertSame(firstResultA, secondResultA);
    Mockito.reset();

    // Cache miss
    doModuleSetup(moduleA, "moduleA");
    doModuleSetup(moduleC, "moduleC");
    builder = new ConfiguredObjectMapper.Builder(cache, 100);
    ConfiguredObjectMapper firstResultAC = builder
        .addRegisteredModules(ImmutableList.of(moduleA, moduleC))
        .build();
    assertEquals(4, cache.size());
    Mockito.verify(moduleA, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.verify(moduleC, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.reset();

    // Cache miss, config doesn't match
    doModuleSetup(moduleA, "moduleA");
    builder = new ConfiguredObjectMapper.Builder(cache, 100);
    ConfiguredObjectMapper otherResultA = builder
        .addRegisteredModules(ImmutableList.of(moduleA))
        .apiSerializationConfig(barConfig)
        .build();
    assertEquals(5, cache.size());
    assertNotSame(firstResultA, otherResultA);
    Mockito.verify(moduleA, atLeastOnce()).setupModule(any(SetupContext.class));
    Mockito.reset();

    // Check the cache contents
    assertThat(cache.values())
        .containsExactly(firstResultA, firstResultAB, firstResultB, firstResultAC, otherResultA);
    assertEquals(
        5, ImmutableSet.of(firstResultA.delegate, firstResultAB.delegate, firstResultB.delegate,
                           firstResultAC.delegate, otherResultA.delegate).size());
  }
}
