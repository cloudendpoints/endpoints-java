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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Tests for {@link ServletInitializationParameters}.
 */
@RunWith(JUnit4.class)
public class ServletInitializationParametersTest {

  @Test
  public void testBuilder_defaults() {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .build();
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertTrue(initParameters.isServletRestricted());
    assertTrue(initParameters.isClientIdWhitelistEnabled());
    assertFalse(initParameters.isIllegalArgumentBackendError());
    assertTrue(initParameters.isExceptionCompatibilityEnabled());
    assertTrue(initParameters.isPrettyPrintEnabled());
    verifyAsMap(initParameters, "", "true", "true", "false", "true", "true");
  }

  @Test
  public void testBuilder_emptySetsAndTrue() {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .setClientIdWhitelistEnabled(true)
        .setRestricted(true)
        .addServiceClasses(ImmutableSet.<Class<?>>of())
        .setIllegalArgumentBackendError(true)
        .setExceptionCompatibilityEnabled(true)
        .setPrettyPrintEnabled(true)
        .build();
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertTrue(initParameters.isServletRestricted());
    assertTrue(initParameters.isClientIdWhitelistEnabled());
    assertTrue(initParameters.isIllegalArgumentBackendError());
    assertTrue(initParameters.isExceptionCompatibilityEnabled());
    verifyAsMap(initParameters, "", "true", "true", "true", "true", "true");
  }

  @Test
  public void testBuilder_oneEntrySetsAndFalse() {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .setRestricted(false)
        .addServiceClass(String.class)
        .setClientIdWhitelistEnabled(false)
        .setIllegalArgumentBackendError(false)
        .setExceptionCompatibilityEnabled(false)
        .setPrettyPrintEnabled(false)
        .build();
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class);
    assertFalse(initParameters.isServletRestricted());
    assertFalse(initParameters.isClientIdWhitelistEnabled());
    verifyAsMap(
        initParameters, String.class.getName(), "false", "false", "false", "false", "false");
  }

  @Test
  public void testBuilder_twoEntrySets() {
    ServletInitializationParameters initParameters = ServletInitializationParameters.builder()
        .addServiceClasses(ImmutableSet.of(String.class, Integer.class))
        .build();
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class, Integer.class);
    verifyAsMap(initParameters, String.class.getName() + ',' + Integer.class.getName(), "true",
        "true", "false", "true", "true");
  }

  @Test
  public void testFromServletConfig_nullConfig() throws ServletException {
    ServletInitializationParameters initParameters =
        ServletInitializationParameters.fromServletConfig(null, getClass().getClassLoader());
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertTrue(initParameters.isServletRestricted());
    assertTrue(initParameters.isClientIdWhitelistEnabled());
  }

  @Test
  public void testFromServletConfig_nullValues() throws ServletException {
    ServletInitializationParameters initParameters =
        fromServletConfig(null, null, null, null, null, null);
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertTrue(initParameters.isServletRestricted());
    assertTrue(initParameters.isClientIdWhitelistEnabled());
    assertFalse(initParameters.isIllegalArgumentBackendError());
    assertTrue(initParameters.isExceptionCompatibilityEnabled());
    assertTrue(initParameters.isPrettyPrintEnabled());
  }

  @Test
  public void testFromServletConfig_emptySetsAndFalse() throws ServletException {
    ServletInitializationParameters initParameters =
        fromServletConfig("", "false", "false", "false", "false", "false");
    assertThat(initParameters.getServiceClasses()).isEmpty();
    assertFalse(initParameters.isServletRestricted());
    assertFalse(initParameters.isClientIdWhitelistEnabled());
    assertFalse(initParameters.isIllegalArgumentBackendError());
    assertFalse(initParameters.isExceptionCompatibilityEnabled());
    assertFalse(initParameters.isPrettyPrintEnabled());
  }

  @Test
  public void testFromServletConfig_oneEntrySetsAndTrue() throws ServletException {
    ServletInitializationParameters initParameters =
        fromServletConfig(String.class.getName(), "true", "true", "true", "true", "true");
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class);
    assertTrue(initParameters.isServletRestricted());
    assertTrue(initParameters.isClientIdWhitelistEnabled());
    assertTrue(initParameters.isIllegalArgumentBackendError());
    assertTrue(initParameters.isExceptionCompatibilityEnabled());
    assertTrue(initParameters.isPrettyPrintEnabled());
  }

  @Test
  public void testFromServletConfig_twoEntrySets() throws ServletException {
    ServletInitializationParameters initParameters = fromServletConfig(
        String.class.getName() + ',' + Integer.class.getName(), null, null, null, null, null);
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class, Integer.class);
  }

  @Test
  public void testFromServletConfig_skipsEmptyElements() throws ServletException {
    ServletInitializationParameters initParameters = fromServletConfig(
        ",," + String.class.getName() + ",,," + Integer.class.getName() + ",", null, null, null,
        null, null);
    assertThat(initParameters.getServiceClasses()).containsExactly(String.class, Integer.class);
  }

  @Test
  public void testFromServletConfig_invalidRestrictedThrows() throws ServletException {
    try {
      fromServletConfig(null, "yes", null, null, null, null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  private void verifyAsMap(
      ServletInitializationParameters initParameters, String serviceClasses,
      String isServletRestricted, String isClientIdWhitelistEnabled,
      String isIllegalArgumentBackendError, String isExceptionCompatibilityEnabled,
      String isPrettyPrintEnabled) {
    Map<String, String> map = initParameters.asMap();
    assertEquals(6, map.size());
    assertEquals(serviceClasses, map.get("services"));
    assertEquals(isServletRestricted, map.get("restricted"));
    assertEquals(isClientIdWhitelistEnabled, map.get("clientIdWhitelistEnabled"));
    assertEquals(isIllegalArgumentBackendError, map.get("illegalArgumentIsBackendError"));
    assertEquals(isExceptionCompatibilityEnabled, map.get("enableExceptionCompatibility"));
    assertEquals(isPrettyPrintEnabled, map.get("prettyPrint"));
  }

  private ServletInitializationParameters fromServletConfig(
      String serviceClasses, String isServletRestricted,
      String isClientIdWhitelistEnabled, String isIllegalArgumentBackendError,
      String isExceptionCompatibilityEnabled, String isPrettyPrintEnabled)
      throws ServletException {
    ServletConfig servletConfig = new StubServletConfig(serviceClasses,
        isServletRestricted, isClientIdWhitelistEnabled, isIllegalArgumentBackendError,
        isExceptionCompatibilityEnabled, isPrettyPrintEnabled);
    return ServletInitializationParameters.fromServletConfig(
            servletConfig, getClass().getClassLoader());
  }

  private static class StubServletConfig implements ServletConfig {
    private final Map<String, String> initParameters;

    public StubServletConfig(
        String serviceClasses, String isServletRestricted, String isClientIdWhitelistEnabled,
        String isIllegalArgumentBackendError, String isExceptionCompatibilityEnabled,
        String isPrettyPrintEnabled) {
      initParameters = Maps.newHashMap();
      initParameters.put("services", serviceClasses);
      initParameters.put("restricted", isServletRestricted);
      initParameters.put("clientIdWhitelistEnabled", isClientIdWhitelistEnabled);
      initParameters.put("illegalArgumentIsBackendError", isIllegalArgumentBackendError);
      initParameters.put("enableExceptionCompatibility", isExceptionCompatibilityEnabled);
      initParameters.put("prettyPrint", isPrettyPrintEnabled);
    }

    @Override
    public String getServletName() {
      return null;
    }

    @Override
    public ServletContext getServletContext() {
      return null;
    }

    @Override
    public String getInitParameter(String name) {
      return initParameters.get(name);
    }

    @Override
    public Enumeration<?> getInitParameterNames() {
      return null;
    }
  }
}
