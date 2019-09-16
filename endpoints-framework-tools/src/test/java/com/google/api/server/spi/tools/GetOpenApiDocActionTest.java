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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.api.server.spi.swagger.SwaggerGenerator.SwaggerContext;
import com.google.appengine.tools.util.Option;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link GetOpenApiDocAction}.
 */
@RunWith(JUnit4.class)
public class GetOpenApiDocActionTest extends EndpointsToolTest {

  private URL[] classPath;
  private String outputFilePath;
  private String basePath;
  private List<String> serviceClassNames;
  private String tagTemplate;
  private String operationIdTemplate;
  private boolean outputToDisk;
  private boolean addGoogleJsonErrorAsDefaultResponse;
  private boolean addErrorCodesForServiceExceptionsOption;

  @Override
  protected void addTestAction(Map<String, EndpointsToolAction> actions) {
    actions.put(GetOpenApiDocAction.NAME, new GetOpenApiDocAction() {

      @Override
      public String genOpenApiDoc(
          URL[] classPath, String outputFilePath, String hostname, String basePath,
          String title, String description, String apiName,
          String tagTemplate, String operationIdTemplate,
          boolean addGoogleJsonErrorAsDefaultResponse,
          boolean addErrorCodesForServiceExceptionsOption,
          boolean extractCommonParametersAsRefsOption,
          boolean combineCommonParametersInSamePathOption,
          List<String> serviceClassNames, boolean outputToDisk) {
        GetOpenApiDocActionTest.this.classPath = classPath;
        GetOpenApiDocActionTest.this.outputFilePath = outputFilePath;
        GetOpenApiDocActionTest.this.basePath = basePath;
        GetOpenApiDocActionTest.this.serviceClassNames = serviceClassNames;
        GetOpenApiDocActionTest.this.tagTemplate = tagTemplate;
        GetOpenApiDocActionTest.this.operationIdTemplate = operationIdTemplate;
        GetOpenApiDocActionTest.this.outputToDisk = outputToDisk;
        GetOpenApiDocActionTest.this.addGoogleJsonErrorAsDefaultResponse 
            = addGoogleJsonErrorAsDefaultResponse;
        GetOpenApiDocActionTest.this.addErrorCodesForServiceExceptionsOption 
            = addErrorCodesForServiceExceptionsOption;
        return null;
      }

      @Override
      public String getHostname(Option option, String warPath) {
        return "myapi.appspot.com";
      }
    });
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();

    usagePrinted = false;
    classPath = null;
    outputFilePath = null;
    basePath = null;
    serviceClassNames = null;
  }

  @Test
  public void testGetOpenApiDoc() throws Exception {
    tool.execute(
        new String[]{GetOpenApiDocAction.NAME, option(EndpointsToolAction.OPTION_CLASS_PATH_SHORT),
            "classPath", option(EndpointsToolAction.OPTION_OUTPUT_DIR_SHORT), "outputDir", 
            option("addGoogleJsonErrorAsDefaultResponse", false),
            option("tt"), "myCustomTemplate",
            "MyService",
            "MyService2"});
    assertFalse(usagePrinted);
    assertThat(Lists.newArrayList(classPath))
        .containsExactly(new File("classPath").toURI().toURL(),
            new File(new File(EndpointsToolAction.DEFAULT_WAR_PATH).getAbsoluteFile(),
                "/WEB-INF/classes")
                .toURI()
                .toURL());
    assertEquals("outputDir", outputFilePath);
    assertEquals(EndpointsToolAction.DEFAULT_BASE_PATH, basePath);
    assertTrue(addGoogleJsonErrorAsDefaultResponse);
    assertFalse(addErrorCodesForServiceExceptionsOption);
    assertEquals("myCustomTemplate", tagTemplate);
    assertEquals(SwaggerContext.DEFAULT_OPERATION_ID_TEMPLATE, operationIdTemplate);
    assertStringsEqual(Arrays.asList("MyService", "MyService2"), serviceClassNames);
    assertTrue(outputToDisk);
  }
}
