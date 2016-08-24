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
package com.google.api.server.spi.config.annotationreader;

import static org.junit.Assert.assertEquals;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.model.ApiCacheControlConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ApiCacheControlConfig}
 */
@RunWith(JUnit4.class)
public class ApiCacheControlAnnotationConfigTest {
  private ApiCacheControlConfig config;
  private ApiCacheControlAnnotationConfig annotationConfig;

  @Before
  public void setUp() throws Exception {
    config = new ApiCacheControlConfig();
    annotationConfig = new ApiCacheControlAnnotationConfig(config);
  }

  @Test
  public void testDefaults() {
    assertEquals("no-cache", config.getType());
    assertEquals(0, config.getMaxAge());
  }

  @Test
  public void testSetTypeIfNotEmpty() {
    annotationConfig.setTypeIfNotEmpty("bleh");
    assertEquals("bleh", config.getType());
  }

  @Test
  public void testSetTypeIfNotEmpty_empty() {
    annotationConfig.setTypeIfNotEmpty("");
    testDefaults();

    annotationConfig.setTypeIfNotEmpty("foo");
    annotationConfig.setTypeIfNotEmpty("");
    assertEquals("foo", config.getType());
  }

  @Test
  public void testSetMaxAgeIfSpecified() {
    annotationConfig.setMaxAgeIfSpecified(4);
    assertEquals(4, config.getMaxAge());
  }

  @Test
  public void testSetMaxAgeIfSpecified_unspecified() {
    annotationConfig.setMaxAgeIfSpecified(Api.UNSPECIFIED_INT);
    testDefaults();

    annotationConfig.setMaxAgeIfSpecified(12);
    annotationConfig.setMaxAgeIfSpecified(Api.UNSPECIFIED_INT);
    assertEquals(12, config.getMaxAge());
  }
}
