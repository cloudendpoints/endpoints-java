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

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.google.common.flogger.FluentLogger;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple trie that maps pairs of HTTP methods and parameterized paths to arbitrary data. Each
 * node in the tree is a path segment. For example, given a path "discovery/v1/apis", the data would
 * be stored in the node path represented by "discovery" -&gt; "v1" -&gt; "apis". A path is
 * considered parameterized if one or more segments is of the form "{name}". When a parameterized
 * path is resolved, a map from parameter names to raw String values is returned as part of the
 * result. Null values are not acceptable values in this trie. Parameter names can only contain
 * alphanumeric characters or underscores, and cannot start with a numeric.
 */
public class PathTrie<T> {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final Splitter PATH_SPLITTER = Splitter.on('/');
  private static final String PARAMETER_PATH_SEGMENT = "{}";
  private static final Pattern PARAMETER_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z_\\d]*");
  // General delimiters that must be URL encoded, as defined by RFC 3986.
  private static final CharMatcher RESERVED_URL_CHARS = CharMatcher.anyOf(":/?#[]{}");

  private final ImmutableMap<String, PathTrie<T>> subTries;
  private final ImmutableMap<HttpMethod, MethodInfo<T>> httpMethodMap;

  private PathTrie(Builder<T> builder) {
    this.httpMethodMap = ImmutableMap.copyOf(builder.httpMethodMap);
    ImmutableMap.Builder<String, PathTrie<T>> subTriesBuilder = ImmutableMap.builder();
    for (Entry<String, Builder<T>> entry : builder.subBuilders.entrySet()) {
      subTriesBuilder.put(entry.getKey(), new PathTrie<>(entry.getValue()));
    }
    this.subTries = subTriesBuilder.build();
  }

  /**
   * Attempts to resolve a path. Resolution prefers literal paths over path parameters. The result
   * includes the object to which the path mapped, as well a map from parameter names to
   * URL-decoded values. If the path cannot be resolved, null is returned.
   */
  public Result<T> resolve(HttpMethod method, String path) {
    Preconditions.checkNotNull(method, "method");
    Preconditions.checkNotNull(path, "path");
    return resolve(method, getPathSegments(path), 0, new ArrayList<String>());
  }

  private Result<T> resolve(
      HttpMethod method, List<String> pathSegments, int index, List<String> rawParameters) {
    if (index < pathSegments.size()) {
      String segment = pathSegments.get(index);
      PathTrie<T> subTrie = subTries.get(segment);
      if (subTrie != null) {
        Result<T> result = subTrie.resolve(method, pathSegments, index + 1, rawParameters);
        if (result != null) {
          return result;
        }
      }
      subTrie = subTries.get(PARAMETER_PATH_SEGMENT);
      if (subTrie != null) {
        // TODO: We likely need to enforce non-empty values here.
        rawParameters.add(segment);
        Result<T> result = subTrie.resolve(method, pathSegments, index + 1, rawParameters);
        if (result == null) {
          rawParameters.remove(rawParameters.size() - 1);
        }
        return result;
      }
      return null;
    } else if (httpMethodMap.containsKey(method)) {
      MethodInfo<T> methodInfo = httpMethodMap.get(method);
      ImmutableList<String> parameterNames = methodInfo.parameterNames;
      Preconditions.checkState(rawParameters.size() == parameterNames.size());
      Map<String, String> rawParameterMap = Maps.newHashMap();
      for (int i = 0; i < parameterNames.size(); i++) {
        rawParameterMap.put(parameterNames.get(i), decodeUri(rawParameters.get(i)));
      }
      return new Result<>(methodInfo.value, rawParameterMap);
    }
    return null;
  }

  /**
   * The resulting information for a successful path resolution, which includes the value to which
   * the path maps, as well as the raw (but URL decoded) string values of all path parameters.
   */
  public static class Result<T> {
    private final T result;
    private final Map<String, String> rawParameters;

    public Result(T result, Map<String, String> rawParameters) {
      this.result = result;
      this.rawParameters = rawParameters;
    }

    public T getResult() {
      return result;
    }

    public Map<String, String> getRawParameters() {
      return rawParameters;
    }
  }

  /**
   * Returns a new, path conflict validating {@link PathTrie.Builder}.
   *
   * @param <T> the type that the trie will be storing
   */
  public static <T> Builder<T> builder() {
    return new Builder<>(true);
  }

  /**
   * Returns a new {@link PathTrie.Builder}.
   *
   * @param throwOnConflict whether or not to throw an exception on path conflicts
   * @param <T> the type that the trie will be storing
   */
  public static <T> Builder<T> builder(boolean throwOnConflict) {
    return new Builder<>(throwOnConflict);
  }

  /**
   * A builder for creating a {@link PathTrie}, which is immutable.
   */
  public static class Builder<T> {
    private final Map<String, Builder<T>> subBuilders = Maps.newHashMap();
    private final Map<HttpMethod, MethodInfo<T>> httpMethodMap = new EnumMap<>(HttpMethod.class);
    private final boolean throwOnConflict;

    public Builder(boolean throwOnConflict) {
      this.throwOnConflict = throwOnConflict;
    }

    /**
     * Adds a path to the trie.
     *
     * @throws IllegalArgumentException if the path cannot be added to the trie
     * @throws NullPointerException if either path or value are null
     */
    public Builder<T> add(HttpMethod method, String path, T value) {
      Preconditions.checkNotNull(method, "method");
      Preconditions.checkNotNull(path, "path");
      Preconditions.checkNotNull(value, "value");
      // TODO: We likely want to do something about trailing slashes here (make configurable)
      add(method, path, getPathSegments(path).iterator(), value, new ArrayList<String>());
      return this;
    }

    public PathTrie<T> build() {
      return new PathTrie<>(this);
    }

    private void add(HttpMethod method, String path, Iterator<String> pathSegments, T value,
        List<String> parameterNames) {
      if (pathSegments.hasNext()) {
        String segment = pathSegments.next();
        if (segment.startsWith("{")) {
          if (segment.endsWith("}")) {
            parameterNames.add(getAndCheckParameterName(segment));
            getOrCreateSubBuilder(PARAMETER_PATH_SEGMENT)
                .add(method, path, pathSegments, value, parameterNames);
          } else {
            throw new IllegalArgumentException(
                String.format("'%s' contains invalid parameter syntax: %s", path, segment));
          }
        } else {
          if (RESERVED_URL_CHARS.matchesAnyOf(segment)) {
            throw new IllegalArgumentException(
                String.format("'%s' contains invalid path segment: %s", path, segment));
          }
          getOrCreateSubBuilder(segment).add(method, path, pathSegments, value, parameterNames);
        }
      } else {
        boolean pathExists = httpMethodMap.containsKey(method);
        if (pathExists && throwOnConflict) {
          throw new IllegalArgumentException(String.format("Path '%s' is already mapped", path));
        }
        if (pathExists) {
          log.atWarning().log("Path '%s' is already mapped, but overwriting it", path);
        }
        httpMethodMap.put(method, new MethodInfo<>(parameterNames, value));
      }
    }

    private String getAndCheckParameterName(String segment) {
      String name = segment.substring(1, segment.length() - 1);
      Matcher matcher = PARAMETER_NAME_PATTERN.matcher(name);
      if (!matcher.matches()) {
        throw new IllegalArgumentException(
            String.format("'%s' not a valid path parameter name", name));
      }
      return name;
    }

    private Builder<T> getOrCreateSubBuilder(String segment) {
      Builder<T> subBuilder = subBuilders.get(segment);
      if (subBuilder == null) {
        subBuilder = builder(throwOnConflict);
        subBuilders.put(segment, subBuilder);
      }
      return subBuilder;
    }
  }

  private static List<String> getPathSegments(String path) {
    return PATH_SPLITTER.splitToList(path);
  }

  private static String decodeUri(String value) {
    try {
      return URLDecoder.decode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return value;
    }
  }

  private static class MethodInfo<T> {
    private final ImmutableList<String> parameterNames;
    private final T value;

    MethodInfo(List<String> parameterNames, T value) {
      this.parameterNames = ImmutableList.copyOf(parameterNames);
      this.value = value;
    }
  }
}
