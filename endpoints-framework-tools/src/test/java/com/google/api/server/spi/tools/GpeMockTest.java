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

import static org.junit.Assert.assertTrue;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.common.collect.ImmutableList;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Named;

/**
 * This test mocks how GPE (Google Plugin for Eclipse) calls Endpoints tooling to generate Endpoints
 * client library. GPE is currently in maintenance mode, so it is relatively safe to write this
 * mocking test to make sure that future changes at SPI side won't break client generation
 * functionality at GPE.
 * 
 * <p>This test mocks how GPE works.
 * 
 * <p>Failure to pass put an alarm that your changes might have broken GPE fuctionality. 
 * So DO NOT CHANGE this test just to make test pass!!!
 */
@Ignore
@RunWith(JUnit4.class)
public class GpeMockTest {
  private static final String DISCOVERY_API_ROOT = "https://webapis-discovery.appspot.com/_ah/api";

  private static final String CLIENT_LIB_GENERATOR =
      "https://developers.google.com/resources/api-libraries/endpoints/genlib";

  @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

  private ClassLoader loader = Thread.currentThread().getContextClassLoader();

  @Test
  public void testCreateSwarmApi() throws Exception {
    List<Class<?>> serviceClassList = ImmutableList.<Class<?>>of(MyApi.class);
    Map<String, String> map = getApiConfigs(serviceClassList);
    String apiConfig = (String) map.values().toArray()[0];
    String discoveryDoc = getDiscoveryDoc(apiConfig);
    File clientLib = getClientLib(discoveryDoc);
    ZipFile zipFile = new ZipFile(clientLib);
    assertTrue(containsSourceJar(zipFile));
    zipFile.close();
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getApiConfigs(List<Class<?>> serviceClassList) throws Exception {
    Class<?> serviceContext = loader.loadClass("com.google.api.server.spi.ServiceContext");
    ArrayList<Object> methodArgs = new ArrayList<Object>();
    methodArgs.add("myapp");
    methodArgs.add("myapi");
    Method serviceContextCreateMethod =
        serviceContext.getMethod("create", String.class, String.class);

    Object serviceContextInstance = serviceContextCreateMethod.invoke(null, methodArgs.toArray());
    Class<?> annotationApiConfigGenerator =
        loader.loadClass("com.google.api.server.spi.tools.AnnotationApiConfigGenerator");
    Method generateConfigMethod = annotationApiConfigGenerator.getMethod("generateConfig",
        serviceContext, serviceClassList.toArray(new Class<?>[0]).getClass());

    Map<String, String> apiConfigs = (Map<String, String>) generateConfigMethod.invoke(
        annotationApiConfigGenerator.newInstance(),
        new Object[] {serviceContextInstance, serviceClassList.toArray(new Class<?>[0])});
    return apiConfigs;
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  private String getDiscoveryDoc(String apiConfig) throws Exception {
    Class<Enum> formatEnum = (Class<Enum>) loader.loadClass(
        "com.google.api.server.spi.tools.DiscoveryDocGenerator$Format");
    Class<?> discoveryDocGenerator =
        loader.loadClass("com.google.api.server.spi.tools.CloudDiscoveryDocGenerator");
    Method usingMethod = discoveryDocGenerator.getMethod("using", String.class);
    Method generateDiscoveryDocMethod =
        discoveryDocGenerator.getMethod("generateDiscoveryDoc", String.class, formatEnum);

    String discoveryDoc = (String) generateDiscoveryDocMethod.invoke(
        usingMethod.invoke(null, DISCOVERY_API_ROOT), apiConfig, Enum.valueOf(formatEnum, "REST"));
    return discoveryDoc;
  }
  
  private File getClientLib(String discoveryDocString) throws Exception {
    Class<?> clientLibGenerator =
        loader.loadClass("com.google.api.server.spi.tools.CloudClientLibGenerator");
    Method clientLibGeneratorMethod = null;
    clientLibGeneratorMethod = clientLibGenerator.getMethod("generateClientLib",
        String.class, /* discoveryDoc */
        String.class, /* language */
        String.class, /* languageVersion */
        String.class, /* layout */
        File.class /* file */);
    File destFile =
        File.createTempFile("client_lib", ".zip", tmpFolder.getRoot().getAbsoluteFile());
    ArrayList<Object> methodArgs = new ArrayList<Object>();
    methodArgs.add(discoveryDocString);
    methodArgs.add("JAVA");
    methodArgs.add("1.18.0-rc");
    methodArgs.add(null);
    methodArgs.add(destFile);
    Object clientLibGeneratorInstance =
        clientLibGenerator.getMethod("using", String.class).invoke(null, CLIENT_LIB_GENERATOR);
    clientLibGeneratorMethod.invoke(clientLibGeneratorInstance, methodArgs.toArray());
    return destFile;
  }
  
  private boolean containsSourceJar(ZipFile zipFile) {
    Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
    while (zipEntries.hasMoreElements()) {
      if (((ZipEntry) zipEntries.nextElement()).getName().endsWith("sources.jar")) {
        return true;
      }
    }
    return false;
  }
  
  // Simple Endpoints annotated API service class.
  @Api(name = "myapi", version = "v1", description = "Test API",
      defaultVersion = AnnotationBoolean.TRUE)
  public class MyApi {
    @ApiMethod(name = "add")
    public EndpointsTestResult add(@Named("x") int x, @Named("y") int y) {
      return new EndpointsTestResult(x + y);
    }

    public class EndpointsTestResult implements Serializable {
      private int result;

      public EndpointsTestResult(int result) {
        this.result = result;
      }

      public int getResult() {
        return result;
      }
    }
  }
}
