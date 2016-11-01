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

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.model.ApiIssuerAudienceConfig;
import com.google.api.server.spi.config.model.ApiIssuerConfigs;
import com.google.api.server.spi.config.model.ApiIssuerConfigs.IssuerConfig;

/**
 * Utilities for converting third-party issuer annotations to internal models.
 */
public class IssuerUtil {
  private IssuerUtil() { }

  public static ApiIssuerConfigs toConfig(ApiIssuer[] issuerConfigs) {
    if (issuerConfigs.length == 1
        && Api.UNSPECIFIED_STRING_FOR_LIST.equals(issuerConfigs[0].name())) {
      return ApiIssuerConfigs.UNSPECIFIED;
    }
    ApiIssuerConfigs.Builder builder = ApiIssuerConfigs.builder();
    for (ApiIssuer issuerConfig : issuerConfigs) {
      builder.addIssuer(
          new IssuerConfig(issuerConfig.name(), issuerConfig.issuer(), issuerConfig.jwksUri()));
    }
    return builder.build();
  }

  public static ApiIssuerAudienceConfig toConfig(ApiIssuerAudience[] issuers) {
    boolean thirdPartyIssuersUnspecified =
        issuers.length == 1 && Api.UNSPECIFIED_STRING_FOR_LIST.equals(issuers[0].name());
    if (thirdPartyIssuersUnspecified) {
      return ApiIssuerAudienceConfig.UNSPECIFIED;
    }
    ApiIssuerAudienceConfig.Builder builder = ApiIssuerAudienceConfig.builder();
    for (ApiIssuerAudience issuer : issuers) {
      builder.addIssuerAudiences(issuer.name(), issuer.audiences());
    }
    return builder.build();
  }
}
