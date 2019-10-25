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
package com.google.api.server.spi.discovery;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.services.discovery.model.DirectoryList;
import com.google.api.services.discovery.model.RestDescription;
import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests for {@link ProxyingDiscoveryService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProxyingDiscoveryServiceTest {
  private static final String SERVER_NAME = "localhost";
  private static final int SERVER_PORT = 8080;
  private static final String BASE_PATH = "/api/";
  private static final String SERVER_ROOT = "http://localhost:8080/api/";
  private static final String API_NAME = "tictactoe";
  private static final String API_VERSION = "v1";
  private static final RestDescription REST_DOC = new RestDescription()
      .setName(API_NAME)
      .setVersion(API_VERSION);
  private static final DirectoryList DIRECTORY = new DirectoryList()
      .setItems(ImmutableList.of(new DirectoryList.Items()
          .setName(API_NAME)
          .setVersion(API_VERSION)));

  @Mock private DiscoveryProvider provider;

  @Test
  public void getRestDocument() throws Exception {
    ProxyingDiscoveryService discoveryService = createDiscoveryService(true);
    when(provider.getRestDocument(SERVER_ROOT, API_NAME, API_VERSION)).thenReturn(REST_DOC);

    RestDescription actual = discoveryService.getRestDocument(
        createRequest("discovery/v1/apis/tictactoe/v1/rest"), API_NAME, API_VERSION);

    assertThat(actual).isEqualTo(REST_DOC);
  }

  @Test
  public void getRestDocument_notFound() throws Exception {
    ProxyingDiscoveryService discoveryService = createDiscoveryService(true);
    when(provider.getRestDocument(SERVER_ROOT, API_NAME, API_VERSION))
        .thenThrow(new NotFoundException(""));

    try {
      discoveryService.getRestDocument(
          createRequest("discovery/v1/apis/tictactoe/v1/rest"), API_NAME, API_VERSION);
      fail("expected NotFoundException");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void getRestDocument_internalServerError() throws Exception {
    ProxyingDiscoveryService discoveryService = createDiscoveryService(true);
    when(provider.getRestDocument(SERVER_ROOT, API_NAME, API_VERSION))
        .thenThrow(new InternalServerErrorException(""));

    try {
      discoveryService.getRestDocument(
          createRequest("discovery/v1/apis/tictactoe/v1/rest"), API_NAME, API_VERSION);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  @Test
  public void getRestDocument_uninitialized() throws Exception {
    try {
      ProxyingDiscoveryService discoveryService = createDiscoveryService(false);
      discoveryService.getRestDocument(null /* request */, null /* name */, null /* verson */);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  @Test
  public void getApiList() throws Exception {
    ProxyingDiscoveryService discoveryService = createDiscoveryService(true);
    when(provider.getDirectory(SERVER_ROOT)).thenReturn(DIRECTORY);

    DirectoryList actual = discoveryService.getApiList(createRequest("discovery/v1/apis"));

    assertThat(actual).isEqualTo(DIRECTORY);
  }

  @Test
  public void getApiList_internalServerError() throws Exception {
    ProxyingDiscoveryService discoveryService = createDiscoveryService(true);
    when(provider.getDirectory(SERVER_ROOT))
        .thenThrow(new InternalServerErrorException(""));

    try {
      discoveryService.getApiList(createRequest("discovery/v1/apis"));
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  @Test
  public void getApiList_uninitialized() throws Exception {
    try {
      ProxyingDiscoveryService discoveryService = createDiscoveryService(false);
      discoveryService.getApiList(null /* request */);
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  @Test
  public void getActualRoot_badRoot() throws Exception {
    try {
      ProxyingDiscoveryService.getActualRoot(createRequest("badroot"));
      fail("expected InternalServerErrorException");
    } catch (InternalServerErrorException e) {
      // expected
    }
  }

  private ProxyingDiscoveryService createDiscoveryService(boolean initialize) {
    ProxyingDiscoveryService discoveryService = new ProxyingDiscoveryService();
    if (initialize) {
      discoveryService.initialize(provider);
    }
    return discoveryService;
  }

  private static MockHttpServletRequest createRequest(String apiPath) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(BASE_PATH + apiPath);
    request.setServerName(SERVER_NAME);
    request.setServerPort(SERVER_PORT);
    return request;
  }
}
