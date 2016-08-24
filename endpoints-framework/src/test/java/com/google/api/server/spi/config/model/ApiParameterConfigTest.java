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
package com.google.api.server.spi.config.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.model.ApiParameterConfig.Classification;
import com.google.api.server.spi.testing.DefaultValueSerializer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

/**
 * Tests for {@link ApiParameterConfig}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiParameterConfigTest {
  private ApiParameterConfig config;
  private ApiParameterConfig configWithArray;
  private ApiSerializationConfig serializationConfig;
  private TypeLoader typeLoader;

  @Mock private ApiConfig apiConfig;
  @Mock private ApiClassConfig apiClassConfig;
  @Mock private ApiMethodConfig apiMethodConfig;

  private static class TestSerializer1 extends DefaultValueSerializer<String, Integer[]> {
    @SuppressWarnings("unused")
    public TestSerializer1() {
      super(new Integer[] {21}, "bleh");
    }
  }

  private static class TestSerializer2 extends DefaultValueSerializer<String, Integer[]> {
    @SuppressWarnings("unused")
    public TestSerializer2() {
      super(new Integer[] {27}, "bleh");
    }
  }

  private static class TestSerializer3 extends DefaultValueSerializer<Integer, Boolean> {
    @SuppressWarnings("unused")
    public TestSerializer3() {
      super(true, 4);
    }
  }

  private static class TestSerializer4 extends DefaultValueSerializer<Integer, Boolean> {
    @SuppressWarnings("unused")
    public TestSerializer4() {
      super(false, 5);
    }
  }

  @Before
  public void setUp() throws Exception {
    serializationConfig = new ApiSerializationConfig();
    typeLoader = new TypeLoader();

    Mockito.when(apiMethodConfig.getApiClassConfig()).thenReturn(apiClassConfig);
    Mockito.when(apiClassConfig.getApiConfig()).thenReturn(apiConfig);
    Mockito.when(apiConfig.getSerializationConfig()).thenReturn(serializationConfig);

    config = new ApiParameterConfig(apiMethodConfig, "bleh", false, null, String.class, typeLoader);
    configWithArray =
        new ApiParameterConfig(apiMethodConfig, "bleh", false, null, Boolean[].class, typeLoader);
  }

  @Test
  public void testRepeated() {
    assertTrue(configWithArray.isRepeated());
    assertEquals(Boolean[].class, configWithArray.getSchemaBaseType());
    assertEquals(Boolean.class, configWithArray.getRepeatedItemType());
    assertEquals(Boolean.class, configWithArray.getRepeatedItemSerializedType());
  }

  @Test
  public void testNotRepeated() {
    assertFalse(config.isRepeated());
    assertEquals(String.class, config.getSchemaBaseType());
    assertNull(config.getRepeatedItemType());
    assertNull(config.getRepeatedItemSerializedType());
  }

  @Test
  public void testSerializedToRepeated() {
    config.setSerializer(TestSerializer1.class);
    config.setRepeatedItemSerializer(TestSerializer3.class);

    assertEquals(Integer[].class, config.getSchemaBaseType());
    assertTrue(config.isRepeated());
    assertEquals(Integer.class, config.getRepeatedItemType());
    assertEquals(Boolean.class, config.getRepeatedItemSerializedType());
  }

  @Test
  public void testGetSerializers() {
    serializationConfig.addSerializationConfig(TestSerializer2.class);
    serializationConfig.addSerializationConfig(TestSerializer4.class);
    config.setSerializer(TestSerializer1.class);
    config.setRepeatedItemSerializer(TestSerializer3.class);

    assertEquals(Collections.singletonList(TestSerializer1.class), config.getSerializers());
    assertEquals(Collections.singletonList(TestSerializer3.class),
        config.getRepeatedItemSerializers());
  }

  @Test
  public void testGetSerializer_default() {
    serializationConfig.addSerializationConfig(TestSerializer2.class);
    serializationConfig.addSerializationConfig(TestSerializer4.class);

    assertEquals(Collections.singletonList(TestSerializer2.class), config.getSerializers());
    assertEquals(Collections.singletonList(TestSerializer4.class),
        config.getRepeatedItemSerializers());
  }

  @Test
  public void testGetSerializer_none() {
    assertTrue(config.getSerializers().isEmpty());
    assertTrue(config.getRepeatedItemSerializers().isEmpty());
  }

  @Test
  public void standardParametersAreInjected() {
    for (String param : StandardParameters.STANDARD_PARAM_NAMES) {
      assertThat(createStandardParameter(param).getClassification())
          .isEqualTo(Classification.INJECTED);
    }
  }

  private ApiParameterConfig createStandardParameter(String name) {
    return new ApiParameterConfig(apiMethodConfig, "alt", false, null, String.class, typeLoader);
  }
}
