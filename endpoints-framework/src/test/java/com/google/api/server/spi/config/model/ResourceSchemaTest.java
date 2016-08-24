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
package com.google.api.server.spi.config.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.api.server.spi.config.ResourcePropertySchema;
import com.google.api.server.spi.config.ResourceSchema;
import com.google.common.testing.NullPointerTester;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link ResourceSchema}. */
@RunWith(JUnit4.class)
public class ResourceSchemaTest {

  @Test
  public void testNullStaticMethods() {
    new NullPointerTester()
        .testAllPublicStaticMethods(ResourceSchema.class);
  }

  @Test
  public void testDefaultSchema() {
    ResourceSchema schema = ResourceSchema.builderForType(Integer.class).build();
    assertNull(schema.getName());
    assertEquals(Integer.class, schema.getType());
    assertThat(schema.getProperties()).isEmpty();
  }

  @Test
  public void testDefaultSchemaWithAlternateName() {
    ResourceSchema schema = ResourceSchema.builderForType(Integer.class).setName("Number").build();
    assertEquals("Number", schema.getName());
    assertEquals(Integer.class, schema.getType());
  }

  @Test
  public void testDefaultSchemaWithProperties() {
    ResourceSchema schema = ResourceSchema.builderForType(Integer.class)
        .addProperty("foo", ResourcePropertySchema.of(Float.class))
        .addProperty("bar", ResourcePropertySchema.of(Double.class))
        .build();
    assertThat(schema.getProperties().keySet()).containsExactly("foo", "bar");
    assertEquals(Float.class, schema.getProperties().get("foo").getJavaType());
    assertEquals(Double.class, schema.getProperties().get("bar").getJavaType());
  }

  @Test
  public void testDuplicateProperties() {
    try {
      ResourceSchema schema = ResourceSchema.builderForType(Integer.class)
          .addProperty("foo", ResourcePropertySchema.of(Float.class))
          .addProperty("foo", ResourcePropertySchema.of(Double.class))
          .build();
      fail("Expected " + IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testBuilderWithResource() {
    ResourceSchema originalSchema = ResourceSchema.builderForType(Integer.class)
        .addProperty("foo", ResourcePropertySchema.of(Float.class))
        .build();
    ResourceSchema.Builder newSchemaBuilder = ResourceSchema.builderWithSchema(originalSchema);
    assertEquals(originalSchema, newSchemaBuilder.build());
    newSchemaBuilder.addProperty("bar", ResourcePropertySchema.of(Double.class));
    newSchemaBuilder.setName("Number");
    assertThat(newSchemaBuilder.build().getProperties().keySet()).containsExactly("foo", "bar");
    assertEquals(newSchemaBuilder.build().getName(), "Number");
  }
}
