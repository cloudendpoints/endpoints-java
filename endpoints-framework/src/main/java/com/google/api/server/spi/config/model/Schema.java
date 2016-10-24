package com.google.api.server.spi.config.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.reflect.TypeToken;

import javax.annotation.Nullable;

/**
 * A schema representation, which is used to store JSON type information for a Java type after all
 * transformations have been applied.
 */
@AutoValue
public abstract class Schema {
  /** The name of the schema. */
  public abstract String name();
  public abstract String type();

  /** A map from field names to fields for the schema. */
  public abstract ImmutableSortedMap<String, Field> fields();

  /**
   * If the schema is an enum, a list of possible enum values in their string representation.
   */
  @Nullable public abstract ImmutableList<String> enumValues();

  /**
   * If the schema is an enum, a list of enum value descriptions.
   */
  @Nullable public abstract ImmutableList<String> enumDescriptions();

  public static Builder builder() {
    return new AutoValue_Schema.Builder();
  }

  /**
   * A {@link Schema} builder.
   */
  @AutoValue.Builder
  public abstract static class Builder {
    private final ImmutableSortedMap.Builder<String, Field> fieldsBuilder =
        ImmutableSortedMap.naturalOrder();

    public abstract Builder setName(String name);
    public abstract Builder setType(String type);
    public abstract Builder setFields(ImmutableSortedMap<String, Field> fields);
    public Builder addField(String name, Field field) {
      fieldsBuilder.put(name, field);
      return this;
    }
    abstract ImmutableList.Builder<String> enumValuesBuilder();
    public Builder addEnumValue(String value) {
      enumValuesBuilder().add(value);
      return this;
    }
    abstract ImmutableList.Builder<String> enumDescriptionsBuilder();
    public Builder addEnumDescription(String value) {
      enumDescriptionsBuilder().add(value);
      return this;
    }
    abstract Schema autoBuild();
    public Schema build() {
      return setFields(fieldsBuilder.build()).autoBuild();
    }
  }

  /**
   * Representation of a field in a JSON object.
   */
  @AutoValue
  public static abstract class Field {
    /** The name of the field. */
    public abstract String name();

    /** The type classification of the field. */
    public abstract FieldType type();

    /**
     * If {@link #type()} is {@link FieldType#OBJECT}, a reference to the schema type that the field
     * refers to.
     */
    @Nullable public abstract SchemaReference schemaReference();

    /**
     * If {@link type()} is {@link FieldType#ARRAY}, a reference to the array item type.
     */
    @Nullable public abstract Field arrayItemSchema();
    public static Builder builder() {
      return new AutoValue_Schema_Field.Builder();
    }

    /**
     * A {@link Field} builder.
     */
    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setName(String name);
      public abstract Builder setType(FieldType type);
      public abstract Builder setSchemaReference(SchemaReference ref);
      public abstract Builder setArrayItemSchema(Field schema);
      public abstract Field build();
    }
  }

  /**
   * A lazy reference to another {@link Schema} within a {@link SchemaRepository}. Because of the
   * way types are constructed, some schema references shouldn't be resolved until needed. This
   * class is a simple utility for storing the reference.
   */
  @AutoValue
  public abstract static class SchemaReference {
    public abstract SchemaRepository repository();
    public abstract ApiConfig apiConfig();
    public abstract TypeToken<?> type();

    public Schema get() {
      return repository().get(type(), apiConfig());
    }

    public static SchemaReference create(
        SchemaRepository repository, ApiConfig apiConfig, TypeToken type) {
      return new AutoValue_Schema_SchemaReference(repository, apiConfig, type);
    }
  }
}
