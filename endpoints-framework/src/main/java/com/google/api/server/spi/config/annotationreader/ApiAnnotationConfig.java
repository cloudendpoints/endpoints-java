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
package com.google.api.server.spi.config.annotationreader;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiLimitMetric;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiIssuerAudienceConfig;
import com.google.api.server.spi.config.model.ApiIssuerConfigs;
import com.google.api.server.spi.config.model.ApiLimitMetricConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;

/**
 * Flattened configuration for a swarm endpoint.  Data generally originates from
 * {@link com.google.api.server.spi.config.Api} annotations.
 *
 * @author Eric Orth
 */
class ApiAnnotationConfig {
  private final ApiConfig config;

  public ApiAnnotationConfig(ApiConfig config) {
    this.config = config;
  }

  public ApiConfig getConfig() {
    return config;
  }

  public void setRootIfNotEmpty(String root) {
    if (!Strings.isNullOrEmpty(root)) {
      config.setRoot(root);
    }
  }

  public void setNameIfNotEmpty(String name) {
    if (!Strings.isNullOrEmpty(name)) {
      config.setName(name);
    }
  }

  public void setCanonicalNameIfNotEmpty(String canonicalName) {
    if (!Strings.isNullOrEmpty(canonicalName)) {
      config.setCanonicalName(canonicalName);
    }
  }

  public void setVersionIfNotEmpty(String version) {
    if (!Strings.isNullOrEmpty(version)) {
      config.setVersion(version);
    }
  }

  public void setTitleIfNotEmpty(String title) {
    if (!Strings.isNullOrEmpty(title)) {
      config.setTitle(title);
    }
  }

  public void setDescriptionIfNotEmpty(String description) {
    if (!Strings.isNullOrEmpty(description)) {
      config.setDescription(description);
    }
  }

  public void setDocumentationLinkIfNotEmpty(String documentationLink) {
    if (!Strings.isNullOrEmpty(documentationLink)) {
      config.setDocumentationLink(documentationLink);
    }
  }

  public void setBackendRootIfNotEmpty(String backendRoot) {
    if (!Strings.isNullOrEmpty(backendRoot)) {
      config.setBackendRoot(backendRoot);
    }
  }

  public void setIsAbstractIfSpecified(AnnotationBoolean isAbstract) {
    if (isAbstract == AnnotationBoolean.TRUE) {
      config.setIsAbstract(true);
    } else if (isAbstract == AnnotationBoolean.FALSE) {
      config.setIsAbstract(false);
    }
  }

  public void setIsDefaultVersionIfSpecified(AnnotationBoolean defaultVersion) {
    if (defaultVersion == AnnotationBoolean.TRUE) {
      config.setIsDefaultVersion(true);
    } else if (defaultVersion == AnnotationBoolean.FALSE) {
      config.setIsDefaultVersion(false);
    }
  }

  public void setIsDiscoverableIfSpecified(AnnotationBoolean discoverable) {
    if (discoverable == AnnotationBoolean.TRUE) {
      config.setIsDiscoverable(true);
    } else if (discoverable == AnnotationBoolean.FALSE) {
      config.setIsDiscoverable(false);
    }
  }

  public void setUseDatastoreIfSpecified(AnnotationBoolean useDatastore) {
    if (useDatastore == AnnotationBoolean.TRUE) {
      config.setUseDatastore(true);
    } else if (useDatastore == AnnotationBoolean.FALSE) {
      config.setUseDatastore(false);
    }
  }

  public void setResourceIfNotEmpty(String resource) {
    if (!resource.isEmpty()) {
      config.setResource(resource);
    }
  }

  public void setAuthLevelIfSpecified(AuthLevel authLevel) {
    if (authLevel != AuthLevel.UNSPECIFIED) {
      config.setAuthLevel(authLevel);
    }
  }

  public void setScopesIfSpecified(String[] scopes) {
    if (!AnnotationUtil.isUnspecified(scopes)) {
      config.setScopeExpression(AuthScopeExpressions.interpret(scopes));
    }
  }

  public void setAudiencesIfSpecified(String[] audiences) {
    if (!AnnotationUtil.isUnspecified(audiences)) {
      config.setAudiences(Arrays.asList(audiences));
    }
  }

  public void setIssuersIfSpecified(ApiIssuerConfigs issuers) {
    if (issuers.isSpecified()) {
      config.setIssuers(issuers);
    }
  }

  public void setIssuerAudiencesIfSpecified(ApiIssuerAudienceConfig issuerAudiences) {
    if (issuerAudiences.isSpecified()) {
      config.setIssuerAudiences(issuerAudiences);
    }
  }

  public void setClientIdsIfSpecified(String[] clientIds) {
    if (!AnnotationUtil.isUnspecified(clientIds)) {
      config.setClientIds(Arrays.asList(clientIds));
    }
  }

  public void setAuthenticatorsIfSpecified(Class<? extends Authenticator>[] authenticators) {
    if (!AnnotationUtil.isUnspecified(authenticators)) {
      config.setAuthenticators(Arrays.asList(authenticators));
    }
  }

  public void setPeerAuthenticatorsIfSpecified(
      Class<? extends PeerAuthenticator>[] peerAuthenticators) {
    if (!AnnotationUtil.isUnspecifiedPeerAuthenticators(peerAuthenticators)) {
      config.setPeerAuthenticators(Arrays.asList(peerAuthenticators));
    }
  }

  public void setApiKeyRequiredIfSpecified(AnnotationBoolean apiKeyRequired) {
    if (apiKeyRequired == AnnotationBoolean.TRUE) {
      config.setApiKeyRequired(true);
    } else if (apiKeyRequired == AnnotationBoolean.FALSE) {
      config.setApiKeyRequired(false);
    }
  }

  public void setApiLimitMetrics(ApiLimitMetric[] apiLimitMetrics) {
    ImmutableList.Builder<ApiLimitMetricConfig> metricConfigs = ImmutableList.builder();
    if (apiLimitMetrics != null && apiLimitMetrics.length > 0) {
      for (ApiLimitMetric metric : apiLimitMetrics) {
        metricConfigs.add(ApiLimitMetricConfig.builder()
            .setName(metric.name())
            .setDisplayName(metric.displayName())
            .setLimit(metric.limit())
            .build());
      }
    }
    config.setApiLimitMetrics(metricConfigs.build());
  }
}
