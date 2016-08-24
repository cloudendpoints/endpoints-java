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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.api.server.spi.tools.DiscoveryDocGenerator.Format;
import com.google.appengine.tools.util.Option;
import com.google.common.io.Files;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Commmand to generated a Discovery document from an API configuration file.
 */
public class GenDiscoveryDocAction extends EndpointsToolAction {

  // name of this command
  public static final String NAME = "gen-discovery-doc";

  /**
   * Root URL of discovery doc generation API.  This is on a host that Web APIs project owns so
   * that even if producer has not picked an App Engine app host, this call can still succeed.
   */
  public static final String DISCOVERY_GEN_ROOT = "https://webapis-discovery.appspot.com/_ah/api";

  private Option formatOption = makeFormatOption();

  private Option outputOption = makeOutputOption();

  public GenDiscoveryDocAction() {
    super(NAME);
    setOptions(Arrays.asList(formatOption, outputOption));
    setShortDescription("Generates API Discovery document");
    setHelpDisplayNeeded(false);
  }

  @Override
  public boolean execute() throws JsonParseException, JsonMappingException, IOException {
    if (getArgs().size() != 1) {
      return false;
    }
    Format format = Format.valueOf(getFormat(formatOption).toUpperCase());
    genDiscoveryDocFromFile(format, getOutputPath(outputOption), getArgs().get(0), true);
    return true;
  }

  /**
   * Generates the Discovery doc for an API.
   * @param format Either REST or RPC
   * @param outputDirPath Directory to write generated Discovery doc into
   * @param apiConfigFilePath Path to API configuration file
   * @return Discovery doc generated
   */
  public String genDiscoveryDocFromFile(Format format, String outputDirPath,
      String apiConfigFilePath, boolean outputToDisk) throws IOException {
    String apiConfigJson = IoUtil.readFile(new File(apiConfigFilePath));
    return genDiscoveryDoc(format, outputDirPath, apiConfigJson, outputToDisk);
  }

  /**
   * Generates the Discovery doc for an API.
   * @param format Either REST or RPC
   * @param outputDirPath Directory to write generated Discovery doc into
   * @param apiConfigJson JSON-formatted configuration for the API.
   * @return Discovery doc generated
   */
  public String genDiscoveryDoc(Format format, String outputDirPath, String apiConfigJson,
      boolean outputToDisk) throws IOException {
    File outputDir = new File(outputDirPath);
    if (!outputDir.isDirectory()) {
      throw new IllegalArgumentException(outputDirPath + " is not a directory");
    }
    @SuppressWarnings("unchecked")
    Map<String, ?> apiConfig = ObjectMapperUtil.createStandardObjectMapper()
        .readValue(apiConfigJson, Map.class);

    String name = getNonNullValue(apiConfig, "name");
    String version = getNonNullValue(apiConfig, "version");

    DiscoveryDocGenerator generator = CloudDiscoveryDocGenerator.using(getDiscoveryGenRoot());
    String discoveryDoc = generator.generateDiscoveryDoc(apiConfigJson, format);

    if (outputToDisk) {
      String discoveryDocFilePath = outputDir + "/" + name + "-" + version + "-" +
          format.toString().toLowerCase() + ".discovery";
      Files.write(discoveryDoc, new File(discoveryDocFilePath), UTF_8);
      System.out.println("API Discovery Document written to " + discoveryDocFilePath);
    }

    return discoveryDoc;
  }

  private String getDiscoveryGenRoot() {
    String discoveryGenRoot = System.getenv("DISCOVERY_GEN_ROOT");
    return discoveryGenRoot == null ? DISCOVERY_GEN_ROOT : discoveryGenRoot;
  }

  private static String getNonNullValue(Map<String, ?> apiConfig, String name) {
    String value = (String) apiConfig.get(name);
    if (value == null) {
      throw new IllegalArgumentException("API " + name + " cannot be null");
    }
    return value;
  }

  @Override
  public String getUsageString() {
    return NAME + " <options> <api config file>";
  }
}
