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

import com.google.api.server.spi.dispatcher.PathTrie.Result;
import com.google.common.base.Preconditions;

import java.io.IOException;

/**
 * A low-level dispatcher that executes a handler based on an HTTP method and a path. The dispatcher
 * is not responsible for most error handling. {@link DispatcherHandler} is allowed to throw an
 * {@link IOException} in the event that a low-level error (e.g. error writing a servlet response)
 * takes place. This is a fairly simple wrapper around {@link PathTrie}.
 */
public class PathDispatcher<ContextT extends DispatcherContext> {
  private final PathTrie<DispatcherHandler<ContextT>> trie;

  private PathDispatcher(Builder<ContextT> builder) {
    this.trie = builder.trieBuilder.build();
  }

  /**
   * Attempts to dispatch to a handler, given an HTTP method and path.
   *
   * @return whether or not a handler was executed
   * @throws IOException if the underlying handler threw an exception.
   */
  public boolean dispatch(String httpMethod, String path, ContextT context) throws IOException {
    Preconditions.checkNotNull(httpMethod, "httpMethod");
    Preconditions.checkNotNull(path, "path");
    HttpMethod method = HttpMethod.fromString(httpMethod);
    if (method != null) {
      Result<DispatcherHandler<ContextT>> result = trie.resolve(method, path);
      if (result != null) {
        context.setRawPathParameters(result.getRawParameters());
        result.getResult().handle(context);
        return true;
      }
    }
    return false;
  }

  public static <T extends DispatcherContext> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T extends DispatcherContext> {

    private final PathTrie.Builder<DispatcherHandler<T>> trieBuilder = PathTrie.builder(false);

    public Builder<T> add(String httpMethod, String pathTemplate, DispatcherHandler<T> handler) {
      Preconditions.checkNotNull(httpMethod, "httpMethod");
      Preconditions.checkNotNull(pathTemplate, "pathTemplate");
      Preconditions.checkNotNull(handler, "handler");
      trieBuilder.add(HttpMethod.valueOf(httpMethod.toUpperCase()), pathTemplate, handler);
      return this;
    }

    public PathDispatcher<T> build() {
      return new PathDispatcher<>(this);
    }
  }
}
