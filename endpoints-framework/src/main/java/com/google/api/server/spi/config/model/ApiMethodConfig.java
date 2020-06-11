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
package com.google.api.server.spi.config.model;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.model.ApiParameterConfig.Classification;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Flattened method configuration for a swarm endpoint method.  Data generally originates from
 * {@link com.google.api.server.spi.config.ApiMethod} annotations.
 *
 * @author Eric Orth
 */
public class ApiMethodConfig {

  private static final Map<? extends Class<? extends ServiceException>, Integer>
      KNOWN_EXCEPTION_CODES = ImmutableMap.<Class<? extends ServiceException>, Integer>builder()
      .put(BadRequestException.class, BadRequestException.CODE)
      .put(ForbiddenException.class, ForbiddenException.CODE)
      .put(ServiceUnavailableException.class, ServiceUnavailableException.CODE)
      .put(UnauthorizedException.class, UnauthorizedException.CODE)
      .put(NotFoundException.class, NotFoundException.CODE)
      .put(ConflictException.class, ConflictException.CODE)
      .put(InternalServerErrorException.class, InternalServerErrorException.CODE)
      .build();

  private enum RestMethod {
    LIST("list", "GET") {
      @Override
      public String guessResourceName(
          ApiConfig config, EndpointMethod method, Map<String, Class<?>> classTypes) {
        TypeToken<?> returnType = method.getReturnType();
        if (isValidCollectionType(returnType)) {
          return Types.getSimpleName(
              Types.getTypeParameter(returnType, 0), config.getSerializationConfig()).toLowerCase();
        }
        return null;
      }

      private boolean isValidCollectionType(TypeToken<?> type) {
        return type.isSubtypeOf(Collection.class) || Types.isCollectionResponseType(type);
      }
    },
    GET("get", "GET"),
    INSERT("insert", "POST"),
    UPDATE("update", "PUT"),
    DELETE("delete", "DELETE") {
      @Override
      public String guessResourceName(
          ApiConfig config, EndpointMethod method, Map<String, Class<?>> classTypes) {
        String methodName = method.getMethod().getName();
        return methodNamePrefix.length() >= methodName.length() ? null :
            methodName.substring(methodNamePrefix.length()).toLowerCase();
      }
    },
    REMOVE("remove", "DELETE") {
      @Override
      public String guessResourceName(
          ApiConfig config, EndpointMethod method, Map<String, Class<?>> classTypes) {
        String methodName = method.getMethod().getName();
        return methodNamePrefix.length() >= methodName.length() ? null :
            methodName.substring(methodNamePrefix.length()).toLowerCase();
      }
    },
    DEFAULT("", "POST") {
      @Override
      public String guessResourceName(
          ApiConfig config, EndpointMethod method, Map<String, Class<?>> classTypes) {
        return null;
      }
    };

    protected final String methodNamePrefix;
    private final String httpMethod;

    /**
     * Specifies a default REST method prefix, as well as what HTTP method it should use by default.
     *
     * @param methodNamePrefix A method name prefix.
     * @param httpMethod The default HTTP method for this prefix.
     */
    RestMethod(String methodNamePrefix, String httpMethod) {
      this.methodNamePrefix = methodNamePrefix;
      this.httpMethod = httpMethod;
    }

    /**
     * Gets the method name prefix for this instance.
     *
     * @return The method name prefix.
     */
    public String getMethodNamePrefix() {
      return this.methodNamePrefix;
    }

    /**
     * Gets the default HTTP method for this instance.
     *
     * @return The HTTP method.
     */
    public String getHttpMethod() {
      return this.httpMethod;
    }

    /**
     * Guesses a resource name based off a prefix.
     *
     * @return The HTTP method.
     */
    public String guessResourceName(
        ApiConfig config, EndpointMethod method, Map<String, Class<?>> classTypes) {
      return Types.getSimpleName(method.getReturnType(), config.getSerializationConfig())
          .toLowerCase();
    }
  }

  /** Value of the response status when not set in the definition of the method. */
  public static final int RESPONSE_STATUS_UNSET = -1;

  private final String endpointMethodName;

  private final List<ApiParameterConfig> parameterConfigs;
  private final ApiClassConfig apiClassConfig;

  private String name;
  private String description;
  private String path;
  private String canonicalPath;
  private String httpMethod;

  // If null, get a default from apiConfig.
  // TODO: When splitting out a builder class, pull in actual default on build().
  private AuthLevel authLevel;
  private AuthScopeExpression scopeExpression;
  private List<String> audiences;
  private ApiIssuerAudienceConfig issuerAudiences;
  private List<String> clientIds;
  private List<Class<? extends Authenticator>> authenticators;
  private boolean ignored = false;
  private boolean deprecated = false;
  private Boolean apiKeyRequired;
  private TypeToken<?> returnType;
  private int responseStatus;
  private Class<?>[] exceptionTypes;
  private List<ApiMetricCostConfig> metricCosts;

  private final TypeLoader typeLoader;

  public ApiMethodConfig(EndpointMethod method, TypeLoader typeLoader,
      ApiClassConfig apiClassConfig) {
    this.endpointMethodName = method.getMethod().getName();
    this.parameterConfigs = new ArrayList<>();
    this.apiClassConfig = apiClassConfig;
    this.typeLoader = typeLoader;
    setDefaults(method, typeLoader, apiClassConfig.getResource());
  }

  public ApiMethodConfig(ApiMethodConfig original, ApiClassConfig apiClassConfig) {
    this.endpointMethodName = original.endpointMethodName;
    this.apiClassConfig = apiClassConfig;
    this.name = original.name;
    this.path = original.path;
    this.canonicalPath = original.canonicalPath;
    this.description = original.description;
    this.httpMethod = original.httpMethod;
    this.scopeExpression = original.scopeExpression;
    this.audiences = original.audiences == null ? null : new ArrayList<>(original.audiences);
    this.issuerAudiences = original.issuerAudiences;
    this.clientIds = original.clientIds == null ? null : new ArrayList<>(original.clientIds);
    this.authenticators =
        original.authenticators == null ? null : new ArrayList<>(original.authenticators);
    this.ignored = original.ignored;
    this.apiKeyRequired = original.apiKeyRequired;
    this.returnType = original.returnType;
    this.responseStatus = original.responseStatus;
    this.typeLoader = original.typeLoader;
    this.metricCosts = original.metricCosts;

    // Parameter configs are mutable, so we need to do a deep copy.
    this.parameterConfigs = new ArrayList<>(original.parameterConfigs.size());
    for (ApiParameterConfig parameter : original.parameterConfigs) {
      parameterConfigs.add(new ApiParameterConfig(parameter, this));
    }
  }

  /**
   * Sets all fields to their default value to be used if not set otherwise.  Override to change the
   * default configuration.
   */
  protected void setDefaults(EndpointMethod endpointMethod, TypeLoader typeLoader,
      String apiDefaultResource) {
    Method method = endpointMethod.getMethod();
    RestMethod restMethod = getRestMethod(method);
    String resourceTypeName;
    if (apiDefaultResource != null) {
      resourceTypeName = apiDefaultResource.toLowerCase();
    } else {
      resourceTypeName = restMethod.guessResourceName(
          apiClassConfig.getApiConfig(), endpointMethod, typeLoader.getClassTypes());
    }

    name = null;
    httpMethod = Preconditions.checkNotNull(restMethod.getHttpMethod(), "httpMethod");
    setPath(Preconditions.checkNotNull(
        resourceTypeName == null ? method.getName() : resourceTypeName.toLowerCase(), "path"));
    authLevel = AuthLevel.UNSPECIFIED;
    scopeExpression = null;
    audiences = null;
    issuerAudiences = ApiIssuerAudienceConfig.UNSPECIFIED;
    clientIds = null;
    authenticators = null;
    ignored = false;
    apiKeyRequired = null;
    returnType = endpointMethod.getReturnType();
    responseStatus = RESPONSE_STATUS_UNSET;
    exceptionTypes = endpointMethod.getMethod().getExceptionTypes();
    metricCosts = ImmutableList.of();
  }

  private RestMethod getRestMethod(Method method) {
    String methodName = method.getName();
    for (RestMethod entry : RestMethod.values()) {
      if (methodName.startsWith(entry.getMethodNamePrefix())) {
        return entry;
      }
    }
    throw new AssertionError("It's impossible for method" + method + " to map to no REST path.");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof ApiMethodConfig) {
      ApiMethodConfig config = (ApiMethodConfig) o;
      return Objects.equals(endpointMethodName, config.endpointMethodName) &&
          parameterConfigs.equals(config.parameterConfigs) && Objects.equals(name, config.name) &&
          Objects.equals(path, config.path) && Objects.equals(httpMethod, config.httpMethod) &&
          Objects.equals(scopeExpression, config.scopeExpression) &&
          Objects.equals(audiences, config.audiences) &&
          Objects.equals(issuerAudiences, config.issuerAudiences) &&
          Objects.equals(clientIds, config.clientIds) &&
          Objects.equals(authenticators, config.authenticators) &&
          Objects.equals(typeLoader, config.typeLoader) &&
          ignored == config.ignored &&
          apiKeyRequired == config.apiKeyRequired &&
          Objects.equals(returnType, config.returnType) &&
          responseStatus == config.responseStatus &&
          Objects.equals(metricCosts, config.metricCosts);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpointMethodName, parameterConfigs, name, path, httpMethod,
        scopeExpression, audiences, clientIds, authenticators, typeLoader,
        ignored, issuerAudiences, apiKeyRequired, returnType, responseStatus, metricCosts);
  }

  public ApiClassConfig getApiClassConfig() {
    return apiClassConfig;
  }

  /**
   * Shorthand for {@code getApiClassConfig().getApiConfig()}.
   */
  public ApiConfig getApiConfig() {
    return apiClassConfig.getApiConfig();
  }

  public String getEndpointMethodName() {
    return endpointMethodName;
  }

  /**
   * Generates, using class name and java method name, a dot-separated full name for this endpoint
   * method.  This is different from the method name from {@code getFullMethodName}, which is used
   * to uniquely identify the method within the context of the API. The java method name, however,
   * is used to identify and reflectively call the actual java method.
   */
  public String getFullJavaName() {
    return apiClassConfig.getApiClassJavaName() + "." + getEndpointMethodName();
  }

  /**
   * Adds the given parameter to the configuration and updates the path to add the new parameter if
   * it is non-optional and has no default.
   */
  public ApiParameterConfig addParameter(String name, String description, boolean nullable,
      String defaultValue, TypeToken<?> type) {
    ApiParameterConfig config =
        new ApiParameterConfig(this, name, description, nullable, defaultValue, type, typeLoader);
    parameterConfigs.add(config);

    if (config.getClassification() != Classification.INJECTED && name != null && !nullable
        && defaultValue == null) {
      String pathFragment = "{" + name + "}";
      if (!path.contains(pathFragment)) {
        setPath(path + "/" + pathFragment);
      }
    }

    return config;
  }

  public List<ApiParameterConfig> getParameterConfigs() {
    return parameterConfigs;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    if (name != null) {
      return name;
    } else if (apiClassConfig.getResource() != null) {
      return String.format("%s.%s", apiClassConfig.getResource(), endpointMethodName);
    } else {
      if (apiClassConfig.getApiClassJavaSimpleName().isEmpty()) {
        return String.format("%s", endpointMethodName);
      } else {
        return String.format(
            "%s.%s", apiClassConfig.getApiClassJavaSimpleName(), endpointMethodName);
      }
    }
  }

  /*
   * Parts in dot delimited external method names such as JSON-RPC method names
   * must be camel-cased alpha-numeric.
   * [a-z][a-zA-Z0-9]*
   */
  public static String methodNameFormatter(String methodName) {
    StringBuilder builder = new StringBuilder();
    for (String s : methodName.split("\\.")) {
      if (!s.isEmpty()) {
        builder.append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).append('.');
      }
    }
    if (builder.length() == 0) {
      return builder.toString();
    } else {
      return builder.deleteCharAt(builder.length() - 1).toString();
    }
  }

  /**
   * Generates, using API name and method name, a dot-separated full name for this endpoint method.
   * The name is sanitized by {@code methodNameFormatter}.
   */
  public String getFullMethodName() {
    return methodNameFormatter(apiClassConfig.getApiConfig().getName() + "." + getName());
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setPath(String path) {
    this.path = path;
    if (path.startsWith("/")) {
      canonicalPath = path.substring(1);
    } else {
      canonicalPath = getApiConfig().getName() + "/" + getApiConfig().getVersion() + "/" + path;
    }
  }

  public String getPath() {
    return path;
  }

  public String getCanonicalPath() {
    return canonicalPath;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  /**
   * Generates a string representing the signature of the method when called using REST.  Can be
   * used to determine ambiguity of such REST calls.
   *
   * For example, a method with path "getFoo/{idA}/bar/{idB}" using http method GET would generate
   * the rest path "GET getFoo/{}/bar/{}".
   */
  public String getRestfulSignature() {
    return getHttpMethod() + " " + getPath().replaceAll("\\{([^\\}]*)\\}", "\\{\\}");
  }

  public void setAuthLevel(AuthLevel authLevel) {
    this.authLevel = authLevel;
  }

  public AuthLevel getAuthLevel() {
    return authLevel != AuthLevel.UNSPECIFIED ? authLevel : apiClassConfig.getAuthLevel();
  }

  public void setScopeExpression(AuthScopeExpression scopeExpression) {
    this.scopeExpression = scopeExpression;
  }

  public AuthScopeExpression getScopeExpression() {
    return scopeExpression != null ? scopeExpression : apiClassConfig.getScopeExpression();
  }

  public void setAudiences(List<String> audiences) {
    this.audiences = audiences;
  }

  public List<String> getAudiences() {
    return audiences != null ? audiences : apiClassConfig.getAudiences();
  }

  public void setIssuerAudiences(ApiIssuerAudienceConfig issuerAudiences) {
    Preconditions.checkNotNull(issuerAudiences, "issuerAudiences should never be null");
    this.issuerAudiences = issuerAudiences;
    if (issuerAudiences.hasGoogleIssuer()) {
      getApiClassConfig().getApiConfig().ensureGoogleIssuer();
    }
  }

  public ApiIssuerAudienceConfig getIssuerAudiences() {
    return issuerAudiences.isSpecified() ? issuerAudiences : apiClassConfig.getIssuerAudiences();
  }

  public void setClientIds(List<String> clientIds) {
    this.clientIds = clientIds;
  }

  public List<String> getClientIds() {
    return clientIds != null ? clientIds : apiClassConfig.getClientIds();
  }

  public void setAuthenticators(List<Class<? extends Authenticator>> authenticators) {
    this.authenticators = authenticators;
  }

  public List<Class<? extends Authenticator>> getAuthenticators() {
    return authenticators != null ? authenticators : apiClassConfig.getAuthenticators();
  }

  public void setIgnored(boolean ignored) {
    this.ignored = ignored;
  }

  public boolean isIgnored() {
    return ignored;
  }

  public void setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }

  public boolean isDeprecated() {
    return deprecated;
  }

  public void setApiKeyRequired(boolean apiKeyRequired) {
    this.apiKeyRequired = apiKeyRequired;
  }

  public boolean isApiKeyRequired() {
    return apiKeyRequired != null ? apiKeyRequired : apiClassConfig.isApiKeyRequired();
  }

  /**
   * Gets parameters in current path.
   */
  public Collection<String> getPathParameters() {
    Pattern pathPattern = java.util.regex.Pattern.compile("\\{([^\\}]*)\\}");
    Matcher pathMatcher = pathPattern.matcher(path);

    Collection<String> pathParameters = new LinkedHashSet<>();
    while (pathMatcher.find()) {
      pathParameters.add(pathMatcher.group(1));
    }

    return pathParameters;
  }

  public TypeToken<?> getReturnType() {
    return returnType;
  }

  public void setResponseStatus(int responseStatus) {
    this.responseStatus = responseStatus;
  }

  public int getResponseStatus() {
    return responseStatus;
  }

  public int getEffectiveResponseStatus() {
    return responseStatus == RESPONSE_STATUS_UNSET ? (hasResourceInResponse() ? SC_OK : SC_NO_CONTENT) : responseStatus;
  }

  public class ErrorResponse {
    public final int code;
    public final String name;
    public final String description;

    public ErrorResponse(int code, String name, String description) {
      this.code = code;
      this.name = name;
      this.description = description;
    }
  }
  
  public List<ErrorResponse> getErrorReponses() {
    List<ErrorResponse> responses = new ArrayList<>();
    for (Class<?> exceptionType : exceptionTypes) {
      //TODO allow custom exceptions by introducing a new annotation
      Integer code = KNOWN_EXCEPTION_CODES.get(exceptionType);
      if (code != null) {
        String name = exceptionType.getSimpleName().replace("Exception", "");
        String description = Joiner.on(' ').join(StringUtils.splitByCharacterTypeCamelCase(name));
        responses.add(new ErrorResponse(code, name, description));
      }
    }
    return responses;
  }

  /**
   * Returns whether or not the method has a resource (is non-void) in the response.
   */
  public boolean hasResourceInResponse() {
    Type returnType = getReturnType().getRawType();
    return returnType != Void.TYPE && returnType != Void.class;
  }

  public void setMetricCosts(List<ApiMetricCostConfig> metricCosts) {
    this.metricCosts = metricCosts;
  }

  public List<ApiMetricCostConfig> getMetricCosts() {
    return metricCosts;
  }
}
