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
package com.google.api.server.spi.dispatcher;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

/**
 * Tests for {@link PathDispatcher}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PathDispatcherTest {
  private DispatcherContext context;
  @Mock private DispatcherHandler<DispatcherContext> getHandler;
  @Mock private DispatcherHandler<DispatcherContext> postHandler;

  @Before
  public void setUp() {
    context = new DispatcherContext("", "");
  }

  @Test
  public void simple() throws IOException {
    PathDispatcher<DispatcherContext> dispatcher = PathDispatcher.builder()
        .add("GET", "test/one/two/{three}", getHandler)
        .add("POST", "test/one/two/{three}", postHandler)
        .build();

    assertThat(dispatcher.dispatch("GET", "test/one/two/3", context)).isTrue();

    verify(getHandler, times(1)).handle(context);
    assertThat(context.getRawPathParameters()).isEqualTo(ImmutableMap.of("three", "3"));
  }

  @Test
  public void noTrieForHttpMethod() throws IOException {
    PathDispatcher<DispatcherContext> dispatcher = PathDispatcher.builder().build();
    assertThat(dispatcher.dispatch("GET", "test/one/two/3", context)).isFalse();
  }

  @Test
  public void notFound() throws IOException {
    PathDispatcher<DispatcherContext> dispatcher = PathDispatcher.builder()
        .add("GET", "test/one/two/{three}", getHandler)
        .build();

    assertThat(dispatcher.dispatch("GET", "test/one/two", context)).isFalse();
  }

  @Test
  public void dispatchNullHttpMethod() throws IOException {
    try {
      PathDispatcher.builder().build().dispatch(null, "", context);
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void dispatchNullPath() throws IOException {
    try {
      PathDispatcher.builder().build().dispatch("", null, context);
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void builderNullHttpMethod() {
    try {
      PathDispatcher.builder().add(null, "", getHandler);
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void builderNullPath() {
    try {
      PathDispatcher.builder().add("GET", null, getHandler);
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void builderNullHandler() {
    try {
      PathDispatcher.builder().add("GET", "", null);
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }
}
