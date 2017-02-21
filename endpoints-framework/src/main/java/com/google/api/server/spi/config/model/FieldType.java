package com.google.api.server.spi.config.model;

import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Classifier and metadata for field types.
 */
public enum FieldType {
  STRING("string", null, "String"),
  INT8("integer", null, "Byte"),
  INT16("integer", null, "Short"),
  INT32("integer", "int32", "Integer"),
  INT64("string", "int64", "Long"),
  FLOAT("number", "float", "Float"),
  DOUBLE("number", "double", "Double"),
  DATE("string", "date", "Date"),
  DATE_TIME("string", "date-time", "DateTime"),
  BOOLEAN("boolean", null, "Boolean"),
  BYTE_STRING("string", "byte", "Bytes"),
  ENUM("string", null, null),
  OBJECT("object", null, null),
  ARRAY("array", null, null);

  private final String discoveryType;
  private final String discoveryFormat;
  private final String collectionName;

  FieldType(String discoveryType, String discoveryFormat, String collectionName) {
    this.discoveryType = discoveryType;
    this.discoveryFormat = discoveryFormat;
    this.collectionName = collectionName;
  }

  public String getDiscoveryType() {
    return discoveryType;
  }

  public String getDiscoveryFormat() {
    return discoveryFormat;
  }

  public String getCollectionName() {
    return collectionName;
  }

  private static final ImmutableMap<Type, FieldType> TYPE_MAP;

  static {
    ImmutableMap.Builder<Type, FieldType> builder = ImmutableMap.<Type, FieldType>builder()
        .put(String.class, STRING)
        .put(Short.class, INT16)
        .put(Short.TYPE, INT16)
        .put(Byte.class, INT8)
        .put(Byte.TYPE, INT8)
        .put(Character.class, STRING)
        .put(Character.TYPE, STRING)
        .put(Integer.class, INT32)
        .put(Integer.TYPE, INT32)
        .put(Long.class, INT64)
        .put(Long.TYPE, INT64)
        .put(Float.class, FLOAT)
        .put(Float.TYPE, FLOAT)
        .put(Double.class, DOUBLE)
        .put(Double.TYPE, DOUBLE)
        .put(Boolean.class, BOOLEAN)
        .put(Boolean.TYPE, BOOLEAN)
        .put(Date.class, DATE_TIME)
        .put(DateAndTime.class, DATE_TIME)
        .put(SimpleDate.class, DATE)
        .put(byte[].class, BYTE_STRING);
    try {
      builder.put(
          FieldType.class.getClassLoader().loadClass("com.google.appengine.api.datastore.Blob"),
          BYTE_STRING);
    } catch (ClassNotFoundException e) {
      // Do nothing; if we're on Flex, this class won't exist.
    }
    TYPE_MAP = builder.build();
  }

  public static FieldType fromType(TypeToken<?> type) {
    FieldType ft = TYPE_MAP.get(type.getRawType());
    if (ft != null) {
      return ft;
    } else if (Types.getArrayItemType(type) != null) {
      return ARRAY;
    } else if (Types.isEnumType(type)) {
      return ENUM;
    }
    return OBJECT;
  }
}
