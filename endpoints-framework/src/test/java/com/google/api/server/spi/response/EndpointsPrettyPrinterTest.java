/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.google.api.server.spi.response;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.ObjectMapperUtil;

import com.fasterxml.jackson.databind.ObjectWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link EndpointsPrettyPrinter}.
 */
@RunWith(JUnit4.class)
public class EndpointsPrettyPrinterTest {
  private final ObjectWriter writer = ObjectMapperUtil.createStandardObjectMapper()
      .writer(new EndpointsPrettyPrinter());
  private final Foo foo = new Foo();

  @Test
  public void testColonSpacing() throws Exception {
    assertThat(writer.writeValueAsString(foo)).contains("\": 1");
  }

  @Test
  public void testSingleSpaceIndent() throws Exception {
    assertThat(writer.writeValueAsString(foo)).contains("{\n \"x");
  }

  private static class Foo {
    public int x = 1;
  }
}
