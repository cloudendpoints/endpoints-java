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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for {@link BackendProperties}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BackendPropertiesTest {
  @Mock private BackendProperties.EnvReader envReader;
  private BackendProperties appEngineProperties;
  private BackendProperties properties;

  public static final String PROJECT_NUMBER_STR = "123456789123456789";
  public static final long PROJECT_NUMBER = 123456789123456789L;
  public static final String PROJECT_ID = "project_awesome";
  public static final String APPLICATION_ID = "awesome_app";

  @Before
  public void setUp() {
    this.appEngineProperties = new BackendProperties(true, envReader);
    this.properties = new BackendProperties(false, envReader);

    System.setProperty(BackendProperties.APP_ID_PROPERTY, APPLICATION_ID);

    Mockito.when(envReader.getenv(BackendProperties.PROJECT_NUMBER_PROPERTY))
        .thenReturn(PROJECT_NUMBER_STR);
    Mockito.when(envReader.getenv(BackendProperties.PROJECT_ID_PROPERTY)).thenReturn(PROJECT_ID);
  }

  @Test
  public void testGetProjectNumber_appEngine() {
    assertEquals(BackendProperties.PROJECT_NUMBER_UNKNOWN, appEngineProperties.getProjectNumber());
  }

  @Test
  public void testGetProjectNumber_tornado() {
    assertEquals(PROJECT_NUMBER, properties.getProjectNumber());
  }

  @Test
  public void testGetProjectNumber_tornado_notSet() {
    Mockito.when(envReader.getenv(BackendProperties.PROJECT_NUMBER_PROPERTY)).thenReturn(null);
    assertEquals(BackendProperties.PROJECT_NUMBER_UNKNOWN, properties.getProjectNumber());
  }

  @Test
  public void testGetProjectNumber_tornado_badFormat() {
    Mockito.when(envReader.getenv(BackendProperties.PROJECT_NUMBER_PROPERTY)).thenReturn("foo");
    assertEquals(BackendProperties.PROJECT_NUMBER_UNKNOWN, properties.getProjectNumber());
  }

  @Test
  public void testGetProjectId_appEngine() {
    assertNull(appEngineProperties.getProjectId());
  }

  @Test
  public void testGetProjectId_tornado() {
    assertEquals(PROJECT_ID, properties.getProjectId());
  }

  @Test
  public void testGetProjectId_tornado_notSet() {
    Mockito.when(envReader.getenv(BackendProperties.PROJECT_ID_PROPERTY)).thenReturn(null);
    assertNull(properties.getProjectId());
  }

  @Test
  public void testGetApplicationId_appEngine() {
    assertEquals(APPLICATION_ID, appEngineProperties.getApplicationId());
  }

  @Test
  public void testGetApplicationId_flex() {
    System.clearProperty(BackendProperties.APP_ID_PROPERTY);
    Mockito.when(envReader.getenv(BackendProperties.GCLOUD_PROJECT_PROPERTY))
        .thenReturn(APPLICATION_ID);
    assertNull(properties.getApplicationId());
  }
}
