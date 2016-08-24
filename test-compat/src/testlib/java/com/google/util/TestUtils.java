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
package com.google.util;

import com.google.api.client.googleapis.services.AbstractGoogleClient;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Miscellaneous utilities for end-to-end tests.
 */
public class TestUtils {
  private static final String TEST_BACKEND_URL_PROPERTY = "test.backend.url";

  public static <T extends AbstractGoogleClient.Builder> T configureApiClient(T builder) {
    builder
        .setRootUrl(System.getProperty(TEST_BACKEND_URL_PROPERTY))
        .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
          @Override
          public void initialize(AbstractGoogleClientRequest<?> request) throws IOException {
            request.setDisableGZipContent(true);
          }
        })
        .setHttpRequestInitializer(new HttpRequestInitializer() {
          @Override
          public void initialize(HttpRequest request) throws IOException {
            request.setUnsuccessfulResponseHandler(new ErrorHandler());
          }
        });
    return builder;
  }

  private static class ErrorHandler implements HttpUnsuccessfulResponseHandler {
    @Override
    public boolean handleResponse(HttpRequest request, HttpResponse response, boolean supportsRetry)
        throws IOException {
      System.out.println(response.getStatusCode());
      BufferedReader in = new BufferedReader(new InputStreamReader(response.getContent()));
      String line;
      while ((line = in.readLine()) != null) {
        System.out.println(line);
      }
      return false;
    }
  }
}
