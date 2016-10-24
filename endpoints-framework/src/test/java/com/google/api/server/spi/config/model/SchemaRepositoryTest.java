package com.google.api.server.spi.config.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiConfigLoader;
import com.google.api.server.spi.config.Transformer;
import com.google.api.server.spi.config.annotationreader.ApiConfigAnnotationReader;
import com.google.api.server.spi.config.model.ApiParameterConfig.Classification;
import com.google.api.server.spi.config.model.Schema.Field;
import com.google.api.server.spi.config.model.Schema.SchemaReference;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.testing.TestEnum;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Tests for {@link SchemaRepository}.
 */
public class SchemaRepositoryTest {
  private SchemaRepository repo;
  private ApiConfigLoader configLoader;
  private ApiConfig config;

  @Before
  public void setUp() throws Exception {
    TypeLoader typeLoader = new TypeLoader(getClass().getClassLoader());
    ApiConfigAnnotationReader annotationReader =
        new ApiConfigAnnotationReader(typeLoader.getAnnotationTypes());
    this.repo = new SchemaRepository(typeLoader);
    this.configLoader = new ApiConfigLoader(new ApiConfig.Factory(), typeLoader,
        annotationReader);
    this.config = configLoader.loadConfiguration(ServiceContext.create(), FooEndpoint.class);
  }

  @Test
  public void getOrAdd_genericRequestType() throws Exception {
    ApiMethodConfig methodConfig = fooEndpointSetParameterized();
    checkParameterizedSchema(
        repo.getOrAdd(getRequestResource(methodConfig).getType(), config), Integer.class);
  }

  @Test
  public void getOrAdd_genericReturnType() throws Exception {
    ApiMethodConfig methodConfig = fooEndpointSetParameterized();
    checkParameterizedSchema(
        repo.getOrAdd(methodConfig.getReturnType(), config), Integer.class);
  }

  @Test
  public void getOrAdd_responseCollection() throws Exception {
    ApiMethodConfig methodConfig = getMethodConfig("getIntegerCollection");
    checkIntegerCollectionResponse(repo.getOrAdd(methodConfig.getReturnType(), config));
  }

  @Test
  public void getOrAdd_intArray() throws Exception {
    ApiMethodConfig methodConfig = getMethodConfig("getPrimitiveIntegerArray");
    checkIntegerCollection(repo.getOrAdd(methodConfig.getReturnType(), config));
  }

  @Test
  public void getOrAdd_any() throws Exception {
    ApiMethodConfig methodConfig = getMethodConfig("getAny");
    assertThat(repo.getOrAdd(methodConfig.getReturnType(), config))
        .isEqualTo(SchemaRepository.ANY_SCHEMA);
  }

  @Test
  public void getOrAdd_jsonMap() throws Exception {
    ApiMethodConfig methodConfig = getMethodConfig("getJsonMap");
    assertThat(repo.getOrAdd(methodConfig.getReturnType(), config))
        .isEqualTo(SchemaRepository.MAP_SCHEMA);
  }

  @Test
  public void getOrAdd_transformer() throws Exception {
    ApiMethodConfig methodConfig = getMethodConfig("getTransformed");
    checkParameterizedSchema(
        repo.getOrAdd(methodConfig.getReturnType(), config), String.class);
  }

  @Test
  public void getOrAdd_primitiveReturn() throws Exception {
    try {
      repo.getOrAdd(TypeToken.of(int.class), config);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  @Test
  public void getOrAdd_enum() throws Exception {
    assertThat(repo.getOrAdd(TypeToken.of(TestEnum.class), config))
        .isEqualTo(Schema.builder()
            .setName("TestEnum")
            .setType("string")
            .addEnumValue("VALUE1")
            .addEnumValue("VALUE2")
            .addEnumDescription("")
            .addEnumDescription("")
            .build());
  }

  @Test
  public void get() {
    TypeToken<Parameterized<Integer>> type = new TypeToken<Parameterized<Integer>>() {};
    assertThat(repo.get(type, config)).isNull();
    repo.getOrAdd(type, config);
    checkParameterizedSchema(repo.get(type, config), Integer.class);
  }

  @Api(transformers = {ParameterizedShortTransformer.class})
  private static class FooEndpoint {
    public Parameterized<Integer> setParameterized(Parameterized<Integer> p) {
      return null;
    }

    public CollectionResponse<Integer> getIntegerCollection() {
      return null;
    }

    public int[] getPrimitiveIntegerArray() {
      return null;
    }

    public Object getAny() {
      return null;
    }

    public Map<String, Object> getJsonMap() {
      return null;
    }

    public Parameterized<Short> getTransformed() {
      return null;
    }
  }

  private static class Parameterized<T> {
    public T getFoo() {
      return null;
    }

    public void setFoo(T foo) { }

    public Parameterized<T> getNext() {
      return null;
    }

    public TestEnum getTestEnum() {
      return null;
    }
  }

  private static class ParameterizedShortTransformer implements
      Transformer<Parameterized<Short>, Parameterized<String>> {
    @Override
    public Parameterized<String> transformTo(Parameterized<Short> in) {
      return null;
    }

    @Override
    public Parameterized<Short> transformFrom(Parameterized<String> in) {
      return null;
    }
  }

  private ApiMethodConfig fooEndpointSetParameterized() throws Exception {
    return getMethodConfig("setParameterized", Parameterized.class);
  }

  private ApiMethodConfig getMethodConfig(String name, Class<?>... params) throws Exception {
    return config.getApiClassConfig().getMethods().get(
        EndpointMethod.create(FooEndpoint.class, FooEndpoint.class.getMethod(name, params)));
  }

  private static ApiParameterConfig getRequestResource(ApiMethodConfig methodConfig) {
    for (ApiParameterConfig parameterConfig : methodConfig.getParameterConfigs()) {
      if (parameterConfig.getClassification() == Classification.RESOURCE) {
        return parameterConfig;
      }
    }
    throw new IllegalStateException("no resource on method");
  }

  private <T> void checkParameterizedSchema(Schema schema, Class<T> type) {
    assertThat(schema).isEqualTo(Schema.builder()
        .setName("Parameterized_" + type.getSimpleName())
        .setType("object")
        .addField("foo", Field.builder()
            .setName("foo")
            .setType(FieldType.fromType(TypeToken.of(type)))
            .build())
        .addField("next", Field.builder()
            .setName("next")
            .setType(FieldType.OBJECT)
            .setSchemaReference(
                SchemaReference.create(repo, config, new TypeToken<Parameterized<T>>() {}
                    .where(new TypeParameter<T>() {}, TypeToken.of(type))))
            .build())
        .addField("testEnum", Field.builder()
            .setName("testEnum")
            .setType(FieldType.ENUM)
            .setSchemaReference(SchemaReference.create(repo, config, TypeToken.of(TestEnum.class)))
            .build())
        .build());
  }

  private static void checkIntegerCollectionResponse(Schema schema) {
    assertThat(schema).isEqualTo(Schema.builder()
        .setName("CollectionResponse_Integer")
        .setType("object")
        .addField("items", Field.builder()
            .setName("items")
            .setType(FieldType.ARRAY)
            .setArrayItemSchema(Field.builder()
                .setName("unused for array items")
                .setType(FieldType.INT32)
                .build())
            .build())
        .addField("nextPageToken", Field.builder()
            .setName("nextPageToken")
            .setType(FieldType.STRING)
            .build())
        .build());
  }

  private static void checkIntegerCollection(Schema schema) {
    assertThat(schema).isEqualTo(Schema.builder()
        .setName("IntegerCollection")
        .setType("object")
        .addField("items", Field.builder()
            .setName("items")
            .setType(FieldType.ARRAY)
            .setArrayItemSchema(Field.builder()
                .setName(SchemaRepository.ARRAY_UNUSED_MSG)
                .setType(FieldType.INT32)
                .build())
            .build())
        .build());
  }
}