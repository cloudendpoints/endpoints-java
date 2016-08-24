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
package com.google.testapi;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.util.TestUtils;

import com.compat_tests.tictactoe.Tictactoe;
import com.compat_tests.tictactoe.model.FieldContainer;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * End-to-end tests for {@link TestEndpoint}, which implements a tictactoe api (in name only).
 */
public class TictactoeTest {
  private static final int INT = 1234;
  private static final long LONG = 1234L;
  private static final float FLOAT = 1.5f;
  private static final double DOUBLE = 5.2;
  private static final String STRING = "中文";
  private static final DateTime SIMPLE_DATE = new DateTime("2016-01-26");
  private static final DateTime DATE_TIME = new DateTime("2016-01-26T11:46:00Z");
  private static final DateTime JAVA_DATE = new DateTime("2016-01-26T11:52:00Z");
  private static final String ENUM_VALUE = "VALUE1";

  private Tictactoe api;

  @Before
  public void setUp() {
    api = TestUtils.configureApiClient(
        new Tictactoe.Builder(new NetHttpTransport(), new JacksonFactory(), null)).build();
  }

  @Test
  public void test() throws IOException {
    assertThat(api.testEndpoint().test().execute().getValue()).isEqualTo("x");
  }

  @Test
  public void echo() throws IOException {
    assertThat(api.testEndpoint().echo("test").execute().getValue()).isEqualTo("test");
  }

  @Test
  public void testParamsPath() throws IOException {
    checkResponse(api.testEndpoint().testParamsPath(INT, LONG, FLOAT, DOUBLE, true, "中文",
        SIMPLE_DATE, DATE_TIME, JAVA_DATE, ENUM_VALUE).execute());
  }

  @Test
  public void testParamsQuery() throws IOException {
    checkResponse(api.testEndpoint().testParamsQuery(true, JAVA_DATE, DATE_TIME, DOUBLE, FLOAT,
        LONG, ENUM_VALUE, INT, SIMPLE_DATE, STRING).execute());
  }

  @Test
  public void testPathParamCollision() throws IOException {
    assertThat(api.testEndpoint().getCollidingPath("test").execute().getValue()).isEqualTo("test");
    assertThat(api.testEndpoint().putCollidingPath("test").execute().getValue()).isEqualTo("test");
  }

  private void checkResponse(FieldContainer ret) {
    assertThat(ret.getAnInt()).isEqualTo(INT);
    assertThat(ret.getALong()).isEqualTo(LONG);
    assertThat(ret.getAFloat()).isWithin(0.0f).of(FLOAT);
    assertThat(ret.getADouble()).isWithin(0.0).of(DOUBLE);
    assertThat(ret.getABoolean()).isTrue();
    assertThat(ret.getAString()).isEqualTo(STRING);
    assertThat(ret.getASimpleDate()).isEqualTo(SIMPLE_DATE);
    assertThat(ret.getADateAndTime()).isEqualTo(DATE_TIME);
    assertThat(ret.getAnEnum()).isEqualTo(ENUM_VALUE);
  }
}
