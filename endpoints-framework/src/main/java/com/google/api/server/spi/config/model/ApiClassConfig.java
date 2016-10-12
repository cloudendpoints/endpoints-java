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
import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.scope.AuthScopeExpression;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Flattened configuration for a Swarm API class.  Data generally originates from
 * {@link com.google.api.server.spi.config.ApiClass} annotations.
 *
 * @author Eric Orth
 */
public class ApiClassConfig {
  private final ApiConfig apiConfig;

  private final String apiClassJavaName;
  private final String apiClassJavaSimpleName;
  private final TypeLoader typeLoader;

  private String resource;
  private Boolean useDatastore;

  private AuthLevel authLevel;
  private AuthScopeExpression scopeExpression;
  private List<String> audiences;
  private ApiIssuerAudienceConfig issuerAudiences;
  private List<String> clientIds;
  private List<Class<? extends Authenticator>> authenticators;
  private List<Class<? extends PeerAuthenticator>> peerAuthenticators;
  private Boolean apiKeyRequired;

  private final MethodConfigMap methods;

  public ApiClassConfig(ApiConfig apiConfig, TypeLoader typeLoader, Class<?> apiClass) {
    this.apiConfig = apiConfig;
    this.apiClassJavaName = apiClass.getName();
    this.apiClassJavaSimpleName = apiClass.getSimpleName();
    this.typeLoader = typeLoader;
    this.resource = null;
    this.authLevel = AuthLevel.UNSPECIFIED;
    this.scopeExpression = null;
    this.audiences = null;
    this.issuerAudiences = ApiIssuerAudienceConfig.UNSPECIFIED;
    this.clientIds = null;
    this.authenticators = null;
    this.peerAuthenticators = null;
    this.useDatastore = null;
    this.methods = new MethodConfigMap(this);
    this.apiKeyRequired = null;
  }

  public ApiClassConfig(ApiClassConfig original, ApiConfig apiConfig) {
    this.apiConfig = apiConfig;
    this.apiClassJavaName = original.apiClassJavaName;
    this.apiClassJavaSimpleName = original.apiClassJavaSimpleName;
    this.typeLoader = original.typeLoader;
    this.resource = original.resource;
    this.authLevel = original.authLevel;
    this.scopeExpression = original.scopeExpression;
    this.audiences = original.audiences == null ? null : new ArrayList<>(original.audiences);
    this.issuerAudiences = original.issuerAudiences;
    this.clientIds = original.clientIds == null ? null : new ArrayList<>(original.clientIds);
    this.authenticators =
        original.authenticators == null ? null : new ArrayList<>(original.authenticators);
    this.peerAuthenticators =
        original.peerAuthenticators == null ? null : new ArrayList<>(original.peerAuthenticators);
    this.useDatastore = original.useDatastore;
    this.methods = new MethodConfigMap(original.methods, this);
    this.apiKeyRequired = original.apiKeyRequired;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof ApiClassConfig) {
      ApiClassConfig config = (ApiClassConfig) o;
      return Objects.equals(apiClassJavaName, config.apiClassJavaName) &&
          Objects.equals(apiClassJavaSimpleName, config.apiClassJavaSimpleName) &&
          Objects.equals(typeLoader, config.typeLoader) &&
          Objects.equals(resource, config.resource) &&
          Objects.equals(authLevel, config.authLevel) &&
          Objects.equals(scopeExpression, config.scopeExpression) &&
          Objects.equals(audiences, config.audiences) &&
          Objects.equals(issuerAudiences, config.issuerAudiences) &&
          Objects.equals(clientIds, config.clientIds) &&
          Objects.equals(authenticators, config.authenticators) &&
          Objects.equals(peerAuthenticators, config.peerAuthenticators) &&
          Objects.equals(useDatastore, config.useDatastore) &&
          methods.equals(config.methods) &&
          Objects.equals(apiKeyRequired, config.apiKeyRequired);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiClassJavaName, apiClassJavaSimpleName, typeLoader, resource,
        authLevel, scopeExpression, audiences, clientIds, authenticators, peerAuthenticators, 
        useDatastore, methods, issuerAudiences, apiKeyRequired);
  }

  public ApiConfig getApiConfig() {
    return apiConfig;
  }

  public String getApiClassJavaName() {
    return apiClassJavaName;
  }

  public String getApiClassJavaSimpleName() {
    return apiClassJavaSimpleName;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public String getResource() {
    return resource != null ? resource : apiConfig.getResource();
  }

  public void setAuthLevel(AuthLevel authLevel) {
    this.authLevel = authLevel;
  }

  public AuthLevel getAuthLevel() {
    return authLevel != AuthLevel.UNSPECIFIED ? authLevel : apiConfig.getAuthLevel();
  }

  public void setScopeExpression(AuthScopeExpression scopeExpression) {
    this.scopeExpression = scopeExpression;
  }

  public AuthScopeExpression getScopeExpression() {
    return scopeExpression != null ? scopeExpression : apiConfig.getScopeExpression();
  }

  public void setAudiences(List<String> audiences) {
    this.audiences = audiences;
  }

  public List<String> getAudiences() {
    return audiences != null ? audiences : apiConfig.getAudiences();
  }

  public void setIssuerAudiences(ApiIssuerAudienceConfig issuerAudiences) {
    Preconditions.checkNotNull(issuerAudiences, "issuerAudiences should never be null");
    this.issuerAudiences = issuerAudiences;
    if (issuerAudiences.hasIssuer(Constant.GOOGLE_ID_TOKEN_NAME)) {
      getApiConfig().ensureGoogleIssuer();
    }
  }

  public ApiIssuerAudienceConfig getIssuerAudiences() {
    return issuerAudiences.isSpecified() ? issuerAudiences : apiConfig.getIssuerAudiences();
  }

  public void setClientIds(List<String> clientIds) {
    this.clientIds = clientIds;
  }

  public List<String> getClientIds() {
    return clientIds != null ? clientIds : apiConfig.getClientIds();
  }

  public void setAuthenticators(List<Class<? extends Authenticator>> authenticators) {
    this.authenticators = authenticators;
  }

  public List<Class<? extends Authenticator>> getAuthenticators() {
    return authenticators != null ? authenticators : apiConfig.getAuthenticators();
  }


  public void setPeerAuthenticators(List<Class<? extends PeerAuthenticator>> peerAuthenticators) {
    this.peerAuthenticators = peerAuthenticators;
  }

  public List<Class<? extends PeerAuthenticator>> getPeerAuthenticators() {
    return peerAuthenticators != null ? peerAuthenticators : apiConfig.getPeerAuthenticators();
  }

  public void setUseDatastore(boolean useDatastore) {
    this.useDatastore = useDatastore;
  }

  public boolean getUseDatastore() {
    return useDatastore != null ? useDatastore : apiConfig.getUseDatastore();
  }

  public MethodConfigMap getMethods() {
    return methods;
  }

  public void setApiKeyRequired(boolean apiKeyRequired) {
    this.apiKeyRequired = apiKeyRequired;
  }

  public boolean isApiKeyRequired() {
    return apiKeyRequired != null ? apiKeyRequired : apiConfig.isApiKeyRequired();
  }

  /**
   * {@link Map} of API methods for this API class.
   */
  public static class MethodConfigMap extends LinkedHashMap<EndpointMethod, ApiMethodConfig> {
    private final ApiClassConfig apiClassConfig;

    protected MethodConfigMap(ApiClassConfig apiClassConfig) {
      this.apiClassConfig = apiClassConfig;
    }

    // Deep-copy constructor.
    protected MethodConfigMap(MethodConfigMap original, ApiClassConfig apiClassConfig) {
      this.apiClassConfig = apiClassConfig;

      for (Map.Entry<EndpointMethod, ApiMethodConfig> entry : original.entrySet()) {
        this.put(entry.getKey(), new ApiMethodConfig(entry.getValue(), apiClassConfig));
      }
    }

    /**
     * Gets the previously created {@link ApiMethodConfig} instance for the given method, or creates
     * a new one if it does not yet exist and puts it in the underlying map.  Created
     * {@link ApiMethodConfig} are created with defaults based on the current configuration loaded
     * in the ApiConfig.
     */
    public ApiMethodConfig getOrCreate(EndpointMethod method) {
      if (containsKey(method)) {
        return get(method);
      }

      ApiMethodConfig methodConfig =
          createMethodConfig(method, apiClassConfig.typeLoader, apiClassConfig);
      put(method, methodConfig);
      return methodConfig;
    }

    /**
     * Create the method configuration object.  Override to use a subclass instead.
     */
    protected ApiMethodConfig createMethodConfig(EndpointMethod method, TypeLoader typeLoader,
        ApiClassConfig apiClassConfig) {
      return new ApiMethodConfig(method, typeLoader, apiClassConfig);
    }

    public ApiClassConfig getApiClassConfig() {
      return apiClassConfig;
    }
  }
}
