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

import com.google.api.server.spi.IoUtil;
import com.google.api.server.spi.ObjectMapperUtil;
import com.google.appengine.tools.util.Option;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Command to generated a client library from a Discovery document.
 */
public class GenClientLibAction extends EndpointsToolAction {

  // name of this command
  public static final String NAME = "gen-client-lib";

  private static final ObjectMapper mapper = ObjectMapperUtil.createStandardObjectMapper();

  private Option languageOption = makeLanguageOption();
  private Option outputOption = makeOutputOption();
  private Option buildSystemOption = makeBuildSystemOption();

  public GenClientLibAction() {
    super(NAME);
    setOptions(Arrays.asList(languageOption, outputOption, buildSystemOption));
    setShortDescription("Generates a client library");
    setHelpDisplayNeeded(false);
  }

  @Override
  public boolean execute() throws IOException {
    if (getArgs().size() != 1) {
      return false;
    }
    genClientLibFromFile(getLanguage(languageOption), getOutputPath(outputOption), getArgs().get(0),
        getBuildSystem(buildSystemOption));
    return true;
  }

  /**
   * Generates a client library for an API.
   * @param language Language of the client library.
   * @param outputDirPath Directory to write generated client library into
   * @param discoveryDocPath Path to Discovery doc file
   * @param buildSystem The build system to use for the client library
   */
  public Object genClientLibFromFile(String language, String outputDirPath, String discoveryDocPath,
      String buildSystem)
      throws IOException {
    String discoveryDoc = IoUtil.readFile(new File(discoveryDocPath));
    return genClientLib(language, outputDirPath, discoveryDoc, buildSystem);
  }

  /**
   * Generates a client library for an API.
   * @param language Language of the client library.
   * @param outputDirPath Directory to write generated client library into
   * @param discoveryDoc Discovery doc text.
   * @param buildSystem The build system to use for the client library
   */
  public Object genClientLib(String language, String outputDirPath, String discoveryDoc,
      String buildSystem)
      throws IOException {
    if (!isRestDiscoveryDoc(discoveryDoc)) {
      throw new IllegalArgumentException("discovery doc must be in rest format");
    }
    File outputFile = new File(outputDirPath + "/" + getApiNameVersion(discoveryDoc) + "-" +
        language.toLowerCase() + ".zip");
    ClientLibGenerator generator =
        CloudClientLibGenerator.using("https://google-api-client-libraries.appspot.com/generate");
    generator.generateClientLib(discoveryDoc, language, null, buildSystem, outputFile);
    System.out.println("API client library written to " + outputFile.getPath());
    return null;
  }

  private static boolean isRestDiscoveryDoc(String discoveryDoc) throws IOException {
    ObjectNode discoveryJson =
        ObjectMapperUtil.createStandardObjectMapper().readValue(discoveryDoc, ObjectNode.class);
    return "rest".equals(discoveryJson.get("protocol").asText());
  }

  private static String getApiNameVersion(String discoveryDoc)
      throws IOException {
    ObjectNode root = mapper.readValue(discoveryDoc, ObjectNode.class);
    return root.path("name").asText() + "-" + root.path("version").asText();
  }

  @Override
  public String getUsageString() {
    return NAME + " <options> <discovery doc file>";
  }
}
