package com.google.api.server.spi.config.model;

import com.google.api.client.util.Maps;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.api.server.spi.config.annotationreader.ApiAnnotationIntrospector;
import com.google.api.server.spi.config.jsonwriter.JacksonResourceSchemaProvider;
import com.google.api.server.spi.config.jsonwriter.ResourceSchemaProvider;
import com.google.api.server.spi.config.model.Schema.Field;
import com.google.api.server.spi.config.model.Schema.SchemaReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;

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

  @VisibleForTesting
  static final String ARRAY_UNUSED_MSG = "unused for array items";

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
        schemaByApiKeys.put(key, schema);
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
      fillInFieldInformation(arrayItemSchema, arrayItemType, typesForConfig, config);
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
      typesForConfig.put(type, MAP_SCHEMA);
      schemaByApiKeys.put(key, MAP_SCHEMA);
      return MAP_SCHEMA;
    } else if (Types.isEnumType(type)) {
      Schema.Builder builder = Schema.builder()
          .setName(Types.getSimpleName(type, config.getSerializationConfig()))
          .setType("string");
      for (Object enumConstant : type.getRawType().getEnumConstants()) {
        builder.addEnumValue(enumConstant.toString());
        builder.addEnumDescription("");
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

  private Schema createBeanSchema(
      TypeToken<?> type, Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    Schema.Builder builder = Schema.builder()
        .setName(Types.getSimpleName(type, config.getSerializationConfig()))
        .setType("object");
    ResourceSchema schema = resourceSchemaProvider.getResourceSchema(type, config);
    for (Entry<String, ResourcePropertySchema> entry : schema.getProperties().entrySet()) {
      String propertyName = entry.getKey();
      TypeToken<?> propertyType = entry.getValue().getType();
      if (propertyType != null) {
        Field.Builder fieldBuilder = Field.builder().setName(propertyName);
        fillInFieldInformation(fieldBuilder, propertyType, typesForConfig, config);
        builder.addField(propertyName, fieldBuilder.build());
      }
    }
    return builder.build();
  }

  private void fillInFieldInformation(Field.Builder builder, TypeToken<?> fieldType,
      Map<TypeToken<?>, Schema> typesForConfig, ApiConfig config) {
    FieldType ft = FieldType.fromType(fieldType);
    builder.setType(ft);
    if (ft == FieldType.OBJECT || ft == FieldType.ENUM) {
      getOrCreateTypeForConfig(fieldType, typesForConfig, config);
      builder.setSchemaReference(SchemaReference.create(this, config, fieldType));
    } else if (ft == FieldType.ARRAY) {
      Field.Builder arrayItemBuilder = Field.builder().setName(ARRAY_UNUSED_MSG);
      fillInFieldInformation(arrayItemBuilder, Types.getArrayItemType(fieldType),
          typesForConfig, config);
      builder.setArrayItemSchema(arrayItemBuilder.build());
    }
  }
}
