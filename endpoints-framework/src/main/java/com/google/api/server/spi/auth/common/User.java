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
package com.google.api.server.spi.auth.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Representation of a specific authenticated user.
 *
 * @author Eric Orth
 */
public class User implements Serializable {
  private final String id;
  private final String email;

  public User(String email) {
    this(null, email);
  }

  public User(String id, String email) {
    this.id = id;
    this.email = email;
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof User) {
      User user = (User) other;
      return Objects.equals(id, user.id) && Objects.equals(email, user.email);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, email);
  }

  @Override
  public String toString() {
    return String.format("id:%s, email:%s", id, email);
  }
}
