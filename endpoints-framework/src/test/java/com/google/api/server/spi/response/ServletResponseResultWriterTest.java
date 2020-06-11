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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.api.server.spi.ConfiguredObjectMapper;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.appengine.api.datastore.Blob;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Tests for {@link ServletResponseResultWriter}.
 */
@RunWith(JUnit4.class)
public class ServletResponseResultWriterTest {

  private static final long DATE_VALUE = 1000000000L;
  private static final String DATE_VALUE_STRING = "1970-01-12T13:46:40.000";
  private static final String DATE_AND_TIME_VALUE_STRING = "2002-10-02T10:00:00-05:00";
  private static final String SIMPLE_DATE_VALUE_STRING = "2002-10-02";

  @Test
  public void testTypeChangesInMapAsString() throws Exception {
    Map<String, Object> value = new HashMap<>();
    value.put("nonPrimitive", 100L);
    value.put("primitive", 200L);
    value.put("date", new Date(DATE_VALUE));
    value.put("dateAndTime", DateAndTime.parseRfc3339String(DATE_AND_TIME_VALUE_STRING));
    value.put("simpleDate", new SimpleDate(2002, 10, 2));
    testTypeChangesAsString(value);
  }

  @Test
  @SuppressWarnings("unused")
  public void testTypeChangesInBeanAsString() throws Exception {
    Object value = new Object() {
      public long getPrimitive() {
        return 200L;
      }
      public Long getNonPrimitive() {
        return 100L;
      }
      public Long getLongNull() {
        return null;
      }
      public String getStringNull() {
        return null;
      }
      public String getStringEmpty() {
        return "";
      }
      public Date getDate() {
        return new Date(DATE_VALUE);
      }
      public Date getDateNull() {
        return null;
      }
      public DateAndTime getDateAndTime() {
        return DateAndTime.parseRfc3339String(DATE_AND_TIME_VALUE_STRING);
      }
      public DateAndTime getDateAndTimeNull() {
        return null;
      }

      public SimpleDate getSimpleDate() {
        return new SimpleDate(2002, 10, 2);
      }

      public SimpleDate getSimpleDateNull() {
        return null;
      }
      public Long[] getEmptyLongArray() {
        return new Long[0];
      }
      public List<Long> getEmptyLongList() {
        return new ArrayList<>();
      }
    };
    ObjectNode output = testTypeChangesAsString(value);
    assertTrue(output.path("longNull").isMissingNode());
    assertTrue(output.path("stringNull").isMissingNode());
    assertEquals("", output.path("stringEmpty").asText(null));
    assertTrue(output.path("dateNull").isMissingNode());
    assertTrue(output.path("dateAndTimeNull").isMissingNode());
    assertTrue(output.path("simpleDateNull").isMissingNode());
    assertTrue(output.path("emptyLongArray").isMissingNode());
    assertTrue(output.path("emptyLongList").isMissingNode());
  }

  @Test
  @SuppressWarnings("unused")
  public void testPropertyInclusion_noModifier() throws Exception {
    Object value = new Object() {
      public String getStringEmpty() {
        return "";
      }
      public String getStringNotEmpty() {
        return "not empty";
      }
      public Optional<String> getOptionalStringNull() {
        return null;
      }
      public Optional<String> getOptionalStringEmpty() {
        return Optional.empty();
      }
      public Optional<String> getOptionalStringEmptyContent() {
        return Optional.of("");
      }
      public Optional<String> getOptionalStringNotEmpty() {
        return Optional.of("not empty");
      }
      public OptionalLong getOptionalLongNull() {
        return null;
      }
      public OptionalLong getOptionalLongEmpty() {
        return OptionalLong.empty();
      }
      public OptionalLong getOptionalLongNotEmpty() {
        return OptionalLong.of(123L);
      }
      public Long getLongNull() {
        return null;
      }
      public String getStringNull() {
        return null;
      }
      public Date getDate() {
        return new Date(DATE_VALUE);
      }
      public Date getDateNull() {
        return null;
      }
      public DateAndTime getDateAndTime() {
        return DateAndTime.parseRfc3339String(DATE_AND_TIME_VALUE_STRING);
      }
      public DateAndTime getDateAndTimeNull() {
        return null;
      }

      public SimpleDate getSimpleDate() {
        return new SimpleDate(2002, 10, 2);
      }

      // Null or empty objects, no annotation
      public SimpleDate getSimpleDateNull() {
        return null;
      }
      public Long[] getNullLongArray() {
        return null;
      }
      public List<Long> getNullLongList() {
        return null;
      }
      public Long[] getEmptyLongArray() {
        return new Long[0];
      }
      public List<Long> getEmptyLongList() {
        return new ArrayList<>();
      }
      public Map<Long,Object> getEmptyMap() {
        return new HashMap<>();
      }
      public List<List<Map<Long,Object>>> getDeeplyEmptyLongList() {
        List<Map<Long,Object>> list = ImmutableList.of(new HashMap<>());
        return ImmutableList.of(list);
      }
      public Map<Long,List<Long>> getDeeplyEmptyMapList() {
        return ImmutableMap.of(12L, new ArrayList<>());
      }
      public Map<Long,Long[]> getDeeplyEmptyMapArray() {
        return ImmutableMap.of(12L, new Long[0]);
      }
      
      //string handling in collections / maps
      public List<String> getStringListWithEmptyValues() {
        return Lists.newArrayList(null, "");
      }
      public String[] getStringArrayWithEmptyValues() {
        return new String[] {null, ""};
      }
      public Map<String,String> getStringMapWithEmptyValues() {
        return new HashMap<String, String>() {{
          put("null_value", null);
          put("empty_value", "");
        }};
      }
      public Map<String,List<String>> getMapWithEmptyListValues() {
        return new HashMap<String, List<String>>() {{
          put("null_value", null);
          put("empty_value", new ArrayList<>());
          put("empty_string_value", Lists.newArrayList(null, ""));
        }};
      }
    };

    ObjectNode legacyOutput = toJSON(value, true);
    ObjectNode output = toJSON(value);
    
    //the new Mapper config must produce same result as the old one using WRITE_EMPTY_JSON_ARRAYS
    //see https://github.com/FasterXML/jackson-databind/issues/1547 for more on the issue
    assertEquals(legacyOutput, output);

    //String is handled specifically
    assertEquals("", output.path("stringEmpty").asText(null));
    assertPathPresent("\"not empty\"", output.path("stringNotEmpty"));
    
    //optionals
    assertTrue(output.path("optionalStringNull").isMissingNode());
    assertPathPresent("null", output.path("optionalStringEmpty"));
    assertPathPresent("\"\"", output.path("optionalStringEmptyContent"));
    assertPathPresent("\"not empty\"", output.path("optionalStringNotEmpty"));

    //simple objects
    assertTrue(output.path("longNull").isMissingNode());
    assertTrue(output.path("stringNull").isMissingNode());
    assertTrue(output.path("dateNull").isMissingNode());
    assertTrue(output.path("dateAndTimeNull").isMissingNode());
    assertTrue(output.path("simpleDateNull").isMissingNode());
    
    //collections
    assertTrue(output.path("nullLongArray").isMissingNode());
    assertTrue(output.path("nullLongList").isMissingNode());
    assertTrue(output.path("emptyLongArray").isMissingNode());
    assertTrue(output.path("emptyLongList").isMissingNode());
    assertTrue(output.path("emptyMap").isMissingNode());
    assertTrue(output.path("deeplyEmptyLongList").isMissingNode());
    assertTrue(output.path("deeplyEmptyMapList").isMissingNode());
    assertTrue(output.path("deeplyEmptyMapArray").isMissingNode());
    
    //strings in collections
    assertPathPresent("[null,\"\"]", output.path("stringListWithEmptyValues"));
    assertPathPresent("[null,\"\"]", output.path("stringArrayWithEmptyValues"));
    assertPathPresent("{\"empty_value\":\"\"}", output.path("stringMapWithEmptyValues"));
    assertPathPresent("{\"empty_string_value\":[null,\"\"],\"empty_value\":[]}", 
        output.path("mapWithEmptyListValues"));
  }
  
  @Test
  @SuppressWarnings("unused")
  public void testPropertyInclusion_includeAlways() throws Exception {
    Object value = new Object() {
      // Null or empty objects, annotation value (ALWAYS)
      @JsonInclude
      public String getStringEmpty() {
        return "";
      }
      @JsonInclude
      public String getStringNotEmpty() {
        return "not empty";
      }
      @JsonInclude
      public Optional<String> getOptionalStringNull() {
        return null;
      }
      @JsonInclude
      public Optional<String> getOptionalStringEmpty() {
        return Optional.empty();
      }
      @JsonInclude
      public Optional<String> getOptionalStringNotEmpty() {
        return Optional.of("not empty");
      }
      @JsonInclude
      public SimpleDate getSimpleDateNull() {
        return null;
      }
      @JsonInclude
      public Long[] getNullLongArray() {
        return null;
      }
      @JsonInclude
      public List<Long> getNullLongList() {
        return null;
      }
      @JsonInclude
      public Long[] getEmptyLongArray() {
        return new Long[0];
      }
      @JsonInclude
      public List<Long> getEmptyLongList() {
        return new ArrayList<>();
      }
      @JsonInclude
      public Map<Long,Object> getEmptyMap() {
        return new HashMap<>();
      }
      @JsonInclude
      public List<List<Map<Long,Object>>> getDeeplyEmptyLongList() {
        List<Map<Long,Object>> list = ImmutableList.of(new HashMap<>());
        return ImmutableList.of(list);
      }
      @JsonInclude
      public Map<Long,List<Long>> getDeeplyEmptyMapList() {
        return ImmutableMap.of(12L, new ArrayList<>());
      }
      @JsonInclude
      public Map<Long,Long[]> getDeeplyEmptyMapArray() {
        return ImmutableMap.of(12L, new Long[0]);
      }
      //string handling in collections / maps
      @JsonInclude
      public List<String> getStringListWithEmptyValues() {
        return Lists.newArrayList(null, "");
      }
      @JsonInclude
      public String[] getStringArrayWithEmptyValues() {
        return new String[] {null, ""};
      }
      @JsonInclude
      public Map<String,String> getStringMapWithEmptyValues() {
        return new HashMap<String, String>() {{
          put("null_value", null);
          put("empty_value", "");
        }};
      }
      @JsonInclude
      public Map<String,List<String>> getMapWithEmptyListValues() {
        return new HashMap<String, List<String>>() {{
          put("null_value", null);
          put("empty_value", new ArrayList<>());
          put("empty_string_value", Lists.newArrayList(null, ""));
        }};
      }
    };
    
    ObjectNode output = toJSON(value);
    
    //String is handled specifically
    assertEquals("", output.path("stringEmpty").asText(null));
    assertPathPresent("\"not empty\"", output.path("stringNotEmpty"));

    //simple objects
    assertPathPresent("null", output.path("simpleDateNull"));
    assertPathPresent("null", output.path("nullLongArray"));
    assertPathPresent("null", output.path("nullLongList"));
    
    //optionals
    assertPathPresent("null", output.path("optionalStringNull"));
    assertPathPresent("null", output.path("optionalStringEmpty"));
    assertPathPresent("\"not empty\"", output.path("optionalStringNotEmpty"));
    
    //collections
    assertPathPresent("[]", output.path("emptyLongArray"));
    assertPathPresent("[]", output.path("emptyLongList"));
    assertPathPresent("{}", output.path("emptyMap"));
    assertPathPresent("[[{}]]", output.path("deeplyEmptyLongList"));
    assertPathPresent("{\"12\":[]}", output.path("deeplyEmptyMapList"));
    assertPathPresent("{\"12\":[]}", output.path("deeplyEmptyMapArray"));

    //strings in collections
    assertPathPresent("[null,\"\"]", output.path("stringListWithEmptyValues"));
    assertPathPresent("[null,\"\"]", output.path("stringArrayWithEmptyValues"));
    assertPathPresent("{\"null_value\":null,\"empty_value\":\"\"}",
        output.path("stringMapWithEmptyValues"));
    assertPathPresent(
        "{\"null_value\":null,\"empty_string_value\":[null,\"\"],\"empty_value\":[]}",
        output.path("mapWithEmptyListValues"));
  }

  @Test
  @SuppressWarnings("unused")
  public void testPropertyInclusion_includeNonNull() throws Exception {
    Object value = new Object() {
      // Null or empty objects, annotation NON_NULL
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public String getStringEmpty() {
        return "";
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public String getStringNotEmpty() {
        return "not empty";
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public SimpleDate getSimpleDateNull() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Optional<String> getOptionalStringNull() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Optional<String> getOptionalStringEmpty() {
        return Optional.empty();
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Optional<String> getOptionalStringNotEmpty() {
        return Optional.of("not empty");
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Long[] getNullLongArray() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public List<Long> getNullLongList() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Long[] getEmptyLongArray() {
        return new Long[0];
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public List<Long> getEmptyLongList() {
        return new ArrayList<>();
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Map<Long,Object> getEmptyMap() {
        return new HashMap<>();
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public List<List<Map<Long,Object>>> getDeeplyEmptyLongList() {
        List<Map<Long,Object>> list = ImmutableList.of(new HashMap<>());
        return ImmutableList.of(list);
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Map<Long,List<Long>> getDeeplyEmptyMapList() {
        return ImmutableMap.of(12L, new ArrayList<>());
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Map<Long,Long[]> getDeeplyEmptyMapArray() {
        return ImmutableMap.of(12L, new Long[0]);
      }
      //string handling in collections / maps
      @JsonInclude(value = JsonInclude.Include.NON_NULL, content = Include.NON_NULL)
      public List<String> getStringListWithEmptyValues() {
        return Lists.newArrayList(null, "");
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL, content = Include.NON_NULL)
      public String[] getStringArrayWithEmptyValues() {
        return new String[] {null, ""};
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL, content = Include.NON_NULL)
      public Map<String,String> getStringMapWithEmptyValues() {
        return new HashMap<String, String>() {{
          put("null_value", null);
          put("empty_value", "");
        }};
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL, content = Include.NON_NULL)
      public Map<String,List<String>> getMapWithEmptyListValues() {
        return new HashMap<String, List<String>>() {{
          put("null_value", null);
          put("empty_value", new ArrayList<>());
          put("empty_string_value", Lists.newArrayList(null, ""));
        }};
      }
    };
    ObjectNode output = toJSON(value);

    //String is handled specifically
    assertEquals("", output.path("stringEmpty").asText(null));
    assertPathPresent("\"not empty\"", output.path("stringNotEmpty"));

    //simple objects
    assertTrue(output.path("simpleDateNull").isMissingNode());
    assertTrue(output.path("nullLongArray").isMissingNode());
    assertTrue(output.path("nullLongList").isMissingNode());

    //optionals
    assertTrue(output.path("optionalStringNull").isMissingNode());
    assertPathPresent("null", output.path("optionalStringEmpty"));
    assertPathPresent("\"not empty\"", output.path("optionalStringNotEmpty"));

    //collections
    assertPathPresent("[]", output.path("emptyLongArray"));
    assertPathPresent("[]", output.path("emptyLongList"));
    assertPathPresent("{}", output.path("emptyMap"));
    assertPathPresent("[[{}]]", output.path("deeplyEmptyLongList"));
    assertPathPresent("{\"12\":[]}", output.path("deeplyEmptyMapList"));
    assertPathPresent("{\"12\":[]}", output.path("deeplyEmptyMapArray"));

    //strings in collections
    assertPathPresent("[null,\"\"]", output.path("stringListWithEmptyValues"));
    assertPathPresent("[null,\"\"]", output.path("stringArrayWithEmptyValues"));
    assertPathPresent("{\"empty_value\":\"\"}", output.path("stringMapWithEmptyValues"));
    assertPathPresent("{\"empty_string_value\":[null,\"\"],\"empty_value\":[]}",
        output.path("mapWithEmptyListValues"));
  }

  @Test
  @SuppressWarnings("unused")
  public void testPropertyInclusion_includeNonEmpty() throws Exception {
    Object value = new Object() {
      // Null or empty objects, annotation NON_EMPTY
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public String getStringEmpty() {
        return "";
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public String getStringNotEmpty() {
        return "not empty";
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Optional<String> getOptionalStringNull() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Optional<String> getOptionalStringEmpty() {
        return Optional.empty();
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Optional<String> getOptionalStringEmptyContent() {
        return Optional.of("");
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Optional<String> getOptionalStringNotEmpty() {
        return Optional.of("not empty");
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public SimpleDate getSimpleDateNull() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Long[] getNullLongArray() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<Long> getNullLongList() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Long[] getEmptyLongArray() {
        return new Long[0];
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<Long> getEmptyLongList() {
        return new ArrayList<>();
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,Object> getEmptyMap() {
        return new HashMap<>();
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<List<Map<Long,Object>>> getDeeplyEmptyLongList() {
        List<Map<Long,Object>> list = ImmutableList.of(new HashMap<>());
        return ImmutableList.of(list);
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,List<Long>> getDeeplyEmptyMapList() {
        return ImmutableMap.of(12L, new ArrayList<>());
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,Long[]> getDeeplyEmptyMapArray() {
        return ImmutableMap.of(12L, new Long[0]);
      }

      // Non empty objects, annotation NON_EMPTY
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Long[] getNonEmptyLongArray() {
        return new Long[] {12L};
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<Long> getNonEmptyLongList() {
        return ImmutableList.of(12L);
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,Object> getNonEmptyMap() {
        return ImmutableMap.of(12L, "");
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<List<Map<Long,Object>>> getDeeplyNonEmptyLongList() {
        List<Map<Long,Object>> list = ImmutableList.of(ImmutableMap.of(12L, ""));
        return ImmutableList.of(list);
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,List<Long>> getDeeplyNonEmptyMapList() {
        return ImmutableMap.of(12L, ImmutableList.of(23L));
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,Long[]> getDeeplyNonEmptyMapArray() {
        return ImmutableMap.of(12L, new Long[] {23L});
      }
      //string handling in collections / maps
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = Include.NON_EMPTY)
      public List<String> getStringListWithEmptyValues() {
        return Lists.newArrayList(null, "");
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = Include.NON_EMPTY)
      public String[] getStringArrayWithEmptyValues() {
        return new String[] {null, ""};
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = Include.NON_EMPTY)
      public Map<String,String> getStringMapWithEmptyValues() {
        return new HashMap<String, String>() {{
          put("null_value", null);
          put("empty_value", "");
        }};
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = Include.NON_EMPTY)
      public Map<String,List<String>> getMapWithEmptyListValues() {
        return new HashMap<String, List<String>>() {{
          put("null_value", null);
          put("empty_value", new ArrayList<>());
          put("empty_string_value", Lists.newArrayList(null, ""));
        }};
      }
    };
    ObjectNode output = toJSON(value);
    
    //String is handled specifically
    assertTrue(output.path("stringEmpty").isMissingNode());
    assertPathPresent("\"not empty\"", output.path("stringNotEmpty"));

    //optionals
    assertTrue(output.path("optionalStringNull").isMissingNode());
    assertTrue(output.path("optionalStringEmpty").isMissingNode());
    //this is different than empty plain string behavior
    assertPathPresent("\"\"", output.path("optionalStringEmptyContent"));
    assertPathPresent("\"not empty\"", output.path("optionalStringNotEmpty"));
    
    
    //simple objects
    assertTrue(output.path("simpleDateNull").isMissingNode());

    //collections
    assertTrue(output.path("nullLongArray").isMissingNode());
    assertTrue(output.path("nullLongList").isMissingNode());
    assertTrue(output.path("emptyLongArray").isMissingNode());
    assertTrue(output.path("emptyLongList").isMissingNode());
    assertTrue(output.path("emptyMap").isMissingNode());
    assertTrue(output.path("deeplyEmptyLongList").isMissingNode());
    assertTrue(output.path("deeplyEmptyMapList").isMissingNode());
    assertTrue(output.path("deeplyEmptyMapArray").isMissingNode());
    assertPathPresent("[\"12\"]", output.path("nonEmptyLongArray"));
    assertPathPresent("[\"12\"]", output.path("nonEmptyLongList"));
    assertPathPresent("{\"12\":\"\"}", output.path("nonEmptyMap"));
    assertPathPresent("[[{\"12\":\"\"}]]", output.path("deeplyNonEmptyLongList"));
    assertPathPresent("{\"12\":[\"23\"]}", output.path("deeplyNonEmptyMapList"));
    assertPathPresent("{\"12\":[\"23\"]}", output.path("deeplyNonEmptyMapArray"));

    //strings in collections
    assertPathPresent("[null,\"\"]", output.path("stringListWithEmptyValues"));
    assertPathPresent("[null,\"\"]", output.path("stringArrayWithEmptyValues"));
    assertPathPresent("{}", output.path("stringMapWithEmptyValues"));
    assertPathPresent("{\"empty_string_value\":[null,\"\"]}",
        output.path("mapWithEmptyListValues"));
  }

  private void assertPathPresent(String expectedString, JsonNode path) {
    assertFalse(path.isMissingNode());
    assertEquals(expectedString, path.toString());
  }

  @Test
  public void testTypeChangesInArrayAsString() throws Exception {
    Object[] array = new Object[]{100L, 200L};
    ObjectNode output = toJSON(array);
    ArrayNode items = (ArrayNode) output.get("items");
    assertTrue(items.get(0).isTextual());
    assertEquals("100", items.get(0).asText());
    assertTrue(items.get(1).isTextual());
    assertEquals("200", items.get(1).asText());
  }

  @Test
  public void testWriteNull() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = getDefaultWriter(response);
    writer.write(null, HttpServletResponse.SC_NO_CONTENT);
    assertEquals("", response.getContentAsString());
    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
  }

  @Test
  public void testWriteCustomStatus() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = getDefaultWriter(response);
    writer.write("response", HttpServletResponse.SC_CREATED);
    assertEquals("\"response\"", response.getContentAsString());
    assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
  }

  @SuppressWarnings("unused")
  public void testByteArrayAsBase64() throws Exception {
    Object value = new Object() {
      public byte[] getValues() {
        return new byte[]{1, 2, 3, 4};
      }
    };
    ObjectNode output = ObjectMapperUtil.createStandardObjectMapper()
        .readValue(writeToResponse(value, false), ObjectNode.class);
    assertEquals("AQIDBA==", output.path("values").asText());
  }

  @Test
  public void testWriteErrorResponseHeaders() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = getDefaultWriter(response);
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("name0", "value0");
    headers.put("name1", "value1");
    writer.writeError(new UnauthorizedException("message", "schema", headers));
    assertEquals("schema name0=value0, name1=value1", response.getHeader("WWW-Authenticate"));
  }

  @Test
  public void testPrettyPrint() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = new ServletResponseResultWriter(response, 
        (ApiSerializationConfig) null, true /* prettyPrint */, true /* addContentLength */);
    writer.write(ImmutableMap.of("one", "two", "three", "four"), HttpServletResponse.SC_OK);
    // If the response is pretty printed, there should be at least two newlines.
    String body = response.getContentAsString();
    int index = body.indexOf('\n');
    assertThat(index).isAtLeast(0);
    index = body.indexOf('\n', index + 1);
    assertThat(index).isAtLeast(0);
    // Unlike the Jackson pretty printer, which will either put no space around a colon or a space
    // on both sides, we want to ensure that a space comes after a colon, but not before.
    assertThat(body).contains("\": ");
    assertThat(body).doesNotContain("\" :");
  }

  @SuppressWarnings("unused")
  public void testBlobAsBase64() throws Exception {
    Object value = new Object() {
      public Blob getBlob() {
        return new Blob(new byte[]{1, 2, 3, 4});
      }
    };
    ObjectNode output = ObjectMapperUtil.createStandardObjectMapper()
        .readValue(writeToResponse(value, false), ObjectNode.class);
    assertEquals("AQIDBA==", output.path("blob").asText());
  }

  public enum TestEnum {
    TEST1, TEST2
  }

  @Test
  public void testEnumAsString() throws Exception {
    TestEnum value = TestEnum.TEST1;
    JsonNode output = ObjectMapperUtil.createStandardObjectMapper()
        .readValue(writeToResponse(value, false), JsonNode.class);
    assertEquals("TEST1", output.asText());
  }

  private ObjectNode testTypeChangesAsString(Object value) throws Exception {
    ObjectNode output = toJSON(value);
    assertTrue(output.get("nonPrimitive").isTextual());
    assertEquals("100", output.get("nonPrimitive").asText());
    assertTrue(output.get("primitive").isTextual());
    assertEquals("200", output.get("primitive").asText());
    assertEquals(
        new com.google.api.client.util.DateTime(DATE_VALUE_STRING),
        new com.google.api.client.util.DateTime(output.get("date").asText()));
    assertEquals(DATE_AND_TIME_VALUE_STRING, output.get("dateAndTime").asText());
    assertEquals(
        DateAndTime.parseRfc3339String(DATE_AND_TIME_VALUE_STRING),
        DateAndTime.parseRfc3339String(output.get("dateAndTime").asText()));
    assertEquals(SIMPLE_DATE_VALUE_STRING, output.get("simpleDate").asText());
    return output;
  }

  private ObjectNode toJSON(Object value) throws IOException {
    return toJSON(value, false);  
  }
  
  private ObjectNode toJSON(Object value, boolean legacy) throws IOException {
    String responseBody = writeToResponse(value, legacy);
    return ObjectMapperUtil.createStandardObjectMapper()
        .readValue(responseBody, ObjectNode.class);
  }

  private String writeToResponse(Object value, boolean legacy) throws IOException {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = legacy
        ? new ServletResponseResultWriter(response, getLegacyObjectWriter(), false, false)
        : getDefaultWriter(response);
    writer.write(value, HttpServletResponse.SC_OK);
    return response.getContentAsString();
  }

  @Test
  public void testExceptionWriterShouldNotBeCustomized() throws IOException {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = createCustomizedWriter(response);
    ServiceException exception = new ServiceException(400, "sample message");
    writer.writeError(exception);
    String errorContent = response.getContentAsString();
    assertEquals("{\"error_message\":\"sample message\"}", errorContent);
  }

  @Test
  public void testWriterCustomization() throws IOException {
    Map<String, String> unorderedMap = new HashMap<>();
    unorderedMap.put("a", "value_a");
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = createCustomizedWriter(response);
    writer.write(unorderedMap, HttpServletResponse.SC_OK);
    String content = response.getContentAsString();
    assertEquals("{a:\"value_a\"}", content);
  }

  //Customized writer: for response, the fields name has no quote. For error, they have.
  private ServletResponseResultWriter createCustomizedWriter(HttpServletResponse response) {
    return new ServletResponseResultWriter(response, (ApiSerializationConfig) null, false, false) {
      @Override
      protected ObjectWriter configureWriter(ObjectWriter objectWriter) {
        return objectWriter.withoutFeatures(JsonWriteFeature.QUOTE_FIELD_NAMES);
      }
    };
  }

  //creates an object writer with configuration as before 2.4
  private ObjectWriter getLegacyObjectWriter() {
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper(null);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    return ConfiguredObjectMapper.builder()
        .addRegisteredModules(ServletResponseResultWriter.WRITER_MODULES)
        .buildWithCustomMapper(mapper).writer();
  }

  private ServletResponseResultWriter getDefaultWriter(MockHttpServletResponse response) {
    return new ServletResponseResultWriter(
        response, (ApiSerializationConfig) null, false, false);
  }
}
