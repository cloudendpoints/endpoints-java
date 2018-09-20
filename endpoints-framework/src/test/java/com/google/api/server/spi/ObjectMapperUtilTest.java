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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.server.spi.config.model.EndpointsFlag;
import org.junit.Test;

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
}