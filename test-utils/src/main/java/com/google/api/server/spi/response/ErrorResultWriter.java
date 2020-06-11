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
import static org.junit.Assert.fail;

import com.google.api.server.spi.ServiceException;

import java.io.IOException;

/**
 * A fake {@link ResultWriter} that expects a failure.
 */
public class ErrorResultWriter implements ResultWriter {
  private final int expectedStatus;
  private final String expectedMessage;
  private final boolean checkMessage;
  private final boolean innerExceptionMessage;

  public ErrorResultWriter(int expectedStatus) {
    this.expectedStatus = expectedStatus;
    this.expectedMessage = null;
    this.checkMessage = false;
    this.innerExceptionMessage = false;
  }

  public ErrorResultWriter(int expectedStatus, String expectedMessage) {
    this(expectedStatus, expectedMessage, false);
  }

  public ErrorResultWriter(int expectedStatus, String expectedMessage,
      boolean innerExceptionMessage) {
    this.expectedStatus = expectedStatus;
    this.expectedMessage = expectedMessage;
    this.checkMessage = true;
    this.innerExceptionMessage = innerExceptionMessage;
  }

  @Override
  public void write(Object result, int status) throws IOException {
    fail("expected a ServiceException to be thrown");
  }

  @Override
  public void writeError(ServiceException e) throws IOException {
    assertThat(e.getStatusCode()).isEqualTo(expectedStatus);
    if (checkMessage) {
      assertThat(innerExceptionMessage ? e.getCause().getMessage() : e.getMessage())
          .isEqualTo(expectedMessage);
    }
  }
}
