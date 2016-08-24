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

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.annotations.VisibleForTesting;

/**
 * HTTP Client Utilities.
 */
public class Client {
  private static final Client INSTANCE = new Client();

  private final HttpTransport transport;
  private final JsonFactory jsonFactory;
  private final HttpRequestFactory jsonHttpRequestFactory;

  @VisibleForTesting
  Client() {
    if (EnvUtil.isRunningOnAppEngineProd()) {
      transport = new UrlFetchTransport();
    } else {
      transport = new NetHttpTransport();
    }
    jsonFactory = new JacksonFactory();
    jsonHttpRequestFactory = transport.createRequestFactory(new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest request) {
        request.setParser(new JsonObjectParser(jsonFactory));
      }
    });
  }

  public static Client getInstance() {
    return INSTANCE;
  }

  public HttpTransport getHttpTransport() {
    return transport;
  }

  public JsonFactory getJsonFactory() {
    return jsonFactory;
  }

  public HttpRequestFactory getJsonHttpRequestFactory() {
    return jsonHttpRequestFactory;
  }
}
