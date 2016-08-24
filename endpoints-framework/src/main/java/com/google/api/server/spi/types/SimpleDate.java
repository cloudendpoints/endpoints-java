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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Calendar;
import java.util.Objects;

/**
 * A class for storing simple date values without a time component.
 */
public class SimpleDate {

  private int year;
  private int month;
  private int day;

  /**
   * Constructs a new simple date.
   *
   * @param year the full year number
   * @param month a month number between 1-12
   * @param day a day number between 1-31
   * @throws IllegalArgumentException throws if the date is invalid, or the year is outside the
   *     RFC3339 supported range
   */
  public SimpleDate(int year, int month, int day) {
    checkArgument(year >= 1 && year <= 9999, "Only year values between 1 and 9999 are supported.");

    Calendar calendar = Calendar.getInstance();
    calendar.setLenient(false);
    calendar.set(year, month - 1, day, 0, 0, 0);
    calendar.getTime(); // Throws IllegalArgumentException if time is invalid.

    this.year = year;
    this.month = month;
    this.day = day;
  }

  /**
   * Gets the calendar year.
   *
   * @return the full year number
   */
  public int getYear() {
    return year;
  }

  /**
   * Gets the calendar month number. This would return 1 for January, and 12 for December.
   *
   * @return the month number
   */
  public int getMonth() {
    return month;
  }

  /**
   * Gets the calendar day; this should be between 1 and 31.
   *
   * @return the day number
   */
  public int getDay() {
    return day;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof SimpleDate)) {
      return false;
    }
    SimpleDate other = (SimpleDate) obj;
    return this.year == other.year && this.month == other.month && this.day == other.day;
  }

  @Override
  public int hashCode() {
    return Objects.hash(year, month, day);
  }

  @Override
  public String toString() {
    return String.format("%04d-%02d-%02d", year, month, day);
  }
}
