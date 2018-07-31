package com.google.api.server.spi.config.model;

import com.google.common.annotations.VisibleForTesting;

/**
 * These flags control the behavior of the schema generators regarding Map types.
 *
 * By default, schema generation uses "additionalProperties" in JsonSchema to describe Map types
 * (both for Discovery and OpenAPI), with a proper description of the value types.
 * This mode supports key types that can be serialized from / to String, and supports any value
 * type except array-like ones (see {@link MapSchemaFlag#SUPPORT_ARRAYS_VALUES} for more details).
 * In previous versions of Cloud Endpoints, Maps were always represented using the untyped "JsonMap"
 * object (see {@link com.google.api.server.spi.config.model.SchemaRepository#MAP_SCHEMA}).
 *
 * To enable one of these enum flags, you can either:
 * - Set system property "endpoints.mapSchema." + systemPropertyName to any value except a falsy one
 * - Set env variable "ENDPOINTS_MAP_SCHEMA_" + name() to any value except a falsy one
 *
 * Notes:
 * - System properties are evaluated before env variables.
 * - falsy is defined as a case-insensitive equality with "false".
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

  MapSchemaFlag(String systemPropertyName) {
    this.envVarName = ENV_VARIABLE_PREFIX + name();
    this.systemPropertyName = SYSTEM_PROPERTY_PREFIX + systemPropertyName;
  }

  public boolean isEnabled() {
    String envVar = System.getenv(envVarName);
    String systemProperty = System.getProperty(systemPropertyName);
    return systemProperty != null && !"false".equalsIgnoreCase(systemProperty)
        || envVar != null && !"false".equalsIgnoreCase(envVar);
  }

}
