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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpIOExceptionHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.Key;
import com.google.api.server.spi.Client;
import com.google.api.server.spi.Constant;
import com.google.api.server.spi.Strings;
import com.google.api.server.spi.request.Attribute;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * Common auth utils for built-in authenticators.
 */
public class GoogleAuth {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  // Identifies JSON Web Tokens
  // From java/com/google/gaia/client/AuthSubRequestDetector.java
  private static final String BASE64_REGEX = "[a-zA-Z0-9+/=_-]{6,}+";
  private static final Pattern JWT_PATTERN =
      Pattern.compile(String.format("%s\\.%s\\.%s", BASE64_REGEX, BASE64_REGEX, BASE64_REGEX));

  // Remote API for validating access or id token.
  private static final String TOKEN_INFO_ENDPOINT =
      "https://www.googleapis.com/oauth2/v2/tokeninfo";

  @VisibleForTesting
  static final String AUTHORIZATION_HEADER = "Authorization";

  // See https://tools.ietf.org/html/rfc6750. "Bearer" is the new spec, "OAuth"
  // is for backward compatiblity.
  @VisibleForTesting
  static final String[] ALLOWED_AUTH_SCHEMES = {"Bearer", "OAuth"};

  // See https://tools.ietf.org/html/rfc6750. "access_token" is the new spec.
  // "bearer_token" is the old name for backward compatibility.
  @VisibleForTesting
  static final String[] BEARER_TOKEN_PARAMETER_NAMES = {"access_token", "bearer_token"};

  @VisibleForTesting
  static final String[] OAUTH2_TOKEN_PREFIXES = {"ya29.", "1/"};

  @VisibleForTesting
  static final List<String> SKIP_CLIENT_ID_CHECK_LIST =
      ImmutableList.of(Constant.SKIP_CLIENT_ID_CHECK);

  public static String getAuthToken(HttpServletRequest request) {
    if (request.getAttribute(Attribute.AUTH_TOKEN) == null) {
      String token = getAuthTokenFromHeader(request.getHeader(AUTHORIZATION_HEADER));
      if (token == null) {
        token = getAuthTokenFromQueryParameters(request);
      }
      request.setAttribute(Attribute.AUTH_TOKEN, token);
    }
    return (String) request.getAttribute(Attribute.AUTH_TOKEN);
  }

  private static String getAuthTokenFromQueryParameters(HttpServletRequest request) {
    for (String parameterName : BEARER_TOKEN_PARAMETER_NAMES) {
      String token = request.getParameter(parameterName);
      if (token != null) {
        return token;
      }
    }
    return null;
  }

  private static String getAuthTokenFromHeader(String authHeader) {
    String authScheme = matchAuthScheme(authHeader);
    if (authScheme == null || authScheme.length() >= authHeader.length()) {
      return null;
    } else {
      return authHeader.substring(authScheme.length() + 1);
    }
  }

  private static String matchAuthScheme(String authHeader) {
    if (authHeader == null) {
      return null;
    }
    for (String authScheme : ALLOWED_AUTH_SCHEMES) {
      if (authHeader.startsWith(authScheme)) {
        return authScheme;
      }
    }
    return null;
  }

  public static boolean isJwt(String token) {
    if (token == null) {
      return false;
    }
    return JWT_PATTERN.matcher(token).matches();
  }

  public static boolean isOAuth2Token(String token) {
    if (token == null) {
      return false;
    }
    String strippedToken = token.trim().replaceFirst("^['\"]", "");
    for (String prefix : OAUTH2_TOKEN_PREFIXES) {
      if (strippedToken.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the client id in auth token is whitelisted.
   *
   * @param clientId clientId
   * @param allowedClientIds list of whitelisted clientIds
   * @param allowSkipClientIdCheck true only for OAuth2 access token request.
   */
  static boolean checkClientId(String clientId, List<String> allowedClientIds,
      boolean allowSkipClientIdCheck) {
    if (Strings.isWhitelisted(clientId, allowedClientIds)) {
      return true;
    }
    if (allowSkipClientIdCheck && !Strings.isEmptyOrNull(allowedClientIds)
        && allowedClientIds.equals(SKIP_CLIENT_ID_CHECK_LIST)) {
      return true;
    }
    return false;
  }

  /**
   * Check if the audience in auth token is whitelisted. Audience is either equal to the audience
   * specified (Android case) or equal to the Client ID field (iOS, general OAuth cases).
   */
  static boolean checkAudience(String audience, List<String> allowedAudiences, String clientId) {
    if (Strings.isWhitelisted(audience, allowedAudiences)) {
      return true;
    }
    if (!Strings.isEmptyOrWhitespace(audience) && audience.equals(clientId)) {
      return true;
    }
    return false;
  }

  /**
   * Class to hold remote access token validation response.
   */
  public static class TokenInfo {
    @Key("email") public String email;
    @Key("issued_to") public String clientId;
    @Key("scope") public String scopes;
    @Key("user_id") public String userId;
    @Key("audience") public String audience;
    @Key("expires_in") public Integer expiresIn;
    @Key("verified_email") public Boolean verifiedEmail;
    @Key("error_description") public String errorDescription;
  }

  /**
   * Get OAuth2 token info from remote token validation API.
   * Retries IOExceptions and 5xx responses once.
   */
  public static TokenInfo getTokenInfoRemote(String token) throws ServiceUnavailableException {
    try {
      String tokenParam;
      if (isOAuth2Token(token)) {
        tokenParam = "?access_token=";
      } else if(isJwt(token)) {
        tokenParam = "?id_token=";
      } else {
        return null;
      }
      HttpRequest request = Client.getInstance().getJsonHttpRequestFactory()
          .buildGetRequest(new GenericUrl(TOKEN_INFO_ENDPOINT + tokenParam + token));
      configureErrorHandling(request);
      return parseTokenInfo(request);
    } catch (IOException e) {
      throw new ServiceUnavailableException("Failed to perform access token validation", e);
    }
  }

  @VisibleForTesting
  static TokenInfo parseTokenInfo(HttpRequest request)
      throws IOException, ServiceUnavailableException {
    HttpResponse response = request.execute();
    int statusCode = response.getStatusCode();
    TokenInfo info = response.parseAs(TokenInfo.class);
    if (statusCode != 200) {
      String errorDescription = "Unknown error";
      if (info != null && info.errorDescription != null) {
        errorDescription = info.errorDescription;
      }
      errorDescription += " (" + statusCode + ")";
      if (statusCode >= 500) {
        logger.atSevere().log("Error validating access token: %s", errorDescription);
        throw new ServiceUnavailableException("Failed to validate access token");
      }
      logger.atInfo().log("Invalid access token: %s", errorDescription);
      return null;
    }
    if (info == null || Strings.isEmptyOrWhitespace(info.email)) {
      logger.atWarning().log("Access token does not contain email scope");
      return null;
    }
    return info;
  }

  @VisibleForTesting
  static void configureErrorHandling(HttpRequest request) {
    request.setNumberOfRetries(1)
        .setThrowExceptionOnExecuteError(false)
        .setIOExceptionHandler(new HttpIOExceptionHandler() {
          @Override
          public boolean handleIOException(HttpRequest request, boolean supportsRetry) {
            return true; // consider all IOException as transient
          }
        })
        .setUnsuccessfulResponseHandler(new HttpUnsuccessfulResponseHandler() {
          @Override
          public boolean handleResponse(HttpRequest request, HttpResponse response,
              boolean supportsRetry) {
            return response.getStatusCode() >= 500; // only retry Google's backend errors
          }
        });
  }
}
