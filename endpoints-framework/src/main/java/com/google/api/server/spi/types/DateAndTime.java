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
package com.google.api.server.spi.types;

import com.google.common.base.Preconditions;

/**
 * A class for storing date-time values. Unlike {@code java.util.Date}, this preserves time zone
 * information.
 */
public final class DateAndTime {

  private final String rfc3339String;
  private final com.google.api.client.util.DateTime clientDateTime;

  private DateAndTime(String rfc3339String) {
    this.rfc3339String = Preconditions.checkNotNull(rfc3339String);
    this.clientDateTime = com.google.api.client.util.DateTime.parseRfc3339(rfc3339String);
  }

  /**
   * Returns the RFC 3339 representation of the date-time.
   *
   * @return an RFC 3339 string
   */
  public String toRfc3339String() {
    return rfc3339String;
  }

  /**
   * Constructs a new date-time from an RFC 3339 formatted string.
   *
   * @param rfc3339String An RFC 3339 string to parse.
   * @return the parsed datetime
   */
  public static DateAndTime parseRfc3339String(String rfc3339String) {
    try {
      return new DateAndTime(rfc3339String);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "DateAndTime string is not valid RFC3339: " + rfc3339String, e);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof DateAndTime)) {
      return false;
    }
    DateAndTime other = (DateAndTime) obj;
    return this.clientDateTime.equals(other.clientDateTime);
  }

  @Override
  public int hashCode() {
    return clientDateTime.hashCode();
  }

  @Override
  public String toString() {
    return toRfc3339String();
  }
}
