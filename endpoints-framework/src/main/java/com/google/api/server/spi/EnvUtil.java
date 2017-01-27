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

/**
 * Utililty for runtime environment related operations, such as checking whether current Endpoints
 * backend is running on App Engine.
 */
public class EnvUtil {
  public static final String ENV_APPENGINE_RUNTIME = "com.google.appengine.runtime.environment";
  public static final String ENV_APPENGINE_PROD = "Production";
  private static final String ORIGINAL_APPENGINE_RUNTIME_ENV =
      System.getProperty(EnvUtil.ENV_APPENGINE_RUNTIME);
  public static final String DEFAULT_VERSION_HOSTNAME =
      "com.google.appengine.runtime.default_version_hostname";

  /**
   * Resets ENV_APPENGINE_RUNTIME to its original state. Used for testing usually.
   */
  public static void recoverAppEngineRuntime() {
    if (ORIGINAL_APPENGINE_RUNTIME_ENV != null) {
      System.setProperty(EnvUtil.ENV_APPENGINE_RUNTIME, ORIGINAL_APPENGINE_RUNTIME_ENV);
    } else {
      System.clearProperty(EnvUtil.ENV_APPENGINE_RUNTIME);
    }
  }

  /**
   * Returns whether this is an Endpoints backend running on App Engine. It can be either running
   * on App Engine production, or local machine using App Engine dev app server.
   */
  public static boolean isRunningOnAppEngine() {
    String property = System.getProperty(ENV_APPENGINE_RUNTIME);
    return property != null && !property.isEmpty();
  }

  /**
   * Returns whether this is an Endpoints backend running on App Engine in production.
   * {@code false} if the backend is local using App Engine dev server or if not running on App
   * Engine at all (Tornado backend).
   */
  public static boolean isRunningOnAppEngineProd() {
    String property = System.getProperty(ENV_APPENGINE_RUNTIME);
    return property != null && property.equals(ENV_APPENGINE_PROD);
  }
}
