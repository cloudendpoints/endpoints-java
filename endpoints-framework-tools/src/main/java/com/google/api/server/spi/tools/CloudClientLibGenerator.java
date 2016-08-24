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
import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of (@link ClientLibGenerator} using cloud service.
 */
public class CloudClientLibGenerator implements ClientLibGenerator {

  public static final String BOUNDARY_PREFIX = "----GoogleApisClientBoundary";

  private final String clientLibGenApiUrl;

  public static ClientLibGenerator using(String clientLibGenApiUrl) {
    return new CloudClientLibGenerator(clientLibGenApiUrl);
  }

  @VisibleForTesting
  CloudClientLibGenerator(String clientLibGenApiUrl) {
    this.clientLibGenApiUrl = clientLibGenApiUrl;
  }

  @Override
  public void generateClientLib(String discoveryDoc, String language, String languageVersion,
      String layout, File file) throws IOException {
    String boundary = getBoundary();
    String form = createForm(
        discoveryDoc, language.toLowerCase(), languageVersion, layout, boundary);
    InputStream jar = postRequest(clientLibGenApiUrl, boundary, form);
    copyJar(jar, file);
  }

  @VisibleForTesting
  InputStream postRequest(String url, String boundary, String content) throws IOException {
    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(url),
        ByteArrayContent.fromString("multipart/form-data; boundary=" + boundary, content));
    request.setReadTimeout(60000);  // 60 seconds is the max App Engine request time
    HttpResponse response = request.execute();
    if (response.getStatusCode() >= 300) {
      throw new IOException("Client Generation failed at server side: " + response.getContent());
    } else {
      return response.getContent();
    }
  }

  private String getBoundary() {
    return BOUNDARY_PREFIX + System.currentTimeMillis();
  }

  private String createForm(String content, String lang, String lv, String layout,
      String boundary) {
    StringBuilder sb = new StringBuilder("--" + boundary);
    addParam(sb, "lang", lang, boundary);
    addParam(sb, "lv", lv, boundary);
    addParam(sb, "content", content, boundary);
    addParam(sb, "layout", layout, boundary);
    return sb.toString();
  }

  private void addParam(StringBuilder sb, String name, String value, String boundary) {
    if (value != null) {
      sb.append("\nContent-Disposition: form-data; name=\"");
      sb.append(name);
      sb.append("\"\n\n");
      sb.append(value);
      sb.append("\n--");
      sb.append(boundary);
    }
  }

  @VisibleForTesting
  void copyJar(InputStream input, File output) throws IOException {
    IoUtil.copy(input, output);
  }
}
