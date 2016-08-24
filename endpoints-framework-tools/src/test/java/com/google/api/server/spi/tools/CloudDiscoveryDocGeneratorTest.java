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
package com.google.api.server.spi.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.api.server.spi.tools.DiscoveryDocGenerator.Format;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Tests for {@link CloudDiscoveryDocGenerator}.
 */
@RunWith(JUnit4.class)
public class CloudDiscoveryDocGeneratorTest {

  private static final String DISCOVERY_API_ROOT = "https://discoveryApiRoot";
  private static final String TEST_API_CONFIG = "{\"test\":\"apiConfig\"}";
  private static final String TEST_API_CONFIG2 = "{\"test2\":\"apiConfig2\"}";
  private static final String TEST_RESPONSE = "{\"test\":\"response\"}";

  @Test
  public void testGenerateDiscoveryDocRest() throws IOException {
    testGenerateDiscoveryDoc(Format.REST);
  }

  @Test
  public void testGenerateDiscoveryDocRpc() throws IOException {
    testGenerateDiscoveryDoc(Format.RPC);
  }

  @Test
  public void testGenerateApiDirectory() throws IOException {
    DiscoveryDocGenerator generator = new CloudDiscoveryDocGenerator(DISCOVERY_API_ROOT) {
      @Override
      String postRequest(String url, String content) throws IOException {
        assertEquals(DISCOVERY_API_ROOT + "/discovery/v1/apis/generate/directory", url);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = new ObjectMapper().readValue(content, Map.class);
        assertTrue(payload.get("configs") != null);
        assertTrue(content.startsWith("{\"configs\":[\"{\\\"test\\\":\\\"apiConfig\\\"}\","));
        return TEST_RESPONSE;
      }
    };
    ArrayList<String> apiConfigs = new ArrayList<>();
    apiConfigs.add(TEST_API_CONFIG);
    apiConfigs.add(TEST_API_CONFIG2);
    String discoveryDoc = generator.generateApiDirectory(apiConfigs);
    assertEquals(TEST_RESPONSE, discoveryDoc);
  }

  private void testGenerateDiscoveryDoc(final Format format) throws IOException {
    DiscoveryDocGenerator generator = new CloudDiscoveryDocGenerator(DISCOVERY_API_ROOT) {
      @Override
      String postRequest(String url, String content) throws IOException {
        assertEquals(DISCOVERY_API_ROOT + "/discovery/v1/apis/generate/" +
            format.toString().toLowerCase(), url);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = new ObjectMapper().readValue(content, Map.class);
        assertEquals(TEST_API_CONFIG, payload.get("config"));
        return TEST_RESPONSE;
      }
    };
    String discoveryDoc = generator.generateDiscoveryDoc(TEST_API_CONFIG, format);
    assertEquals(TEST_RESPONSE, discoveryDoc);
  }
}
