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
package com.google.api.server.spi.request;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.EndpointsContext;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.AuthLevel;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.api.server.spi.testing.TestEndpoint;
import com.google.api.server.spi.testing.TestEndpoint.Request;
import com.google.api.server.spi.types.DateAndTime;
import com.google.api.server.spi.types.SimpleDate;
import com.google.appengine.api.datastore.Blob;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for {@link ServletRequestParamReader}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServletRequestParamReaderTest {
  private static final String VALUE_STRING = "123";
  private static final boolean VALUE_BOOLEAN = true;
  private static final int VALUE_INTEGER = 123;
  private static final long VALUE_LONG = 1230L;
  private static final float VALUE_FLOAT = 12.3f;
  private static final double VALUE_DOUBLE = 1.23;

  private static final String USER_EMAIL = "test@gmail.com";
  private static final User USER = new User(USER_EMAIL);
  private static final com.google.appengine.api.users.User APP_ENGINE_USER =
      new com.google.appengine.api.users.User(USER_EMAIL, "");

  @Mock
  private HttpServletRequest request;

  @Mock
  private ServletContext context;

  @Mock
  private EndpointsContext endpointsContext;

  @Before
  public void setUp() {
    when(endpointsContext.getRequest()).thenReturn(request);
    when(endpointsContext.isPrettyPrintEnabled()).thenReturn(true);
  }

  @Test
  public void testRead() throws Exception {
    Object[] params = readExecuteMethod(ImmutableMap.<String, String>builder()
        .put(TestEndpoint.NAME_STRING, "\"" + VALUE_STRING + "\"")
        .put(TestEndpoint.NAME_BOOLEAN, String.valueOf(VALUE_BOOLEAN))
        .put(TestEndpoint.NAME_INTEGER, String.valueOf(VALUE_INTEGER))
        .put(TestEndpoint.NAME_LONG, String.valueOf(VALUE_LONG))
        .put(TestEndpoint.NAME_FLOAT, String.valueOf(VALUE_FLOAT))
        .put(TestEndpoint.NAME_DOUBLE, String.valueOf(VALUE_DOUBLE))
        .put(TestEndpoint.NAME_BOOLEAN_OBJECT, String.valueOf(VALUE_BOOLEAN))
        .put(TestEndpoint.NAME_INTEGER_OBJECT, String.valueOf(VALUE_INTEGER))
        .put(TestEndpoint.NAME_LONG_OBJECT, String.valueOf(VALUE_LONG))
        .put(TestEndpoint.NAME_FLOAT_OBJECT, String.valueOf(VALUE_FLOAT))
        .put(TestEndpoint.NAME_DOUBLE_OBJECT, String.valueOf(VALUE_DOUBLE))
        .put("stringValue", "321")
        .put("integerValue", "321")
        .put("more", "999").build());

    assertEquals(VALUE_STRING, params[0]);
    assertEquals(VALUE_BOOLEAN, params[1]);
    assertEquals(VALUE_INTEGER, params[2]);
    assertEquals(VALUE_LONG, params[3]);
    assertEquals(VALUE_FLOAT, params[4]);
    assertEquals(VALUE_DOUBLE, params[5]);
    assertEquals(VALUE_BOOLEAN, params[6]);
    assertEquals(VALUE_INTEGER, params[7]);
    assertEquals(VALUE_LONG, params[8]);
    assertEquals(VALUE_FLOAT, params[9]);
    assertEquals(VALUE_DOUBLE, params[10]);
    assertEquals("321", ((Request) params[11]).getStringValue());
    assertEquals(321, (int) ((Request) params[11]).getIntegerValue());
    assertEquals(USER, params[12]);
    assertEquals(APP_ENGINE_USER, params[13]);
    assertEquals(request, params[14]);
  }

  @Test
  public void testReadDate() throws Exception {
    Method method = TestEndpoint.class.getDeclaredMethod("getDate", Date.class);
    Object[] params =
        readParameters("{" + TestEndpoint.NAME_DATE + ":\"1970-01-01T00:00:00Z\"}", method);

    assertEquals(1, params.length);
    assertEquals(new Date(0), params[0]);
  }

  @Test
  public void testReadMissingParameters() throws Exception {
    Object[] params = readExecuteMethod(ImmutableMap.<String, String>builder()
        .put(TestEndpoint.NAME_STRING, "\"" + VALUE_STRING + "\"")
        .put(TestEndpoint.NAME_BOOLEAN, String.valueOf(VALUE_BOOLEAN))
        .put(TestEndpoint.NAME_LONG, String.valueOf(VALUE_LONG))
        .put(TestEndpoint.NAME_LONG_OBJECT, String.valueOf(VALUE_LONG))
        .put(TestEndpoint.NAME_DOUBLE, String.valueOf(VALUE_DOUBLE))
        .put(TestEndpoint.NAME_DOUBLE_OBJECT, String.valueOf(VALUE_DOUBLE))
        .put(TestEndpoint.NAME_BOOLEAN_OBJECT, String.valueOf(VALUE_BOOLEAN))
        .put(TestEndpoint.NAME_INTEGER, String.valueOf(VALUE_INTEGER))
        .put(TestEndpoint.NAME_INTEGER_OBJECT, String.valueOf(VALUE_INTEGER))
        .put(TestEndpoint.NAME_FLOAT, String.valueOf(VALUE_FLOAT))
        .put(TestEndpoint.NAME_FLOAT_OBJECT, String.valueOf(VALUE_FLOAT))
        .put("stringValue", "321")
        .put("more", "999").build());

    assertEquals(VALUE_STRING, params[0]);
    assertEquals(VALUE_BOOLEAN, params[1]);
    assertEquals(VALUE_INTEGER, params[2]);
    assertEquals(VALUE_LONG, params[3]);
    assertEquals(VALUE_FLOAT, params[4]);
    assertEquals(VALUE_DOUBLE, params[5]);
    assertEquals(VALUE_BOOLEAN, params[6]);
    assertEquals(VALUE_INTEGER, params[7]);
    assertEquals(VALUE_LONG, params[8]);
    assertEquals(VALUE_FLOAT, params[9]);
    assertEquals(VALUE_DOUBLE, params[10]);
    assertEquals("321", ((Request) params[11]).getStringValue());
    assertEquals(-1, (int) ((Request) params[11]).getIntegerValue());
    assertEquals(USER, params[12]);
    assertEquals(APP_ENGINE_USER, params[13]);
    assertEquals(request, params[14]);
  }

  private Object[] readExecuteMethod(ImmutableMap<String, String> parameters) throws Exception {
    Method method = TestEndpoint.class.getDeclaredMethod("succeed", String.class,
        boolean.class, int.class, long.class, float.class, double.class,
        Boolean.class, Integer.class, Long.class, Float.class, Double.class,
        Request.class, User.class, com.google.appengine.api.users.User.class,
        HttpServletRequest.class);
    StringBuilder builder = new StringBuilder("{");
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      builder.append(String.format("\"%s\":%s,", entry.getKey(), entry.getValue()));
    }
    builder.replace(builder.length() - 1, builder.length(), "}");
    Object[] params = readParameters(builder.toString(), method);
    assertEquals(15, params.length);
    return params;
  }

  @Test
  public void testReadDateAndTime() throws Exception {
    Method method = TestEndpoint.class.getDeclaredMethod("getDateAndTime", DateAndTime.class);
    String dateAndTimeString = "2002-10-02T10:00:00-05:00";

    Object[] params = readParameters(
        "{" + TestEndpoint.NAME_DATE_AND_TIME + ":\"" + dateAndTimeString + "\"}", method);

    assertEquals(1, params.length);
    assertEquals(DateAndTime.parseRfc3339String(dateAndTimeString), params[0]);
    assertEquals(dateAndTimeString, ((DateAndTime) params[0]).toRfc3339String());
  }

  @Test
  public void testReadSimpleDate_success() throws Exception {
    Method method = TestEndpoint.class.getDeclaredMethod("getSimpleDate", SimpleDate.class);
    Object[] params = null;
    params = readParameters(
        "{" + TestEndpoint.NAME_DATE_AND_TIME + ":\"2002-10-02\"}", method);
    assertThat(Arrays.asList(params)).containsExactly(new SimpleDate(2002, 10, 2)).inOrder();
  }

  @Test
  public void testReadSimpleDate_invalidYear() throws Exception {
    verifySimpleDateSerializationFails("-099-10-02");
  }

  @Test
  public void testReadSimpleDate_invalidMonth() throws Exception {
    verifySimpleDateSerializationFails("2002-13-02");
  }

  @Test
  public void testReadSimpleDate_invalidFormat() throws Exception {
    verifySimpleDateSerializationFails("99-10-02");
  }

  @Test
  public void testReadNoParameters() throws Exception {
    Method method = TestEndpoint.class.getDeclaredMethod("getResultNoParams");
    Object[] params = readParameters("", method);
    assertEquals(0, params.length);
  }

  @Test
  public void testReadByteArrayParameter() throws Exception {
    Method method =
        TestEndpoint.class.getDeclaredMethod("doSomething", byte[].class);
    Object[] params = readParameters("{\"bytes\":\"AQIDBA==\"}", method);

    assertEquals(1, params.length);
    assertThat((byte[]) params[0]).isEqualTo(new byte[]{1, 2, 3, 4});
  }

  @Test
  public void testReadBlobParameter() throws Exception {
    Method method =
        TestEndpoint.class.getDeclaredMethod("doBlob", Blob.class);
    Object[] params = readParameters("{\"blob\":\"AQIDBA==\"}", method);

    assertEquals(1, params.length);
    assertThat(((Blob) params[0]).getBytes()).isEqualTo(new byte[]{1, 2, 3, 4});
  }

  @Test
  public void testReadEnumParameter() throws Exception {
    Method method = TestEndpoint.class.getDeclaredMethod("doEnum", TestEndpoint.TestEnum.class);
    Object[] params = readParameters("{" + TestEndpoint.NAME_ENUM + ":\"TEST1\"}", method);

    assertEquals(1, params.length);
    assertEquals(TestEndpoint.TestEnum.TEST1, params[0]);
  }

  @Test
  public void testReadNullList() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void collection(@Nullable @Named("list") List<Integer> integers) {}
    }
    Method method = Test.class.getDeclaredMethod("collection", List.class);

    Object[] params = readParameters("{}", method);
    assertEquals(1, params.length);
    @SuppressWarnings("unchecked")
    List<Integer> integers = (List<Integer>) params[0];
    assertNull(integers);
  }

  @Test
  public void testReadNullArray() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void collection(@Nullable @Named("integer") Integer[] integers) {}
    }
    Method method = Test.class.getDeclaredMethod("collection", Integer[].class);

    Object[] params = readParameters("{}", method);
    assertEquals(1, params.length);
    @SuppressWarnings("unchecked")
    Integer[] integers = (Integer[]) params[0];
    assertNull(integers);
  }

  @Test
  public void testReadCollectionParameters() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void collection(@Named("collection") Collection<Integer> integers) {}
    }
    Method method = Test.class.getDeclaredMethod("collection", Collection.class);
    doTestCollectionParameter(
        "collection", EndpointMethod.create(Test.class, method));
  }

  @Test
  public void testReadGenericCollectionParameters() throws Exception {
    class TestGeneric<T> {
      @SuppressWarnings("unused")
      public void collection(@Named("collection") Collection<T> integers) {}
    }
    class Test extends TestGeneric<Integer> {}
    doTestCollectionParameter("collection", EndpointMethod.create(
        Test.class, Test.class.getMethod("collection", Collection.class),
        TypeToken.of(Test.class).getSupertype(TestGeneric.class)));
  }

  @Test
  public void testReadListParameters() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void collection(@Named("list") List<Integer> integers) {}
    }
    Method method = Test.class.getDeclaredMethod("collection", List.class);
    doTestCollectionParameter(
        "list", EndpointMethod.create(Test.class, method));
  }

  @Test
  public void testReadSetParameters() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void collection(@Named("set") Set<Integer> integers) {}
    }
    Method method = Test.class.getDeclaredMethod("collection", Set.class);
    doTestSetParameter(
        "set", EndpointMethod.create(Test.class, method));
  }

  @Test
  public void testReadArrayParameters() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void array(@Named("array") Integer[] integers) {}
    }
    Method method = Test.class.getDeclaredMethod("array", Integer[].class);
    doTestReadArrayParameter(
        "array", EndpointMethod.create(Test.class, method));
  }

  private void doTestCollectionParameter(String name, EndpointMethod method) throws Exception {
    Object[] params = readParameters("{\"" + name + "\":[1,2,3]}", method);

    assertEquals(1, params.length);
    @SuppressWarnings("unchecked")
    Collection<Integer> integers = (Collection<Integer>) params[0];
    assertEquals(3, integers.size());
    Iterator<Integer> iterator = integers.iterator();
    assertEquals(1, (int) iterator.next());
    assertEquals(2, (int) iterator.next());
    assertEquals(3, (int) iterator.next());
  }

  @Test
  public void testReadGenericArrayParameters() throws Exception {
    class TestGeneric<T> {
      @SuppressWarnings("unused")
      public void array(@Named("array") T[] integers) {}
    }
    class Test extends TestGeneric<Integer> {}
    doTestReadArrayParameter("array", EndpointMethod.create(
        Test.class, Test.class.getMethod("array", Object[].class),
        TypeToken.of(Test.class).getSupertype(TestGeneric.class)));
  }

  private void doTestSetParameter(String name, EndpointMethod method) throws Exception {
    Object[] params = readParameters("{\"" + name + "\":[1,2,1]}", method);

    assertEquals(1, params.length);
    @SuppressWarnings("unchecked")
    Set<Integer> integers = (Set<Integer>) params[0];
    assertEquals(2, integers.size());
    assertTrue(integers.contains(1));
  }

  private void doTestReadArrayParameter(String name, EndpointMethod method) throws Exception {
    Object[] params = readParameters("{\"" + name + "\":[1,2,3]}", method);

    assertEquals(1, params.length);
    Integer[] integers = (Integer[]) params[0];
    assertEquals(3, integers.length);
    assertEquals(1, (int) integers[0]);
    assertEquals(2, (int) integers[1]);
    assertEquals(3, (int) integers[2]);
  }

  @Test
  public void testReadDateCollectionParameters() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void collection(@Named("collection") Collection<Date> dates) {}
    }
    Method method = Test.class.getDeclaredMethod("collection", Collection.class);
    doTestReadCollectionDateParameter(
        "collection", EndpointMethod.create(Test.class, method));
  }

  @Test
  public void testReadGenericDateCollectionParameters() throws Exception {
    class TestGeneric<T> {
      @SuppressWarnings("unused")
      public void collection(@Named("collection") Collection<T> dates) {}
    }
    class Test extends TestGeneric<Date> {}
    doTestReadCollectionDateParameter("collection", EndpointMethod.create(
        Test.class, Test.class.getMethod("collection", Collection.class),
        TypeToken.of(Test.class).getSupertype(TestGeneric.class)));
  }

  private void doTestReadCollectionDateParameter(
      String name, EndpointMethod method) throws Exception {
    Object[] params = readParameters(
        "{\"" + name + "\":[\"2002-10-01\",\"2002-10-02\",\"2002-10-03\"]}", method);

    assertEquals(1, params.length);
    @SuppressWarnings("unchecked")
    Collection<Date> dates = (Collection<Date>) params[0];
    assertEquals(3, dates.size());
    Iterator<Date> iterator = dates.iterator();
    assertEquals(1, getCalendarFromDate(iterator.next()).get(Calendar.DAY_OF_MONTH));
    assertEquals(2, getCalendarFromDate(iterator.next()).get(Calendar.DAY_OF_MONTH));
    assertEquals(3, getCalendarFromDate(iterator.next()).get(Calendar.DAY_OF_MONTH));
  }

  @Test
  public void testReadDateArrayParameters() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void array(@Named("array") Date[] dates) {}
    }
    Method method = Test.class.getDeclaredMethod("array", Date[].class);
    doTestReadArrayDateParameter(
        "array", EndpointMethod.create(Test.class, method));
  }

  @Test
  public void testReadDateGenericArrayParameters() throws Exception {
    class TestGeneric<T> {
      @SuppressWarnings("unused")
      public void array(@Named("array") T[] dates) {}
    }
    class Test extends TestGeneric<Date> {}
    doTestReadArrayDateParameter("array", EndpointMethod.create(
        Test.class, Test.class.getMethod("array", Object[].class),
        TypeToken.of(Test.class).getSupertype(TestGeneric.class)));
  }

  private void doTestReadArrayDateParameter(String name, EndpointMethod method) throws Exception {
    Object[] params = readParameters(
        "{\"" + name + "\":[\"2002-10-01\",\"2002-10-02\",\"2002-10-03\"]}", method);

    assertEquals(1, params.length);
    Date[] dates = (Date[]) params[0];
    assertEquals(3, dates.length);
    assertEquals(1, getCalendarFromDate(dates[0]).get(Calendar.DAY_OF_MONTH));
    assertEquals(2, getCalendarFromDate(dates[1]).get(Calendar.DAY_OF_MONTH));
    assertEquals(3, getCalendarFromDate(dates[2]).get(Calendar.DAY_OF_MONTH));
  }

  private enum Outcome {
    WON, LOST, TIE
  }

  @Test
  public void testReadEnumCollectionParameters() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void collection(@Named("collection") Collection<Outcome> outcomes) {}
    }
    Method method = Test.class.getDeclaredMethod("collection", Collection.class);
    doTestReadCollectionEnumParameter(
        "collection", EndpointMethod.create(Test.class, method));
  }

  @Test
  public void testReadGenericEnumCollectionParameters() throws Exception {
    class TestGeneric<T> {
      @SuppressWarnings("unused")
      public void collection(@Named("collection") Collection<T> outcomes) {}
    }
    class Test extends TestGeneric<Outcome> {}
    doTestReadCollectionEnumParameter("collection", EndpointMethod.create(
        Test.class, Test.class.getMethod("collection", Collection.class),
        TypeToken.of(Test.class).getSupertype(TestGeneric.class)));
  }

  private void doTestReadCollectionEnumParameter(
      String name, EndpointMethod method) throws Exception {
    Object[] params = readParameters("{\"" + name + "\":[\"WON\",\"LOST\",\"TIE\"]}", method);

    assertEquals(1, params.length);
    @SuppressWarnings("unchecked")
    Collection<Outcome> outcomes = (Collection<Outcome>) params[0];
    assertEquals(3, outcomes.size());
    Iterator<Outcome> iterator = outcomes.iterator();
    assertEquals(Outcome.WON, iterator.next());
    assertEquals(Outcome.LOST, iterator.next());
    assertEquals(Outcome.TIE, iterator.next());
  }

  @Test
  public void testReadEnumArrayParameters() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void array(@Named("array") Outcome[] outcomes) {}
    }
    Method method = Test.class.getDeclaredMethod("array", Outcome[].class);
    doTestReadArrayEnumParameter("array", EndpointMethod.create(Test.class, method));
  }

  @Test
  public void testReadEnumGenericArrayParameters() throws Exception {
    class TestGeneric<T> {
      @SuppressWarnings("unused")
      public void array(@Named("array") T[] outcomes) {}
    }
    class Test extends TestGeneric<Outcome> {}
    doTestReadArrayEnumParameter("array", EndpointMethod.create(
        Test.class, Test.class.getMethod("array", Object[].class),
        TypeToken.of(Test.class).getSupertype(TestGeneric.class)));
  }

  private void doTestReadArrayEnumParameter(String name, EndpointMethod method) throws Exception {
    Object[] params = readParameters("{\"" + name + "\":[\"WON\",\"LOST\",\"TIE\"]}", method);

    assertEquals(1, params.length);
    Outcome[] outcomes = (Outcome[]) params[0];
    assertEquals(3, outcomes.length);
    assertEquals(Outcome.WON, outcomes[0]);
    assertEquals(Outcome.LOST, outcomes[1]);
    assertEquals(Outcome.TIE, outcomes[2]);
  }

  @Test
  public void testReadMultipleResourcesTest() throws Exception {
    class TestMultipleResources {
      @SuppressWarnings("unused")
      public void foo(@Named("str") String string,
          @Named("integer_array") Integer[] integers,
          @Named("integer_collection") Collection<Integer> ints,
          Request request) {}
    }
    String requestString = "{\"str\":\"hello\",\"" + TestEndpoint.NAME_STRING + "\":\""
        + VALUE_STRING + "\",\"" + TestEndpoint.NAME_INTEGER + "\":" + VALUE_INTEGER
        + ",\"integer_array\":[1,2,3]," + "\"integer_collection\":[4,5,6], \"stringValue\":"
        + "\"321\", \"integerValue\":321}";

    Method method = TestMultipleResources.class.getDeclaredMethod("foo",
        String.class, Integer[].class, Collection.class, Request.class);
    Object[] params = readParameters(requestString, method);

    assertEquals(4, params.length);
    String string = (String) params[0];
    assertEquals("hello", string);
    Integer[] integers = (Integer[]) params[1];
    assertEquals(3, integers.length);
    assertEquals(1, (int) integers[0]);
    assertEquals(2, (int) integers[1]);
    assertEquals(3, (int) integers[2]);
    @SuppressWarnings("unchecked")
    Collection<Integer> ints = (Collection<Integer>) params[2];
    assertEquals(3, ints.size());
    Iterator<Integer> iterator = ints.iterator();
    assertEquals(4, (int) iterator.next());
    assertEquals(5, (int) iterator.next());
    assertEquals(6, (int) iterator.next());
    Request request = (Request) params[3];
    assertEquals("321", request.getStringValue());
    assertEquals(321, (int) request.getIntegerValue());
  }

  @Test
  public void testJavaxNamed() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void foo(
          @javax.inject.Named("str") String str,
          @Nullable @javax.inject.Named("i") Integer i) {}
    }
    String requestString = "{\"str\":\"hello\"}";

    Method method = Test.class.getDeclaredMethod("foo", String.class, Integer.class);
    Object[] params = readParameters(requestString, method);

    assertEquals(2, params.length);
    assertEquals("hello", params[0]);
    assertNull(params[1]);
  }

  @Test
  public void testCachedNamesAreUsed() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void foo(@Named("foo1") String f1, @Nullable @Named("foo2") String f2,
          @Named("foo3") String f3) {}
    }

    Method method = Test.class.getDeclaredMethod("foo", String.class, String.class, String.class);
    EndpointMethod endpointMethod = EndpointMethod.create(method.getDeclaringClass(), method);
    endpointMethod.setParameterNames(ImmutableList.of("name1", "name2", "name3"));

    String requestString = "{\"name1\":\"v1\", \"foo2\":\"v2\", \"name3\":\"v3\"}";

    Object[] params = readParameters(requestString, endpointMethod);

    assertEquals(3, params.length);
    assertEquals("v1", params[0]);
    assertNull(params[1]);
    assertEquals("v3", params[2]);
  }

  @Test
  public void testNamesAreCached() throws Exception {
    class Test {
      @SuppressWarnings("unused")
      public void foo(
          @Nullable @Named("foo1") String f1,
          @Nullable @Named("foo2") String f2,
          @Nullable @Named("foo3") String f3) {}
    }

    Method method = Test.class.getDeclaredMethod("foo", String.class, String.class, String.class);
    EndpointMethod endpointMethod = EndpointMethod.create(method.getDeclaringClass(), method);

    readParameters("{}", endpointMethod);
    List<String> parameterNames = endpointMethod.getParameterNames();

    assertEquals(3, parameterNames.size());
    assertEquals("foo1", parameterNames.get(0));
    assertEquals("foo2", parameterNames.get(1));
    assertEquals("foo3", parameterNames.get(2));
  }

  static class TestUser extends User {
    public TestUser(String email) {
      super(email);
    }
  }

  @Test
  public void testReadUser() throws Exception {
    class TestUserEndpoint {
      @SuppressWarnings("unused")
      public void user(TestUser user) {}
    }
    final TestUser user = new TestUser("test");
    Method method = TestUserEndpoint.class.getDeclaredMethod("user", TestUser.class);
    ParamReader reader = new ServletRequestParamReader(
        EndpointMethod.create(method.getDeclaringClass(), method), endpointsContext, context, null,
        null) {
      @Override
      User getUser() {
        return user;
      }

      @Override
      com.google.appengine.api.users.User getAppEngineUser() {
        return APP_ENGINE_USER;
      }
    };
    Object[] params = readParameters("{}", reader);
    assertEquals(1, params.length);
    assertEquals(user, params[0]);
  }

  @Test
  public void testReadUserIp() throws Exception {
    class TestUserIp {
      @SuppressWarnings("unused")
      public void userIp(@Named("userIp") String userIp) {}
    }
    String ip = "9.8.7.6";
    when(request.getRemoteAddr()).thenReturn(ip);
    Object[] params = readParameters("{}",
        TestUserIp.class.getDeclaredMethod("userIp", String.class));
    assertEquals(1, params.length);
    assertEquals(ip, params[0]);
  }

  @Test
  public void testReadAlt() throws Exception {
    class TestAlt {
      @SuppressWarnings("unused")
      public void alt(@Named("alt") String alt) {}
    }
    Object[] params = readParameters(
        "{\"alt\":\"test\"}", TestAlt.class.getDeclaredMethod("alt", String.class));
    assertEquals(1, params.length);
    assertEquals("test", params[0]);
  }

  @Test
  public void testReadAlt_defaultValue() throws Exception {
    class TestAlt {
      @SuppressWarnings("unused")
      public void alt(@Named("alt") String alt) {}
    }
    Object[] params = readParameters("{}", TestAlt.class.getDeclaredMethod("alt", String.class));
    assertEquals(1, params.length);
    assertEquals("json", params[0]);
  }

  @Test
  public void testReadFields() throws Exception {
    class TestFields {
      @SuppressWarnings("unused")
      public void fields(@Named("fields") String fields) {}
    }
    Object[] params = readParameters(
        "{\"fields\":\"test\"}", TestFields.class.getDeclaredMethod("fields", String.class));
    assertEquals(1, params.length);
    assertEquals("test", params[0]);
  }

  @Test
  public void testReadKey() throws Exception {
    class TestKey {
      @SuppressWarnings("unused")
      public void key(@Named("key") String key) {}
    }
    Object[] params = readParameters(
        "{\"key\":\"test\"}", TestKey.class.getDeclaredMethod("key", String.class));
    assertEquals(1, params.length);
    assertEquals("test", params[0]);
  }

  @Test
  public void testReadOAuthToken() throws Exception {
    class TestOAuthToken {
      @SuppressWarnings("unused")
      public void oAuthToken(@Named("oauth_token") String token) {}
    }
    Object[] params = readParameters(
        "{\"oauth_token\":\"test\"}",
        TestOAuthToken.class.getDeclaredMethod("oAuthToken", String.class));
    assertEquals(1, params.length);
    assertEquals("test", params[0]);
  }

  @Test
  public void testQuotaUser() throws Exception {
    class TestQuotaUser {
      @SuppressWarnings("unused")
      public void quotaUser(@Named("quotaUser") String quotaUser) {}
    }
    Object[] params = readParameters(
        "{\"quotaUser\":\"test\"}",
        TestQuotaUser.class.getDeclaredMethod("quotaUser", String.class));
    assertEquals(1, params.length);
    assertEquals("test", params[0]);
  }

  @Test
  public void testPrettyPrint() throws Exception {
    class TestPrettyPrint {
      @SuppressWarnings("unused")
      public void prettyPrint(@Named("prettyPrint") String prettyPrint) {}
    }
    when(request.getParameter("prettyPrint")).thenReturn("false");
    Object[] params =
        readParameters("{}", TestPrettyPrint.class.getDeclaredMethod("prettyPrint", String.class));
    assertEquals(1, params.length);
    assertEquals(false, params[0]);
  }

  @Test
  public void testPrettyPrint_defaultValue() throws Exception {
    class TestPrettyPrint {
      @SuppressWarnings("unused")
      public void prettyPrint(@Named("prettyPrint") String prettyPrint) {}
    }
    Object[] params =
        readParameters("{}", TestPrettyPrint.class.getDeclaredMethod("prettyPrint", String.class));
    assertEquals(1, params.length);
    assertEquals(true, params[0]);
  }

  @Test
  public void testUserInjectionThrowsExceptionIfRequired() throws Exception {
    @SuppressWarnings("unused")
    class TestUser {
      @SuppressWarnings("unused")
      public void getUser(User user) { }
    }
    ApiMethodConfig methodConfig = Mockito.mock(ApiMethodConfig.class);
    when(methodConfig.getAuthLevel()).thenReturn(AuthLevel.REQUIRED);
    methodConfig.setAuthLevel(AuthLevel.REQUIRED);
    try {
      Method method = TestUser.class.getDeclaredMethod("getUser", User.class);
      readParameters(
          "{}", EndpointMethod.create(method.getDeclaringClass(), method),
          methodConfig,
          null,
          null);
      fail("expected unauthorized method exception");
    } catch (UnauthorizedException ex) {
      // expected
    }
  }

  @Test
  public void testAppEngineUserInjectionThrowsExceptionIfRequired() throws Exception {
    @SuppressWarnings("unused")
    class TestUser {
      @SuppressWarnings("unused")
      public void getUser(com.google.appengine.api.users.User user) { }
    }
    ApiMethodConfig methodConfig = Mockito.mock(ApiMethodConfig.class);
    when(methodConfig.getAuthLevel()).thenReturn(AuthLevel.REQUIRED);
    methodConfig.setAuthLevel(AuthLevel.REQUIRED);
    try {
      Method method = TestUser.class
          .getDeclaredMethod("getUser", com.google.appengine.api.users.User.class);
      readParameters(
          "{}",
          EndpointMethod.create(method.getDeclaringClass(), method),
          methodConfig,
          null,
          null);
      fail("expected unauthorized method exception");
    } catch (UnauthorizedException ex) {
      // expected
    }
  }

  @Test
  public void testNullValueForRequiredParam() throws Exception {
    class TestNullValueForRequiredParam {
      @SuppressWarnings("unused")
      public void test(@Named("testParam") String testParam) {}
    }
    try {
      Object[] params =
          readParameters("{}",
              TestNullValueForRequiredParam.class.getDeclaredMethod("test", String.class));
      fail("expected bad request exception");
    } catch (BadRequestException ex) {
      // expected
    }
  }

  private Object[] readParameters(String input, Method method) throws Exception {
    return readParameters(input, EndpointMethod.create(method.getDeclaringClass(), method));
  }

  private Object[] readParameters(final String input, EndpointMethod method) throws Exception {
    return readParameters(input, method, null, USER, APP_ENGINE_USER);
  }

  private Object[] readParameters(final String input, EndpointMethod method,
      ApiMethodConfig methodConfig, final User user,
      final com.google.appengine.api.users.User appEngineUser)
      throws Exception {
    ParamReader reader = new ServletRequestParamReader(method, endpointsContext, context, null,
        methodConfig) {
      @Override
      User getUser() {
        return user;
      }
      @Override
      com.google.appengine.api.users.User getAppEngineUser() {
        return appEngineUser;
      }
    };
    return readParameters(input, reader);
  }

  private Object[] readParameters(final String input, ParamReader reader)
      throws Exception {
    ServletInputStream servletInputStream = new ServletInputStream() {

      private final InputStream inputStream =
          new ByteArrayInputStream(input.getBytes(UTF_8));

      @Override
      public int read() throws IOException {
        return inputStream.read();
      }
    };
    when(request.getInputStream()).thenReturn(servletInputStream);
    return reader.read();
  }

  private void verifySimpleDateSerializationFails(String simpleDateString)
      throws Exception {
    Method method = TestEndpoint.class.getDeclaredMethod("getSimpleDate", SimpleDate.class);
    try {
      readParameters(
          "{" + TestEndpoint.NAME_DATE_AND_TIME + ":\"" + simpleDateString + "\"}", method);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {}
  }

  private Calendar getCalendarFromDate(Date date) {
    Calendar c = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
    c.setTime(date);
    return c;
  }
}
