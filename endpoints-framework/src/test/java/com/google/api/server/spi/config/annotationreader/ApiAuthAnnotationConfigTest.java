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

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.model.ApiAuthConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

/**
 * Tests for {@link ApiAuthConfig}.
 */
@RunWith(JUnit4.class)
public class ApiAuthAnnotationConfigTest {
  private ApiAuthConfig config;
  private ApiAuthAnnotationConfig annotationConfig;

  @Before
  public void setUp() throws Exception {
    config = new ApiAuthConfig();
    annotationConfig = new ApiAuthAnnotationConfig(config);
  }

  @Test
  public void testDefaultConfig() {
    assertEquals(false, config.getAllowCookieAuth());
    assertEquals(0, config.getBlockedRegions().size());
  }

  @Test
  public void testSetAllowCookieAuthIfSpecified() {
    annotationConfig.setAllowCookieAuthIfSpecified(AnnotationBoolean.TRUE);
    assertEquals(true, config.getAllowCookieAuth());
  }

  @Test
  public void testSetAllowCookieAuthIfSpecified_unspecified() {
    annotationConfig.setAllowCookieAuthIfSpecified(AnnotationBoolean.UNSPECIFIED);
    testDefaultConfig();

    annotationConfig.setAllowCookieAuthIfSpecified(AnnotationBoolean.TRUE);
    annotationConfig.setAllowCookieAuthIfSpecified(AnnotationBoolean.UNSPECIFIED);
    assertEquals(true, config.getAllowCookieAuth());
  }

  @Test
  public void testSetBlockedRegionsIfNotEmpty() {
    String[] regions = { "foo", "bar" };
    annotationConfig.setBlockedRegionsIfNotEmpty(regions);
    assertEquals(Arrays.asList(regions), config.getBlockedRegions());
  }

  @Test
  public void testSetBlockedRegionsIfNotEmpty_empty() {
    String[] regions = {};
    annotationConfig.setBlockedRegionsIfNotEmpty(regions);
    testDefaultConfig();
  }
}
