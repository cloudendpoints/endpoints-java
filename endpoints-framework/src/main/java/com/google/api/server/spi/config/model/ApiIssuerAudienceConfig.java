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

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Objects;

/**
 * A helper class representing a map from issuer names to accepted audiences.
 */
public class ApiIssuerAudienceConfig {
  private static final String UNSPECIFIED_NAME = "_unspecified_issuer_name";
  private static final String UNSPECIFIED_AUDIENCE = "_unspecified_audience";
  public static final ApiIssuerAudienceConfig UNSPECIFIED = builder()
      .addIssuerAudiences(UNSPECIFIED_NAME, UNSPECIFIED_AUDIENCE)
      .build();
  public static final ApiIssuerAudienceConfig EMPTY = builder().build();
  private final ImmutableListMultimap<String, String> issuerAudiences;

  private ApiIssuerAudienceConfig(ApiIssuerAudienceConfig.Builder builder) {
    this.issuerAudiences = builder.issuerAudiences.build();
  }

  public ImmutableMap<String, Collection<String>> asMap() {
    return issuerAudiences.asMap();
  }

  public boolean isSpecified() {
    return !this.equals(UNSPECIFIED);
  }

  public boolean isEmpty() {
    return issuerAudiences.isEmpty();
  }

  public boolean hasIssuer(String issuer) {
    return issuerAudiences.containsKey(issuer);
  }

  @Override
  public boolean equals(Object o) {
    return o != null && o instanceof ApiIssuerAudienceConfig
        && Objects.equals(issuerAudiences, ((ApiIssuerAudienceConfig) o).issuerAudiences);
  }

  @Override
  public int hashCode() {
    return issuerAudiences.hashCode();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final ImmutableListMultimap.Builder<String, String> issuerAudiences =
        ImmutableListMultimap.builder();

    public Builder addIssuerAudiences(String issuer, String... audiences) {
      issuerAudiences.putAll(issuer, audiences);
      return this;
    }

    public ApiIssuerAudienceConfig build() {
      return new ApiIssuerAudienceConfig(this);
    }
  }
}
