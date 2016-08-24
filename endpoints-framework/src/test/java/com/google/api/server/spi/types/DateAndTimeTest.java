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
package com.google.api.server.spi.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link DateAndTime}. */
@RunWith(JUnit4.class)
public class DateAndTimeTest {

  @Test
  public void testValidRfc3339() {
    String validTime = "1937-01-01T12:00:27+00:20";
    DateAndTime parsed = DateAndTime.parseRfc3339String(validTime);
    assertEquals(validTime, parsed.toRfc3339String());
    assertEquals(validTime, parsed.toString());
  }

  @Test
  public void testInvalidRfc3339() {
    try {
      DateAndTime.parseRfc3339String("1937-01");
      fail("Parsing string should have failed, it's not valid RFC3339");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testSimpleDate() {
    String simpleDate = "1937-01-01";
    DateAndTime parsed = DateAndTime.parseRfc3339String(simpleDate);
    assertEquals(simpleDate, parsed.toRfc3339String());
    assertEquals(simpleDate, parsed.toString());
  }

  @Test
  public void testEqualityAndHash() {
    String explicitUtc = "1996-12-19T16:39:57+00:00";
    String implicitUtc = "1996-12-19T16:39:57Z";
    String utcPlusOne = "1996-12-19T17:39:57+01:00";

    DateAndTime explicitUtcParsed = DateAndTime.parseRfc3339String(explicitUtc);
    DateAndTime implicitUtcParsed = DateAndTime.parseRfc3339String(implicitUtc);
    DateAndTime utcPlusOneParsed = DateAndTime.parseRfc3339String(utcPlusOne);
    assertEquals(explicitUtc, explicitUtcParsed.toRfc3339String());
    assertEquals(implicitUtc, implicitUtcParsed.toRfc3339String());
    assertEquals(utcPlusOne, utcPlusOneParsed.toRfc3339String());
    assertEquals(explicitUtcParsed, implicitUtcParsed);
    assertEquals(explicitUtcParsed.hashCode(), implicitUtcParsed.hashCode());
    assertFalse(explicitUtcParsed.equals(utcPlusOneParsed));
  }
}
