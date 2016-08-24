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
package com.google.testapi;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;

import java.util.Date;

@Api(name = "tictactoe",
    namespace = @ApiNamespace(ownerDomain = "compat-tests.com", ownerName = "Compat Tests"))
public class TestEndpoint {
  @ApiMethod(httpMethod = HttpMethod.GET, path = "test")
  public StringValue test() throws Exception {
    return new StringValue("x");
  }

  @ApiMethod(httpMethod = HttpMethod.GET, path = "echo/{value}")
  public StringValue echo(@Named("value") String value) {
    return new StringValue(value);
  }

  public static class FieldContainer {
    public int anInt;
    public long aLong;
    public float aFloat;
    public double aDouble;
    public boolean aBoolean;
    public String aString;
    public SimpleDate aSimpleDate;
    public DateAndTime aDateAndTime;
    public Date aDate;
    public TestEnum anEnum;
  }

  // http://localhost:8080/_ah/api/tictactoe/v1/testparamspath/1/2/1.1/2.1/true/中文/2015-10-26/
  // 2015-10-26T18:46:21Z/2015-10-26T16:46:21Z/VALUE2
  @ApiMethod(httpMethod = HttpMethod.GET, path = "testparamspath/{anint}/{along}/{afloat}/{adouble}"
      + "/{aboolean}/{astring}/{asimpledate}/{adateandtime}/{adate}/{anenum}")
  public FieldContainer testParamsPath(
      @Named("anint") int anInt,
      @Named("along") long aLong,
      @Named("afloat") float aFloat,
      @Named("adouble") double aDouble,
      @Named("aboolean") boolean aBoolean,
      @Named("astring") String aString,
      @Named("asimpledate") SimpleDate aSimpleDate,
      @Named("adateandtime") DateAndTime aDateAndTime,
      @Named("adate") Date aDate,
      @Named("anenum") TestEnum anEnum) {
    FieldContainer ret = new FieldContainer();
    ret.anInt = anInt;
    ret.aLong = aLong;
    ret.aFloat = aFloat;
    ret.aDouble = aDouble;
    ret.aBoolean = aBoolean;
    ret.aString = aString;
    ret.aSimpleDate = aSimpleDate;
    ret.aDateAndTime = aDateAndTime;
    ret.aDate = aDate;
    ret.anEnum = anEnum;
    return ret;
  }

  // http://localhost:8080/_ah/api/tictactoe/v1/testparamsquery?anint=1&along=2&afloat=1.1
  // &adouble=2.1&aboolean=true&astring=中文&asimpledate=2015-10-26
  // &adateandtime=2015-10-26T18:46:21Z&adate=2015-10-26T16:46:21Z&anenum=VALUE1
  @ApiMethod(httpMethod = HttpMethod.GET, path = "testparamsquery")
  public FieldContainer testParamsQuery(
      @Named("anint") Integer anInt,
      @Named("along") Long aLong,
      @Named("afloat") Float aFloat,
      @Named("adouble") Double aDouble,
      @Named("aboolean") Boolean aBoolean,
      @Named("astring") String aString,
      @Named("asimpledate") SimpleDate aSimpleDate,
      @Named("adateandtime") DateAndTime aDateAndTime,
      @Named("adate") Date aDate,
      @Named("anenum") TestEnum anEnum) {
    FieldContainer ret = new FieldContainer();
    ret.anInt = anInt;
    ret.aLong = aLong;
    ret.aFloat = aFloat;
    ret.aDouble = aDouble;
    ret.aBoolean = aBoolean;
    ret.aString = aString;
    ret.aSimpleDate = aSimpleDate;
    ret.aDateAndTime = aDateAndTime;
    ret.aDate = aDate;
    ret.anEnum = anEnum;
    return ret;
  }

  @ApiMethod(httpMethod = HttpMethod.GET, path = "collidingpath/{id}")
  public StringValue getCollidingPath(@Named("id") String id) {
    return new StringValue(id);
  }

  @ApiMethod(httpMethod = HttpMethod.PUT, path = "collidingpath/{anotherId}")
  public StringValue putCollidingPath(@Named("anotherId") String id) {
    return new StringValue(id);
  }

  public static class StringValue {
    public String value;

    public StringValue(String value) {
      this.value = value;
    }
  }

  public enum TestEnum {
    VALUE1, VALUE2
  }
}
