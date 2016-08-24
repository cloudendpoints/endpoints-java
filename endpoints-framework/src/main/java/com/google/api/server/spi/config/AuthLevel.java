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
package com.google.api.server.spi.config;

/**
 * Enum to choose frontend authentication level for use in endpoints annotations. Frontend
 * authentication is handled by a Google API server prior to the request reaching backends. An early
 * return before hitting the backend can happen if the request does not fulfil the requirement
 * specified by the AuthLevel.
 */
public enum AuthLevel {
  /**
   * Valid authentication credentials are required.
   */
  REQUIRED,

  /**
   * Authentication is optional. If authentication credentials are supplied they must be valid.
   */
  OPTIONAL,

  /**
   * Authentication is optional and will be attempted if authentication credential are supplied. The
   * request can still reach backend if authentication failed.
   */
  OPTIONAL_CONTINUE,

  /**
   * Frontend authentication will be skipped. Existing authentication in cloud endpoints still
   * applies.
   */
  NONE,

  /**
   * Unspecified; AuthLevel is system default if there is any.
   */
  UNSPECIFIED;
}
