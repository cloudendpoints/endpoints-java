/*
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.google.appengine.tools.util.Option;

/**
 * Command to generate a client API source code from a Discovery document.
 */
public class GenClientSrcAction extends EndpointsToolAction {

  // name of this command
  public static final String NAME = "gen-client-src";

  private Option languageOption = makeLanguageOption();
  private Option outputOption = makeOutputOption();

  public GenClientSrcAction() {
    super(NAME);
    setOptions(Arrays.asList(languageOption, outputOption));
    setShortDescription("Generates a client API source code");
    setHelpDisplayNeeded(false);
  }

  @Override
  public boolean execute() throws IOException {
    if (getArgs().size() != 1) {
      return false;
    }
    genClientSrcFromFile(getLanguage(languageOption), getOutputPath(outputOption), getArgs().get(0));
    return true;
  }

  /**
   * Generates a client library for an API.
   * @param language Language of the client library.
   * @param outputDirPath Directory to write generated client library into
   * @param discoveryDoc Discovery doc file
   */
  public Object genClientSrcFromFile(String language, String outputDirPath, String discoveryDoc)
      throws IOException {
    ClientLibGenerator generator = new LocalClientLibGenerator();
    generator.generateClientLib(discoveryDoc, language, "", "", new File(outputDirPath));
    return null;
  }

  @Override
  public String getUsageString() {
    return NAME + " <options> <discovery doc file>";
  }
}
