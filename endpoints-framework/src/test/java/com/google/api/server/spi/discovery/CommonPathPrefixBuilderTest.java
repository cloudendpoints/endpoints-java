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
package com.google.api.server.spi.discovery;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link CommonPathPrefixBuilder}.
 */
@RunWith(JUnit4.class)
public class CommonPathPrefixBuilderTest {
  private CommonPathPrefixBuilder builder;

  @Before
  public void setUp() {
    builder = new CommonPathPrefixBuilder();
  }

  @Test
  public void getCommonPrefix() {
    builder.addPath("discovery/v1/apis");
    builder.addPath("discovery/v1/apis/{api}/rest");
    assertThat(builder.getCommonPrefix()).isEqualTo("discovery/v1/");
  }

  @Test
  public void getCommonPrefix_noPathParams() {
    builder.addPath("discovery/v1/apis/{api}/rest");
    builder.addPath("discovery/v1/apis/{api}/rpc");
    assertThat(builder.getCommonPrefix()).isEqualTo("discovery/v1/apis/");
  }

  @Test
  public void getCommonPrefix_noCommonPath() {
    builder.addPath("discovery/v1/apis");
    builder.addPath("tictactoe/v1/board");
    assertThat(builder.getCommonPrefix()).isEmpty();
  }
}