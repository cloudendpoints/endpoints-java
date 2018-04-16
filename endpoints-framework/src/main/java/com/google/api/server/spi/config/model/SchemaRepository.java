package com.google.api.server.spi.config.model;

import com.google.api.client.util.Maps;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.Description;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.jsonwriter.JacksonResourceSchemaProvider;
import com.google.api.server.spi.config.jsonwriter.ResourceSchemaProvider;
import com.google.api.server.spi.config.model.Schema.Field;
import com.google.api.server.spi.config.model.Schema.SchemaReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A repository which creates and caches the compiled schemas for an API.
 */
public class SchemaRepository {
  private static final Schema PLACEHOLDER_SCHEMA = Schema.builder()
      .setName("_placeholder_")
      .setType("_placeholder_")
      .build();

  @VisibleForTesting
  static final Schema ANY_SCHEMA = Schema.builder()
      .setName("_any")
      .setType("any")
      .build();

  @VisibleForTesting
  static final Schema MAP_SCHEMA = Schema.builder()
      .setName("JsonMap")
      .setType("object")
      .build();

  private static final EnumSet<FieldType> SUPPORTED_MAP_KEY_TYPES = EnumSet.of(
          FieldType.STRING,
          FieldType.ENUM,
          FieldType.BOOLEAN,
          FieldType.INT8, FieldType.INT16, FieldType.INT32, FieldType.INT64,
          FieldType.FLOAT, FieldType.DOUBLE,
          FieldType.DATE, FieldType.DATE_TIME
  );

  @VisibleForTesting
  static final String ARRAY_UNUSED_MSG = "unused for array items";
  @VisibleForTesting
  static final String MAP_UNUSED_MSG = "unused for map values";

  /**
   * If enabled, will use "additionalProperties" in JsonSchema (both for Discovery and OpenAPI) to describe
   * Map types, with proper description of the value types.
   * This mode supports key types that can be serialized from / to String, and supports any value type.
   * However, API client generation does not support array-like values in Maps yet, so these type of Maps will still use
   * JsonMap as their schema by default to avoid generating invalid clients.
   * It is still possible to enable support for array-like values with the flag
   * {@link SchemaRepository#SUPPORT_ARRAY_VALUES_IN_MAP_FLAG}, for example when using the generated Discovery file in
   * the API explorer.
   */
  public static final String SUPPORT_GENERIC_MAP_TYPES_FLAG = "endpoints.supportGenericMapTypes";
  /**
   * If enabled, warns about unsupported key or value types in Maps when generating API description documents.
   * Can be enabled without "endpoints.supportGenericMapTypes" to analyze existing APIs.
   */
  public static final String WARN_ABOUT_UNSUPPORTED_MAP_TYPES_FLAG = "endpoints.warnAboutUnsupportedMapTypes";
  /**
   * Array values in Maps are supported by the API Explorer, but not by the client generation.
   * This flag should be enabled when deploying an API, but disabled when generating clients.
   */
  public static final String SUPPORT_ARRAY_VALUES_IN_MAP_FLAG = "endpoints.supportArrayValuesInMap";

  private final Multimap<ApiKey, Schema> schemaByApiKeys = LinkedHashMultimap.create();
  private final Map<ApiSerializationConfig, Map<TypeToken<?>, Schema>> types = Maps.newHashMap();
  private final ResourceSchemaProvider resourceSchemaProvider = new JacksonResourceSchemaProvider();

  private final TypeLoader typeLoader;

  public SchemaRepository(TypeLoader typeLoader) {
    this.typeLoader = typeLoader;
  }

  /**
   * Gets a schema for a type and API config.
   *
   * @return a {@link Schema} if one has been created, or null otherwise.
   */
  public Schema get(TypeToken<?> type, ApiConfig config) {
    Map<TypeToken<?>, Schema> typesForConfig = getAllTypesForConfig(config);
    type = ApiAnnotationIntrospector.getSchemaType(type, config);
    Schema schema = typesForConfig.get(type);
    if (schema != null) {
      if (schema == PLACEHOLDER_SCHEMA) {
        throw new IllegalStateException("schema repository is in a bad state!");
      }
      return schema;
    }
    return null;
  }

  /**
   * Gets a schema for a type and API config, creating it if it doesn't already exist.
   *
   * @return a {@link Schema} for the requested type and API config.
   */
  public Schema getOrAdd(TypeToken<?> type, ApiConfig config) {
    Map<TypeToken<?>, Schema> typesForConfig = getAllTypesForConfig(config);
    Schema schema = getOrCreateTypeForConfig(type, typesForConfig, config);
    if (schema == PLACEHOLDER_SCHEMA) {
      throw new IllegalStateException("schema repository is in a bad state!");
    }
    return schema;
  }

  /**
   * Gets all schema for an API key.
   */
  public List<Schema> getAllSchemaForApi(ApiKey apiKey) {
    return ImmutableList.copyOf(schemaByApiKeys.get(apiKey.withoutRoot()));
  }

  /**
   * Gets all schema for an API config.
   *
   * @return a {@link Map} from {@link TypeToken} to {@link Schema}. If there are no schema for
   *     this config, an empty map is returned.
   */
  private Map<TypeToken<?>, Schema> getAllTypesForConfig(ApiConfig config) {
    Map<TypeToken<?>, Schema> typesForConfig = types.get(config.getSerializationConfig());
    if (typesForConfig == null) {
      typesForConfig = Maps.newHashMap();
      types.put(config.getSerializationConfig(), typesForConfig);
    }
    return typesForConfig;
  }

  private Schema getOrCreateTypeForConfig(
      TypeToken type, Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    type = ApiAnnotationIntrospector.getSchemaType(type, config);
    Schema schema = typesForConfig.get(type);
    ApiKey key = config.getApiKey().withoutRoot();
    if (schema != null) {
      // If the schema is a placeholder, it's currently being constructed and will be added when
      // the type construction is complete.
      if (schema != PLACEHOLDER_SCHEMA) {
        addSchemaToApi(key, schema);
      }
      return schema;
    }
    // We put a placeholder in because this is a recursive process that may result in circular
    // references. This should never be returned in the public interface.
    typesForConfig.put(type, PLACEHOLDER_SCHEMA);
    TypeToken<?> arrayItemType = Types.getArrayItemType(type);
    if (typeLoader.isSchemaType(type)) {
      throw new IllegalArgumentException("Can't add a primitive type as a resource");
    } else if (arrayItemType != null) {
      Field.Builder arrayItemSchema = Field.builder().setName(ARRAY_UNUSED_MSG);
      fillInFieldInformation(arrayItemSchema, arrayItemType, null, typesForConfig, config);
      schema = Schema.builder()
          .setName(Types.getSimpleName(type, config.getSerializationConfig()))
          .setType("object")
          .addField("items", Field.builder()
              .setName("items")
              .setType(FieldType.ARRAY)
              .setArrayItemSchema(arrayItemSchema.build())
              .build())
          .build();
      typesForConfig.put(type, schema);
      schemaByApiKeys.put(key, schema);
      return schema;
    } else if (Types.isObject(type)) {
      typesForConfig.put(type, ANY_SCHEMA);
      schemaByApiKeys.put(key, ANY_SCHEMA);
      return ANY_SCHEMA;
    } else if (Types.isMapType(type)) {
      schema = MAP_SCHEMA;
      boolean isGenericMapType = type.getType() instanceof ParameterizedType;
      boolean supportsGenericMapTypes = isFlagEnabled(SUPPORT_GENERIC_MAP_TYPES_FLAG);
      boolean warnOnly = isFlagEnabled(WARN_ABOUT_UNSUPPORTED_MAP_TYPES_FLAG);
      if (isGenericMapType && (supportsGenericMapTypes || warnOnly)) {
        schema = createMapSchema(type, typesForConfig, config, warnOnly).or(schema);
      }
      typesForConfig.put(type, schema);
      schemaByApiKeys.put(key, schema);
      return schema;
    } else if (Types.isEnumType(type)) {
      Schema.Builder builder = Schema.builder()
          .setName(Types.getSimpleName(type, config.getSerializationConfig()))
          .setType("string");
      for (java.lang.reflect.Field field : type.getRawType().getFields()) {
        if (field.isEnumConstant()) {
          builder.addEnumValue(field.getName());
          Description description = field.getAnnotation(Description.class);
          builder.addEnumDescription(description == null ? "" : description.value());
        }
      }
      schema = builder.build();
      typesForConfig.put(type, schema);
      schemaByApiKeys.put(key, schema);
      return schema;
    } else {
      schema = createBeanSchema(type, typesForConfig, config);
      typesForConfig.put(type, schema);
      schemaByApiKeys.put(key, schema);
      return schema;
    }
  }

  //TODO is this the right way to enable flags?
  private boolean isFlagEnabled(String flag) {
    return Boolean.parseBoolean(System.getenv(flag)) || Boolean.parseBoolean(System.getProperty(flag));
  }

  private void addSchemaToApi(ApiKey key, Schema schema) {
    if (schemaByApiKeys.containsEntry(key, schema)) {
      return;
    }
    schemaByApiKeys.put(key, schema);
    for (Field f : schema.fields().values()) {
      while (f.type() == FieldType.ARRAY) {
        f = f.arrayItemSchema();
      }
      if (f.type() == FieldType.OBJECT || f.type() == FieldType.ENUM) {
        addSchemaToApi(key, f.schemaReference().get());
      }
    }
    Field mapValueSchema = schema.mapValueSchema();
    if (mapValueSchema != null && mapValueSchema.schemaReference() != null)  {
      addSchemaToApi(key, mapValueSchema.schemaReference().get());
    }
  }

  private Optional<Schema> createMapSchema(TypeToken type, Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config,
                                           boolean warnOnly) {
    FieldType keyFieldType = FieldType.fromType(Types.getTypeParameter(type, 0));
    boolean supportedKeyType = checkMapType(SUPPORTED_MAP_KEY_TYPES.contains(keyFieldType),
            "Type " + type + " not supported because key type is not serializable to String");
    //TODO should it throw an exception if supportedKeyType is false? It will probably generate one at runtime.
    TypeToken<?> valueTypeToken = Types.getTypeParameter(type, 1);
    FieldType valueFieldType = FieldType.fromType(valueTypeToken);
    boolean supportsArrayValueTypes = isFlagEnabled(SUPPORT_ARRAY_VALUES_IN_MAP_FLAG);
    boolean supportedValueType = checkMapType(supportsArrayValueTypes || valueFieldType != FieldType.ARRAY,
            "Type " + type + " not supported because value type is array-like");
    if (!supportedKeyType || !supportedValueType || warnOnly) {
      return Optional.absent();
    }
    TypeToken<?> valueSchemaType = ApiAnnotationIntrospector.getSchemaType(valueTypeToken, config);
    Schema.Builder builder = Schema.builder()
            .setName(Types.getSimpleName(type, config.getSerializationConfig()))
            .setType("object");
    Field.Builder fieldBuilder = Field.builder().setName(MAP_UNUSED_MSG);
    fillInFieldInformation(fieldBuilder, valueSchemaType, null, typesForConfig, config);
    return Optional.of(builder.setMapValueSchema(fieldBuilder.build()).build());
  }

  private boolean checkMapType(boolean precondition, String message) {
    if (isFlagEnabled(WARN_ABOUT_UNSUPPORTED_MAP_TYPES_FLAG) && !precondition) {
      System.err.println(message + ", will use JsonMap");
    }
    return precondition;
  }

  private Schema createBeanSchema(
      TypeToken<?> type, Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    Schema.Builder builder = Schema.builder()
        .setName(Types.getSimpleName(type, config.getSerializationConfig()))
        .setType("object");
    ResourceSchema schema = resourceSchemaProvider.getResourceSchema(type, config);
    for (Entry<String, ResourcePropertySchema> entry : schema.getProperties().entrySet()) {
      String propertyName = entry.getKey();
      ResourcePropertySchema propertySchema = entry.getValue();
      TypeToken<?> propertyType = propertySchema.getType();
      if (propertyType != null) {
        Field.Builder fieldBuilder = Field.builder().setName(propertyName);
        fillInFieldInformation(fieldBuilder, propertyType, propertySchema.getDescription(), typesForConfig, config);
        builder.addField(propertyName, fieldBuilder.build());
      }
    }
    return builder.build();
  }

  private void fillInFieldInformation(Field.Builder builder, TypeToken<?> fieldType,
      String description, Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    FieldType ft = FieldType.fromType(fieldType);
    builder.setType(ft);
    builder.setDescription(description);
    if (ft == FieldType.OBJECT || ft == FieldType.ENUM) {
      getOrCreateTypeForConfig(fieldType, typesForConfig, config);
      builder.setSchemaReference(SchemaReference.create(this, config, fieldType));
    } else if (ft == FieldType.ARRAY) {
      Field.Builder arrayItemBuilder = Field.builder().setName(ARRAY_UNUSED_MSG);
      fillInFieldInformation(
          arrayItemBuilder,
          ApiAnnotationIntrospector.getSchemaType(Types.getArrayItemType(fieldType), config),
          null,
          typesForConfig,
          config);
      builder.setArrayItemSchema(arrayItemBuilder.build());
    }
  }
}
