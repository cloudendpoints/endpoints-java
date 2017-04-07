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

import static com.google.common.truth.Truth.assertThat;

import com.google.api.server.spi.EndpointsContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for {@link StandardParameters}.
 */
@RunWith(JUnit4.class)
public class StandardParametersTest {
  @Test
  public void shouldPrettyPrint_defaultValueIsTrue_globalDefaultTrue() {
    assertThat(StandardParameters.shouldPrettyPrint(
        getEndpointsContext(new MockHttpServletRequest(), true))).isTrue();
  }

  @Test
  public void shouldPrettyPrint_false_globalDefaultTrue() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("prettyPrint", "false");
    assertThat(StandardParameters.shouldPrettyPrint(getEndpointsContext(request, true))).isFalse();
  }

  @Test
  public void shouldPrettyPrint_true_globalDefaultTrue() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("prettyPrint", "true");
    assertThat(StandardParameters.shouldPrettyPrint(getEndpointsContext(request, true))).isTrue();
  }

  @Test
  public void shouldPrettyPrint_defaultValueIsFalse_globalDefaultFalse() {
    assertThat(StandardParameters.shouldPrettyPrint(
        getEndpointsContext(new MockHttpServletRequest(), false))).isFalse();
  }

  @Test
  public void shouldPrettyPrint_false_globalDefaultFalse() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("prettyPrint", "false");
    assertThat(StandardParameters.shouldPrettyPrint(getEndpointsContext(request, false))).isFalse();
  }

  @Test
  public void shouldPrettyPrint_true_globalDefaultFalse() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("prettyPrint", "true");
    assertThat(StandardParameters.shouldPrettyPrint(getEndpointsContext(request, false))).isTrue();
  }

  private EndpointsContext getEndpointsContext(
      MockHttpServletRequest request, boolean prettyPrint) {
    return new EndpointsContext("GET", "/", request, new MockHttpServletResponse(), prettyPrint);
  }

  @Test
  public void isStandardParamName() {
    assertThat(StandardParameters.isStandardParamName("alt")).isTrue();
    assertThat(StandardParameters.isStandardParamName("fields")).isTrue();
    assertThat(StandardParameters.isStandardParamName("key")).isTrue();
    assertThat(StandardParameters.isStandardParamName("oauth_token")).isTrue();
    assertThat(StandardParameters.isStandardParamName("prettyPrint")).isTrue();
    assertThat(StandardParameters.isStandardParamName("quotaUser")).isTrue();
    assertThat(StandardParameters.isStandardParamName("userIp")).isTrue();
    assertThat(StandardParameters.isStandardParamName("sanityCheck")).isFalse();
  }
}
