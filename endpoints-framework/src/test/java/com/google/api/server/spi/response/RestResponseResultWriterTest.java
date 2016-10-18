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
import com.google.api.server.spi.ServiceException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletResponse;

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
        "unsupportedProtocol" /* compatReason */, "paymentRequired" /* reason */);
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
        "unsupportedProtocol" /* compatReason */, "notAcceptable" /* reason */);
  }

  @Test
  public void writeError_407() throws Exception {
    doWriteErrorTest(407 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "proxyAuthentication" /* reason */);
  }

  @Test
  public void writeError_408() throws Exception {
    doWriteErrorTest(408 /* exceptionCode */, 503 /* expectedCompatCode */,
        "backendError" /* compatReason */, "requestTimeout" /* reason */);
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
        "unsupportedProtocol" /* compatReason */, "lengthRequired" /* reason */);
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
        "unsupportedProtocol" /* compatReason */, "uriTooLong" /* reason */);
  }

  @Test
  public void writeError_415() throws Exception {
    doWriteErrorTest(415 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "unsupportedMediaType");
  }

  @Test
  public void writeError_416() throws Exception {
    doWriteErrorTest(416 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "rangeNotSatisfiable");
  }

  @Test
  public void writeError_417() throws Exception {
    doWriteErrorTest(417 /* exceptionCode */, 404 /* expectedCompatCode */,
        "unsupportedProtocol" /* compatReason */, "expectationFailed");
  }

  @Test
  public void writeError_500s() throws Exception {
    int[] codes = {500, 501, 502, 503, 504, 505};
    for (int code : codes) {
      doWriteErrorTest(code /* exceptionCode */, 503 /* expectedCompatCode */, "backendError");
    }
  }

  /**
   * Tests that an error is translated according to Lily if specified, and the code is left alone
   * if compatibility mode is off. Both cases test for the correct error structure in the response.
   */
  private void doWriteErrorTest(int exceptionCode, int expectedCompatCode, String reason)
      throws Exception {
    doWriteErrorTest(exceptionCode, expectedCompatCode, reason, reason);
  }

  /**
   * Tests that an error is translated according to Lily if specified, and the code is left alone
   * if compatibility mode is off. Both cases test for the correct error structure in the response.
   */
  private void doWriteErrorTest(int exceptionCode, int expectedCompatCode, String compatReason,
      String reason) throws Exception {
    writeError(exceptionCode, expectedCompatCode, compatReason, true);
    writeError(exceptionCode, exceptionCode, reason, false);
  }

  private void writeError(int exceptionCode, int expectedCode, String reason,
      boolean enableExceptionCompatibility) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    RestResponseResultWriter writer = new RestResponseResultWriter(
        response, null, true /* prettyPrint */, enableExceptionCompatibility);
    writer.writeError(new ServiceException(exceptionCode, "error"));
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    ObjectNode content = mapper.readValue(response.getContentAsString(), ObjectNode.class);
    JsonNode outerError = content.path("error");
    assertThat(outerError.path("code").asInt()).isEqualTo(expectedCode);
    assertThat(outerError.path("message").asText()).isEqualTo("error");
    JsonNode innerError = outerError.path("errors").path(0);
    assertThat(innerError.path("domain").asText()).isEqualTo("global");
    assertThat(innerError.path("reason").asText()).isEqualTo(reason);
    assertThat(innerError.path("message").asText()).isEqualTo("error");
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
            response, null, true /* prettyPrint */, enableExceptionCompatibility);
    writer.writeError(new ServiceException(400, "error", customReason, customDomain));
    ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();
    ObjectNode content = mapper.readValue(response.getContentAsString(), ObjectNode.class);
    JsonNode innerError = content.path("error").path("errors").path(0);
    assertThat(innerError.path("domain").asText()).isEqualTo(expectedDomain);
    assertThat(innerError.path("reason").asText()).isEqualTo(expectedReason);
  }
}
