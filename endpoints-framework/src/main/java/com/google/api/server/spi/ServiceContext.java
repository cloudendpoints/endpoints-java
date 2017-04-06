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
 * Provides context for a service class when its API configuration is being generated.
 */
public class ServiceContext {

  /** API name to use when it is not provided or inferred. */
  public static final String DEFAULT_API_NAME = "myapi";

  /** Application name to use when it is not provided or inferred. */
  public static final String DEFAULT_APP_NAME = "myapp";

  private final String defaultApiName;
  private final String appHostname;

  /**
   * Creates a service context with default application and API name.
   */
  public static ServiceContext create() {
    return create(DEFAULT_APP_NAME, DEFAULT_API_NAME);
  }

  /**
   * Creates a service context with a given default application and API name.
   * @param applicationId Default application id to use.  Format is either "&lt;appName&gt;" or
   *     "google.com:&lt;appName&gt;", where the host name of the former is
   *     "&lt;appName&gt;.appspot.com" and ther latter is "&lt;appName&gt;.googleplex.com".
   * @param apiName Default API name to use.
   */
  public static ServiceContext create(String applicationId, String apiName) {
    String hostname = null;
    String defaultApiName = null;
    if (applicationId == null || applicationId.trim().isEmpty()) {
      applicationId = DEFAULT_APP_NAME;
    }
    // For Endpoints runtime, appHostname is generated from App Engine environment directly. When
    // generating endpoints client library statically from annotated class, derive appHostname from
    // application id set in appengine-web.xml.
    int colon = applicationId.indexOf(":");
    if (colon >= 0) {
      String appName = applicationId.substring(colon + 1);
      defaultApiName = apiName == null ? appName : apiName;
      if (applicationId.substring(0, colon).equals("google.com")) {
        hostname = appName + ".googleplex.com";
      } else {
        throw new IllegalArgumentException("Invalid application id '" + applicationId + "'");
      }
    } else {
      defaultApiName = apiName == null ? applicationId : apiName;
      hostname = applicationId + ".appspot.com";
    }
    return new ServiceContext(hostname, defaultApiName);
  }

  public static ServiceContext createFromHostname(String hostname, String apiName) {
    return new ServiceContext(hostname, apiName);
  }

  private ServiceContext(String appHostname, String defaultApiName) {
    this.appHostname = appHostname;
    this.defaultApiName = defaultApiName;
  }

  public String getDefaultApiName() {
    return defaultApiName;
  }

  public String getAppHostname() {
    return appHostname;
  }

  public String getTransferProtocol() {
    // Return https when running on App Engine prod or when not running in App Engine environment
    // (dev server or app engine prod) at all. The latter case will happen when endpoints tool
    // generates client library statically. Otherwise return "http".
    return EnvUtil.isRunningOnAppEngineProd() || !EnvUtil.isRunningOnAppEngine() ? "https"
        : "http";
  }
}
