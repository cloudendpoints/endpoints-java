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


/**
 * Constants used by Endpoints SPI framework.
 */
public final class Constant {

  private Constant() {}

  /**
   * Name of property in response for a collection type of value.
   */
  public static final String ITEMS = "items";

  /**
   * Name of property of next page token.
   */
  public static final String NEXT_PAGE_TOKEN = "nextPageToken";

  /**
   * Name of property of error message in response.
   */
  public static final String ERROR_MESSAGE = "error_message";

  /**
   * Client id of Google APIs Explorer web app.
   */
  public static final String API_EXPLORER_CLIENT_ID = "292824132082.apps.googleusercontent.com";

  /**
   * Email scope, required for access token authentication
   */
  public static final String API_EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";

  /**
   * Skip clientId Check, ONLY work for access token request. Use String constant so this can be
   * used in annotation directly as @Api(clientIds = {Constant.SKIP_CLIENT_ID_CHECK},...)
   */
  public static final String SKIP_CLIENT_ID_CHECK = "*";

  /**
   * Friendly name to refer to Google ID token authentication with accounts.google.com issuer.
   */
  public static final String GOOGLE_ID_TOKEN_ALT = "google_id_token_legacy";

  /**
   * Google ID token authentication variant with https://accounts.google.com issuer.
   */
  public static final String GOOGLE_ID_TOKEN_NAME = "google_id_token";

  /**
   * Google JWKS URI
   */
  public static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v1/certs";

  /**
   * Google OAuth2 authentication URL 
   */
  public static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
  
}
