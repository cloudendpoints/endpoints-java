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
package com.google.api.server.spi.config;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Iterables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ApiConfigInconsistency}.
 */
@RunWith(JUnit4.class)
public class ApiConfigInconsistencyTest {
  private
  ApiConfigInconsistency.ListBuilder<Object> builder;

  @Before
  public void setUp() {
    builder = ApiConfigInconsistency.listBuilder();
  }

  @Test
  public void testAddIfInconsistent_inconsistent() {
    builder.addIfInconsistent("foo", 10, 12);

    ApiConfigInconsistency<?> inconsistency = Iterables.getFirst(builder.build(), null);
    assertEquals("foo", inconsistency.getPropertyName());
    assertEquals(10, inconsistency.getValue1());
    assertEquals(12, inconsistency.getValue2());
  }

  @Test
  public void testAddIfInconsistent_consistent() {
    builder.addIfInconsistent("foo", 1.0, 1.0);
    assertThat(builder.build()).isEmpty();
  }
}
