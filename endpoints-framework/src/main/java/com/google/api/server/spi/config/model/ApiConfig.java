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

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.ServiceContext;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.ApiConfigInconsistency;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Flattened configuration for a swarm endpoint.  Data generally originates from
 * {@link com.google.api.server.spi.config.Api} annotations.
 *
 * @author Eric Orth
 */
// TODO: Clean things up a bit by separating it into an immutable config object and a
// Builder, and merge the new vs copy creation by having the new just copy a default config.  Then
// efficientisize config reload make it so the builder only copies once an actual change is
// detected.
public class ApiConfig {
  // Default value of scopes and clientIds if unset
  private static final AuthScopeExpression DEFAULT_SCOPE_EXPRESSION =
      AuthScopeExpressions.interpret(Constant.API_EMAIL_SCOPE);
  private static final List<String> DEFAULT_CLIENT_IDS =
      ImmutableList.of(Constant.API_EXPLORER_CLIENT_ID);

  private final TypeLoader typeLoader;

  private String root;
  private String name;
  private String canonicalName;
  private String version;
  private String title;
  private String description;
  private String documentationLink;
  private String backendRoot;
  private boolean isAbstract;
  private boolean defaultVersion;
  private boolean discoverable;

  private String resource;
  private boolean useDatastore;

  private AuthLevel authLevel;
  private AuthScopeExpression scopeExpression;
  private List<String> audiences;
  private ApiIssuerConfigs issuers;
  private ApiIssuerAudienceConfig issuerAudiences;
  private List<String> clientIds;
  private List<Class<? extends Authenticator>> authenticators;
  private List<Class<? extends PeerAuthenticator>> peerAuthenticators;
  private boolean apiKeyRequired;

  private final ApiAuthConfig authConfig;
  private final ApiCacheControlConfig cacheControlConfig;
  private final ApiFrontendLimitsConfig frontendLimitsConfig;
  private final ApiSerializationConfig serializationConfig;
  private final ApiNamespaceConfig namespaceConfig;

  private final ApiClassConfig apiClassConfig;
  private List<ApiLimitMetricConfig> apiLimitMetrics;

  /**
   * Simple factory to create {@link ApiConfig} instances.
   */
  public static class Factory {
    public ApiConfig create(ServiceContext serviceContext, TypeLoader typeLoader,
        Class<?> endpointClass) {
      return new ApiConfig(serviceContext, typeLoader, endpointClass);
    }

    public ApiConfig copy(ApiConfig old) {
      return new ApiConfig(old);
    }
  }

  /**
   * Hidden constructor.  Instantiate using {@link Factory}.
   */
  protected ApiConfig(ServiceContext serviceContext, TypeLoader typeLoader,
      Class<?> apiClass) {
    this.typeLoader = typeLoader;
    authConfig = createAuthConfig();
    cacheControlConfig = createCacheControlConfig();
    frontendLimitsConfig = createFrontendLimitsConfig();
    serializationConfig = createSerializationConfig();
    namespaceConfig = createNamespaceConfig();
    apiClassConfig = createApiClassConfig(typeLoader, apiClass);

    setDefaults(serviceContext);
  }

  /**
   * Hidden copy constructor.  Use {@link Factory}.
   */
  protected ApiConfig(ApiConfig original) {
    this.typeLoader = original.typeLoader;
    this.root = original.root;
    this.name = original.name;
    this.canonicalName = original.canonicalName;
    this.version = original.version;
    this.title = original.title;
    this.description = original.description;
    this.documentationLink = original.documentationLink;
    this.backendRoot = original.backendRoot;
    this.isAbstract = original.isAbstract;
    this.defaultVersion = original.defaultVersion;
    this.discoverable = original.discoverable;
    this.resource = original.resource;
    this.useDatastore = original.useDatastore;
    this.authLevel = original.authLevel;
    this.scopeExpression = original.scopeExpression;
    this.audiences = original.audiences == null ? null : new ArrayList<>(original.audiences);
    this.issuers = original.issuers;
    this.issuerAudiences = original.issuerAudiences;
    this.clientIds = original.clientIds == null ? null : new ArrayList<>(original.clientIds);
    this.authenticators = original.authenticators;
    this.peerAuthenticators = original.peerAuthenticators;
    this.apiKeyRequired = original.apiKeyRequired;
    this.apiLimitMetrics = original.apiLimitMetrics;
    this.authConfig = new ApiAuthConfig(original.authConfig);
    this.cacheControlConfig = new ApiCacheControlConfig(original.cacheControlConfig);
    this.frontendLimitsConfig = new ApiFrontendLimitsConfig(original.frontendLimitsConfig);
    this.serializationConfig = new ApiSerializationConfig(original.serializationConfig);
    this.namespaceConfig = new ApiNamespaceConfig(original.namespaceConfig);
    this.apiClassConfig = new ApiClassConfig(original.apiClassConfig, this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof ApiConfig) {
      ApiConfig config = (ApiConfig) o;
      return Iterables.isEmpty(getConfigurationInconsistencies(config))
          && apiClassConfig.equals(config.apiClassConfig);
    } else {
      return false;
    }
  }

  /**
   * @return {@code true} if all API-level (not class or method specific) configuration is
   * identical.
   */
  public Iterable<ApiConfigInconsistency<Object>> getConfigurationInconsistencies(
      ApiConfig config) {
    return ApiConfigInconsistency.listBuilder()
        .addIfInconsistent("typeLoader", typeLoader, config.typeLoader)
        .addIfInconsistent("root", root, config.root)
        .addIfInconsistent("name", name, config.name)
        .addIfInconsistent("cannonicalName", canonicalName, config.canonicalName)
        .addIfInconsistent("version", version, config.version)
        .addIfInconsistent("title", title, config.title)
        .addIfInconsistent("description", description, config.description)
        .addIfInconsistent("documentationLink", documentationLink, config.documentationLink)
        .addIfInconsistent("backendRoot", backendRoot, config.backendRoot)
        .addIfInconsistent("isAbstract", isAbstract, config.isAbstract)
        .addIfInconsistent("defaultVersion", defaultVersion, config.defaultVersion)
        .addIfInconsistent("discoverable", discoverable, config.discoverable)
        .addIfInconsistent("useDatastore", useDatastore, config.useDatastore)
        .addIfInconsistent("resource", resource, config.resource)
        .addIfInconsistent("authLevel", authLevel, config.authLevel)
        .addIfInconsistent("scopeExpression", scopeExpression, config.scopeExpression)
        .addIfInconsistent("audiences", audiences, config.audiences)
        .addIfInconsistent("issuers", issuers, config.issuers)
        .addIfInconsistent("issuerAudiencies", issuerAudiences, config.issuerAudiences)
        .addIfInconsistent("clientIds", clientIds, config.clientIds)
        .addIfInconsistent("authenticators", authenticators, config.authenticators)
        .addIfInconsistent("peerAuthenticators", peerAuthenticators, config.peerAuthenticators)
        .addIfInconsistent("apiKeyRequired", apiKeyRequired, config.apiKeyRequired)
        .addIfInconsistent("apiLimitMetrics", apiLimitMetrics, config.apiLimitMetrics)
        .addAll(authConfig.getConfigurationInconsistencies(config.authConfig))
        .addAll(cacheControlConfig.getConfigurationInconsistencies(config.cacheControlConfig))
        .addAll(frontendLimitsConfig.getConfigurationInconsistencies(config.frontendLimitsConfig))
        .addAll(serializationConfig.getConfigurationInconsistencies(config.serializationConfig))
        .addAll(namespaceConfig.getConfigurationInconsistencies(config.namespaceConfig))
        .build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeLoader, root, name, canonicalName, version, title, description,
        documentationLink, backendRoot, isAbstract, defaultVersion, discoverable, useDatastore,
        resource, authLevel, scopeExpression, audiences, clientIds, authenticators,
        peerAuthenticators, authConfig, cacheControlConfig, frontendLimitsConfig,
        serializationConfig, apiClassConfig, issuers, issuerAudiences, apiKeyRequired,
        apiLimitMetrics);
  }

  /**
   * Creates the auth configuration object.  Override to use a subclass instead.
   */
  protected ApiAuthConfig createAuthConfig() {
    return new ApiAuthConfig();
  }

  /**
   * Creates the cache control configuration object.  Override to use a subclass instead.
   */
  protected ApiCacheControlConfig createCacheControlConfig() {
    return new ApiCacheControlConfig();
  }

  /**
   * Creates the frontend limits configuration object.  Override to use a subclass instead.
   */
  protected ApiFrontendLimitsConfig createFrontendLimitsConfig() {
    return new ApiFrontendLimitsConfig();
  }

  /**
   * Creates the serialization configuration object.  Override to use a subclass instead.
   */
  protected ApiSerializationConfig createSerializationConfig() {
    return new ApiSerializationConfig();
  }

  /**
   * Creates the namespace configuration object.  Override to use a subclass instead.
   */
  protected ApiNamespaceConfig createNamespaceConfig() {
    return new ApiNamespaceConfig();
  }

  protected ApiClassConfig createApiClassConfig(TypeLoader typeLoader, Class<?> apiClass) {
    return new ApiClassConfig(this, typeLoader, apiClass);
  }

  /**
   * Sets all fields to their default value to be used if not set otherwise.  Override to change the
   * default configuration.
   */
  protected void setDefaults(ServiceContext serviceContext) {
    root =
        serviceContext.getTransferProtocol() + "://" + serviceContext.getAppHostname() + "/_ah/api";
    name = serviceContext.getDefaultApiName();
    canonicalName = null;
    version = "v1";
    description = null;
    backendRoot =
        serviceContext.getTransferProtocol() + "://" + serviceContext.getAppHostname() + "/_ah/spi";
    isAbstract = false;
    defaultVersion = true;
    discoverable = true;
    useDatastore = false;
    resource = null;

    authLevel = AuthLevel.NONE;
    scopeExpression = DEFAULT_SCOPE_EXPRESSION;
    audiences = Collections.emptyList();
    issuers = ApiIssuerConfigs.EMPTY;
    issuerAudiences = ApiIssuerAudienceConfig.EMPTY;
    clientIds = DEFAULT_CLIENT_IDS;
    authenticators = null;
    peerAuthenticators = null;
    apiKeyRequired = false;
    apiLimitMetrics = ImmutableList.of();
  }

  public ApiKey getApiKey() {
    return new ApiKey(name, version, root);
  }

  public void setRoot(String root) {
    this.root = root;
  }

  public String getRoot() {
    return root;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setCanonicalName(String canonicalName) {
    this.canonicalName = canonicalName;
  }

  public String getCanonicalName() {
    return canonicalName;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public void setDocumentationLink(String documentationLink) {
    this.documentationLink = documentationLink;
  }

  public String getDocumentationLink() {
    return documentationLink;
  }

  public void setBackendRoot(String backendRoot) {
    this.backendRoot = toHttps(backendRoot);
  }

  public String getBackendRoot() {
    return backendRoot;
  }

  public void setIsAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public boolean getIsAbstract() {
    return isAbstract;
  }

  public void setIsDefaultVersion(boolean defaultVersion) {
    this.defaultVersion = defaultVersion;
  }

  public boolean getIsDefaultVersion() {
    return defaultVersion;
  }

  public void setIsDiscoverable(boolean discoverable) {
    this.discoverable = discoverable;
  }

  public boolean getIsDiscoverable() {
    return discoverable;
  }

  public void setUseDatastore(boolean useDatastore) {
    this.useDatastore = useDatastore;
  }

  public boolean getUseDatastore() {
    return useDatastore;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public String getResource() {
    return resource;
  }

  public ApiAuthConfig getAuthConfig() {
    return authConfig;
  }

  public ApiCacheControlConfig getCacheControlConfig() {
    return cacheControlConfig;
  }

  public ApiFrontendLimitsConfig getFrontendLimitsConfig() {
    return frontendLimitsConfig;
  }

  public ApiSerializationConfig getSerializationConfig() {
    return serializationConfig;
  }

  public ApiNamespaceConfig getNamespaceConfig() {
    return namespaceConfig;
  }

  // TODO: When builder is split off, these get/setters for default auth properties should
  // only be necessary in the builder.
  public void setAuthLevel(AuthLevel authLevel) {
    this.authLevel = authLevel;
  }

  public AuthLevel getAuthLevel() {
    return authLevel;
  }

  public void setScopeExpression(AuthScopeExpression scopeExpression) {
    this.scopeExpression = scopeExpression;
  }

  public AuthScopeExpression getScopeExpression() {
    return scopeExpression;
  }

  public void setAudiences(List<String> audiences) {
    this.audiences = audiences;
  }

  public List<String> getAudiences() {
    return audiences;
  }

  public void setIssuers(ApiIssuerConfigs issuers) {
    this.issuers = issuers;
  }

  public void ensureGoogleIssuer() {
    this.issuers = issuers.withGoogleIdToken();
  }

  public ApiIssuerConfigs getIssuers() {
    return issuers;
  }

  public void setIssuerAudiences(ApiIssuerAudienceConfig issuerAudiences) {
    Preconditions.checkNotNull(issuerAudiences, "issuerAudiences should never be null");
    this.issuerAudiences = issuerAudiences;
    if (issuerAudiences.hasIssuer(Constant.GOOGLE_ID_TOKEN_NAME)) {
      ensureGoogleIssuer();
    }
  }

  public ApiIssuerAudienceConfig getIssuerAudiences() {
    return issuerAudiences;
  }

  public void setClientIds(List<String> clientIds) {
    this.clientIds = clientIds;
  }

  public List<String> getClientIds() {
    return clientIds;
  }

  public void setAuthenticators(List<Class<? extends Authenticator>> authenticators) {
    this.authenticators = authenticators;
  }

  public List<Class<? extends Authenticator>> getAuthenticators() {
    return authenticators;
  }

  public void setPeerAuthenticators(List<Class<? extends PeerAuthenticator>> peerAuthenticators) {
    this.peerAuthenticators = peerAuthenticators;
  }

  public List<Class<? extends PeerAuthenticator>> getPeerAuthenticators() {
    return peerAuthenticators;
  }

  public void setApiKeyRequired(boolean apiKeyRequired) {
    this.apiKeyRequired = apiKeyRequired;
  }

  public boolean isApiKeyRequired() {
    return apiKeyRequired;
  }

  private String toHttps(String url) {
    if (url != null && url.startsWith("http:")) {
      return "https:" + url.substring(5);
    }
    return url;
  }

  public ApiClassConfig getApiClassConfig() {
    return apiClassConfig;
  }

  public void setApiLimitMetrics(List<ApiLimitMetricConfig> apiLimitMetrics) {
    this.apiLimitMetrics = apiLimitMetrics;
  }

  public List<ApiLimitMetricConfig> getApiLimitMetrics() {
    return apiLimitMetrics;
  }
}
