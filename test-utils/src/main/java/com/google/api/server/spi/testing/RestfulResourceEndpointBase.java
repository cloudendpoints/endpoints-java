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
package com.google.api.server.spi.testing;

import com.google.api.server.spi.config.Api;

import java.util.List;

import javax.inject.Named;

/**
 * This class has the basic REST methods (list, get, update, insert).
 *
 * @param <T> is the parameterized type of this skeleteon ReSTful endpoint.
 */
public abstract class RestfulResourceEndpointBase<T> {
  /**
   * Return all the objects of type T.
   */
  public List<T> list() {
    return null;
  }

  /**
   * Get a single object of type T by id.
   *
   * @param id the id of the object to get.
   */
  public T get(@Named("id") long id) {
    return null;
  }

  /**
   * Insert an object of type T.
   *
   * @param object the object to insert.
   */
  public T insert(T object) {
    return null;
  }

  /**
   * Update an existing object.
   *
   * @param updated the updated object.
   */
  public T update(T updated) {
    return null;
  }

  /**
   * Remove an object of type T.
   *
   * @param id the object to remove.
   */
  public void remove(@Named("id") long id) {
    // empty
  }

  /**
   * A miscelannous function.
   *
   * @param item is the object to be operated on/with.
   */
  public void misc(T item) {
    // empty
  }

  /**
   * Tests to see that parameterized methods can overload generics ones.
   */
  @Api(name = "partialApi")
  public static class PartiallySpecializedEndpoint extends RestfulResourceEndpointBase<Foo> {
    @Override
    public Foo get(@Named("id") long id) {
      return null;
    }

    @Override
    public Foo insert(Foo updated) {
      return null;
    }

    @Override
    public Foo update(Foo updated) {
      return null;
    }
  }

  /**
   * Tests to see that parameterized methods can overload generics ones.
   */
  @Api(name = "fullApi")
  public static class FullySpecializedEndpoint extends PartiallySpecializedEndpoint {
    @Override
    public List<Foo> list() {
      return null;
    }

    @Override
    public void misc(Foo item) {
      // empty
    }
  }
}



