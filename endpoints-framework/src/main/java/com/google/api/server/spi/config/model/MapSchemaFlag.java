/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

import com.google.common.annotations.VisibleForTesting;

/**
 * These flags control the behavior of the schema generators regarding Map types.<br>
 * <br>
 * By default, schema generation uses "additionalProperties" in JsonSchema to describe Map types
 * (both for Discovery and OpenAPI), with a proper description of the value types.<br> This mode
 * supports key types that can be serialized from / to String, and supports any value type except
 * array-like ones (see {@link MapSchemaFlag#SUPPORT_ARRAYS_VALUES} for more details).<br> In
 * previous versions of Cloud Endpoints, Maps were always represented using the untyped "JsonMap"
 * object (see {@link com.google.api.server.spi.config.model.SchemaRepository#MAP_SCHEMA}).<br>
 * <br>
 * To enable one of these enum flags, you can either:
 * <ul>
 * <li>Set system property {@link MapSchemaFlag#systemPropertyName} (defined as
 * "endpoints.mapSchema." + systemPropertySuffix) to any value except a falsy one</li>
 * <li>Set env variable {@link MapSchemaFlag#envVarName} (defined as "ENDPOINTS_MAP_SCHEMA_"
 * + name()) to any value except a falsy one</li>
 * </ul>
 * <br>
 * Notes:
 * <ul>
 * <li>System properties are evaluated before env variables.</li>
 * <li>falsy is defined as a case-insensitive equality with "false".</li>
 * </ul>
 */
public enum MapSchemaFlag {

  /**
   * Reenabled the previous behavior of Cloud Endpoints, using untyped "JsonMap" for all Map types.
   */
  FORCE_JSON_MAP_SCHEMA("forceJsonMapSchema"),

  /**
   * When enabled, schema generation will not throw an error when handling Map types with keys that
   * are not serializable from / to string (previous Cloud Endpoints behavior). It will still
   * probably generate an error when serializing / deserializing these types at runtime.
   */
  IGNORE_UNSUPPORTED_KEY_TYPES("ignoreUnsupportedKeyTypes"),

  /**
   * Array values in "additionalProperties" are supported by the API Explorer, but not by the Java
   * client generation. This flag can be enabled when deploying an API to the server, but should
   * always be disabled when generating Java clients.
   */
  SUPPORT_ARRAYS_VALUES("supportArrayValues");

  private static final String ENV_VARIABLE_PREFIX = "ENDPOINTS_MAP_SCHEMA_";
  private static final String SYSTEM_PROPERTY_PREFIX = "endpoints.mapSchema.";

  @VisibleForTesting
  public String envVarName;
  @VisibleForTesting
  public String systemPropertyName;

  MapSchemaFlag(String systemPropertySuffix) {
    this.envVarName = ENV_VARIABLE_PREFIX + name();
    this.systemPropertyName = SYSTEM_PROPERTY_PREFIX + systemPropertySuffix;
  }

  public boolean isEnabled() {
    String envVar = System.getenv(envVarName);
    String systemProperty = System.getProperty(systemPropertyName);
    return systemProperty != null && !"false".equalsIgnoreCase(systemProperty)
        || envVar != null && !"false".equalsIgnoreCase(envVar);
  }

}
