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

import com.google.api.server.spi.config.ApiConfigException;
import com.google.appengine.tools.util.Action;
import com.google.appengine.tools.util.Option;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Base class for {@link Action}s used by the Cloud Endpoints command line tool. Users can use
 * Endpoints command line tool to generate client library bundles and discovery docs.
 */
public abstract class EndpointsToolAction extends Action {
  private static Logger log = Logger.getLogger(EndpointsToolAction.class.getName());

  private static final int MAX_WIDTH = 80;
  private static final int OPTION_INDENT = 2;
  private static final int OPTION_DESCRIPTION_INDENT = 2;

  // short name of class path option
  public static final String OPTION_CLASS_PATH_SHORT = "cp";

  // long name of class path option
  public static final String OPTION_CLASS_PATH_LONG = "classpath";

  // short name of output directory option
  public static final String OPTION_OUTPUT_DIR_SHORT = "o";

  // long name of output directory option
  public static final String OPTION_OUTPUT_DIR_LONG = "output";

  // short name of input war
  public static final String OPTION_WAR_SHORT = "w";

  // long name of input war
  public static final String OPTION_WAR_LONG = "war";

  // short name of the language option
  public static final String OPTION_LANGUAGE_SHORT = "l";

  // long name of the language option
  public static final String OPTION_LANGUAGE_LONG = "language";

  // short name of the build system option
  public static final String OPTION_BUILD_SYSTEM_SHORT = "bs";

  // long name of the build system option
  public static final String OPTION_BUILD_SYSTEM_LONG = "build-system";

  // short name of the format option
  public static final String OPTION_FORMAT_SHORT = "f";

  // long name of the format option
  public static final String OPTION_FORMAT_LONG = "format";

  public static final String OPTION_DEBUG = "debug";

  // short name of the format option
  public static final String OPTION_HOSTNAME_SHORT = "h";

  // long name of the format option
  public static final String OPTION_HOSTNAME_LONG = "hostname";

  // short name of the base path option
  public static final String OPTION_BASE_PATH_SHORT = "p";

  // long name of the base path option
  public static final String OPTION_BASE_PATH_LONG = "path";

  @VisibleForTesting
  static final String DEFAULT_WAR_PATH = "./war";

  @VisibleForTesting
  static final String DEFAULT_OUTPUT_PATH = "./";

  @VisibleForTesting
  static final String DEFAULT_WAR_OUTPUT_PATH_SUFFIX = "WEB-INF";

  @VisibleForTesting
  static final String DEFAULT_OPENAPI_OUTPUT_PATH = "./openapi.json";

  @VisibleForTesting
  static final String DEFAULT_CLASS_PATH = "";

  @VisibleForTesting
  static final String DEFAULT_LANGUAGE = "java";

  @VisibleForTesting
  static final String DEFAULT_BUILD_SYSTEM = null;

  @VisibleForTesting
  static final String DEFAULT_FORMAT = "rest";

  @VisibleForTesting
  static final String DEFAULT_HOSTNAME = "myapi.appspot.com";

  @VisibleForTesting
  static final String DEFAULT_BASE_PATH = "/_ah/api";

  // boolean flag which determines whether this action needs to show up in the help message
  private boolean helpDisplayNeeded = true;

  private String exampleString = null;

  public EndpointsToolAction(String name) {
    super(name);
  }

  /**
   * Returns a usage string for help output.
   */
  public abstract String getUsageString();

  /**
   * Executes the command with the given arguments.
   *
   * @return whether or not all required arguments were provided
   */
  public abstract boolean execute() throws ClassNotFoundException, IOException, ApiConfigException;

  @Override
  public void apply() {
    // We don't handle execution here because we want to further check that all arguments are
    // present in EndpointsTool beforehand.
  }

  @Override
  protected List<String> getHelpLines() {
    ImmutableList.Builder<String> helpLines = ImmutableList.builder();
    helpLines.add(getNameString());
    helpLines.add("");
    helpLines.addAll(wrap(getShortDescription(), MAX_WIDTH, 0));
    helpLines.add("");
    helpLines.add("Usage: <Endpoints tool> " + getUsageString());
    if (getOptions().size() > 0) {
      helpLines.add("");
      helpLines.add("Options:");
      for (Option option : getOptions()) {
        for (String optionHelpLine : option.getHelpLines()) {
          helpLines.add(Strings.repeat(" ", OPTION_INDENT) + optionHelpLine);
        }
      }
    }
    helpLines.add("");
    if (!Strings.isNullOrEmpty(exampleString)) {
      helpLines.add("Example:");
      helpLines.add("  " + exampleString);
      helpLines.add("");
    }
    return helpLines.build();
  }

  protected String getWarPath(Option warOption) {
    return getOptionOrDefault(warOption, DEFAULT_WAR_PATH);
  }

  protected String getOutputPath(Option outputOption) {
    return getOptionOrDefault(outputOption, DEFAULT_OUTPUT_PATH);
  }

  protected String getWarOutputPath(Option outputOption, String warPath) {
    if (outputOption.getValue() != null) {
      return outputOption.getValue();
    }
    return warPath + File.separator + DEFAULT_WAR_OUTPUT_PATH_SUFFIX;
  }

  protected String getOpenApiOutputPath(Option outputOption) {
    return getOptionOrDefault(outputOption, DEFAULT_OPENAPI_OUTPUT_PATH);
  }

  protected String getClassPath(Option classPathOption) {
    return getOptionOrDefault(classPathOption, DEFAULT_CLASS_PATH);
  }

  protected String getLanguage(Option languageOption) {
    return getOptionOrDefault(languageOption, DEFAULT_LANGUAGE);
  }

  protected String getBuildSystem(Option buildSystemOption) {
    return getOptionOrDefault(buildSystemOption, DEFAULT_BUILD_SYSTEM);
  }

  protected String getFormat(Option formatOption) {
    return getOptionOrDefault(formatOption, DEFAULT_FORMAT);
  }

  protected boolean getDebug(Option debugOption) {
    return debugOption.getValue() != null;
  }

  protected String getHostname(Option hostnameOption, String warPath) {
    // Rather than using the getOptionOrDefault path here, which eagerly evaluates the default
    // app hostname, do it manually for the lazy evaluation. In the Flex case, the app id is likely
    // not in the app.yaml and is passed in manually (the quickstart does this already). Therefore,
    // we don't want to evaluate it because it may throw an exception.
    if (hostnameOption.getValue() != null) {
      return hostnameOption.getValue();
    }
    String defaultHostname = AppEngineUtil.getApplicationDefaultHostname(warPath);
    if (defaultHostname != null) {
      return defaultHostname;
    }
    return DEFAULT_HOSTNAME;
  }

  protected String getBasePath(Option basePathOption) {
    return getOptionOrDefault(basePathOption, DEFAULT_BASE_PATH);
  }

  protected String getOptionOrDefault(Option option, String defaultValue) {
    if (option.getValue() != null) {
      return option.getValue();
    }
    return defaultValue;
  }

  protected Option makeWarOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_WAR_SHORT,
        OPTION_WAR_LONG,
        "WAR_PATH",
        "Sets the path to the war directory where web-appengine.xml and other metadata are "
            + "located. Default: " + DEFAULT_WAR_PATH + ".");
  }

  protected Option makeOutputOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_OUTPUT_DIR_SHORT,
        OPTION_OUTPUT_DIR_LONG,
        "OUTPUT_DIR",
        "Sets the directory where the output will be written to. Default: the directory the tool "
            + "is invoked from.");
  }

  protected Option makeWarOutputOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_OUTPUT_DIR_SHORT,
        OPTION_OUTPUT_DIR_LONG,
        "OUTPUT_DIR",
        "Sets the directory where the output will be written to. Default: the directory the tool "
            + "is invoked from.");
  }

  protected Option makeOpenApiOutputOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_OUTPUT_DIR_SHORT,
        OPTION_OUTPUT_DIR_LONG,
        "OUTPUT_FILE",
        "Sets the file where output will be written to. Default: " + DEFAULT_OPENAPI_OUTPUT_PATH);
  }

  protected Option makeClassPathOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_CLASS_PATH_SHORT,
        OPTION_CLASS_PATH_LONG,
        "CLASSPATH",
        "Lets you specify the service class or classes from a path other than the default "
            + "<war-directory>/WEB-INF/libs and <war-directory>/WEB-INF/classes, where <war-directory "
            + "is the directory specified in the war option, or simply " + DEFAULT_WAR_PATH + " if "
            + "that option is not supplied.");
  }

  protected Option makeLanguageOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_LANGUAGE_SHORT,
        OPTION_LANGUAGE_LONG,
        "LANGUAGE",
        "Sets the target output programming language. Default: " + DEFAULT_LANGUAGE + ".");
  }

  protected Option makeBuildSystemOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_BUILD_SYSTEM_SHORT,
        OPTION_BUILD_SYSTEM_LONG,
        "BUILD_SYSTEM",
        "Sets the build type for the generated client bundle. Possible values: default, maven and "
            + "gradle. Default: default.");
  }


  protected Option makeFormatOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_FORMAT_SHORT,
        OPTION_FORMAT_LONG,
        "FORMAT",
        "Sets the format of the generated discovery document. Possible values: rest or rpc. "
            + "Default is " + DEFAULT_FORMAT + ".");
  }

  protected Option makeDebugOption() {
    return EndpointsOption.makeInvisibleFlagOption(null, OPTION_DEBUG);
  }

  protected Option makeHostnameOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_HOSTNAME_SHORT,
        OPTION_HOSTNAME_LONG,
        "HOSTNAME",
        "Sets the hostname for the generated document. Default is the app's default hostname.");
  }

  protected Option makeBasePathOption() {
    return EndpointsOption.makeVisibleNonFlagOption(
        OPTION_BASE_PATH_SHORT,
        OPTION_BASE_PATH_LONG,
        "BASE_PATH",
        "Sets the base path for the generated document. Default is " + DEFAULT_BASE_PATH + ".");
  }

  /**
   * Computes an array of URLs representing a classpath for a WAR directory. Given the path to a
   * WAR, it adds the classes subdirectory and any JAR file under the lib subdirectory.
   *
   * @param warPath a path to a directory with a WAR structure
   * @param classPath a list of class path entries separated by {@link File#pathSeparator}.
   */
  protected URL[] computeClassPath(String warPath, String classPath)
      throws MalformedURLException {
    ImmutableList.Builder<URL> urls = ImmutableList.builder();
    File warDir = new File(warPath).getAbsoluteFile();
    File webInfDir = new File(warDir, "WEB-INF");
    File classesDir = new File(webInfDir, "classes");

    urls.add(classesDir.toURI().toURL());

    File libDir = new File(webInfDir, "lib");
    String[] files = libDir.list();
    if (files != null) {
      for (String file : files) {
        File maybeJar = new File(libDir, file);
        if (maybeJar.isFile() && file.endsWith(".jar")) {
          urls.add(maybeJar.toURI().toURL());
        }
      }
    }
    if (!Strings.isNullOrEmpty(classPath)) {
      for (String classPathEntry : classPath.split(File.pathSeparator)) {
        urls.add(new File(classPathEntry).toURI().toURL());
      }
    }
    return urls.build().toArray(new URL[0]);
  }

  protected List<String> getServiceClassNames(String warPath) {
    if (!getArgs().isEmpty()) {
      return getArgs();
    }
    try {
      File warDir = new File(warPath).getAbsoluteFile();
      File webInfDir = new File(warDir, "WEB-INF");
      File webXmlFile = new File(webInfDir, "web.xml");
      if (webXmlFile.exists()) {
        return WebXml.parse(webXmlFile).endpointsServiceClasses();
      }
    } catch (ParserConfigurationException | IOException | SAXException e) {
      log.log(Level.WARNING, "could not parse web.xml for service classes", e);
    }
    return Collections.emptyList();
  }

  /**
   * Tells whether help lines of this action needs to be displayed in the usage.
   */
  public boolean isHelpDisplayNeeded() {
    return helpDisplayNeeded;
  }

  /**
   * Controls whether the help lines of the action need to be displayed in the usage.
   */
  public void setHelpDisplayNeeded(boolean helpDisplayNeeded) {
    this.helpDisplayNeeded = helpDisplayNeeded;
  }

  /**
   * Return the example string which will be displayed in the usage.
   */
  public String getExampleString() {
    return exampleString;
  }

  /**
   * Set the example string which will be displayed in the usage.
   */
  public void setExampleString(String exampleString) {
    this.exampleString = exampleString;
  }

  /**
   * Base class for {@link Option}s used by the Cloud Endpoints tool.
   */
  @VisibleForTesting
  protected static class EndpointsOption extends Option {

    private String description;
    private String placeHolderValue;
    private boolean isVisible;

    /**
     * Creates a new {@code EndpointsOption}.
     *
     * @param shortName The short name to support.
     * @param longName The long name to support.
     * @param isFlag true to indicate that this represents a boolean value.
     * @param isVisible true to display the flag in help text.
     * @param placeHolderValue The placeholder value to display in help text. May be null if isFlag
     * or isVisible is false.
     * @param description The description to display in help text. May be null if isVisible is
     * false.
     */
    private EndpointsOption(
        @Nullable String shortName,
        @Nullable String longName,
        boolean isFlag,
        boolean isVisible,
        @Nullable String placeHolderValue,
        @Nullable String description) {
      super(shortName, longName, isFlag);
      Preconditions.checkArgument(isFlag || !isVisible || placeHolderValue != null,
          "non-null placeholder value required");
      Preconditions.checkArgument(!isVisible || description != null,
          "non-null description required");
      this.description = description;
      this.placeHolderValue = placeHolderValue;
      this.isVisible = isVisible;
    }

    public static EndpointsOption makeVisibleNonFlagOption(
        @Nullable String shortName,
        @Nullable String longName,
        @Nullable String placeHolderValue,
        @Nullable String description) {
      return new EndpointsOption(shortName, longName, false, true, placeHolderValue, description);
    }

    public static EndpointsOption makeInvisibleFlagOption(
        @Nullable String shortName,
        @Nullable String longName) {
      return new EndpointsOption(shortName, longName, true, false, null, null) {
        @Override
        public void apply() {
          getValues().add("true");
        }
      };
    }

    @Override
    public void apply() {
    }

    /**
     * Returns the short form of an option, e.g. '-p PORT' or null if the option defines no short
     * name.
     */
    public Optional<String> getShortForm(boolean includePlaceholder) {
      if (getShortName() == null) {
        return Optional.<String>absent();
      }
      StringBuffer form = new StringBuffer();
      form.append("-" + getShortName());
      if (!isFlag() && includePlaceholder) {
        form.append(" " + placeHolderValue);
      }
      return Optional.<String>of(form.toString());
    }

    /**
     * Returns the long form of an option, e.g. '--port=PORT' or null if the option defines no long
     * name.
     */
    public Optional<String> getLongForm(boolean includePlaceholder) {
      if (getLongName() == null) {
        return Optional.<String>absent();
      }
      StringBuffer form = new StringBuffer();
      form.append("--" + getLongName());
      if (!isFlag() && includePlaceholder) {
        form.append("=" + placeHolderValue);
      }
      return Optional.<String>of(form.toString());
    }

    @Override
    public List<String> getHelpLines() {
      if (!isVisible) {
        return super.getHelpLines();
      } else {
        Optional<String> shortForm = getShortForm(true);
        Optional<String> longForm = getLongForm(true);
        StringBuffer option = new StringBuffer();
        if (shortForm.isPresent()) {
          option.append(shortForm.get());
          if (longForm.isPresent()) {
            option.append(", ");
          }
        }
        if (longForm.isPresent()) {
          option.append(longForm.get());
        }

        ImmutableList.Builder<String> lines = ImmutableList.builder();
        lines.add(option.toString());
        lines.addAll(wrap(description, MAX_WIDTH - OPTION_INDENT, OPTION_DESCRIPTION_INDENT));
        return lines.build();
      }
    }
  }

  @VisibleForTesting
  static List<String> wrap(String source, int maxWidth, int indent) {
    Iterable<String> words = Splitter.on(" ").split(source);
    ImmutableList.Builder<String> wrappedLines = ImmutableList.builder();
    StringBuffer line = new StringBuffer(Strings.repeat(" ", indent));
    int lineLength = indent;
    for (String word : words) {
      if ((lineLength + word.length()) >= maxWidth) {
        wrappedLines.add(line.toString());
        line = new StringBuffer(Strings.repeat(" ", indent));
        lineLength = indent;
      }
      // If lineLength == maxWidth, then we'll definitely wrap the next word. No trailing space
      // is required.
      if (lineLength > indent && lineLength < maxWidth) {
        line.append(" ");
        lineLength++;
      }
      line.append(word);
      lineLength += (word.length());
    }
    wrappedLines.add(line.toString());
    return wrappedLines.build();
  }
}
