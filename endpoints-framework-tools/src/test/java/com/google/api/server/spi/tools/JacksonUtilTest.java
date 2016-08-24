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
package com.google.api.server.spi.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests of {@link JacksonUtil}.
 *
 * @author sven@google.com (Sven Mawson)
 */
@RunWith(JUnit4.class)
public class JacksonUtilTest {

  private final ObjectMapper mapper = new ObjectMapper()
      .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

  @Test
  public void testMergeObject_disjoint() throws Exception {
    assertMergeObject("{\"a\": 10}", "{\"b\": 20}", "{\"a\":10,\"b\":20}");
  }

  @Test
  public void testMergeObject_sameValue() throws Exception {
    assertMergeObject("{\"a\": 10}", "{\"a\": 10}", "{\"a\":10}");
  }

  @Test
  public void testMergeObject_conflictingValue() throws Exception {
    assertNoMergeObject("{\"a\": 10}", "{\"a\": 20}");
  }

  @Test
  public void testMergeObject_conflictingValue_noThrowOnConflict() throws Exception {
    assertMergeObject("{\"a\": 10}", "{\"a\": 20}", "{\"a\": 10}", false);
  }

  @Test
  public void testMergingObject_deepDisjoint() throws Exception {
    assertMergeObject(
        "{\"a\":10,\"w\":{\"x\":100}}",
        "{\"b\":20,\"w\":{\"y\":200}}",
        "{\"a\":10,\"b\":20,\"w\":{\"x\":100,\"y\":200}}");
  }

  @Test
  public void testMergeObject_deepConflict() throws Exception {
    assertNoMergeObject(
        "{\"a\":10,\"w\":{\"x\":100}}",
        "{\"a\":10,\"w\":{\"x\":200}}");
  }

  @Test
  public void testMergeObject_deepConflict_noThrowOnConflict() throws Exception {
    assertMergeObject(
        "{\"a\":10,\"w\":{\"x\":100}}",
        "{\"a\":10,\"w\":{\"x\":200}}",
        "{\"a\":10,\"w\":{\"x\":100}}",
        false);
  }

  @Test
  public void testMergeObject_deepArrayConflict_noThrowOnConflict() throws Exception {
    assertMergeObject(
        "{\"a\":10,\"w\":{\"x\":[100]}}",
        "{\"a\":10,\"w\":{\"x\":200}}",
        "{\"a\":10,\"w\":{\"x\":[100]}}",
        false);
  }

  @Test
  public void testMergeObject_deepObjectConflict_noThrowOnConflict() throws Exception {
    assertMergeObject(
        "{\"a\":10,\"w\":{\"x\":{\"y\":100}}}",
        "{\"a\":10,\"w\":{\"x\":200}}",
        "{\"a\":10,\"w\":{\"x\":{\"y\":100}}}",
        false);
  }

  /** Asserts that the given json gets merged correctly. */
  private void assertMergeObject(String json1, String json2, String jsonExpected) throws Exception {
    assertMergeObject(json1, json2, jsonExpected, true);
  }

  private void assertMergeObject(String json1, String json2, String jsonExpected,
      boolean throwOnConflict) throws Exception {
    ObjectNode object1 = mapper.readValue(json1, ObjectNode.class);
    ObjectNode object2 = mapper.readValue(json2, ObjectNode.class);
    ObjectNode expected = mapper.readValue(jsonExpected, ObjectNode.class);
    ObjectNode merged = JacksonUtil.mergeObject(object1, object2, throwOnConflict);
    assertEquals(expected, merged);
  }

  /** Asserts that the merging of the given json objects fails. */
  private void assertNoMergeObject(String json1, String json2) throws Exception {
    try {
      assertMergeObject(json1, json2, "{}");
      fail("Should not be able to merge with conflicting values.");
    } catch (IllegalArgumentException expected) {
      // Expected.
    }
  }
}