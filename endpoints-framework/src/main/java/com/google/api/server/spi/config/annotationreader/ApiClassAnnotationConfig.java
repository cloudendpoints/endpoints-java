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
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Authenticator;
import com.google.api.server.spi.config.PeerAuthenticator;
import com.google.api.server.spi.config.model.ApiClassConfig;
import com.google.api.server.spi.config.model.ApiIssuerAudienceConfig;
import com.google.api.server.spi.config.scope.AuthScopeExpressions;

import java.util.Arrays;

/**
 * Annotaion-specific setting helpers for {@link ApiClassConfig}.
 *
 * @author Eric Orth
 */
public class ApiClassAnnotationConfig {
  private final ApiClassConfig config;

  public ApiClassAnnotationConfig(ApiClassConfig config) {
    this.config = config;
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

  public void setUseDatastoreIfSpecified(AnnotationBoolean useDatastore) {
    if (useDatastore == AnnotationBoolean.TRUE) {
      config.setUseDatastore(true);
    } else if (useDatastore == AnnotationBoolean.FALSE) {
      config.setUseDatastore(false);
    }
  }

  public void setApiKeyRequiredIfSpecified(AnnotationBoolean apiKeyRequired) {
    if (apiKeyRequired == AnnotationBoolean.TRUE) {
      config.setApiKeyRequired(true);
    } else if (apiKeyRequired == AnnotationBoolean.FALSE) {
      config.setApiKeyRequired(false);
    }
  }
}
