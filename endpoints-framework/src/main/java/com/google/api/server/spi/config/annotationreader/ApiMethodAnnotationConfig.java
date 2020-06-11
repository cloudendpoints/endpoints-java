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
import com.google.api.server.spi.config.ApiMetricCost;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.model.ApiIssuerAudienceConfig;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiMetricCostConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;

/**
 * Flattened method configuration for a swarm endpoint method. Data generally originates from
 * {@link com.google.api.server.spi.config.ApiMethod} annotations.
 *
 * @author Eric Orth
 */
class ApiMethodAnnotationConfig {
  private final ApiMethodConfig config;

  public ApiMethodAnnotationConfig(ApiMethodConfig config) {
    this.config = config;
  }

  public ApiMethodConfig getConfig() {
    return config;
  }

  public void setNameIfNotEmpty(String name) {
    if (name != null && !name.isEmpty()) {
      config.setName(name);
    }
  }

  public void setDescriptionIfNotEmpty(String description) {
    if (description != null && !description.isEmpty()) {
      config.setDescription(description);
    }
  }

  public void setPathIfNotEmpty(String path) {
    if (path != null && !path.isEmpty()) {
      config.setPath(path);
    }
  }

  public void setHttpMethodIfNotEmpty(String httpMethod) {
    if (httpMethod != null && !httpMethod.isEmpty()) {
      config.setHttpMethod(httpMethod);
    }
  }

  public void setResponseStatus(int responseStatus) {
    config.setResponseStatus(responseStatus);
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

  public void setIgnoredIfSpecified(AnnotationBoolean ignored) {
    if (ignored == AnnotationBoolean.TRUE) {
      config.setIgnored(true);
    } else if (ignored == AnnotationBoolean.FALSE) {
      config.setIgnored(false);
    }
  }

  public void setApiKeyRequiredIfSpecified(AnnotationBoolean apiKeyRequired) {
    if (apiKeyRequired == AnnotationBoolean.TRUE) {
      config.setApiKeyRequired(true);
    } else if (apiKeyRequired == AnnotationBoolean.FALSE) {
      config.setApiKeyRequired(false);
    }
  }

  public void setMetricCosts(ApiMetricCost[] metricCosts) {
    ImmutableList.Builder<ApiMetricCostConfig> costs = ImmutableList.builder();
    if (metricCosts != null && metricCosts.length > 0) {
      for (ApiMetricCost cost : metricCosts) {
        costs.add(ApiMetricCostConfig.builder()
            .setName(cost.name())
            .setCost(cost.cost())
            .build());
      }
    }
    config.setMetricCosts(costs.build());
  }
}
