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

import com.google.api.server.spi.config.ApiResourceProperty;

/**
 * Test resource type with descriptions.
 */
public class FooDescription {

  @ApiResourceProperty(description = "description of name")
  private String name;
  private int value;
  private String hidden;
  @ApiResourceProperty(description = "description of choice")
  private TestEnumDescription choice;

  public String getName() {
    return name;
  }

  @ApiResourceProperty(description = "description of value")
  public int getValue() {
    return value;
  }

  private String getHidden() {
    return hidden;
  }

  private void setHidden(String hidden) {
    this.hidden = hidden;
  }

  public TestEnumDescription getChoice() {
    return choice;
  }
}
