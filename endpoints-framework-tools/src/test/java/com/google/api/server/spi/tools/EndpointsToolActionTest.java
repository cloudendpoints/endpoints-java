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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.server.spi.config.ApiConfigException;
import com.google.api.server.spi.tools.EndpointsToolAction.EndpointsOption;
import com.google.appengine.tools.util.Option;
import com.google.common.collect.ImmutableList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Tests for {@link EndpointsToolAction}.
 */
@RunWith(JUnit4.class)
public class EndpointsToolActionTest {
  @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();
  
  public class TestAction extends EndpointsToolAction {
    
    public Option warOption = makeWarOption();
    public Option outputOption = makeOutputOption();
    public Option warOutputOption = makeWarOutputOption();
    public Option classpathOption = makeClassPathOption();
    public Option languageOption = makeLanguageOption();
    public Option buildSystemOption = makeBuildSystemOption();
    public Option formatOption = makeFormatOption();
    public Option debugOption = makeDebugOption();
    
    public TestAction() {
      super("test");
      setOptions(ImmutableList.of(warOption, outputOption, warOutputOption, classpathOption,
          languageOption, buildSystemOption, formatOption, debugOption));
      setShortDescription("This is an Endpoints tool action for junit tests.");
      setExampleString("<Endpoints tool> --option=option test");
      setHelpDisplayNeeded(true);
    }
    
    @Override
    public String getUsageString() {
      return "test <OPTIONS>";
    }

    @Override
    public boolean execute()
        throws ClassNotFoundException,
        IOException,
        ApiConfigException {
      return false;
    }
  }

  @Test
  public void testGetHelpLines() {
    TestAction action = new TestAction() {
      @Override
      public List<Option> getOptions() {
        return ImmutableList.<Option>of(
            EndpointsOption.makeVisibleNonFlagOption(
                "t",
                "test",
                "TEST",
                "This is a totally meaningless description, which is used to test output of "
                + "helpLines() method."));
      }
    };
    assertTrue(action.isHelpDisplayNeeded());

    List<String> expectedLines = ImmutableList.of(
        "test",
        "",
        "This is an Endpoints tool action for junit tests.",
        "",
        "Usage: <Endpoints tool> test <OPTIONS>",
        "",
        "Options:",
        "  -t TEST, --test=TEST",
        "    This is a totally meaningless description, which is used to test output of",
        "    helpLines() method.",
        "",
        "Example:",
        "  <Endpoints tool> --option=option test",
        "");
    assertEquals(expectedLines, action.getHelpLines());
  }

  @Test
  public void testEndpointsOption() {
    EndpointsOption testOption =
        EndpointsOption.makeVisibleNonFlagOption("t", "test", "TEST_VALUE", "test description");
    assertEquals("t", testOption.getShortName());
    assertEquals("test", testOption.getLongName());
    assertTrue(!testOption.isFlag());
    List<String> expectedLines = ImmutableList.of(
        "-t TEST_VALUE, --test=TEST_VALUE",
        "  test description");
    assertEquals(expectedLines, testOption.getHelpLines());
  }

  @SuppressWarnings("unused")
  public void testEndpointsOptionWithBadArguments() {
    // Validate that visible non-flag EndpointsOpiton should fail to be created when description is
    // null.
    try {
      EndpointsOption testOption =
          EndpointsOption.makeVisibleNonFlagOption("t", "test", "TEST_VALUE", null);
      fail("Should fail to build testOption.");
    } catch (Exception e) {
      // Expected
    }

    // Validate that visible non-flag EndpointsOpiton should fail to be created when
    // placeHolderValue is null.
    try {
      EndpointsOption testOption =
          EndpointsOption.makeVisibleNonFlagOption("t", "test", null, "test description");
      fail("fail(\"Should fail to build testOption.\");");
    } catch (Exception e) {
      // Expected
    }
  }

  @Test
  public void testGetShortForm() {
    // When it is an invisible flag option.
    EndpointsOption option = EndpointsOption.makeInvisibleFlagOption("f", "file");
    assertEquals("-f", option.getShortForm(true).get());
    assertEquals("-f", option.getShortForm(false).get());

    // When it is a visible non-flag option.
    option = EndpointsOption.makeVisibleNonFlagOption("f", "file", "FILE", "description");
    assertEquals("-f FILE", option.getShortForm(true).get());
    assertEquals("-f", option.getShortForm(false).get());

    // When shortName is null.
    option = EndpointsOption.makeVisibleNonFlagOption(null, "file", "FILE", "description");
    assertFalse(option.getShortForm(true).isPresent());
    assertFalse(option.getShortForm(false).isPresent());
  }

  @Test
  public void testGetLongForm() {
    // When it is an invisible flag option.
    EndpointsOption  option = EndpointsOption.makeInvisibleFlagOption("f", "file");
    assertEquals("--file", option.getLongForm(true).get());
    assertEquals("--file", option.getLongForm(false).get());

    // When it is a visible non-flag option.
    option = EndpointsOption.makeVisibleNonFlagOption("f", "file", "FILE", "description");
    assertEquals("--file=FILE", option.getLongForm(true).get());
    assertEquals("--file", option.getLongForm(false).get());

    // When longName is null.
    option = EndpointsOption.makeVisibleNonFlagOption("f", null, "FILE", "description");
    assertFalse(option.getLongForm(true).isPresent());
    assertFalse(option.getLongForm(false).isPresent());
  }

  @Test
  public void testWrap() {
    String source = "This is a sample option description to test static method wrap in "
        + "EndpointsToolAction.java";
    List<String> lines = EndpointsToolAction.wrap(source, 20, 2);
    assertEquals(5, lines.size());
    assertEquals("  This is a sample", lines.get(0));
    assertEquals("  option description", lines.get(1));
    assertEquals("  to test static", lines.get(2));
    assertEquals("  method wrap in", lines.get(3));
    // No wrap if word length is larger than maxWidth
    assertEquals("  EndpointsToolAction.java", lines.get(4));
  }

  @Test
  public void testComputeClasspath() throws IOException {
    String tempDir = tmpFolder.getRoot().getAbsolutePath();
    File classesDir = new File(tempDir, "WEB-INF" + File.separator + "classes");
    if (!classesDir.exists()) {
      classesDir.mkdirs();
    }
    File libDir = new File(tempDir, "WEB-INF" + File.separator + "lib");
    if (!libDir.exists()) {
      libDir.mkdirs();
    }
    File jarFileInLibDir = new File(libDir.getAbsolutePath() + File.separator + "lib.jar");
    jarFileInLibDir.createNewFile();
    File jarFileInClasspath = new File(tempDir + File.separator + "classpath.jar");
    jarFileInClasspath.createNewFile();

    TestAction action = new TestAction();
    URL[] urls = action.computeClassPath(tempDir, jarFileInClasspath.getAbsolutePath());
    assertEquals(classesDir.getAbsolutePath() + File.separator, urls[0].getFile());
    assertEquals(jarFileInLibDir.getAbsolutePath(), urls[1].getFile());
    assertEquals(jarFileInClasspath.getAbsolutePath(), urls[2].getFile());
  }
}
