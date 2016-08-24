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
import com.google.api.server.spi.config.ApiMethod;

import java.io.Serializable;

import javax.inject.Named;

class BoundedGenericEndpoint2<T extends Number & Serializable, U extends Foo> {
  public void foo(T t, U u) {
    
  }
}

class BoundedGenericEndpoint1<T extends Integer & Comparable<Integer>> 
    extends BoundedGenericEndpoint2<T, Foo> {
}

/**
 * Testing bounded generics. This is surprisingly simple, since
 *   A) Wildcards are not allowed as class parameters. Since parameter types are supposed
 *   to be references throughout the class (they don't serve only as produced/consumed), they have
 *   to be named... hence no "? extends Blah"
 *   
 *   B) The bounds are enforced at compile type and that information is lost. In fact,
 *   Class<?>.getTypeParameters will only return "T" and not "T extends Number"
 */
@Api
public class BoundedGenericEndpoint extends BoundedGenericEndpoint1<Integer> {
  @Override
  @ApiMethod
  public void foo(@Named("my_int") Integer i, Foo d) {
    
  } 
}
