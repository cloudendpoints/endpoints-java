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
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.ServiceException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link RestResponseResultWriter}.
 */
@RunWith(JUnit4.class)
public class RestResponseResultWriterTest {
  @Test
  public void writeError_400() throws Exception {
    doWriteErrorTest(400 /* exceptionCode */, 400 /* expectedCompatCode */, "badRequest");
  }

  @Test
  public void writeError_401() throws Exception {
    doWriteErrorTest(401 /* exceptionCode */, 401 /* expectedCompatCode */, "required");
  }

  @Test
  public void writeError_402() throws Exception {
    doWriteErrorTest(402 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "paymentRequired" /* reason */,
        "error" /* message */);
  }

  @Test
  public void writeError_403() throws Exception {
    doWriteErrorTest(403 /* exceptionCode */, 403 /* expectedCompatCode */, "forbidden");
  }

  @Test
  public void writeError_404() throws Exception {
    doWriteErrorTest(404 /* exceptionCode */, 404 /* expectedCompatCode */, "notFound");
  }

  @Test
  public void writeError_405() throws Exception {
    doWriteErrorTest(405 /* exceptionCode */, 501 /* expectedCompatCode */, "unsupportedMethod");
  }

  @Test
  public void writeError_406() throws Exception {
    doWriteErrorTest(406 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "notAcceptable" /* reason */,
        "error" /* message */);
  }

  @Test
  public void writeError_407() throws Exception {
    doWriteErrorTest(407 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "proxyAuthentication" /* reason */,
        "error" /* message */);
  }

  @Test
  public void writeError_408() throws Exception {
    doWriteErrorTest(408 /* exceptionCode */, 503 /* expectedCompatCode */,
        "backendError" /* compatReason */, "requestTimeout" /* reason */, "error" /* message */);
  }

  @Test
  public void writeError_409() throws Exception {
    doWriteErrorTest(409 /* exceptionCode */, 409 /* expectedCompatCode */, "conflict");
  }

  @Test
  public void writeError_410() throws Exception {
    doWriteErrorTest(410 /* exceptionCode */, 410 /* expectedCompatCode */, "deleted");
  }

  @Test
  public void writeError_411() throws Exception {
    doWriteErrorTest(411 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "lengthRequired" /* reason */,
        "error" /* message */);
  }

  @Test
  public void writeError_412() throws Exception {
    doWriteErrorTest(412 /* exceptionCode */, 412 /* expectedCompatCode */, "conditionNotMet");
  }

  @Test
  public void writeError_413() throws Exception {
    doWriteErrorTest(413 /* exceptionCode */, 413 /* expectedCompatCode */, "uploadTooLarge");
  }

  @Test
  public void writeError_414() throws Exception {
    doWriteErrorTest(414 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "uriTooLong" /* reason */, "error" /* message */);
  }

  @Test
  public void writeError_415() throws Exception {
    doWriteErrorTest(415 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "unsupportedMediaType", "error" /* message */);
  }

  @Test
  public void writeError_416() throws Exception {
    doWriteErrorTest(416 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "rangeNotSatisfiable", "error" /* message */);
  }

  @Test
  public void writeError_417() throws Exception {
    doWriteErrorTest(417 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "expectationFailed", "error" /* message */);
  }

  @Test
  public void writeError_500s() throws Exception {
    int[] codes = {500, 501, 502, 503, 504, 505};
    for (int code : codes) {
      doWriteErrorTest(code /* exceptionCode */, 503 /* expectedCompatCode */, "backendError");
    }
  }

  @Test
  public void writeError_nullMessage() throws Exception {
    doWriteErrorTest(500 /* exceptionCode */, 503 /* expectedCompatCode */,
        "backendError" /* compatReason */, "backendError" /* reason */, null);
  }

  /**
   * Tests that an error is translated according to Lily if specified, and the code is left alone
   * if compatibility mode is off. Both cases test for the correct error structure in the response.
   */
  private void doWriteErrorTest(int exceptionCode, int expectedCompatCode, String reason)
      throws Exception {
    doWriteErrorTest(exceptionCode, expectedCompatCode, reason, reason, "error");
  }

  /**
   * Tests that an error is translated according to Lily if specified, and the code is left alone
   * if compatibility mode is off. Both cases test for the correct error structure in the response.
   */
  private void doWriteErrorTest(int exceptionCode, int expectedCompatCode, String compatReason,
      String reason, String message) throws Exception {
    writeError(exceptionCode, expectedCompatCode, compatReason, message, true);
    writeError(exceptionCode, exceptionCode, reason, message, false);
  }

  private void writeError(int exceptionCode, int expectedCode, String reason, String message,
      boolean enableExceptionCompatibility) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    RestResponseResultWriter writer = new RestResponseResultWriter(
        response, null, true /* prettyPrint */,
        true /* addContentLength */, enableExceptionCompatibility);
    writer.writeError(new ServiceException(exceptionCode, message));
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    ObjectNode content = mapper.readValue(response.getContentAsString(), ObjectNode.class);
    JsonNode outerError = content.path("error");
    assertThat(outerError.path("code").asInt()).isEqualTo(expectedCode);
    if (message == null) {
      assertThat(outerError.path("message").isNull()).isTrue();
    } else {
      assertThat(outerError.path("message").asText()).isEqualTo(message);
    }
    JsonNode innerError = outerError.path("errors").path(0);
    assertThat(innerError.path("domain").asText()).isEqualTo("global");
    assertThat(innerError.path("reason").asText()).isEqualTo(reason);
    if (message == null) {
      assertThat(innerError.path("message").isNull()).isTrue();
    } else {
      assertThat(innerError.path("message").asText()).isEqualTo(message);
    }
  }

  @Test
  public void writeError_CustomReasonAndDomain() throws Exception {
    writeError(false, null, "badRequest", null, "global");
    writeError(true, null, "badRequest", null, "global");
    writeError(false, "", "badRequest", "", "global");
    writeError(true, "", "badRequest", "", "global");
    writeError(false, "customReason", "customReason", "customDomain", "customDomain");
    writeError(true, "customReason", "customReason", "customDomain", "customDomain");
  }

  private void writeError(boolean enableExceptionCompatibility, String customReason, 
      String expectedReason, String customDomain, String expectedDomain) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    RestResponseResultWriter writer = new RestResponseResultWriter(
            response, null, true /* prettyPrint */,
        true /* addContentLength */, enableExceptionCompatibility);
    writer.writeError(new ServiceException(400, "error", customReason, customDomain));
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    ObjectNode content = mapper.readValue(response.getContentAsString(), ObjectNode.class);
    JsonNode innerError = content.path("error").path("errors").path(0);
    assertThat(innerError.path("domain").asText()).isEqualTo(expectedDomain);
    assertThat(innerError.path("reason").asText()).isEqualTo(expectedReason);
  }

  @Test
  public void writeError_extraFields() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    RestResponseResultWriter writer = new RestResponseResultWriter(response, null, true /* prettyPrint */,
        true /* addContentLength */, true /* enableExceptionCompatibility */);

    ServiceException serviceException = new ServiceException(400, "customMessage", "customReason", "customDomain");
    // Extra field string
    serviceException.putExtraField("someExtraString", "string1")
                    .putExtraField("someNullString", (String)null);
    // Extra field number
    serviceException.putExtraField("someExtraInt", Integer.valueOf(12))
                    .putExtraField("someExtraFloat", Float.valueOf(1.2f))
                    .putExtraField("someNullNumber", (Number)null);
    // Extra field boolean
    serviceException.putExtraField("someExtraTrue", TRUE)
                    .putExtraField("someExtraFalse", FALSE)
                    .putExtraField("someNullBoolean", (Boolean)null);
    // Extra field, keys are equals to reserved keywords when ignoring case
    serviceException.putExtraField("Domain", TRUE)
                    .putExtraField("REASON", Long.valueOf(1234567890))
                    .putExtraField("messAge", "hello world!");

    String expectedError = "{\"error\": {\"errors\": [{" +
        "  \"domain\": \"customDomain\"," +
        "  \"reason\": \"customReason\"," +
        "  \"message\": \"customMessage\"," +
        "  \"someExtraString\": \"string1\"," +
        "  \"someNullString\": null," +
        "  \"someExtraInt\": 12," +
        "  \"someExtraFloat\": 1.2," +
        "  \"someNullNumber\": null," +
        "  \"someExtraTrue\": true," +
        "  \"someExtraFalse\": false," +
        "  \"someNullBoolean\": null," +
        "  \"Domain\": true," +
        "  \"REASON\": \"1234567890\"," +
        "  \"messAge\": \"hello world!\"" +
        " }]," +
        " \"code\": 400," +
        " \"message\": \"customMessage\"" +
        "}}";

    writer.writeError(serviceException);
    JSONAssert.assertEquals(expectedError, response.getContentAsString(), true);
  }

  @Test
  public void writeError_extraFieldsUnsafe() throws Exception {

    MockHttpServletResponse response = new MockHttpServletResponse();
    RestResponseResultWriter writer = new RestResponseResultWriter(response, null, true /* prettyPrint */,
            true /* addContentLength */, true /* enableExceptionCompatibility */);

    TestServiceExceptionExtraFieldUnsafe serviceException = new TestServiceExceptionExtraFieldUnsafe(400, "customMessage", "customReason", "customDomain");

    // Extra field array
    Boolean[] booleans = new Boolean[] { TRUE, FALSE, TRUE };

    // Extra field List
    List<String> stringList = Arrays.asList("First", "Second", "Last");

    // Extra field Map
    Map<Integer, TestValue> map = new HashMap<>();
    map.put(1, new TestValue("Alice", 7, TestEnum.VALUE1));
    map.put(2, new TestValue("Bob", 12, TestEnum.VALUE2));
    map.put(3, new TestValue("Clark", 31, TestEnum.VALUE3));

    serviceException.putExtraFieldUnsafe("someExtraNull", null)
                    .putExtraFieldUnsafe("someExtraArray", booleans)
                    .putExtraFieldUnsafe("someExtraList", stringList)
                    .putExtraFieldUnsafe("someExtraMap", map);

    String expectedError = "{\"error\": {\"errors\": [{" +
            "  \"domain\": \"customDomain\"," +
            "  \"reason\": \"customReason\"," +
            "  \"message\": \"customMessage\"," +
            "  \"someExtraNull\": null," +
            "  \"someExtraArray\": [true, false, true]," +
            "  \"someExtraList\": [\"First\", \"Second\", \"Last\"]," +
            "  \"someExtraMap\": {" +
            "    \"1\": {\"name\": \"Alice\", \"age\": 7, \"testEnum\": \"VALUE1\"}," +
            "    \"2\": {\"name\": \"Bob\", \"age\": 12, \"testEnum\": \"VALUE2\"}," +
            "    \"3\": {\"name\": \"Clark\", \"age\": 31, \"testEnum\": \"VALUE3\"}" +
            "  }" +
            " }]," +
            " \"code\": 400," +
            " \"message\": \"customMessage\"" +
            "}}";

    writer.writeError(serviceException);
    JSONAssert.assertEquals(expectedError, response.getContentAsString(), true);
  }

  enum TestEnum {
    VALUE1, VALUE2, VALUE3;
  }

  class TestValue {
    public String name;
    public int age;
    public TestEnum testEnum;

    TestValue(String name, int age, TestEnum testEnum) {
      this.name = name;
      this.age = age;
      this.testEnum = testEnum;
    }
  }

  class TestServiceExceptionExtraFieldUnsafe extends ServiceException {

    TestServiceExceptionExtraFieldUnsafe(int statusCode, String statusMessage, String reason, String domain) {
      super(statusCode, statusMessage, reason, domain);
    }

    @Override
    public TestServiceExceptionExtraFieldUnsafe putExtraFieldUnsafe(String fieldName, Object value) {
      return (TestServiceExceptionExtraFieldUnsafe) super.putExtraFieldUnsafe(fieldName, value);
    }
  }
}
