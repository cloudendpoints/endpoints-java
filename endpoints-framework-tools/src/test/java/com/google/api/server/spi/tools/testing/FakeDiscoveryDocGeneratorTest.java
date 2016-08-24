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
package com.google.api.server.spi.tools.testing;

import static org.junit.Assert.assertNotNull;

import com.google.api.server.spi.tools.DiscoveryDocGenerator.Format;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.HashMap;

/**
 * Tests for {@link FakeDiscoveryDocGenerator}.
 */
@RunWith(JUnit4.class)
public class FakeDiscoveryDocGeneratorTest {

  @Test
  public void testGenerateRestDiscoveryDoc() throws IOException {
    String discoveryDoc = new FakeDiscoveryDocGenerator().generateDiscoveryDoc("", Format.REST);
    assertNotNull(discoveryDoc);
    assertNotNull(new ObjectMapper().readValue(discoveryDoc, HashMap.class));
  }
}
