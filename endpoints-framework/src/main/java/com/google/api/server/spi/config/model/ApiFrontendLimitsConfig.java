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


import com.google.api.server.spi.config.ApiConfigInconsistency;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Flattened frontend limits configuration for a swarm endpoint.  Data generally originates from
 * {@link com.google.api.server.spi.config.ApiFrontendLimits} annotations.
 *
 * @author Eric Orth
 */
public class ApiFrontendLimitsConfig {
  private int unregisteredUserQps;
  private int unregisteredQps;
  private int unregisteredDaily;

  private final Map<String, FrontendLimitsRule> rules;

  public ApiFrontendLimitsConfig() {
    rules = new LinkedHashMap<String, FrontendLimitsRule>();

    setDefaults();
  }

  public ApiFrontendLimitsConfig(ApiFrontendLimitsConfig original) {
    this.unregisteredUserQps = original.unregisteredUserQps;
    this.unregisteredQps = original.unregisteredQps;
    this.unregisteredDaily = original.unregisteredDaily;

    this.rules = new LinkedHashMap<String, FrontendLimitsRule>(original.rules);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof ApiFrontendLimitsConfig) {
      ApiFrontendLimitsConfig config = (ApiFrontendLimitsConfig) o;
      return Iterables.isEmpty(getConfigurationInconsistencies(config));
    } else {
      return false;
    }
  }

  public Iterable<ApiConfigInconsistency<Object>> getConfigurationInconsistencies(
      ApiFrontendLimitsConfig config) {
    return ApiConfigInconsistency.listBuilder()
        .addIfInconsistent("frontendLimits.unregisteredUserQps", unregisteredUserQps,
            config.unregisteredUserQps)
        .addIfInconsistent("frontendLimits.unregisteredQps", unregisteredQps,
            config.unregisteredQps)
        .addIfInconsistent("frontendLimits.unregisteredDaily", unregisteredDaily,
            config.unregisteredDaily)
        .addIfInconsistent("frontendLimits.rules", rules, config.rules)
        .build();
  }

  @Override
  public int hashCode() {
    return Objects.hash(unregisteredUserQps, unregisteredQps, unregisteredDaily, rules);
  }

  /**
   * Sets all fields to their default value to be used if not set otherwise.  Override to change the
   * default configuration.
   */
  protected void setDefaults() {
    unregisteredUserQps = -1;
    unregisteredQps = -1;
    unregisteredDaily = -1;
  }

  public void setUnregisteredUserQps(int unregisteredUserQps) {
    this.unregisteredUserQps = unregisteredUserQps;
  }

  public int getUnregisteredUserQps() {
    return unregisteredUserQps;
  }

  public void setUnregisteredQps(int unregisteredQps) {
    this.unregisteredQps = unregisteredQps;
  }

  public int getUnregisteredQps() {
    return unregisteredQps;
  }

  public void setUnregisteredDaily(int unregisteredDaily) {
    this.unregisteredDaily = unregisteredDaily;
  }

  public int getUnregisteredDaily() {
    return unregisteredDaily;
  }

  public void addRule(String match, int qps, int userQps, int daily, String analyticsId) {
    rules.put(match, new FrontendLimitsRule(match, qps, userQps, daily, analyticsId));
  }

  public List<FrontendLimitsRule> getRules() {
    return new ArrayList<FrontendLimitsRule>(rules.values());
  }

  /**
   * Pure read-only data object to represent a single matching rule for the frontend limits
   * configuration.
   */
  public static class FrontendLimitsRule {
    private final String match;
    private final int qps;
    private final int userQps;
    private final int daily;
    private final String analyticsId;

    public FrontendLimitsRule(String match, int qps, int userQps, int daily, String analyticsId) {
      this.match = match;
      this.qps = qps;
      this.userQps = userQps;
      this.daily = daily;
      this.analyticsId = analyticsId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (o instanceof FrontendLimitsRule) {
        FrontendLimitsRule rule = (FrontendLimitsRule) o;
        return Objects.equals(match, rule.match) && qps == rule.qps && userQps == rule.userQps &&
            daily == rule.daily && Objects.equals(analyticsId, rule.analyticsId);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(match, qps, userQps, daily, analyticsId);
    }

    public String getMatch() {
      return match;
    }

    public int getQps() {
      return qps;
    }

    public int getUserQps() {
      return userQps;
    }

    public int getDaily() {
      return daily;
    }

    public String getAnalyticsId() {
      return analyticsId;
    }
  }
}
