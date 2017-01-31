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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.appengine.api.utils.SystemProperty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link EnvUtil}.
 */
@RunWith(JUnit4.class)
public class EnvUtilTest {
  public void testIsRunningOnAppEngine() {
    System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, "Production");
    assertTrue(EnvUtil.isRunningOnAppEngine());
    System.clearProperty(EnvUtil.ENV_APPENGINE_RUNTIME);
    assertFalse(EnvUtil.isRunningOnAppEngine());
  }

  @Test
  public void testIsRunningOnAppEngineProd() {
    SystemProperty.environment.set(SystemProperty.Environment.Value.Production);
    assertTrue(EnvUtil.isRunningOnAppEngineProd());
    SystemProperty.environment.set(SystemProperty.Environment.Value.Development);
    assertFalse(EnvUtil.isRunningOnAppEngineProd());
    System.clearProperty(EnvUtil.ENV_APPENGINE_RUNTIME);
    assertFalse(EnvUtil.isRunningOnAppEngineProd());
  }
}
