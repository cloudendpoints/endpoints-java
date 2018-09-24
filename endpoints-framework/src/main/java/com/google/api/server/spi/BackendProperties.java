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

import com.google.common.annotations.VisibleForTesting;

import com.google.common.flogger.FluentLogger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Common handler for general configuration information about this backend.  These properties
 * generally come from environment variables.
 */
public class BackendProperties {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String APP_ID_PROPERTY = "com.google.appengine.application.id";
  static final String PROJECT_NUMBER_PROPERTY = "GOOGLE_PROJECT_NUMBER";
  static final String GCLOUD_PROJECT_PROPERTY = "GCLOUD_PROJECT";
  static final long PROJECT_NUMBER_UNKNOWN = 0L;

  static final String PROJECT_ID_PROPERTY = "GOOGLE_PROJECT_ID";

  // Inject an override to change env values read for tests.
  @VisibleForTesting
  static class EnvReader {
    public String getenv(String name) {
      return System.getenv(name);
    }
  }

  private boolean isOnAppEngine;
  private EnvReader envReader;

  public BackendProperties() {
    this(EnvUtil.isRunningOnAppEngine(), new EnvReader());
  }

  @VisibleForTesting
  BackendProperties(boolean isAppEngine, EnvReader envReader) {
    this.isOnAppEngine = isAppEngine;
    this.envReader = envReader;
  }

  public boolean isOnAppEngine() {
    return isOnAppEngine;
  }

  public long getProjectNumber() {
    if (isOnAppEngine()) {
      // This information is not currently available from App Engine backends.
      return PROJECT_NUMBER_UNKNOWN;
    } else if (envReader.getenv(PROJECT_NUMBER_PROPERTY) != null) {
      String property = envReader.getenv(PROJECT_NUMBER_PROPERTY);
      try {
        return Long.parseLong(property);
      } catch (NumberFormatException e) {
        logger.atWarning().log("Project number (%s) is not an int64.", property);
        return PROJECT_NUMBER_UNKNOWN;
      }
    } else {
      return PROJECT_NUMBER_UNKNOWN;
    }
  }

  /**
   * @return {@code null} if the project ID is not known.
   */
  @Nullable
  public String getProjectId() {
    if (isOnAppEngine()) {
      // This information is not currently available from App Engine backends.
      return null;
    } else {
      return envReader.getenv(PROJECT_ID_PROPERTY);
    }
  }

  /**
   * @return The App Engine application ID or {@code null} if not App Engine.
   */
  @Nullable
  public String getApplicationId() {
    if (isOnAppEngine()) {
      String appId = System.getProperty(APP_ID_PROPERTY);
      if (appId == null) {
        appId = envReader.getenv(GCLOUD_PROJECT_PROPERTY);
      }
      return appId;
    } else {
      return null;
    }
  }
}
