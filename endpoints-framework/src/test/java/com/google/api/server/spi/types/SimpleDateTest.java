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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SimpleDate}. */
@RunWith(JUnit4.class)
public class SimpleDateTest {

  @Test
  public void testToGetters() {
    assertEquals(2013, new SimpleDate(2013, 5, 10).getYear());
    assertEquals(5, new SimpleDate(2013, 5, 10).getMonth());
    assertEquals(10, new SimpleDate(2013, 5, 10).getDay());
  }

  @Test
  public void testEquals() {
    assertEquals(new SimpleDate(2013, 5, 10), new SimpleDate(2013, 5, 10));
    assertThat(new SimpleDate(2013, 5, 10)).isNotEqualTo(new SimpleDate(2014, 5, 10));
    assertThat(new SimpleDate(2013, 5, 10)).isNotEqualTo(new SimpleDate(2013, 6, 10));
    assertThat(new SimpleDate(2013, 5, 10)).isNotEqualTo(new SimpleDate(2013, 5, 11));
  }

  @Test
  public void testHashCode() {
    assertEquals(new SimpleDate(2013, 5, 10).hashCode(), new SimpleDate(2013, 5, 10).hashCode());
    assertThat(new SimpleDate(2013, 5, 10).hashCode()).isNotEqualTo(
        new SimpleDate(2014, 5, 10).hashCode());
    assertThat(new SimpleDate(2013, 5, 10).hashCode()).isNotEqualTo(
        new SimpleDate(2013, 6, 10).hashCode());
    assertThat(new SimpleDate(2013, 5, 10).hashCode()).isNotEqualTo(
        new SimpleDate(2013, 5, 11).hashCode());
  }

  @Test
  public void testToString() {
    assertEquals("2013-05-10", new SimpleDate(2013, 5, 10).toString());
  }

  @Test
  public void testConstructor() {
    new SimpleDate(9999, 1, 1);
    new SimpleDate(1, 1, 1);
    new SimpleDate(2013, 1, 1);
    new SimpleDate(2013, 12, 1);
    new SimpleDate(2013, 1, 1);
    new SimpleDate(2013, 1, 31);
    constructInvalidDate(2013, 0, 1);
    constructInvalidDate(2013, 13, 1);
    constructInvalidDate(2013, 1, 0);
    constructInvalidDate(2013, 1, 32);
    constructInvalidDate(10000, 1, 1);
    constructInvalidDate(0, 1, 1);
  }

  private void constructInvalidDate(int year, int month, int day) {
    try {
      SimpleDate date = new SimpleDate(year, month, day);
      fail("Constructor should have thrown exception on bad date object " + date);
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
