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
package com.google.api.server.spi.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.server.spi.auth.GoogleAuth.TokenInfo;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;

/**
 * Test for GoogleAuthUtil.
 */
@RunWith(JUnit4.class)
public class GoogleAuthTest {
  private static final String ACCESS_TOKEN = GoogleAuth.OAUTH2_TOKEN_PREFIXES[0] + "abc123";
  private static final String CLIENT_ID1 = "clientId1";
  private static final String CLIENT_ID2 = "clientId2";
  private static final ImmutableList<String> CLIENT_ID_LIST = ImmutableList.of(CLIENT_ID1);
  private static final String AUDIENCE1 = "audience1";
  private static final String AUDIENCE2 = "audience2";
  private static final ImmutableList<String> AUDIENCE_LIST = ImmutableList.of(AUDIENCE1);

  private static final String SAMPLE_CONTENT_WITHOUT_EMAIL = "{\n"
      + " \"issued_to\": \"123.apps.googleusercontent.com\",\n"
      + " \"audience\": \"123.apps.googleusercontent.com\",\n"
      + " \"scope\": \"https://www.googleapis.com/auth/xapi.zoo\",\n" + " \"expires_in\": 3581,\n"
      + " \"access_type\": \"online\"\n" + "}";

  private static final String SAMPLE_CONTENT_WITH_EMAIL = "{\n"
      + " \"issued_to\": \"123.apps.googleusercontent.com\",\n"
      + " \"audience\": \"123.apps.googleusercontent.com\",\n" + " \"user_id\": \"1234567\",\n"
      + " \"scope\": \"https://www.googleapis.com/auth/userinfo.email"
      + " https://www.googleapis.com/auth/xapi.zoo https://www.googleapis.com/auth/plus.me\",\n"
      + " \"expires_in\": 3574,\n" + " \"email\": \"dummy@gmail.com\",\n"
      + " \"verified_email\": true,\n" + " \"access_type\": \"online\"\n" + "}";

  @Test
  public void testIsJwt() {
    assertFalse(GoogleAuth.isJwt("ya29.abcdef"));
    assertFalse(GoogleAuth.isJwt("abcdef.abcdef"));
    assertFalse(GoogleAuth.isJwt("abc.abcdef.abcdef"));
    assertFalse(GoogleAuth.isJwt("abcdef.abc.abcdef"));
    assertFalse(GoogleAuth.isJwt("abcdef.abcdef.abc"));
    assertFalse(GoogleAuth.isJwt("abcdef.abcdef.abcdef.abcdef"));
    assertFalse(GoogleAuth.isJwt("abcdef.abcd*ef.abcdef"));
    assertTrue(GoogleAuth.isJwt("abcdef.abcdef.abcdef"));
  }

  @Test
  public void testIsOAuth2Token() {
    for (String prefix : GoogleAuth.OAUTH2_TOKEN_PREFIXES) {
      assertTrue(GoogleAuth.isOAuth2Token(prefix + "abc"));
      assertFalse(GoogleAuth.isOAuth2Token("x" + prefix + "abc"));
    }
  }

  @Test
  public void testGetAuthToken_fromHeaders() {
    for (String scheme : GoogleAuth.ALLOWED_AUTH_SCHEMES) {
      MockHttpServletRequest request = new MockHttpServletRequest();

      request.addHeader(GoogleAuth.AUTHORIZATION_HEADER, scheme + " some-value");
      assertEquals("some-value", GoogleAuth.getAuthToken(request));
    }
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(GoogleAuth.AUTHORIZATION_HEADER, "noSuchAuthScheme some-value");
    assertNull(GoogleAuth.getAuthToken(request));
  }

  @Test
  public void testGetAuthToken_queryParameter() {
    for (String parameterName : GoogleAuth.BEARER_TOKEN_PARAMETER_NAMES) {
      testGetAuthTokenFromQueryParameter(parameterName);
    }
  }

  private void testGetAuthTokenFromQueryParameter(String parameterName) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(parameterName, ACCESS_TOKEN);
    assertEquals(ACCESS_TOKEN, GoogleAuth.getAuthToken(request));
  }

  @Test
  public void testCheckClientId() {
    // Empty allowed list or client Id.
    assertFalse(GoogleAuth.checkClientId(CLIENT_ID1, ImmutableList.<String>of(), true));
    assertFalse(GoogleAuth.checkClientId(CLIENT_ID1, null, true));
    assertFalse(GoogleAuth.checkClientId("", CLIENT_ID_LIST, true));
    assertFalse(GoogleAuth.checkClientId(null, CLIENT_ID_LIST, true));

    // ["*"]
    assertTrue(
        GoogleAuth.checkClientId(CLIENT_ID1, GoogleAuth.SKIP_CLIENT_ID_CHECK_LIST, true));
    assertFalse(
        GoogleAuth.checkClientId(CLIENT_ID1, GoogleAuth.SKIP_CLIENT_ID_CHECK_LIST, false));

    // In or not in whitelist.
    assertTrue(GoogleAuth.checkClientId(CLIENT_ID1, CLIENT_ID_LIST, true));
    assertFalse(GoogleAuth.checkClientId(CLIENT_ID2, CLIENT_ID_LIST, true));
  }

  @Test
  public void testCheckAudience() {
    // Empty allowed list or audience.
    assertFalse(GoogleAuth.checkAudience(AUDIENCE1, ImmutableList.<String>of(), CLIENT_ID1));
    assertFalse(GoogleAuth.checkAudience(AUDIENCE1, null, CLIENT_ID1));
    assertFalse(GoogleAuth.checkAudience("", AUDIENCE_LIST, CLIENT_ID1));
    assertFalse(GoogleAuth.checkAudience(null, AUDIENCE_LIST, CLIENT_ID1));

    // In or not in whitelist.
    assertTrue(GoogleAuth.checkAudience(AUDIENCE1, AUDIENCE_LIST, CLIENT_ID1));
    assertFalse(GoogleAuth.checkAudience(AUDIENCE2, AUDIENCE_LIST, CLIENT_ID1));

    // Equals to client id.
    assertTrue(GoogleAuth.checkAudience(CLIENT_ID1, AUDIENCE_LIST, CLIENT_ID1));
  }

  @Test
  public void testParseTokenInfo_withEmail() throws Exception {
    HttpRequest request = constructHttpRequest(SAMPLE_CONTENT_WITH_EMAIL);
    TokenInfo info = GoogleAuth.parseTokenInfo(request);
    assertEquals("123.apps.googleusercontent.com", info.clientId);
    assertEquals("https://www.googleapis.com/auth/userinfo.email"
        + " https://www.googleapis.com/auth/xapi.zoo" + " https://www.googleapis.com/auth/plus.me",
        info.scopes);
    assertEquals("1234567", info.userId);
    assertEquals("dummy@gmail.com", info.email);
  }

  @Test
  public void testParseTokenInfo_withoutEmail() throws Exception {
    HttpRequest request = constructHttpRequest(SAMPLE_CONTENT_WITHOUT_EMAIL);
    assertNull(GoogleAuth.parseTokenInfo(request));
  }

  @Test
  public void testParseTokenInfo_with400() throws Exception {
    HttpRequest request = constructHttpRequest("{\"error_description\": \"Invalid Value\"}", 400);
    assertNull(GoogleAuth.parseTokenInfo(request));
  }

  @Test(expected = ServiceUnavailableException.class)
  public void testParseTokenInfo_with500() throws Exception {
    HttpRequest request = constructHttpRequest("{\"error_description\": \"Backend Error\"}", 500);
    GoogleAuth.parseTokenInfo(request);
  }

  private HttpRequest constructHttpRequest(final String content) throws IOException {
    return constructHttpRequest(content, 200);
  }

  private HttpRequest constructHttpRequest(final String content, final int statusCode) throws IOException {
    HttpTransport transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return new MockLowLevelHttpRequest() {
          @Override
          public LowLevelHttpResponse execute() throws IOException {
            MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
            result.setContentType("application/json");
            result.setContent(content);
            result.setStatusCode(statusCode);
            return result;
          }
        };
      }
    };
    HttpRequest httpRequest = transport.createRequestFactory().buildGetRequest(new GenericUrl("https://google.com")).setParser(new JsonObjectParser(new JacksonFactory()));
    GoogleAuth.configureErrorHandling(httpRequest);
    return httpRequest;
  }
}
