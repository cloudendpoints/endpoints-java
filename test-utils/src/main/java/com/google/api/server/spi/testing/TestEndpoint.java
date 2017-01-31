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

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.oauth.OAuthRequestException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * Endpoint class for testing.
 */
@Api
public class TestEndpoint extends TestEndpointSuperclass<Foo> {

  public static final String NAME_DATE = "date";
  public static final String NAME_DATE_AND_TIME = "dateandtime";
  public static final String NAME_STRING = "string";
  public static final String NAME_BOOLEAN = "boolean";
  public static final String NAME_INTEGER = "integer";
  public static final String NAME_LONG = "long";
  public static final String NAME_FLOAT = "float";
  public static final String NAME_DOUBLE = "double";
  public static final String NAME_BOOLEAN_OBJECT = "Boolean";
  public static final String NAME_INTEGER_OBJECT = "Integer";
  public static final String NAME_LONG_OBJECT = "Long";
  public static final String NAME_FLOAT_OBJECT = "Float";
  public static final String NAME_DOUBLE_OBJECT = "Double";
  public static final String NAME_ENUM = "enum";
  public static final Foo RESULT = new Foo();
  public static final String ERROR_MESSAGE = "error message";

  public static class Request {
    private String string;
    private Integer integer = -1;

    public void setStringValue(String string) {
      this.string = string;
    }

    public void setIntegerValue(int integer) {
      this.integer = integer;
    }

    public String getStringValue() {
      return string;
    }

    public Integer getIntegerValue() {
      return integer;
    }
  }

  public enum TestEnum {
    TEST1, TEST2
  }

  public Foo succeed(@Named(NAME_STRING) String s, @Named(NAME_BOOLEAN) boolean b1,
      @Named(NAME_INTEGER) int i1, @Named(NAME_LONG) long l1, @Named(NAME_FLOAT) float f1,
      @Named(NAME_DOUBLE) double d1, @Named(NAME_BOOLEAN_OBJECT) Boolean b2,
      @Named(NAME_INTEGER_OBJECT) Integer i2, @Named(NAME_LONG_OBJECT) Long l2,
      @Named(NAME_FLOAT_OBJECT) Float f2, @Named(NAME_DOUBLE_OBJECT) Double d2,
      Request request, User user1, com.google.appengine.api.users.User user2,
      HttpServletRequest servletRequest) {
    return RESULT;
  }

  public Foo getDate(@Named(NAME_DATE) Date date) {
    return RESULT;
  }

  @ApiMethod(path = "dateandtime")
  public Foo getDateAndTime(@Named(NAME_DATE_AND_TIME) DateAndTime dateTime) {
    return RESULT;
  }

  @ApiMethod(path = "simpledate")
  public Foo getSimpleDate(@Named(NAME_DATE_AND_TIME) SimpleDate simpleDate) {
    return RESULT;
  }

  @ApiMethod(path = "noresults")
  public Foo getResultNoParams() {
    return RESULT;
  }

  public Foo fail(@Named(NAME_STRING) String p0, @Named(NAME_INTEGER) int p1, Request request,
      User p2, HttpServletRequest httpRequest) throws BadRequestException {
    throw new BadRequestException(ERROR_MESSAGE);
  }

  public Foo failOAuth(@Named(NAME_STRING) String p0, @Named(NAME_INTEGER) int p1,
      Request request, User p2, HttpServletRequest httpRequest) throws OAuthRequestException {
    throw new OAuthRequestException(ERROR_MESSAGE);
  }

  public Foo failWrapped() throws Exception {
    throw new Exception(null, new ServiceException(401, ERROR_MESSAGE));
  }

  public Foo failIllegalArgumentException() {
    throw new IllegalArgumentException();
  }

  public void doSomething(@Named("bytes") byte[] in) {}

  public void doBlob(@Named("blob") Blob b) {}

  public void doParameterizedOverloadTest(Map<String, String> m) {}

  public void doMap(Map<String, String> m) {}

  public void doEnum(@Named(NAME_ENUM) TestEnum e) {}

  @Override
  public Foo overrideMethod(Foo s) {
    return null;
  }

  @Override
  public void overrideMethod1() {}

  @Override
  public Foo overrideMethod2(@Named(NAME_BOOLEAN) boolean bleh) {
    return null;
  }

  public static Map<String, Object> staticMethod() {
    return null;
  }

  public enum ExpectedMethod {
    SUCCEED(TestEndpoint.class),
    GET_DATE(TestEndpoint.class),
    GET_DATE_AND_TIME(TestEndpoint.class),
    GET_SIMPLE_DATE(TestEndpoint.class),
    GET_RESULT_NO_PARAMS(TestEndpoint.class),
    FAIL(TestEndpoint.class),
    FAIL_OAUTH(TestEndpoint.class),
    FAIL_WRAPPED(TestEndpoint.class),
    FAIL_ILLEGAL_ARGUMENT_EXCEPTION(TestEndpoint.class),
    DO_SOMETHING(TestEndpoint.class),
    DO_SOMETHING_PARAMETERIZED_OVERLOAD(TestEndpoint.class),
    DO_BLOB(TestEndpoint.class),
    DO_MAP(TestEndpoint.class),
    DO_ENUM(TestEndpoint.class),
    OVERRIDE_METHOD(TestEndpoint.class, true),
    OVERRIDE_METHOD1(TestEndpoint.class, true),
    OVERRIDE_METHOD2(TestEndpoint.class, true),
    SUPERCLASS_METHOD(TestEndpointSuperclass.class),
    UNKNOWN(null);

    public final Class<?> declaringClass;
    public boolean isOverride;

    ExpectedMethod(Class<?> declaringClass) {
      this(declaringClass, false);
    }

    ExpectedMethod(Class<?> declaringClass, boolean isOverride) {
      this.declaringClass = declaringClass;
      this.isOverride = isOverride;
    }

    private static Map<String, ExpectedMethod> nameToExpectedMethodMap;
    static {
      nameToExpectedMethodMap = new HashMap<String, ExpectedMethod>();
      nameToExpectedMethodMap.put("succeed", SUCCEED);
      nameToExpectedMethodMap.put("getDate", GET_DATE);
      nameToExpectedMethodMap.put("getDateAndTime", GET_DATE_AND_TIME);
      nameToExpectedMethodMap.put("getSimpleDate", GET_SIMPLE_DATE);
      nameToExpectedMethodMap.put("getResultNoParams", GET_RESULT_NO_PARAMS);
      nameToExpectedMethodMap.put("fail", FAIL);
      nameToExpectedMethodMap.put("failOAuth", FAIL_OAUTH);
      nameToExpectedMethodMap.put("failWrapped", FAIL_WRAPPED);
      nameToExpectedMethodMap.put("failIllegalArgumentException", FAIL_ILLEGAL_ARGUMENT_EXCEPTION);
      nameToExpectedMethodMap.put("doSomething", DO_SOMETHING);
      nameToExpectedMethodMap.put(
          "doParameterizedOverloadTest", DO_SOMETHING_PARAMETERIZED_OVERLOAD);
      nameToExpectedMethodMap.put("doBlob", DO_BLOB);
      nameToExpectedMethodMap.put("doMap", DO_MAP);
      nameToExpectedMethodMap.put("doEnum", DO_ENUM);
      nameToExpectedMethodMap.put("overrideMethod", OVERRIDE_METHOD);
      nameToExpectedMethodMap.put("overrideMethod1", OVERRIDE_METHOD1);
      nameToExpectedMethodMap.put("overrideMethod2", OVERRIDE_METHOD2);
      nameToExpectedMethodMap.put("superclassMethod", SUPERCLASS_METHOD);
    }

    public static ExpectedMethod fromName(String name) {
      ExpectedMethod method = nameToExpectedMethodMap.get(name);
      return method == null ? UNKNOWN : method;
    }
  }
}
