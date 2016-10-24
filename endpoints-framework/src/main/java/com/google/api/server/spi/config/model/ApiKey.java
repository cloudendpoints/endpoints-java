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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * An immutable name which fully identifies an API.
 */
public final class ApiKey {

  private String name;
  private String version;
  private String root;

  public ApiKey(String name, String version) {
    this(name, version, null);
  }

  public ApiKey(String name, String version, @Nullable String root) {
    this.name = Preconditions.checkNotNull(name);
    this.version = Preconditions.checkNotNull(version);
    this.root = root;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  @Nullable
  public String getRoot() {
    return root;
  }

  /**
   * Returns a string that uniquely identifies this API in the current context.
   * Since any API running on App Engine is already implicitily under the same root,
   * &lt;name&gt;-&lt;version&gt; should be able to uniquely identify an API.
   */
  public String getApiString() {
    return name + "-" + version;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ApiKey) {
      ApiKey that = (ApiKey) o;
      return this.name.equals(that.name)
          && this.version.equals(that.version)
          && Objects.equals(this.root, that.root);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, root);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("version", version)
        .add("root", root)
        .toString();
  }

  public ApiKey withoutRoot() {
    if (root == null) {
      return this;
    }
    return new ApiKey(name, version);
  }
}
