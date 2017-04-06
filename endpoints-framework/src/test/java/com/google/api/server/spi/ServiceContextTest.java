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
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ServiceContext}.
 */
@RunWith(JUnit4.class)
public class ServiceContextTest {

  @Test
  public void testAppspotHost() {
    ServiceContext c = ServiceContext.create("abc", "xyz");
    assertEquals("abc.appspot.com", c.getAppHostname());
    assertEquals("xyz", c.getDefaultApiName());
  }

  @Test
  public void testGoogleplexHost() {
    ServiceContext c = ServiceContext.create("google.com:abc", "xyz");
    assertEquals("abc.googleplex.com", c.getAppHostname());
    assertEquals("xyz", c.getDefaultApiName());
  }

  @Test
  public void testInvalidDomain() {
    try {
      ServiceContext c = ServiceContext.create("123.com:abc", "xyz");
      fail("invalid domain should have failed");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testEmptyAppId() {
    ServiceContext c = ServiceContext.create("", "xyz");
    assertEquals("myapp.appspot.com", c.getAppHostname());
    assertEquals("xyz", c.getDefaultApiName());
  }

  @Test
  public void testGetTransferProtocol() {
    ServiceContext c = ServiceContext.create();
    assertEquals("https", c.getTransferProtocol());
  }
}
