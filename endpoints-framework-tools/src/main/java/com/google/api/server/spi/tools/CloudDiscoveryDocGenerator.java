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

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.common.annotations.VisibleForTesting;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link DiscoveryDocGenerator} using a cloud based API endpoint.
 */
public class CloudDiscoveryDocGenerator implements DiscoveryDocGenerator {

  private static final Logger logger = Logger.getLogger(CloudDiscoveryDocGenerator.class.getName());

  private final String discoveryApiRoot;

  /**
   * Creates a Discovery Document generator using a certain Discovery API.
   * @param discoveryApiRoot Root URL of the Discovery API to use, e.g.
   *     "https://guestbook.appspot.com/_ah/api"
   */
  public static DiscoveryDocGenerator using(String discoveryApiRoot) {
    return new CloudDiscoveryDocGenerator(discoveryApiRoot);
  }

  @VisibleForTesting
  CloudDiscoveryDocGenerator(String discoveryApiRoot) {
    this.discoveryApiRoot = discoveryApiRoot;
  }

  @Override
  public String generateDiscoveryDoc(String apiConfigJson, Format format) throws IOException {
    String url = discoveryApiRoot + "/discovery/v1/apis/generate/" +
        format.toString().toLowerCase();
    logger.log(Level.FINE, "url={0}", url);
    ObjectNode body = ObjectMapperUtil.createStandardObjectMapper().createObjectNode();
    body.put("config", apiConfigJson);
    logger.log(Level.FINE, "config={0}", apiConfigJson);
    return postRequest(url, body.toString());
  }
  @Override
  public String generateApiDirectory(List<String> apiConfigsJson) throws IOException {
    String url = discoveryApiRoot + "/discovery/v1/apis/generate/directory";
    ObjectNode requestJson = ObjectMapperUtil.createStandardObjectMapper().createObjectNode();
    ArrayNode configsArray = requestJson.putArray("configs");
    for (String apiConfigJson : apiConfigsJson) {
      configsArray.add(apiConfigJson);
    }
    String requestBody = requestJson.toString();
    return postRequest(url, requestBody);
  }

  @VisibleForTesting
  String postRequest(String url, String content) throws IOException {
    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest request = requestFactory.buildPostRequest(
        new GenericUrl(url), ByteArrayContent.fromString("application/json", content));
    request.setReadTimeout(60000);  // 60 seconds is the max App Engine request time
    HttpResponse response = request.execute();
    if (response.getStatusCode() >= 300) {
      throw new IOException("Failed to post API configuration to " + url);
    } else {
      return IoUtil.readStream(response.getContent());
    }
  }
}
