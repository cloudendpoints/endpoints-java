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

import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The reserved API backend service used for Google's own purposes.
 */
public class BackendService {

  private static final Logger logger = Logger.getLogger(BackendService.class.getName());
  private static final Logger frameworkLogger = Logger.getLogger("com.google.api.server.spi");

  @VisibleForTesting
  static final String CONFIG_RELOAD_SCOPE =
      "https://www.googleapis.com/auth/endpoints.config.reload";

  private final String minorVersion;
  private final SystemService systemService;
  private final BackendProperties backendProperties;

  public BackendService(SystemService systemService) {
    this(getMinorVersion(), systemService, new BackendProperties());
  }

  public BackendService(String minorVersion, SystemService systemService,
      BackendProperties backendProperties) {
    this.minorVersion = minorVersion;
    this.systemService = systemService;
    this.backendProperties = backendProperties;
  }

  private static String getMajorVersion() {
    if (!EnvUtil.isRunningOnAppEngine()) {
      // Version only exists on App Engine.
      return null;
    }

    Environment env = ApiProxy.getCurrentEnvironment();
    return env == null ? null : env.getVersionId().split("\\.")[0];
  }

  private static String getMinorVersion() {
    if (!EnvUtil.isRunningOnAppEngine()) {
      // Version only exists on App Engine.
      return null;
    }

    Environment env = ApiProxy.getCurrentEnvironment();
    return env == null ? null : env.getVersionId().split("\\.")[1];
  }

  /**
   * Reads all WEB-INF/*.api files and return their contents in a list of strings.
   * @param expectedAppRevision App revision client expects to see
   * @throws InternalServerErrorException
   * @throws BadRequestException
   */
  public Collection<String> getApiConfigs(@Named("appRevision") String expectedAppRevision)
      throws InternalServerErrorException, BadRequestException {
    if (minorVersion != null && expectedAppRevision != null) {
      if (!minorVersion.equals(expectedAppRevision)) {
        throw new BadRequestException("API backend's app revision '" + minorVersion +
            "' not the same as expected '" + expectedAppRevision + "'");
      }
    }

    try {
      return systemService.getApiConfigs().values();
    } catch (ApiConfigException e) {
      logger.log(Level.SEVERE, "Could not generate configuration.", e);
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Initiates a request to reload the current application's API configs. Does not work in local
   * development.
   * @throws ServiceUnavailableException
   */
  public static void reloadApiConfigs() throws ServiceUnavailableException, NotFoundException {
    if (!EnvUtil.isRunningOnAppEngine()) {
      // TODO: Write a generic reload implementation that works without App Engine.
      throw new NotFoundException("reload only available on App Engine");
    }

    reloadApiConfigs(getHostnameWithMajorVersion(getMajorVersion()), getMinorVersion(),
        URLFetchServiceFactory.getURLFetchService());
  }

  @VisibleForTesting
  static void reloadApiConfigs(String hostname, String minorVersion, URLFetchService fetcher)
      throws ServiceUnavailableException {
    if (hostname == null) {
      throw new ServiceUnavailableException("invalid hostname");
    }
    List<String> scopes = new ArrayList<String>();
    scopes.add(CONFIG_RELOAD_SCOPE);
    AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();
    AppIdentityService.GetAccessTokenResult accessToken = appIdentity.getAccessToken(scopes);
    try {
      String reloadUrl = "https://" + hostname + "/_ah/api/discovery/v1/apis/reload"
          + makeReloadQueryParameters(minorVersion);
      URL url = new URL(reloadUrl);
      HTTPRequest reloadRequest = new HTTPRequest(url, HTTPMethod.POST);
      reloadRequest.addHeader(new HTTPHeader("Content-Type", "application/json"));
      reloadRequest.addHeader(
          new HTTPHeader("Authorization", "OAuth " + accessToken.getAccessToken()));
      HTTPResponse reloadResponse = fetcher.fetch(reloadRequest);
      if (reloadResponse.getResponseCode() != 200) {
        throw new ServiceUnavailableException(
            "Request returned HTTP " + reloadResponse.getResponseCode());
      }
    } catch (IOException e) {
      throw new ServiceUnavailableException(e);
    }
  }

  private static String getHostnameWithMajorVersion(String majorVersion) {
    return getHostnameWithMajorVersion(getDefaultHostname(), majorVersion);
  }

  // VisibleForTesting
  static String getHostnameWithMajorVersion(String defaultHostname, String majorVersion) {
    String hostnamePrefix = majorVersion != null ? majorVersion + "-dot-" : "";
    return hostnamePrefix + defaultHostname;
  }

  private static String getDefaultHostname() {
    Environment env = ApiProxy.getCurrentEnvironment();
    return env != null ? (String) env.getAttributes()
        .get("com.google.appengine.runtime.default_version_hostname")
        : null;
  }

  private static String makeReloadQueryParameters(String minorVersion) {
    if (minorVersion != null) {
      return "?appMinorVersion=" + minorVersion;
    }
    return "";
  }

  @VisibleForTesting
  static class MessageEntry {
    private String level;
    private String message;

    public String getLevel() {
      return level;
    }

    public void setLevel(String level) {
      this.level = level;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }

  private static final Map<String, Level> levelMap = new HashMap<String, Level>() {{
    put("info", Level.INFO);
    put("warning", Level.WARNING);
    put("error", Level.SEVERE);
  }};

  /**
   * Writes an array of messages to local log, each specified at its own level.
   */
  public void logMessages(@Named("messages") MessageEntry[] messages) {
    for (MessageEntry entry : messages) {
      Level level = levelMap.get(entry.getLevel());
      log(level == null ? Level.INFO : level, entry.getMessage());
    }
  }

  /**
   * Resource object for general backend properties.
   */
  public static class Properties {
    private long projectNumber;
    private String projectId;

    public Properties(long projectNumber, String projectId) {
      this.projectNumber = projectNumber;
      this.projectId = projectId;
    }

    public long getProjectNumber() {
      return projectNumber;
    }

    public String getProjectId() {
      return projectId;
    }
  }

  /**
   * Used to retrieve general properties of the backend such as expected project id.
   */
  public Properties getProperties() {
    return new Properties(backendProperties.getProjectNumber(), backendProperties.getProjectId());
  }

  @VisibleForTesting
  void log(Level level, String message) {
    if (EnvUtil.isRunningOnAppEngine()) {
      logWithAppEngine(level, message);
    } else {
      logLocal(level, message);
    }
  }

  private static final Map<Level, ApiProxy.LogRecord.Level> internalLevels =
      new HashMap<Level, ApiProxy.LogRecord.Level>() {{
    put(Level.INFO, ApiProxy.LogRecord.Level.info);
    put(Level.WARNING, ApiProxy.LogRecord.Level.warn);
    put(Level.SEVERE, ApiProxy.LogRecord.Level.error);
  }};

  private void logWithAppEngine(Level level, String message) {
    ApiProxy.LogRecord.Level internalLevel = internalLevels.get(level);
    ApiProxy.LogRecord record = new ApiProxy.LogRecord(
        internalLevel == null ? ApiProxy.LogRecord.Level.info : internalLevel,
        System.currentTimeMillis() * 1000, message);
    ApiProxy.log(record);
  }

  private void logLocal(Level level, String message) {
    // These messages are not coming from BackendService, but from the server.  Log them as coming
    // from the framework as a whole instead of from this specific method or class.
    frameworkLogger.logp(level, null, null, message);
  }
}
