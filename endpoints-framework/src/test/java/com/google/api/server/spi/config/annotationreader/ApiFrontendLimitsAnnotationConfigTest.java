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
import com.google.api.server.spi.config.model.ApiFrontendLimitsConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ApiFrontendLimitsConfig}.
 */
@RunWith(JUnit4.class)
public class ApiFrontendLimitsAnnotationConfigTest {
  private ApiFrontendLimitsConfig config;
  private ApiFrontendLimitsAnnotationConfig annotationConfig;

  @Before
  public void setUp() throws Exception {
    config = new ApiFrontendLimitsConfig();
    annotationConfig = new ApiFrontendLimitsAnnotationConfig(config);
  }

  @Test
  public void testDefaults() {
    assertEquals(-1, config.getUnregisteredUserQps());
    assertEquals(-1, config.getUnregisteredQps());
    assertEquals(-1, config.getUnregisteredDaily());
  }

  @Test
  public void testSetUnregisteredUserQpsIfSpecified() {
    annotationConfig.setUnregisteredUserQpsIfSpecified(4);
    assertEquals(4, config.getUnregisteredUserQps());
  }

  @Test
  public void testSetUnregisteredUserQpsIfSpecified_unspecified() {
    annotationConfig.setUnregisteredUserQpsIfSpecified(Api.UNSPECIFIED_INT);
    testDefaults();

    annotationConfig.setUnregisteredUserQpsIfSpecified(22);
    annotationConfig.setUnregisteredUserQpsIfSpecified(Api.UNSPECIFIED_INT);
    assertEquals(22, config.getUnregisteredUserQps());
  }

  @Test
  public void testSetUnregisteredQpsIfSpecified() {
    annotationConfig.setUnregisteredQpsIfSpecified(4);
    assertEquals(4, config.getUnregisteredQps());
  }

  @Test
  public void testSetUnregisteredQpsIfSpecified_unspecified() {
    annotationConfig.setUnregisteredQpsIfSpecified(Api.UNSPECIFIED_INT);
    testDefaults();

    annotationConfig.setUnregisteredQpsIfSpecified(22);
    annotationConfig.setUnregisteredQpsIfSpecified(Api.UNSPECIFIED_INT);
    assertEquals(22, config.getUnregisteredQps());
  }

  @Test
  public void testSetUnregisteredDailyIfSpecified() {
    annotationConfig.setUnregisteredDailyIfSpecified(4);
    assertEquals(4, config.getUnregisteredDaily());
  }

  @Test
  public void testSetUnregisteredDailyIfSpecified_unspecified() {
    annotationConfig.setUnregisteredDailyIfSpecified(Api.UNSPECIFIED_INT);
    testDefaults();

    annotationConfig.setUnregisteredDailyIfSpecified(22);
    annotationConfig.setUnregisteredDailyIfSpecified(Api.UNSPECIFIED_INT);
    assertEquals(22, config.getUnregisteredDaily());
  }

  @Test
  public void testAddRule() {
    assertEquals(0, config.getRules().size());

    config.addRule("bleh", 4, 5, -1, "foo");
    assertEquals(1, config.getRules().size());
    assertEquals("bleh", config.getRules().get(0).getMatch());
  }
}
