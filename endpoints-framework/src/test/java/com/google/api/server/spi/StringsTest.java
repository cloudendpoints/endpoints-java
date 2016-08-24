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
package com.google.api.server.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for Strings.
 */
@RunWith(JUnit4.class)
public class StringsTest {
  public void testIsEmptyOrWhitespace() {
    assertTrue(Strings.isEmptyOrWhitespace(null));
    assertTrue(Strings.isEmptyOrWhitespace(""));
    assertTrue(Strings.isEmptyOrWhitespace("  "));
    assertTrue(Strings.isEmptyOrWhitespace("\n"));
    assertTrue(Strings.isEmptyOrWhitespace("\t"));
    assertTrue(Strings.isEmptyOrWhitespace("\r"));
    assertFalse(Strings.isEmptyOrWhitespace(" x"));
  }

  @Test
  public void testIsEmtyOrNull() {
    assertTrue(Strings.isEmptyOrNull(null));
    assertTrue(Strings.isEmptyOrNull(ImmutableList.<String>of()));
    assertFalse(Strings.isEmptyOrNull(ImmutableList.of("abc")));
  }

  @Test
  public void testIsWhitelisted() {
    assertFalse(Strings.isWhitelisted(null, ImmutableList.<String>of("")));
    assertFalse(Strings.isWhitelisted(" ", ImmutableList.<String>of(" ")));
    assertFalse(Strings.isWhitelisted("abc", null));
    assertFalse(Strings.isWhitelisted("abc", ImmutableList.<String>of()));
    assertTrue(Strings.isWhitelisted("abc", ImmutableList.<String>of("abc", "def")));
    assertFalse(Strings.isWhitelisted("abc", ImmutableList.<String>of("def")));
  }

  @Test
  public void testStripLeadingSlash() {
    assertNull(Strings.stripLeadingSlash(null));
    assertEquals("string", Strings.stripLeadingSlash("string"));
    assertEquals(
        "stringWithLeadingSlash", Strings.stripLeadingSlash("/stringWithLeadingSlash"));
    assertEquals("", Strings.stripLeadingSlash(""));
  }

  @Test
  public void testStripTrailingSlash() {
    assertNull(Strings.stripLeadingSlash(null));
    assertEquals("string", Strings.stripTrailingSlash("string"));
    assertEquals(
        "stringWithTrailingSlash", Strings.stripTrailingSlash("stringWithTrailingSlash/"));
  }
}
