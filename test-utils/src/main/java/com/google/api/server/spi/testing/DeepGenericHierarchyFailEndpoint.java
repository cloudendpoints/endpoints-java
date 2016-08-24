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

import java.util.Collection;
import java.util.List;

import javax.inject.Named;

class DeepGenericHierarchyFailEndpoint3<T, U, V> {
  @ApiMethod(name = "Endpoint3.foo", path = "bar")
  public void foo(@Named("1") T t, @Named("2") U u, @Named("3") V v) {
    
  }
}

class DeepGenericHierarchyFailEndpoint2<T, U> 
    extends DeepGenericHierarchyFailEndpoint3<T, U, List<Integer>> {
}

class DeepGenericHierarchyFailEndpoint1<T> extends DeepGenericHierarchyFailEndpoint2<T, Integer> {
}

/**
 * Deep hierarchy
 */
@Api
public class DeepGenericHierarchyFailEndpoint extends DeepGenericHierarchyFailEndpoint1<String> {
  @ApiMethod
  public void foo(@Named("id_1") String s, @Named("id_2") Integer i,
      @Named("id_3") Collection<Integer> c) {
    
  }
}
