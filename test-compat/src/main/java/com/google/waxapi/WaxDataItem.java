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
package com.google.waxapi;

import java.util.Objects;

/**
 * An arbitrary item used for testing in the Wax API.
 */
public class WaxDataItem {
  private String id;
  private String name;
  private String blobOfData;

  public WaxDataItem() {
  }

  public WaxDataItem(String id, String name, String blobOfData) {
    this.id = id;
    this.name = name;
    this.blobOfData = blobOfData;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBlobOfData() {
    return blobOfData;
  }

  public void setBlobOfData(String blobOfData) {
    this.blobOfData = blobOfData;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof WaxDataItem)) {
      return false;
    }
    WaxDataItem other = (WaxDataItem) obj;
    return Objects.equals(id, other.id) && Objects.equals(name, other.name)
        && Objects.equals(blobOfData, other.blobOfData);
  }

  @Override
  public String toString() {
    return "[WaxDataItem id: " + id + " name: " + name + " blobOfData: " + blobOfData + "]";
  }
}
