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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.BackendService.MessageEntry;
import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.tools.development.testing.LocalAppIdentityServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Tests for {@link BackendService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BackendServiceTest {

  private static final String API_CONFIG_0 = "apiConfig0";
  private static final String API_CONFIG_1 = "apiConfig1";

  private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
      new LocalAppIdentityServiceTestConfig(), new LocalMemcacheServiceTestConfig());
  @Mock private SystemService systemService;

  private static class RequestMatcher extends ArgumentMatcher<HTTPRequest> {
    private final HTTPRequest request;

    public RequestMatcher(HTTPRequest request) {
      this.request = request;
    }

    public static HTTPRequest eq(HTTPRequest request) {
      return argThat(new RequestMatcher(request));
    }

    @Override
    public boolean matches(Object arg0) {
      if (arg0 instanceof HTTPRequest) {
        HTTPRequest otherRequest = (HTTPRequest) arg0;
        return request.getURL().toString().equals(otherRequest.getURL().toString())
            && request.getMethod().equals(otherRequest.getMethod())
            && headerEquals(request.getHeaders(), otherRequest.getHeaders());
      }
      return false;
    }

    private boolean headerEquals(List<HTTPHeader> headers, List<HTTPHeader> headers2) {
      for (HTTPHeader header : headers) {
        for (HTTPHeader header2 : headers2) {
          if (header.getName().equals(header2.getName())
              && !header.getValue().equals(header2.getValue())) {
            return false;
          }
        }
      }
      return headers.size() == headers2.size();
    }
  }

  @Before
  public void setUp() throws Exception {
    helper.setUp();
  }

  @After
  public void tearDown() throws Exception {
    helper.tearDown();
  }

  @Test
  public void testGetApiConfigs() throws Exception {
    ImmutableMap<ApiKey, String> configs = ImmutableMap.of(
        new ApiKey("api0", "v1"), API_CONFIG_0, new ApiKey("api1", "v1"), API_CONFIG_1);

    when(systemService.getApiConfigs()).thenReturn(configs);

    BackendService backendService =
        new BackendService("1.2.3", systemService, new BackendProperties());

    Collection<String> apiConfigs = backendService.getApiConfigs("1.2.3");

    assertEquals(2, apiConfigs.size());
    assertThat(apiConfigs).containsExactly(API_CONFIG_0, API_CONFIG_1);
  }

  @Test
  public void testErrorIfAppRevisionNotMatching() throws InternalServerErrorException {
    BackendService endpoint = new BackendService("1.2.3", systemService, new BackendProperties());
    try {
      endpoint.getApiConfigs("1.2.4");
      fail("Should have failed because expected app revision different");
    } catch (BadRequestException e) {
      // expected
    }
  }

  @Test
  public void testSystemServiceConfigLoadError() throws Exception {
    when(systemService.getApiConfigs()).thenThrow(new ApiConfigException("bleh"));

    BackendService backendService =
        new BackendService("1.2.3", systemService, new BackendProperties());
    try {
      backendService.getApiConfigs("1.2.3");
      fail();
    } catch (InternalServerErrorException e) {
      // Expected.
    }
  }

  @Test
  public void testReloadApiConfigs() throws Exception {
    URLFetchService fetcher = Mockito.mock(URLFetchService.class);
    HTTPResponse response = Mockito.mock(HTTPResponse.class);
    HTTPRequest request = new HTTPRequest(new URL(
        "https://1.test/_ah/api/discovery/v1/apis/reload?appMinorVersion=2"),
        HTTPMethod.POST);
    AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();
    request.addHeader(new HTTPHeader("Content-Type", "application/json"));
    request.addHeader(new HTTPHeader("Authorization", "OAuth " + appIdentity.getAccessToken(
        ImmutableList.of(BackendService.CONFIG_RELOAD_SCOPE)).getAccessToken()));

    when(fetcher.fetch(RequestMatcher.eq(request))).thenReturn(response);
    when(response.getResponseCode()).thenReturn(200);

    BackendService.reloadApiConfigs("1.test", "2", fetcher);
    verify(fetcher, atLeastOnce()).fetch(Mockito.any(HTTPRequest.class));
    verify(response, atLeastOnce()).getResponseCode();
  }

  @Test
  public void testReloadApiConfigsFails() throws Exception {
    URLFetchService fetcher = Mockito.mock(URLFetchService.class);
    HTTPResponse response = Mockito.mock(HTTPResponse.class);

    when(fetcher.fetch(Mockito.any(HTTPRequest.class))).thenReturn(response);
    when(response.getResponseCode()).thenReturn(403);

    try {
      BackendService.reloadApiConfigs("1.test", "2", fetcher);
      fail("expected exception");
    } catch (ServiceUnavailableException e) {
      // expected
    }
    verify(fetcher, atLeastOnce()).fetch(Mockito.any(HTTPRequest.class));
    verify(response, atLeastOnce()).getResponseCode();
  }

  private int logMessagesCount = 0;

  @Test
  public void testLogMessages() {
    final MessageEntry[] messages = new MessageEntry[] {
        createMessageEntry("info", "INFO message"),
        createMessageEntry("warning", "WARNING message"),
        createMessageEntry("error", "SEVERE message"),
        createMessageEntry("abc", "INFO message")
    };

    BackendService backendService =
        new BackendService("1.2.3", systemService, new BackendProperties()) {

      @Override
      void log(Level level, String message) {
        assertEquals(messages[logMessagesCount].getMessage(), level.toString() + " message");
        logMessagesCount++;
      }
    };
    backendService.logMessages(messages);
    assertEquals(4, logMessagesCount);
  }

  @Test
  public void testGetHostnameWithMajorVersion() {
    assertEquals("guestbook.appspot.com",
        BackendService.getHostnameWithMajorVersion("guestbook.appspot.com", null));
    assertEquals("2-dot-guestbook.appspot.com",
        BackendService.getHostnameWithMajorVersion("guestbook.appspot.com", "2"));
  }

  @Test
  public void testGetProperties() {
    BackendProperties backendProperties = Mockito.mock(BackendProperties.class);
    when(backendProperties.getProjectNumber()).thenReturn(8675309L);
    when(backendProperties.getProjectId()).thenReturn("project");

    BackendService backendService = new BackendService("1.2.3", systemService, backendProperties);

    BackendService.Properties properties = backendService.getProperties();
    assertNotNull(properties);
    assertEquals(8675309L, properties.getProjectNumber());
    assertEquals("project", properties.getProjectId());
  }

  private MessageEntry createMessageEntry(String level, String message) {
    MessageEntry e = new MessageEntry();
    e.setLevel(level);
    e.setMessage(message);
    return e;
  }
}
