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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * Initialization parameters supported by the {@link EndpointsServlet}.
 */
public class ServletInitializationParameters {
  private static final String INIT_PARAM_NAME_SERVICES = "services";
  private static final String INIT_PARAM_NAME_RESTRICTED = "restricted";
  private static final String INIT_PARAM_NAME_CLIENT_ID_WHITELIST_ENABLED =
      "clientIdWhitelistEnabled";
  private static final String INIT_PARAM_NAME_ILLEGAL_ARGUMENT_BACKEND_ERROR =
      "illegalArgumentIsBackendError";
  private static final String INIT_PARAM_NAME_ENABLE_EXCEPTION_COMPATIBILITY =
      "enableExceptionCompatibility";

  private static final Splitter CSV_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
  private static final Joiner CSV_JOINER = Joiner.on(',').skipNulls();
  private static final Function<Class<?>, String> CLASS_TO_NAME = new Function<Class<?>, String>() {
    @Override public String apply(Class<?> clazz) {
      return clazz.getName();
    }
  };

  private final ImmutableSet<Class<?>> serviceClasses;
  private final boolean isServletRestricted;
  private final boolean isClientIdWhitelistEnabled;
  private final boolean isIllegalArgumentBackendError;
  private final boolean isExceptionCompatibilityEnabled;

  /**
   * Returns a new {@link Builder} for this class.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new instance from the provided {@link ServletConfig} and {@link ClassLoader}.
   */
  public static ServletInitializationParameters fromServletConfig(
      ServletConfig config, ClassLoader classLoader) throws ServletException {
    Builder builder = builder();
    if (config != null) {
      String serviceClassNames = config.getInitParameter(INIT_PARAM_NAME_SERVICES);
      if (serviceClassNames != null) {
        for (String serviceClassName : CSV_SPLITTER.split(serviceClassNames)) {
          builder.addServiceClass(getClassForName(serviceClassName, classLoader));
        }
      }
      String isServletRestricted = config.getInitParameter(INIT_PARAM_NAME_RESTRICTED);
      if (isServletRestricted != null) {
        builder.setRestricted(parseBoolean(isServletRestricted, "is servlet restricted"));
      }
      String isClientIdWhitelistEnabled =
          config.getInitParameter(INIT_PARAM_NAME_CLIENT_ID_WHITELIST_ENABLED);
      if (isClientIdWhitelistEnabled != null) {
        builder.setClientIdWhitelistEnabled(
            parseBoolean(isClientIdWhitelistEnabled, "is the client id whitelist enabled"));
      }
      String isIllegalArgumentBackendError =
          config.getInitParameter(INIT_PARAM_NAME_ILLEGAL_ARGUMENT_BACKEND_ERROR);
      if (isIllegalArgumentBackendError != null) {
        builder.setIllegalArgumentIsBackendError(parseBoolean(
            isIllegalArgumentBackendError, "is IllegalArgumentException a backend error"));
      }
      String isExceptionCompatibilityEnabled =
          config.getInitParameter(INIT_PARAM_NAME_ENABLE_EXCEPTION_COMPATIBILITY);
      if (isExceptionCompatibilityEnabled != null) {
        builder.setExceptionCompatibilityEnabled(
            parseBoolean(isExceptionCompatibilityEnabled, "is exception compatibility enabled"));
      }
    }
    return builder.build();
  }

  private static boolean parseBoolean(String booleanString, String descriptionForErrors) {
    if ("true".equalsIgnoreCase(booleanString)) {
      return true;
    } else if ("false".equalsIgnoreCase(booleanString)) {
      return false;
    }
    throw new IllegalArgumentException(String.format(
        "Expected 'true' or 'false' for %s servlet initialization parameter but got '%s'",
        descriptionForErrors, booleanString));
  }

  private static Class<?> getClassForName(String className, ClassLoader classLoader)
      throws ServletException {
    try {
      return Class.forName(className, true, classLoader);
    } catch (ClassNotFoundException e) {
      throw new ServletException(String.format("Cannot find service class: %s", className), e);
    }
  }

  /**
   * Returns the endpoint service classes to serve.
   */
  public ImmutableSet<Class<?>> getServiceClasses() {
    return serviceClasses;
  }

  /**
   * Returns {@code true} if the SPI servlet is restricted.
   */
  public boolean isServletRestricted() {
    return isServletRestricted;
  }

  /**
   * Returns {@code true} if client ID whitelisting is enabled.
   */
  public boolean isClientIdWhitelistEnabled() {
    return isClientIdWhitelistEnabled;
  }

  /**
   * Returns {@code true} if an {@link IllegalArgumentException} should be returned as a backend
   * error (500) instead of a user error (400).
   */
  public boolean isIllegalArgumentBackendError() {
    return isIllegalArgumentBackendError;
  }

  /**
   * Returns {@code true} if v1.0 style exceptions should be returned to users. In v1.0, certain
   * codes are not permissible, and other codes are translated to other status codes.
   */
  public boolean isExceptionCompatibilityEnabled() {
    return isExceptionCompatibilityEnabled;
  }

  /**
   * Returns the parameters as a {@link java.util.Map} of parameter name to {@link String} value.
   */
  public ImmutableMap<String, String> asMap() {
    ImmutableMap.Builder<String, String> parameterNameToValue = ImmutableMap.builder();
    parameterNameToValue.put(INIT_PARAM_NAME_SERVICES,
        CSV_JOINER.join(Iterables.transform(serviceClasses, CLASS_TO_NAME)));
    parameterNameToValue.put(INIT_PARAM_NAME_RESTRICTED, Boolean.toString(isServletRestricted));
    parameterNameToValue.put(
        INIT_PARAM_NAME_CLIENT_ID_WHITELIST_ENABLED, Boolean.toString(isClientIdWhitelistEnabled));
    parameterNameToValue.put(INIT_PARAM_NAME_ILLEGAL_ARGUMENT_BACKEND_ERROR,
        Boolean.toString(isIllegalArgumentBackendError));
    parameterNameToValue.put(INIT_PARAM_NAME_ENABLE_EXCEPTION_COMPATIBILITY,
        Boolean.toString(isExceptionCompatibilityEnabled));
    return parameterNameToValue.build();
  }

  private ServletInitializationParameters(
      ImmutableSet<Class<?>> serviceClasses, boolean isServletRestricted,
      boolean isClientIdWhitelistEnabled, boolean isIllegalArgumentBackendError,
      boolean isExceptionCompatibilityEnabled) {
    this.serviceClasses = serviceClasses;
    this.isServletRestricted = isServletRestricted;
    this.isClientIdWhitelistEnabled = isClientIdWhitelistEnabled;
    this.isIllegalArgumentBackendError = isIllegalArgumentBackendError;
    this.isExceptionCompatibilityEnabled = isExceptionCompatibilityEnabled;
  }

  /**
   * A builder for {@link ServletInitializationParameters}.
   */
  public static class Builder {
    private final ImmutableSet.Builder<Class<?>> serviceClasses = ImmutableSet.builder();
    private boolean isServletRestricted = true;
    private boolean isClientIdWhitelistEnabled = true;
    private boolean isIllegalArgumentBackendError = false;
    private boolean isExceptionCompatibilityEnabled = true;

    /**
     * Adds an endpoint service class to serve.
     */
    public Builder addServiceClass(Class<?> serviceClass) {
      this.serviceClasses.add(serviceClass);
      return this;
    }

    /**
     * Adds some endpoint service classes to serve.
     */
    public Builder addServiceClasses(Iterable<? extends Class<?>> serviceClasses) {
      this.serviceClasses.addAll(serviceClasses);
      return this;
    }

    /**
     * Sets if the SPI servlet is restricted ({@code true}) or not ({@code false}).  If this
     * method is not called, it defaults to {@code true}.
     */
    public Builder setRestricted(boolean isServletRestricted) {
      this.isServletRestricted = isServletRestricted;
      return this;
    }

    /**
     * Sets if the client ID whitelist is enabled ({@code true}) or not ({@code false}).  If this
     * method is not called, it defaults to {@code true}.
     */
    public Builder setClientIdWhitelistEnabled(boolean isClientIdWhitelistEnabled) {
      this.isClientIdWhitelistEnabled = isClientIdWhitelistEnabled;
      return this;
    }

    /**
     * Sets if an {@link IllegalArgumentException} should be treated as a backend error (500)
     * instead of a user error (400).
     */
    public Builder setIllegalArgumentIsBackendError(boolean isIllegalArgumentBackendError) {
      this.isIllegalArgumentBackendError = isIllegalArgumentBackendError;
      return this;
    }

    public Builder setExceptionCompatibilityEnabled(boolean isExceptionCompatibilityEnabled) {
      this.isExceptionCompatibilityEnabled = isExceptionCompatibilityEnabled;
      return this;
    }

    /**
     * Builds a new {@link ServletInitializationParameters} instance with the values from this
     * builder.
     */
    public ServletInitializationParameters build() {
      return new ServletInitializationParameters(
          serviceClasses.build(), isServletRestricted, isClientIdWhitelistEnabled,
          isIllegalArgumentBackendError, isExceptionCompatibilityEnabled);
    }
  }
}
