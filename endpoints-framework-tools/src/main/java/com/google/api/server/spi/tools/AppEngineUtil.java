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

import com.google.apphosting.utils.config.AppEngineWebXml;
import com.google.apphosting.utils.config.AppEngineWebXmlReader;
import com.google.apphosting.utils.config.AppYaml;
import com.google.common.base.Preconditions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * App Engine related utilities.
 */
public class AppEngineUtil {

  private interface AppPropertyReader {
    String readYaml(AppYaml yaml);
    String readXml(AppEngineWebXml xml);
  }

  /**
   * Path in a WAR layout to appengine-web.xml
   */
  private static final String APPENGINE_WEB_XML_PATH = "WEB-INF/appengine-web.xml";

  /**
   * Path in a WAR layout to app.yaml
   */
  private static final String APP_YAML_PATH = "WEB-INF/app.yaml";

  /**
   * Path in a staged WAR layout to app.yaml
   */
  private static final String STAGED_APP_YAML_PATH = "app.yaml";

  /**
   * Given the directory of an App Engine application, returns its application id by checking
   * WEB-INF/app.yaml first and then WEB-INF/appengine-web.xml.
   */
  public static String getApplicationId(String appDir) {
    return getAppProperty(appDir, new AppPropertyReader() {
      @Override
      public String readYaml(AppYaml yaml) {
        return yaml.getApplication();
      }

      @Override
      public String readXml(AppEngineWebXml xml) {
        return xml.getAppId();
      }
    });
  }

  /**
   * Given the directory of an App Engine application, returns its version by checking
   * war/WEB-INF/app.yaml first and then war/WEB-INF/appengine-web.xml.
   */
  public static String getApplicationVersion(String appDir) {
    return getAppProperty(appDir, new AppPropertyReader() {
      @Override
      public String readYaml(AppYaml yaml) {
        return yaml.getVersion();
      }

      @Override
      public String readXml(AppEngineWebXml xml) {
        return xml.getMajorVersionId();
      }
    });
  }

  /**
   * Given the directory of an App Engine Application, returns its default hostname by checking
   * app.yaml or appengine-web.xml.
   *
   * @return the default hostname if the app id is set, otherwise null
   */
  public static String getApplicationDefaultHostname(String appDir) {
    Preconditions.checkNotNull(appDir, "appDir");
    String appId = getApplicationId(appDir);
    if (appId == null) {
      return null;
    }
    return getDefaultHostnameFromAppId(appId);
  }

  /**
   * Returns the default hostname for an app id. Only supports appspot.com and googleplex.com.
   *
   * @throws IllegalArgumentException if a domain app that's not google.com or appId is null.
   */
  public static String getDefaultHostnameFromAppId(String appId) {
    Preconditions.checkNotNull(appId, "appId");
    int colon = appId.indexOf(":");
    if (colon >= 0) {
      String appName = appId.substring(colon + 1);
      if (appId.substring(0, colon).equals("google.com")) {
        return appName + ".googleplex.com";
      } else {
        throw new IllegalArgumentException("Invalid application id '" + appId + "'");
      }
    } else {
      return appId + ".appspot.com";
    }
  }

  private static String getAppProperty(String appDir, AppPropertyReader reader) {
    // try app.yaml first because it may be newer
    File appDirFile = new File(appDir);
    File appYamlFile = new File(appDirFile, APP_YAML_PATH);
    if (!appYamlFile.exists()) {
      appYamlFile = new File(appDirFile, STAGED_APP_YAML_PATH);
    }
    try {
      return AppYaml.parse(new FileReader(appYamlFile)).getApplication();
    } catch (FileNotFoundException e) {
      // Do nothing, see if we can fallback to appengine-web.xml
    }

    // try appengine-web.xml next
    return reader.readXml(new AppEngineWebXmlReader(appDir, APPENGINE_WEB_XML_PATH)
        .readAppEngineWebXml());
  }
}
