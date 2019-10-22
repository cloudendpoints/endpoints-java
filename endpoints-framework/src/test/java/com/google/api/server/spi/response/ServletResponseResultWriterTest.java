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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.appengine.api.datastore.Blob;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

import javax.servlet.http.HttpServletResponse;

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
      public String getStringNotEmpty() {
        return "not empty";
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

      // Null or empty objects, annotation value (ALWAYS)
      @JsonInclude
      public SimpleDate getSimpleDateNull_IncludeAlways() {
        return null;
      }
      @JsonInclude
      public Long[] getNullLongArray_IncludeAlways() {
        return null;
      }
      @JsonInclude
      public List<Long> getNullLongList_IncludeAlways() {
        return null;
      }
      @JsonInclude
      public Long[] getEmptyLongArray_IncludeAlways() {
        return new Long[0];
      }
      @JsonInclude
      public List<Long> getEmptyLongList_IncludeAlways() {
        return new ArrayList<>();
      }
      @JsonInclude
      public Map<Long,Object> getEmptyMap_IncludeAlways() {
        return new HashMap<>();
      }
      @JsonInclude
      public List<List<Map<Long,Object>>> getDeeplyEmptyLongList_IncludeAlways() {
        List<Map<Long,Object>> list = ImmutableList.of(new HashMap<>());
        return ImmutableList.of(list);
      }
      @JsonInclude
      public Map<Long,List<Long>> getDeeplyEmptyMapList_IncludeAlways() {
        return ImmutableMap.of(12L, new ArrayList<>());
      }
      @JsonInclude
      public Map<Long,Long[]> getDeeplyEmptyMapArray_IncludeAlways() {
        return ImmutableMap.of(12L, new Long[0]);
      }

      // Null or empty objects, annotation NON_NULL
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public SimpleDate getSimpleDateNull_IncludeNonNull() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Long[] getNullLongArray_IncludeNonNull() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public List<Long> getNullLongList_IncludeNonNull() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Long[] getEmptyLongArray_IncludeNonNull() {
        return new Long[0];
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public List<Long> getEmptyLongList_IncludeNonNull() {
        return new ArrayList<>();
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Map<Long,Object> getEmptyMap_IncludeNonNull() {
        return new HashMap<>();
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public List<List<Map<Long,Object>>> getDeeplyEmptyLongList_IncludeNonNull() {
        List<Map<Long,Object>> list = ImmutableList.of(new HashMap<>());
        return ImmutableList.of(list);
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Map<Long,List<Long>> getDeeplyEmptyMapList_IncludeNonNull() {
        return ImmutableMap.of(12L, new ArrayList<>());
      }
      @JsonInclude(value = JsonInclude.Include.NON_NULL)
      public Map<Long,Long[]> getDeeplyEmptyMapArray_IncludeNonNull() {
        return ImmutableMap.of(12L, new Long[0]);
      }

      // Null or empty objects, annotation NON_EMPTY
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public SimpleDate getSimpleDateNull_IncludeNonEmpty() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Long[] getNullLongArray_IncludeNonEmpty() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<Long> getNullLongList_IncludeNonEmpty() {
        return null;
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Long[] getEmptyLongArray_IncludeNonEmpty() {
        return new Long[0];
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<Long> getEmptyLongList_IncludeNonEmpty() {
        return new ArrayList<>();
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,Object> getEmptyMap_IncludeNonEmpty() {
        return new HashMap<>();
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<List<Map<Long,Object>>> getDeeplyEmptyLongList_IncludeNonEmpty() {
        List<Map<Long,Object>> list = ImmutableList.of(new HashMap<>());
        return ImmutableList.of(list);
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,List<Long>> getDeeplyEmptyMapList_IncludeNonEmpty() {
        return ImmutableMap.of(12L, new ArrayList<>());
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,Long[]> getDeeplyEmptyMapArray_IncludeNonEmpty() {
        return ImmutableMap.of(12L, new Long[0]);
      }

      // Non empty objects, annotation NON_EMPTY
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Long[] getNonEmptyLongArray_IncludeNonEmpty() {
        return new Long[] {12L};
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<Long> getNonEmptyLongList_IncludeNonEmpty() {
        return ImmutableList.of(12L);
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,Object> getNonEmptyMap_IncludeNonEmpty() {
        return ImmutableMap.of(12L, "");
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public List<List<Map<Long,Object>>> getDeeplyNonEmptyLongList_IncludeNonEmpty() {
        List<Map<Long,Object>> list = ImmutableList.of(ImmutableMap.of(12L, ""));
        return ImmutableList.of(list);
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,List<Long>> getDeeplyNonEmptyMapList_IncludeNonEmpty() {
        return ImmutableMap.of(12L, ImmutableList.of(23L));
      }
      @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
      public Map<Long,Long[]> getDeeplyNonEmptyMapArray_IncludeNonEmpty() {
        return ImmutableMap.of(12L, new Long[] {23L});
      }
    };
    ObjectNode output = testTypeChangesAsString(value);
    assertTrue(output.path("longNull").isMissingNode());
    assertTrue(output.path("stringNull").isMissingNode());
    assertTrue(output.path("stringEmpty").isMissingNode());
    assertPathPresent("\"not empty\"", output.path("stringNotEmpty"));
    assertTrue(output.path("dateNull").isMissingNode());
    assertTrue(output.path("dateAndTimeNull").isMissingNode());

    assertTrue(output.path("simpleDateNull").isMissingNode());
    assertTrue(output.path("nullLongArray").isMissingNode());
    assertTrue(output.path("nullLongList").isMissingNode());
    assertTrue(output.path("emptyLongArray").isMissingNode());
    assertTrue(output.path("emptyLongList").isMissingNode());
    assertTrue(output.path("emptyMap").isMissingNode());
    assertTrue(output.path("deeplyEmptyLongList").isMissingNode());
    assertTrue(output.path("deeplyEmptyMapList").isMissingNode());
    assertTrue(output.path("deeplyEmptyMapArray").isMissingNode());

    assertPathPresent("null", output.path("simpleDateNull_IncludeAlways"));
    assertPathPresent("null", output.path("nullLongArray_IncludeAlways"));
    assertPathPresent("null", output.path("nullLongList_IncludeAlways"));
    assertPathPresent("[]", output.path("emptyLongArray_IncludeAlways"));
    assertPathPresent("[]", output.path("emptyLongList_IncludeAlways"));
    assertPathPresent("{}", output.path("emptyMap_IncludeAlways"));
    assertPathPresent("[[{}]]", output.path("deeplyEmptyLongList_IncludeAlways"));
    assertPathPresent("{\"12\":[]}", output.path("deeplyEmptyMapList_IncludeAlways"));
    assertPathPresent("{\"12\":[]}", output.path("deeplyEmptyMapArray_IncludeAlways"));

    assertTrue(output.path("simpleDateNull_IncludeNonNull").isMissingNode());
    assertTrue(output.path("nullLongArray_IncludeNonNull").isMissingNode());
    assertTrue(output.path("nullLongList_IncludeNonNull").isMissingNode());
    assertPathPresent("[]", output.path("emptyLongArray_IncludeNonNull"));
    assertPathPresent("[]", output.path("emptyLongList_IncludeNonNull"));
    assertPathPresent("{}", output.path("emptyMap_IncludeNonNull"));
    assertPathPresent("[[{}]]", output.path("deeplyEmptyLongList_IncludeNonNull"));
    assertPathPresent("{\"12\":[]}", output.path("deeplyEmptyMapList_IncludeNonNull"));
    assertPathPresent("{\"12\":[]}", output.path("deeplyEmptyMapArray_IncludeNonNull"));

    assertTrue(output.path("simpleDateNull_IncludeNonEmpty").isMissingNode());
    assertTrue(output.path("nullLongArray_IncludeNonEmpty").isMissingNode());
    assertTrue(output.path("nullLongList_IncludeNonEmpty").isMissingNode());
    assertTrue(output.path("emptyLongArray_IncludeNonEmpty").isMissingNode());
    assertTrue(output.path("emptyLongList_IncludeNonEmpty").isMissingNode());
    assertTrue(output.path("emptyMap_IncludeNonEmpty").isMissingNode());
    assertTrue(output.path("deeplyEmptyLongList_IncludeNonEmpty").isMissingNode());
    assertTrue(output.path("deeplyEmptyMapList_IncludeNonEmpty").isMissingNode());
    assertTrue(output.path("deeplyEmptyMapArray_IncludeNonEmpty").isMissingNode());

    assertPathPresent("[\"12\"]", output.path("nonEmptyLongArray_IncludeNonEmpty"));
    assertPathPresent("[\"12\"]", output.path("nonEmptyLongList_IncludeNonEmpty"));
    assertPathPresent("{\"12\":\"\"}", output.path("nonEmptyMap_IncludeNonEmpty"));
    assertPathPresent("[[{\"12\":\"\"}]]", output.path("deeplyNonEmptyLongList_IncludeNonEmpty"));
    assertPathPresent("{\"12\":[\"23\"]}", output.path("deeplyNonEmptyMapList_IncludeNonEmpty"));
    assertPathPresent("{\"12\":[\"23\"]}", output.path("deeplyNonEmptyMapArray_IncludeNonEmpty"));
  }

  private void assertPathPresent(String expectedString, JsonNode path) {
    assertFalse(path.isMissingNode());
    assertEquals(expectedString, path.toString());
  }

  @Test
  public void testTypeChangesInArrayAsString() throws Exception {
    Object[] array = new Object[]{100L, 200L};
    String responseBody = writeToResponse(array);

    ObjectNode output = ObjectMapperUtil.createStandardObjectMapper()
        .readValue(responseBody, ObjectNode.class);
    ArrayNode items = (ArrayNode) output.get("items");
    assertTrue(items.get(0).isTextual());
    assertEquals("100", items.get(0).asText());
    assertTrue(items.get(1).isTextual());
    assertEquals("200", items.get(1).asText());
  }

  @Test
  public void testWriteNull() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = new ServletResponseResultWriter(response, null);
    writer.write(null);
    assertEquals("", response.getContentAsString());
    assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
  }

  @SuppressWarnings("unused")
  public void testByteArrayAsBase64() throws Exception {
    Object value = new Object() {
      public byte[] getValues() {
        return new byte[]{1, 2, 3, 4};
      }
    };
    ObjectNode output = ObjectMapperUtil.createStandardObjectMapper()
        .readValue(writeToResponse(value), ObjectNode.class);
    assertEquals("AQIDBA==", output.path("values").asText());
  }

  @Test
  public void testWriteErrorResponseHeaders() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = new ServletResponseResultWriter(response, null);
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("name0", "value0");
    headers.put("name1", "value1");
    writer.writeError(new UnauthorizedException("message", "schema", headers));
    assertEquals("schema name0=value0, name1=value1", response.getHeader("WWW-Authenticate"));
  }

  @Test
  public void testPrettyPrint() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = new ServletResponseResultWriter(response, null,
        true /* prettyPrint */, true /* addContentLength */);
    writer.write(ImmutableMap.of("one", "two", "three", "four"));
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
        .readValue(writeToResponse(value), ObjectNode.class);
    assertEquals("AQIDBA==", output.path("blob").asText());
  }

  public enum TestEnum {
    TEST1, TEST2
  }

  @Test
  public void testEnumAsString() throws Exception {
    TestEnum value = TestEnum.TEST1;
    JsonNode output = ObjectMapperUtil.createStandardObjectMapper()
        .readValue(writeToResponse(value), JsonNode.class);
    assertEquals("TEST1", output.asText());
  }

  private ObjectNode testTypeChangesAsString(Object value) throws Exception {
    String responseBody = writeToResponse(value);
    ObjectNode output = ObjectMapperUtil.createStandardObjectMapper()
        .readValue(responseBody, ObjectNode.class);
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

  private String writeToResponse(Object value) throws IOException {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletResponseResultWriter writer = new ServletResponseResultWriter(response, null);
    writer.write(value);
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
    writer.write(unorderedMap);
    String content = response.getContentAsString();
    assertEquals("{a:\"value_a\"}", content);
  }
  
  //Customized writer: for response, the fields name has no quote. For error, they have.
  private ServletResponseResultWriter createCustomizedWriter(HttpServletResponse response) {
    ServletResponseResultWriter writer = new ServletResponseResultWriter(response, null) {
      @Override
      protected ObjectWriter configureWriter(ObjectWriter objectWriter) {
        return objectWriter.withoutFeatures(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
      }
    };
    return writer;
  }
}
