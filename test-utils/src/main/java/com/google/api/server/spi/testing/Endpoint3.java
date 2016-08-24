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


import java.util.List;

import javax.inject.Named;

/**
 * Test endpoint with inheritance and 2 resource collections, second of which uses
 * default configuration.
 */
public class Endpoint3 extends Endpoint1 {

  public List<Bar> listBars() {
    return null;
  }

  public Bar getBar(@Named("id") String id) {
    return null;
  }

  public Bar insertBar(Bar bar) {
    return null;
  }

  public Bar updateBar(@Named("id") String id, Bar bar) {
    return null;
  }

  public void removeBar(@Named("id") String id) {
  }
}
