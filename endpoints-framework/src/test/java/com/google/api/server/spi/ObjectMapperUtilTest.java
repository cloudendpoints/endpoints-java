/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.config.model.EndpointsFlag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Tests for {@link ObjectMapperUtil}
 */
public class ObjectMapperUtilTest {
  
  @Test
  public void createStandardObjectMapper_base64Variant() throws Exception {
    byte[] bytes = new byte[] {(byte) 0xff, (byte) 0xef};
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    assertThat(mapper.writeValueAsString(bytes)).isEqualTo("\"_-8\"");
    assertThat(mapper.readValue("\"_-8\"", byte[].class)).isEqualTo(bytes);
  }

  @Test
  public void createStandardObjectMapper_useJacksonAnnotations() throws Exception {
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    assertThat(mapper.writeValueAsString(new TestObject())).contains("test");
  }

  @Test
  public void createStandardObjectMapper_disableJacksonAnnotations() throws Exception {
    System.setProperty(EndpointsFlag.JSON_USE_JACKSON_ANNOTATIONS.systemPropertyName, "false");
    try {
      ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
      assertThat(mapper.writeValueAsString(new TestObject())).contains("TEST");
    } finally {
      System.clearProperty(EndpointsFlag.JSON_USE_JACKSON_ANNOTATIONS.systemPropertyName);
    }
  }

  @Test
  public void createStandardObjectMapper_deserialize_optional() throws JsonProcessingException {
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    ObjectReader reader = mapper.readerFor(TestOptionals.class);
    assertThat(reader.<TestOptionals>readValue("{\"optionalString\":\"value\"}").getOptionalString())
        .isEqualTo(Optional.of("value"));
    assertThat(reader.<TestOptionals>readValue("{\"optionalString\":\"\"}").getOptionalString())
        .isEqualTo(Optional.of(""));
    assertThat(reader.<TestOptionals>readValue("{\"optionalString\":null}").getOptionalString())
        .isEqualTo(Optional.empty());
    assertThat(reader.<TestOptionals>readValue("{}").getOptionalString())
        .isEqualTo(null);
    assertThat(reader.<TestOptionals>readValue("{\"optionalLong\":123}").getOptionalLong())
        .isEqualTo(OptionalLong.of(123));
    assertThat(reader.<TestOptionals>readValue("{\"optionalLong\":null}").getOptionalLong())
        .isEqualTo(OptionalLong.empty());
    assertThat(reader.<TestOptionals>readValue("{}").getOptionalLong())
        .isEqualTo(null);
  }
  
  @Test
  public void createStandardObjectMapper_serialize_optional() throws Exception {
    testJava8Type(TestOptionals.class, 
        "{\"optionalString\":null,\"optionalLong\":null}",
        "{\"optionalString\":null,\"optionalLong\":null}"
    );
    testJava8Type(TestOptionalsNonAbsent.class, 
        "{}",
        "{}"
    );
    testJava8Type(TestOptionalsNonEmpty.class, 
        "{}",
        "{}"
    );
    testJava8Type(TestOptionalsNonNull.class, 
        "{\"optionalString\":null,\"optionalLong\":null}",
        "{}"
    );
    testJava8Type(TestOptionalsNonDefault.class, 
        "{\"optionalString\":null,\"optionalLong\":null}",
        "{}"
    );
    testJava8Type(TestOptionalsUseDefaults.class,
        "{\"optionalString\":null,\"optionalLong\":null}",
        "{\"optionalString\":null,\"optionalLong\":null}"
    );
    testJava8Type(TestOptionalsAlways.class, 
        "{\"optionalString\":null,\"optionalLong\":null}",
        "{\"optionalString\":null,\"optionalLong\":null}"
    );
  }

  private <T extends TestOptionals> void testJava8Type(Class<T> type, String expectedForEmpty,
      String expectedForNull) throws Exception {
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    Constructor<T> constructor = type.getConstructor(Optional.class, OptionalLong.class);
    assertThat(mapper.writeValueAsString(constructor.newInstance(Optional.of("value"), OptionalLong.of(123))))
        .isEqualTo("{\"optionalString\":\"value\",\"optionalLong\":123}");
    assertThat(mapper.writeValueAsString(constructor.newInstance(Optional.of(""), OptionalLong.of(0))))
        .isEqualTo("{\"optionalString\":\"\",\"optionalLong\":0}");
    assertThat(mapper.writeValueAsString(constructor.newInstance(Optional.empty(), OptionalLong.empty())))
        .isEqualTo(expectedForEmpty);
    assertThat(mapper.writeValueAsString(constructor.newInstance(null, null)))
        .isEqualTo(expectedForNull);
  }

  @Test
  public void createStandardObjectMapper_parameterNames() throws Exception {
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    TestParameterNames result = mapper.readerFor(TestParameterNames.class)
        .readValue("{\"name\":\"Jerry\",\"surname\":\"Smith\"}");
    assertThat(result).isEqualTo(new TestParameterNames("Jerry", "Smith"));
  }

  @Test
  public void createStandardObjectMapper_parameterNames_misingRequiredField() throws Exception {
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    TestParameterNames result = mapper.readerFor(TestParameterNames.class)
        .readValue("{\"name\":\"Jerry\"}");
    assertThat(result).isEqualTo(new TestParameterNames("Jerry", null));
  }
  
  private enum TestEnum {
    @JsonProperty("test") TEST
  }

  private class TestObject {
    TestEnum test;

    TestObject() {
      this.test = TestEnum.TEST;
    }

    public void setTest(TestEnum test) {
      this.test = test;
    }

    public TestEnum getTest() {
      return test;
    }
  }
  
  private static class TestOptionals {
    Optional<String> optionalString;
    OptionalLong optionalLong;

    public TestOptionals() {
    }

    public TestOptionals(Optional<String> optionalString, OptionalLong optionalLong) {
      this.optionalString = optionalString;
      this.optionalLong = optionalLong;
    }

    public Optional<String> getOptionalString() {
      return optionalString;
    }

    public void setOptionalString(Optional<String> optionalString) {
      this.optionalString = optionalString;
    }

    public OptionalLong getOptionalLong() {
      return optionalLong;
    }

    public TestOptionals setOptionalLong(OptionalLong optionalLong) {
      this.optionalLong = optionalLong;
      return this;
    }
  }

  @JsonInclude(Include.NON_ABSENT)
  private static class TestOptionalsNonAbsent extends TestOptionals {
    public TestOptionalsNonAbsent() {
    }
    public TestOptionalsNonAbsent(Optional<String> optionalString, OptionalLong optionalLong) {
      super(optionalString, optionalLong);
    }
  }

  @JsonInclude(Include.NON_EMPTY)
  private static class TestOptionalsNonEmpty extends TestOptionals {
    public TestOptionalsNonEmpty() {
    }
    public TestOptionalsNonEmpty(Optional<String> optionalString, OptionalLong optionalLong) {
      super(optionalString, optionalLong);
    }
  }

  @JsonInclude(Include.NON_NULL)
  private static class TestOptionalsNonNull extends TestOptionals {
    public TestOptionalsNonNull() {
    }
    public TestOptionalsNonNull(Optional<String> optionalString, OptionalLong optionalLong) {
      super(optionalString, optionalLong);
    }
  }

  @JsonInclude(Include.NON_DEFAULT)
  private static class TestOptionalsNonDefault extends TestOptionals {
    public TestOptionalsNonDefault() {
    }
    public TestOptionalsNonDefault(Optional<String> optionalString,
        OptionalLong optionalLong) {
      super(optionalString, optionalLong);
    }
  }

  @JsonInclude(Include.USE_DEFAULTS)
  private static class TestOptionalsUseDefaults extends TestOptionals {
    public TestOptionalsUseDefaults() {
    }
    public TestOptionalsUseDefaults(Optional<String> optionalString,
        OptionalLong optionalLong) {
      super(optionalString, optionalLong);
    }
  }
  
  @JsonInclude(Include.ALWAYS)
  private static class TestOptionalsAlways extends TestOptionals {
    public TestOptionalsAlways() {
    }
    public TestOptionalsAlways(Optional<String> optionalString, OptionalLong optionalLong) {
      super(optionalString, optionalLong);
    }
  }

  private static class TestParameterNames {

    private final String name;
    private final String surname;

    //could not be deserialized without @JsonProperty("fieldName) if not compiled with -parameters
    public TestParameterNames(String name, String surname) {
      this.name = name;
      this.surname = surname;
    }
    
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestParameterNames that = (TestParameterNames) o;
      return Objects.equals(name, that.name) &&
          Objects.equals(surname, that.surname);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, surname);
    }

    @Override
    public String toString() {
      return "TestParameterNames{" +
          "name='" + name + '\'' +
          ", surname='" + surname + '\'' +
          '}';
    }
  }
  
}