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
 * These flags control various Endpoints behavior.<br>
 * <br>
 * To enable one of these enum flags, you can either:
 * <ul>
 * <li>Set system property {@link EndpointsFlag#systemPropertyName} (defined as
 * "endpoints." + systemPropertySuffix) to any value except a false-y one</li>
 * <li>Set env variable {@link EndpointsFlag#envVarName} (defined as "ENDPOINTS_"
 * + name()) to any value except a falsy one</li>
 * </ul>
 * <br>
 * Notes:
 * <ul>
 * <li>System properties are evaluated before env variables.</li>
 * <li>falsy is defined as a case-insensitive equality with "false".</li>
 * </ul>
 */
public enum EndpointsFlag {

  /**
   * Reenabled the previous behavior of Cloud Endpoints, using untyped "JsonMap" for all Map types.
   * By default, schema generation uses "additionalProperties" in JsonSchema to describe Map types
   * (both for Discovery and OpenAPI), with a proper description of the value types.
   */
  MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA("mapSchema.forceJsonMapSchema"),

  /**
   * When enabled, schema generation will not throw an error when handling Map types with keys that
   * are not serializable from / to string (previous Cloud Endpoints behavior). It will still
   * probably generate an error when serializing / deserializing these types at runtime. {@link
   * #MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA} must be enabled for this to take effect.
   */
  MAP_SCHEMA_IGNORE_UNSUPPORTED_KEY_TYPES("mapSchema.ignoreUnsupportedKeyTypes"),

  /**
   * Array values in "additionalProperties" are supported by the API Explorer, but not by the Java
   * client generation. This flag can be enabled when deploying an API to the server, but should
   * always be disabled when generating Java clients. {@link #MAP_SCHEMA_FORCE_JSON_MAP_SCHEMA} must
   * be enabled for this to take effect.
   */
  MAP_SCHEMA_SUPPORT_ARRAYS_VALUES("mapSchema.supportArrayValues"),

  /**
   * When enabled, allows use of Jackson serialization annotations. Previously, the Jackson
   * annotation introspector was unused because Jackson was a vendored dependency. Now that Jackson
   * is an explicit dependency, this can cause conflict with apps that use Jackson annotations for
   * reasons outside of using this framework.
   */
  JSON_DISABLE_JACKSON_ANNOTATIONS("json.disableJacksonAnnotations");

  private static final String ENV_VARIABLE_PREFIX = "ENDPOINTS_";
  private static final String SYSTEM_PROPERTY_PREFIX = "endpoints.";

  @VisibleForTesting
  public String envVarName;
  @VisibleForTesting
  public String systemPropertyName;

  EndpointsFlag(String systemPropertySuffix) {
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
